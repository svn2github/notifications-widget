package com.roymam.android.notificationswidget;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.INotificationSideChannel;
import android.support.v4.app.NotificationCompatSideChannelService;
import android.util.Log;

public class NotificationsSideChannelService extends NotificationCompatSideChannelService {
    private static final String TAG = NotificationsSideChannelService.class.getSimpleName();

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

    public void onCreate() {
        Log.d(TAG, "onCreate");
        Intent intent = new Intent(this, NotificationsService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy");

        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getAction().equals("android.support.app.notification.BIND_SIDE_CHANNEL")) {
            return new NotificationSideChannelLegacyStub();
        }
        return super.onBind(intent);
    }

    @Override
    public void notify(String packageName, int id, String tag, Notification notification) {
        Log.d(TAG, "notify: packageName:"+packageName+ " id:"+id);
        if (!mBound)
            Log.e(TAG, "Notifications Service is not bounded. stop and restart NotificationsListener to rebind it");
        else {
            if (!packageName.equals(this.getPackageName())) // won't show NiLS internal side channel notifications
                mService.onNotificationPosted(notification, packageName, id, tag, true);
        }
    }

    @Override
    public void cancel(String packageName, int id, String tag) {
        Log.d(TAG, "cancel: packageName:"+packageName+ " id:"+id);
        if (!mBound)
            Log.e(TAG, "Notifications Service is not bounded. stop and restart NotificationsListener to rebind it");
        else {
            mService.onNotificationRemoved(null, packageName, id, null);
        }
    }

    @Override
    public void cancelAll(String packageName) {
        Log.d(TAG, "cancelAll: packageName:"+packageName);
        if (!mBound)
            Log.e(TAG, "Notifications Service is not bounded. stop and restart NotificationsListener to rebind it");
        else {
            mService.clearNotificationsForApps(new String[]{packageName});
        }
    }

    private class NotificationSideChannelLegacyStub
            extends INotificationSideChannel.Stub
    {
        private final String TAG = NotificationsSideChannelService.class.getSimpleName();
        private NotificationSideChannelLegacyStub() {}

        public void cancel(String packageName, int id, String tag)
                throws RemoteException
        {
            long l = Binder.clearCallingIdentity();
            try
            {
                NotificationsSideChannelService.this.cancel(packageName, id, tag);
                return;
            }
            finally
            {
                Binder.restoreCallingIdentity(l);
            }
        }

        public void cancelAll(String packageName)
        {
            long l = Binder.clearCallingIdentity();
            try
            {
                NotificationsSideChannelService.this.cancelAll(packageName);
                return;
            }
            finally
            {
                Binder.restoreCallingIdentity(l);
            }
        }

        public void notify(String packageName, int id, String tag, Notification notification)
                throws RemoteException
        {
            long l = Binder.clearCallingIdentity();
            try
            {
                NotificationsSideChannelService.this.notify(packageName, id, tag, notification);
                return;
            }
            finally
            {
                Binder.restoreCallingIdentity(l);
            }
        }
    }
}
