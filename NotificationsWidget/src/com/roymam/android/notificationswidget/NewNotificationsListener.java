package com.roymam.android.notificationswidget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


@TargetApi(18)
public class NewNotificationsListener extends NotificationListenerService implements NotificationsProvider
{
    private static NotificationsProvider instance = null;
    private NotificationEventListener listener;
    private ArrayList<NotificationData> notifications = new ArrayList<NotificationData>();
    private NotificationParser parser;
    private HashMap<String, PersistentNotification> persistentNotifications = new HashMap<String, PersistentNotification>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
        parser = new NotificationParser(getApplicationContext());
        getApplicationContext().sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_READY));
    }

    @Override
    public void onDestroy()
    {
        instance = null;
        getApplicationContext().sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_DIED));
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        Log.d("NiLS","onNotificationPosted");
        if (!parser.isPersistent(sbn.getNotification(), sbn.getPackageName()))
        {
            NotificationData nd = parser.parseNotification(sbn.getNotification(), sbn.getPackageName(), sbn.getId(), sbn.getTag());
            if (nd != null)
            {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                boolean keepOnlyLastNotification = prefs.getBoolean(nd.packageName+"."+AppSettingsActivity.KEEP_ONLY_LAST, false);
                String syncmode = prefs.getString(SettingsActivity.SYNC_NOTIFICATIONS, SettingsActivity.SYNC_NOTIFICATIONS_TWOWAY);

                boolean sync =  syncmode.equals(SettingsActivity.SYNC_NOTIFICATIONS_ONEWAY) ||
                                syncmode.equals(SettingsActivity.SYNC_NOTIFICATIONS_TWOWAY) ;
                boolean updated = false;

                // remove old notification
                Log.d("NiLS", "onNotificationPosted: iteration started");
                Iterator<NotificationData> iter = notifications.iterator();

                while (iter.hasNext())
                {
                    NotificationData oldnd = iter.next();

                    // remove only if one of the following scenarios:
                    // 1. sync is enabled and its the same package and id
                    // 2. notification is similar to the old one
                    // 3. user choose to keep only last notification
                    if (oldnd.packageName.equals(sbn.getPackageName())  &&
                            ((oldnd.id == sbn.getId() && sync) ||
                                    oldnd.isSimilar(nd) ||
                                    keepOnlyLastNotification))
                    {
                        nd.uid = oldnd.uid;
                        iter.remove();
                        updated = true;
                        break;
                    }
                }
                Log.d("NiLS", "onNotificationPosted: iteration finished");

                // add the new notification
                notifications.add(nd);

                // notify that the notification was added
                if (listener != null)
                {
                    if (updated)
                        listener.onNotificationUpdated(nd);
                    else
                        listener.onNotificationAdded(nd, true);
                    listener.onNotificationsListChanged();
                }
            }
        }
        else
        {
            PersistentNotification pn = parser.parsePersistentNotification(sbn.getNotification(), sbn.getPackageName(), sbn.getId());
            if (pn != null)
            {
                persistentNotifications.put(sbn.getPackageName(), pn);
                if (listener != null) listener.onPersistentNotificationAdded(pn);
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String sync = prefs.getString(SettingsActivity.SYNC_NOTIFICATIONS, SettingsActivity.SYNC_NOTIFICATIONS_SMART);
        if (sync.equals(SettingsActivity.SYNC_NOTIFICATIONS_TWOWAY) ||
            sync.equals(SettingsActivity.SYNC_NOTIFICATIONS_SMART) ||
            sync.equals(SettingsActivity.SYNC_NOTIFICATIONS_ONEWAY))
        {
            Log.d("NiLS","onNotificationRemoved");
            boolean cleared = false;

            ArrayList<NotificationData> clearedNotifications = new ArrayList<NotificationData>();

            // find the notification and remove it
            Log.d("NiLS", "onNotificationRemoved: iteration started");
            Iterator<NotificationData> iter = notifications.iterator();

            while (iter.hasNext())
            {
                NotificationData nd = iter.next();

                if (nd.packageName.equals(sbn.getPackageName()) && nd.id == sbn.getId() && !nd.pinned)
                {
                    // remove the notification
                    iter.remove();

                    // notify that the notification was cleared
                    clearedNotifications.add(nd);

                    cleared = true;
                    // do not stop loop - keep looping to clear all of the notifications with the same id
                }
            }


            Log.d("NiLS", "onNotificationRemoved: iteration finished");

            // notify listener for cleared notifications
            if (cleared && listener != null)
            {
                for(NotificationData nd : clearedNotifications)
                {
                    listener.onNotificationCleared(nd);
                }
                listener.onNotificationsListChanged();
            }


            // remove also persistent notification
            if (parser.isPersistent(sbn.getNotification(), sbn.getPackageName()))
            {
                if (persistentNotifications.containsKey(sbn.getPackageName()))
                {
                    PersistentNotification pn = persistentNotifications.get(sbn.getPackageName());
                    persistentNotifications.remove(sbn.getPackageName());
                    if (listener != null)
                    {
                        listener.onPersistentNotificationCleared(pn);
                    }
                }
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
        return notifications;
    }

    @Override
    public HashMap<String, PersistentNotification> getPersistentNotifications()
    {
        return persistentNotifications;
    }

    @Override
    public void clearAllNotifications()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean syncback = prefs.getString(SettingsActivity.SYNC_NOTIFICATIONS, SettingsActivity.SYNC_NOTIFICATIONS_TWOWAY).equals(SettingsActivity.SYNC_NOTIFICATIONS_TWOWAY);

        Log.d("NiLS", "clearAllNotifications: iteration started");
        ArrayList<NotificationData> clearedNotifications = new ArrayList<NotificationData>();

        Iterator<NotificationData> i = notifications.iterator();
        while (i.hasNext())
        {
            NotificationData nd = i.next();
            if (!nd.pinned)
            {
                clearedNotifications.add(nd);
                i.remove();
            }
        }
        Log.d("NiLS", "clearAllNotifications: iteration finished");

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

    @Override
    public synchronized void clearNotification(String packageName, int notificationId)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String sync = prefs.getString(SettingsActivity.SYNC_NOTIFICATIONS, SettingsActivity.SYNC_NOTIFICATIONS_SMART);
        boolean syncback = (sync.equals(SettingsActivity.SYNC_NOTIFICATIONS_TWOWAY) ||
                            sync.equals(SettingsActivity.SYNC_NOTIFICATIONS_SMART));

        boolean changed = false;

        // first, find it on list
        Log.d("NiLS", "clearNotification(packageName, notificationId): iteration started");
        Iterator<NotificationData> iter = notifications.iterator();
        ArrayList<NotificationData> clearedNotifications = new ArrayList<NotificationData>();
        while(iter.hasNext())
        {
            NotificationData nd = iter.next();

            if (nd.packageName.equals(packageName) && nd.id == notificationId)
            {
                iter.remove();
                clearedNotifications.add(nd);
                changed = true;

                // do not stop loop - keep searching for more notification with the same id
            }
        }
        Log.d("NiLS", "clearNotification(packageName, notificationId): iteration finished");

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
        String sync = prefs.getString(SettingsActivity.SYNC_NOTIFICATIONS, SettingsActivity.SYNC_NOTIFICATIONS_SMART);
        boolean syncback = (sync.equals(SettingsActivity.SYNC_NOTIFICATIONS_TWOWAY) ||
                sync.equals(SettingsActivity.SYNC_NOTIFICATIONS_SMART));
        boolean changed = false;
        ArrayList<NotificationData> clearedNotifications = new ArrayList<NotificationData>();

        for(String packageName : packages)
        {
            Log.d("NiLS", "clearNotificationsForApps: iteration started");
            ListIterator<NotificationData> i = notifications.listIterator();
            while (i.hasNext())
            {
                NotificationData nd = i.next();
                if (!nd.pinned && nd.packageName.equals(packageName))
                {
                    i.remove();
                    changed = true;
                    clearedNotifications.add(nd);
                }
            }
            Log.d("NiLS", "clearNotificationsForApps: iteration finished");
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String sync = prefs.getString(SettingsActivity.SYNC_NOTIFICATIONS, SettingsActivity.SYNC_NOTIFICATIONS_SMART);
        boolean syncback = (sync.equals(SettingsActivity.SYNC_NOTIFICATIONS_TWOWAY) ||
                sync.equals(SettingsActivity.SYNC_NOTIFICATIONS_SMART));

        // first, find it on list
        Log.d("NiLS", "clearNotification(uid): iteration started");
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

                iter.remove();
                removed = true;
            }
        }
        Log.d("NiLS", "clearNotification(uid): iteration finished");

        if (removed)
        {
            // search for more notification with the same id - if not found - dismiss the notifcation from android status bar
            boolean more = false;
            for(NotificationData nd : notifications)
            {
                if (nd.id == removedNd.id &&
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
    }
}
