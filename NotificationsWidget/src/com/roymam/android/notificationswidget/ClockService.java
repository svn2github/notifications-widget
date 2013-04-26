package com.roymam.android.notificationswidget;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class ClockService extends Service 
{
    @Override
    public void onCreate() 
    {
    	BroadcastReceiver bReceiver = new BroadcastReceiver() 
    	{
    		@Override
    		public void onReceive(Context context, Intent intent) 
    		{
    			sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));    			
    		}
    	};
  
    	IntentFilter intentFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
    	registerReceiver(bReceiver, intentFilter);
    }

	@Override
	public IBinder onBind(Intent arg0) 
	{		
		return null;
	}
}
