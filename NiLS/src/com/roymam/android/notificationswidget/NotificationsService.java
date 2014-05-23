package com.roymam.android.notificationswidget;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.KeyguardManager;
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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.roymam.android.common.SysUtils;
import com.roymam.android.nilsplus.activities.OpenNotificationActivity;
import com.roymam.android.nilsplus.ui.NPViewManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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

    private NPReceiver npreceiver = null;
    private NPViewManager viewManager = null;
    private NPViewManager.Callbacks mViewManagerCallbacks = null;
    private SysUtils mSysUtils;
    private Context context;
    private NotificationEventListener listener;
    private ArrayList<NotificationData> mNotifications = new ArrayList<NotificationData>();
    private NotificationParser parser;
    private HashMap<String, PersistentNotification> persistentNotifications = new HashMap<String, PersistentNotification>();
    private BroadcastReceiver receiver = null;
    private boolean mDirty = true;
    private ReadWriteLock lock = new ReentrantReadWriteLock();;
    private ArrayList<NotificationData> mFilteredNotificationsList;
    private final Handler mHandler = new Handler();

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

        Log.d("NiLS", "NotificationsService:onCreate");
        instance = this;

        // create a notification parser
        context = getApplicationContext();
        parser = new NotificationParser(getApplicationContext());

        // set up events listener
        setNotificationEventListener(new NotificationAdapter(context, new Handler()));

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
        Log.d("NiLS", "NotificationsService:onUnbind");
        saveLog(getApplicationContext(), true);

        if (listener != null)
            listener.onServiceStopped();

        // clear notifications and add an error notification
        notifyUserToEnableTheService();

        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy()
    {
        Log.d("NiLS","NotificationsService:onDestroy");
        instance = null;

        // save the recent log into a file
        saveLog(getApplicationContext(),true);

        // notify world that NiLS service has stopped
        getApplicationContext().sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_DIED));
        Log.w("NiLS", "NiLS FP Service was killed. hiding notifications list.");

        stopProximityMonitoring();

        // cleanup view manager
        if (viewManager != null)
        {
            viewManager.destroy();
            viewManager = null;
            mNotifications = null;
            mViewManagerCallbacks = null;
        }

        // cleanup broadcast receiever
        if (npreceiver != null)
        {
            unregisterReceiver(npreceiver);
            npreceiver = null;
        }

        super.onDestroy();
    }

    private void notifyUserToEnableTheService()
    {
        viewManager.saveNotificationsState();
        mNotifications.clear();
        NotificationData ni = new NotificationData();
        ni.setTitle(getString(R.string.nils_service_is_not_running));
        ni.setText(getString(R.string.open_nils_to_enable_it));
        ni.setAppIcon(((BitmapDrawable)getApplicationContext().getResources().getDrawable(R.drawable.nilsfp_icon_mono)).getBitmap());
        ni.setIcon(((BitmapDrawable)getApplicationContext().getResources().getDrawable(R.drawable.ic_launcher)).getBitmap());
        ni.setPackageName(getApplicationContext().getPackageName());

        Intent startNiLSIntent = getApplicationContext().getPackageManager().getLaunchIntentForPackage("com.roymam.android.notificationswidget");
        if (startNiLSIntent != null)
        {
            ni.setAction(PendingIntent.getActivity(getApplicationContext(), 0, startNiLSIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        }
        ni.setActions(new NotificationData.Action[0]);
        ni.setId(0);
        ni.setReceived(System.currentTimeMillis());
        mNotifications.add(ni);
        viewManager.notifyDataChanged();
        viewManager.animateNotificationsChange();
    }

    private static void saveLog(Context context, boolean silent)
    {
        // save crash log
        Log.d("NiLS", "Something happened. saving log file...");
        try
        {
            // read logcat
            Time now = new Time();
            now.setToNow();
            String filename = context.getExternalFilesDir(null) + "/" + now.format("%Y-%m-%dT%H:%M:%S")+".log";
            Runtime.getRuntime().exec("logcat -d -v time -f " + filename);

            Log.d("NiLS", "Log file written to "+context.getExternalFilesDir(null)+"/"+filename);

            if (!silent)
            {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.appicon)
                        .setLargeIcon(((BitmapDrawable) context.getResources().getDrawable(R.drawable.appicon)).getBitmap())
                        .setContentTitle("Something went wrong...")
                        .setContentText("log file was written to " + filename);

                NotificationCompat.BigTextStyle bigtextstyle = new NotificationCompat.BigTextStyle();
                bigtextstyle.setBigContentTitle("Something went wrong...");
                bigtextstyle.bigText("log file was written to " + filename);
                mBuilder.setStyle(bigtextstyle);

                Notification n = mBuilder.build();

                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse("file://" + context.getExternalFilesDir(null) + "/" + filename);
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
        Log.d("NiLS", "Testing proximity sensor...");

        startProximityMonitoring();

        // test after 100ms if the proximity sensor status was changed
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (mCovered != null) {
                    Log.d("NiLS", "immediate response");
                    prefs.edit().putBoolean(SettingsActivity.IMMEDIATE_PROXIMITY, true).commit();
                    // stop proximity monitoring - there is no need to if the proximity is immediate
                    stopProximityMonitoring();
                }
                else {
                    Log.d("NiLS", "no response after 100ms");
                    prefs.edit().putBoolean(SettingsActivity.IMMEDIATE_PROXIMITY, false).commit();
                }

                // stop proximity monitoring if wakeup mode is always or never
                String wakeupMode = SettingsActivity.getWakeupMode(context, null);
                if (wakeupMode.equals(SettingsActivity.WAKEUP_ALWAYS) || wakeupMode.equals(SettingsActivity.WAKEUP_NEVER))
                    stopProximityMonitoring();
            }
        },100);
    }

    SensorManager sensorManager;
    SensorEventListener sensorListener;

    public void startProximityMonitoring()
    {
        Log.d("NiLS", "Starting monitoring proximity sensor constantly");
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
        Log.d("NiLS", "Stopping monitoring proximity sensor");
        if (sensorListener != null && sensorManager != null) {
            sensorManager.unregisterListener(sensorListener);
            sensorListener = null;
        }
    }

    //** End of Proximity Sensor Monitoring **/



    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (context == null) context = getApplicationContext();
        if (parser == null) parser = new NotificationParser(getApplicationContext());
        if (listener == null) setNotificationEventListener(new NotificationAdapter(context, mHandler));

        if (intent.getAction() != null)
            if (intent.getAction().equals("refresh"))
        d        updateViewManager();

        return super.onStartCommand(intent, flags, startId);
    }

    public void refreshLayout()
    {
        viewManager.refreshLayout();
    }

    //** Notifications Add/Remove Handling **/
    private void addNotification(NotificationData nd)
    {
        if (nd != null)
        {
            Log.d("NiLS","NotificationsService:addNotification " + nd.packageName + ":" + nd.id);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String notificationMode = SettingsActivity.getNotificationMode(getApplicationContext(), nd.packageName);
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
                    // 1. Android >=4.3 - notification mode is "grouped" and the notification has the same package and id
                    // 2. Android <4.3 - notification mode is "grouped" and the notification has the same package
                    // 3. notification is similar to the old one
                    if (oldnd.packageName.equals(nd.packageName) &&
                            (((oldnd.id == nd.id || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) && notificationMode.equals(SettingsActivity.MODE_GROUPED)) ||
                                    oldnd.isSimilar(nd, true))) {
                        nd.uid = oldnd.uid;
                        nd.deleted = oldnd.deleted;

                        // protect it from being cleared on next purge command
                        nd.protect = true;

                        iter.remove();
                        oldnd.cleanup();
                        updated = true;
                        changed = !oldnd.isEqual(nd);

                        // if it is exact the same notificaiton - keep old received time
                        if (!changed) nd.received = oldnd.received;

                        break;
                    } else // check if the old notification is a duplicate of the current but contains more data than the current - if so - ignore the new one
                    {
                        if (nd.isSimilar(oldnd, false))
                        {
                            ignoreNotification = true;
                            updated = false;
                        }
                    }
                }

                if (!ignoreNotification)
                {
                    // add the new notification
                    mNotifications.add(nd);
                    mDirty = true;
                }
            }
            finally
            {
                w.unlock();
            }


            // notify that the notification was added
            if (listener != null && !nd.deleted && !ignoreNotification)
            {
                if (updated)
                    listener.onNotificationUpdated(nd, changed, mCovered);
                else
                    listener.onNotificationAdded(nd, true, mCovered);
                listener.onNotificationsListChanged();
                callUpdateViewManager();
            }
        }
    }

    private void addPersistentNotification(PersistentNotification pn)
    {
        if (pn != null)
        {
            Log.d("NiLS","NotificationsService:addPersistentNotification " + pn.packageName);

            persistentNotifications.put(pn.packageName, pn);
            if (listener != null) listener.onPersistentNotificationAdded(pn);

            if (pn.packageName.equals("com.android.dialer") || pn.packageName.equals("com.google.android.dialer"))
                context.sendBroadcast(new Intent(INCOMING_CALL));
        }
    }

    private void removeNotification(String packageName, int id, boolean logical)
    {
        Log.d("NiLS","NotificationsService:removeNotification  " + packageName + ":" + id);
        boolean sync = SettingsActivity.shouldClearWhenClearedFromNotificationsBar(getApplicationContext());
        if (sync)
        {
            boolean cleared = false;

            ArrayList<NotificationData> clearedNotifications = new ArrayList<NotificationData>();

            // find the notification and remove it
            Lock w = lock.writeLock();
            w.lock();
            try {
                Iterator<NotificationData> iter = mNotifications.iterator();

                while (iter.hasNext()) {
                    NotificationData nd = iter.next();

                    if (nd.packageName.equals(packageName) && nd.id == id && !nd.pinned) {
                        // mark as delete if it's part of multiple events notification
                        if (logical && nd.event) {
                            // mark notification as cleared
                            nd.deleted = true;
                        } else if (!nd.deleted) // make sure it hasn't been deleted previously by the user
                        {
                            // immediately remove notification
                            iter.remove();
                        }

                        mDirty = true;

                        // notify that the notification was cleared
                        clearedNotifications.add(nd);

                        cleared = true;
                        // do not stop loop - keep looping to clear all of the notifications with the same id
                    }
                }
            }
            finally
            {
                w.unlock();
            }

            // notify listener for cleared notifications
            if (cleared && listener != null)
            {
                for (NotificationData nd : clearedNotifications)
                {
                    listener.onNotificationCleared(nd);
                }
                listener.onNotificationsListChanged();
                callUpdateViewManager();
            }
        }
    }

    private void callUpdateViewManager()
    {
        if (viewManager != null)
            viewManager.saveNotificationsState();

        Intent refreshListIntent = new Intent(context, NotificationsService.class);
        refreshListIntent.setAction("refresh");
        startService(refreshListIntent);
    }

    @SuppressWarnings("UnusedParameters")
    private void removePersistentNotification(String packageName, int id)
    {
        Log.d("NiLS","NotificationsService:removePersistentNotification "+packageName);
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
        if (!mDirty)
            return mFilteredNotificationsList;

        mFilteredNotificationsList = new ArrayList<NotificationData>();
        if (context != null)
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String sortBy = prefs.getString(SettingsActivity.NOTIFICATIONS_ORDER, "time");
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
                    if (!nd.deleted)
                        mFilteredNotificationsList.add(nd);
                }
                sortNotificationsList(mFilteredNotificationsList, sortBy);
            }
        }

        mDirty = false;
        return mFilteredNotificationsList;
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
                            // if we reached here, the priorities are equal - sory by time
                            if (n1.received < n2.received)
                                return 1;
                            if (n1.received > n2.received)
                                return -1;
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
                            return 0;
                        }
                    });
                }
        }
    }

    @Override
    public void clearAllNotifications()
    {
        Log.d("NiLS","NotificationsService:clearAllNotifications");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean syncback = prefs.getBoolean(SettingsActivity.SYNC_BACK, SettingsActivity.DEFAULT_SYNC_BACK);

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
                    if (nd.event)
                        nd.deleted = true;
                    else // otehrwise remove it immediately
                        i.remove();
                    mDirty = true;
                }
            }
        }
        finally
        {
            w.unlock();
        }

        // notify listener for cleared notifications
        if (listener != null)
        {
            for(NotificationData nd : clearedNotifications)
            {
                // notify android to clear it too
                if (syncback)
                    try
                    {
                        cancelNotification(nd.packageName, nd.tag, nd.id);
                    }
                    catch (Exception exp)
                    {
                        exp.printStackTrace();
                    }
                listener.onNotificationCleared(nd);
            }
            listener.onNotificationsListChanged();
        }
    }

    private void cancelNotification(String packageName, String tag, int id)
    {
        Log.d("NiLS","NotificationsService:cancelNotification " + packageName + ":" + id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            Intent intent = new Intent(context, NotificationsListener.class);
            intent.setAction(CANCEL_NOTIFICATION);
            intent.putExtra(EXTRA_PACKAGENAME, packageName);
            intent.putExtra(EXTRA_TAG, tag);
            intent.putExtra(EXTRA_ID, id);
            startService(intent);
        }
    }

    @Override
    public synchronized void clearNotificationsForApps(String[] packages)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean syncback = prefs.getBoolean(SettingsActivity.SYNC_BACK, SettingsActivity.DEFAULT_SYNC_BACK);
        boolean changed = false;
        ArrayList<NotificationData> clearedNotifications = new ArrayList<NotificationData>();

        for(String packageName : packages)
        {
            Log.d("NiLS","NotificationsService:clearNotificationsForApps " + packageName);
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
                        if (nd.event)
                            nd.deleted = true;
                        else
                            i.remove();

                        mDirty = true;
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
        if (changed && listener != null)
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
                listener.onNotificationCleared(nd);
            }
            listener.onNotificationsListChanged();
        }
    }

    @Override
    public synchronized void clearNotification(int uid)
    {
        if (viewManager != null)
            viewManager.saveNotificationsState();

        Log.d("NiLS","NotificationsService:clearNotification uid:" + uid);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean syncback = prefs.getBoolean(SettingsActivity.SYNC_BACK, SettingsActivity.DEFAULT_SYNC_BACK);

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
                    if (nd.event)
                        nd.deleted = true;
                    else
                        iter.remove();

                    mDirty = true;
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
            // search for more notification with the same id - if not found - dismiss the notification from android status bar
            boolean more = false;
            Lock r = lock.readLock();
            r.lock();
            try
            {
                for (NotificationData nd : mNotifications)
                {
                    if (nd.id == removedNd.id && !nd.deleted &&
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
                    cancelNotification(removedNd.packageName, removedNd.tag, removedNd.id);
                }
                catch (Exception exp)
                {
                    exp.printStackTrace();
                }

            if (listener != null)
            {
                listener.onNotificationCleared(removedNd);
                listener.onNotificationsListChanged();

                // notify view manager that the data has been changed
                updateViewManager();
            }
        }
        else
        {
            Log.d("NiLS", "NotificationsService:clearNotification - wasn't found");
        }
    }

    private void updateViewManager()
    {
        if (viewManager != null) {
            viewManager.notifyDataChanged();
            viewManager.animateNotificationsChange();
        }
    }

    public void onNotificationPosted(Notification n, String packageName, int id, String tag)
    {
        try {
            if (!parser.isPersistent(n, packageName)) {
                List<NotificationData> notifications = parser.parseNotification(n, packageName, id, tag);
                for (NotificationData nd : notifications) {
                    addNotification(nd);
                }
                // after adding all of the new notifications delete all of the old ones that marked as deleted
                purgeDeletedNotifications(packageName, id);
            } else {
                PersistentNotification pn = parser.parsePersistentNotification(n, packageName, id);
                addPersistentNotification(pn);
            }
        }
        catch(Exception exp)
        {
            Log.e("NiLS", "NotificationsService:onNotificationPosted: an exception has occured");
            exp.printStackTrace();
            // make sure this isn't NiLS own crash notification
            if (!(packageName != null && packageName.equals(context.getPackageName())))
                saveLog(context, false);
        }
    }

    private void purgeDeletedNotifications(String packageName, int id)
    {
        Log.d("NiLS","purging deleted mNotifications "+ packageName + ":" + id);

        Lock w = lock.writeLock();
        w.lock();
        try
        {
            Iterator<NotificationData> iter = mNotifications.iterator();
            while (iter.hasNext())
            {
                NotificationData nd = iter.next();
                if (nd.packageName.equals(packageName) &&
                        nd.deleted &&
                        !nd.protect &&
                        (nd.id == id || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2))
                {
                    Log.d("NiLS", "permanently removing uid:" + nd.uid);
                    iter.remove();
                    mDirty = true;
                }
                // make sure next time it won't be protected from deleting
                nd.protect = false;
            }
        }
        finally
        {
            w.unlock();
        }
    }

    public void onNotificationRemoved(Notification n, String packageName, int id)
    {
        try
        {
            removeNotification(packageName, id, false);

            // remove also persistent notification
            if (n != null && parser.isPersistent(n, packageName)) {
                removePersistentNotification(packageName, id);
            }
        }
        catch(Exception exp)
        {
            Log.e("NiLS", "NotificationsService:onNotificationRemoved: an exception has occured");
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

    public static boolean isServiceRunning(Context context, Class serviceClass)
    {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isServiceRunning(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            return isServiceRunning(context, NotificationsListener.class);
        else
            return isServiceRunning(context, NiLSAccessibilityService.class);
    }

    private class NPReceiver extends BroadcastReceiver
    {
        private PendingIntent checkLockScreenPendingIntent = null;

        public void onReceive(Context context, Intent intent)
        {
            try
            {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

                String lockScreenApp = prefs.getString(SettingsActivity.LOCKSCREEN_APP, STOCK_LOCKSCREEN_PACKAGENAME);

                if(intent.getAction().equals(Intent.ACTION_USER_PRESENT) && lockScreenApp.equals(STOCK_LOCKSCREEN_PACKAGENAME) ||
                        intent.getAction().equals(WIDGET_LOCKER_UNLOCKED) ||
                        intent.getAction().equals(WIDGET_LOCKER_HIDE) ||
                        intent.getAction().equals(GO_LOCKER_UNLOCKED) )
                {
                    // hide notifications list
                    hide(false);
                }
                else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
                {
                    // show notifications when the screen is turned off
                    NotificationsService.this.viewManager.refreshLayout();
                    NotificationsService.this.viewManager.show();

                    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                    if (checkLockScreenPendingIntent != null)
                    {
                        Log.d("NiLS", "screen is off, stop monitoring for foreground app");
                        am.cancel(checkLockScreenPendingIntent);
                        checkLockScreenPendingIntent = null;
                    }

                }
                else if (intent.getAction().equals(INCOMING_CALL))
                {
                    NotificationsService.this.viewManager.hide(false);
                }
                else if (intent.getAction().equals(CHECK_LSAPP) || intent.getAction().equals(Intent.ACTION_SCREEN_ON))
                {
                    boolean dontHide = prefs.getBoolean(SettingsActivity.DONT_HIDE, SettingsActivity.DEFAULT_DONT_HIDE);
                    if (!dontHide)
                    {
                        boolean autoDetect = false;

                        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
                        {
                            Log.d("NiLS", "screen is on, auto detecting lock screen app");
                            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                            checkLockScreenPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(CHECK_LSAPP), PendingIntent.FLAG_UPDATE_CURRENT);
                            am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, 1000, checkLockScreenPendingIntent);
                            autoDetect = true;

                            // make sure screen will stay on as needed seconds as defined on settings
                            mSysUtils.turnScreenOn(true);
                        }
                        if (shouldHideNotifications(autoDetect))
                        {
                            // hide notifications list if not needed
                            NotificationsService.this.viewManager.hide(false);

                            Log.d("NiLS", "lock screen is no longer active, stop monitoring");
                            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                            am.cancel(checkLockScreenPendingIntent);
                            checkLockScreenPendingIntent = null;
                        }
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

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        viewManager.refreshLayout();
    }

    private boolean shouldHideNotifications(boolean autoDetect)
    {
        return shouldHideNotifications(getApplicationContext(), autoDetect);
    }

    public static boolean shouldHideNotifications(Context context, boolean autoDetect)
    {
        String currentApp = getForegroundApp(context);
        return shouldHideNotifications(context, currentApp, autoDetect);
    }

    public static boolean shouldHideNotifications(Context context, String currentApp, boolean autoDetect)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // get the current lock screen app (if set)
        String lockScreenApp = prefs.getString(SettingsActivity.LOCKSCREEN_APP, STOCK_LOCKSCREEN_PACKAGENAME );
        boolean shouldHide = true;

        // if the current app is one of the allowed apps to be on top of the lock screen, hide NiLS
        if (SettingsActivity.BLACKLIST_PACKAGENAMES.contains(currentApp))
        {
            return true;
        }

        if (autoDetect)
        {
            PackageManager pm = context.getPackageManager();

            // check if the current app is a lock screen app
            if (pm.checkPermission(android.Manifest.permission.DISABLE_KEYGUARD, currentApp) == PackageManager.PERMISSION_GRANTED)
            {
                if (!lockScreenApp.equals(currentApp))
                {
                    // store current app as the lock screen app until next time
                    Log.d("NiLS", "new lock screen app detected: " + currentApp);
                    prefs.edit().putString(SettingsActivity.LOCKSCREEN_APP, currentApp).commit();
                    lockScreenApp = currentApp;
                }
            }
            else if (isKeyguardLocked(context))
            {
                if (!lockScreenApp.equals(STOCK_LOCKSCREEN_PACKAGENAME))
                {
                    // store current app as the lock screen app until next time
                    Log.d("NiLS", "stock lock screen app detected");
                    prefs.edit().putString(SettingsActivity.LOCKSCREEN_APP, STOCK_LOCKSCREEN_PACKAGENAME ).commit();
                    lockScreenApp = STOCK_LOCKSCREEN_PACKAGENAME ;
                }
            }
        }

        // check if the device is secured or the current app is the lock screen app
        if (lockScreenApp.equals(STOCK_LOCKSCREEN_PACKAGENAME ) && isKeyguardLocked(context) || lockScreenApp.equals(currentApp))
            shouldHide = false;

        return shouldHide;
    }

    public static String getForegroundApp(Context context)
    {
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
        return tasks.get(0).topActivity.getPackageName();
    }

    private static boolean isAppForground(Context context, String packageName)
    {
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> l = mActivityManager
                .getRunningAppProcesses();
        Iterator<ActivityManager.RunningAppProcessInfo> i = l.iterator();
        while (i.hasNext())
        {
            ActivityManager.RunningAppProcessInfo info = i.next();
            for (String p : info.pkgList)
            {
                if (p.equals(packageName) && info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
                    return true;
            }
        }
        return false;
    }

    public static boolean isKeyguardLocked(Context context)
    {
        KeyguardManager kmanager = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            return kmanager.isKeyguardLocked();
        else
            return kmanager.inKeyguardRestrictedInputMode();
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

                boolean halo = prefs.getBoolean(SettingsActivity.HALO_MODE, SettingsActivity.DEFAULT_HALO_MODE);

                if (halo)
                {
                    openNotificationOnHalo(ni);
                }
                else
                {
                    // unlock device and open the notification)
                    Intent intent = new Intent(getApplicationContext(), OpenNotificationActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("action", ni.getAction());
                    intent.putExtra("package", ni.getPackageName());
                    intent.putExtra("id", ni.getId());
                    intent.putExtra("uid", ni.getUid());
                    intent.putExtra("lockscreen_package", getForegroundApp(getApplicationContext()));
                    startActivity(intent);
                }
            }

            @Override
            public void onAction(NotificationData ni, PendingIntent action, String actionName)
            {
                // show the name of the action
                Toast.makeText(getApplicationContext(), actionName,Toast.LENGTH_SHORT).show();

                if (!isActivity(action))
                {
                    // open it in background
                    try
                    {
                        // remove the notification and keep device locked
                        action.send();
                        clearNotification(ni.getUid());
                    } catch (PendingIntent.CanceledException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    // unlock device and open the notification)
                    Intent intent = new Intent(getApplicationContext(), OpenNotificationActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("action", action);
                    intent.putExtra("package", ni.getPackageName());
                    intent.putExtra("id", ni.getId());
                    intent.putExtra("uid", ni.getUid());
                    intent.putExtra("lockscreen_package", getForegroundApp(getApplicationContext()));
                    startActivity(intent);
                    viewManager.keepScreenOn();
                    viewManager.hide(false);
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

        mSysUtils = SysUtils.getInstance(getApplicationContext(), mHandler);
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
        Log.w("NiLS", "Low memory warning. NiLS will probably be killed soon.");
    }

    public void hide(boolean force)
    {
        viewManager.hide(force);
    }

    public void show()
    {
        viewManager.show();
    }
}