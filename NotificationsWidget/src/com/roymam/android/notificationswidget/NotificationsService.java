package com.roymam.android.notificationswidget;

import java.util.ArrayList;
import java.util.List;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.webkit.WebView.FindListener;
import android.widget.TextView;
import android.widget.Toast;

public class NotificationsService extends AccessibilityService 
{
	private static NotificationsService sSharedInstance;
	private List<NotificationData> notifications;
	private boolean deviceIsUnlocked = true;
	private boolean deviceCovered = false;
	private boolean editMode =  false;
	private String clearButtonName = "Clear all notifications.";
	
	private final int CONTENT_TEXT_ID = 16908309;
	private final int CONTENT_SUBTEXT_ID = 16908358;
	private final int CONTENT_INFO_ID = 16909095;	
	private final int EXPANDED_LINE_1_ID = 16909100;
	private final int EXPANDED_LINE_2_ID = 16909101;
	private final int EXPANDED_LINE_3_ID = 16909103;
	private final int BIG_TEXT_ID = 16909096;
	

	public static NotificationsService getSharedInstance() { return sSharedInstance; }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	@Override
	protected void onServiceConnected() 
	{
		super.onServiceConnected();
		sSharedInstance = this;
		
		AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		
		// check if "Clear Notifications Monitor" feature enabled, if so - monitor view clicks
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.CLEAR_ON_CLEAR, false))
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
	    
	    // register proximity change sensor
		registerProximitySensor();
			
		// keep app on foreground if requested
		keepOnForeground();
	}	
	
	// Proximity Sensor Monitoring
	SensorManager sensorManager;
	Sensor proximitySensor;
	SensorEventListener sensorListener;
	
	private void registerProximitySensor()
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
					deviceCovered = false;
				}
			}				
		};
		
		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.DISABLE_PROXIMITY, false) &&
		     PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.TURNSCREENON, true))
		{
			startProximityMontior();
		}
	}
	public void startProximityMontior()
	{		       
		if (proximitySensor != null)
		{
			sensorManager.registerListener(sensorListener, proximitySensor, 5);
		}		
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
				!((n.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) && 
				 n.tickerText != null)
			{
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
				boolean isScreenOn = false;
				if (!sharedPref.getBoolean(SettingsActivity.COLLECT_ON_UNLOCK, true))
				{
					PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
					isScreenOn = powerManager.isScreenOn();
					if (!isScreenOn)
					{
						if (deviceIsUnlocked && sharedPref.getBoolean(SettingsActivity.CLEAR_ON_LOCK, false))
						{
							clearAllNotifications();
						}
						deviceIsUnlocked = false;
					}
				}

				// collect only on two sceneries: 1. the screen is off. 2. the screen is on but the device is unlocked.  
				if (!isScreenOn || (isScreenOn && !deviceIsUnlocked))
				{			
					boolean ignoreApp = sharedPref.getBoolean(packageName+"."+AppSettingsActivity.IGNORE_APP, false);
					if (!ignoreApp)
					{
						// check if need to turn screen on
						Boolean turnScreenOn = sharedPref.getBoolean(SettingsActivity.TURNSCREENON, true);					
						if (turnScreenOn && !deviceCovered)
						{
							PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
							PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Notification");
							wl.acquire();
							wl.release();
						}
						
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
						
						nd.text = n.tickerText.toString();
						if (n.when != 0)
							nd.received = n.when;
						else
							nd.received = System.currentTimeMillis();
						nd.action = n.contentIntent;
						nd.count = 1;
						nd.packageName = packageName;
						nd.notificationContent = n.contentView;
						//nd.notificationExpandedContent = n.bigContentView;
						
						// try to extract extra content from view
						try
						{
							LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
							ViewGroup localView = (ViewGroup) inflater.inflate(nd.notificationContent.getLayoutId(), null);
							nd.notificationContent.reapply(getApplicationContext(), localView);
							nd.layoutId = localView.getId();
							
							View tv = localView.findViewById(android.R.id.title);
							if (tv != null && tv instanceof TextView) nd.title = ((TextView) tv).getText().toString();
							tv = localView.findViewById(CONTENT_SUBTEXT_ID);
							if (tv != null && tv instanceof TextView) nd.subtext = ((TextView) tv).getText().toString();
							tv = localView.findViewById(CONTENT_INFO_ID);
							if (tv != null && tv instanceof TextView) nd.info = ((TextView) tv).getText().toString();
							tv = localView.findViewById(16908388);
							if (tv != null && tv instanceof TextView) nd.time = ((TextView) tv).getText().toString();
						}
						catch (Exception exp)
						{
							nd.layoutId = 0;
						}
						
						if (sharedPref.getBoolean(nd.packageName+"."+AppSettingsActivity.USE_EXPANDED_TEXT, false))
						{
							String expandedText = getExpandedText(n);
							if (expandedText!=null)
							{
								nd.text = expandedText;
							}
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
						notifications.add(0,nd);
	
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
		}
	}	
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private String getExpandedText(Notification n)
	{
		String text = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{		
			if (n.bigContentView != null)
			{
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				ViewGroup localView = (ViewGroup) inflater.inflate(n.bigContentView.getLayoutId(), null);
				n.bigContentView.reapply(getApplicationContext(), localView);
				View v;		
				
				// try to get big text				
				v = localView.findViewById(this.BIG_TEXT_ID);
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
					v = localView.findViewById(EXPANDED_LINE_1_ID);
					if (v != null && v instanceof TextView) 
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text = s;
					}
					v = localView.findViewById(EXPANDED_LINE_2_ID);
					if (v != null && v instanceof TextView) 
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text += "\n" + s;
					}
					
					v = localView.findViewById(EXPANDED_LINE_3_ID);
					if (v != null && v instanceof TextView)  
					{
						String s = ((TextView)v).getText().toString();
						if (!s.equals("")) text += "\n" + s;
					}
				}
				
				// if no content lines, try to get subtext
				if (text == null)
				{
					v = localView.findViewById(this.CONTENT_SUBTEXT_ID);
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
		}
		
		return text;
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

	public List<NotificationData> getNotifications()
	{
		return notifications;
	}
	
	public void clearAllNotifications()
	{
		notifications.clear();
		
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
	
	public void setEditMode(boolean mode)
	{
		editMode = mode;
	}
	
	public boolean isEditMode()
	{
		return editMode;
	}
	
	@Override
	public void onInterrupt() 
	{
	}
	
	public boolean onUnbind(Intent intent) 
	{
	    sSharedInstance = null;
	    stopProximityMontior();
	    return super.onUnbind(intent);
	}

	public void launchNotification(int pos) 
	{
		if (pos >=0 && pos < notifications.size())
		{
			try 
			{
				notifications.get(pos).action.send();
				notifications.remove(pos);
				
				// update notifications list
				AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
				ComponentName widgetComponent = new ComponentName(this, NotificationsWidgetProvider.class);
				int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
				
				for (int i=0; i<widgetIds.length; i++) 
		        {
		        	AppWidgetManager.getInstance(this).notifyAppWidgetViewDataChanged(widgetIds[i], R.id.notificationsListView);
		        }
				sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));	
			} catch (CanceledException e) 
			{
				Toast.makeText(getApplicationContext(), "Cannot open notification", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	
}
