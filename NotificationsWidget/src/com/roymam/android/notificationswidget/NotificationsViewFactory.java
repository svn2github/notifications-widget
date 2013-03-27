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
import android.opengl.Visibility;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
		        return(s.getNotificationsCount());		        
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
		    if (s.getNotificationsCount() >0 && position < s.getNotificationsCount())
		    {
		    	NotificationData n = s.getNotification(position);		    		    
				
		    	// set on click intent 
				Intent i=new Intent();
				Bundle extras=new Bundle();			
				extras.putInt(NotificationsWidgetProvider.NOTIFICATION_INDEX,position);
				i.putExtras(extras);
				row.setOnClickFillInIntent(R.id.notificationContainer, i);							
								
				// prepare action bar
				createActionBar(row,position,n);
				
				// set notification style
				int textColor = Resources.getSystem().getColor(Integer.parseInt(preferences.getString("notification_text_color", String.valueOf(android.R.color.white))));
				int timeColor = Resources.getSystem().getColor(Integer.parseInt(preferences.getString("notification_time_color", String.valueOf(android.R.color.holo_blue_dark))));				
				
				String notStyle = preferences.getString(SettingsActivity.NOTIFICATION_STYLE, "normal");
				RemoteViews styleView;
				int iconId = R.id.notificationIcon;
				
				if (notStyle.equals("large"))
				{
					if (n.originalNotification != null)
					{
						styleView = n.largeNotification;
						
						// change style for large notification
						// set background to transparent (the item background will be shown instead)						
						if (n.layoutId != 0) n.originalNotification.setInt(n.layoutId , "setBackgroundColor", Color.TRANSPARENT);
						if (n.hasTime) n.originalNotification.setTextColor(16908388, timeColor);
						if (n.hasTitle) n.originalNotification.setTextColor(s.notification_title_id, textColor); 
						if (n.hasSubtitle) n.originalNotification.setTextColor(s.notification_subtext_id, textColor);
						if (n.hasText) n.originalNotification.setTextColor(s.notification_text_id, textColor);
						if (n.hasBigText) n.originalNotification.setTextColor(s.big_notification_content_text, textColor);
					//if (n.hasImage) iconId = s.notification_image_id;	
					}
					else
					{
						styleView = n.normalNotification;
					}
				}
				else if (notStyle.equals("normal"))
				{
					styleView = n.normalNotification;
					int maxLines = Integer.parseInt(preferences.getString(SettingsActivity.MAX_LINES, "2"));
					styleView.setInt(R.id.notificationText, "setMaxLines", maxLines);			    	
				}
				else
				{
					styleView = n.smallNotification;				
				}
				
				// add style view
				row.removeAllViews(R.id.notificationContainer);
				row.addView(R.id.notificationContainer, styleView);
				
				// customize style
				int opacity = preferences.getInt(SettingsActivity.NOTIFICATION_BG_OPACITY, 75);				
				row.setInt(R.id.notificationBG, "setBackgroundColor", Color.argb(opacity * 255 / 100, 20, 20, 20));
				
				styleView.setTextColor(R.id.notificationText, textColor);
				styleView.setTextColor(R.id.notificationTime, timeColor);
				styleView.setTextColor(R.id.notificationCount, textColor);	
				if (n.pinned)
				{
					styleView.setViewVisibility(R.id.pinIcon, View.VISIBLE);
					styleView.setViewVisibility(R.id.notificationCount, View.GONE);
				}
				else
				{
					styleView.setViewVisibility(R.id.pinIcon, View.GONE);	
					styleView.setViewVisibility(R.id.notificationCount, View.VISIBLE);
				}
								
				// set action bar intent
				Intent editModeIntent = new Intent(NotificationsWidgetProvider.PERFORM_ACTION);					
				editModeIntent.putExtra(NotificationsWidgetProvider.PERFORM_ACTION,NotificationsWidgetProvider.ACTIONBAR_TOGGLE);
				editModeIntent.putExtra(NotificationsWidgetProvider.NOTIFICATION_INDEX, position);
				styleView.setOnClickPendingIntent(
						iconId, 
						PendingIntent.getBroadcast(ctxt, NotificationsWidgetProvider.ACTIONBAR_TOGGLE+position*10, editModeIntent, PendingIntent.FLAG_UPDATE_CURRENT));			    	
				
		    }
		}	
		return(row);
	}
	
	private void createActionBar(RemoteViews row, int position, NotificationData n) 
	{
		row.removeAllViews(R.id.actionbarContainer);
		RemoteViews actionBar = new RemoteViews(ctxt.getPackageName(),R.layout.notification_actionbar);
		row.addView(R.id.actionbarContainer, actionBar);	
		
		if (NotificationsService.getSharedInstance().getSelectedIndex() == position)
		{
			row.setViewVisibility(R.id.actionbarContainer, View.VISIBLE);
		}
		else
		{
			row.setViewVisibility(R.id.actionbarContainer, View.GONE);
		}
		
		// set app settings intent
		Intent appSettingsIntent = new Intent(NotificationsWidgetProvider.PERFORM_ACTION);					
		appSettingsIntent.putExtra(NotificationsWidgetProvider.PERFORM_ACTION,NotificationsWidgetProvider.SETTINGS_ACTION);
		appSettingsIntent.putExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME, n.packageName);
		appSettingsIntent.putExtra(NotificationsWidgetProvider.NOTIFICATION_INDEX, position);
		actionBar.setOnClickPendingIntent(
				R.id.actionSettings, 
				PendingIntent.getBroadcast(ctxt, NotificationsWidgetProvider.SETTINGS_ACTION+position*10, appSettingsIntent, PendingIntent.FLAG_UPDATE_CURRENT));			    	

		// set pin notification intent
		Intent pinIntent = new Intent(NotificationsWidgetProvider.PERFORM_ACTION);					
		pinIntent.putExtra(NotificationsWidgetProvider.PERFORM_ACTION,NotificationsWidgetProvider.PIN_ACTION);
		pinIntent.putExtra(NotificationsWidgetProvider.NOTIFICATION_INDEX, position);
		actionBar.setOnClickPendingIntent(
				R.id.actionPin, 
				PendingIntent.getBroadcast(ctxt, NotificationsWidgetProvider.PIN_ACTION+position*10, pinIntent, PendingIntent.FLAG_UPDATE_CURRENT));			    	

		// set clear notification intent
		Intent clearIntent = new Intent(NotificationsWidgetProvider.PERFORM_ACTION);					
		clearIntent.putExtra(NotificationsWidgetProvider.PERFORM_ACTION,NotificationsWidgetProvider.CLEAR_ACTION);
		clearIntent.putExtra(NotificationsWidgetProvider.NOTIFICATION_INDEX, position);
		actionBar.setOnClickPendingIntent(
				R.id.actionClear, 
				PendingIntent.getBroadcast(ctxt, NotificationsWidgetProvider.CLEAR_ACTION+position*10, clearIntent, PendingIntent.FLAG_UPDATE_CURRENT));
		
		// hide clear button for pinned notifications
		if (NotificationsService.getSharedInstance().getNotification(position).pinned)
		{
			actionBar.setViewVisibility(R.id.actionClear, View.GONE);	
			actionBar.setTextViewText(R.id.actionPin, ctxt.getText(R.string.unpin));			
		}
		else
		{
			actionBar.setViewVisibility(R.id.actionClear, View.VISIBLE);
			actionBar.setTextViewText(R.id.actionPin, ctxt.getText(R.string.pin));			
		}
		
		// add custom app action
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctxt);
		
		// if it's expanded large notification - don't show buttons in actions bar 
		if (!(sharedPref.getBoolean(n.packageName+"."+AppSettingsActivity.USE_EXPANDED_TEXT, sharedPref.getBoolean(AppSettingsActivity.USE_EXPANDED_TEXT, true))
		      && sharedPref.getString(SettingsActivity.NOTIFICATION_STYLE, "normal").equals("large"))		
			&& n.actions != null)
			{
				if (n.actions.length >= 1)
				{
					actionBar.setImageViewBitmap(R.id.customAction1, n.actions[0].drawable);
					actionBar.setOnClickPendingIntent(R.id.customAction1, n.actions[0].actionIntent);
					actionBar.setViewVisibility(R.id.customAction1, View.VISIBLE);
				}
				else
				{
					actionBar.setViewVisibility(R.id.customAction1, View.GONE);
				}
				
				if (n.actions.length >= 2)
				{
					actionBar.setImageViewBitmap(R.id.customAction2, n.actions[1].drawable);
					actionBar.setOnClickPendingIntent(R.id.customAction2, n.actions[1].actionIntent);
					actionBar.setViewVisibility(R.id.customAction2, View.VISIBLE);
				}
				else
				{
					actionBar.setViewVisibility(R.id.customAction2, View.GONE);
				}
			}
			else
			{
				actionBar.setViewVisibility(R.id.customAction1, View.GONE);
				actionBar.setViewVisibility(R.id.customAction2, View.GONE);
			}
	}

	@Override
	public RemoteViews getLoadingView() 
	{
		return(null);
	}
	
	@Override
	public int getViewTypeCount() 
	{
		return(1);
	}
	
	@Override
	public long getItemId(int position) 
	{
		return(position);
	}
	
	@Override
	public boolean hasStableIds() 
	{
		return(true);
	}
	
	@Override
	public void onDataSetChanged() 
	{
		// no-op
	}
}
