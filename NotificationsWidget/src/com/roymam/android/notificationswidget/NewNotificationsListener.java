package com.roymam.android.notificationswidget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


@TargetApi(18)
public class NewNotificationsListener extends NotificationListenerService implements NotificationsProvider
{
    private static NotificationsProvider instance = null;
    private NotificationEventListener listener;
    private ArrayList<NotificationData> notifications = new ArrayList<NotificationData>();
    private NotificationParser parser;

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
        if (!parser.isPersistent(sbn.getNotification()))
        {
            NotificationData nd = parser.parseNotification(sbn.getNotification(), sbn.getPackageName(), sbn.getId(), sbn.getTag());
            if (nd != null)
            {
                // remove old notification with the same id
                for(NotificationData oldnd : notifications)
                {
                    if (oldnd.id == sbn.getId())
                    {
                        notifications.remove(oldnd);
                        break;
                    }
                }

                // add the new notification
                notifications.add(nd);

                // notify that the notification was added
                if (listener != null)
                {
                    listener.onNotificationAdded(nd);
                    listener.onNotificationsListChanged();
                }
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        Log.d("NiLS","onNotificationRemoved");
        // find the notification and remove it
        for (NotificationData nd : notifications)
        {
            if (nd.id == sbn.getId() && !nd.pinned)
            {
                // remove the notification
                notifications.remove(nd);

                // notify that the notification was added
                if (listener != null)
                {
                    listener.onNotificationCleared(nd);
                    listener.onNotificationsListChanged();
                }
                break;
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
        return new HashMap<String, PersistentNotification>();
    }

    @Override
    public void clearAllNotifications()
    {
        Iterator<NotificationData> i = notifications.iterator();
        while (i.hasNext())
        {
            NotificationData nd = i.next();
            if (!nd.pinned)
            {
                if (listener != null) listener.onNotificationCleared(nd);
                i.remove();
                // notify android to clear it too
                cancelNotification(nd.packageName, nd.tag, nd.id);
            }
        }

        if (listener != null) listener.onNotificationsListChanged();
    }

    @Override
    public void clearNotification(int notificationId)
    {
        // first, find it on list
        for(NotificationData nd : notifications)
        {
            if (nd.id == notificationId)
            {
                cancelNotification(nd.packageName, nd.tag, nd.id);
            }
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
    public void clearNotificationsForApps(String[] packages)
    {
        boolean changed = false;
        for(String packageName : packages)
        {
            Iterator<NotificationData> i = notifications.iterator();
            while (i.hasNext())
            {
                NotificationData nd = i.next();
                if (!nd.pinned && nd.packageName.equals(packageName))
                {
                    i.remove();
                    cancelNotification(packageName, nd.tag, nd.id);
                    changed = true;
                    if (listener != null) listener.onNotificationCleared(nd);
                }
            }
        }
        if (changed)
        {
            if (listener != null) listener.onNotificationsListChanged();
        }
    }
}
