package com.roymam.android.notificationswidget;

import java.util.List;

import android.R.anim;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.text.format.DateFormat;
import android.text.format.Time;

public class NotificationsViewFactory implements RemoteViewsService.RemoteViewsFactory 
{
	private Context ctxt=null;
	public NotificationsViewFactory(Context ctxt, Intent intent) 
	{
		this.ctxt=ctxt;
		intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
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
		        return(events.size());		        
		}
		else
		{
			return(0);
		}
	}

	@SuppressWarnings("unused")
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
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctxt);
		if (s != null) 
		{
		    List<NotificationData> notifications = s.getNotifications();
		    if (notifications.size()>0)
		    {
		    	NotificationData n = notifications.get(position);
		    	row.setImageViewBitmap(R.id.notificationIcon, n.icon);
		    	row.setImageViewBitmap(R.id.appIcon, n.appicon);	
		    	row.setTextViewText(R.id.widget_item, n.text);		
		    	if (n.count > 1)
		    		row.setTextViewText(R.id.notificationCount, Integer.toString(n.count));
		    	else
		    		row.setTextViewText(R.id.notificationCount, null);
		    	Time t = new Time();
		    	t.set(n.received);
		    	String timeFormat = "%H:%M";
		    	if (!DateFormat.is24HourFormat(ctxt))
		    		timeFormat = "%l:%M%P";
		    	row.setTextViewText(R.id.notificationTime, t.format(timeFormat));
				Intent i=new Intent();
				Bundle extras=new Bundle();			
				extras.putInt(NotificationsWidgetProvider.EXTRA_APP_ID,position);
				i.putExtras(extras);
				row.setOnClickFillInIntent(R.id.notificationContainer, i);
				row.setOnClickFillInIntent(R.id.widget_item, i);
				row.setOnClickFillInIntent(R.id.largeNotification, i);
				
				
				// set opacity by preference
				int opacity = preferences.getInt(SettingsActivity.NOTIFICATION_BG_OPACITY, 75);
				row.setInt(R.id.smallNotification, "setBackgroundColor", Color.argb(opacity * 255 / 100, 20, 20, 20));
				
				// set colors by preferences
				int textColor = Integer.parseInt(preferences.getString("notification_text_color", String.valueOf(android.R.color.white)));
				int timeColor = Integer.parseInt(preferences.getString("notification_time_color", String.valueOf(android.R.color.holo_blue_dark)));
				row.setTextColor(R.id.widget_item, Resources.getSystem().getColor(textColor));
				row.setTextColor(R.id.notificationTime, Resources.getSystem().getColor(timeColor));
				
				row.removeAllViews(R.id.largeNotification);
				row.addView(R.id.largeNotification, n.notificationContent);
				
				if (preferences.getBoolean("showfullnotification", false))
				{
					row.setViewVisibility(R.id.largeNotification, View.VISIBLE);
					row.setViewVisibility(R.id.smallNotification, View.GONE);
				}
				else
				{
					row.setViewVisibility(R.id.largeNotification, View.GONE);
					row.setViewVisibility(R.id.smallNotification, View.VISIBLE);
				}
		    }
		}	
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
