package com.roymam.android.notificationswidget;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

public class NotificationActivity extends Activity 
{
  @Override
  public void onCreate(Bundle state) 
  {
    super.onCreate(state);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

    int pos=getIntent().getIntExtra(NotificationsWidgetProvider.NOTIFICATION_INDEX,-1);
    NotificationsProvider ns = NotificationsService.getSharedInstance();

    if (ns != null &&
        pos >= 0 && pos < ns.getNotifications().size())
    {
        NotificationData nd = ns.getNotifications().get(pos);
        nd.launch(getApplicationContext());
        ns.clearNotification(nd.uid);
  	}
    finish();
  }
}