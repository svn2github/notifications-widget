package com.roymam.android.notificationswidget;
import android.app.PendingIntent;
import android.graphics.Bitmap;

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

	CharSequence 	text;
	CharSequence	title;
	CharSequence	content;
	Bitmap 	icon;
	Bitmap 	appicon;
	long	received;
	String	packageName;
	PendingIntent action;
	int 	count;
	boolean pinned = false;	
	public Action[] actions = null;
	public int priority;
}
