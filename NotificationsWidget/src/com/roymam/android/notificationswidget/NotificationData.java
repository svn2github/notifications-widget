package com.roymam.android.notificationswidget;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.widget.RemoteViews;

public class NotificationData 
{	
	String 	text;
	Bitmap 	icon;
	Bitmap 	appicon;
	long	received;
	String	packageName;
	PendingIntent action;
	int 	count;
	boolean pinned = false;
	
	// indicator for content view
	boolean hasTime = false;
	boolean hasTitle = false;
	boolean hasSubtitle = false;
	boolean hasText = false;
	boolean hasBigText = false;
	boolean hasImage = false;
	
	// data from notification remoteviews
	RemoteViews smallNotification;
	RemoteViews normalNotification;
	RemoteViews originalNotification;
	int layoutId;
	
	
	
}
