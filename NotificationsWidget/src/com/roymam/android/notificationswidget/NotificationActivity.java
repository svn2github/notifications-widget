package com.roymam.android.notificationswidget;

import android.app.Activity;
import android.app.PendingIntent.CanceledException;
import android.os.Bundle;
import android.widget.Toast;

public class NotificationActivity extends Activity {
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    
    int pos=getIntent().getIntExtra(NotificationsWidgetProvider.EXTRA_APP_ID,-1);
    
    if (pos != -1)
	try 
    {
		NotificationsService.getSharedInstance().getNotifications().get(pos).action.send();
    } catch (CanceledException e) 
	{
	}
    
    finish();
  }
}