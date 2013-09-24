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
    private final String WIDGET_LOCKER_LOCKED = "com.teslacoilsw.widgetlocker.intent.OCKED";
    public final static String FN_DISMISS_NOTIFICATIONS = "robj.floating.notifications.dismissed";
    public final static String DISMISS_NOTIFICATIONS = "com.roymam.android.nils.remove_notification";
    public final static String OPEN_NOTIFICATION = "com.roymam.android.nils.open_notification";
    public final static String RESEND_ALL_NOTIFICATIONS = "com.roymam.android.nils.resend_all_notifications";
    public final static String ADD_NOTIFICATION = "com.roymam.android.nils.add_notification";
    public final static String PING = "com.roymam.android.nils.ping";
    public final static String ALIVE = "com.roymam.android.nils.alive";

    public void onReceive(Context context, Intent intent)
    {
        if (intent != null)
        {
           NotificationsProvider ns = NotificationsService.getSharedInstance(context);
           SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

           String action = intent.getAction();
           if (action.equals(Intent.ACTION_USER_PRESENT) && !sharedPref.getBoolean("widgetlocker", false) ||
               action.equals(WIDGET_LOCKER_UNLOCKED))
           {
                // clear all notifications if needed
                if (sharedPref.getBoolean(SettingsActivity.CLEAR_ON_UNLOCK, false))
                {
                    if (ns != null)
                        ns.clearAllNotifications();
                }
                   if (action.equals(WIDGET_LOCKER_UNLOCKED))
                       sharedPref.edit().putBoolean("widgetlocker", true).commit();
           }
           else if (action.equals(WIDGET_LOCKER_LOCKED))
           {
               sharedPref.edit().putBoolean("widgetlocker", true).commit();
           }
           else if (action.equals(DISMISS_NOTIFICATIONS) || action.equals(FN_DISMISS_NOTIFICATIONS))
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
                    int id = intent.getIntExtra("id",-1);
                    if (id > -1)
                    {
                        Log.d("NiLS", "remove notification package:" + packageName +" #" + id);
                        if (ns != null) ns.clearNotification(packageName, id);
                    }
                    else
                        if (ns != null) ns.clearNotificationsForApps(new String[]{packageName});
                }
            }
            else if (intent.getAction().equals(RESEND_ALL_NOTIFICATIONS))
            {
                if (ns != null)
                for(int i = ns.getNotifications().size()-1; i>=0; i--)
                {
                    NotificationData nd = ns.getNotifications().get(i);
                    ns.getNotificationEventListener().onNotificationAdded(nd, false);
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
               if (NotificationsService.getSharedInstance(context) != null)
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
