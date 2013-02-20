package com.roymam.android.notificationswidget;

import java.util.List;

import com.roymam.android.common.ListPreferenceChangeListener;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	public static String TURNSCREENON = "turnscreenon";
	public static String DISABLE_PROXIMITY = "disableproximity";
	public static String KEEP_ON_FOREGROUND = "keep_on_foreground";
	public static String CLOCK_STYLE = "clockstyle";
	//public static String SHOW_CLEAR_BUTTON= "showclearbutton";
	public static String CLEAR_BUTTON_MODE = "clearbuttonmode";
	public static String SHOW_EDIT_BUTTON= "showeditbutton";
	public static String NOTIFICATION_BG_OPACITY="notification_bg_opacity";
	public static String TEXT_COLOR = "notification_text_color";
	public static String TIME_COLOR = "notification_time_color";
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
	        
	        // set list preferences auto summary
	        // clear mode
	        final Preference clearPref = findPreference(CLEAR_BUTTON_MODE);	        
	        String currValue = getPreferenceScreen().getSharedPreferences().getString(CLEAR_BUTTON_MODE, "visibile");
	        
	        ListPreferenceChangeListener listener = new ListPreferenceChangeListener(
	        		getResources().getStringArray(R.array.settings_buttons_entries),
	        		getResources().getStringArray(R.array.settings_buttons_values));
	        listener.setPrefSummary(clearPref, (String)currValue);
	        clearPref.setOnPreferenceChangeListener(listener);
	        	        
	        // text / time colors
	        listener = new ListPreferenceChangeListener(
	        		getResources().getStringArray(R.array.settings_colors_entries),
	        		getResources().getStringArray(R.array.settings_colors_values));
	        
	        Preference textColorPref = findPreference(TEXT_COLOR);	        
	        currValue = getPreferenceScreen().getSharedPreferences().getString(TEXT_COLOR, "white");	        
	        listener.setPrefSummary(textColorPref, (String)currValue);
	        textColorPref.setOnPreferenceChangeListener(listener);
	        
	        Preference timeColorPref = findPreference(TIME_COLOR);	        
	        currValue = getPreferenceScreen().getSharedPreferences().getString(TIME_COLOR, "blue");	        
	        listener.setPrefSummary(timeColorPref, (String)currValue);
	        timeColorPref.setOnPreferenceChangeListener(listener);
	        
	        // clock style
	        listener = new ListPreferenceChangeListener(
	        		getResources().getStringArray(R.array.settings_clock_entries),
	        		getResources().getStringArray(R.array.settings_clock_values));
	        
	        Preference clockPref = findPreference(CLOCK_STYLE);	        
	        currValue = getPreferenceScreen().getSharedPreferences().getString(CLOCK_STYLE, "auto");	        
	        listener.setPrefSummary(clockPref, (String)currValue);
	        clockPref.setOnPreferenceChangeListener(listener);
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
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) 
	{		
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
