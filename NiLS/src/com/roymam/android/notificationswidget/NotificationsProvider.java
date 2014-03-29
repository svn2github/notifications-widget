package com.roymam.android.notificationswidget;

import java.util.HashMap;
import java.util.List;

public interface NotificationsProvider
{
    public final static String ACTION_SERVICE_READY = "com.roymam.android.nils.service_ready";
    public final static String ACTION_SERVICE_DIED = "com.roymam.android.nils.service_died";

    public List<NotificationData> getNotifications();
    public HashMap<String, PersistentNotification> getPersistentNotifications();
    public void clearAllNotifications();
    //public void clearNotification(String packageName, int notificationId);
    public void setNotificationEventListener(NotificationEventListener listener);
    public NotificationEventListener getNotificationEventListener();
    public void clearNotificationsForApps(String[] strings);
    public void clearNotification(int uid);
}
