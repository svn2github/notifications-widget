package com.roymam.android.notificationswidget;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.roymam.android.nilsplus.CardPreferenceFragment;

public class SpecificSettingsPreferencesActivity extends PreferenceActivity
{
    public static final String EXTRA_PACKAGE_NAME = "com.roymam.android.notificationswidget.packagename";
    protected String packageName;

    protected void onCreate(Bundle savedInstanceState, int titleRes, int layout, int preferencesXml)
    {
        super.onCreate(savedInstanceState);

        packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);

        // get package title
        try
        {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(packageName, 0);
            String appName = getPackageManager().getApplicationLabel(ai).toString();
            if (appName == null) appName = packageName;
            setTitle(appName + " - " + getString(titleRes));
        }
        catch (PackageManager.NameNotFoundException e)
        {
            setTitle(packageName + " - " + getString(titleRes));
        }


        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(layout, null);
        setContentView(v);

        addPreferencesFromResource(preferencesXml);
        PreferenceScreen prefScreen = getPreferenceScreen();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        for(int i=0; i<prefScreen.getPreferenceCount();i++)
        {
            PreferenceGroup prefGroup = (PreferenceGroup) prefScreen.getPreference(i);

            for (int j=0; j<prefGroup.getPreferenceCount(); j++) {
                Preference pref = prefGroup.getPreference(j);
                String key = packageName + "." + pref.getKey();

                if (pref instanceof ListPreference)
                {
                    ListPreference listPref = ((ListPreference) pref);
                    String globalValue = listPref.getValue();
                    String currValue = sharedPrefs.getString(key, globalValue);

                    listPref.setKey(key);
                    listPref.setValue(currValue);
                }
                else if (pref instanceof CheckBoxPreference)
                {
                    CheckBoxPreference checkPref = (CheckBoxPreference) pref;
                    boolean globalValue = checkPref.isChecked();
                    boolean currValue = sharedPrefs.getBoolean(key, globalValue);
                    checkPref.setKey(key);
                    checkPref.setChecked(currValue);
                }
            }
        }

        // apply card layout
        CardPreferenceFragment.applyLayoutToPreferences(prefScreen);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle other action bar items...
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
