package com.roymam.android.notificationswidget;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static android.app.ActivityManager.RunningAppProcessInfo;

public class NotificationsWidgetService extends Service 
{	
	public static final String REFRESH_LIST = "com.roymam.android.notificationswidget.REFRESH_LIST";
	public static final String IS_EXPANDED = "com.roymam.android.notificationswidget.IS_EXPANDED";
	public static final String ACTION = "com.roymam.android.notificationswidget.ACTION";
	public static final int ACTION_RENDER_WIDGETS = 0;
	public static final int ACTION_OPTIONS_CHANGED = 1;
    public static final int ACTION_MONITOR_APPS = 9;
	private static boolean widgetExpanded;
	private static boolean clockStarted = false;
	public static boolean widgetActive = false;

    @Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// start clock service (if hasn't started yet)
        startService(new Intent(getApplicationContext(), ClockService.class));

		if (intent != null)
		{
			int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
			boolean refreshList = intent.getBooleanExtra(REFRESH_LIST, false);
			int action = intent.getIntExtra(ACTION, ACTION_RENDER_WIDGETS);
			
			if (action == ACTION_RENDER_WIDGETS)   
			{
				if (prefs.getBoolean(SettingsActivity.DISABLE_AUTO_SWITCH, false))
				{
					widgetExpanded = true;
				}
				
				for (int widgetId : allWidgetIds) 
				{
					updateWidget(widgetId);
					
					if (refreshList)
					{
						AppWidgetManager.getInstance(this).notifyAppWidgetViewDataChanged(widgetId, R.id.notificationsListView);
					}
				}
				
				updateClearOnUnlockState();
			}
			else if (action == ACTION_OPTIONS_CHANGED)
			{
				int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
				if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
				{					
					int hostCategory = intent.getIntExtra(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD);
					
					if (!prefs.getBoolean(SettingsActivity.DISABLE_AUTO_SWITCH, false))
					{
						boolean isExpanded = intent.getBooleanExtra(NotificationsWidgetService.IS_EXPANDED, true);
					    
					    if ((isExpanded && !widgetExpanded || !isExpanded && widgetExpanded) && 
								hostCategory==AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD ||
								hostCategory==AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN )
						{
							widgetExpanded = isExpanded;			
						}
					}
					else
					{
						widgetExpanded = true;
					}
					    
					String lastWidgetMode = prefs.getString(SettingsActivity.WIDGET_MODE +"." + appWidgetId, "");
					String newWidgetMode = SettingsActivity.HOME_WIDGET_MODE;
					
					if (widgetExpanded && hostCategory == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
					{
						newWidgetMode = SettingsActivity.EXPANDED_WIDGET_MODE;
					}
					else if (!widgetExpanded && hostCategory == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
					{
						newWidgetMode = SettingsActivity.COLLAPSED_WIDGET_MODE;
					}
					
					// if mode has been changed
					if (!lastWidgetMode.equals(newWidgetMode))
					{
						prefs.edit().putString(SettingsActivity.WIDGET_MODE + "." + appWidgetId, newWidgetMode)
									.putString(SettingsActivity.LAST_WIDGET_MODE, newWidgetMode).commit();												
						
						updateWidget(appWidgetId);
						// notify widget that it should be refreshed
						AppWidgetManager.getInstance(this).notifyAppWidgetViewDataChanged(appWidgetId, R.id.notificationsListView);
					}					
				}			
			}
            else if (action == ACTION_MONITOR_APPS)
            {
                monitorRunningApps();
            }
		}
        stopSelf(startId);
        return super.onStartCommand(intent, flags, startId);
	}

    private void monitorRunningApps() 
    {
        NotificationsService ns = NotificationsService.getSharedInstance();
        if (ns!=null)
        {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            List<RunningAppProcessInfo> l = am.getRunningAppProcesses();
            Iterator<RunningAppProcessInfo> i = l.iterator();
            ArrayList<String> runningApps = new ArrayList<String>();
            while(i.hasNext())
            {
                RunningAppProcessInfo info = i.next();
                if (info.importance <= RunningAppProcessInfo.IMPORTANCE_SERVICE)
                    for(String packageName : info.pkgList)
                        runningApps.add(packageName);
            }
            ns.purgePersistentNotifications(runningApps);
        }
    }

    private void updateClearOnUnlockState() 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		NotificationsService ns = NotificationsService.getSharedInstance();
		
	    // check if the screen has been turned on to start collecting
	    if (!prefs.getBoolean(SettingsActivity.COLLECT_ON_UNLOCK, true) && ns != null)
		{
			PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
			boolean isScreenOn = powerManager.isScreenOn();
			if (!isScreenOn && ns.isDeviceIsUnlocked())
			{
				ns.setDeviceIsLocked();
			}
		}
	
	}

	private void updateWidget(int widgetId) 
	{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String widgetMode = prefs.getString(SettingsActivity.WIDGET_MODE + "." + widgetId, SettingsActivity.EXPANDED_WIDGET_MODE);

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());
		RemoteViews widget=new RemoteViews(this.getPackageName(), R.layout.widget_layout);
		
		// hide loading spinner
		widget.setViewVisibility(R.id.loadingSpinner, View.GONE);
		
		// update clock
		widget.removeAllViews(R.id.clockContainer);
		String clockStyle = getClockStyle(widgetId);
		if (!prefs.getBoolean(widgetMode + "." + SettingsActivity.CLOCK_HIDDEN, false))
		{
			widget.addView(R.id.clockContainer, createClock(clockStyle, widgetId));
		}
		
		// set clock bg color
	    int bgColor = prefs.getInt(widgetMode + "." + SettingsActivity.CLOCK_BG_COLOR, Color.BLACK);		    
	    int alpha = prefs.getInt(widgetMode + "." + SettingsActivity.CLOCK_BG_OPACITY, 0);
	    bgColor = Color.argb(alpha * 255 / 100, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor));
	    widget.setInt(R.id.clockContainer, "setBackgroundColor", bgColor);

		// persistent notifications
		widget.removeAllViews(R.id.persistentNotificationsView);
		widget.setInt(R.id.persistentNotificationsView, "setBackgroundColor", bgColor);

        if (prefs.getBoolean(widgetMode + "." + SettingsActivity.SHOW_PERSISTENT_NOTIFICATIONS, true))
        {
            RemoteViews[] persistentNotifications = getPersistentNotifications();
            for(RemoteViews pn : persistentNotifications)
            {
                widget.addView(R.id.persistentNotificationsView, pn);
            }
        }
		
		// set up notifications list 
		setupNotificationsList(widget, widgetId);

		appWidgetManager.updateAppWidget(widgetId, widget);
	}
	
	private PendingIntent getClockAppIntent()
	{
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
	        return PendingIntent.getActivity(this, 0, alarmClockIntent, 0);
	    }
	    else
	    {
	    	return null;
	    }
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setupNotificationsList(RemoteViews widget, int appWidgetId)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		NotificationsService ns = NotificationsService.getSharedInstance();
		
		// set up notifications list
		Intent svcIntent=new Intent(this, NotificationsRemoteViewsFactoryService.class);
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
	     	    	
	    int notificationsCount = 0;
	    
	    if (ns!=null)
	    {
	    	notificationsCount = ns.getNotificationsCount();    	    	
	    }
	    
	    String widgetMode = prefs.getString(SettingsActivity.WIDGET_MODE + "." + appWidgetId, SettingsActivity.EXPANDED_WIDGET_MODE);
	    if (prefs.getBoolean(widgetMode + "." + SettingsActivity.NOTIFICATION_IS_CLICKABLE, true))
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
	    
	    boolean showClearButton = prefs.getBoolean(widgetMode + "." + SettingsActivity.SHOW_CLEAR_BUTTON, widgetMode.equals(SettingsActivity.COLLAPSED_WIDGET_MODE)?false:true);
	    widget.setViewVisibility(R.id.clearButton, showClearButton?View.VISIBLE:View.GONE);
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
	
	private String getNotificationStyle(int widgetId) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);		
		String widgetMode = prefs.getString(SettingsActivity.WIDGET_MODE + "." + widgetId, SettingsActivity.EXPANDED_WIDGET_MODE);
		String notificationsStyle = prefs.getString(widgetMode + "." + SettingsActivity.NOTIFICATION_STYLE, widgetMode.equals(SettingsActivity.COLLAPSED_WIDGET_MODE)?"compact":"normal");
		return notificationsStyle;
	}

	private String getClockStyle(int widgetId)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		NotificationsService ns = NotificationsService.getSharedInstance();
		String widgetMode = prefs.getString(SettingsActivity.WIDGET_MODE + "." + widgetId, SettingsActivity.EXPANDED_WIDGET_MODE);
		
		// hide clock if required
	    String clockstyle = prefs.getString(widgetMode + "." + SettingsActivity.CLOCK_STYLE, SettingsActivity.CLOCK_AUTO);
	    String notificationsStyle = getNotificationStyle(widgetId);
	    int notificationsCount = 0;
	    if (ns != null) notificationsCount  = ns.getNotificationsCount();

	    if (clockstyle.equals(SettingsActivity.CLOCK_AUTO))
	    {
	    	int largeClockLimit;
	    	int mediumClockLimit;
	    	
	    	boolean actionBarVisible = (ns != null && ns.getSelectedIndex() >= 0);
	    	
	    	if (notificationsStyle.equals("compact"))
	    	{
	    		largeClockLimit = 3;
	    		mediumClockLimit = 4;
	    	}
	    	else if (notificationsStyle.equals("normal"))
	    	{
	    		largeClockLimit = 2;
	    		mediumClockLimit = 3;
	    	}
	    	else
	    	{
	    		largeClockLimit = 1;
	    		mediumClockLimit = 2;
	    	}
	    	if (actionBarVisible)
    		{
    			largeClockLimit--;
    			mediumClockLimit--;
    		}
	    	if (notificationsCount < largeClockLimit )
	    		clockstyle = SettingsActivity.CLOCK_LARGE;
	    	else if (notificationsCount < mediumClockLimit)
	    		clockstyle = SettingsActivity.CLOCK_MEDIUM;
	    	else
	    		clockstyle = SettingsActivity.CLOCK_SMALL;
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
    						if (pn.contentIntent != null)
    							rv.setOnClickPendingIntent(R.id.smallLayout, pn.contentIntent);
    					} else if (layout.equals("normal"))
    					{
    						rv.addView(R.id.normalLayout, content);
    						rv.setViewVisibility(R.id.smallLayout, View.GONE);
    						rv.setViewVisibility(R.id.normalLayout, View.VISIBLE);
    						rv.setViewVisibility(R.id.maxLayout, View.GONE);
    						if (pn.contentIntent != null)
    							rv.setOnClickPendingIntent(R.id.normalLayout, pn.contentIntent);
    					} else 
    					{
    						rv.addView(R.id.maxLayout, content);
    						rv.setViewVisibility(R.id.smallLayout, View.GONE);
    						rv.setViewVisibility(R.id.normalLayout, View.GONE);
    						rv.setViewVisibility(R.id.maxLayout, View.VISIBLE);
    						if (pn.contentIntent != null)
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

	private RemoteViews createClock(String type, int widgetId)
	{		
		NotificationsService ns = NotificationsService.getSharedInstance();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		RemoteViews clock;
		int clockId; 
		if (type.equals(SettingsActivity.CLOCK_SMALL))
		{
			clock = new RemoteViews(this.getPackageName(), R.layout.small_clock);
			clockId = R.id.smallClock;
		}
		else if (type.equals(SettingsActivity.CLOCK_MEDIUM))
		{
			clock = new RemoteViews(this.getPackageName(), R.layout.medium_clock);	
			clockId = R.id.mediumClock;
		}
		else
		{
			clock = new RemoteViews(this.getPackageName(), R.layout.large_clock);
			clockId = R.id.largeClock;
		}
		
		// get current time
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
	    
	    String datestr = DateFormat.getLongDateFormat(this).format(t.toMillis(true));
	    clock.setTextViewText(R.id.date, datestr.toUpperCase(Locale.getDefault()));
	    
	    // display next alarm if needed
	    String nextAlarm = Settings.System.getString(getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
	    if (nextAlarm != null && !nextAlarm.equals(""))
	    {
	    	clock.setViewVisibility(R.id.alarmtime, View.VISIBLE);
	    	clock.setTextViewText(R.id.alarmtime, "⏰" + nextAlarm.toUpperCase(Locale.getDefault()));
	    }
	    else
	    {
	    	clock.setViewVisibility(R.id.alarmtime, View.GONE);
	    }
	    
	    String widgetMode = prefs.getString(SettingsActivity.WIDGET_MODE + "." + widgetId, SettingsActivity.EXPANDED_WIDGET_MODE);
		
	    int clockColor = prefs.getInt(widgetMode + "." + SettingsActivity.CLOCK_COLOR, Color.WHITE);
	    int dateColor = prefs.getInt(widgetMode + "." + SettingsActivity.CLOCK_DATE_COLOR, Color.WHITE);
	    int alarmColor  = prefs.getInt(widgetMode + "." + SettingsActivity.CLOCK_ALARM_COLOR, Color.GRAY);
	    clock.setTextColor(R.id.hours, clockColor);
	    clock.setTextColor(R.id.minutes, clockColor);
	    clock.setTextColor(R.id.ampm, clockColor);
	    clock.setTextColor(R.id.date, dateColor);
	    clock.setTextColor(R.id.alarmtime, alarmColor);
	   
	    if (dateColor == Color.TRANSPARENT)
	    	clock.setViewVisibility(R.id.date, View.GONE);
	    else
	    	clock.setViewVisibility(R.id.date, View.VISIBLE);
	    
	    if (alarmColor == Color.TRANSPARENT)
	    	clock.setViewVisibility(R.id.alarmtime, View.GONE);
	    
	    boolean boldHours = prefs.getBoolean(widgetMode + "." + SettingsActivity.BOLD_HOURS, true);
	    boolean boldMinutes = prefs.getBoolean(widgetMode + "." + SettingsActivity.BOLD_MINUTES, false);
	    
	    String hoursStr = t.format(hourFormat);
    	SpannableString s = new SpannableString(hoursStr); 
    	
	    if (boldHours)
	    {
            s.setSpan(new StyleSpan(Typeface.BOLD), 0, hoursStr.length(), 0); 
            s.setSpan(new TypefaceSpan("sans-serif"), 0, hoursStr.length(), 0);
	    }
	    else
	    {
	    	s.setSpan(new StyleSpan(Typeface.NORMAL), 0, hoursStr.length(), 0); 
            s.setSpan(new TypefaceSpan("sans-serif-thin"), 0, hoursStr.length(), 0);
	    }
	    clock.setTextViewText(R.id.hours, s); 
	    
	    String minutesStr = t.format(minuteFormat);
    	SpannableString s2 = new SpannableString(minutesStr); 
    	
	    if (boldMinutes)
	    {
            s2.setSpan(new StyleSpan(Typeface.BOLD), 0, minutesStr.length(), 0); 
            s2.setSpan(new TypefaceSpan("sans-serif"), 0, minutesStr.length(), 0);
	    }
	    else
	    {
	    	s2.setSpan(new StyleSpan(Typeface.NORMAL), 0, minutesStr.length(), 0); 
            s2.setSpan(new TypefaceSpan("sans-serif-thin"), 0, minutesStr.length(), 0);
	    }
	    clock.setTextViewText(R.id.minutes, s2);
	   
	    // set up filler for clear button
	    if (ns != null &&
	    	ns.getNotificationsCount() > 0 &&
                prefs.getBoolean(widgetMode + "." + SettingsActivity.SHOW_CLEAR_BUTTON, widgetMode.equals(SettingsActivity.COLLAPSED_WIDGET_MODE) ? false : true))
	    	clock.setViewVisibility(R.id.clearButtonFiller, View.VISIBLE);
	    else
	    	clock.setViewVisibility(R.id.clearButtonFiller, View.GONE);
	    
	    if (prefs.getBoolean(widgetMode +"." + SettingsActivity.CLOCK_IS_CLICKABLE, true))
	    {
            PendingIntent pi = getClockAppIntent();
            if (pi != null)
	    	    clock.setOnClickPendingIntent(clockId, getClockAppIntent());
	    }

        return clock;
	}

	@Override
	public IBinder onBind(Intent arg0) 
	{
		return null;
	}
	
}
