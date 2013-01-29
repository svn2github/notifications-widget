package com.roymam.android.notificationswidget;
import android.content.Intent;
import android.widget.RemoteViewsService;

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
