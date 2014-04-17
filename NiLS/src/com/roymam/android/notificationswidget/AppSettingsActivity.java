package com.roymam.android.notificationswidget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.roymam.android.common.ListPreferenceChangeListener;

public class AppSettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener 
{
	public static final String EXTRA_PACKAGE_NAME = "com.roymam.android.notificationswidget.packagename";
	public static final String IGNORE_APP = "ignoreapp";
    public static final String IGNORE_EMPTY_NOTIFICATIONS = "ignore_empty_notifications";
	private static final String KEEP_ONLY_LAST = "showlast";
    private static final String MULTIPLE_EVENTS_HANDLING = "multiple_events_handling";
    private static final String TRY_EXTRACT_TITLE = "try_extract_title";
	public static final String USE_EXPANDED_TEXT = "useexpandedtext";
	public static final String APP_PRIORITY = "apppriority";
    private static final String ALWAYS_USE_APP_ICON = "use_app_icon";
	
	private String packageName;

    @Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
		
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

        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.activity_app_settings, null);
        setContentView(v);

        addPreferencesFromResource(R.xml.app_specific_settings);
        PreferenceScreen prefScreen = getPreferenceScreen();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        for(int i=0; i<prefScreen.getPreferenceCount();i++)
        {
            Preference pref = prefScreen.getPreference(i);
            String key = packageName + "." + pref.getKey();

            if (pref instanceof ListPreference)
            {
                ListPreference listPref = ((ListPreference) pref);
                String globalValue = listPref.getValue();
                String currValue = sharedPrefs.getString(key, globalValue);

                listPref.setKey(key);
                listPref.setValue(currValue);

                // set summary from current value
                ListPreferenceChangeListener listener = new ListPreferenceChangeListener(
                        null, listPref.getEntries(), listPref.getEntryValues());

                listener.setPrefSummary(listPref, currValue);
                listPref.setOnPreferenceChangeListener(listener);
            }
        }
	}

    public void resetAppSettings(View v)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppSettingsActivity.this);
        prefs.edit().remove(packageName + "." + SettingsActivity.WAKEUP_MODE)
                    .remove(packageName + "." + SettingsActivity.NOTIFICATION_MODE)
                    .remove(packageName + "." + SettingsActivity.NOTIFICATION_ICON)
                    .remove(packageName + "." + IGNORE_APP)
                    .commit();

        removeAppFromAppSpecificSettings(packageName, AppSettingsActivity.this);
        finish();
        Intent intent = new Intent(getApplicationContext(), this.getClass());
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0,0);
    }
    public void ignoreThisApp(View v)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppSettingsActivity.this);
        prefs.edit().putBoolean(packageName+"."+IGNORE_APP, true).commit();
        addAppToAppSpecificSettings(packageName, getApplicationContext());
        finish();
    }
	
	public static void removeAppFromAppSpecificSettings(String packageName, Context ctx)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		
		String specficApps = prefs.getString(SettingsActivity.APPS_SETTINGS, "");
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
		prefs.edit().putString(SettingsActivity.APPS_SETTINGS, updatedSpecificApps).commit();
	}
	
	public static void addAppToAppSpecificSettings(String packageName, Context ctx)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	
		String specificApps = prefs.getString(SettingsActivity.APPS_SETTINGS, "");
		boolean hasApp = false;
		for (String token : specificApps.split(",")) 
		{
		     if (token.equals(packageName)) hasApp = true;
		}
        // remove it and re-add to the start of the list
        if (hasApp)
            removeAppFromAppSpecificSettings(packageName, ctx);

        // add this app to the list of specific apps
        specificApps = prefs.getString(SettingsActivity.APPS_SETTINGS, "");
        if (!specificApps.equals(""))
            specificApps = packageName + "," + specificApps;
        else
            specificApps = packageName;
		
		prefs.edit().putString("specificapps", specificApps).commit();		
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
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) 
	{
		if (key.startsWith(packageName))
		{
			String specificApps = sharedPreferences.getString(SettingsActivity.APPS_SETTINGS, "");
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
			
			sharedPreferences.edit().putString("specificapps", specificApps).commit();
		}
	}
	
}