package com.roymam.android.notificationswidget;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.PendingIntent.CanceledException;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

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
			try 
		    {
				if (pos < ns.getNotifications().size())
				{
					ns.getNotifications().get(pos).action.send();
					ns.getNotifications().remove(pos);
				}
		    } catch (CanceledException e) 
			{
			}
	    }
  	}
    finish();
  }
}