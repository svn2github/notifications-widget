package com.roymam.android.notificationswidget;

import android.app.PendingIntent;
import android.widget.RemoteViews;

public class PersistentNotification 
{
	public String packageName;
	public RemoteViews content;
	public RemoteViews expandedContent;
	public PendingIntent contentIntent;
	public long recieved;
}
