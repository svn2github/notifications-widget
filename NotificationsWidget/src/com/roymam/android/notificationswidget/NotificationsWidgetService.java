package com.roymam.android.notificationswidget;
import android.content.Intent;
import android.widget.RemoteViewsService;

public class NotificationsWidgetService extends RemoteViewsService 
{
	  @Override
	  public RemoteViewsFactory onGetViewFactory(Intent intent) 
	  {
	    return(new NotificationsViewFactory(this.getApplicationContext(), intent));
	  }
	  
	  
}
