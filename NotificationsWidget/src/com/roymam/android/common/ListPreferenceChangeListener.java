package com.roymam.android.common;

import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

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
			if (newValue.equals(values[i]))
			{
				prefs.setSummary(entries[i]);
			}
		}
	}
	@Override
	public boolean onPreferenceChange(Preference prefs, Object newValue) 
	{
		setPrefSummary(prefs, (String)newValue);
		return true;
	}
}
