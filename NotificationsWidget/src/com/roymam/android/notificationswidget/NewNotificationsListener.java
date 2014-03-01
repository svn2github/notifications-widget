package com.roymam.android.notificationswidget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.List;


@TargetApi(18)
public class NewNotificationsListener extends NotificationListenerService
{
    private NotificationParser parser;
    //private BroadcastReceiver receiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d("NiLS","NewNotificationsListener:onStartCommand");
        if (intent != null && intent.getAction() != null)
        {
            if (intent.getAction().equals(NotificationsService.CANCEL_NOTIFICATION))
            {
                String packageName = intent.getStringExtra(NotificationsService.EXTRA_PACKAGENAME);
                String tag = intent.getStringExtra(NotificationsService.EXTRA_TAG);
                int id = intent.getIntExtra(NotificationsService.EXTRA_ID, -1);

                Log.d("NiLS","cancel notification #" + id);
                cancelNotification(packageName, tag, id);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate()
    {
        Log.d("NiLS","NewNotificationsListener:onCreate");

        // start NotificationsService
        Intent intent = new Intent(getApplicationContext(), NotificationsService.class);
        getApplicationContext().startService(intent);

        // create a notification parser
        parser = new NotificationParser(getApplicationContext());

        // register a receiver for cancel notification action
        /*receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                Log.d("NiLS","NewNotificationsListener:onReceive");
                if (intent.getAction() != null)
                {
                    if (intent.getAction().equals(NotificationsService.CANCEL_NOTIFICATION))
                    {
                        String packageName = intent.getStringExtra(NotificationsService.EXTRA_PACKAGENAME);
                        String tag = intent.getStringExtra(NotificationsService.EXTRA_TAG);
                        int id = intent.getIntExtra(NotificationsService.EXTRA_ID, -1);

                        Log.d("NiLS","cancel notification #" + id);
                        cancelNotification(packageName, tag, id);
                    }
                }
            }
        };
        registerReceiver(receiver, new IntentFilter(NotificationsService.CANCEL_NOTIFICATION));*/

        // notify that the service has been started
        getApplicationContext().sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_READY));

        super.onCreate();
    }

    @Override
    public void onDestroy()
    {
        Log.d("NiLS","NewNotificationsListener:onDestroy");
        getApplicationContext().sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_DIED));
        /*if (receiver != null)
        {
            unregisterReceiver(receiver);
            receiver = null;
        }*/
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        if (!parser.isPersistent(sbn.getNotification(), sbn.getPackageName()))
        {
            Log.d("NiLS","NewNotificationsListener:onNotificationPosted #" + sbn.getId());

            List<NotificationData> notifications = parser.parseNotification(sbn.getNotification(), sbn.getPackageName(), sbn.getId(), sbn.getTag());
            for (NotificationData n : notifications)
            {
                Intent intent = new Intent(getApplicationContext(), NotificationsService.class);
                intent.setAction(NotificationsService.NOTIFICATION_POSTED);
                intent.putExtra(NotificationsService.EXTRA_NOTIFICATION, n);
                getApplicationContext().startService(intent);
            }
        }
        else
        {
            PersistentNotification pn = parser.parsePersistentNotification(sbn.getNotification(), sbn.getPackageName(), sbn.getId());

            Intent intent = new Intent(getApplicationContext(), NotificationsService.class);
            intent.setAction(NotificationsService.PERSISTENT_NOTIFICATION_POSTED);
            intent.putExtra(NotificationsService.EXTRA_NOTIFICATION, pn);
            getApplicationContext().startService(intent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        Log.d("NiLS","NewNotificationsListener:onNotificationRemoved #" + sbn.getId());

        Intent intent = new Intent(getApplicationContext(), NotificationsService.class);
        intent.setAction(NotificationsService.NOTIFICATION_REMOVED);
        intent.putExtra(NotificationsService.EXTRA_PACKAGENAME, sbn.getPackageName());
        intent.putExtra(NotificationsService.EXTRA_ID, sbn.getId());
        getApplicationContext().startService(intent);

        // remove also persistent notification
        if (parser.isPersistent(sbn.getNotification(), sbn.getPackageName()))
        {
            intent = new Intent(getApplicationContext(), NotificationsService.class);
            intent.setAction(NotificationsService.PERSISTENT_NOTIFICATION_REMOVED);
            intent.putExtra(NotificationsService.EXTRA_PACKAGENAME, sbn.getPackageName());
            intent.putExtra(NotificationsService.EXTRA_ID, sbn.getId());
            getApplicationContext().startService(intent);
        }
    }
}
