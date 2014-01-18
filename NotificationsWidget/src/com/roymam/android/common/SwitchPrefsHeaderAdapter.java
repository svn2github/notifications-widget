package com.roymam.android.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceActivity.Header;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.roymam.android.notificationswidget.R;

import java.util.List;

public class SwitchPrefsHeaderAdapter extends ArrayAdapter<Header>
{
    public static final int HEADER_TYPE_CATEGORY = 0;
    public static final int HEADER_TYPE_NORMAL = 1;
    public static final int HEADER_TYPE_SWITCH = 2;
    public static final String HEADER_TYPE = "header_type";
    public static final String HEADER_KEY = "key";
    public static final String HEADER_DEFAULT_VALUE = "defaultValue";
    public static final String SWITCH_ENABLED_MESSAGE = "switch_enabled_message";
    public static final String SWITCH_DISABLED_MESSAGE = "switch_disabled_message";

    private LayoutInflater mInflater;

	public SwitchPrefsHeaderAdapter(Context context, List<Header> objects)
    {
		super(context, 0, objects);

		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public View getView(int position, View convertView, ViewGroup parent)
    {
		Header header = getItem(position);
		int headerType = getHeaderType(header);
		View view = null;

		switch (headerType)
        {
		case HEADER_TYPE_CATEGORY:
			view = mInflater.inflate(android.R.layout.preference_category, parent, false);
			((TextView) view.findViewById(android.R.id.title)).setText(header.getTitle(getContext()
					.getResources()));
			break;

		case HEADER_TYPE_SWITCH:
			view = mInflater.inflate(R.layout.preference_header_switch_item, parent, false);

			((ImageView) view.findViewById(android.R.id.icon)).setImageResource(header.iconRes);
			((TextView) view.findViewById(android.R.id.title)).setText(header.getTitle(getContext()
					.getResources()));
            TextView summaryTV = (TextView) view.findViewById(android.R.id.summary);
            if (summaryTV != null &&
                getContext().getResources() != null &&
                header.getSummary(getContext().getResources()) != null)
                summaryTV.setText(header.getSummary(getContext().getResources()));

            final String key = header.extras.getString(HEADER_KEY);
            final boolean defaultValue = header.extras.getBoolean(HEADER_DEFAULT_VALUE, false);
            if (key != null)
            {
                final CharSequence switchEnabledMessage = header.extras.getCharSequence(SWITCH_ENABLED_MESSAGE);
                final CharSequence switchDisabledMessage = header.extras.getCharSequence(SWITCH_DISABLED_MESSAGE);

                Switch s = (Switch) view.findViewById(R.id.switchWidget);
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                    {
                        boolean lastSwitchState = prefs.getBoolean(key, defaultValue);
                        if (isChecked != lastSwitchState)
                        {
                            prefs.edit().putBoolean(key, isChecked).commit();
                            if (isChecked && switchEnabledMessage != null)
                                Toast.makeText(getContext(), switchEnabledMessage, Toast.LENGTH_LONG).show();
                            else if (!isChecked && switchDisabledMessage != null)
                                Toast.makeText(getContext(), switchDisabledMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
                s.setChecked(prefs.getBoolean(key, defaultValue));
            }

			break;

		case HEADER_TYPE_NORMAL:
			view = mInflater.inflate(R.layout.preference_header_item, parent, false);
			((ImageView) view.findViewById(android.R.id.icon)).setImageResource(header.iconRes);
			((TextView) view.findViewById(android.R.id.title)).setText(header.getTitle(getContext()
					.getResources()));
			((TextView) view.findViewById(android.R.id.summary)).setText(header
					.getSummary(getContext().getResources()));
			break;
		}

		return view;
	}

	public static int getHeaderType(Header header)
    {
        if (header.extras != null)
        {
            int type = header.extras.getInt(HEADER_TYPE, -1);
            if (type != -1) return type;
            else if ((header.fragment == null) && (header.intent == null))
                return HEADER_TYPE_CATEGORY;
            else
                return HEADER_TYPE_NORMAL;
        }
        else return HEADER_TYPE_NORMAL;
	}
}
