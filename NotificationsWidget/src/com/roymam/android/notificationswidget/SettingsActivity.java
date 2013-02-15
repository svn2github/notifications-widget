package com.roymam.android.notificationswidget;

import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	public static String TURNSCREENON = "turnscreenon";
	public static String DISABLE_PROXIMITY = "disableproximity";
	public static String KEEP_ON_FOREGROUND = "keep_on_foreground";
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
	
	public static class PrefsGeneralFragment extends PreferenceFragment 
	{
	    @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        super.onCreate(savedInstanceState);

	        // Load the preferences from an XML resource
	        addPreferencesFromResource(R.xml.preferences);
	    }
	}
	
	public static class PrefsAppearanceFragment extends PreferenceFragment 
	{
	    @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        super.onCreate(savedInstanceState);

	        // Load the preferences from an XML resource
	        addPreferencesFromResource(R.xml.appearancepreferences);
	    }
	}
	
	public static class PrefsAdvancedFragment extends PreferenceFragment 
	{
	    @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        super.onCreate(savedInstanceState);

	        // Load the preferences from an XML resource
	        addPreferencesFromResource(R.xml.advancedpreferences);
	    }
	}
	
	@Override
    public void onBuildHeaders(List<Header> target) 
	{
        loadHeadersFromResource(R.xml.preferences_headers, target);
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
    }
	
	@Override
	protected void onResume() 
	{
	    super.onResume();	    
	    PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() 
	{
	    super.onPause();
	    PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		NotificationsService ns = NotificationsService.getSharedInstance();
		if ((key.equals(DISABLE_PROXIMITY) || key.equals(TURNSCREENON)) && ns != null) 
        {
			if (!prefs.getBoolean(SettingsActivity.DISABLE_PROXIMITY, false) &&
				 prefs.getBoolean(SettingsActivity.TURNSCREENON, true))
			{
				ns.startProximityMontior();
			}
			else 
			{
				ns.stopProximityMontior();
			}
        }
		else if (key.equals(KEEP_ON_FOREGROUND))
		{
			if (prefs.getBoolean(SettingsActivity.KEEP_ON_FOREGROUND, false))
			{
				NotificationsService.getSharedInstance().keepOnForeground();
			}
			else
			{
				NotificationsService.getSharedInstance().removeFromForeground();
			}
		}
	}
}
