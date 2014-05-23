package com.roymam.android.nilsplus.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.roymam.android.nilsplus.CardPreferenceFragment;
import com.roymam.android.notificationswidget.R;

public class AboutPreferencesFragment extends CardPreferenceFragment
{
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mContext = getActivity();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Load the global_settings from an XML resource
        addPreferencesFromResource(R.xml.about_preferences);

        // setting "version" button summary
        Preference versionPref = findPreference("version");
        String versionString = "";
        try
        {
            versionString = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
            versionPref.setSummary(getText(R.string.version) + " " + versionString);
        } catch (PackageManager.NameNotFoundException e)
        {
        }
    }
}
