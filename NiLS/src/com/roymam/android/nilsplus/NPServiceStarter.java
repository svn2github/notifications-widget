package com.roymam.android.nilsplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.roymam.android.notificationswidget.NotificationsService;

public class NPServiceStarter extends BroadcastReceiver
{
    public void onReceive(Context context, Intent intent)
    {
        if (intent != null && intent.getAction() != null)
        {
            if (intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED))
            {
                // NiLS Floating Panel has been updated - restart the service
                context.startService(new Intent(context, NotificationsService.class));
            }
        }
    }
}
