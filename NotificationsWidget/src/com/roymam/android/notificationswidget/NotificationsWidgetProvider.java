/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.roymam.android.notificationswidget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

public class NotificationsWidgetProvider extends AppWidgetProvider 
{
	public static String NOTIFICATION_INDEX = "com.roymam.android.notificationswidget.notification_index";
    public static String CLEAR_ALL = "com.roymam.android.notificationswidget.clearall";
    public static String UPDATE_CLOCK = "com.roymam.android.notificationswidget.update_clock";
    public static String PERFORM_ACTION = "com.roymam.android.notificationswidget.performaction";
    public static int ACTIONBAR_TOGGLE = 0;
    public static int CLEAR_ACTION = 1;
    public static int PIN_ACTION = 3;
    public static int SETTINGS_ACTION = 2;
    
    public static boolean widgetActive = false;
	public static boolean widgetExpanded = false;
	
    public NotificationsWidgetProvider() 
    {
    }
	
    @Override
    public void onEnabled(Context context) 
    {    
	   widgetActive = true;
	   //notifyReady(context);
	   super.onEnabled(context);
       // register clock for tick events
	   context.getApplicationContext().registerReceiver(this, new IntentFilter(Intent.ACTION_TIME_TICK));
    }
    
    @Override
	public void onDeleted(Context context, int[] appWidgetIds) 
    {
		super.onDeleted(context, appWidgetIds);
	}

	@Override
	public void onDisabled(Context context) 
	{
		widgetActive = false;
		super.onDisabled(context);
	}

	public void updateWidget(Context ctx, AppWidgetManager appWidgetManager, boolean refreshList)
	{
		ComponentName thisWidget = new ComponentName(ctx, NotificationsWidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

	    // Build the intent to call the service
	    Intent intent = new Intent(ctx.getApplicationContext(), NotificationsWidgetService.class);
	    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
	    intent.putExtra(NotificationsWidgetService.REFRESH_LIST, refreshList);

	    // Update the widgets via the service
	    ctx.startService(intent);
	}
	
	@Override
    public void onReceive(Context ctx, Intent intent) 
    {  
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx.getApplicationContext());
		NotificationsService ns = NotificationsService.getSharedInstance();
	    
    	if (intent.getAction().equals(CLEAR_ALL))
    	{
    		if (ns != null)
    	    {
    	    	ns.clearAllNotifications();
    	    	ns.setSelectedIndex(-1);
    	    	updateWidget(ctx,appWidgetManager, true);
    	    }
    	}
    	else if (intent.getAction().equals(Intent.ACTION_TIME_TICK) ||
    			 intent.getAction().equals(UPDATE_CLOCK))
    	{
    		updateWidget(ctx, appWidgetManager, false);			
    	}
    	else if (intent.getAction().equals("com.teslacoilsw.widgetlocker.intent.LOCKED"))
    	{
    		if (ns!=null)
    		{
    			ns.setDeviceIsLocked();
    			ns.setWidgetLockerEnabled(true);
    		}
    	}
    	else if (intent.getAction().equals("com.teslacoilsw.widgetlocker.intent.UNLOCKED"))
    	{
    		if (ns != null)
    		{
    			ns.setDeviceIsUnlocked();
				ns.setSelectedIndex(-1);
				ns.setWidgetLockerEnabled(true);
    		}
    	}
    	else if (intent.getAction().equals("android.intent.action.SCREEN_ON"))
    	{
    		if (ns != null)
    		{
    			// if the screen is on, so the device is currently ocked (until USER_PRESENT will trigger)
    			ns.setDeviceIsLocked();
    		}
    	}
    	else if (intent.getAction().equals("com.teslacoilsw.widgetlocker.intent.DISABLED"))
    	{
    		if (ns != null)
    		{
    			ns.setWidgetLockerEnabled(false);
    		}
    	}
    	else if (intent.getAction().equals("com.teslacoilsw.widgetlocker.intent.ENABLED"))
    	{
    		if (ns != null)
    		{
    			ns.setWidgetLockerEnabled(true);
    		}
    	}
    	else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT))
    	{
    		if (ns != null)
    		{
    			if (!ns.isWidgetLockerEnabled())
    			{
    				ns.setDeviceIsUnlocked();
    				ns.setSelectedIndex(-1);
    			}
    		}
    	}
    	else if (intent.getAction().equals(PERFORM_ACTION))
    	{   
    		int pos = intent.getIntExtra(NOTIFICATION_INDEX, -1);
    		int action=intent.getIntExtra(NotificationsWidgetProvider.PERFORM_ACTION,-1);
    		    		
    		if (ns!=null)
    		{
    			if (action == ACTIONBAR_TOGGLE)
    			{
    				if (pos != ns.getSelectedIndex())
    					ns.setSelectedIndex(pos);
    				else
    					ns.setSelectedIndex(-1);
    			}
	    		if (action == CLEAR_ACTION && pos >= 0 && pos < ns.getNotificationsCount())
	    		{	    			
	    				ns.removeNotification(pos);
	    				ns.setSelectedIndex(-1);
	    		}
	    		else if (action == SETTINGS_ACTION && pos >= 0 && pos < ns.getNotificationsCount())
	    		{
	    			Intent appSettingsIntent = new Intent(ctx, AppSettingsActivity.class);
	    			appSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    			Bundle settingsExtras=new Bundle();			
					settingsExtras.putString(AppSettingsActivity.EXTRA_PACKAGE_NAME, intent.getStringExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME));
					appSettingsIntent.putExtras(settingsExtras);					
	    			ctx.startActivity(appSettingsIntent);
	    		}
	    		else if (action == PIN_ACTION && pos >= 0 && pos < ns.getNotificationsCount())
	    		{
	    			ns.togglePinNotification(pos);
	    		}	    		
	    		updateWidget(ctx, appWidgetManager, true);
    		}
    	}    	
    	super.onReceive(ctx, intent);
    }
	
	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) 
	{
		int currHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
		int hostCategory = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		// if the widget is collapsed on lock screen
		if (currHeight <=134 && hostCategory == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
		{
			// store this app widget id as collapsed mode
			prefs.edit().putString(SettingsActivity.WIDGET_MODE + "." + appWidgetId, SettingsActivity.COLLAPSED_WIDGET_MODE).commit();
			
			// refresh view if state changed
			if (widgetExpanded)
			{
				widgetExpanded = false;
				updateWidget(context, appWidgetManager, true);
			}
		}
		else if (hostCategory == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
		{
			// store this app widget id as expanded mode			
			prefs.edit().putString(SettingsActivity.WIDGET_MODE + "." + appWidgetId, SettingsActivity.EXPANDED_WIDGET_MODE).commit();
			// refresh view if state changed
			if (!widgetExpanded)
			{
				widgetExpanded = true;
				updateWidget(context, appWidgetManager, true);
			}
		}
		else
		{
			prefs.edit().putString(SettingsActivity.WIDGET_MODE + "." + appWidgetId, SettingsActivity.HOME_WIDGET_MODE).commit();
			updateWidget(context, appWidgetManager, true);
		}
	}

	

    @Override
    public void onUpdate(Context ctxt, AppWidgetManager appWidgetManager, int[] appWidgetIds) 
    {   
    	updateWidget(ctxt, appWidgetManager, false);
    	widgetActive = true;
    	super.onUpdate(ctxt, appWidgetManager, appWidgetIds);    		
    }
    
    public void notifyReady(Context ctx)
    {
		NotificationsService ns = NotificationsService.getSharedInstance();

    	NotificationCompat.Builder mBuilder =
    	        new NotificationCompat.Builder(ctx)
    	        .setSmallIcon(R.drawable.appicon)
    	        .setContentTitle("Congratulations!")
    	        .setContentText("You have successfully added NotificationsWidget")
    	        .setTicker("You have successfully added NotificationsWidget");

    	Intent resultIntent = new Intent(ctx, MainActivity.class);    	
    	PendingIntent resultPendingIntent =
    			PendingIntent.getActivity(ctx, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    	mBuilder.setContentIntent(resultPendingIntent);

    	// mId allows you to update the notification later on.
    	Notification n = mBuilder.build();
    	if (ns!=null)
    	{
    		ns.handleNotification(n, ctx.getPackageName());
    	}
    	else
    	{
        	NotificationManager mNotificationManager =
            	    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    		mNotificationManager.notify(0, mBuilder.build());
    	}
    }
}