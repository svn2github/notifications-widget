package com.roymam.android.notificationswidget;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;

import com.roymam.android.nilsplus.NPService;

import java.io.IOException;
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
    public static final String CANCEL_NOTIFICATION = "com.roymam.android.nils.cancel_notification";
    public static final String BIND_NILS_FP = "com.roymam.android.nils.bind_nils_fp";
    public static final String EXTRA_PACKAGENAME = "EXTRA_PACKAGENAME";
    public static final String EXTRA_ID = "EXTRA_ID";
    public static final String EXTRA_UID = "EXTRA_UID";
    public static final String EXTRA_TAG = "EXTRA_TAG";
    private static final int RECREATE = 1;

    private static NotificationsProvider instance;
    private Context context;
    private NotificationEventListener listener;
    private ArrayList<NotificationData> mNotifications = new ArrayList<NotificationData>();
    private NotificationParser parser;
    private HashMap<String, PersistentNotification> persistentNotifications = new HashMap<String, PersistentNotification>();
    private BroadcastReceiver receiver = null;
    private boolean mDirty = true;
    private ReadWriteLock lock = new ReentrantReadWriteLock();;
    private ArrayList<NotificationData> mFilteredNotificationsList;
    private Handler mHandler;

    public NotificationsService()
    {
    }

    /** Messenger for communicating with the NiLS FP service. */
    Messenger mService = null;

    /** Flag indicating whether we have called bind on the service. */
    boolean mBound;

    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.
            Log.d("NiLS", "Connected to NiLS FP Service");
            mService = new Messenger(service);
            mBound = true;

            // Create and send a message to the service, using a supported 'what' value
            Message msg = Message.obtain(null, RECREATE, 0, 0);
            try
            {
                mService.send(msg);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }

            // Re-send all of the current notfications
            for(int i = getNotifications().size()-1; i>=0; i--)
            {
                NotificationData nd = getNotifications().get(i);
                listener.onNotificationAdded(nd, false, mCovered);
            }
        }

        public void onServiceDisconnected(ComponentName className)
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            Log.d("NiLS", "Disconnected from NiLS FP Service");
            mService = null;
            mBound = false;

            // try to rebind it
            bindNiLSFP();
        }
    };

    @Override
    public void onCreate()
    {
        super.onCreate();

        Log.d("NiLS", "NotificationsService:onCreate");
        instance = this;

        // create a notification parser
        context = getApplicationContext();
        parser = new NotificationParser(getApplicationContext());
        mHandler = new Handler();
        setNotificationEventListener(new NotificationAdapter(context, new Handler()));

        // TBD register a receiver for system broadcasts
        //receiver = new NotificationsServiceReceiver();
        //registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_ON));

        bindNiLSFP();

        testProximity();

        context.sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_READY));
    }

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

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.d("NiLS", "NotificationsService:onUnbind");
        saveLog(getApplicationContext(), true);

        // unbound NiLS FP (if bounded)
        if (mBound)
        {
            unbindService(mConnection);
            mBound = false;
        }
        if (listener != null)
            listener.onServiceStopped();
        return super.onUnbind(intent);
    }

    public void bindNiLSFP()
    {
        if (!mBound)
        {
            // Try to bind to NiLS FP Service
            Intent npsIntent = new Intent(context, NPService.class);
            bindService(npsIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
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

    @Override
    public void onDestroy()
    {
        Log.d("NiLS","NotificationsService:onDestroy");

        instance = null;
        saveLog(getApplicationContext(),true);
        getApplicationContext().sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_DIED));
        stopProximityMonitoring();
        super.onDestroy();
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
            Process process = Runtime.getRuntime().exec("logcat -d -v time -f " + filename);

            Log.d("NiLS", "Log file written to "+context.getExternalFilesDir(null)+"/"+filename);

            if (!silent)
            {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.appicon)
                        .setLargeIcon(((BitmapDrawable) context.getResources().getDrawable(R.drawable.appicon)).getBitmap())
                        .setContentTitle("Something went wrong...")
                        .setContentText("log file was written to " + context.getExternalFilesDir(null) + "/" + filename);

                NotificationCompat.BigTextStyle bigtextstyle = new NotificationCompat.BigTextStyle();
                bigtextstyle.setBigContentTitle("Something went wrong...");
                bigtextstyle.bigText("log file was written to " + context.getExternalFilesDir(null) + "/" + filename);
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        //Log.d("NiLS","NotificationsService:onStartCommand " + intent.getAction());
        if (context == null) context = getApplicationContext();
        if (parser == null) parser = new NotificationParser(getApplicationContext());
        if (listener == null) setNotificationEventListener(new NotificationAdapter(context, mHandler));

        if (intent != null && intent.getAction() != null)
        {
            String action = intent.getAction();

            // cancel notification request from NiLS FP
            if (action.equals(CANCEL_NOTIFICATION))
            {
                int uid = intent.getIntExtra(EXTRA_UID, -1);
                clearNotification(uid);
            }

            if (action.equals(BIND_NILS_FP))
            {
                bindNiLSFP();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

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
                context.sendBroadcast(new Intent(NPService.INCOMING_CALL));
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
            }
        }
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
    public static NotificationsProvider getSharedInstance()
    {
        return instance;
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
            Intent intent = new Intent(context, NewNotificationsListener.class);
            intent.setAction(CANCEL_NOTIFICATION);
            intent.putExtra(EXTRA_PACKAGENAME, packageName);
            intent.putExtra(EXTRA_TAG, tag);
            intent.putExtra(EXTRA_ID, id);
            startService(intent);
        }
    }

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
            }
        }
        else
        {
            Log.d("NiLS", "NotificationsService:clearNotification - wasn't found");
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

    // binding stuff
    //***************
    private final IBinder mBinder = new LocalBinder();

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
            return isServiceRunning(context, NewNotificationsListener.class);
        else
            return isServiceRunning(context, NiLSAccessibilityService.class);
    }

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
    {       // bound to Accessibility / Notifications service
        // bind it to NiLS FP
        bindNiLSFP();
        if (listener != null)
            listener.onServiceStarted();
        return mBinder;
    }
}