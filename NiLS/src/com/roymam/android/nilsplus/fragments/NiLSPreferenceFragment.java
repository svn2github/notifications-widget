package com.roymam.android.nilsplus.fragments;

import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.roymam.android.nilsplus.CardPreferenceFragment;
import com.roymam.android.notificationswidget.SettingsManager;

public class NiLSPreferenceFragment extends CardPreferenceFragment
{
    public void unlockFeatures()
    {
        PreferenceScreen prefsScreen = getPreferenceScreen();

        // unlock premium features
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean unlocked =  prefs.getBoolean(SettingsManager.UNLOCKED, SettingsManager.DEFAULT_UNLOCKED);
        recursiveUnlockPremiumFeatures(prefsScreen, unlocked);
    }

    private void recursiveUnlockPremiumFeatures(PreferenceGroup prefs, boolean unlocked)
    {
        for (int i=0; i<prefs.getPreferenceCount(); i++)
        {
            Preference pref = prefs.getPreference(i);

            if (!pref.isEnabled())
            {
                if (unlocked)
                {
                    pref.setEnabled(true);
                }
                else
                {
                    pref.setIcon(android.R.drawable.ic_lock_lock);
                }
            }
            if (pref instanceof PreferenceGroup)
                recursiveUnlockPremiumFeatures((PreferenceGroup) pref, unlocked);
        }
    }
}
