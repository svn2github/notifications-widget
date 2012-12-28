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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.ArrayAdapter;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

import com.roymam.android.notificationswidget.R;

public class NotificationsWidgetProvider extends AppWidgetProvider 
{
    public static String CLICK_ACTION = "com.roymam.android.notificationswidget.click";
    public static String NOTIFICATION_CREATED_ACTION = "com.roymam.android.notificationswidget.NOTIFICATION_CREATED";
    public static String EXTRA_APP_ID = "com.roymam.android.notificationswidget.extraappid";

    public NotificationsWidgetProvider() 
    {
    }

    @Override
    public void onEnabled(Context context) 
    {
    	RemoteViews remoteViews = new RemoteViews( context.getPackageName(), R.layout.widget_layout);
    	ComponentName watchWidget = new ComponentName( context, NotificationsWidgetProvider.class );
        AppWidgetManager.getInstance(context).updateAppWidget( watchWidget, remoteViews);
        
        Intent svcIntent=new Intent(context, NotificationsWidgetService.class);
//        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
        svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));   
        remoteViews.setRemoteAdapter(R.id.notificationsListView, svcIntent);

        Intent clickIntent=new Intent(context, NotificationActivity.class);
        PendingIntent clickPI=PendingIntent.getActivity(context, 0,
                                                  		clickIntent,
                                                  		PendingIntent.FLAG_UPDATE_CURRENT);
            
        remoteViews.setPendingIntentTemplate(R.id.notificationsListView, clickPI);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) 
    {
        final String action = intent.getAction();
        if (action.equals(CLICK_ACTION)) 
        {
            // TODO - Open the app that was clicked
            final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            final String appId = intent.getStringExtra(EXTRA_APP_ID);
            final String formatStr = ctx.getResources().getString(R.string.toast_format_string);
            Toast.makeText(ctx, String.format(formatStr, appId), Toast.LENGTH_SHORT).show();
        } 
        else if (action.equals(NOTIFICATION_CREATED_ACTION )) 
        {
        	String s = intent.getStringExtra("NotificationString");
        	System.out.println("Notification Receieved to Widget:"+s);
        	
        	RemoteViews remoteViews = new RemoteViews( ctx.getPackageName(), R.layout.widget_layout);
        	ComponentName watchWidget = new ComponentName( ctx, NotificationsWidgetProvider.class );
            //remoteViews.setTextViewText( R.id.center_text, s);
            AppWidgetManager.getInstance(ctx).updateAppWidget( watchWidget, remoteViews);            
        }

        super.onReceive(ctx, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) 
    {
    	super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}