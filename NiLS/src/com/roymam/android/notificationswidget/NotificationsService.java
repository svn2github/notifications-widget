package com.roymam.android.notificationswidget;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.roymam.android.common.PopupDialog;
import com.roymam.android.common.SysUtils;
import com.roymam.android.nilsplus.activities.OpenNotificationActivity;
import com.roymam.android.nilsplus.activities.QuickReplyActivity;
import com.roymam.android.nilsplus.ui.NPViewManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NotificationsService extends Service implements NotificationsProvider
{
    private static final String TAG = NotificationsService.class.getSimpleName();
    public static final String EXTRA_PACKAGENAME = "EXTRA_PACKAGENAME";
    public static final String EXTRA_UID = "EXTRA_UID";
    public static final String EXTRA_ID = "EXTRA_ID";
    public static final String EXTRA_TAG = "EXTRA_TAG";

    // notification list modification events
    public static final String REMOVE_NOTIFICATION = "com.roymam.android.nils.remove_notification";
    public static final String CANCEL_NOTIFICATION = "com.roymam.android.nils.cancel_notification";

    // lock screen constants
    public static final String STOCK_LOCKSCREEN_PACKAGENAME = "com.android.keyguard" ;

    // widgetlocker events
    public static final String WIDGET_LOCKER_UNLOCKED = "com.teslacoilsw.widgetlocker.intent.UNLOCKED";
    public static final String WIDGET_LOCKER_HIDE = "com.teslacoilsw.widgetlocker.intent.HIDE";

    public static final String WIDGET_LOCKER_PACKAGENAME = "com.teslacoilsw.widgetlocker";

    // go locker events
    public static final String GO_LOCKER_PACKAGENAME = "com.jiubang.goscreenlock";
    public static final String GO_LOCKER_UNLOCKED = "com.jiubang.goscreenlock.unlock";

    // other events
    public static final String CHECK_LSAPP = "com.roymam.android.nils.CHECK_LS";
    public static final String INCOMING_CALL = "com.roymam.android.nils.INCOMING_CALL";
    public static final String DEVICE_UNLOCKED = "com.roymam.android.nils.UNLOCKED";
    public static final String REFRESH_LIST = "com.roymam.android.nils.REFRESH_LIST";

    private NPReceiver npreceiver = null;
    private NPViewManager viewManager = null;
    private NPViewManager.Callbacks mViewManagerCallbacks = null;
    private SysUtils mSysUtils;
    private Context context;
    private NotificationEventListener listener;
    private ArrayList<NotificationData> mNotifications = new ArrayList<NotificationData>();
    private NotificationParser parser;
    private HashMap<String, PersistentNotification> persistentNotifications = new HashMap<String, PersistentNotification>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();;
    private ArrayList<NotificationData> mFilteredNotificationsList;
    private final Handler mHandler = new Handler();

    public HashMap<String, NotificationData> groupedNotifications = new HashMap<String, NotificationData>();

    // "singleton" like declaration
    private static NotificationsService instance;

    public static NotificationsService getSharedInstance()
    {
        return instance;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        Log.d(TAG, "NotificationsService:onCreate");
        instance = this;

        // create a notification parser
        context = getApplicationContext();
        parser = new NotificationParser(getApplicationContext());
        mFilteredNotificationsList = new ArrayList<NotificationData>();

        // set up events listener
        setNotificationEventListener(new NotificationEventsAdapter(context, new Handler()));

        // set up proximity sensor
        testProximity();

        // set up notifications list
        NiLSFPCreate();

        // notify world that the service is ready
        context.sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_READY));
    }



    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.d(TAG, "NotificationsService:onUnbind");
        saveLog(getApplicationContext(), true);

        if (listener != null)
            listener.onServiceStopped();

        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG,"NotificationsService:onDestroy");
        instance = null;

        // save the recent log into a file
        saveLog(getApplicationContext(),true);

        // notify world that NiLS service has stopped
        getApplicationContext().sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_DIED));
        Log.w(TAG, "NiLS FP Service was killed. hiding notifications list.");

        stopProximityMonitoring();

        // cleanup view manager
        if (viewManager != null)
        {
            viewManager.destroy();
            viewManager = null;
            mNotifications = null;
            mViewManagerCallbacks = null;
        }

        // cleanup broadcast receiver
        if (npreceiver != null)
        {
            unregisterReceiver(npreceiver);
            npreceiver = null;
        }

        super.onDestroy();
    }

    private static void saveLog(Context context, boolean silent)
    {
        // save crash log
        Log.d(TAG, "Something happened. saving log file...");
        try
        {
            // read logcat
            Time now = new Time();
            now.setToNow();
            String filename = context.getExternalFilesDir(null) + "/" + now.format("%Y-%m-%dT%H:%M:%S")+".log";
            Runtime.getRuntime().exec("logcat -d -v time -f " + filename);

            Log.d(TAG, "Log file written to "+ filename);

            if (!silent)
            {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                        .setLargeIcon(((BitmapDrawable) context.getResources().getDrawable(R.drawable.ic_launcher)).getBitmap())
                        .setSmallIcon(R.drawable.ic_nils_icon_mono)
                        .setContentTitle("Something went wrong...")
                        .setContentText("log file was written to " + filename);

                NotificationCompat.BigTextStyle bigtextstyle = new NotificationCompat.BigTextStyle();
                bigtextstyle.setBigContentTitle("Something went wrong...");
                bigtextstyle.bigText("log file was written to " + filename);
                mBuilder.setStyle(bigtextstyle);

                Notification n = mBuilder.build();

                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse("file://" + filename);
                intent.setDataAndType(uri, "text/plain");

                n.contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(0, n);
            }
        }
        catch (IOException e)
        {}
    }

    //** Proximity Sensor Monitoring **//
    private Boolean mCovered = null;

    private void testProximity()
    {
        Log.d(TAG, "Testing proximity sensor...");

        startProximityMonitoring();

        // test after 100ms if the proximity sensor status was changed
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (mCovered != null) {
                    Log.d(TAG, "immediate response");
                    prefs.edit().putBoolean(SettingsManager.IMMEDIATE_PROXIMITY, true).commit();
                    // stop proximity monitoring - there is no need to if the proximity is immediate
                    stopProximityMonitoring();
                }
                else {
                    Log.d(TAG, "no response after 500ms");
                    prefs.edit().putBoolean(SettingsManager.IMMEDIATE_PROXIMITY, false).commit();
                }

                // stop proximity monitoring if wakeup mode is always or never
                String wakeupMode = SettingsManager.getWakeupMode(context, null);
                if (wakeupMode.equals(SettingsManager.WAKEUP_ALWAYS) || wakeupMode.equals(SettingsManager.WAKEUP_NEVER))
                    stopProximityMonitoring();
            }
        },500);
    }

    SensorManager sensorManager;
    SensorEventListener sensorListener;

    public void startProximityMonitoring()
    {
        Log.d(TAG, "Starting monitoring proximity sensor constantly");
        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                mCovered = (event.values[0] < event.sensor.getMaximumRange());
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sensorManager.registerListener(sensorListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopProximityMonitoring()
    {
        Log.d(TAG, "Stopping monitoring proximity sensor");
        if (sensorListener != null && sensorManager != null) {
            sensorManager.unregisterListener(sensorListener);
            mCovered = null;
            sensorListener = null;
        }
    }

    //** End of Proximity Sensor Monitoring **/



    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (context == null) context = getApplicationContext();
        if (parser == null) parser = new NotificationParser(getApplicationContext());
        if (listener == null) setNotificationEventListener(new NotificationEventsAdapter(context, mHandler));

        if (intent != null && intent.getAction() != null) {
            Log.d(TAG, "onStartCommand with action:"+intent.getAction());
            if (intent.getAction().equals("refresh")) {
                updateViewManager(intent.getIntExtra("uid", -1));
            } else if (intent.getAction().equals("dismiss")) {
                String packageName = intent.getStringExtra("package");
                String tag = intent.getStringExtra("tag");
                int id = intent.getIntExtra("id", -1);
                removeNotification(packageName, id, null, true);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void refreshLayout(boolean recreate)
    {
        viewManager.refreshLayout(recreate);
    }

    //** Notifications Add/Remove Handling **/
    private void addNotification(NotificationData nd)
    {
        addNotification(nd, true);
    }

    private void addNotification(NotificationData nd, boolean refresh)
    {
        if (viewManager != null && refresh) viewManager.saveNotificationsState();
        if (nd != null)
        {
            Log.d(TAG,"NotificationsService:addNotification " + nd.packageName + ":" + nd.id);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String notificationMode = SettingsManager.getNotificationMode(getApplicationContext(), nd.packageName);
            boolean updated = false;

            // remove old notification
            final Lock w = lock.writeLock();
            w.lock();

            boolean changed = false;
            boolean ignoreNotification = false;

            try
            {
                Iterator<NotificationData> iter = mNotifications.iterator();

                while (iter.hasNext())
                {
                    NotificationData oldnd = iter.next();

                    // remove only if one of the following scenarios:
                    // 1. notification mode is "grouped" and the notification has the same package (and same id on 4.3+)
                    // 2. notification mode is "separated" and the notification is similar to the old one
                    if (oldnd.packageName.equals(nd.packageName) &&
                        (((oldnd.id == nd.id && (oldnd.tag == null && nd.tag == null || oldnd.tag != null && nd.tag != null && oldnd.tag.equals(nd.tag)))
                         || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) && (/*!nd.event || */notificationMode.equals(SettingsManager.MODE_GROUPED))) ||
                          oldnd.isSimilar(nd, true)) {
                        nd.uid = oldnd.uid;

                        // if the notification is sideloaded, use the original id and tag
                        if (nd.sideLoaded) {
                            nd.id = oldnd.id;
                            nd.tag = oldnd.tag;
                        }

                        if (oldnd.isDeleted())
                        {
                            Log.d(TAG, "notification " + nd.packageName + ":" + nd.id + "#" + nd.uid + " was already dismissed previously, marking this new one as deleted");
                            nd.delete();
                        }

                        // protect it from being cleared on next purge command
                        nd.protect = true;

                        iter.remove();
                        oldnd.cleanup();
                        updated = true;
                        changed = !oldnd.isEqual(nd);

                        // if it is exact the same notification - keep old received time
                        if (!changed) nd.received = oldnd.received;

                        break;
                    } else // check if the old notification is a duplicate of the current but contains more data than the current - if so - ignore the new one
                    {
                        if (nd.isSimilar(oldnd, false))
                        {
                            ignoreNotification = true;
                            updated = false;

                            // if the sideloaded notification doesn't have an icon - use this notification icon
                            if (oldnd.sideLoaded) {
                                if (oldnd.largeIcon == null) {
                                    oldnd.largeIcon = nd.largeIcon;
                                    oldnd.icon = nd.icon;
                                }
                                // copy id and tag if from the original notification
                                oldnd.id = nd.id;
                                oldnd.tag = nd.tag;
                            }
                        }

                        // protect the old one from being cleared on next purge command
                        oldnd.protect = true;
                    }
                }

                if (!ignoreNotification)
                {
                    // add the new notification
                    mNotifications.add(nd);
                }
            }
            finally
            {
                w.unlock();
            }

            // notify that the notification was added
            if (!ignoreNotification && !nd.isDeleted())
            {
                if (listener != null)
                if (updated)
                    listener.onNotificationUpdated(nd, changed, mCovered);
                else
                    listener.onNotificationAdded(nd, true, mCovered);

                notifyAndroidWear(nd);

                if (refresh)
                    callRefresh();
            }
        }
    }

    private void addPersistentNotification(PersistentNotification pn)
    {
        if (pn != null)
        {
            Log.d(TAG,"NotificationsService:addPersistentNotification " + pn.packageName);

            persistentNotifications.put(pn.packageName, pn);
            if (listener != null) listener.onPersistentNotificationAdded(pn);

            //if (pn.packageName.equals("com.android.dialer") || pn.packageName.equals("com.google.android.dialer"))
            //    context.sendBroadcast(new Intent(INCOMING_CALL));
        }
    }

    private void removeNotification(String packageName, int id, String tag, boolean logical)
    {
        Log.d(TAG,"NotificationsService:removeNotification  " + packageName + ":" + id);
        boolean sync = SettingsManager.shouldClearWhenClearedFromNotificationsBar(getApplicationContext());
        if (sync)
        {
            boolean cleared = false;
            boolean isGrouped = groupedNotifications.containsKey(packageName) &&
                                groupedNotifications.get(packageName).id == id &&
                    (groupedNotifications.get(packageName).tag == null && tag == null ||
                     groupedNotifications.get(packageName).tag != null && tag != null && groupedNotifications.get(packageName).tag.equals(tag));

            ArrayList<NotificationData> clearedNotifications = new ArrayList<NotificationData>();

            // find the notification and remove it
            Lock w = lock.writeLock();
            w.lock();
            try {
                Iterator<NotificationData> iter = mNotifications.iterator();

                while (iter.hasNext()) {
                    NotificationData nd = iter.next();

                    if (nd.packageName.equals(packageName) &&
                            (isGrouped ||
                                (nd.id == id && (nd.tag == null && tag == null ||
                                                 nd.tag != null && tag != null && nd.tag.equals(tag)))) && !nd.pinned) {
                        // mark as delete if it's part of multiple events notification
                        if (logical && (nd.event || isGrouped)) {
                            // mark notification as cleared
                            if (!nd.isDeleted())
                            {
                                nd.delete();
                                cleared = true;

                                // notify that the notification was cleared
                                clearedNotifications.add(nd);
                            }
                        } else if (!nd.isDeleted()) // make sure it hasn't been deleted previously by the user
                        {
                            // immediately remove notification
                            iter.remove();
                            cleared = true;

                            // notify that the notification was cleared
                            clearedNotifications.add(nd);
                        }

                        // do not stop loop - keep looping to clear all of the notifications with the same id
                    }
                }
            }
            finally
            {
                w.unlock();
            }

            // notify listener for cleared notifications
            if (cleared)
            {
                if (viewManager != null) viewManager.saveNotificationsState();
                if (listener != null)
                {
                    for (NotificationData nd : clearedNotifications)
                    {
                        listener.onNotificationCleared(nd, false);
                        notifyAndroidWearDismissed(nd);
                    }
                }

                callRefresh();
            }
        }
    }

    private void callRefresh()
    {
        sendBroadcast(new Intent(REFRESH_LIST));
    }

    private void callUpdateViewManager(int uid)
    {
        Intent refreshListIntent = new Intent(context, NotificationsService.class);
        refreshListIntent.setAction("refresh");
        refreshListIntent.putExtra("uid", uid);
        startService(refreshListIntent);
    }

    @SuppressWarnings("UnusedParameters")
    private void removePersistentNotification(String packageName, int id)
    {
        Log.d(TAG,"NotificationsService:removePersistentNotification "+packageName);
        if (persistentNotifications.containsKey(packageName))
        {
            PersistentNotification pn = persistentNotifications.get(packageName);
            persistentNotifications.remove(packageName);
            if (listener != null)
            {
                listener.onPersistentNotificationCleared(pn);
            }
        }
    }

    @Override
    public synchronized List<NotificationData> getNotifications()
    {
        return mFilteredNotificationsList;
    }

    public synchronized void updateNotificationsList()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // re-create filtered notifications list
        if (context != null)
        {
            mFilteredNotificationsList = new ArrayList<NotificationData>();
            String sortBy = prefs.getString(SettingsManager.NOTIFICATIONS_ORDER, "time");
            Object[] arr;

            Lock r = lock.readLock();
            r.lock();
            try
            {
                arr = mNotifications.toArray();
            }
            finally
            {
                r.unlock();
            }

            if (arr != null)
            {
                for (Object obj : arr)
                {
                    NotificationData nd = (NotificationData) obj;
                    if (!nd.isDeleted())
                        mFilteredNotificationsList.add(nd);
                }
                sortNotificationsList(mFilteredNotificationsList, sortBy);
            }

            // notify that the list was changed
            updateViewManager(-1);
        }
    }

    @Override
    public HashMap<String, PersistentNotification> getPersistentNotifications()
    {
        return persistentNotifications;
    }

    private void sortNotificationsList(List<NotificationData> notifications, String sortBy)
    {
        if (notifications.size() > 0)
        {
                if (sortBy.equals("priority"))
                {
                    // sort by priority
                    Collections.sort(notifications, new Comparator<NotificationData>()
                    {
                        @Override
                        public int compare(NotificationData n1, NotificationData n2)
                        {
                            if (n1 == null || n2 == null) return 0;
                            if (n1.priority < n2.priority)
                                return 1;
                            if (n1.priority > n2.priority)
                                return -1;
                            // if we reached here, the priorities are equal - sort by time
                            if (n1.received < n2.received)
                                return 1;
                            if (n1.received > n2.received)
                                return -1;

                            // if we reached here, the time is equal - sort by group order
                            if (n1.groupOrder != null && n2.groupOrder != null)
                                return n1.groupOrder.compareTo(n2.groupOrder);
                            else
                                return 0;
                        }
                    });
                }
                else if (sortBy.equals("timeasc"))
                {
                    // sort by time
                    Collections.sort(notifications, new Comparator<NotificationData>()
                    {
                        @Override
                        public int compare(NotificationData n1, NotificationData n2)
                        {
                            if (n1.received > n2.received)
                                return 1;
                            if (n1.received < n2.received)
                                return -1;
                            // if we reached here, the time is equal - sort by group order
                            if (n1.groupOrder != null && n2.groupOrder != null)
                                return n1.groupOrder.compareTo(n2.groupOrder);
                            else
                                return 0;
                        }
                    });
                }
                else if (sortBy.equals("time"))
                {
                    // sort by time
                    Collections.sort(notifications, new Comparator<NotificationData>()
                    {
                        @Override
                        public int compare(NotificationData n1, NotificationData n2)
                        {
                            if (n1.received < n2.received)
                                return 1;
                            if (n1.received > n2.received)
                                return -1;
                            // if we reached here, the time is equal - sort by group order
                            if (n1.groupOrder != null && n2.groupOrder != null)
                                return n1.groupOrder.compareTo(n2.groupOrder);
                            else
                                return 0;
                        }
                    });
                }
        }
    }

    @Override
    public void clearAllNotifications()
    {
        Log.d(TAG,"NotificationsService:clearAllNotifications");
        if (viewManager != null) viewManager.saveNotificationsState();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean syncback = prefs.getBoolean(SettingsManager.SYNC_BACK, SettingsManager.DEFAULT_SYNC_BACK);

        ArrayList<NotificationData> clearedNotifications = new ArrayList<NotificationData>();

        Lock w = lock.writeLock();
        w.lock();
        try
        {
            Iterator<NotificationData> i = mNotifications.iterator();
            while (i.hasNext())
            {
                NotificationData nd = i.next();
                if (!nd.pinned)
                {
                    clearedNotifications.add(nd);

                    // if its event - mark it as deleted
                    if (nd.event || nd.sideLoaded && nd.group != null)
                        nd.delete();
                    else // otherwise remove it immediately
                        i.remove();
                }
            }
        }
        finally
        {
            w.unlock();
        }

        for(NotificationData nd : clearedNotifications)
        {
            // notify android to clear it too
            if (syncback)
                try
                {
                    cancelNotification(nd.packageName, nd.tag, nd.id);

                    // cancel also the grouped notifications for this app if it has any
                    if (groupedNotifications.containsKey(nd.packageName))
                    {
                        NotificationData groupedNd = groupedNotifications.get(nd.packageName);
                        cancelNotification(groupedNd.packageName, groupedNd.tag, groupedNd.id);
                        groupedNotifications.remove(nd.packageName);
                    }
                }
                catch (Exception exp)
                {
                    exp.printStackTrace();
                }
            if (listener != null) listener.onNotificationCleared(nd, false);
            notifyAndroidWearDismissed(nd);

        }

        callRefresh();
    }

    private void cancelNotification(String packageName, String tag, int id)
    {
        Log.d(TAG,"NotificationsService:cancelNotification " + packageName + ":" + id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            try
            {
                Intent intent = new Intent(context, NotificationsListener.class);
                intent.setAction(CANCEL_NOTIFICATION);
                intent.putExtra(EXTRA_PACKAGENAME, packageName);
                intent.putExtra(EXTRA_TAG, tag);
                intent.putExtra(EXTRA_ID, id);
                startService(intent);
            }
            catch (Exception exp)
            {
                Log.wtf(TAG, "sdk_int:"+Build.VERSION.SDK_INT+" but NotificationsListener class doesn't exists... weird.");
            }
        }
    }

    @Override
    public synchronized void clearNotificationsForApps(String[] packages)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean syncback = prefs.getBoolean(SettingsManager.SYNC_BACK, SettingsManager.DEFAULT_SYNC_BACK);
        boolean changed = false;
        ArrayList<NotificationData> clearedNotifications = new ArrayList<NotificationData>();

        if (viewManager != null) viewManager.saveNotificationsState();

        for(String packageName : packages)
        {
            Log.d(TAG,"NotificationsService:clearNotificationsForApps " + packageName);
            Lock w = lock.writeLock();
            w.lock();
            try
            {
                ListIterator<NotificationData> i = mNotifications.listIterator();
                while (i.hasNext())
                {
                    NotificationData nd = i.next();
                    if (!nd.pinned && nd.packageName.equals(packageName))
                    {
                        // mark notification as deleted
                        if (nd.event || nd.sideLoaded && nd.group != null)
                            nd.delete();
                        else
                            i.remove();

                        changed = true;
                        clearedNotifications.add(nd);
                    }
                }
            }
            finally
            {
                w.unlock();
            }
        }
        // notify listener for cleared notifications
        if (changed)
        {
            for(NotificationData nd : clearedNotifications)
            {
                if (syncback)
                    try
                    {
                        cancelNotification(nd.packageName, nd.tag, nd.id);
                    }
                    catch (Exception exp)
                    {
                        exp.printStackTrace();
                    }
                if (listener != null) listener.onNotificationCleared(nd, false);
                notifyAndroidWearDismissed(nd);
            }

            callRefresh();
        }
    }

    @Override
    public synchronized void clearNotification(int uid)
    {
        Log.d(TAG,"NotificationsService:clearNotification uid:" + uid);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean syncback = prefs.getBoolean(SettingsManager.SYNC_BACK, SettingsManager.DEFAULT_SYNC_BACK);

        // first, find it on list
        boolean removed = false;
        NotificationData removedNd = null;

        Lock w = lock.writeLock();
        w.lock();
        try
        {
            Iterator<NotificationData> iter = mNotifications.iterator();

            while (iter.hasNext() && !removed)
            {
                NotificationData nd = iter.next();
                if (nd.uid == uid)
                {
                    // store id and package name to search for more notifications with the same id
                    removedNd = nd;

                    // mark notification as deleted
                    if (nd.event || nd.sideLoaded && nd.group != null)
                        nd.delete();
                    else
                        iter.remove();

                    removed = true;
                }
            }
        }
        finally
        {
            w.unlock();
        }

        if (removed)
        {
            if (viewManager != null) viewManager.saveNotificationsState();

            // search for more notification with the same id - if not found - dismiss the notification from android status bar
            boolean more = false;
            boolean hasGroup = groupedNotifications.containsKey(removedNd.packageName);

            Lock r = lock.readLock();
            r.lock();
            try
            {
                for (NotificationData nd : mNotifications)
                {
                    if ((hasGroup || nd.id == removedNd.id) && !nd.isDeleted() &&
                         nd.packageName.equals(removedNd.packageName))
                         more = true;
                }
            }
            finally
            {
                r.unlock();
            }

            if (syncback && !more)
                try
                {
                    Log.d(TAG, "no more notifications with the same id(+"+removedNd.id+"), removing it also from status bar. hasGroup:"+hasGroup);
                    cancelNotification(removedNd.packageName, removedNd.tag, removedNd.id);

                    // cancel also the grouped notifications for this app if it has any
                    if (groupedNotifications.containsKey(removedNd.packageName))
                    {
                        NotificationData groupedNd = groupedNotifications.get(removedNd.packageName);
                        cancelNotification(groupedNd.packageName, groupedNd.tag, groupedNd.id);
                        groupedNotifications.remove(removedNd.packageName);
                    }
                }
                catch (Exception exp)
                {
                    exp.printStackTrace();
                }

            if (listener != null)
            {
                listener.onNotificationCleared(removedNd, more);
                notifyAndroidWearDismissed(removedNd);
            }

            callRefresh();
        }
        else
        {
            Log.d(TAG, "NotificationsService:clearNotification - wasn't found");
        }
    }

    private void updateViewManager(int uid)
    {
        if (viewManager != null) {
            viewManager.notifyDataChanged(uid);
        }
    }

    public void onNotificationPosted(Notification n, String packageName, int id, String tag, boolean sideLoaded)
    {
        Log.d(TAG, "onNotificationPosted package:"+packageName+" id:"+id+" tag:"+tag);
        try {
            if (!parser.isPersistent(n, packageName)) {
                List<NotificationData> notifications = parser.parseNotification(n, packageName, id, tag, sideLoaded);
                if (viewManager != null) viewManager.saveNotificationsState();
                unprotectNotifications(packageName);
                for (NotificationData nd : notifications) {
                    addNotification(nd, false);
                }

                // after adding all of the new notifications delete all of the old ones that marked as deleted
                purgeDeletedNotifications(packageName, id);
                callRefresh();
            } else {
                PersistentNotification pn = parser.parsePersistentNotification(n, packageName, id);
                addPersistentNotification(pn);
            }
        }
        catch(Exception exp)
        {
            Log.e(TAG, "NotificationsService:onNotificationPosted: an exception has occured");
            exp.printStackTrace();
            // make sure this isn't NiLS own crash notification
            if (!(packageName != null && packageName.equals(context.getPackageName())))
                saveLog(context, false);
        }
    }

    private HashMap<String, Integer> wearNotificationIds = new HashMap<String, Integer>();

    private void notifyAndroidWearDismissed(NotificationData nd) {
        // re-transmit notification (if needed)
        if (SettingsManager.getBoolean(context, nd.packageName, AppSettingsActivity.RETRANSMIT, AppSettingsActivity.DEFAULT_RETRANSMIT)) {
            // Issue the notification
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(this);
            notificationManager.cancel(nd.uid);
        }
    }

    private void notifyAndroidWear(NotificationData nd)
    {
        // re-transmit notification (if needed)
        if (SettingsManager.getBoolean(context, nd.packageName, AppSettingsActivity.RETRANSMIT, AppSettingsActivity.DEFAULT_RETRANSMIT))
        {
            NotificationCompat.WearableExtender wearableExtender =
                    new NotificationCompat.WearableExtender()
                            .setHintHideIcon(true);

            // Build the notification, setting the group appropriately
            Intent clearNotificationIntent = new Intent(context, NotificationsService.class);
            clearNotificationIntent.setAction("dismiss");
            Log.d(TAG, "posting to wearable - package:"+nd.packageName+" id:"+nd.uid);
            clearNotificationIntent.putExtra("id", nd.id);
            clearNotificationIntent.putExtra("package", nd.packageName);
            clearNotificationIntent.putExtra("tag", nd.tag);
            PendingIntent clearNotificationPI = PendingIntent.getService(context, 0, clearNotificationIntent, 0);
            Notification notif = new NotificationCompat.Builder(context)
                    .setContentTitle(nd.title)
                    .setContentText(nd.text)
                    .setLargeIcon(nd.largeIcon!=null?nd.largeIcon:nd.icon)
                    .setSmallIcon(android.R.color.transparent)
                    .setContentIntent(nd.action)
                    .setDeleteIntent(clearNotificationPI)
                    .setGroup(nd.packageName)
                    .extend(wearableExtender)
                    .build();

            // Issue the notification
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(this);
            notificationManager.notify(nd.uid, notif);
        }
    }

    private void unprotectNotifications(String packageName)
    {
        Lock w = lock.writeLock();
        w.lock();
        try
        {
            for (NotificationData nd : mNotifications)
            {
                if (nd.packageName.equals(packageName))
                    nd.protect = false;
            }
        }
        finally {
            w.unlock();
        }
    }

    private void purgeDeletedNotifications(String packageName, int id)
    {
        Log.d(TAG,"purging deleted mNotifications "+ packageName + ":" + id);

        Lock w = lock.writeLock();
        w.lock();
        try
        {
            Iterator<NotificationData> iter = mNotifications.iterator();
            while (iter.hasNext())
            {
                NotificationData nd = iter.next();
                if (nd.packageName.equals(packageName) &&
                        nd.isDeleted() &&
                        !nd.protect &&
                        (nd.id == id || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2))
                {
                    Log.d(TAG, "permanently removing uid:" + nd.uid);
                    iter.remove();
                }
            }
        }
        finally
        {
            w.unlock();
        }
    }

    public void onNotificationRemoved(Notification n, String packageName, int id, String tag)
    {
        try
        {
            removeNotification(packageName, id, tag, true);

            // remove also persistent notification
            if (n != null && parser.isPersistent(n, packageName)) {
                removePersistentNotification(packageName, id);
            }
        }
        catch(Exception exp)
        {
            Log.e(TAG, "NotificationsService:onNotificationRemoved: an exception has occured");
            exp.printStackTrace();
            saveLog(context, false);
        }
    }

    //** End of Notifications Add/Remove Handling **/

    @Override
    public void setNotificationEventListener(NotificationEventListener listener)
    {
        this.listener = listener;
    }

    @Override
    public NotificationEventListener getNotificationEventListener()
    {
        return listener;
    }

    public NotificationData findNotificationByUid(int uid) {
        Lock w = lock.readLock();
        w.lock();
        boolean found = false;
        NotificationData foundNd = null;
        try
        {
            Iterator<NotificationData> iter = mNotifications.iterator();

            while (iter.hasNext() && !found)
            {
                NotificationData nd = iter.next();
                if (nd.uid == uid)
                {
                    // store id and package name to search for more notifications with the same id
                    foundNd = nd;
                    found = true;
                }
            }
        }
        finally
        {
            w.unlock();
        }
        return foundNd;
    }

    // binding stuff
    //***************
    private final IBinder mBinder = new LocalBinder();


    public class LocalBinder extends Binder
    {
        NotificationsService getService()
        {
            // Return this instance of LocalService so clients can call public methods
            return NotificationsService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // bound to Accessibility / Notifications service
        if (listener != null)
            listener.onServiceStarted();
        return mBinder;
    }

    private class NPReceiver extends BroadcastReceiver
    {
        private PendingIntent checkLockScreenPendingIntent = null;

        public void onReceive(Context context, Intent intent)
        {
            try
            {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                final PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

                String lockScreenApp = prefs.getString(SettingsManager.LOCKSCREEN_APP, STOCK_LOCKSCREEN_PACKAGENAME);

                if(intent.getAction().equals(REFRESH_LIST))
                {
                    updateNotificationsList();
                    if (listener != null) listener.onNotificationsListChanged();
                }
                else if(intent.getAction().equals(Intent.ACTION_USER_PRESENT) && lockScreenApp.equals(STOCK_LOCKSCREEN_PACKAGENAME) ||
                        intent.getAction().equals(WIDGET_LOCKER_UNLOCKED) ||
                        intent.getAction().equals(WIDGET_LOCKER_HIDE) ||
                        intent.getAction().equals(GO_LOCKER_UNLOCKED) ||
                        intent.getAction().equals(DEVICE_UNLOCKED))
                {
                    // restore original device timeout
                    mSysUtils.restoreDeviceTimeout();
                    Log.d(TAG,intent.getAction());
                    hide(false);

                    // clear all notifications if needed
                    if (SettingsManager.shouldClearOnUnlock(context))
                    {
                        clearAllNotifications();
                    }

                    // keep the screen on for the device default timeout
                    if (pm.isScreenOn()) mSysUtils.turnScreenOn(true, true, "device unlocked");
                }
                else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
                {
                    // store current device timeout settings
                    mSysUtils.setDeviceTimeout();

                    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                    if (checkLockScreenPendingIntent != null)
                    {
                        Log.d(TAG, "screen is off, stop monitoring for foreground app");
                        am.cancel(checkLockScreenPendingIntent);
                        checkLockScreenPendingIntent = null;
                    }
                }
                else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
                {
                    boolean accessibilityServiceIsActive = NiLSAccessibilityService.isServiceRunning(context);
                    Log.d(TAG, "ACTION_SCREEN_ON - auto detecting lock screen app");

                    // if the accessibility service is not running start monitoring the active app
                    if (!accessibilityServiceIsActive) {
                        // detect current lock screen
                        detectLockScreenApp(context);

                        // show / hide notifications list
                        if (shouldHideNotifications(false)) {
                            hide(false);
                            // send a broadcast the device is unlocked and hide notifications list immediately
                            Log.d(TAG, "accessibility service is not active and shouldHideNotifications returned true - sending unlocked event");
                            sendBroadcast(new Intent(DEVICE_UNLOCKED));
                        } else {
                            viewManager.refreshLayout(false);
                            show(true);

                            // start polling current app to see if the device is unlocked
                            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                            checkLockScreenPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(CHECK_LSAPP), PendingIntent.FLAG_UPDATE_CURRENT);
                            am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 500, 500, checkLockScreenPendingIntent);

                            // make sure screen will stay on as needed seconds as defined on settings
                            if (pm.isScreenOn()) mSysUtils.turnScreenOn(true, "screen was manually turned on, keep it on");
                        }
                    }
                    // if the accessibility service is not running
                    else {
                        // check if the last detected package name is not the lock screen app
                        String lastPackage = prefs.getString(NiLSAccessibilityService.LAST_OPENED_WINDOW_PACKAGENAME, SettingsManager.STOCK_LOCKSCREEN_PACKAGENAME);
                        detectLockScreenApp(context, lastPackage);

                        if (shouldHideNotifications(context, lastPackage, false)) {
                            // if it is not the lock screen app - call "unlock" method
                            Log.d(TAG, "accessibility service is active and shouldHideNotifications returned true - sending unlocked event, lastPackage is:"+lastPackage);
                            sendBroadcast(new Intent(DEVICE_UNLOCKED));
                        } else {
                            // show notifications when the screen is turned on and the lock screen is displayed
                            viewManager.refreshLayout(false);
                            show(true);

                            // make sure screen will stay on as needed seconds as defined on settings
                            mSysUtils.turnScreenOn(true, "screen was turned on manually, keep it on");
                        }
                    }
                }
                else if (intent.getAction().equals(CHECK_LSAPP))
                {
                    // check if another (non-lock screen) app was launched
                    if (shouldHideNotifications(false))
                    {
                        // hide notifications list
                        hide(false);

                        // stop checking
                        if (checkLockScreenPendingIntent != null) {
                            Log.d(TAG, "lock screen is no longer active, stop monitoring");
                            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                            am.cancel(checkLockScreenPendingIntent);
                            checkLockScreenPendingIntent = null;
                        }

                        // send a broadcast the device is unlocked and hide notifications list immediately
                        Log.d(TAG, "constant polling - shouldHideNotifications returned true - sending unlocked event");
                        sendBroadcast(new Intent(DEVICE_UNLOCKED));
                    }
                }
            }
            catch(Exception exp)
            {
                exp.printStackTrace();
                saveLog(context, false);
            }
        }
    }

    private static void detectLockScreenApp(Context context)
    {
        String currentApp = SysUtils.getForegroundApp(context);
        detectLockScreenApp(context, currentApp);
    }

    private static void detectLockScreenApp(Context context, final String currentApp)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(SettingsManager.AUTO_DETECT_LOCKSCREEN_APP, SettingsManager.AUTO_DETECT_LOCKSCREEN_APP_DEFAULT )) {
            String lockScreenApp = prefs.getString(SettingsManager.LOCKSCREEN_APP, STOCK_LOCKSCREEN_PACKAGENAME);
            PackageManager pm = context.getPackageManager();

            List<String> blacklistPackageNames = Arrays.asList(SettingsManager.BLACKLIST_PACKAGENAMES);

            // check if the current app is a lock screen app
            if (pm.checkPermission(android.Manifest.permission.DISABLE_KEYGUARD, currentApp) == PackageManager.PERMISSION_GRANTED) {
                if (!lockScreenApp.equals(currentApp) && !blacklistPackageNames.contains(currentApp)) {
                    // store current app as the lock screen app until next time
                    Log.d(TAG, "new lock screen app detected: " + currentApp);

                    popupLockScreenChangedDialog(context, currentApp);
                }
                else hidePopups();
            } else // when the device is secured - then the stock lock screen is currently used
                if (SysUtils.isKeyguardLocked(context)) {
                    if (!lockScreenApp.equals(STOCK_LOCKSCREEN_PACKAGENAME)) {
                        // store current app as the lock screen app until next time
                        Log.d(TAG, "stock lock screen app detected");

                        popupLockScreenChangedDialog(context, STOCK_LOCKSCREEN_PACKAGENAME);
                    }
                }
                else hidePopups();
        }
    }

    private static PopupDialog pd = null;
    private static PopupDialog pd2 = null;

    private static void hidePopups()
    {
        if (pd != null && pd.isVisible())
            pd.hide();
        if (pd2 != null && pd2.isVisible())
            pd2.hide();
    }

    private static void popupLockScreenChangedDialog(final Context context, final String currentApp)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (pd == null) pd = PopupDialog.create(context);
        if (pd2 != null) pd2.hide();
        pd.setTitle(context.getString(R.string.new_lock_screen_detected, SettingsManager.PrefsGeneralFragment.getAppName(context, currentApp)))
          .setText(context.getString(R.string.new_lock_screen_detected_text, SettingsManager.PrefsGeneralFragment.getCurrentLockScreenAppName(context)))
          .setPositiveButton(context.getString(R.string.yes), new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  prefs.edit().putString(SettingsManager.LOCKSCREEN_APP, currentApp).commit();
                  if (NotificationsService.getSharedInstance() != null)
                      NotificationsService.getSharedInstance().show(false);
                  pd.hide();

                  // if user didn't request specifically to auto detect, stop detecting after the first "Yes" answer
                  if (!prefs.getBoolean("user_defined_auto_detect", false))
                    prefs.edit().putBoolean(SettingsManager.AUTO_DETECT_LOCKSCREEN_APP, false).commit();
              }
          })
                .setNegativeButton(context.getString(R.string.no), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int n = prefs.getInt(SettingsManager.NUMBER_OF_LS_DETECT_REFUSES, 0);
                        n++;
                        if (n == 2) {
                            if (pd2 == null) pd2 = PopupDialog.create(context);
                            pd2.setTitle(context.getString(R.string.never_suggest_title))
                                    .setText(context.getString(R.string.never_suggest_lockscreen_apps))
                                    .setPositiveButton(context.getString(R.string.yes), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            prefs.edit().putBoolean(SettingsManager.AUTO_DETECT_LOCKSCREEN_APP, true).commit();
                                            prefs.edit().putBoolean("user_defined_auto_detect", true).commit();
                                            pd2.hide();
                                        }
                                    })
                                    .setNegativeButton(context.getString(R.string.no), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            prefs.edit().putBoolean(SettingsManager.AUTO_DETECT_LOCKSCREEN_APP, false).commit();
                                            pd.hide();
                                        }
                                    })
                                    .show();
                            n = 0;
                        }
                        prefs.edit().putInt(SettingsManager.NUMBER_OF_LS_DETECT_REFUSES, n).commit();
                        pd.hide();
                    }
                })
                .show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        viewManager.refreshLayout(false);
    }

    private boolean shouldHideNotifications(boolean autoDetect)
    {
        return shouldHideNotifications(getApplicationContext(), autoDetect);
    }

    public static boolean shouldHideNotifications(Context context, boolean autoDetect)
    {
        String currentApp = SysUtils.getForegroundApp(context);
        return shouldHideNotifications(context, currentApp, autoDetect);
    }

    public static boolean shouldHideNotifications(Context context, String currentApp, boolean autoDetect)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // get the current lock screen app (if set)
        String lockScreenApp = prefs.getString(SettingsManager.LOCKSCREEN_APP, STOCK_LOCKSCREEN_PACKAGENAME );
        boolean shouldHide = true;

        // if the current app is one of the allowed apps to be on top of the lock screen, hide NiLS
        List<String> blacklistPackageNames = Arrays.asList(SettingsManager.BLACKLIST_PACKAGENAMES);
        if (NiLSAccessibilityService.isServiceRunning(context) && blacklistPackageNames.contains(currentApp))
        {
            return true;
        }


        String activity =  SysUtils.getForegroundActivity(context);
        if (activity.contains("InCallActivity") ||          // never show it on top of an incoming call
            activity.contains("ScreensaverActivity") ||     // never show it on top of daydream
            activity.contains("PopupNotificationLocked") || // never show it on top of WhatsApp popup
            activity.contains("AlarmActivity")      ||      // never show it on top of AlarmClock
            activity.contains("com.google.android.velvet.ui.VelvetLockscreenActivity") || // never show it on top of Google Now search
            activity.contains("com.sec.android.app.camera.Camera")) // never show it on top of the camera app
            return true;

        if (autoDetect)
        {
            detectLockScreenApp(context);
        }

        // check if the device is secured or the current app is the lock screen app
        if (lockScreenApp.equals(STOCK_LOCKSCREEN_PACKAGENAME ) && SysUtils.isKeyguardLocked(context) || lockScreenApp.equals(currentApp))
            shouldHide = false;

        return shouldHide;
    }

    public void NiLSFPCreate()
    {
        Log.d("NiLS+", "NPService.onCreate()");
        super.onCreate();

        mNotifications = new ArrayList<NotificationData>();
        mViewManagerCallbacks = new NPViewManager.Callbacks()
        {
            @Override
            public void onDismissed(NotificationData ni)
            {
                clearNotification(ni.getUid());
            }

            @Override
            public void onOpen(NotificationData ni)
            {
                viewManager.hide(false);

                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                boolean halo = prefs.getBoolean(SettingsManager.HALO_MODE, SettingsManager.DEFAULT_HALO_MODE);

                if (halo)
                {
                    openNotificationOnHalo(ni);
                }
                else
                {
                    runPendingIntent(ni.action, ni.packageName, ni.uid, null);
                }
            }

            @Override
            public void onAction(NotificationData ni, int actionPos)
            {
                NotificationData.Action action = ni.actions[actionPos];

                if (action.remoteInputs != null)
                {
                    // open quick reply activity
                    Intent intent = new Intent(context, QuickReplyActivity.class);

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("uid", ni.uid);
                    intent.putExtra("actionPos", actionPos);
                    startActivity(intent);
                }
                else {
                    // show the name of the action
                    Toast.makeText(getApplicationContext(), action.title, Toast.LENGTH_SHORT).show();

                    if (!isActivity(action.actionIntent)) {
                        // open it in background
                        try {
                            // remove the notification and keep device locked
                            action.actionIntent.send();
                            clearNotification(ni.getUid());
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // unlock device and open the notification)
                        runPendingIntent(action.actionIntent, ni.getPackageName(), ni.uid, null);

                        hide(false);
                    }
                }
            };
        };

        viewManager = new NPViewManager(getApplicationContext(), mViewManagerCallbacks);

        // register receivers
        npreceiver = new NPReceiver();
        registerReceiver(npreceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(npreceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        registerReceiver(npreceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
        registerReceiver(npreceiver, new IntentFilter(WIDGET_LOCKER_UNLOCKED));
        registerReceiver(npreceiver, new IntentFilter(GO_LOCKER_UNLOCKED));
        registerReceiver(npreceiver, new IntentFilter(CHECK_LSAPP));
        registerReceiver(npreceiver, new IntentFilter(INCOMING_CALL));
        registerReceiver(npreceiver, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
        registerReceiver(npreceiver, new IntentFilter(DEVICE_UNLOCKED));
        registerReceiver(npreceiver, new IntentFilter(REFRESH_LIST));

        mSysUtils = SysUtils.getInstance(getApplicationContext(), mHandler);
    }

    private void runPendingIntent(PendingIntent action, String packageName, int uid, Intent paramIntent)
    {
        Log.d(TAG, "runPendingIntent: packageName:"+packageName+" uid:"+uid);
        Intent intent = new Intent(getApplicationContext(), OpenNotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("action", action);
        intent.putExtra("package", packageName);
        intent.putExtra("uid", uid);
        intent.putExtra("lockscreen_package", SysUtils.getForegroundApp(getApplicationContext()));
        intent.putExtra("paramIntent", paramIntent);
        startActivity(intent);

        // hide viewmanager if visible
        hide(false);
    }

    private void unlockDevice()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lockscreenPackageName = prefs.getString(SettingsManager.LOCKSCREEN_APP, SettingsManager.DEFAULT_LOCKSCREEN_APP);

        if (  !lockscreenPackageName.equals(NotificationsService.GO_LOCKER_PACKAGENAME) &&
              !lockscreenPackageName.equals(NotificationsService.WIDGET_LOCKER_PACKAGENAME) &&
              SysUtils.isKeyguardLocked(context) &&
              prefs.getBoolean(SettingsManager.UNLOCK_ON_OPEN, SettingsManager.DEFAULT_UNLOCK_ON_OPEN))
        {
            Intent intent = new Intent(context, UnlockDeviceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
        }
    }

    private boolean isActivity(PendingIntent action)
    {
        try
        {
            // call hidden method of PendingIntent - isActivity
            Class c = PendingIntent.class;
            Method m = c.getMethod("isActivity");
            Object o = m.invoke(action,null);
            return ((Boolean) o).booleanValue();
        }
        catch (Exception exp)
        {
            return true;
        }
    }

    private void openNotificationOnHalo(NotificationData ni)
    {
        Intent haloFlag = new Intent();
        int flags = Intent.FLAG_RECEIVER_FOREGROUND |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
                Intent.FLAG_ACTIVITY_NO_USER_ACTION |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                0x00002000; // = 277651456

        haloFlag.addFlags(flags);

        try {
            ni.getAction().send(getApplicationContext(), 0, haloFlag);
        } catch (PendingIntent.CanceledException e)
        {
            // if cannot launch intent, create a new one for the app
            try
            {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(ni.getPackageName());
                launchIntent.addFlags(0x00002000);
                startActivity(launchIntent);
            }
            catch(Exception e2)
            {
                // cannot launch intent - do nothing...
                e2.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error - cannot launch app:"+ni.getPackageName(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLowMemory()
    {
        Log.w(TAG, "Low memory warning. NiLS will probably be killed soon.");
    }

    public void hide(boolean force)
    {
        if (viewManager.isVisible())
            viewManager.hide(force);
    }

    public void show(boolean immediate)
    {
        if (!viewManager.isVisible())
            viewManager.show(immediate);
    }
}