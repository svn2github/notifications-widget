package com.roymam.android.notificationswidget;

public interface NotificationEventListener
{
    public void onNotificationAdded(NotificationData nd, boolean wake);
    public void onNotificationUpdated(NotificationData nd, boolean changed);
    public void onNotificationCleared(NotificationData nd);
    public void onNotificationsListChanged();
    public void onPersistentNotificationAdded(PersistentNotification pn);
    public void onPersistentNotificationCleared(PersistentNotification pn);

    public void onServiceStarted();
    public void onServiceStopped();

    public void registerProximitySensor(String packageName);
    public void stopProximityMontior();
}
