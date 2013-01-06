package com.roymam.android.notificationswidget;

import java.util.List;

import android.app.Notification;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.view.accessibility.AccessibilityEvent;

public class NotificationsViewFactory implements RemoteViewsService.RemoteViewsFactory 
{
	private Context ctxt=null;
	private int appWidgetId;
	
	public NotificationsViewFactory(Context ctxt, Intent intent) 
	{
		this.ctxt=ctxt;
		appWidgetId=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
	}
	
	@Override
	public void onCreate() {
	// no-op
	}
	
	@Override
	public void onDestroy() {
	// no-op
	}
	
	@Override
	public int getCount() 
	{
		List<Notification> events = null;
		NotificationsService s = NotificationsService.getSharedInstance();
		if (s != null) 
		{
		        // The service is running and connected.
		        events = s.getNotifications();
		        if (events.size() > 0)
		        {
		        	System.out.println("Total Events:" + events.size());
		        	return(events.size());
		        }
		        else
		        {
		        	System.out.println("No Events");	        
		        	return 1;
		        }
		}
		else
		{
			System.out.println("No Events");	        
			return(1);
		}
	}
	
	@Override
	public RemoteViews getViewAt(int position) 
	{
		RemoteViews row=new RemoteViews(ctxt.getPackageName(), R.layout.dark_widget_item);	
		NotificationsService s = NotificationsService.getSharedInstance();
		String eventString = "No Notifications";
		if (s != null) 
		{
		    List<Notification> notifications = s.getNotifications();
		    if (notifications.size()>0)
		    {
		    	Notification n = notifications.get(position);
		    	eventString = n.tickerText.toString();
		    	row.setImageViewBitmap(R.id.notificationIcon, n.largeIcon);
		    }
		}
		row.setTextViewText(R.id.widget_item, eventString);		
	
		Intent i=new Intent();
		Bundle extras=new Bundle();
	
		extras.putString(NotificationsWidgetProvider.EXTRA_APP_ID, eventString);
		i.putExtras(extras);
		row.setOnClickFillInIntent(R.id.widget_item, i);
	
		return(row);
	}
	
	@Override
	public RemoteViews getLoadingView() {
	return(null);
	}
	
	@Override
	public int getViewTypeCount() {
	return(1);
	}
	
	@Override
	public long getItemId(int position) {
	return(position);
	}
	
	@Override
	public boolean hasStableIds() {
	return(true);
	}
	
	@Override
	public void onDataSetChanged() {
	// no-op
	}
}
