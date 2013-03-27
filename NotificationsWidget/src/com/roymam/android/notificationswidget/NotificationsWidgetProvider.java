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

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

import com.roymam.android.notificationswidget.R;

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
    
    public NotificationsWidgetProvider() 
    {
    }
    
    private PendingIntent clockPendingIntent = null;
	
    @Override
    public void onEnabled(Context context) 
    {    
    	// create alarm for clock updates
		//prepare Alarm Service to trigger Widget
	   Intent intent = new Intent(UPDATE_CLOCK);
	   clockPendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
	   AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	   Calendar calendar = Calendar.getInstance();
	   calendar.setTimeInMillis(System.currentTimeMillis());
	   calendar.add(Calendar.SECOND, 10);
	   alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), 20*1000, clockPendingIntent);
	   widgetActive = true;
	   //notifyReady(context);
	   super.onEnabled(context);
    }
    
    @Override
	public void onDeleted(Context context, int[] appWidgetIds) 
    {
		super.onDeleted(context, appWidgetIds);
	}

	@Override
	public void onDisabled(Context context) 
	{
		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(clockPendingIntent);
		widgetActive = false;
		super.onDisabled(context);
	}

	public void updateWidget(Context ctx, boolean refreshList)
	{
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(ctx);
		ComponentName widgetComponent = new ComponentName(ctx, NotificationsWidgetProvider.class);
		int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
		
		if (refreshList)
		{
			for (int i=0; i<widgetIds.length; i++) 
	        {
				AppWidgetManager.getInstance(ctx).notifyAppWidgetViewDataChanged(widgetIds[i], R.id.notificationsListView);
	        }
		}
		onUpdate(ctx, widgetManager, widgetIds);
	}
	
	@Override
    public void onReceive(Context ctx, Intent intent) 
    {  
		//Toast.makeText(ctx, intent.getAction()+":"+intent.getIntExtra(NOTIFICATION_INDEX, -1), Toast.LENGTH_LONG).show();
		NotificationsService ns = NotificationsService.getSharedInstance();
	    
    	if (intent.getAction().equals(CLEAR_ALL))
    	{
    		if (ns != null)
    	    {
    	    	ns.clearAllNotifications();
    	    	ns.setSelectedIndex(-1);
    	    	updateWidget(ctx,true);
    	    }
    	}
    	else if (intent.getAction().equals(UPDATE_CLOCK))
    	{
    		updateWidget(ctx,false);			
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
	    		updateWidget(ctx,true);
    		}
    	}    	
    	super.onReceive(ctx, intent);
    }
	
	private void populateTime(Context ctxt, RemoteViews widget, int containerId, int hourId, int minuteId, int ampmId, int dateId)
	{
	    // set up clock
	    Time t = new Time();
	    t.setToNow();
	    String hourFormat = "%H";
	    String minuteFormat = ":%M";
	    String ampmstr = "";
    	if (!DateFormat.is24HourFormat(ctxt))
    	{
    		hourFormat = "%l";
    		minuteFormat = ":%M";
    		ampmstr = t.format("%p");
    	}
	    widget.setTextViewText(hourId, t.format(hourFormat));
	    widget.setTextViewText(minuteId, t.format(minuteFormat));
	    widget.setTextViewText(ampmId, ampmstr);		    
	    String datestr = DateFormat.format("EEE, MMMM dd", t.toMillis(true)).toString();
	    widget.setTextViewText(dateId, datestr.toUpperCase(Locale.getDefault()));
	    
	    // set clock text color
	    int color = Resources.getSystem().getColor(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(ctxt).getString(SettingsActivity.CLOCK_COLOR, String.valueOf(android.R.color.white))));
	    widget.setTextColor(hourId, color);
	    widget.setTextColor(minuteId, color);
	    widget.setTextColor(ampmId, color);
	    widget.setTextColor(dateId, color);
	    widget.setTextColor(R.id.alarmtime, color);
	    
	    // add alarm clock intent
	    PackageManager packageManager = ctxt.getPackageManager();
	    Intent alarmClockIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

	    // Verify clock implementation
	    String clockImpls[][] = 
	    	{
	            {"HTC Alarm Clock", "com.htc.android.worldclock", "com.htc.android.worldclock.WorldClockTabControl" },
	            {"Standar Alarm Clock", "com.android.deskclock", "com.android.deskclock.AlarmClock"},
	            {"Moto Blur Alarm Clock", "com.motorola.blur.alarmclock",  "com.motorola.blur.alarmclock.AlarmClock"},
	            {"Samsung Galaxy Clock", "com.sec.android.app.clockpackage","com.sec.android.app.clockpackage.ClockPackage"},
	            {"Froyo Nexus Alarm Clock", "com.google.android.deskclock", "com.android.deskclock.DeskClock"}
	    };

	    boolean foundClockImpl = false;

	    for(int i=0; i<clockImpls.length; i++) 
	    {
	        String vendor = clockImpls[i][0];
	        String packageName = clockImpls[i][1];
	        String className = clockImpls[i][2];
	        try 
	        {
	            ComponentName cn = new ComponentName(packageName, className);
	            ActivityInfo aInfo = packageManager.getActivityInfo(cn, PackageManager.GET_META_DATA);
	            alarmClockIntent.setComponent(cn);
	            foundClockImpl = true;
	        } catch (NameNotFoundException e) 
	        {	            
	        }
	    }

	    if (foundClockImpl) {
	        PendingIntent pendingIntent = PendingIntent.getActivity(ctxt, 0, alarmClockIntent, 0);
	        widget.setOnClickPendingIntent(containerId, pendingIntent);	       
	    }
	}

    @Override
    public void onUpdate(Context ctxt, AppWidgetManager appWidgetManager, int[] appWidgetIds) 
    {    		
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
		NotificationsService ns = NotificationsService.getSharedInstance();
		widgetActive = true;

    	for (int i=0; i<appWidgetIds.length; i++) 
    	{
    		RemoteViews widget=new RemoteViews(ctxt.getPackageName(), R.layout.widget_layout);
    	    
    		// hide loading spinner
    		widget.setViewVisibility(R.id.loadingSpinner, View.GONE);
    		
    		// set up notifications list
    		Intent svcIntent=new Intent(ctxt, NotificationsWidgetService.class);
    	    svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
    	    svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
    	    widget.setRemoteAdapter(R.id.notificationsListView, svcIntent);

    	    // set up clock
    	    populateTime(ctxt, widget, R.id.smallClock, R.id.timeHour, R.id.timeMinute, R.id.timeAMPM, R.id.dateFull);
    	    populateTime(ctxt, widget, R.id.bigClock, R.id.bigHours, R.id.bigminutes, R.id.timeAMPM, R.id.bigDate);
    	    
    	    // display next alarm if needed
    	    String nextAlarm = Settings.System.getString(ctxt.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
    	    if (!nextAlarm.equals(""))
    	    {
    	    	widget.setViewVisibility(R.id.nextAlarmContainer, View.VISIBLE);
    	    	widget.setTextViewText(R.id.alarmtime, "⏰" + nextAlarm.toUpperCase(Locale.getDefault()));
    	    }
    	    else
    	    {
    	    	widget.setViewVisibility(R.id.nextAlarmContainer, View.GONE);
    	    }
		    // set up buttons
		    Intent clearIntent = new Intent(ctxt, NotificationsWidgetProvider.class);
		    clearIntent.setAction(NotificationsWidgetProvider.CLEAR_ALL);
		    widget.setOnClickPendingIntent(R.id.clearButton, 
		    		  PendingIntent.getBroadcast(ctxt, 0, clearIntent, PendingIntent.FLAG_UPDATE_CURRENT));
    	          	      
		    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
	    	widget.setOnClickPendingIntent(R.id.serviceInactiveButton, 
	    			  PendingIntent.getActivity(ctxt, 0, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT));
		     	    	
    	    // hide clock if required
    	    String clockstyle = prefs.getString(SettingsActivity.CLOCK_STYLE, SettingsActivity.CLOCK_AUTO);					
    	    String clearButtonMode = prefs.getString(SettingsActivity.CLEAR_BUTTON_MODE, "visible");					
    	   
    	    int notificationsCount = 0;
    	    String notifiationsStyle = prefs.getString(SettingsActivity.NOTIFICATION_STYLE, "normal");
    	        	    
    	    if (ns!=null)
    	    {
    	    	notificationsCount = ns.getNotificationsCount();    	    	
    	    }
    	    
    	    if (!prefs.getBoolean(SettingsActivity.DISABLE_NOTIFICATION_CLICK, false))
    	    {
    	    	Intent clickIntent=new Intent(ctxt, NotificationActivity.class);
    	    	PendingIntent clickPI=PendingIntent.getActivity(ctxt, 0,
	                                            			clickIntent,
	                                            			PendingIntent.FLAG_UPDATE_CURRENT);
    	    	widget.setPendingIntentTemplate(R.id.notificationsListView, clickPI);  
    	    }
    	    else
    	    {
    	    	Intent clickIntent=new Intent(NotificationsWidgetProvider.PERFORM_ACTION);
    	    	PendingIntent clickPI=PendingIntent.getBroadcast(ctxt, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    	    	widget.setPendingIntentTemplate(R.id.notificationsListView, clickPI);    	   
    	    }
    			
    	    if (clockstyle.equals(SettingsActivity.CLOCK_SMALL) ||
    	    	clockstyle.equals(SettingsActivity.CLOCK_AUTO) && 
    	    		(notifiationsStyle.equals("large") && notificationsCount > 0 ||
    	    		 notifiationsStyle.equals("normal") && notificationsCount > 1 || 
    	    				notificationsCount > 2 || 
    	    				ns != null && ns.getSelectedIndex() >= 0))
    	    {
        	    widget.setViewVisibility(R.id.smallClock, View.VISIBLE);
        	    widget.setViewVisibility(R.id.bigClock, View.GONE);
    	    } else if (clockstyle.equals(SettingsActivity.CLOCK_LARGE) ||
        	    	clockstyle.equals(SettingsActivity.CLOCK_AUTO) && 
        	    	(notifiationsStyle.equals("large") && notificationsCount == 0 || 
        	    	 notifiationsStyle.equals("normal") && notificationsCount <= 1 ||
        	    	 notificationsCount <= 2 ))
    	    {
        	    widget.setViewVisibility(R.id.smallClock, View.GONE);
        	    widget.setViewVisibility(R.id.bigClock, View.VISIBLE);
    	    }else
    	    {
        	    widget.setViewVisibility(R.id.smallClock, View.GONE);
        	    widget.setViewVisibility(R.id.bigClock, View.GONE);
    	    }
    	    widget.setViewVisibility(R.id.clearButton, (clearButtonMode.equals("visible"))?View.VISIBLE:View.GONE); 
    	    
    	    // hide clear button if no notifications are displayed
    	    if (notificationsCount == 0)
    	    	{
	    	    	widget.setViewVisibility(R.id.clearButton, View.GONE);   
				  	if (ns==null )
				  	{
				  		widget.setViewVisibility(R.id.serviceInactiveButton, View.VISIBLE);
				  		widget.setViewVisibility(R.id.serviceInactiveView, View.VISIBLE);
				  		widget.setViewVisibility(R.id.notificationsListView, View.GONE);
				  	}
				  	else
				  	{
				  		widget.setViewVisibility(R.id.serviceInactiveButton, View.GONE);   
				  		widget.setViewVisibility(R.id.serviceInactiveView, View.GONE);
				  		widget.setViewVisibility(R.id.notificationsListView, View.VISIBLE);
				  	}
				}
    	
		      appWidgetManager.updateAppWidget(appWidgetIds[i], widget);    	      
    	    }
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