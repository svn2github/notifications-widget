package com.roymam.android.notificationswidget;

public interface NotificationEventListener
{
    public void onNotificationAdded(NotificationData nd);
    public void onNotificationCleared(NotificationData nd);
    public void onNotificationsListChanged();
    public void onPersistentNotificationAdded(PersistentNotification pn);

    public void onServiceStarted();
    public void onServiceStopped();
}
