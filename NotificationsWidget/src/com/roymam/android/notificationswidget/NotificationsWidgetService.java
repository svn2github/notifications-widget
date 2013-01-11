package com.roymam.android.notificationswidget;
import java.util.List;

import android.content.Intent;
import android.content.IntentFilter;
import android.widget.RemoteViewsService;
import android.view.accessibility.AccessibilityEvent;

public class NotificationsWidgetService extends RemoteViewsService 
{	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		return START_STICKY;
	}

	@Override
	  public RemoteViewsFactory onGetViewFactory(Intent intent) 
	  {  
		  return(new NotificationsViewFactory(this.getApplicationContext(), intent));
	  }
	  
}
