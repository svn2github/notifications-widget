package com.roymam.android.notificationswidget;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
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
			Notification n = (Notification)event.getParcelableData();
			
			if (n != null)
			{
				if (!((n.flags & Notification.FLAG_NO_CLEAR) == Notification.FLAG_NO_CLEAR) &&
					!((n.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) &&
					! n.tickerText.toString().equals("")
							)
				{					
					//PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
					//PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Notification");
					//wl.acquire();

					NotificationData nd = new NotificationData();
					nd.icon = n.largeIcon;
					nd.text = n.tickerText.toString();
					nd.received = n.when;
					
					notifications.add(0,nd);					
					Intent intent = new Intent(NotificationsWidgetProvider.NOTIFICATION_CREATED_ACTION);							
					getApplicationContext().sendBroadcast(intent);
					
					//AppWidgetManager widgetManager = AppWidgetManager.getInstance(getApplicationContext());
					//ComponentName widgetComponent = new ComponentName(getApplicationContext(), NotificationsWidgetProvider.class);
					//int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
					//Intent update = new Intent();
					//update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
					//update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, getPackageName());
					//update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
					//getApplicationContext().sendBroadcast(update);
					
					//Do whatever you need right here
					//wl.release();
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
