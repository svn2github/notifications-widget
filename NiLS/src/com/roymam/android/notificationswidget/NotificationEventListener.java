package com.roymam.android.notificationswidget;

public interface NotificationEventListener
{
    public void onNotificationAdded(NotificationData nd, boolean wake, Boolean covered);
    public void onNotificationUpdated(NotificationData nd, boolean changed, Boolean covered);
    public void onNotificationCleared(NotificationData nd, boolean more);
    public void onNotificationsListChanged();
    public void onPersistentNotificationAdded(PersistentNotification pn);
    public void onPersistentNotificationCleared(PersistentNotification pn);

    public void onServiceStarted();
    public void onServiceStopped();
}
