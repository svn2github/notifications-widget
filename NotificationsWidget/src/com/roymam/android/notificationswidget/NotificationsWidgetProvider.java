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
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;
import com.roymam.android.notificationswidget.R;

public class NotificationsWidgetProvider extends AppWidgetProvider 
{
    public static String EXTRA_APP_ID = "com.roymam.android.notificationswidget.extraappid";
    public static String CLEAR_ALL = "com.roymam.android.notificationswidget.clearall";
    public static String UPDATE_CLOCK = "com.roymam.android.notificationswidget.update_clock";
    public static String ACTIVATE_SERVICE = "com.roymam.android.notificationswidget.activate_service";
    public static boolean widgetActive = false;
    
    public NotificationsWidgetProvider() 
    {
    }
    
    private PendingIntent clockPendingIntent = null;
    
    @Override
    public void onEnabled(Context context) 
    {    
    	// onEnabled doesn't work, need to check why
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

	@Override
    public void onReceive(Context ctx, Intent intent) 
    {    
    	if (intent.getAction().equals(CLEAR_ALL))
    	{
    		NotificationsService ns = NotificationsService.getSharedInstance();
    	    if (ns != null)
    	    {
    	    	ns.getNotifications().clear();
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
    	else if (intent.getAction().equals(UPDATE_CLOCK))
    	{
    		AppWidgetManager widgetManager = AppWidgetManager.getInstance(ctx);
			ComponentName widgetComponent = new ComponentName(ctx, NotificationsWidgetProvider.class);
			int[] appWidgetIds = widgetManager.getAppWidgetIds(widgetComponent);
			
			onUpdate(ctx, widgetManager, appWidgetIds);
			
    	}
    	else if (intent.getAction().equals(ACTIVATE_SERVICE))
    	{
    		Intent settingsIntent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
    		settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		ctx.startActivity(settingsIntent);
    	}
    	super.onReceive(ctx, intent);
    }
	
	private void populateTime(Context ctxt, RemoteViews widget, int hourId, int minuteId, int ampmId, int dateId)
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
	    widget.setTextViewText(dateId, datestr.toUpperCase());		
	}

    @Override
    public void onUpdate(Context ctxt, AppWidgetManager appWidgetManager, int[] appWidgetIds) 
    {    	
    	if (clockPendingIntent == null)
    	{
    		// create alarm for clock updates
    		//prepare Alarm Service to trigger Widget
    	   Intent intent = new Intent(UPDATE_CLOCK);
    	   clockPendingIntent = PendingIntent.getBroadcast(ctxt, 0, intent, 0);
    	   AlarmManager alarmManager = (AlarmManager)ctxt.getSystemService(Context.ALARM_SERVICE);
    	   Calendar calendar = Calendar.getInstance();
    	   calendar.setTimeInMillis(System.currentTimeMillis());
    	   calendar.add(Calendar.SECOND, 10);
    	   alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), 20*1000, clockPendingIntent);
    	   widgetActive = true;
    	}
    	
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

    	    // register click event on list
    	    Intent clickIntent=new Intent(ctxt, NotificationActivity.class);
    	    PendingIntent clickPI=PendingIntent.getActivity(ctxt, 0,
	                                            			clickIntent,
	                                            			PendingIntent.FLAG_UPDATE_CURRENT);
    	    widget.setPendingIntentTemplate(R.id.notificationsListView, clickPI);    	   

    	    // set up clock
    	    populateTime(ctxt, widget, R.id.timeHour, R.id.timeMinute, R.id.timeAMPM, R.id.dateFull);
    	    
		    // set up buttons
		    Intent clearIntent = new Intent(ctxt, NotificationsWidgetProvider.class);
		    clearIntent.setAction(NotificationsWidgetProvider.CLEAR_ALL);
		    widget.setOnClickPendingIntent(R.id.clearButton, 
		    		  PendingIntent.getBroadcast(ctxt, 0, clearIntent, PendingIntent.FLAG_UPDATE_CURRENT));
    	          	      
		    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
	    	widget.setOnClickPendingIntent(R.id.serviceInactiveButton, 
	    			  PendingIntent.getActivity(ctxt, 0, settingsIntent, Intent.FLAG_ACTIVITY_NEW_TASK));
		      
    	    // hide clock if required
    	    Boolean showClock = PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(SettingsActivity.SHOW_CLOCK, true);					
    	    Boolean showClearButton = PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(SettingsActivity.SHOW_CLEAR_BUTTON, true);					
    	      
    	    widget.setViewVisibility(R.id.clockbar, showClock.booleanValue()?View.VISIBLE:View.GONE);
    	    widget.setViewVisibility(R.id.clearButton, showClearButton.booleanValue()?View.VISIBLE:View.GONE); 

    	    // hide clear button if no notifications are displayed
    	    if (NotificationsService.getSharedInstance()==null ||
    	    	NotificationsService.getSharedInstance().getNotifications().size() == 0)
				{
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
}