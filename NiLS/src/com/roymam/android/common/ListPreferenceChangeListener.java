package com.roymam.android.common;

import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import java.util.Set;

public class ListPreferenceChangeListener implements OnPreferenceChangeListener 
{
    private final CharSequence prefix;
    private CharSequence[] entries;
	private CharSequence[] values;
	
	public ListPreferenceChangeListener(CharSequence prefix, CharSequence[] entries, CharSequence[] values)
	{
        this.prefix = prefix;
		this.entries = entries;
		this.values = values;
	}
	
	public void setPrefSummary(Preference prefs, String newValue)
	{
		for(int i=0;i<values.length;i++)
		{
			if (newValue != null && newValue.contains(values[i]))
			{
                if (prefix != null && !prefix.equals(""))
                    prefs.setSummary(prefix + ":" + entries[i]);
                else
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

        if (summary.length() > 1) summary = summary.substring(0, summary.length()-1);
        pref.setSummary(summary);
    }
}
