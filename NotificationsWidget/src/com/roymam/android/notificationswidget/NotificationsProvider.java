package com.roymam.android.notificationswidget;

import java.util.HashMap;
import java.util.List;

public interface NotificationsProvider
{
    public List<NotificationData> getNotifications();
    public HashMap<String, PersistentNotification> getPersistentNotifications();
    public void clearAllNotifications();
    public void clearNotification(int notificationId);
    public void setNotificationEventListener(NotificationEventListener listener);
    public void clearNotificationsForApps(String[] strings);
}
