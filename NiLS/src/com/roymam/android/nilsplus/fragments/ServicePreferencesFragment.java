package com.roymam.android.nilsplus.fragments;

import android.os.Bundle;

import com.roymam.android.notificationswidget.R;

public class ServicePreferencesFragment extends NiLSPreferenceFragment
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the global_settings from an XML resource
        addPreferencesFromResource(R.xml.service_preferences);
        unlockFeatures();
    }
}
