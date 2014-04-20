package com.roymam.android.notificationswidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class PersistentNotificationSettingsActivity extends SpecificSettingsPreferencesActivity
{
	public static final String USE_EXPANDED_TEXT = "useexpandedtext";
	public static final String SHOW_PERSISTENT_NOTIFICATION = "showpersistent";
	public static final String PERSISTENT_NOTIFICATION_HEIGHT = "persistent_notification_height";
    public static final String CATCH_ALL_NOTIFICATIONS = "catch_all_notifications";
    public static final String HIDE_WHEN_NOTIFICATIONS = "hidewhennotifications";
    public static final String HIDE_CLOCK_WHEN_VISIBLE = "hide_clock_when_visible";
    public static final String PN_TIMEOUT = "pn_timeout";
	public static final String PERSISTENT_APPS = "persistent_apps";

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
        onCreate(savedInstanceState, R.string.persistent_notifications, R.layout.appearance_settings_view, R.xml.persistent_notifications_settings);
	}

	public static void removeAppFromPersistentNotifications(String packageName, Context ctx)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		
		String specficApps = prefs.getString(PersistentNotificationSettingsActivity.PERSISTENT_APPS, "");
		String updatedSpecificApps = "";
		for(String app:specficApps.split(","))
		{
			if (!app.equals(packageName))
			{
				if (updatedSpecificApps.isEmpty())
					updatedSpecificApps = app;
				else
					updatedSpecificApps+=","+app;
			}
		}
		prefs.edit().putString(PersistentNotificationSettingsActivity.PERSISTENT_APPS, updatedSpecificApps).commit();
	}
	
	public static void addAppToPersistentNotifications(String packageName, Context ctx)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	
		String specificApps = prefs.getString(PersistentNotificationSettingsActivity.PERSISTENT_APPS, "");
		boolean hasApp = false;
		for (String token : specificApps.split(",")) 
		{
		     if (token.equals(packageName)) hasApp = true;
		}
		if (!hasApp)
		{
			// add this app to the list of specific apps
			if (!specificApps.equals(""))
				specificApps+= ",";
			specificApps+= packageName; 
		}
		
		prefs.edit().putString(PersistentNotificationSettingsActivity.PERSISTENT_APPS, specificApps).commit();		
	}
}