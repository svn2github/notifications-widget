package com.roymam.android.nilsplus;

import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.roymam.android.common.ListPreferenceChangeListener;
import com.roymam.android.notificationswidget.R;

import net.margaritov.preference.colorpicker.AlphaPatternDrawable;

public class CardPreferenceFragment extends PreferenceFragment
{
    protected ViewGroup mTopviewContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.appearance_settings_view, null);
        mTopviewContainer = (ViewGroup) v.findViewById(R.id.preview_container);
        return v;
    }

    @Override
    public void addPreferencesFromResource(int preferencesResId)
    {
        super.addPreferencesFromResource(preferencesResId);

        PreferenceScreen prefScreen = getPreferenceScreen();
        applyLayoutToPreferences(prefScreen);
    }

    public static void applyLayoutToPreferences(PreferenceScreen prefScreen)
    {
        for(int i=0; i<prefScreen.getPreferenceCount();i++)
        {
            Preference pref = prefScreen.getPreference(i);
            if (pref instanceof PreferenceGroup)
            {
                pref.setLayoutResource(R.layout.card_layout_top);
                int count = ((PreferenceGroup) pref).getPreferenceCount();
                for(int j=0; j<count; j++)
                {
                    Preference innerPref = ((PreferenceGroup) pref).getPreference(j);
                    if (innerPref instanceof CheckBoxPreference)
                        innerPref.setLayoutResource(R.layout.card_checkbox_layout_middle);
                    else
                        innerPref.setLayoutResource(R.layout.card_layout_middle);

                    if (innerPref instanceof ListPreference)
                        applyListPrefAutoDescription((ListPreference) innerPref);
                }

                // add card bottom graphics
                Preference cardBottom = new Preference(prefScreen.getContext());
                cardBottom.setLayoutResource(R.layout.card_layout_bottom);
                ((PreferenceGroup) pref).addPreference(cardBottom);
            }
        }
    }

    public static void applyListPrefAutoDescription(ListPreference pref)
    {
        ListPreferenceChangeListener listener = new ListPreferenceChangeListener(
                pref.getSummary(),
                pref.getEntries(),
                pref.getEntryValues());

        listener.setPrefSummary(pref, pref.getValue());
        pref.setOnPreferenceChangeListener(listener);
    }
}
