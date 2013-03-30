package com.roymam.android.notificationswidget;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.roymam.android.notificationswidget.NotificationData.Action;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

public class NotificationsService extends AccessibilityService 
{
	private static NotificationsService sSharedInstance;
	private List<NotificationData> notifications;
	private HashMap<String, RemoteViews> persistentNotifications;
	private boolean deviceIsUnlocked = true;
	private boolean deviceCovered = false;
	private boolean newNotificationsAvailable = false;
	private boolean widgetLockerEnabled = false;
	private int 	selectedIndex = -1;
	private String clearButtonName = "Clear all notifications.";
	
	public int notification_image_id = 0;
	public int notification_title_id = 0;
	public int notification_text_id = 0;
	public int notification_info_id = 0;
	public int notification_subtext_id = 0;
	public int big_notification_summary_id = 0;
	public int big_notification_content_title = 0;
	public int big_notification_content_text = 0;
	public int inbox_notification_event_1_id = 0;
	public int inbox_notification_event_2_id = 0;
	public int inbox_notification_event_3_id = 0;
	public int inbox_notification_event_4_id = 0;
	public int inbox_notification_event_5_id = 0;
	public int inbox_notification_event_6_id = 0;
	public int inbox_notification_event_7_id = 0;
	public int inbox_notification_event_8_id = 0;
	public int inbox_notification_event_9_id = 0;
	public int inbox_notification_event_10_id = 0;
	
	public static NotificationsService getSharedInstance() { return sSharedInstance; }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	@Override
	protected void onServiceConnected() 
	{
		super.onServiceConnected();
	
		// first run preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean firstRun = prefs.getBoolean("com.roymam.android.notificationswidget.firstrun", true);
		prefs.edit().putBoolean("com.roymam.android.notificationswidget.firstrun", false).commit();
		if (firstRun)
		{
			if (Build.MODEL.equals("Nexus 4"))
			{
				prefs.edit().putBoolean(SettingsActivity.DISABLE_PROXIMITY, true).commit();
			}
		}
		
		sSharedInstance = this;		
		AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		
		// check if "Clear Notifications Monitor" feature enabled, if so - monitor view clicks
		if (prefs.getBoolean(SettingsActivity.CLEAR_ON_CLEAR, false))
		{
			// find "clear all notifications." button text
		    Resources res;
			try 
			{
				res = getPackageManager().getResourcesForApplication("com.android.systemui");
				int i = res.getIdentifier("accessibility_clear_all", "string", "com.android.systemui");
				if (i!=0)
				{
					clearButtonName = res.getString(i);
				}							
			} 
			catch (Exception exp)
			{
				Toast.makeText(this, R.string.failed_to_monitor_clear_button, Toast.LENGTH_LONG).show();
			}			
		    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED | AccessibilityEvent.TYPE_VIEW_CLICKED;		    
		}
		else
		{		
		    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;		   
		}
		
		info.notificationTimeout = 100;
	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
	    setServiceInfo(info);
	    	    
	    notifications = new ArrayList<NotificationData>();
	    persistentNotifications = new HashMap<String, RemoteViews>();
	    
	    // register proximity change sensor
		registerProximitySensor();
			
		// keep app on foreground if requested
		keepOnForeground();
		
		// detect expanded notification id's 
		detectNotificationIds();		
	}	
	
	// Proximity Sensor Monitoring
	SensorManager sensorManager = null;
	Sensor proximitySensor = null;
	SensorEventListener sensorListener = null;
	
	private void registerProximitySensor()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (!prefs.getBoolean(SettingsActivity.DISABLE_PROXIMITY, false) &&
			 prefs.getBoolean(SettingsActivity.TURNSCREENON, true))
			{
				sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
				proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
				sensorListener = new SensorEventListener()
				{
					@Override
					public void onAccuracyChanged(Sensor sensor, int accuracy) 
					{
					}
		
					@Override
					public void onSensorChanged(SensorEvent event) 
					{
						if (event.values[0] == 0)
						{
							deviceCovered = true;
						}
						else
						{
							if (deviceCovered)
							{
								deviceCovered = false;
								SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(NotificationsService.this);
								if (sharedPref.getBoolean(SettingsActivity.DELAYED_SCREEON, false) && newNotificationsAvailable)
								{
									turnScreenOn();
								}
							}
						}
					}				
				};
				startProximityMontior();
			}
		
	}
	public void startProximityMontior()
	{	
		if (proximitySensor != null)
		{
			sensorManager.registerListener(sensorListener, proximitySensor, SensorManager.SENSOR_DELAY_UI);
		}
		else registerProximitySensor();
			
	}	
	public void stopProximityMontior()
	{
		sensorManager.unregisterListener(sensorListener);
		deviceCovered = false;
	}

	/////////////////////////////////////////
	
	public void handleNotification(Notification n, String packageName)
	{
		if (n != null)
		{
			// handle only dismissable notifications
			if (!((n.flags & Notification.FLAG_NO_CLEAR) == Notification.FLAG_NO_CLEAR) &&
				!((n.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT))
			{
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
				boolean isScreenOn = false;
				if (!sharedPref.getBoolean(SettingsActivity.COLLECT_ON_UNLOCK, true))
				{
					PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
					isScreenOn = powerManager.isScreenOn();
					if (!isScreenOn && deviceIsUnlocked)
					{
						setDeviceIsLocked();
					}
				}

				// collect only on two sceneries: 1. the screen is off. 2. the screen is on but the device is unlocked.  
				if (!isScreenOn || (isScreenOn && !deviceIsUnlocked))
				{			
					boolean ignoreApp = sharedPref.getBoolean(packageName+"."+AppSettingsActivity.IGNORE_APP, false);
					if (!ignoreApp)
					{
						newNotificationsAvailable = true;
						
						turnScreenOn();
						
						// build notification data object
						NotificationData nd = new NotificationData();
						
						// extract app icons
						Resources res;
						try {
							res = getPackageManager().getResourcesForApplication(packageName);
							PackageInfo info = getPackageManager().getPackageInfo(packageName,0);
							nd.appicon = BitmapFactory.decodeResource(res, n.icon);
							if (nd.appicon == null)
							{
								nd.appicon = BitmapFactory.decodeResource(res, info.applicationInfo.icon);
							}
						} catch (NameNotFoundException e) 
						{
							nd.appicon = null;
						}
						
						if (n.largeIcon != null)
						{
							nd.icon = n.largeIcon;
						}
						else
						{
							try {
								res = getPackageManager().getResourcesForApplication(packageName);
								PackageInfo info = getPackageManager().getPackageInfo(packageName,0);
								nd.icon = BitmapFactory.decodeResource(res, info.applicationInfo.icon);
							} catch (NameNotFoundException e) 
							{
								nd.icon = null;
							}
						}
						
						if (n.tickerText != null)
						{
							nd.text = n.tickerText.toString();
						}
						else
						{
							ApplicationInfo ai;
							try 
							{
								ai = getPackageManager().getApplicationInfo(packageName, 0);
								nd.text = getPackageManager().getApplicationLabel(ai).toString();
							} catch (NameNotFoundException e) 
							{
								nd.text = packageName;
							}							
						}
						if (n.when != 0)
							nd.received = n.when;
						else
							nd.received = System.currentTimeMillis();
						nd.action = n.contentIntent;
						nd.count = 1;
						nd.packageName = packageName;
						nd.originalNotification = n.contentView;
						
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) 
						{
							nd.actions = getActionsFromNotification(n, packageName);
						}
				        
						// find layout background id
						try
						{							
							LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);						
							ViewGroup localView = (ViewGroup) inflater.inflate(nd.originalNotification.getLayoutId(), null);
							nd.originalNotification.reapply(getApplicationContext(), localView);
							nd.layoutId = localView.getId();
							View time = localView.findViewById(16908388);
							View title = localView.findViewById(notification_title_id);
							View subtitle = localView.findViewById(notification_subtext_id);
							View text = localView.findViewById(notification_text_id);
							View bigtext = localView.findViewById(big_notification_content_text);
							View image = localView.findViewById(notification_image_id);
							
							nd.hasTime = (time != null && time instanceof TextView);
							nd.hasTitle = (title != null && title instanceof TextView);
							nd.hasSubtitle = (subtitle != null && subtitle instanceof TextView);
							nd.hasText = (text != null && text instanceof TextView);
							nd.hasBigText = (bigtext != null && bigtext instanceof TextView);
							nd.hasImage = (image != null && image instanceof ImageView);
							if (!nd.hasImage)
							{
								// try to find an image
								nd.customImageId = recursiveFindFirstImage(localView);
							}														
						}
						catch (Exception exp)
						{
							nd.layoutId = 0;
						}
						
						if (sharedPref.getBoolean(nd.packageName+"."+AppSettingsActivity.USE_EXPANDED_TEXT, sharedPref.getBoolean(AppSettingsActivity.USE_EXPANDED_TEXT, true)))
						{
							getExpandedText(n,nd);							
						}
						
						// check for duplicated notification
						boolean keepOnlyLastNotification = sharedPref.getBoolean(nd.packageName+"."+AppSettingsActivity.KEEP_ONLY_LAST, false);
						int duplicated = -1;
						for(int i=0;i<notifications.size();i++)
						{
							if (nd.packageName.equals(notifications.get(i).packageName) &&
								(nd.text.equals(notifications.get(i).text) || keepOnlyLastNotification))
								{
									duplicated = i;
								}
						}
						if (duplicated >= 0)
						{
							NotificationData dup = notifications.get(duplicated);
							notifications.remove(duplicated);
							nd.count = dup.count+1;						
						}
						
						nd.normalNotification = createNormalNotification(nd);
						nd.smallNotification = createSmallNotification(nd);
						nd.largeNotification = createLargeNotification(nd);
						notifications.add(0,nd);						
				    	if (selectedIndex >= 0) selectedIndex++;
						
						// update widgets
						AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
						ComponentName widgetComponent = new ComponentName(this, NotificationsWidgetProvider.class);
						int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
						
						for (int i=0; i<widgetIds.length; i++) 
			            {
			            	AppWidgetManager.getInstance(this).notifyAppWidgetViewDataChanged(widgetIds[i], R.id.notificationsListView);
			            }	
						sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));
					}
				}
			}
			// handle persistent notifications
			else //if ((n.flags & Notification.FLAG_NO_CLEAR) == Notification.FLAG_NO_CLEAR)
			{
				// keep only the last persistent notification for the app
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
				boolean useExpanded = (sharedPref.getBoolean(packageName + "." + AppSettingsActivity.USE_EXPANDED_TEXT, 
									sharedPref.getBoolean(AppSettingsActivity.USE_EXPANDED_TEXT, true)));

				if (useExpanded && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
				{
					this.persistentNotifications.put(packageName, getExpandedContent(n));
				}
				else
				{
					this.persistentNotifications.put(packageName, n.contentView);
				}				
				updateWidget();
			}
		}
	}
		
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private RemoteViews getExpandedContent(Notification n) 
	{
		if (n.bigContentView != null)
			return n.bigContentView;
		else
			return n.contentView;
	}

	private Action[] getActionsFromNotification(Notification n, String packageName) 
	{
		ArrayList<Action> returnActions = new ArrayList<Action>();
		try
		{
			Object[] actions = null;
			Field fs = n.getClass().getDeclaredField("actions");
			if (fs != null)
			{
				fs.setAccessible(true);
				actions = (Object[]) fs.get(n);												
			}
			if (actions != null)
			{
				for(int i=0; i<actions.length; i++)
				{
					Action a = new Action();
					Class<?> actionClass=Class.forName("android.app.Notification$Action");
					a.icon = actionClass.getDeclaredField("icon").getInt(actions[i]);
					a.title = (CharSequence) actionClass.getDeclaredField("title").get(actions[i]);;
					a.actionIntent = (PendingIntent) actionClass.getDeclaredField("actionIntent").get(actions[i]);;					
					
					// find drawable 
					// extract app icons
					Resources res;
					try {
						res = getPackageManager().getResourcesForApplication(packageName);
						a.drawable = BitmapFactory.decodeResource(res, a.icon);						
					} catch (NameNotFoundException e) 
					{
						a.drawable = null;
					}
					returnActions.add(a);
				}
			}
		}
		catch(Exception exp)
		{
			
		}	
		Action[] returnArray = new Action[returnActions.size()];
		returnActions.toArray(returnArray);
		return returnArray;
	}
	
	private RemoteViews createNormalNotification(NotificationData nd) 
	{
		// create remoteview for normal notification
		RemoteViews n = new RemoteViews(getPackageName(), R.layout.normal_notification);
		
		n.setImageViewBitmap(R.id.notificationIcon, nd.icon);
		n.setImageViewBitmap(R.id.appIcon, nd.appicon);
		n.setTextViewText(R.id.notificationText, nd.text);
		if (nd.count > 1)
			n.setTextViewText(R.id.notificationCount, Integer.toString(nd.count));
    	else
    		n.setTextViewText(R.id.notificationCount, null);
		Time t = new Time();
    	t.set(nd.received);
    	String timeFormat = "%H:%M";
    	if (!DateFormat.is24HourFormat(this)) timeFormat = "%l:%M%P";
    	n.setTextViewText(R.id.notificationTime, t.format(timeFormat));	
    	return n;
	}
	
	private RemoteViews createLargeNotification(NotificationData nd) 
	{
		// create remoteview for normal notification
		RemoteViews n = new RemoteViews(getPackageName(), R.layout.large_notification);
		n.removeAllViews(R.id.largeNotificationContainer);
		n.addView(R.id.largeNotificationContainer, nd.originalNotification);
		return n;
	}
	
	private RemoteViews createSmallNotification(NotificationData nd) 
	{
		// create remoteview for small notification
		RemoteViews n = new RemoteViews(getPackageName(), R.layout.compact_notification);
		
		n.setImageViewBitmap(R.id.notificationIcon, nd.appicon);
		n.setTextViewText(R.id.notificationText, nd.text);
		Time t = new Time();
    	t.set(nd.received);
    	String timeFormat = "%H:%M";
    	if (!DateFormat.is24HourFormat(this)) timeFormat = "%l:%M%P";
    	n.setTextViewText(R.id.notificationTime, t.format(timeFormat));	    	
    	return n;
	}

	private void turnScreenOn() 
	{
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		
		// check if need to turn screen on
		Boolean turnScreenOn = sharedPref.getBoolean(SettingsActivity.TURNSCREENON, true);					
		if (turnScreenOn && !deviceCovered)
		{
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			// turn the screen on only if it was off
			if (!pm.isScreenOn())
			{
				final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Notification");
				wl.acquire();	
				
				// release after 5 seconds
				final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
				Runnable task = new Runnable() 
				{
				    public void run() 
				    {
				    	wl.release();
				    }
				};
				worker.schedule(task, 10, TimeUnit.SECONDS);
			}		
			newNotificationsAvailable = false;
		}	
	}

	private int recursiveFindFirstImage(ViewGroup v)
	{
		for(int i=0; i<v.getChildCount(); i++)
		{
			View child = v.getChildAt(i);
			if (child instanceof ViewGroup)
				recursiveFindFirstImage((ViewGroup)child);			
			if (child instanceof ImageView)
			{
				Drawable d = ((ImageView)child).getDrawable();
				if (d!=null)
				{
					return child.getId();
				}
			}			
		}
		return -1;
	}
	
	private void recursiveDetectNotificationsIds(ViewGroup v)
	{
		for(int i=0; i<v.getChildCount(); i++)
		{
			View child = v.getChildAt(i);
			if (child instanceof ViewGroup)
				recursiveDetectNotificationsIds((ViewGroup)child);
			else if (child instanceof TextView)
			{
				String text = ((TextView)child).getText().toString();
				int id = child.getId();
				if (text.equals("1")) notification_title_id = id;
				else if (text.equals("2")) notification_text_id = id;
				else if (text.equals("3")) notification_info_id = id;
				else if (text.equals("4")) notification_subtext_id = id;
				else if (text.equals("5")) big_notification_summary_id = id;
				else if (text.equals("6")) big_notification_content_title = id;
				else if (text.equals("7")) big_notification_content_text = id;
				else if (text.equals("8")) inbox_notification_event_1_id = id;
				else if (text.equals("9")) inbox_notification_event_2_id = id;
				else if (text.equals("10")) inbox_notification_event_3_id = id;				
				else if (text.equals("11")) inbox_notification_event_4_id = id;
				else if (text.equals("12")) inbox_notification_event_5_id = id;
				else if (text.equals("13")) inbox_notification_event_6_id = id;				
				else if (text.equals("14")) inbox_notification_event_7_id = id;
				else if (text.equals("15")) inbox_notification_event_8_id = id;
				else if (text.equals("16")) inbox_notification_event_9_id = id;				
				else if (text.equals("17")) inbox_notification_event_10_id = id;
			}
			else if (child instanceof ImageView)
			{
				Drawable d = ((ImageView)child).getDrawable();
				if (d!=null)
				{
					this.notification_image_id = child.getId();
				}
			}	
		}
	}	
		
	private void detectNotificationIds()
	{		
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
	    .setSmallIcon(R.drawable.appicon)
	    .setContentTitle("1")
	    .setContentText("2")
	    .setContentInfo("3")
	    .setSubText("4");

		Notification n = mBuilder.build();
				
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup localView;
	
		// detect id's from normal view
		localView = (ViewGroup) inflater.inflate(n.contentView.getLayoutId(), null);
		n.contentView.reapply(getApplicationContext(), localView);
		recursiveDetectNotificationsIds(localView);
		
		// detect id's from expanded views		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) 
		{
			NotificationCompat.BigTextStyle bigtextstyle = new NotificationCompat.BigTextStyle();
			bigtextstyle.setSummaryText("5");
			bigtextstyle.setBigContentTitle("6");
			bigtextstyle.bigText("7");			
					
			mBuilder.setStyle(bigtextstyle);
			detectExpandedNotificationsIds(mBuilder.build());
			
			NotificationCompat.InboxStyle inboxStyle =
			        new NotificationCompat.InboxStyle();
			String[] events = {"8","9","10","11","12","13","14","15","16","17"};
			inboxStyle.setBigContentTitle("6");
			inboxStyle.setSummaryText("5");
			
			for (int i=0; i < events.length; i++) 
			{	
			    inboxStyle.addLine(events[i]);
			}
			mBuilder.setStyle(inboxStyle);
			
			detectExpandedNotificationsIds(mBuilder.build());			
		}
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void detectExpandedNotificationsIds(Notification n)
	{
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup localView = (ViewGroup) inflater.inflate(n.bigContentView.getLayoutId(), null);
		n.bigContentView.reapply(getApplicationContext(), localView);
		recursiveDetectNotificationsIds(localView);
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void getExpandedText(Notification n, NotificationData nd)
	{
		String text = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{		
			RemoteViews view = n.bigContentView;
			
			if (n.bigContentView == null)
			{
				view = n.contentView;
			}	
			boolean hasParsableContent = true;
			ViewGroup localView = null;
			try
			{
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				localView = (ViewGroup) inflater.inflate(view.getLayoutId(), null);
				view.reapply(getApplicationContext(), localView);
			}
			catch (Exception exp)
			{
				hasParsableContent = false;				
			}
			if (hasParsableContent)
			{
				View v;						
				// try to get big text				
				v = localView.findViewById(big_notification_content_text);
				if (v != null && v instanceof TextView)
				{
					String s = ((TextView)v).getText().toString();
					if (!s.equals(""))
					{
						// add title string if available
						View titleView = localView.findViewById(android.R.id.title);
						if (v != null && v instanceof TextView)
						{
							String title = ((TextView)titleView).getText().toString();
							if (!title.equals(""))
								text = title + " " + s;
							else
								text = s;
						}
						else
							text = s;
					}
				}
				
				// if not found, try to get expanded content lines
				if (text == null)
				{
					// try to extract details lines 
					v = localView.findViewById(inbox_notification_event_1_id);
					if (v != null && v instanceof TextView) 
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text = s;
					}
					v = localView.findViewById(inbox_notification_event_2_id);
					if (v != null && v instanceof TextView) 
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text += "\n" + s;
					}
					
					v = localView.findViewById(inbox_notification_event_3_id);
					if (v != null && v instanceof TextView)  
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text += "\n" + s;
					}
					
					v = localView.findViewById(inbox_notification_event_4_id);
					if (v != null && v instanceof TextView)  
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text += "\n" + s;
					}
					
					v = localView.findViewById(inbox_notification_event_5_id);
					if (v != null && v instanceof TextView)  
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text += "\n" + s;
					}
					
					v = localView.findViewById(inbox_notification_event_6_id);
					if (v != null && v instanceof TextView)  
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text += "\n" + s;
					}
					
					v = localView.findViewById(inbox_notification_event_7_id);
					if (v != null && v instanceof TextView)  
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text += "\n" + s;
					}
					
					v = localView.findViewById(inbox_notification_event_8_id);
					if (v != null && v instanceof TextView)  
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text += "\n" + s;
					}
					
					v = localView.findViewById(inbox_notification_event_9_id);
					if (v != null && v instanceof TextView)  
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text += "\n" + s;
					}
					
					v = localView.findViewById(inbox_notification_event_10_id);
					if (v != null && v instanceof TextView)  
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text += "\n" + s;
					}
				}
				
				// if no content lines, try to get subtext
				if (text == null)
				{
					v = localView.findViewById(notification_subtext_id);
					if (v != null && v instanceof TextView)
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals(""))
						{
							text = s;
						}
					}
				}	
			}
			if (text!=null)
			{
				nd.text = text;
			}
			if (n.bigContentView != null)
			{
				nd.originalNotification = n.bigContentView;
			}
		}
	}
	
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) 
	{
		if (event != null)
		{
			// if it's notification
			if (event.getClassName().equals(android.app.Notification.class.getName()))
			{
				Notification n = (Notification)event.getParcelableData();
				handleNotification(n, event.getPackageName().toString());
			}
			else 
				if (event.getPackageName()!= null && event.getClassName() != null && event.getContentDescription() != null)
				{
					if (event.getPackageName().equals("com.android.systemui"))
						{
							if (event.getClassName().equals(android.widget.ImageView.class.getName()) &&
							 event.getContentDescription().equals(clearButtonName))
							{
								// clear notifications button clicked
								SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
				
								if (sharedPref.getBoolean(SettingsActivity.CLEAR_ON_CLEAR, false))
								{
									clearAllNotifications();
								}
							}
						}
				}
		}
	}
	
	public void keepOnForeground()
	{
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.KEEP_ON_FOREGROUND, false))
		{
			 Notification noti = new NotificationCompat.Builder(this)
	         .setContentTitle("Notifications Widget")
	         .setContentText("Notifications Widget Service is Active")
	         .setSmallIcon(R.drawable.appicon)
	         .setContentIntent(
	        		 PendingIntent.getActivity(this, 0, 
	        				 new Intent(this, MainActivity.class), 
	        				 PendingIntent.FLAG_UPDATE_CURRENT))
	         .build();
			noti.flags|=Notification.FLAG_NO_CLEAR;
			this.startForeground(0, noti);
		}
	}
	
	public void removeFromForeground()
	{
		this.stopForeground(true);
	}

	public int getNotificationsCount()
	{
		return notifications.size();
	}
	
	public NotificationData getNotification(int i)
	{
		if (i>=0 && i<notifications.size())
			return notifications.get(i);
		else
			return null;
	}
	
	public void removeNotification(int i)
	{
		if (i>=0 && i<notifications.size())
		{
			if (!notifications.get(i).pinned)
				notifications.remove(i);
		}
	}
	
	public void clearAllNotifications()
	{
		Iterator<NotificationData> i = notifications.iterator();
		while (i.hasNext()) 
		{
			NotificationData nd = i.next(); 
			if (!nd.pinned) i.remove();
		}
		updateWidget();			
	}
	
	private void updateWidget() 
	{
		Context ctx = getApplicationContext();
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(ctx);
		ComponentName widgetComponent = new ComponentName(ctx, NotificationsWidgetProvider.class);
		int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
		
		for (int i=0; i<widgetIds.length; i++) 
        {
        	AppWidgetManager.getInstance(ctx).notifyAppWidgetViewDataChanged(widgetIds[i], R.id.notificationsListView);
        }
		sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));		
	}

	public void setDeviceIsUnlocked()
	{
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.CLEAR_ON_UNLOCK, false))
		{
			clearAllNotifications();
		}
		deviceIsUnlocked = true;
	}
	
	public void setDeviceIsLocked()
	{
		deviceIsUnlocked = false;
	}
	
	@Override
	public void onInterrupt() 
	{
	}
	
	public boolean onUnbind(Intent intent) 
	{
		//Toast.makeText(this, "Notifications Service Stopped", Toast.LENGTH_LONG).show();
	    sSharedInstance = null;
	    stopProximityMontior();
	    return super.onUnbind(intent);
	}
	
	public void togglePinNotification(int pos)
	{
		if (pos >=0 && pos < notifications.size())
		{
			NotificationData n = notifications.get(pos);
			if (!n.pinned)
			{				
				n.pinned = true;
			}
			else
			{
				n.pinned = false;
			}
		}
	}

	public void launchNotification(int pos) 
	{
		if (pos >=0 && pos < notifications.size())
		{			
			try 
			{
				notifications.get(pos).action.send();
			} catch (Exception e) 
			{
				// if cannot launch intent, create a new one for the app
				try
				{
					Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(notifications.get(pos).packageName);
					startActivity(LaunchIntent);					
				}
				catch(Exception e2)
				{
					// cannot launch intent - do nothing...
					e2.printStackTrace();
					Toast.makeText(this, "Error - cannot launch app", Toast.LENGTH_SHORT).show();
				}
			}
			removeNotification(pos);
			updateWidget();
		}
	}
	
	public HashMap<String, RemoteViews> getPersistentNotifications() 
	{
		return persistentNotifications;
	}

	public boolean isWidgetLockerEnabled() 
	{
		return widgetLockerEnabled;
	}

	public void setWidgetLockerEnabled(boolean widgetLockerEnabled) 
	{
		this.widgetLockerEnabled = widgetLockerEnabled;
	}

	public int getSelectedIndex() 
	{
		return selectedIndex;
	}

	public void setSelectedIndex(int selectedIndex) 
	{
		this.selectedIndex = selectedIndex;
	}
	
	
}
