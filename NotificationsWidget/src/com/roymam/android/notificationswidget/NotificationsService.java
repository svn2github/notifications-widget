package com.roymam.android.notificationswidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class NotificationsService implements NotificationsProvider
{
    private static NotificationsProvider instance;
    private NotificationsProvider source;
    private NotificationAdapter adapter;
    private Context context;

    private NotificationsService(Context context, NotificationsProvider source)
    {
        this.source = source;
        this.context = context;
        this.adapter = new NotificationAdapter(context);
        setNotificationEventListener(adapter);
    }

    public static NotificationsProvider getSharedInstance(Context context)
    {
        if (instance == null)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            {
                if (NewNotificationsListener.getSharedInstance() != null)
                    instance = new NotificationsService(context, NewNotificationsListener.getSharedInstance());
            }
            else
            {
                if (NiLSAccessibilityService.getSharedInstance() != null)
                    instance = new NotificationsService(context, NiLSAccessibilityService.getSharedInstance());
            }
        }
        else // (instance != null)
        {
            // make sure that the source provider is still alive
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            {
                if (NewNotificationsListener.getSharedInstance() == null)
                    instance = null;
            }
            else
            {
                if (NiLSAccessibilityService.getSharedInstance() == null)
                    instance = null;
            }
        }
        return instance;
    }

    @Override
    public List<NotificationData> getNotifications()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        List<NotificationData> notifications = source.getNotifications();

        String sortBy = prefs.getString(SettingsActivity.NOTIFICATIONS_ORDER, "time");
        sortNotificationsList(notifications, sortBy);

        return notifications;
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
    public HashMap<String, PersistentNotification> getPersistentNotifications()
    {
        return source.getPersistentNotifications();
    }

    @Override
    public void clearAllNotifications()
    {
        source.clearAllNotifications();
    }

    @Override
    public void clearNotification(String packageName, int notificationId)
    {
        source.clearNotification(packageName, notificationId);
    }

    @Override
    public void setNotificationEventListener(NotificationEventListener listener)
    {
        source.setNotificationEventListener(listener);
    }

    @Override
    public NotificationEventListener getNotificationEventListener()
    {
        return source.getNotificationEventListener();
    }

    @Override
    public void clearNotificationsForApps(String[] apps)
    {
        source.clearNotificationsForApps(apps);
    }

    @Override
    public void clearNotification(int uid)
    {
        source.clearNotification(uid);
    }
}