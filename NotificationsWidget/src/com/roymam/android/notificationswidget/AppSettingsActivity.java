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
                        listPref.getEntries(), listPref.getEntryValues());

                listener.setPrefSummary(listPref, currValue);
                listPref.setOnPreferenceChangeListener(listener);
            }
        }


        /*
		// add app specific settings
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
		
		// Blacklist preference
        CheckBoxPreference ignoreNotificationsPref = new CheckBoxPreference(this);
        ignoreNotificationsPref.setKey(packageName+"."+IGNORE_APP);
        ignoreNotificationsPref.setTitle(R.string.ignore_notifications);
        ignoreNotificationsPref.setSummary(R.string.ignore_notifications_summary);
        //ignoreNotificationsPref.setDisableDependentsState(true);
        root.addPreference(ignoreNotificationsPref);

        CheckBoxPreference ignoreEmptyNotificationsPref = new CheckBoxPreference(this);
        ignoreEmptyNotificationsPref.setKey(packageName+"."+IGNORE_EMPTY_NOTIFICATIONS);
        ignoreEmptyNotificationsPref.setTitle(R.string.ignore_empty_notifications);
        ignoreEmptyNotificationsPref.setSummary(R.string.ignore_empty_notifications_summary);
        ignoreEmptyNotificationsPref.setDefaultValue(false);
        root.addPreference(ignoreEmptyNotificationsPref);
        
        // Show last preference
        CheckBoxPreference showlastNotificationsPref = new CheckBoxPreference(this);
        showlastNotificationsPref.setKey(packageName+"."+KEEP_ONLY_LAST);
        showlastNotificationsPref.setTitle(R.string.show_only_last_notification);
        showlastNotificationsPref.setSummary(R.string.show_only_last_notification_summary);
        //showlastNotificationsPref.setDependency(packageName+"."+IGNORE_APP);
        root.addPreference(showlastNotificationsPref);
        
        // Extract expanded text preference
        CheckBoxPreference useExpandedTextPref = new CheckBoxPreference(this);
        useExpandedTextPref.setKey(packageName+"."+USE_EXPANDED_TEXT);
        useExpandedTextPref.setTitle(R.string.extract_expanded_text);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppSettingsActivity.this);
		useExpandedTextPref.setDefaultValue(prefs.getBoolean(USE_EXPANDED_TEXT, true));
        useExpandedTextPref.setSummary(R.string.extract_expanded_text_summary);
        root.addPreference(useExpandedTextPref);

        // Always use app icon preference
        CheckBoxPreference useAppIconPref = new CheckBoxPreference(this);
        useAppIconPref.setKey(packageName+"."+ALWAYS_USE_APP_ICON);
        useAppIconPref.setTitle(R.string.always_use_app_icon);
        useAppIconPref.setDefaultValue(false);
        useAppIconPref.setSummary(R.string.always_use_app_icon_summary);
        root.addPreference(useAppIconPref);

        // multiple events handling prefrence
        ListPreference multipleEvents = new ListPreference(this);
        multipleEvents.setKey(packageName + "." + MULTIPLE_EVENTS_HANDLING);
        multipleEvents.setTitle(R.string.multiple_events_handling);
        multipleEvents.setSummary(R.string.multiple_events_handling_summary);
        multipleEvents.setDefaultValue("all");
        multipleEvents.setEntries(R.array.settings_multiple_events_entries);
        multipleEvents.setEntryValues(R.array.settings_multiple_events_values);
        root.addPreference(multipleEvents);

        CheckBoxPreference tryExtractTitlePref = new CheckBoxPreference(this);
        tryExtractTitlePref.setKey(packageName+"."+TRY_EXTRACT_TITLE);
        tryExtractTitlePref.setTitle(R.string.try_extract_title);
        tryExtractTitlePref.setSummary(R.string.try_extract_title_summary);
        tryExtractTitlePref.setDefaultValue(true);
        root.addPreference(tryExtractTitlePref );

        // Show last preference
        ListPreference overrideAppPriority = new ListPreference(this);
        overrideAppPriority.setKey(packageName + "." + APP_PRIORITY);
        overrideAppPriority.setTitle(R.string.set_app_priority);
        overrideAppPriority.setSummary(R.string.set_app_priority_summary);
        overrideAppPriority.setDefaultValue("-9");
        overrideAppPriority.setEntries(R.array.settings_app_priority_entries);
        overrideAppPriority.setEntryValues(R.array.settings_app_priority_values);
        root.addPreference(overrideAppPriority);

        // Clear app specific preferences button
        PreferenceScreen clearPref = getPreferenceManager().createPreferenceScreen(this);
        clearPref.setTitle(R.string.clear_app_settings);
        clearPref.setSummary(R.string.clear_app_summary);
        clearPref.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
			@Override
			public boolean onPreferenceClick(Preference arg0) 
			{	
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppSettingsActivity.this);
				prefs.edit().remove(packageName+"."+IGNORE_APP)
				            .remove(packageName+"."+KEEP_ONLY_LAST)
                            .remove(packageName+"."+MULTIPLE_EVENTS_HANDLING)
                            .remove(packageName+"."+USE_EXPANDED_TEXT)
                            .remove(packageName+"."+ALWAYS_USE_APP_ICON)
                            .remove(packageName+"."+APP_PRIORITY)
                            .remove(packageName+"."+IGNORE_EMPTY_NOTIFICATIONS)
                            .remove(packageName+"."+MULTIPLE_EVENTS_HANDLING)
                        .commit();
				
				removeAppFromAppSpecificSettings(packageName, AppSettingsActivity.this);
				
				finish();
				return false;
			}
        	
        });
		//Intent runAppSpecificSettings = new Intent(this, AppSettingsActivity.class);
		//runAppSpecificSettings.putExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME, packageName);
		//intentPref.setIntent(runAppSpecificSettings);
		root.addItemFromInflater(clearPref);
        setPreferenceScreen(root);*/
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