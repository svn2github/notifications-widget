package com.roymam.android.notificationswidget;
import java.util.List;

import android.content.Intent;
import android.widget.RemoteViewsService;
import android.view.accessibility.AccessibilityEvent;

public class NotificationsWidgetService extends RemoteViewsService 
{
	  @Override
	  public RemoteViewsFactory onGetViewFactory(Intent intent) 
	  {  
		  return(new NotificationsViewFactory(this.getApplicationContext(), intent));
	  }
	  
}
