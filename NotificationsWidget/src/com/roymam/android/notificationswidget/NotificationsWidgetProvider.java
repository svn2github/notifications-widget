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
import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.widget.ArrayAdapter;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import com.roymam.android.notificationswidget.R;

public class NotificationsWidgetProvider extends AppWidgetProvider 
{
    public static String EXTRA_APP_ID = "com.roymam.android.notificationswidget.extraappid";
    public static String CLEAR_ALL = "com.roymam.android.notificationswidget.clearall";
    public static String UPDATE_CLOCK = "com.roymam.android.notificationswidget.UPDATE_CLOCK";
    
    public NotificationsWidgetProvider() 
    {
    }
    
    private PendingIntent clockPendingIntent = null;
    
    @Override
    public void onEnabled(Context context) 
    {    
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
    	    }
    	}
    	else if (intent.getAction().equals(UPDATE_CLOCK))
    	{
    		AppWidgetManager widgetManager = AppWidgetManager.getInstance(ctx);
			ComponentName widgetComponent = new ComponentName(ctx, NotificationsWidgetProvider.class);
			int[] appWidgetIds = widgetManager.getAppWidgetIds(widgetComponent);
			
			onUpdate(ctx, widgetManager, appWidgetIds);
			
    	}
    	super.onReceive(ctx, intent);
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
    	   alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 20*1000, clockPendingIntent);
    	}
    	for (int i=0; i<appWidgetIds.length; i++) {
    	      Intent svcIntent=new Intent(ctxt, NotificationsWidgetService.class);
    	      
    	      svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
    	      svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
    	      
    	      RemoteViews widget=new RemoteViews(ctxt.getPackageName(),
    	                                          R.layout.widget_layout);
    	      
    	      widget.setRemoteAdapter(appWidgetIds[i], R.id.notificationsListView,
    	                              svcIntent);

    	      Intent clickIntent=new Intent(ctxt, NotificationActivity.class);
    	      PendingIntent clickPI=PendingIntent
    	                              .getActivity(ctxt, 0,
    	                                            clickIntent,
    	                                            PendingIntent.FLAG_UPDATE_CURRENT);
    	      // set up clock
    	      Time t = new Time();
    	      t.setToNow();
    	      widget.setTextViewText(R.id.timeHour, t.format("%H"));
    	      widget.setTextViewText(R.id.timeMinute, t.format(":%M"));
    	      String datestr = DateFormat.format("EEE, MMMM dd", t.toMillis(true)).toString();
		      widget.setTextViewText(R.id.dateFull, datestr.toUpperCase());
		      widget.setPendingIntentTemplate(R.id.notificationsListView, clickPI);

		      // set up click events
		      Intent clearIntent = new Intent(ctxt, NotificationsWidgetProvider.class);
		      clearIntent.setAction(NotificationsWidgetProvider.CLEAR_ALL);
		      
		      widget.setOnClickPendingIntent(R.id.clearButton, 
		    		  PendingIntent.getBroadcast(ctxt, 0, clearIntent, PendingIntent.FLAG_UPDATE_CURRENT));
    	      appWidgetManager.updateAppWidget(appWidgetIds[i], widget);
    	      
    	      
    	    }
    		super.onUpdate(ctxt, appWidgetManager, appWidgetIds);
    		
    }
}