package com.roymam.android.notificationswidget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
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
import java.util.Locale;

public class NotificationsWidgetService extends Service 
{	
	public static final String REFRESH_LIST = "com.roymam.android.notificationswidget.REFRESH_LIST";
	public static final String IS_EXPANDED = "com.roymam.android.notificationswidget.IS_EXPANDED";
	public static final String ACTION = "com.roymam.android.notificationswidget.ACTION";

	public static final int ACTION_RENDER_WIDGETS = 0;
	public static final int ACTION_OPTIONS_CHANGED = 1;
    private static boolean widgetExpanded;
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
				if (prefs.getBoolean(SettingsManager.DISABLE_AUTO_SWITCH, false))
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
			}
			else if (action == ACTION_OPTIONS_CHANGED)
			{
				int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
				if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
				{					
					int hostCategory = intent.getIntExtra(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD);
					
					if (!prefs.getBoolean(SettingsManager.DISABLE_AUTO_SWITCH, false))
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
					    
					String lastWidgetMode = prefs.getString(SettingsManager.WIDGET_MODE +"." + appWidgetId, "");
					String newWidgetMode = SettingsManager.HOME_WIDGET_MODE;
					
					if (widgetExpanded && hostCategory == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
					{
						newWidgetMode = SettingsManager.EXPANDED_WIDGET_MODE;
					}
					else if (!widgetExpanded && hostCategory == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
					{
						newWidgetMode = SettingsManager.COLLAPSED_WIDGET_MODE;
					}
					
					// if mode has been changed
					if (!lastWidgetMode.equals(newWidgetMode))
					{
						prefs.edit().putString(SettingsManager.WIDGET_MODE + "." + appWidgetId, newWidgetMode)
									.putString(SettingsManager.LAST_WIDGET_MODE, newWidgetMode).commit();
						
						updateWidget(appWidgetId);
						// notify widget that it should be refreshed
						AppWidgetManager.getInstance(this).notifyAppWidgetViewDataChanged(appWidgetId, R.id.notificationsListView);
					}					
				}			
			}
		}
        stopSelf(startId);
        return super.onStartCommand(intent, flags, startId);
	}

	private void updateWidget(int widgetId) 
	{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String widgetMode = prefs.getString(SettingsManager.WIDGET_MODE + "." + widgetId, SettingsManager.EXPANDED_WIDGET_MODE);

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());

        RemoteViews clockRV = new RemoteViews(this.getPackageName(), R.layout.widget_layout);
        RemoteViews notificationsRV = new RemoteViews(this.getPackageName(), R.layout.widget_layout);
        RemoteViews persistentNotificationsRV = new RemoteViews(this.getPackageName(), R.layout.widget_layout);

		// hide loading spinner
        clockRV.setViewVisibility(R.id.loadingSpinner, View.GONE);
		
		// update clock
        clockRV.removeAllViews(R.id.clockContainer);
		boolean hideClock = prefs.getBoolean(widgetMode + "." + SettingsManager.CLOCK_HIDDEN, false);

        // check if need to hide the clock because of persistent notifications
        if (prefs.getBoolean(widgetMode + "." + SettingsManager.SHOW_PERSISTENT_NOTIFICATIONS, true))
        {
            NotificationsProvider ns = NotificationsService.getSharedInstance();
            if (ns != null)
            {
                String persistentApps = prefs.getString(PersistentNotificationSettingsActivity.PERSISTENT_APPS, "");
                for (String packageName : persistentApps.split(","))
                {
                    PersistentNotification pn = ns.getPersistentNotifications().get(packageName);
                    if (pn != null && prefs.getBoolean(packageName + "." + PersistentNotificationSettingsActivity.HIDE_CLOCK_WHEN_VISIBLE, false))
                    {
                        hideClock = true;
                    }
                }
            }
        }

        if (!hideClock)
		{
            String clockStyle = getClockStyle(widgetId);
            clockRV.addView(R.id.clockContainer, createClock(clockStyle, widgetId));
		}
		
		// set clock bg color
	    int bgColor = prefs.getInt(widgetMode + "." + SettingsManager.CLOCK_BG_COLOR, Color.BLACK);
	    int alpha = prefs.getInt(widgetMode + "." + SettingsManager.CLOCK_BG_OPACITY, 0);
	    bgColor = Color.argb(alpha * 255 / 100, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor));
        clockRV.setInt(R.id.clockContainer, "setBackgroundColor", bgColor);

		// persistent notifications
		persistentNotificationsRV.removeAllViews(R.id.persistentNotificationsView);
        persistentNotificationsRV.setInt(R.id.persistentNotificationsView, "setBackgroundColor", bgColor);

        if (prefs.getBoolean(widgetMode + "." + SettingsManager.SHOW_PERSISTENT_NOTIFICATIONS, true))
        {
            RemoteViews[] persistentNotifications = getPersistentNotifications();
            for(RemoteViews pn : persistentNotifications)
            {
                persistentNotificationsRV.addView(R.id.persistentNotificationsView, pn);
            }
        }
		
		// set up notifications list
        if (SettingsManager.shouldHideNotifications(getApplicationContext(), widgetMode))
        {
            notificationsRV.setViewVisibility(R.id.notificationsListView, View.GONE);
        }
        else
        {
            notificationsRV.setViewVisibility(R.id.notificationsListView, View.VISIBLE);
            setupNotificationsList(notificationsRV, widgetId);
        }

        appWidgetManager.updateAppWidget(widgetId, clockRV);
        appWidgetManager.partiallyUpdateAppWidget(widgetId, notificationsRV);
        try
        {
            persistentNotificationsRV.setViewVisibility(R.id.persistentNotificationsErrorView, View.GONE);
            appWidgetManager.partiallyUpdateAppWidget(widgetId, persistentNotificationsRV);
        }

        // sometimes this method fails with TransactionIsTooLarge
        catch (Exception exp)
        {
            // if it fails - show an error instead
            persistentNotificationsRV = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_layout);
            persistentNotificationsRV.setViewVisibility(R.id.persistentNotificationsErrorView, View.VISIBLE);
            appWidgetManager.partiallyUpdateAppWidget(widgetId, persistentNotificationsRV);
        }
	}
	
	private PendingIntent getClockAppIntent()
	{

		// add alarm clock intent
	    PackageManager packageManager = this.getPackageManager();

	    // Verify clock implementation
	    String clockImpls[] =
	    {
                "com.sonyericsson.organizer",
                "ch.bitspin.timely",
                "com.sonyericsson.alarm",
                "com.htc.android.worldclock",
                "com.motorola.blur.alarmclock",
                "com.sec.android.app.clockpackage",
                "com.lge.clock",
                "com.android.deskclock",
                "com.google.android.deskclock"
	    };

        Intent alarmClockIntent  = null;
	    for(int i=0; i<clockImpls.length  && alarmClockIntent == null; i++)
	    {
	        String packageName = clockImpls[i];
            alarmClockIntent = packageManager.getLaunchIntentForPackage(packageName);
	    }

        if (alarmClockIntent != null)
	    {
	        return PendingIntent.getActivity(this, 0, alarmClockIntent , 0);
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
		NotificationsProvider ns = NotificationsService.getSharedInstance();
		
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
	          	      
	    Intent settingsIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            settingsIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        else
            settingsIntent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
    	widget.setOnClickPendingIntent(R.id.serviceInactiveButton, 
    			  PendingIntent.getActivity(this, 0, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT));
	     	    	
	    int notificationsCount = 0;
	    
	    if (ns!=null)
	    {
	    	notificationsCount = ns.getNotifications().size();
	    }
	    
	    String widgetMode = prefs.getString(SettingsManager.WIDGET_MODE + "." + appWidgetId, SettingsManager.EXPANDED_WIDGET_MODE);
	    if (prefs.getBoolean(widgetMode + "." + SettingsManager.NOTIFICATION_IS_CLICKABLE, true))
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
	    
	    boolean showClearButton = prefs.getBoolean(widgetMode + "." + SettingsManager.SHOW_CLEAR_BUTTON, widgetMode.equals(SettingsManager.COLLAPSED_WIDGET_MODE)?false:true);
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
		String widgetMode = prefs.getString(SettingsManager.WIDGET_MODE + "." + widgetId, SettingsManager.EXPANDED_WIDGET_MODE);
		String notificationsStyle = prefs.getString(widgetMode + "." + SettingsManager.NOTIFICATION_STYLE, widgetMode.equals(SettingsManager.COLLAPSED_WIDGET_MODE)?"compact":"normal");
		return notificationsStyle;
	}

	private String getClockStyle(int widgetId)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		NotificationsProvider ns = NotificationsService.getSharedInstance();
		String widgetMode = prefs.getString(SettingsManager.WIDGET_MODE + "." + widgetId, SettingsManager.EXPANDED_WIDGET_MODE);
		
		// hide clock if required
	    String clockstyle = prefs.getString(widgetMode + "." + SettingsManager.CLOCK_STYLE, SettingsManager.CLOCK_AUTO);
	    String notificationsStyle = getNotificationStyle(widgetId);
	    int notificationsCount = 0;
	    if (ns != null) notificationsCount  = ns.getNotifications().size();

	    if (clockstyle.equals(SettingsManager.CLOCK_AUTO))
	    {
	    	int largeClockLimit;
	    	int mediumClockLimit;

            boolean actionBarVisible = false;

            if (ns!= null)
                for(NotificationData nd : ns.getNotifications())
                {
                    if (nd.selected) actionBarVisible = true;
                }

            //TODO: make it configurable
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
	    	if (notificationsCount < largeClockLimit || SettingsManager.shouldHideNotifications(getApplicationContext(), widgetMode))
	    		clockstyle = SettingsManager.CLOCK_LARGE;
	    	else if (notificationsCount < mediumClockLimit)
	    		clockstyle = SettingsManager.CLOCK_MEDIUM;
	    	else
	    		clockstyle = SettingsManager.CLOCK_SMALL;
	    }
	    return clockstyle;
	}

	private RemoteViews[] getPersistentNotifications()
	{
		ArrayList<RemoteViews> pns = new ArrayList<RemoteViews>();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		NotificationsProvider ns = NotificationsService.getSharedInstance();

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
	    		    max.set(pn.received + persistentTimeout*60*1000);
	    		    
	    			if (
	    					// notification is not too old 
	    					(persistentTimeout == 0 || now.toMillis(true)<max.toMillis(true))
	    					// and notification is set to be seen
	    					&& prefs.getBoolean(packageName + "." + PersistentNotificationSettingsActivity.SHOW_PERSISTENT_NOTIFICATION, false)
	    					// and notification is not set to hide when notifications appears 
	    					&& (!prefs.getBoolean(packageName+"."+PersistentNotificationSettingsActivity.HIDE_WHEN_NOTIFICATIONS, false) || ns.getNotifications().size() == 0))
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

    private Bitmap getClockIcon(int color)
    {
        Bitmap sourceBitmap = BitmapFactory.decodeResource(Resources.getSystem(), android.R.drawable.ic_lock_idle_alarm);
        if (sourceBitmap != null && sourceBitmap.getWidth() > 0)
        {
            float r = (float) Color.red(color),
                  g = (float) Color.green(color),
                  b = (float) Color.blue(color);

            float[] colorTransform =
                    {
                            r/255, 0    , 0    , 0, 0,  // R color
                            0    , g/255, 0    , 0, 0,  // G color
                            0    , 0    , b/255, 0, 0,  // B color
                            0    , 0    , 0    , 1, 0
                    };

            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0f); // Remove colour
            colorMatrix.set(colorTransform);

            ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
            Paint paint = new Paint();
            paint.setColorFilter(colorFilter);

            Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap);
            Bitmap mutableBitmap = resultBitmap.copy(Bitmap.Config.ARGB_8888, true);

            Canvas canvas = new Canvas(mutableBitmap);
            canvas.drawBitmap(mutableBitmap, 0, 0, paint);

            return mutableBitmap;
        }
        else
        {
            return null;
        }
    }

	private RemoteViews createClock(String type, int widgetId)
	{		
		NotificationsProvider ns = NotificationsService.getSharedInstance();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		RemoteViews clock;
		int clockId; 
		if (type.equals(SettingsManager.CLOCK_SMALL))
		{
			clock = new RemoteViews(this.getPackageName(), R.layout.small_clock);
			clockId = R.id.smallClock;
		}
		else if (type.equals(SettingsManager.CLOCK_MEDIUM))
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
	    
	    String widgetMode = prefs.getString(SettingsManager.WIDGET_MODE + "." + widgetId, SettingsManager.EXPANDED_WIDGET_MODE);

        int clockColor = prefs.getInt(widgetMode + "." + SettingsManager.CLOCK_COLOR, Color.WHITE);
        int dateColor = prefs.getInt(widgetMode + "." + SettingsManager.CLOCK_DATE_COLOR, Color.WHITE);
        int alarmColor  = prefs.getInt(widgetMode + "." + SettingsManager.CLOCK_ALARM_COLOR, Color.GRAY);

        // display next alarm if needed
        String nextAlarm = Settings.System.getString(getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
        if (nextAlarm != null && !nextAlarm.equals("") && alarmColor != Resources.getSystem().getColor(android.R.color.transparent))
	    {
	    	clock.setViewVisibility(R.id.alarmtime, View.VISIBLE);
            clock.setViewVisibility(R.id.alarm_clock_image, View.VISIBLE);
            clock.setImageViewBitmap(R.id.alarm_clock_image, getClockIcon(alarmColor));
	    	clock.setTextViewText(R.id.alarmtime, nextAlarm.toUpperCase(Locale.getDefault()));
	    }
	    else
	    {
	    	clock.setViewVisibility(R.id.alarmtime, View.GONE);
            clock.setViewVisibility(R.id.alarm_clock_image, View.GONE);
	    }
	    
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
	    
	    boolean boldHours = prefs.getBoolean(widgetMode + "." + SettingsManager.BOLD_HOURS, true);
	    boolean boldMinutes = prefs.getBoolean(widgetMode + "." + SettingsManager.BOLD_MINUTES, false);
	    
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
	    	ns.getNotifications().size() > 0 &&
                prefs.getBoolean(widgetMode + "." + SettingsManager.SHOW_CLEAR_BUTTON, widgetMode.equals(SettingsManager.COLLAPSED_WIDGET_MODE) ? false : true))
	    	clock.setViewVisibility(R.id.clearButtonFiller, View.VISIBLE);
	    else
	    	clock.setViewVisibility(R.id.clearButtonFiller, View.GONE);
	    
	    if (prefs.getBoolean(widgetMode +"." + SettingsManager.CLOCK_IS_CLICKABLE, true))
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
