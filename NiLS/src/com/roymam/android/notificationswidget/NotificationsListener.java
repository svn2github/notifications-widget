package com.roymam.android.notificationswidget;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


@TargetApi(18)
public class NotificationsListener extends NotificationListenerService
{
    private final String TAG = this.getClass().getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG,"NotificationsListener:onStartCommand");
        if (intent != null && intent.getAction() != null)
        {
            if (intent.getAction().equals(NotificationsService.CANCEL_NOTIFICATION))
            {
                String packageName = intent.getStringExtra(NotificationsService.EXTRA_PACKAGENAME);
                String tag = intent.getStringExtra(NotificationsService.EXTRA_TAG);
                int id = intent.getIntExtra(NotificationsService.EXTRA_ID, -1);

                Log.d(TAG,"cancel notification #" + id);
                try {
                    cancelNotification(packageName, tag, id);
                }
                catch(java.lang.SecurityException exp)
                {
                    Log.e(TAG, "security exception - cannot cancel notification.");
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate()
    {
        Log.d(TAG,"NotificationsListener:onCreate");

        // start NotificationsService
        //Intent intent = new Intent(getApplicationContext(), NotificationsService.class);
        //getApplicationContext().startService(intent);

        // Bind to NotificationsService
        Intent intent = new Intent(this, NotificationsService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        super.onCreate();
    }

    private NotificationsService mService;
    boolean mBound = false;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service)
        {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NotificationsService.LocalBinder binder = (NotificationsService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            mBound = false;
        }
    };


    @Override
    public void onDestroy()
    {
        Log.d(TAG, "NotificationsListener:onDestroy");

        // Unbind from the service
        if (mBound)
        {
            unbindService(mConnection);
            mBound = false;
        }

        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        Log.d(TAG,"onNotificationPosted package:"+sbn.getPackageName()+" id:" + sbn.getId());
        String str = NotificationCompat.getExtras(sbn.getNotification()).getString("android.title");
        Log.d(TAG, "title:"+str);

        if (!mBound)
            Log.e(TAG, "Notifications Service is not bounded. stop and restart NotificationsListener to rebind it");
        else
        {
            mService.onNotificationPosted(sbn.getNotification(), sbn.getPackageName(), sbn.getId(), sbn.getTag(), false);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        Log.d(TAG,"onNotificationRemoved package:"+sbn.getPackageName()+" id:" + sbn.getId());

        if (!mBound)
            Log.e(TAG, "Notifications Service is not bounded. stop and restart NotificationsListener to rebind it");
        else
        {
            mService.onNotificationRemoved(sbn.getNotification(), sbn.getPackageName(), sbn.getId(), sbn.getTag());
        }
    }
}
