package com.roymam.android.notificationswidget;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
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
		if (!event.getPackageName().equals("com.jim2"))
		{
			Notification n = (Notification)event.getParcelableData();
			
			if (n != null)
			{
				if (!((n.flags & Notification.FLAG_NO_CLEAR) == Notification.FLAG_NO_CLEAR))
				{
					NotificationData nd = new NotificationData();
					nd.icon = n.largeIcon;
					nd.text = n.tickerText.toString();
					nd.received = Calendar.getInstance().getTime();
					
					notifications.add(0,nd);					
					Intent intent = new Intent(NotificationsWidgetProvider.NOTIFICATION_CREATED_ACTION);
					intent.putExtra("PackageName", event.getPackageName());			
					getApplicationContext().sendBroadcast(intent);
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
