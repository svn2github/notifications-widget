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
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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
    public static String SWITCH_TO_EDIT_MODE = "com.roymam.android.notificationswidget.switchmode";
    public static String PERFORM_ACTION = "com.roymam.android.notificationswidget.performaction";
    
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
    	if (intent.getAction().equals(CLEAR_ALL))
    	{
    		NotificationsService ns = NotificationsService.getSharedInstance();
    	    if (ns != null)
    	    {
    	    	ns.getNotifications().clear();
    	    	ns.setEditMode(false);
    	    	updateWidget(ctx,true);
    	    }
    	}
    	else if (intent.getAction().equals(UPDATE_CLOCK))
    	{
    		updateWidget(ctx,false);
			
    	}
    	else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT))
    	{
    		if (NotificationsService.getSharedInstance() != null)
    		{
    			NotificationsService.getSharedInstance().setDeviceIsUnlocked();
    			NotificationsService.getSharedInstance().setEditMode(false);
    		}
    	}
    	
    	else if (intent.getAction().equals(PERFORM_ACTION))
    	{   
    		int pos=intent.getIntExtra(NotificationsWidgetProvider.NOTIFICATION_INDEX,-1);
    		int action=intent.getIntExtra(NotificationsWidgetProvider.PERFORM_ACTION,0);
    		
    		if (NotificationsService.getSharedInstance()!=null)
    		{
	    		if (action == 1 && pos >= 0 && pos < NotificationsService.getSharedInstance().getNotifications().size())
	    		{	    			
	    				NotificationsService.getSharedInstance().getNotifications().remove(pos);
	    				if (NotificationsService.getSharedInstance().getNotifications().size()==0)
	    				{
	    					NotificationsService.getSharedInstance().setEditMode(false);
	    				}
	    		}
	    		else if (action == 2 && pos >= 0 && pos < NotificationsService.getSharedInstance().getNotifications().size())
	    		{
	    			Intent appSettingsIntent = new Intent(ctx, AppSettingsActivity.class);
	    			appSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    			Bundle settingsExtras=new Bundle();			
					settingsExtras.putString(AppSettingsActivity.EXTRA_PACKAGE_NAME, intent.getStringExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME));
					appSettingsIntent.putExtras(settingsExtras);					
	    			ctx.startActivity(appSettingsIntent);
	    			NotificationsService.getSharedInstance().setEditMode(false);
	    		}
	    		else
	    		{
	    			NotificationsService.getSharedInstance().setEditMode(false);
	    		}
	    		updateWidget(ctx,true);
    		}
    	}
    	else if (intent.getAction().equals(SWITCH_TO_EDIT_MODE))
    	{   
    		if (NotificationsService.getSharedInstance()!=null)
    		{
	    		// switch mode
    			boolean editMode = NotificationsService.getSharedInstance().isEditMode();
	    		if (!editMode)
	    		{
	    			editMode = true;
	    		}
	    		else
	    		{
	    			editMode = false;
	    		}
	    		
				// update notifications view
				NotificationsService.getSharedInstance().setEditMode(editMode);
				
				AppWidgetManager widgetManager = AppWidgetManager.getInstance(ctx);
				ComponentName widgetComponent = new ComponentName(ctx, NotificationsWidgetProvider.class);
				int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
				
				for (int i=0; i<widgetIds.length; i++) 
	            {
					AppWidgetManager.getInstance(ctx).notifyAppWidgetViewDataChanged(widgetIds[i], R.id.notificationsListView);
	            }
				onUpdate(ctx, widgetManager, widgetIds);
				
				
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
    	    	widget.setTextViewText(R.id.alarmtime, nextAlarm.toUpperCase(Locale.getDefault()));
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
		     
	    	Intent editModeIntent = new Intent(NotificationsWidgetProvider.SWITCH_TO_EDIT_MODE);
	    	widget.setOnClickPendingIntent(R.id.editMode, 
	    			  PendingIntent.getBroadcast(ctxt, 0, editModeIntent, PendingIntent.FLAG_UPDATE_CURRENT));
		     
    	    // hide clock if required
    	    String clockstyle = PreferenceManager.getDefaultSharedPreferences(ctxt).getString(SettingsActivity.CLOCK_STYLE, SettingsActivity.CLOCK_AUTO);					
    	    Boolean showEditButton = PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(SettingsActivity.SHOW_EDIT_BUTTON, true);
    	    String clearButtonMode = PreferenceManager.getDefaultSharedPreferences(ctxt).getString(SettingsActivity.CLEAR_BUTTON_MODE, "visible");					
    	   
    	    int notificationsCount = 0;
    	    String notifiationsStyle = PreferenceManager.getDefaultSharedPreferences(ctxt).getString(SettingsActivity.NOTIFICATION_STYLE, "normal");
    	    
    	    boolean editMode = false;
    	    if (NotificationsService.getSharedInstance()!=null)
    	    {
    	    	notificationsCount = NotificationsService.getSharedInstance().getNotifications().size();
    	    	
    	    	// register click event on list
        	    editMode = NotificationsService.getSharedInstance().isEditMode();
    	    }
    	    
    	    if (!editMode )
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
    	    				notificationsCount > 2))
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
    	    widget.setViewVisibility(R.id.clearButton, (clearButtonMode.equals("visible") || clearButtonMode.equals("editmode") && editMode)?View.VISIBLE:View.GONE); 
    	    widget.setViewVisibility(R.id.editMode, showEditButton.booleanValue()?View.VISIBLE:View.GONE); 

    	    // hide clear button if no notifications are displayed
    	    if (notificationsCount == 0)
    	    	{
	    	    	widget.setViewVisibility(R.id.editMode, View.GONE);   
	    	    	widget.setViewVisibility(R.id.clearButton, View.GONE);   
				  	if (NotificationsService.getSharedInstance()==null )
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
    	NotificationCompat.Builder mBuilder =
    	        new NotificationCompat.Builder(ctx)
    	        .setSmallIcon(R.drawable.appicon)
    	        .setContentTitle("Congratulations!")
    	        .setContentText("You successfully added NotificationsWidget");

    	Intent resultIntent = new Intent(ctx, MainActivity.class);    	
    	PendingIntent resultPendingIntent =
    			PendingIntent.getActivity(ctx, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    	mBuilder.setContentIntent(resultPendingIntent);

    	// mId allows you to update the notification later on.
    	Notification n = mBuilder.build();
    	if (NotificationsService.getSharedInstance()!=null)
    	{
    		NotificationsService.getSharedInstance().handleNotification(n, ctx.getPackageName());
    	}
    	else
    	{
        	NotificationManager mNotificationManager =
            	    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    		mNotificationManager.notify(0, mBuilder.build());
    	}
    }
}