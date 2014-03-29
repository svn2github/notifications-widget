package com.roymam.android.notificationswidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class ClockService extends Service
{
    private BroadcastReceiver clockReceiver;
    private PendingIntent runningAppsPendingIntent;

    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public static boolean activated = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        // register clock update
        clockReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context arg0, Intent arg1)
            {
                Intent intent = new Intent(NotificationsWidgetProvider.UPDATE_CLOCK);
                getApplicationContext().sendBroadcast(intent);
            }
        };
        
        getApplicationContext().registerReceiver(clockReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        activated = true;
        super.onCreate();
    }

    @Override
    public void onDestroy()
    {
        getApplicationContext().unregisterReceiver(clockReceiver);
        activated = false;
        super.onDestroy();
    }

}
