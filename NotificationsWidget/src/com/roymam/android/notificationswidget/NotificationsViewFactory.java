package com.roymam.android.notificationswidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class NotificationsViewFactory implements RemoteViewsService.RemoteViewsFactory 
{
	private Context ctxt=null;
	private int widgetId;
	public NotificationsViewFactory(Context ctxt, Intent intent) 
	{
		this.ctxt=ctxt;
		widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
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
		RemoteViews row=new RemoteViews(ctxt.getPackageName(), R.layout.listitem_notification);	
		NotificationsService s = NotificationsService.getSharedInstance();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctxt);
		String widgetMode = preferences.getString(SettingsActivity.WIDGET_MODE + "." + widgetId, SettingsActivity.EXPANDED_WIDGET_MODE);

		if (s != null) 
		{		   
		    if (s.getNotificationsCount() >0 && position < s.getNotificationsCount())
		    {
		    	NotificationData n = s.getNotification(position);		    		    
				
		    	// set on click intent 
		    	if (preferences.getBoolean(widgetMode + "." + SettingsActivity.NOTIFICATION_IS_CLICKABLE, true))
		    	{
					Intent i=new Intent();
					Bundle extras=new Bundle();			
					extras.putInt(NotificationsWidgetProvider.NOTIFICATION_INDEX,position);
					i.putExtras(extras);
					row.setOnClickFillInIntent(R.id.notificationContainer, i);							
		    	}
				// prepare action bar
				createActionBar(row,position,n);
				
				// set notification style
				String notStyle = preferences.getString(widgetMode + "." + SettingsActivity.NOTIFICATION_STYLE, widgetMode.equals(SettingsActivity.COLLAPSED_WIDGET_MODE)?"compact":"normal");
				RemoteViews styleView;
				
				if (notStyle.equals("large"))
				{
					styleView = new RemoteViews(ctxt.getPackageName(), R.layout.notification_large);
				}
				else if (notStyle.equals("normal"))
				{
					styleView = new RemoteViews(ctxt.getPackageName(), R.layout.notification_normal);
		 		}
				else
				{
					styleView = new RemoteViews(ctxt.getPackageName(), R.layout.notification_compact);
				}

				// fill notification with text
				int maxLines = preferences.getInt(widgetMode + "." + SettingsActivity.MAX_LINES, 1);

				fillNotificationWithText(styleView, n, notStyle, maxLines);

				int bgColor = preferences.getInt(widgetMode + "." +SettingsActivity.NOTIFICATION_BG_COLOR, Color.BLACK);			
				int defaultOpacity = widgetMode.equals(SettingsActivity.COLLAPSED_WIDGET_MODE)?0:50;
				int opacity = preferences.getInt(widgetMode + "." + SettingsActivity.NOTIFICATION_BG_OPACITY, defaultOpacity);				
				row.setInt(R.id.notificationBG, "setBackgroundColor", Color.argb(opacity * 255 / 100, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor)));

				customizeNotificationView(styleView, n);
				
				// add pinned icon (if needed)
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
				
				// add style view
				row.removeAllViews(R.id.notificationContainer);
				row.addView(R.id.notificationContainer, styleView);
								
				// set action bar intent
				if (preferences.getBoolean(widgetMode + "." +SettingsActivity.NOTIFICATION_ICON_IS_CLICKABLE, true))
				{
					styleView.setViewVisibility(R.id.notificationSpinner, View.VISIBLE);
					Intent editModeIntent = new Intent(NotificationsWidgetProvider.PERFORM_ACTION);					
					editModeIntent.putExtra(NotificationsWidgetProvider.PERFORM_ACTION,NotificationsWidgetProvider.ACTIONBAR_TOGGLE);
					editModeIntent.putExtra(NotificationsWidgetProvider.NOTIFICATION_INDEX, position);
					if (notStyle.equals("compact"))
					{
						styleView.setOnClickPendingIntent(
								R.id.appIcon, 
								PendingIntent.getBroadcast(ctxt, NotificationsWidgetProvider.ACTIONBAR_TOGGLE+position*10, editModeIntent, PendingIntent.FLAG_UPDATE_CURRENT));			    	
					}
					else
					{
						styleView.setOnClickPendingIntent(
							R.id.notificationIcon, 
							PendingIntent.getBroadcast(ctxt, NotificationsWidgetProvider.ACTIONBAR_TOGGLE+position*10, editModeIntent, PendingIntent.FLAG_UPDATE_CURRENT));			    	
					}
				}
				else
				{
					styleView.setViewVisibility(R.id.notificationSpinner, View.GONE);
				}
		    }
		}	
		return(row);
	}
	
	private void customizeNotificationView(RemoteViews styleView, NotificationData n) 
	{		
	}

	private void fillNotificationWithText(RemoteViews n, NotificationData nd, String type, int maxLines) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
		String widgetMode = prefs.getString(SettingsActivity.WIDGET_MODE + "." + widgetId, SettingsActivity.EXPANDED_WIDGET_MODE);
		int titleColor = prefs.getInt(widgetMode + "." +SettingsActivity.TITLE_COLOR, Color.WHITE);
		int textColor = prefs.getInt(widgetMode + "." +SettingsActivity.TEXT_COLOR, Color.LTGRAY);
		int contentColor = prefs.getInt(widgetMode + "." +SettingsActivity.CONTENT_COLOR, Color.DKGRAY);

		n.setImageViewBitmap(R.id.notificationIcon, nd.icon);
		n.setImageViewBitmap(R.id.appIcon, nd.appicon);
		n.setTextViewText(R.id.notificationTitle, nd.title);
		CharSequence text = nd.text;
		
		if (type.equals("compact") && titleColor != Color.TRANSPARENT)
		{
			// combine title and text into one string
			if (nd.text != null)
				text = TextUtils.concat(nd.title," ", nd.text);
			else
				text = nd.title;
			
			// set colors for title and terxt
			SpannableStringBuilder ssb = new SpannableStringBuilder(text);
			CharacterStyle titleStyle = new ForegroundColorSpan(titleColor);
			CharacterStyle textStyle = new ForegroundColorSpan(textColor);
			ssb.setSpan(titleStyle, 0, nd.title.length(),0);
			if (nd.text != null) ssb.setSpan(textStyle, nd.title.length()+1, text.length(),0);
			
			text = ssb;
		}		
		// if user choose to hide text and keep content (why would he??) hide the text on large style 
		if (textColor == Color.TRANSPARENT && type.equals("large"))
		{
			n.setViewVisibility(R.id.notificationText, View.GONE);
		}
		else if (textColor != Color.TRANSPARENT)
		{
			n.setViewVisibility(R.id.notificationText, View.VISIBLE);
			n.setTextViewText(R.id.notificationText, text);
		}
		else
		{
			n.setViewVisibility(R.id.notificationText, View.VISIBLE);
		}
		
		if (contentColor != Color.TRANSPARENT)
			n.setTextViewText(R.id.notificationContent, nd.content);
		else
			n.setTextViewText(R.id.notificationContent, null);
		
		if (nd.count > 1)
			n.setTextViewText(R.id.notificationCount, Integer.toString(nd.count));
    	else
    		n.setTextViewText(R.id.notificationCount, null);
		
		// set time
		Time t = new Time();
    	t.set(nd.received);
    	String timeFormat = "%H:%M";
    	if (!DateFormat.is24HourFormat(ctxt)) timeFormat = "%l:%M%P";
    		n.setTextViewText(R.id.notificationTime, t.format(timeFormat));	

    	// limit number of lines
    	n.setInt(R.id.notificationText, "setMaxLines", maxLines);					
    	n.setInt(R.id.notificationContent, "setMaxLines", maxLines);					

    	// set colors
		n.setTextColor(R.id.notificationText, textColor);
		n.setTextColor(R.id.notificationTitle, titleColor);
		n.setTextColor(R.id.notificationContent, contentColor);
		n.setTextColor(R.id.notificationTime, contentColor);
		n.setTextColor(R.id.notificationCount, contentColor);	

	}

	private void createActionBar(RemoteViews row, int position, NotificationData n) 
	{
		row.removeAllViews(R.id.actionbarContainer);
		RemoteViews actionBar = new RemoteViews(ctxt.getPackageName(),R.layout.view_actionbar);
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
		actionBar.setTextViewText(R.id.actionSettings, ctxt.getText(R.string.settings));

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
		
		// if it's expanded large notification - don't show buttons in actions bar 
		if (/*!(sharedPref.getBoolean(n.packageName+"."+AppSettingsActivity.USE_EXPANDED_TEXT, sharedPref.getBoolean(AppSettingsActivity.USE_EXPANDED_TEXT, true))
		      && sharedPref.getString(SettingsActivity.NOTIFICATION_STYLE, "normal").equals("large"))		
			&& */n.actions != null)
			{
				if (n.actions.length >= 1)
				{
					actionBar.setImageViewBitmap(R.id.customAction1, n.actions[0].drawable);
					actionBar.setOnClickPendingIntent(R.id.customAction1, n.actions[0].actionIntent);
					actionBar.setViewVisibility(R.id.customAction1, View.VISIBLE);
					actionBar.setTextViewText(R.id.actionPin, null);
					actionBar.setTextViewText(R.id.actionSettings, null);
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
					actionBar.setTextViewText(R.id.actionPin, null);
					actionBar.setTextViewText(R.id.actionSettings, null);
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
