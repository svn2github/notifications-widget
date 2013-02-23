package com.roymam.android.notificationswidget;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

public class NotificationData 
{
	String 	title;
	String 	info;
	String	details;
	String 	text;
	Bitmap 	icon;
	Bitmap 	appicon;
	long	received;
	String	packageName;
	PendingIntent action;
	int 	count;
	RemoteViews notificationContent;
}
