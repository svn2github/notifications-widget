package com.roymam.android.notificationswidget;

import java.util.List;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.text.format.Time;

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
	public void onCreate() 
	{
	// no-op
	}
	
	@Override
	public void onDestroy() 
	{
	// no-op
	}
	
	@Override
	public int getCount() 
	{
		List<NotificationData> events = null;
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

	private Bitmap ConvertToBlackAndWhite(Bitmap sampleBitmap)
	{
		ColorMatrix bwMatrix =new ColorMatrix();
		bwMatrix.setSaturation(0);
		final ColorMatrixColorFilter colorFilter= new ColorMatrixColorFilter(bwMatrix);
		Bitmap rBitmap = sampleBitmap.copy(Bitmap.Config.ARGB_8888, true);
		Paint paint=new Paint();
		paint.setColorFilter(colorFilter);
		Canvas myCanvas =new Canvas(rBitmap);
		myCanvas.drawBitmap(rBitmap, 0, 0, paint);
		return rBitmap;
	}
	
	@Override
	public RemoteViews getViewAt(int position) 
	{
		RemoteViews row=new RemoteViews(ctxt.getPackageName(), R.layout.dark_widget_item);	
		NotificationsService s = NotificationsService.getSharedInstance();
		String eventString = "No Notifications";
		row.setTextViewText(R.id.notificationCount, "");
    	row.setTextViewText(R.id.notificationTime, "");
		if (s != null) 
		{
		    List<NotificationData> notifications = s.getNotifications();
		    if (notifications.size()>0)
		    {
		    	NotificationData n = notifications.get(position);
		    	eventString = n.text;
		    	row.setImageViewBitmap(R.id.notificationIcon, n.icon);
		    	row.setImageViewBitmap(R.id.appIcon, n.appicon);		    	
		    	if (n.count > 1)
		    		row.setTextViewText(R.id.notificationCount, Integer.toString(n.count));
		    	else
		    		row.setTextViewText(R.id.notificationCount, null);
		    	Time t = new Time();
		    	t.set(n.received);
		    	row.setTextViewText(R.id.notificationTime, t.format("%H:%M"));
				Intent i=new Intent();
				Bundle extras=new Bundle();			
				extras.putInt(NotificationsWidgetProvider.EXTRA_APP_ID,position);
				i.putExtras(extras);
				row.setOnClickFillInIntent(R.id.widget_item, i);
		    }
		}
		else
		{
			eventString = "Service inactive";
		}

		row.setTextViewText(R.id.widget_item, eventString);		
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
