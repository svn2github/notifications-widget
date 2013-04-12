package com.roymam.android.notificationswidget;
import java.util.ArrayList;
import java.util.Locale;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class NotificationsWidgetService extends RemoteViewsService 
{	
	
	public static final String REFRESH_LIST = "com.roymam.android.notificationswidget.REFRESH_LIST";

	@Override
	public void onStart(Intent intent, int startId) 
	{
		int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
		boolean refreshList = intent.getBooleanExtra(REFRESH_LIST, false);
		    
		for (int widgetId : allWidgetIds) 
		{
			updateWidget(widgetId);
			
			if (refreshList)
			{
				AppWidgetManager.getInstance(this).notifyAppWidgetViewDataChanged(widgetId, R.id.notificationsListView);
			}
		}
		stopSelf();
		//super.onStart(intent, startId);
	}

	private void updateWidget(int widgetId) 
	{
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());
		RemoteViews widget=new RemoteViews(this.getPackageName(), R.layout.widget_layout);
		
		// hide loading spinner
		widget.setViewVisibility(R.id.loadingSpinner, View.GONE);
		
		// update clock
		widget.removeAllViews(R.id.clockContainer);
		String clockStyle = getClockStyle();
		if (!clockStyle.equals(SettingsActivity.CLOCK_HIDDEN))
			widget.addView(R.id.clockContainer, createClock(clockStyle));
		
		// persistent notifications
		widget.removeAllViews(R.id.persistentNotificationsView);
		RemoteViews[] persistentNotifications = getPersistentNotifications();
		for(RemoteViews pn : persistentNotifications)
		{
			widget.addView(R.id.persistentNotificationsView, pn);
		}
		
		// set up notifications list 
		setupNotificationsList(widget, widgetId);
		
		appWidgetManager.updateAppWidget(widgetId, widget);
	}
	
	private void setupNotificationsList(RemoteViews widget, int appWidgetId) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		NotificationsService ns = NotificationsService.getSharedInstance();
		
		// set up notifications list
		Intent svcIntent=new Intent(this, NotificationsWidgetService.class);
	    svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
	    svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
	    widget.setRemoteAdapter(R.id.notificationsListView, svcIntent);
	        	    
	    // set up buttons
	    Intent clearIntent = new Intent(this, NotificationsWidgetProvider.class);
	    clearIntent.setAction(NotificationsWidgetProvider.CLEAR_ALL);
	    widget.setOnClickPendingIntent(R.id.clearButton, 
	    		  PendingIntent.getBroadcast(this, 0, clearIntent, PendingIntent.FLAG_UPDATE_CURRENT));
	          	      
	    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
    	widget.setOnClickPendingIntent(R.id.serviceInactiveButton, 
    			  PendingIntent.getActivity(this, 0, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT));
	     	    	
	    String clearButtonMode = prefs.getString(SettingsActivity.CLEAR_BUTTON_MODE, "visible");					
	   
	    int notificationsCount = 0;
	    
	    if (ns!=null)
	    {
	    	notificationsCount = ns.getNotificationsCount();    	    	
	    }
	    
	    if (!prefs.getBoolean(SettingsActivity.DISABLE_NOTIFICATION_CLICK, false))
	    {
	    	Intent clickIntent=new Intent(this, NotificationActivity.class);
	    	PendingIntent clickPI=PendingIntent.getActivity(this, 0,
                                            			clickIntent,
                                            			PendingIntent.FLAG_UPDATE_CURRENT);
	    	widget.setPendingIntentTemplate(R.id.notificationsListView, clickPI);  
	    }
	    else
	    {
	    	Intent clickIntent=new Intent(NotificationsWidgetProvider.PERFORM_ACTION);
	    	PendingIntent clickPI=PendingIntent.getBroadcast(this, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	    	widget.setPendingIntentTemplate(R.id.notificationsListView, clickPI);    	   
	    }
	    
	    widget.setViewVisibility(R.id.clearButton, (clearButtonMode.equals("visible"))?View.VISIBLE:View.GONE); 
	    
	    // hide clear button if no notifications are displayed
	    if (notificationsCount == 0)
	    	{
    	    	widget.setViewVisibility(R.id.clearButton, View.GONE);   
			  	if (ns==null )
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
	}
	
	private String getNotificationStyle() 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		String notificationsStyle = prefs.getString(SettingsActivity.NOTIFICATION_STYLE, "normal");
	    boolean autoCompact = prefs.getBoolean(SettingsActivity.AUTO_COMPACT_STYLE, false);
		if (autoCompact && !NotificationsWidgetProvider.widgetExpanded)
		{
			notificationsStyle = "compact";
		}
		
		return notificationsStyle;
	}

	private String getClockStyle()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		NotificationsService ns = NotificationsService.getSharedInstance();
		
		// hide clock if required
	    String clockstyle = prefs.getString(SettingsActivity.CLOCK_STYLE, SettingsActivity.CLOCK_AUTO);					
	    String notificationsStyle = getNotificationStyle();
	    int notificationsCount = ns.getNotificationsCount();
	    
	    if (clockstyle.equals(SettingsActivity.CLOCK_SMALL) ||
		    	clockstyle.equals(SettingsActivity.CLOCK_AUTO) && 
		    		(notificationsStyle.equals("large") && notificationsCount > 0 ||
		    		 notificationsStyle.equals("normal") && notificationsCount > 1 || 
		    				notificationsCount > 2 || 
		    				ns != null && ns.getSelectedIndex() >= 0))
		    {
		    	clockstyle = SettingsActivity.CLOCK_SMALL;    	    	
		    } else if (clockstyle.equals(SettingsActivity.CLOCK_LARGE) ||
	    	    	clockstyle.equals(SettingsActivity.CLOCK_AUTO) && 
	    	    	(notificationsStyle.equals("large") && notificationsCount == 0 || 
	    	    	 notificationsStyle.equals("normal") && notificationsCount <= 1 ||
	    	    	 notificationsCount <= 2 ))
		    {
		    	clockstyle = SettingsActivity.CLOCK_LARGE;
		    }
		    else
		    {
		    	clockstyle = SettingsActivity.CLOCK_HIDDEN;
		    }
	    
	    return clockstyle;
	}

	private RemoteViews[] getPersistentNotifications() 
	{
		ArrayList<RemoteViews> pns = new ArrayList<RemoteViews>();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		NotificationsService ns = NotificationsService.getSharedInstance();
		
		if (ns != null)
		{
			String persistentApps = prefs.getString(PersistentNotificationSettingsActivity.PERSISTENT_APPS, "");
			for (String packageName : persistentApps.split(","))
			{
				PersistentNotification pn = ns.getPersistentNotifications().get(packageName);
				if (pn != null)
				{
	    			long persistentTimeout = Long.parseLong(prefs.getString(packageName +"." +PersistentNotificationSettingsActivity.PN_TIMEOUT,"0"));
	    			Time now = new Time();
	    		    now.setToNow();
	    		    Time max = new Time();
	    		    max.set(pn.recieved + persistentTimeout*60*1000);
	    		    
	    			if (
	    					// notification is not too old 
	    					(persistentTimeout == 0 || now.toMillis(true)<max.toMillis(true))
	    					// and notification is set to be seen
	    					&& prefs.getBoolean(packageName + "." + PersistentNotificationSettingsActivity.SHOW_PERSISTENT_NOTIFICATION, false)
	    					// and notification is not set to hide when notifications appears 
	    					&& (!prefs.getBoolean(packageName+"."+PersistentNotificationSettingsActivity.HIDE_WHEN_NOTIFICATIONS, false) || ns.getNotificationsCount() == 0))
    				{
    					String layout = prefs.getString(packageName +"." + PersistentNotificationSettingsActivity.PERSISTENT_NOTIFICATION_HEIGHT, "normal");
    					RemoteViews rv = new RemoteViews(this.getPackageName(), R.layout.notification_persistent);
    					RemoteViews content = pn.content;
    					if (prefs.getBoolean(packageName + "." + AppSettingsActivity.USE_EXPANDED_TEXT, 
    							prefs.getBoolean(AppSettingsActivity.USE_EXPANDED_TEXT, true)))
    						content = pn.expandedContent;
    					if (layout.equals("small"))
    					{
    						rv.addView(R.id.smallLayout, content);
    						rv.setViewVisibility(R.id.smallLayout, View.VISIBLE);
    						rv.setViewVisibility(R.id.normalLayout, View.GONE);
    						rv.setViewVisibility(R.id.maxLayout, View.GONE);
    						rv.setOnClickPendingIntent(R.id.smallLayout, pn.contentIntent);
    					} else if (layout.equals("normal"))
    					{
    						rv.addView(R.id.normalLayout, content);
    						rv.setViewVisibility(R.id.smallLayout, View.GONE);
    						rv.setViewVisibility(R.id.normalLayout, View.VISIBLE);
    						rv.setViewVisibility(R.id.maxLayout, View.GONE);
    						rv.setOnClickPendingIntent(R.id.normalLayout, pn.contentIntent);
    					} else 
    					{
    						rv.addView(R.id.maxLayout, content);
    						rv.setViewVisibility(R.id.smallLayout, View.GONE);
    						rv.setViewVisibility(R.id.normalLayout, View.GONE);
    						rv.setViewVisibility(R.id.maxLayout, View.VISIBLE);
    						rv.setOnClickPendingIntent(R.id.maxLayout, pn.contentIntent);
    					}        					
    					pns.add(rv);
    				}
				}
			}
		}
		
		RemoteViews[] rvs = new RemoteViews[pns.size()];
		pns.toArray(rvs);
		return rvs;
	}

	private RemoteViews createClock(String type)
	{		
		NotificationsService ns = NotificationsService.getSharedInstance();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		RemoteViews clock;
		if (type.equals(SettingsActivity.CLOCK_SMALL))
		{
			clock = new RemoteViews(this.getPackageName(), R.layout.small_clock);
		}
		else 
		{
			clock = new RemoteViews(this.getPackageName(), R.layout.large_clock);		
		}
		
	    // set up clock
	    Time t = new Time();
	    t.setToNow();
	    String hourFormat = "%H";
	    String minuteFormat = ":%M";
	    String ampmstr = "";
    	if (!DateFormat.is24HourFormat(this))
    	{
    		hourFormat = "%l";
    		minuteFormat = ":%M";
    		ampmstr = t.format("%p");
    	}
    	
	    clock.setTextViewText(R.id.hours, t.format(hourFormat));
	    clock.setTextViewText(R.id.minutes, t.format(minuteFormat));
	    clock.setTextViewText(R.id.ampm, ampmstr);		    
	    String datestr = DateFormat.format("EEE, MMMM dd", t.toMillis(true)).toString();
	    clock.setTextViewText(R.id.date, datestr.toUpperCase(Locale.getDefault()));
	    
	    // set clock text color
	    int color = Resources.getSystem().getColor(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.CLOCK_COLOR, String.valueOf(android.R.color.white))));
	    clock.setTextColor(R.id.hours, color);
	    clock.setTextColor(R.id.minutes, color);
	    clock.setTextColor(R.id.ampm, color);
	    clock.setTextColor(R.id.date, color);
	    clock.setTextColor(R.id.alarmtime, color);
	    
	    // display next alarm if needed
	    String nextAlarm = Settings.System.getString(this.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
	    if (!nextAlarm.equals(""))
	    {
	    	clock.setViewVisibility(R.id.alarmtime, View.VISIBLE);
	    	clock.setTextViewText(R.id.alarmtime, "⏰" + nextAlarm.toUpperCase(Locale.getDefault()));
	    }
	    else
	    {
	    	clock.setViewVisibility(R.id.alarmtime, View.GONE);
	    }
	    
	    // add alarm clock intent
	    PackageManager packageManager = this.getPackageManager();
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
	        String packageName = clockImpls[i][1];
	        String className = clockImpls[i][2];
	        try 
	        {
	            ComponentName cn = new ComponentName(packageName, className);
	            packageManager.getActivityInfo(cn, PackageManager.GET_META_DATA);
	            alarmClockIntent.setComponent(cn);
	            foundClockImpl = true;
	        } catch (NameNotFoundException e) 
	        {	            
	        }
	    }

	    if (foundClockImpl) 
	    {
	        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, alarmClockIntent, 0);
	        clock.setOnClickPendingIntent(clock.getLayoutId(), pendingIntent);	       
	    }
	    
	    // set up filler for clear button
	    if (ns != null &&
	    	ns.getNotificationsCount() > 0 &&
	    	prefs.getString(SettingsActivity.CLEAR_BUTTON_MODE, "visible").equals("visible"))
	    	clock.setViewVisibility(R.id.clearButtonFiller, View.VISIBLE);
	    else
	    	clock.setViewVisibility(R.id.clearButtonFiller, View.GONE);
	    
	    return clock;
	}

	/*@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		return START_STICKY;
	}*/

	@Override
	  public RemoteViewsFactory onGetViewFactory(Intent intent) 
	  {  
		  return(new NotificationsViewFactory(this.getApplicationContext(), intent));
	  }

	
}
