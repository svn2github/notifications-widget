package com.roymam.android.notificationswidget;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Notification;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;

public class NotificationsService extends AccessibilityService {

	private static NotificationsService sSharedInstance;
	private List<NotificationData> notifications;

	public boolean onUnbind(Intent intent) 
	{
	    sSharedInstance = null;
	    return super.onUnbind(intent);
	}

	public static NotificationsService getSharedInstance() 
	{
	    return sSharedInstance;
	}
	
	@Override
	protected void onServiceConnected() 
	{
		super.onServiceConnected();
		System.out.println("onServiceConnected");
	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
	    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
	    info.notificationTimeout = 100;
	    info.feedbackType = AccessibilityEvent.TYPES_ALL_MASK;
	    setServiceInfo(info);
	    sSharedInstance = this;
	    notifications = new ArrayList<NotificationData>();
	}	

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		if (event != null)
		{
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			Notification n = (Notification)event.getParcelableData();
			
			if (n != null)
			{
				if (!((n.flags & Notification.FLAG_NO_CLEAR) == Notification.FLAG_NO_CLEAR) &&
					!((n.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) &&
					 n.tickerText != null
							)
				{	
					Context ctx = getApplicationContext();
										
					Boolean turnScreenOn = sharedPref.getBoolean(SettingsActivity.TURNSCREENON, true);					
					if (turnScreenOn)
					{
						PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
						PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Notification");
						wl.acquire();
						wl.release();
					}
					
					NotificationData nd = new NotificationData();
					
					// extract app icon
					Resources res;
					try {
						res = ctx.getPackageManager().getResourcesForApplication(event.getPackageName().toString());
						PackageInfo info = ctx.getPackageManager().getPackageInfo(event.getPackageName().toString(),0);
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
							res = ctx.getPackageManager().getResourcesForApplication(event.getPackageName().toString());
							PackageInfo info = ctx.getPackageManager().getPackageInfo(event.getPackageName().toString(),0);
							nd.icon = BitmapFactory.decodeResource(res, info.applicationInfo.icon);
						} catch (NameNotFoundException e) 
						{
							nd.icon = null;
						}
					}
					
					nd.text = n.tickerText.toString();
					nd.received = n.when;
					nd.action = n.contentIntent;
					nd.count = 1;
					nd.packageName = event.getPackageName().toString();
					
					// check for duplicated notification
					int duplicated = -1;
					for(int i=0;i<notifications.size();i++)
					{
						if (nd.packageName.equals(notifications.get(i).packageName) &&
							nd.text.equals(notifications.get(i).text))
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
					
//					Intent intent = new Intent(NotificationsWidgetProvider.NOTIFICATION_CREATED_ACTION);							
//					ctx.sendBroadcast(intent);
//					
					AppWidgetManager widgetManager = AppWidgetManager.getInstance(ctx);
					ComponentName widgetComponent = new ComponentName(ctx, NotificationsWidgetProvider.class);
					int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
					
					for (int i=0; i<widgetIds.length; i++) 
		            {
		            	AppWidgetManager.getInstance(ctx).notifyAppWidgetViewDataChanged(widgetIds[i], R.id.notificationsListView);
		            }													
				}
			}
		}
	}

	@Override
	public void onInterrupt() {
		// TODO Auto-generated method stub		
	}
	
	public List<NotificationData> getNotifications()
	{
		return notifications;
	}
}
