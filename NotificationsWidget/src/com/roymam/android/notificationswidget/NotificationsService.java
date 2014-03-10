package com.roymam.android.notificationswidget;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class NotificationsService extends Service implements NotificationsProvider
{
    public static final String CANCEL_NOTIFICATION = "com.roymam.android.nils.cancel_notification";
    public static final String EXTRA_PACKAGENAME = "EXTRA_PACKAGENAME";
    public static final String EXTRA_ID = "EXTRA_ID";
    public static final String EXTRA_UID = "EXTRA_UID";
    public static final String EXTRA_TAG = "EXTRA_TAG";

    private static NotificationsProvider instance;
    private Context context;
    private NotificationEventListener listener;
    private ArrayList<NotificationData> notifications = new ArrayList<NotificationData>();
    private NotificationParser parser;
    private HashMap<String, PersistentNotification> persistentNotifications = new HashMap<String, PersistentNotification>();

    public NotificationsService()
    {
    }

    @Override
    public void onCreate()
    {
        Log.d("NiLS","NotificationsService:onCreate");

        instance = this;

        // create a notification parser
        context = getApplicationContext();
        parser = new NotificationParser(getApplicationContext());
        setNotificationEventListener(new NotificationAdapter(context));

        context.sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_READY));
        super.onCreate();
    }

    @Override
    public void onDestroy()
    {
        Log.d("NiLS","NotificationsService:onDestroy");

        instance = null;
        saveLog(getApplicationContext());
        getApplicationContext().sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_DIED));
        super.onDestroy();
    }

    private void saveLog(Context context)
    {
        // save crash log
        Log.d("NiLS","NotificationsService:saveLog");
        Log.d("NiLS", "Service has stopped. saving log file...");
        try
        {
            // read logcat
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder log=new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null)
            {
                log.append(line);
                log.append("\r\n");
            }

            Time now = new Time();
            now.setToNow();
            String filename = now.format("%Y-%m-%dT%H:%M:%S")+".log";

            // save log into a file
            File dst = new File(context.getExternalFilesDir(null),filename);
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(dst));
            output.writeChars(log.toString());
            output.flush();
            output.close();
            bufferedReader.close();

            Log.d("NiLS", "Log file written to "+context.getExternalFilesDir(null)+"/"+filename);
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
        if (listener == null) setNotificationEventListener(new NotificationAdapter(context));

        if (intent != null && intent.getAction() != null)
        {
            String action = intent.getAction();

            // cancel notification request from NiLS FP
            if (action.equals(CANCEL_NOTIFICATION))
            {
                int uid = intent.getIntExtra(EXTRA_UID, -1);
                clearNotification(uid);
            }

            /*if (action.equals(NOTIFICATION_POSTED))
            {
                NotificationData notification = intent.getParcelableExtra(EXTRA_NOTIFICATION);
                addNotification(notification);
            }
            else if (action.equals(NOTIFICATION_REMOVED))
            {
                String packageName = intent.getStringExtra(EXTRA_PACKAGENAME);
                int id = intent.getIntExtra(EXTRA_ID, -1);
                removeNotification(packageName, id);
            }
            else if (action.equals(PERSISTENT_NOTIFICATION_POSTED))
            {
                PersistentNotification pn = intent.getParcelableExtra(EXTRA_NOTIFICATION);
                addPersistentNotification(pn);
            }
            else if (action.equals(PERSISTENT_NOTIFICATION_REMOVED))
            {
                String packageName = intent.getStringExtra(EXTRA_PACKAGENAME);
                int id = intent.getIntExtra(EXTRA_ID, -1);
                removePersistentNotification(packageName, id);
            }*/
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
            Iterator<NotificationData> iter = notifications.iterator();

            boolean changed = false;

            boolean ignoreNotification = false;

            while (iter.hasNext())
            {
                NotificationData oldnd = iter.next();

                // remove only if one of the following scenarios:
                // 1. Android >=4.3 - notification mode is "grouped" and the notification has the same package and id
                // 2. Android <4.3 - notification mode is "grouped" and the notification has the same package
                // 3. notification is similar to the old one
                if (oldnd.packageName.equals(nd.packageName)  &&
                    (((oldnd.id == nd.id || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) && notificationMode.equals(SettingsActivity.MODE_GROUPED)) ||
                     oldnd.isSimilar(nd, true)))
                {
                    nd.uid = oldnd.uid;
                    nd.deleted = oldnd.deleted;

                    // protect it from being cleared on next purge command
                    nd.protect = true;

                    iter.remove();
                    oldnd.cleanup();
                    updated = true;
                    changed = !oldnd.isEqual(nd);
                    break;
                }
                else // check if the old notification is a duplicate of the current but contains more data than the current - if so - ignore the new one
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
                notifications.add(nd);

                // notify that the notification was added
                if (listener != null && !nd.deleted)
                {
                    if (updated)
                        listener.onNotificationUpdated(nd, changed);
                    else
                        listener.onNotificationAdded(nd, true);
                    listener.onNotificationsListChanged();
                }
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
            Iterator<NotificationData> iter = notifications.iterator();

            while (iter.hasNext())
            {
                NotificationData nd = iter.next();

                if (nd.packageName.equals(packageName) && nd.id == id && !nd.pinned)
                {
                    if (logical)
                    {
                        // mark notification as cleared
                        nd.deleted = true;
                    }
                    else
                    {
                        // immediately remove notification
                        iter.remove();
                    }

                    // notify that the notification was cleared
                    clearedNotifications.add(nd);

                    cleared = true;
                    // do not stop loop - keep looping to clear all of the notifications with the same id
                }
            }

            // notify listener for cleared notifications
            if (cleared && listener != null)
            {
                for(NotificationData nd : clearedNotifications)
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
    public List<NotificationData> getNotifications()
    {
        ArrayList<NotificationData> notificationsList = new ArrayList<NotificationData>();
        if (context != null)
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String sortBy = prefs.getString(SettingsActivity.NOTIFICATIONS_ORDER, "time");

            for(NotificationData nd : notifications)
            {
                if (!nd.deleted)
                    notificationsList.add(nd);
            }
            sortNotificationsList(notificationsList, sortBy);
        }
        return notificationsList;
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

        Iterator<NotificationData> i = notifications.iterator();
        while (i.hasNext())
        {
            NotificationData nd = i.next();
            if (!nd.pinned)
            {
                clearedNotifications.add(nd);
                nd.deleted = true;
            }
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
            ListIterator<NotificationData> i = notifications.listIterator();
            while (i.hasNext())
            {
                NotificationData nd = i.next();
                if (!nd.pinned && nd.packageName.equals(packageName))
                {
                    // mark notification as deleted
                    nd.deleted = true;
                    changed = true;
                    clearedNotifications.add(nd);
                }
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
        Iterator<NotificationData> iter = notifications.iterator();
        boolean removed = false;
        NotificationData removedNd = null;

        while (iter.hasNext() && !removed)
        {
            NotificationData nd = iter.next();
            if (nd.uid == uid)
            {
                // store id and package name to search for more notifications with the same id
                removedNd = nd;

                // mark notification as deleted
                nd.deleted = true;

                removed = true;
            }
        }

        if (removed)
        {
            // search for more notification with the same id - if not found - dismiss the notifcation from android status bar
            boolean more = false;
            for(NotificationData nd : notifications)
            {
                if (nd.id == removedNd.id && !nd.deleted &&
                        nd.packageName.equals(removedNd.packageName))
                    more = true;
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
            Log.d("NiLS","NotificationsService:clearNotification - wasn't found");
        }
    }

    public void onNotificationPosted(Notification n, String packageName, int id, String tag)
    {
        if (!parser.isPersistent(n, packageName))
        {
            List<NotificationData> notifications = parser.parseNotification(n, packageName, id, tag);
            for (NotificationData nd : notifications)
            {
                addNotification(nd);
            }
            // after adding all of the new notifications delete all of the old ones that marked as deleted
            purgeDeletedNotifications(packageName, id);
        }
        else
        {
            PersistentNotification pn = parser.parsePersistentNotification(n, packageName, id);
            addPersistentNotification(pn);
        }
    }

    private void purgeDeletedNotifications(String packageName, int id)
    {
        Log.d("NiLS","purging deleted notifications "+ packageName + ":" + id);

        Iterator<NotificationData> iter = notifications.iterator();
        while (iter.hasNext())
        {
            NotificationData nd = iter.next();
            if (nd.packageName.equals(packageName) &&
                nd.deleted &&
               !nd.protect &&
                (nd.id == id || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2))
            {
                Log.d("NiLS","permenantly removing uid:"+nd.uid);
                iter.remove();
            }
            // make sure next time it won't be protected from deleting
            nd.protect = false;
        }
    }

    public void onNotificationRemoved(Notification n, String packageName, int id)
    {
        removeNotification(packageName, id, false);

        // remove also persistent notification
        if (n != null && parser.isPersistent(n, packageName))
        {
            removePersistentNotification(packageName, id);
        }
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
        return mBinder;
    }

}