package com.roymam.android.notificationswidget;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.widget.RemoteViews;

public class NotificationData 
{
	String 	title;
	String 	subtext;
	String 	info;
	String	details;
	String 	text;
	String  time;
	Bitmap 	icon;
	Bitmap 	appicon;
	long	received;
	String	packageName;
	PendingIntent action;
	int 	count;
	
	// data from notification remoteviews
	RemoteViews normalNotification;
	RemoteViews notificationContent;
	RemoteViews notificationExpandedContent;
	int layoutId;
}
