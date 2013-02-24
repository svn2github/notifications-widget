package com.roymam.android.notificationswidget;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.WindowManager;

public class AppSettingsActivity extends PreferenceActivity 
{
	public static String EXTRA_PACKAGE_NAME = "com.roymam.android.notificationswidget.packagename";
	public static String IGNORE_APP = ".ignoreapp";
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		String packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
		
		// get package title
		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(packageName, 0);
			String appName = getPackageManager().getApplicationLabel(ai).toString();
			if (appName == null) appName = packageName;
			setTitle(appName + " - " + getString(R.string.app_specific_settings_title));
		} catch (NameNotFoundException e) {
			setTitle(packageName + " - " + getString(R.string.app_specific_settings_title));
		}
		
		super.onCreate(savedInstanceState);

		// add app specific settings
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
		
		// Blacklist preference
        CheckBoxPreference ignoreNotificationsPref = new CheckBoxPreference(this);
        ignoreNotificationsPref.setKey(packageName+"."+IGNORE_APP);
        ignoreNotificationsPref.setTitle(R.string.ignore_notifications);
        ignoreNotificationsPref.setSummary(R.string.ignore_notifications_summary);
        root.addPreference(ignoreNotificationsPref);
           
        setPreferenceScreen(root);

	}
	
}