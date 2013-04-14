package com.roymam.android.notificationswidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.roymam.android.notificationswidget.WizardActivity.AboutDialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;

public class AppearanceActivity extends FragmentActivity
{
	// constants
	public static String COLLAPSED_WIDGET_MODE = "collapsed";
	public static String EXPANDED_WIDGET_MODE = "expanded";
	
	// page scroll stuff
	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;
	
	// selected widget mode
	private String 	 widgetMode = COLLAPSED_WIDGET_MODE;
	
	// fragements
	private ClockSectionFragment clockSettingsFragment = null;
	private NotificationSectionFragment notificationsSettingsFragment = null;
	private int updateViewId = 0;
	private DialogFragment colorDialog;

	
	// form fields
	private static RadioGroup clockStyleRG;
	private static CheckBox autoSwitch;
	private static CheckBox showClearAll;
	private static ViewAnimator clockStyleView;
	private static ToggleButton clockClickable;
	private static ToggleButton boldHours;
	private static ToggleButton boldMinutes;
	public static View bgColorView;
	public static View clockColorView;
	public static View dateColorView;
	public static View alarmColorView;
	public static ViewGroup bgColorButton;
	public static ViewGroup clockColorButton;
	public static ViewGroup dateColorButton;
	public static ViewGroup alarmColorButton;
	public static SeekBar bgClockOpacitySlider;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_appearance);

		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// Create the adapter that will return a fragment for each of the two
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_appearance, menu);
		
		Spinner widgetModeSpinner = (Spinner) menu.getItem(0).getActionView();
		widgetModeSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long id) 
			{
				switch (position)
				{
				case 0:
					widgetMode = COLLAPSED_WIDGET_MODE;
					break;
				case 1:
					widgetMode = EXPANDED_WIDGET_MODE;
					break;
				}
				if (clockSettingsFragment!=null)
				{					
					clockSettingsFragment.loadSettings(widgetMode);
					clockSettingsFragment.refreshPreview();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) 
			{
			}
			
		});
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onClockStyleChanged(View v) 
	{
		boolean checked = ((RadioButton)v).isChecked();
		if (checked)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			
			String clockStyle = null;
			int position = 0;
			
			switch(v.getId())
			{
			case R.id.radioSmallClock:
				clockStyle = SettingsActivity.CLOCK_SMALL;
				position = 0;
				break;
			case R.id.radioMedium:
				clockStyle = SettingsActivity.CLOCK_MEDIUM;
				position = 1;
				break;
			case R.id.radioLargeClock:
				clockStyle = SettingsActivity.CLOCK_LARGE;
				position = 2;
				break;
			}
			clockStyleView.setDisplayedChild(position);
			autoSwitch.setChecked(false);
			prefs.edit().putString(widgetMode + "." + SettingsActivity.CLOCK_STYLE, clockStyle).commit();
			clockSettingsFragment.refreshPreview();
		}
	}
	
	public void onAutoSwitchChanged(View v)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String clockStyle = SettingsActivity.CLOCK_AUTO;
		boolean checked = ((CheckBox)v).isChecked();
		
		if (!checked)
		{
			int id = clockStyleRG.getCheckedRadioButtonId();
			switch(id)
			{
			case R.id.radioSmallClock:
				clockStyle = SettingsActivity.CLOCK_SMALL;
				break;
			case R.id.radioMedium:
				clockStyle = SettingsActivity.CLOCK_MEDIUM;
				break;
			case R.id.radioLargeClock:
				clockStyle = SettingsActivity.CLOCK_LARGE;
				break;
			}
		}
		
		prefs.edit().putString(widgetMode + "." + SettingsActivity.CLOCK_STYLE, clockStyle).commit();
		clockSettingsFragment.refreshPreview();
	}	
	
	public void onClockPrefChanged(View v)
	{
		boolean checked = ((CompoundButton)v).isChecked();
		String settings = (String) v.getTag();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit().putBoolean(widgetMode + "." + settings, checked).commit();
		clockSettingsFragment.refreshPreview();
	}
	
	public void onColorChoose(View v)
	{
		if (updateViewId != 0)
		{
			// set background color using the color button background
			View v2 = this.findViewById(updateViewId);
			String colorStr = (String) v.getTag();
			int colorId;
			if (!colorStr.equals("#0"))
				colorId = Color.parseColor(colorStr);
			else
				colorId = Color.TRANSPARENT;
			
			v2.setBackgroundColor(colorId);
			
			// store value using the view preference
			String settings = (String) v2.getTag();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			prefs.edit().putInt(widgetMode + "." + settings, colorId).commit();
			clockSettingsFragment.refreshPreview();
			
			colorDialog.dismiss();
		}
	}
	public void onClockBGColorClick(View v)
	{
		showColorDialog(R.id.clockBGColorView);
	}
	
	public void onClockColorClick(View v)
	{
		showColorDialog(R.id.clockColorView);
	}
	
	public void onDateColorClick(View v)
	{
		showColorDialog(R.id.dateColorView);
	}
	
	public void onAlarmColorClick(View v)
	{
		showColorDialog(R.id.alarmColorView);
	}
	
	private void showColorDialog(int viewId) 
	{
		updateViewId = viewId;
		
		colorDialog = new DialogFragment()
		{
			@Override
		    public Dialog onCreateDialog(Bundle savedInstanceState) 
		    {
		    	LayoutInflater inflater = getActivity().getLayoutInflater();
		    	View colorPickupView = inflater.inflate(R.layout.dialog_color_pickup, null);
		    	
		        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		        builder.setTitle("Choose Color")
		        		.setView(colorPickupView)
		        		.setNegativeButton(R.string.about_close, new DialogInterface.OnClickListener() 
		               {
		                   public void onClick(DialogInterface dialog, int id) 
		                   {
		                       // do nothing
		                   }
		               });	        
		        // Create the AlertDialog object and return it
		        return builder.create();
		    }
		};
		colorDialog.show(getFragmentManager(), "ChooseColorDialog");	
	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter 
	{
		public SectionsPagerAdapter(FragmentManager fm) 
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int position) 
		{
			switch (position)
			{
			case 0:
				clockSettingsFragment = new ClockSectionFragment();
				return clockSettingsFragment;
			case 1:
				notificationsSettingsFragment = new NotificationSectionFragment();
				return notificationsSettingsFragment;
			}
			return null;
		}

		@Override
		public int getCount() 
		{
			// Show 2 total pages.
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) 
		{
			switch (position) 
			{
			case 0:
				return getString(R.string.title_section1).toUpperCase(Locale.US);
			case 1:
				return getString(R.string.title_section2).toUpperCase(Locale.US);
			}
			return null;
		}
	}

	public static class ClockSectionFragment extends Fragment implements OnSeekBarChangeListener
	{
		private String widgetMode = AppearanceActivity.COLLAPSED_WIDGET_MODE;
		
		public ClockSectionFragment() 
		{
		}

		public void refreshPreview() 
		{			
			//SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			List<View> clockPreviews = Arrays.asList(getView().findViewById(R.id.smallClock),
													 getView().findViewById(R.id.mediumClock),
													 getView().findViewById(R.id.largeClock));
			
			for(View v : clockPreviews)
			{
				TextView hours = (TextView) v.findViewById(R.id.hours);
				TextView minutes = (TextView) v.findViewById(R.id.minutes);
				TextView ampm = (TextView) v.findViewById(R.id.ampm);
				TextView date = (TextView) v.findViewById(R.id.date);
				TextView alarm = (TextView) v.findViewById(R.id.alarmtime);
				
				// get current time
				Time t = new Time();
			    t.setToNow();
			    String hourFormat = "%H";
			    String minuteFormat = ":%M";
			    String ampmstr = "";
		    	if (!DateFormat.is24HourFormat(getActivity()))
		    	{
		    		hourFormat = "%l";
		    		minuteFormat = ":%M";
		    		ampmstr = t.format("%p");
		    	}
		    	
		    	hours.setText(t.format(hourFormat));
			    minutes.setText(t.format(minuteFormat));
			    ampm.setText(ampmstr);		    
			    String datestr = DateFormat.format("EEE, MMMM dd", t.toMillis(true)).toString();
			    date.setText(datestr.toUpperCase(Locale.getDefault()));
			    
			    // set clock text color
			    int bgColor = ((ColorDrawable) bgColorView.getBackground()).getColor();			    
			    v.setBackgroundColor(bgColor);
			    v.getBackground().setAlpha(bgClockOpacitySlider.getProgress()*255/100);
			    
			    hours.setTextColor(((ColorDrawable)clockColorView.getBackground()).getColor());
			    minutes.setTextColor(((ColorDrawable)clockColorView.getBackground()).getColor());
			    ampm.setTextColor(((ColorDrawable)clockColorView.getBackground()).getColor());
			    date.setTextColor(((ColorDrawable)dateColorView.getBackground()).getColor());
			    alarm.setTextColor(((ColorDrawable)alarmColorView.getBackground()).getColor());
			    
			    if (boldHours.isChecked())
			    {
			    	hours.setTypeface(null, Typeface.BOLD);
			    }
			    else
			    {
			    	hours.setTypeface(null, Typeface.NORMAL);
			    }
			    
			    if (boldMinutes.isChecked())
			    {
			    	minutes.setTypeface(null, Typeface.BOLD);
			    }
			    else
			    {
			    	minutes.setTypeface(null, Typeface.NORMAL);
			    }
			    // display next alarm if needed
			    String nextAlarm = Settings.System.getString(getActivity().getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
			    if (!nextAlarm.equals(""))
			    {
			    	alarm.setVisibility(View.VISIBLE);
			    	alarm.setText("⏰" + nextAlarm.toUpperCase(Locale.getDefault()));
			    }
			    else
			    {
			    	alarm.setVisibility(View.GONE);
			    }
			}
				
		}

		public void loadSettings(String widgetMode) 
		{
			Context ctx = getActivity();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
			
			this.widgetMode = widgetMode;
			// setup clock
			setupClockStyle();
			
			// setup checkboxes
			showClearAll.setChecked(prefs.getBoolean(widgetMode + "." + SettingsActivity.SHOW_CLEAR_BUTTON, true));			
			
			// setup toggles
			clockClickable.setChecked(prefs.getBoolean(widgetMode + "." + SettingsActivity.CLOCK_IS_CLICKABLE, true));
			boldHours.setChecked(prefs.getBoolean(widgetMode + "." + SettingsActivity.BOLD_HOURS, true));
			boldMinutes.setChecked(prefs.getBoolean(widgetMode + "." + SettingsActivity.BOLD_MINUTES, false));
			
			// setup opacity slider
			int opacity = prefs.getInt(widgetMode + "." + SettingsActivity.CLOCK_BG_OPACITY, 0);
			bgClockOpacitySlider.setProgress(opacity);
			
			// setup color buttons
			int bgColor = prefs.getInt(widgetMode + "." + SettingsActivity.CLOCK_BG_COLOR, Color.BLACK);
			int clockColor = prefs.getInt(widgetMode + "." + SettingsActivity.CLOCK_COLOR, Color.WHITE);
			int dateColor = prefs.getInt(widgetMode + "." + SettingsActivity.CLOCK_DATE_COLOR, Color.WHITE);
			int alarmColor = prefs.getInt(widgetMode + "." + SettingsActivity.CLOCK_ALARM_COLOR, Color.DKGRAY);
			bgColorView.setBackgroundColor(bgColor);
			clockColorView.setBackgroundColor(clockColor);
			dateColorView.setBackgroundColor(dateColor);
			alarmColorView.setBackgroundColor(alarmColor);
			
		}

		private void setupClockStyle() 
		{
			Context ctx = getActivity();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
			
			// clock style
			String defaultClockStyle = widgetMode.equals(AppearanceActivity.COLLAPSED_WIDGET_MODE)?
					SettingsActivity.CLOCK_AUTO :
					prefs.getString(SettingsActivity.CLOCK_STYLE, SettingsActivity.CLOCK_STYLE);
			
			String clockStyle = prefs.getString(widgetMode + "." + SettingsActivity.CLOCK_STYLE, defaultClockStyle);
			
			autoSwitch.setChecked(false);
			
			if (clockStyle.equals(SettingsActivity.CLOCK_SMALL))
			{
				clockStyleRG.check(R.id.radioSmallClock);
				clockStyleView.setDisplayedChild(0);
			}
			else if (clockStyle.equals(SettingsActivity.CLOCK_MEDIUM))
			{
				clockStyleRG.check(R.id.radioMedium);
				clockStyleView.setDisplayedChild(1);
			}
			else if (clockStyle.equals(SettingsActivity.CLOCK_LARGE))
			{
				clockStyleRG.check(R.id.radioLargeClock);
				clockStyleView.setDisplayedChild(2);
			}
			else if (clockStyle.equals(SettingsActivity.CLOCK_AUTO))
			{
				clockStyleRG.check(R.id.radioLargeClock);
				autoSwitch.setChecked(true);
				clockStyleView.setDisplayedChild(2);
			}
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) 
		{
			// building clock settings view 
			View clockSettingsView = inflater.inflate(R.layout.tab_clock_settings, null);
			View smallClockView = inflater.inflate(R.layout.small_clock, null);
			View mediumClockView = inflater.inflate(R.layout.medium_clock, null);
			View largeClockView = inflater.inflate(R.layout.large_clock, null);
			ViewAnimator v = (ViewAnimator) clockSettingsView.findViewById(R.id.clockViewAnimator);
			v.addView(smallClockView);
			v.addView(mediumClockView);
			v.addView(largeClockView);
			smallClockView.getLayoutParams().height = LayoutParams.WRAP_CONTENT; 
			mediumClockView.getLayoutParams().height = LayoutParams.WRAP_CONTENT; 
			largeClockView.getLayoutParams().height = LayoutParams.WRAP_CONTENT; 
			
			// find views
			clockStyleView = v;
			clockStyleRG = (RadioGroup) clockSettingsView.findViewById(R.id.clockStyleRG);
			autoSwitch = (CheckBox)clockSettingsView.findViewById(R.id.autoSizeCheckbox);
			showClearAll = (CheckBox)clockSettingsView.findViewById(R.id.showClearButtonCheckbox);
			clockClickable = (ToggleButton)clockSettingsView.findViewById(R.id.clockIsClickableToggle);
			boldHours = (ToggleButton)clockSettingsView.findViewById(R.id.boldHoursToggle);
			boldMinutes = (ToggleButton)clockSettingsView.findViewById(R.id.boldMinutesToggle);
			bgClockOpacitySlider = (SeekBar)clockSettingsView.findViewById(R.id.clockBGTransparencySeekBar);
			bgClockOpacitySlider.setOnSeekBarChangeListener(this);
			bgColorButton = (ViewGroup)clockSettingsView.findViewById(R.id.clockBGColorButton);
			clockColorButton = (ViewGroup)clockSettingsView.findViewById(R.id.clockColorButton);
			dateColorButton = (ViewGroup)clockSettingsView.findViewById(R.id.dateColorButton);
			alarmColorButton = (ViewGroup)clockSettingsView.findViewById(R.id.alarmColorButton);
			bgColorView = clockSettingsView.findViewById(R.id.clockBGColorView);
			clockColorView = clockSettingsView.findViewById(R.id.clockColorView);
			dateColorView = clockSettingsView.findViewById(R.id.dateColorView);
			alarmColorView = clockSettingsView.findViewById(R.id.alarmColorView);
			
			
			
			return clockSettingsView;
		}

		// progress bar events
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
		{
			if (fromUser)
			{
				String settings = (String) seekBar.getTag();
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
				prefs.edit().putInt(widgetMode + "." + settings, progress).commit();
				refreshPreview();
			}
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) 
		{
			// do nothing
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) 
		{
			// do nothing
		}
	}
	
	public static class NotificationSectionFragment extends Fragment 
	{	
		public NotificationSectionFragment()
		{
		}
		
		public void refreshPreview() 
		{
			
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) 
		{
			// load notification settings view from layouts
			View notificationSettingsView = inflater.inflate(R.layout.tab_notification_settings, null);
			View ncView = inflater.inflate(R.layout.notification_compact, null);
			View nnView = inflater.inflate(R.layout.notification_normal, null);
			View nlView = inflater.inflate(R.layout.notification_large, null);
			ViewAnimator v = (ViewAnimator) notificationSettingsView.findViewById(R.id.notificationViewFlipper);
			v.addView(ncView);
			v.addView(nnView);
			v.addView(nlView);
			ncView.getLayoutParams().height = LayoutParams.WRAP_CONTENT; 
			nnView.getLayoutParams().height = LayoutParams.WRAP_CONTENT; 
			nlView.getLayoutParams().height = LayoutParams.WRAP_CONTENT; 
			
			return notificationSettingsView;
		}
	}

	

	

}
