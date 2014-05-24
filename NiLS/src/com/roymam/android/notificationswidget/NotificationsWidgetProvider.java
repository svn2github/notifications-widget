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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.List;

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
    public static boolean widgetExpanded = false;

    public NotificationsWidgetProvider() 
    {
    }
	
    @Override
    public void onEnabled(Context context) 
    {    
       super.onEnabled(context);
       PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(SettingsManager.WIDGET_PRESENT, true).commit();
    }
    
    @Override
	public void onDeleted(Context context, int[] appWidgetIds) 
    {
		super.onDeleted(context, appWidgetIds);
		int[] remainingIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, NotificationsWidgetProvider.class));
        if (remainingIds == null || remainingIds.length == 0) 
        {
            context.stopService(new Intent(context, NotificationsWidgetService.class));
            //Toast.makeText(context, "Timer stopped!!", Toast.LENGTH_SHORT).show();
        }
	}

	@Override
	public void onDisabled(Context context) 
	{
		NotificationsWidgetService.widgetActive = false;
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(SettingsManager.WIDGET_PRESENT, false).commit();
		super.onDisabled(context);
		context.stopService(new Intent(context, NotificationsWidgetService.class));
	}

	private void updateWidget(Context ctx, boolean refreshList)
	{
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx.getApplicationContext());
		ComponentName thisWidget = new ComponentName(ctx, NotificationsWidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

	    // Build the intent to call the service
	    Intent intent = new Intent(ctx.getApplicationContext(), NotificationsWidgetService.class);
	    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
	    intent.putExtra(NotificationsWidgetService.REFRESH_LIST, refreshList);
	    intent.putExtra(NotificationsWidgetService.ACTION, NotificationsWidgetService.ACTION_RENDER_WIDGETS);

	    // Update the widgets via the service
	    ctx.startService(intent);
	}
	
	@Override
    public void onReceive(Context ctx, Intent intent) 
    {  	    
    	if (intent.getAction().equals(CLEAR_ALL))
    	{
    		NotificationsProvider ns = NotificationsService.getSharedInstance();

    		if (ns != null)
    	    {
    	    	ns.clearAllNotifications();
    	    }
    	}
    	else if (intent.getAction().equals(Intent.ACTION_TIME_TICK) ||
    			 intent.getAction().equals(UPDATE_CLOCK))
    	{
    		updateWidget(ctx, false);
    	}

    	else if (intent.getAction().equals(PERFORM_ACTION))
    	{
    		NotificationsProvider ns = NotificationsService.getSharedInstance();
            int pos = intent.getIntExtra(NOTIFICATION_INDEX, -1);
    		int action=intent.getIntExtra(NotificationsWidgetProvider.PERFORM_ACTION,-1);
    		    		
    		if (ns!=null)
    		{
                List<NotificationData> notifications = ns.getNotifications();

    			if (action == ACTIONBAR_TOGGLE)
    			{
                    if (pos >= 0 && pos < notifications .size())
                    {
                        NotificationData nd = notifications .get(pos);
                        if (nd.selected)
                            nd.selected = false;
                        else
                        {
                            // deselect current first
                            for(NotificationData nd2 : notifications )
                            {
                                nd2.selected = false;
                            }
                            // select current
                            nd.selected = true;
                        }
                        updateWidget(ctx,true);
                    }
    			}
	    		if (action == CLEAR_ACTION && pos >= 0 && pos < notifications.size())
	    		{
	    			ns.clearNotification(ns.getNotifications().get(pos).uid);
                    updateWidget(ctx, true);
	    		}
	    		else if (action == SETTINGS_ACTION && pos >= 0 && pos < notifications.size())
	    		{
	    			Intent appSettingsIntent = new Intent(ctx, AppSettingsActivity.class);
	    			appSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    			Bundle settingsExtras=new Bundle();			
					settingsExtras.putString(AppSettingsActivity.EXTRA_PACKAGE_NAME, intent.getStringExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME));
					appSettingsIntent.putExtras(settingsExtras);					
	    			ctx.startActivity(appSettingsIntent);
	    		}
	    		else if (action == PIN_ACTION && pos >= 0 && pos < notifications.size())
	    		{
                    NotificationData nd = notifications.get(pos);
                    if (!nd.pinned) nd.pinned = true;
                    else nd.pinned = false;
                    updateWidget(ctx,true);
	    		}	    		
    		}
    	}
    	super.onReceive(ctx, intent);
    }
	
	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) 
	{
		Resources res = context.getResources();
		boolean isExpanded = (newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                >= res.getDimensionPixelSize(R.dimen.min_expanded_height) /
                res.getDisplayMetrics().density);
		int hostCategory = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY);
	
		if ((isExpanded && !widgetExpanded || !isExpanded && widgetExpanded) && 
				hostCategory==AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD ||
				hostCategory==AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN )
		{
			widgetExpanded = isExpanded;			
			// Build the intent to call the service
		    Intent intent = new Intent(context.getApplicationContext(), NotificationsWidgetService.class);
		    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		    intent.putExtra(NotificationsWidgetService.IS_EXPANDED, isExpanded);
		    intent.putExtra(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, hostCategory);
		    intent.putExtra(NotificationsWidgetService.ACTION, NotificationsWidgetService.ACTION_OPTIONS_CHANGED);
	
		    // Update the widgets via the service
		    context.startService(intent);
		}
	}

    @Override
    public void onUpdate(Context ctxt, AppWidgetManager appWidgetManager, int[] appWidgetIds) 
    {       	
    	updateWidget(ctxt, false);
    	NotificationsWidgetService.widgetActive = true;
    	super.onUpdate(ctxt, appWidgetManager, appWidgetIds);    		
    }
      
}