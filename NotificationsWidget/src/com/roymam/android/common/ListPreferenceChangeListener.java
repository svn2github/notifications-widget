package com.roymam.android.common;

import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import java.util.Set;

public class ListPreferenceChangeListener implements OnPreferenceChangeListener 
{			
	private String[] entries;
	private String[] values;
	
	public ListPreferenceChangeListener(String[] entries, String[] values)
	{
		this.entries = entries;
		this.values = values;
	}
	
	public void setPrefSummary(Preference prefs, String newValue)
	{
		for(int i=0;i<values.length;i++)
		{
			if (newValue.contains(values[i]))
			{
				prefs.setSummary(entries[i]);
			}
		}
	}
	@Override
	public boolean onPreferenceChange(Preference prefs, Object newValue) 
	{
        if (newValue instanceof String)
		    setPrefSummary(prefs, (String)newValue);
        else if (newValue instanceof Set)
            setPrefSummary(prefs, (Set<String>)newValue);
		return true;
	}

    public void setPrefSummary(Preference pref, Set<String> currValues)
    {
        String summary = "";

        for(String currValue : currValues)
        {
            for(int i=0;i<values.length;i++)
            {
                if (currValue.contains(values[i]))
                {
                    summary += entries[i] + "\n";
                }
            }
        }

        if (summary.length() > 1) summary = summary.substring(0, summary.length()-2);
        pref.setSummary(summary);
    }
}
