package com.roymam.android.notificationswidget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity 
{
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		boolean serviceEnabled = NotificationsService.getSharedInstance(this)!=null;
		boolean widgetEnabled = NotificationsWidgetService.widgetActive;
		if (serviceEnabled & widgetEnabled)
		{
			Intent launchSettings = new Intent(this, SettingsActivity.class);
		    startActivity(launchSettings);
		}
		else
		{
			Intent launchWizard = new Intent(this, WizardActivity.class);
		    startActivity(launchWizard);
		}
		finish();
	}
}