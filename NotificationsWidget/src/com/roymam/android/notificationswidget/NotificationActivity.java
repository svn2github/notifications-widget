package com.roymam.android.notificationswidget;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class NotificationActivity extends Activity {
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    
    String word=getIntent().getStringExtra(NotificationsWidgetProvider.EXTRA_APP_ID);
    
    if (word==null) {
      word="We did not get an app id!";
    }
    
    Toast.makeText(this, word, Toast.LENGTH_LONG).show();
    
    finish();
  }
}