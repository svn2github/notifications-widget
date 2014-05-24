package com.roymam.android.notificationswidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;

public class EventsHandler extends BroadcastReceiver
{
    private final String WIDGET_LOCKER_UNLOCKED = "com.teslacoilsw.widgetlocker.intent.UNLOCKED";
    private final String WIDGET_LOCKER_LOCKED = "com.teslacoilsw.widgetlocker.intent.LOCKED";
    public final static String FN_DISMISS_NOTIFICATIONS = "robj.floating.notifications.dismissed";
    public final static String DISMISS_NOTIFICATIONS = "com.roymam.android.nils.remove_notification";
    public final static String OPEN_NOTIFICATION = "com.roymam.android.nils.open_notification";
    public final static String PING = "com.roymam.android.nils.ping";
    public final static String ALIVE = "com.roymam.android.nils.alive";

    public void onReceive(Context context, Intent intent)
    {
        if (intent != null)
        {
           Log.d("NiLS", "EventsHandler:onReceive "+intent.getAction());
           NotificationsProvider ns = NotificationsService.getSharedInstance();
           SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

           String action = intent.getAction();

            if (action.equals(DISMISS_NOTIFICATIONS) || action.equals(FN_DISMISS_NOTIFICATIONS))
            {
                int uid = intent.getIntExtra("uid", -1);
                if (uid != -1)
                {
                    Log.d("NiLS", "remove notification uid:" + uid);
                    if (ns != null) ns.clearNotification(uid);
                }
                else
                {
                    String packageName = intent.getStringExtra("package");
                    if (ns != null) ns.clearNotificationsForApps(new String[]{packageName});
                }
            }
            else if (intent.getAction().equals(OPEN_NOTIFICATION))
            {
                String packageName = intent.getStringExtra("package");
                int id = intent.getIntExtra("id",-1);
                if (id > -1 && ns != null && packageName != null)
                {
                    Log.d("NiLS", "open notification #" + id);
                    launchNotificationById(context, ns.getNotifications(), packageName, id);
                }
            }
            else if (intent.getAction().equals(NotificationsProvider.ACTION_SERVICE_READY))
            {
                // previous call to NotificationsService.getSharedInstance(context) has already
                // connected to the new service listener
                // notify that the service was started
                if (ns != null && ns.getNotificationEventListener() != null)
                    ns.getNotificationEventListener().onServiceStarted();
            }
            else if (intent.getAction().equals(NotificationsProvider.ACTION_SERVICE_DIED))
            {
                // previous call to NotificationsService.getSharedInstance(context) has already
                // connected to the new service listener
                // notify that the service was stopped
                if (ns != null && ns.getNotificationEventListener() != null)
                    ns.getNotificationEventListener().onServiceStopped();
            }
           else if (intent.getAction().equals(PING))
           {
               if (NotificationsService.getSharedInstance() != null)
                    context.sendBroadcast(new Intent(ALIVE));
           }
        }
    }

    private void launchNotificationById(Context context, List<NotificationData> notifications, String packageName, int id)
    {
        for(int i=0; i< notifications.size(); i++)
        {
            NotificationData nd = notifications.get(i);

            if (nd.id == id && nd.packageName.equals(packageName))
            {
                nd.launch(context);
            }
        }
    }
}
