package com.roymam.android.notificationswidget;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	public static String TURNSCREENON = "turnscreenon";
	public static String DISABLE_PROXIMITY = "disableproximity";
	public static String CLOCK_STYLE = "clockstyle";
	public static String SHOW_CLEAR_BUTTON= "showclearbutton";
	public static String NOTIFICATION_BG_OPACITY="notification_bg_opacity";
	public static String CLEAR_ON_UNLOCK = "clearonunlock";
	public static String COLLECT_ON_UNLOCK = "collectonunlock";
	public static String CLEAR_ON_CLEAR = "clearonclear";
	public static String CLOCK_SMALL = "small";
	public static String CLOCK_LARGE = "large";
	public static String CLOCK_HIDDEN = "hidden";
	public static String CLOCK_AUTO = "auto";
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
	
	@Override
	protected void onResume() 
	{
	    super.onResume();
	    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() 
	{
	    super.onPause();
	    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
	{
		NotificationsService ns = NotificationsService.getSharedInstance();
		if (key.equals(DISABLE_PROXIMITY) && ns != null) 
        {
			if (!getPreferenceScreen().getSharedPreferences().getBoolean(SettingsActivity.DISABLE_PROXIMITY, false))
			{
				ns.startProximityMontior();
			}
			else 
			{
				ns.stopProximityMontior();
			}
        }	
	}
}
