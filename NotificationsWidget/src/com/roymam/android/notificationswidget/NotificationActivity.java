package com.roymam.android.notificationswidget;

import android.app.Activity;
import android.app.PendingIntent.CanceledException;
import android.os.Bundle;

public class NotificationActivity extends Activity {
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    int pos=getIntent().getIntExtra(NotificationsWidgetProvider.EXTRA_APP_ID,-1);
    NotificationsService ns = NotificationsService.getSharedInstance();
    if (pos != -1)
    {
	    if (ns != null)
	    {
			try 
		    {
				ns.getNotifications().get(pos).action.send();
				ns.getNotifications().remove(pos);
		    } catch (CanceledException e) 
			{
			}
	    }
  	}
    finish();
  }
}