package com.roymam.android.nilsplus.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.roymam.android.common.SysUtils;
import com.roymam.android.nilsplus.CardPreferenceFragment;
import com.roymam.android.notificationswidget.NiLSAccessibilityService;
import com.roymam.android.notificationswidget.R;
import com.roymam.android.notificationswidget.SettingsManager;

public class MainPrefsFragment extends CardPreferenceFragment
{
    private Context mContext;

    private CheckBoxPreference mPrefNiLSService;
    private CheckBoxPreference mPrefAutoHideService;

    @Override
    public void onResume()
    {
        super.onResume();

        // NiLS Service Status
        boolean serviceRunning = SysUtils.isServiceRunning(mContext);

        if (serviceRunning)
            mPrefNiLSService.setChecked(true);
        else
            mPrefNiLSService.setChecked(false);

        // NiLS Auto Hide Service Status
        if (NiLSAccessibilityService.isServiceRunning(mContext))
            mPrefAutoHideService.setChecked(true);
        else
            mPrefAutoHideService.setChecked(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        //mTopviewContainer.addView(inflater.inflate(R.layout.view_nils_logo, null));
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the global_settings from an XML resource
        addPreferencesFromResource(R.xml.main_preferences);

        mContext = getActivity();
        mPrefNiLSService = (CheckBoxPreference) findPreference(SettingsManager.NILS_SERVICE);
        mPrefNiLSService.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                Intent intent = SettingsManager.getNotificationsServiesIntent();
                startActivity(intent);
                Toast.makeText(getActivity(), R.string.enable_service_tip, Toast.LENGTH_LONG).show();
                return false;
            }
        });

        mPrefAutoHideService = (CheckBoxPreference) findPreference(SettingsManager.AUTO_HIDE_SERVICE);
        mPrefAutoHideService.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(getActivity(), R.string.enable_auto_hide_service_tip, Toast.LENGTH_LONG).show();
                return false;
            }
        });

    }
}
