package com.roymam.android.notificationswidget;

import android.app.Activity;
import android.os.Bundle;

public class NotificationActivity extends Activity 
{
  @Override
  public void onCreate(Bundle state) 
  {
    super.onCreate(state);
    
    int pos=getIntent().getIntExtra(NotificationsWidgetProvider.NOTIFICATION_INDEX,-1);
    NotificationsService ns = NotificationsService.getSharedInstance();
    if (pos != -1)
    {
	    if (ns != null)
	    {
	    	ns.launchNotification(pos);
		}
  	}
    finish();
  }
}