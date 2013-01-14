package com.roymam.android.notificationswidget;
import android.app.PendingIntent;
import android.graphics.Bitmap;

public class NotificationData 
{
	String 	text;
	Bitmap 	icon;
	Bitmap 	appicon;
	long	received;
	String	packageName;
	PendingIntent action;
	int 	count;
}
