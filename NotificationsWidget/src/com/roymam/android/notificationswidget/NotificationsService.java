package com.roymam.android.notificationswidget;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.view.accessibility.AccessibilityEvent;

public class NotificationsService extends AccessibilityService {

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		System.out.println("onServiceConnected");
	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
	    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
	    info.notificationTimeout = 100;
	    info.feedbackType = AccessibilityEvent.TYPES_ALL_MASK;
	    setServiceInfo(info);
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		Intent intent = new Intent(NotificationsWidgetProvider.NOTIFICATION_CREATED_ACTION);
		intent.putExtra("NotificationString", event.getText().toString());
		getApplicationContext().sendBroadcast(intent);
		
	}

	@Override
	public void onInterrupt() {
		// TODO Auto-generated method stub
		
	}

}
