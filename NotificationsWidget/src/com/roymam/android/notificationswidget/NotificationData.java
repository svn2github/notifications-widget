package com.roymam.android.notificationswidget;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

public class NotificationData 
{	
	public static class Action
	{
		public Action() {};
		public int icon;
		public CharSequence title;
		public PendingIntent actionIntent;
		public Bitmap drawable;
	}

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
	RemoteViews largeNotification;
	RemoteViews originalNotification;
	int layoutId;
	int customImageId = -1;
	public Action[] actions = null;
}
