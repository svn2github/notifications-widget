package com.roymam.android.notificationswidget;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity 
{
	public static String TURNSCREENON = "turnscreenon";
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
}
