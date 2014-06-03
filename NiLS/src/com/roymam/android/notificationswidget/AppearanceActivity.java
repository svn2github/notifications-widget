package com.roymam.android.notificationswidget;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AppearanceActivity extends Activity implements OnNavigationListener
{
    // page scroll stuff
	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;

	// selected widget mode
	private static String 	 widgetMode = SettingsManager.COLLAPSED_WIDGET_MODE;

    // fragements
	private ClockSectionFragment clockSettingsFragment = null;
    private NotificationSectionFragment notificationsSettingsFragment = null;
    private int updateViewId = 0;
	private DialogFragment colorDialog;

    // clock form fields
	private static RadioGroup clockStyleRG;
    private static CheckBox autoSwitch;
    private static CheckBox showClearAll;
    private static CheckBox hideClock;
    private static CheckBox showPersistent;
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

    // notification fields
	public static ViewAnimator notificationStyleView;
    public static RadioGroup notificationStyleRG;
    public static RadioButton notificationStyleCompact;
    public static RadioButton notificationStyleNormal;
    public static RadioButton notificationStyleLarge;
    public static ToggleButton notificationClickable;
    public static ToggleButton useExpandedText;
    public static ToggleButton iconClickable;
    public static View notificationBgColorView;
    public static View titleColorView;
    public static View textColorView;
    public static View contentColorView;
    public static ViewGroup notificationBgColorButton;
    public static ViewGroup titleColorButton;
    public static ViewGroup textColorButton;
    public static ViewGroup contentColorButton;
    public static SeekBar notificationBgClockOpacitySlider;
    public static Spinner maxLinesSpinner;
    private SettingsManager.PrefsPersistentNotificationsFragment persistentNotificationsFragment;

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    	super.onSaveInstanceState(outState);
    	if (clockSettingsFragment != null) getFragmentManager().putFragment(outState, ClockSectionFragment.class.getName(), clockSettingsFragment);
        if (notificationsSettingsFragment != null) getFragmentManager().putFragment(outState, NotificationSectionFragment.class.getName(), notificationsSettingsFragment);
        if (persistentNotificationsFragment != null) getFragmentManager().putFragment(outState, SettingsManager.PrefsPersistentNotificationsFragment.class.getName(), persistentNotificationsFragment);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_appearance);

		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		CharSequence[] modes = getResources().getStringArray(R.array.widget_mode_entries);

		ArrayAdapter<CharSequence> list = new ArrayAdapter<CharSequence> (this, R.layout.spinner_widget_mode, android.R.id.text1, modes);
		list.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

	    getActionBar().setListNavigationCallbacks(list, this);
	    getActionBar().setDisplayShowTitleEnabled(false);

	    // select the last widget mode that was changed
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    String widgetMode = prefs.getString(SettingsManager.LAST_WIDGET_MODE, SettingsManager.EXPANDED_WIDGET_MODE);
	    int itemPosition;
	    if (widgetMode.equals(SettingsManager.COLLAPSED_WIDGET_MODE))
	    	itemPosition = 0;
	    else if (widgetMode.equals(SettingsManager.EXPANDED_WIDGET_MODE))
	    	itemPosition = 1 ;
	    else
	    	itemPosition = 2;

	    // Create the adapter that will return a fragment for each of the two
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(1, false);

		if (savedInstanceState != null)
	    {
	        clockSettingsFragment = (ClockSectionFragment) getFragmentManager().getFragment(savedInstanceState, ClockSectionFragment.class.getName());
	        notificationsSettingsFragment = (NotificationSectionFragment) getFragmentManager().getFragment(savedInstanceState, NotificationSectionFragment.class.getName());
            persistentNotificationsFragment = (SettingsManager.PrefsPersistentNotificationsFragment) getFragmentManager().getFragment(savedInstanceState, SettingsManager.PrefsPersistentNotificationsFragment.class.getName());
	    }

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int i, float v, int i2)
            {

            }

            @Override
            public void onPageSelected(int i)
            {
                if (i == 2)
                {
                    getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                    getActionBar().setDisplayShowTitleEnabled(true);
                }
                else
                {
                    getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                    getActionBar().setDisplayShowTitleEnabled(false);
                }
            }

            @Override
            public void onPageScrollStateChanged(int i)
            {

            }
        });

        getActionBar().setSelectedNavigationItem(itemPosition);
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
				clockStyle = SettingsManager.CLOCK_SMALL;
				position = 0;
				break;
			case R.id.radioMedium:
				clockStyle = SettingsManager.CLOCK_MEDIUM;
				position = 1;
				break;
			case R.id.radioLargeClock:
				clockStyle = SettingsManager.CLOCK_LARGE;
				position = 2;
				break;
			}
			clockStyleView.setDisplayedChild(position);
			autoSwitch.setChecked(false);
			prefs.edit().putString(widgetMode + "." + SettingsManager.CLOCK_STYLE, clockStyle).commit();
			clockSettingsFragment.refreshPreview();
			// refresh widget
			sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));
		}
	}
	
	public void onNotificationStyleChanged(View v) 
	{
		boolean checked = ((RadioButton)v).isChecked();
		if (checked)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			
			String notificationStyle = null;
			int position = 0;
			
			switch(v.getId())
			{
			case R.id.notificationStyleCompactRadio:
				notificationStyle = "compact";
				position = 0;
				break;
			case R.id.notificationStyleNormalRadio:
				notificationStyle = "normal";
				position = 1;
				break;
			case R.id.notificationStyleOriginal:
				notificationStyle = "large";
				position = 2;
				break;
			}
			notificationStyleView.setDisplayedChild(position);
			prefs.edit().putString(widgetMode + "." + SettingsManager.NOTIFICATION_STYLE, notificationStyle).commit();
			notificationsSettingsFragment.refreshPreview();
			// refresh widget
			sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));

		}
	}
	
	public void onAutoSwitchChanged(View v)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String clockStyle = SettingsManager.CLOCK_AUTO;
		boolean checked = ((CheckBox)v).isChecked();
		
		if (!checked)
		{
			int id = clockStyleRG.getCheckedRadioButtonId();
			switch(id)
			{
			case R.id.radioSmallClock:
				clockStyle = SettingsManager.CLOCK_SMALL;
				break;
			case R.id.radioMedium:
				clockStyle = SettingsManager.CLOCK_MEDIUM;
				break;
			case R.id.radioLargeClock:
				clockStyle = SettingsManager.CLOCK_LARGE;
				break;
			}
		}
		
		prefs.edit().putString(widgetMode + "." + SettingsManager.CLOCK_STYLE, clockStyle).commit();
		clockSettingsFragment.refreshPreview();
		// refresh widget
		sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));

	}	
	
	public void onClockPrefChanged(View v)
	{
		boolean checked = ((CompoundButton)v).isChecked();
		String settings = (String) v.getTag();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit().putBoolean(widgetMode + "." + settings, checked).commit();
		clockSettingsFragment.refreshPreview();
		// refresh widget
		sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));

	}
	
	public void onNotificationPrefChanged(View v)
	{
		boolean checked = ((CompoundButton)v).isChecked();
		String settings = (String) v.getTag();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit().putBoolean(widgetMode + "." + settings, checked).commit();
		notificationsSettingsFragment.refreshPreview();
		// refresh widget
		sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));
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
			notificationsSettingsFragment.refreshPreview();
			// refresh widget
			sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));
			
			colorDialog.dismiss();
		}
	}

	public void onNotificationkBGColorClick(View v)
	{
		showColorDialog(R.id.bgColorView);
	}
	
	public void onTitleColorClick(View v)
	{
		showColorDialog(R.id.titleColorView);
	}
	
	public void onTextColorClick(View v)
	{
		showColorDialog(R.id.textColorView);
	}
	
	public void onContentColorClick(View v)
	{
		showColorDialog(R.id.contentColorView);
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

    // openPersistentSettings is launched from the custom checkbox in persistent notifications settings
    public void openPersistentSettings(View v)
    {
        // this is a dirty hack to get the package name within the settings button
        String packageName = ((TextView)((View)v.getParent()).findViewById(android.R.id.summary)).getText().toString();

        // open persistent notification settings
        Intent runAppSpecificSettings = new Intent(this, PersistentNotificationSettingsActivity.class);
        runAppSpecificSettings.putExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME, packageName);
        startActivity(runAppSpecificSettings);

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
            case 2:
                persistentNotificationsFragment = new SettingsManager.PrefsPersistentNotificationsFragment();
                return persistentNotificationsFragment;
			}
			return null;
		}

		@Override
		public int getCount() 
		{
			// Show 3 total pages.
			return 3;
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
            case 2:
                return getString(R.string.persistent_notifications).toUpperCase(Locale.US);
            }
			return null;
		}
	}

	public static class ClockSectionFragment extends Fragment implements OnSeekBarChangeListener
	{
		public ClockSectionFragment() 
		{
		}

        private Bitmap getClockIcon(int color)
        {
            Bitmap sourceBitmap = BitmapFactory.decodeResource(Resources.getSystem(), android.R.drawable.ic_lock_idle_alarm);

            float r = (float) Color.red(color),
                    g = (float) Color.green(color),
                    b = (float) Color.blue(color);

            float[] colorTransform =
                    {
                            r/255, 0    , 0    , 0, 0,  // R color
                            0    , g/255, 0    , 0, 0,  // G color
                            0    , 0    , b/255, 0, 0,  // B color
                            0    , 0    , 0    , 1, 0
                    };

            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0f); // Remove colour
            colorMatrix.set(colorTransform);

            ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
            Paint paint = new Paint();
            paint.setColorFilter(colorFilter);

            Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap);
            Bitmap mutableBitmap = resultBitmap.copy(Bitmap.Config.ARGB_8888, true);

            Canvas canvas = new Canvas(mutableBitmap);
            canvas.drawBitmap(mutableBitmap, 0, 0, paint);

            return mutableBitmap;
        }

		public void refreshPreview() 
		{
			List<View> clockPreviews = Arrays.asList(getView().findViewById(R.id.smallClock),
													 getView().findViewById(R.id.mediumClock),
													 getView().findViewById(R.id.largeClock));
			
			for(View v : clockPreviews)
			{
				if (hideClock.isChecked())
				{
					getView().findViewById(R.id.clockViewAnimator).setVisibility(View.INVISIBLE);
				}
				else
				{
					getView().findViewById(R.id.clockViewAnimator).setVisibility(View.VISIBLE);
					TextView hours = (TextView) v.findViewById(R.id.hours);
					TextView minutes = (TextView) v.findViewById(R.id.minutes);
					TextView ampm = (TextView) v.findViewById(R.id.ampm);
					TextView date = (TextView) v.findViewById(R.id.date);
					TextView alarm = (TextView) v.findViewById(R.id.alarmtime);
                    ImageView alarmIcon = (ImageView) v.findViewById(R.id.alarm_clock_image);
					
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
				    String datestr = DateFormat.getLongDateFormat(getActivity()).format(t.toMillis(true));
				    date.setText(datestr.toUpperCase(Locale.getDefault()));
				    
				    // display next alarm if needed
				    String nextAlarm = Settings.System.getString(getActivity().getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
				    int alarmColor = ((ColorDrawable)alarmColorView.getBackground()).getColor();
                    if (nextAlarm != null && !nextAlarm.equals("") && alarmColor != Resources.getSystem().getColor(android.R.color.transparent))
				    {
				    	alarm.setVisibility(View.VISIBLE);
                        alarmIcon.setVisibility(View.VISIBLE);
                        alarmIcon.setImageBitmap(getClockIcon(alarmColor));
				    	alarm.setText(nextAlarm.toUpperCase(Locale.getDefault()));
				    }
				    else
				    {
                        alarmIcon.setVisibility(View.GONE);
				    	alarm.setVisibility(View.GONE);
				    }
				    // set clock text color
				    int bgColor = ((ColorDrawable) bgColorView.getBackground()).getColor();			    
				    v.setBackgroundColor(bgColor);
				    v.getBackground().setAlpha(bgClockOpacitySlider.getProgress()*255/100);
				    
				    hours.setTextColor(((ColorDrawable)clockColorView.getBackground()).getColor());
				    minutes.setTextColor(((ColorDrawable)clockColorView.getBackground()).getColor());
				    ampm.setTextColor(((ColorDrawable)clockColorView.getBackground()).getColor());
				    date.setTextColor(((ColorDrawable)dateColorView.getBackground()).getColor());
				    alarm.setTextColor(((ColorDrawable)alarmColorView.getBackground()).getColor());
				    if (((ColorDrawable)dateColorView.getBackground()).getColor() == Color.TRANSPARENT)
				    	date.setVisibility(View.GONE);
				    else
				    	date.setVisibility(View.VISIBLE);
				    
				    if (((ColorDrawable)alarmColorView.getBackground()).getColor() == Color.TRANSPARENT)
				    	alarm.setVisibility(View.GONE);
				    
				    if (boldHours.isChecked())
				    {
				    	hours.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
				    }
				    else
				    {
				    	hours.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
				    }
				    
				    if (boldMinutes.isChecked())
				    {
				    	minutes.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
				    }
				    else
				    {
				    	minutes.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
				    }
				}
			}			
		}

		public void loadSettings() 
		{
			Context ctx = getActivity();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
			
			// setup clock
			setupClockStyle();
			
			// setup checkboxes
			showClearAll.setChecked(prefs.getBoolean(widgetMode + "." + SettingsManager.SHOW_CLEAR_BUTTON, widgetMode.equals(SettingsManager.COLLAPSED_WIDGET_MODE)?false:true));
			hideClock.setChecked(prefs.getBoolean(widgetMode + "." + SettingsManager.CLOCK_HIDDEN, false));
            showPersistent.setChecked(prefs.getBoolean(widgetMode + "." + SettingsManager.SHOW_PERSISTENT_NOTIFICATIONS, true));
			
			// setup toggles
			clockClickable.setChecked(prefs.getBoolean(widgetMode + "." + SettingsManager.CLOCK_IS_CLICKABLE, true));
			boldHours.setChecked(prefs.getBoolean(widgetMode + "." + SettingsManager.BOLD_HOURS, true));
			boldMinutes.setChecked(prefs.getBoolean(widgetMode + "." + SettingsManager.BOLD_MINUTES, false));
			
			// setup opacity slider
			int opacity = prefs.getInt(widgetMode + "." + SettingsManager.CLOCK_BG_OPACITY, 0);
			bgClockOpacitySlider.setProgress(opacity);
			
			// setup color buttons
			int bgColor = prefs.getInt(widgetMode + "." + SettingsManager.CLOCK_BG_COLOR, Color.BLACK);
			int clockColor = prefs.getInt(widgetMode + "." + SettingsManager.CLOCK_COLOR, Color.WHITE);
			int dateColor = prefs.getInt(widgetMode + "." + SettingsManager.CLOCK_DATE_COLOR, Color.WHITE);
			int alarmColor = prefs.getInt(widgetMode + "." + SettingsManager.CLOCK_ALARM_COLOR, Color.GRAY);
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
			String defaultClockStyle = widgetMode.equals(SettingsManager.COLLAPSED_WIDGET_MODE)?
					SettingsManager.CLOCK_AUTO :
					prefs.getString(SettingsManager.CLOCK_STYLE, SettingsManager.CLOCK_STYLE);
			
			String clockStyle = prefs.getString(widgetMode + "." + SettingsManager.CLOCK_STYLE, defaultClockStyle);
			
			autoSwitch.setChecked(false);
			
			if (clockStyle.equals(SettingsManager.CLOCK_SMALL))
			{
				clockStyleRG.check(R.id.radioSmallClock);
				clockStyleView.setDisplayedChild(0);
			}
			else if (clockStyle.equals(SettingsManager.CLOCK_MEDIUM))
			{
				clockStyleRG.check(R.id.radioMedium);
				clockStyleView.setDisplayedChild(1);
			}
			else if (clockStyle.equals(SettingsManager.CLOCK_LARGE))
			{
				clockStyleRG.check(R.id.radioLargeClock);
				clockStyleView.setDisplayedChild(2);
			}
			else if (clockStyle.equals(SettingsManager.CLOCK_AUTO))
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
            //hideClock = (CheckBox) clockSettingsView.findViewById(R.id.hideClockCheckbox);
            //showPersistent = (CheckBox) clockSettingsView.findViewById(R.id.showPersistentCheckbox);
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
				// refresh widget
				getActivity().sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));

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
	
	public static class NotificationSectionFragment extends Fragment implements OnSeekBarChangeListener, OnItemSelectedListener
	{	
		public NotificationSectionFragment()
		{
		}
		
		public void loadSettings() 
		{
			Context ctx = getActivity();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
			
			setupNotificationStyle();
			
			// setup toggles
			notificationClickable.setChecked(prefs.getBoolean(widgetMode + "." + SettingsManager.NOTIFICATION_IS_CLICKABLE, true));
			useExpandedText.setChecked(prefs.getBoolean(widgetMode + "." + AppSettingsActivity.USE_EXPANDED_TEXT, true));
			iconClickable.setChecked(prefs.getBoolean(widgetMode + "." + SettingsManager.NOTIFICATION_ICON_IS_CLICKABLE, true));

			// setup opacity slider
			int defaultOpacity = widgetMode.equals(SettingsManager.COLLAPSED_WIDGET_MODE)?0:50;
			int opacity = prefs.getInt(widgetMode + "." + SettingsManager.NOTIFICATION_BG_OPACITY, defaultOpacity);
			notificationBgClockOpacitySlider.setProgress(opacity);
			
			// setup color buttons
			int bgColor = prefs.getInt(widgetMode + "." + SettingsManager.NOTIFICATION_BG_COLOR, Color.BLACK);
			int titleColor = prefs.getInt(widgetMode + "." + SettingsManager.TITLE_COLOR, Color.WHITE);
			int textColor = prefs.getInt(widgetMode + "." + SettingsManager.TEXT_COLOR, Color.LTGRAY);
			int contentColor = prefs.getInt(widgetMode + "." + SettingsManager.CONTENT_COLOR, Color.DKGRAY);
			notificationBgColorView.setBackgroundColor(bgColor);
			titleColorView.setBackgroundColor(titleColor);
			textColorView.setBackgroundColor(textColor);
			contentColorView.setBackgroundColor(contentColor);
			
			// setup number of lines
			int maxLines = prefs.getInt(widgetMode + "." + SettingsManager.MAX_LINES, 1);
			if (maxLines <= 9)
				maxLinesSpinner.setSelection(maxLines-1);
			else
				maxLinesSpinner.setSelection(9);

            ((CheckBox)getView().findViewById(R.id.showActionBarCheckbox)).setChecked(prefs.getBoolean(widgetMode + "." + SettingsManager.SHOW_ACTIONBAR, false));
		}

		private void setupNotificationStyle() 
		{
			Context ctx = getActivity();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
			
			// clock style
			String defaultNotificationStyle = widgetMode.equals(SettingsManager.COLLAPSED_WIDGET_MODE)?
					"compact" : "normal";
			
			String notificationStyle = prefs.getString(widgetMode + "." + SettingsManager.NOTIFICATION_STYLE, defaultNotificationStyle);
						
			if (notificationStyle.equals("compact"))
			{
				notificationStyleRG.check(R.id.notificationStyleCompactRadio);
				notificationStyleView.setDisplayedChild(0);
			}
			else if (notificationStyle.equals("normal"))
			{
				notificationStyleRG.check(R.id.notificationStyleNormalRadio);
				notificationStyleView.setDisplayedChild(1);
			}
			else if (notificationStyle.equals("large"))
			{
				notificationStyleRG.check(R.id.notificationStyleOriginal);
				notificationStyleView.setDisplayedChild(2);
			}
		}
		
		public void refreshPreview() 
		{
			int bgColor = ((ColorDrawable)AppearanceActivity.notificationBgColorView.getBackground()).getColor();
			int bgAlpha = AppearanceActivity.notificationBgClockOpacitySlider.getProgress();
			int titleColor = ((ColorDrawable)AppearanceActivity.titleColorView.getBackground()).getColor();
			int textColor = ((ColorDrawable)AppearanceActivity.textColorView.getBackground()).getColor();
			int contentColor = ((ColorDrawable)AppearanceActivity.contentColorView.getBackground()).getColor();
			boolean expanded = AppearanceActivity.useExpandedText.isChecked();
			boolean iconClickable = AppearanceActivity.iconClickable.isChecked();
			int maxLines = AppearanceActivity.maxLinesSpinner.getSelectedItemPosition()+1;
			if (maxLines > 9) maxLines = 999;
		
			List<View> notificationPreviews = Arrays.asList(getView().findViewById(R.id.compactNotification),
					 getView().findViewById(R.id.normalNotification),
					 getView().findViewById(R.id.largeNotification));

			for(View v : notificationPreviews)
			{
				v.setBackgroundColor(bgColor);
				v.getBackground().setAlpha(bgAlpha * 255 / 100);
				
				TextView title = (TextView) v.findViewById(R.id.notificationTitle);
				TextView text = (TextView) v.findViewById(R.id.notificationText);
				CharSequence titleDemoStr = getActivity().getString(R.string.title_appname_demo_text);
				CharSequence textDemoStr = getActivity().getString(R.string.text_demo_text);
				
				if (expanded)
				{
					titleDemoStr =getActivity().getString(R.string.title_demo_text);
				}
				if (title != null)
				{
					title.setText(titleDemoStr);
					title.setTextColor(titleColor);
					if (titleColor == Color.TRANSPARENT)
						title.setVisibility(View.GONE);
					else
						title.setVisibility(View.VISIBLE);
				}
				else if (titleColor != Color.TRANSPARENT)
				{
					// combine title and text into one string
					textDemoStr = TextUtils.concat(titleDemoStr," ", textDemoStr);
					
					// set colors for title and terxt
					SpannableStringBuilder ssb = new SpannableStringBuilder(textDemoStr);
					CharacterStyle titleStyle = new ForegroundColorSpan(titleColor);
					CharacterStyle textStyle = new ForegroundColorSpan(textColor);
					ssb.setSpan(titleStyle, 0, titleDemoStr.length(),0);
					ssb.setSpan(textStyle, titleDemoStr.length()+1, textDemoStr.length(),0);
					
					textDemoStr = ssb;
				}
				
				if (text != null)
				{		
					text.setTextColor(textColor);
					if (textColor == Color.TRANSPARENT && title != null && v.getId() == R.id.largeNotification)
					{
						text.setVisibility(View.GONE);
					}
					else if (textColor != Color.TRANSPARENT)
					{
						text.setVisibility(View.VISIBLE);
						text.setText(textDemoStr);						
						text.setMaxLines(maxLines);
					}
					else
					{
						text.setVisibility(View.VISIBLE);
					}
				}
				
				TextView content = (TextView) v.findViewById(R.id.notificationContent);
				if (content != null)
				{
					if (expanded)
						content.setText(R.string.content_expanded_demo_text);
					else
						content.setText(R.string.content_demo_text);
						
					content.setTextColor(contentColor);
					
					// set max lines					
					content.setMaxLines(maxLines);					
				}
				TextView time = (TextView) v.findViewById(R.id.notificationTime);
				if (time != null)
				{
					time.setText(R.string.time_demo_text);
					time.setTextColor(contentColor);
				}
				TextView count = (TextView) v.findViewById(R.id.notificationCount);
				if (count != null)
				{
					count.setText(R.string.count_demo_text);
					count.setTextColor(contentColor);
				}

                View iconBg = v.findViewById(R.id.notificationIconBG);
                if (iconBg != null)
                {
                    iconBg.getBackground().setAlpha(bgAlpha * 255 / 100);
                }
				
				View iconSpinner = v.findViewById(R.id.notificationSpinner);
				if (iconSpinner != null)
				{
					if (iconClickable)
					{
						iconSpinner.setVisibility(View.VISIBLE);
					}
					else
					{
						iconSpinner.setVisibility(View.GONE);
					}
				}				
			}
		}

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState)
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(view.getContext());
            // setup number of lines
            int maxLines = prefs.getInt(widgetMode + "." + SettingsManager.MAX_LINES, 1);
            if (maxLines <= 9)
                maxLinesSpinner.setSelection(maxLines-1);
            else
                maxLinesSpinner.setSelection(9);

            super.onViewCreated(view, savedInstanceState);
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
			
			notificationStyleRG = (RadioGroup) notificationSettingsView.findViewById(R.id.notificationStyleRG);
			notificationStyleCompact = (RadioButton) notificationSettingsView.findViewById(R.id.notificationStyleCompactRadio);
			notificationStyleNormal = (RadioButton) notificationSettingsView.findViewById(R.id.notificationStyleNormalRadio);
			notificationStyleLarge = (RadioButton) notificationSettingsView.findViewById(R.id.notificationStyleOriginal);
			notificationStyleView = (ViewAnimator) notificationSettingsView.findViewById(R.id.notificationViewFlipper);

			// toggles
			notificationClickable = (ToggleButton) notificationSettingsView.findViewById(R.id.notificationClickableToggle);
			useExpandedText = (ToggleButton) notificationSettingsView.findViewById(R.id.notificationExpandedToggle);
			iconClickable = (ToggleButton) notificationSettingsView.findViewById(R.id.iconClickableToggle);

			// color buttons
			notificationBgColorView = notificationSettingsView.findViewById(R.id.bgColorView);
			titleColorView = notificationSettingsView.findViewById(R.id.titleColorView);
			textColorView = notificationSettingsView.findViewById(R.id.textColorView);
			contentColorView = notificationSettingsView.findViewById(R.id.contentColorView);
			notificationBgColorButton = (ViewGroup) notificationSettingsView.findViewById(R.id.bgColorButton);
			titleColorButton = (ViewGroup) notificationSettingsView.findViewById(R.id.titleColorButton);
			textColorButton = (ViewGroup) notificationSettingsView.findViewById(R.id.textColorButton);
			contentColorButton = (ViewGroup) notificationSettingsView.findViewById(R.id.contentColorButton);
			
			// seekbar
			notificationBgClockOpacitySlider = (SeekBar) notificationSettingsView.findViewById(R.id.bgTransparencySeekBar);
			notificationBgClockOpacitySlider.setOnSeekBarChangeListener(this);

			// maxlines spinner
			maxLinesSpinner = (Spinner) notificationSettingsView.findViewById(R.id.maxLinesSpinner);
			maxLinesSpinner.setOnItemSelectedListener(this);

			return notificationSettingsView;
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
				// refresh widget
				getActivity().sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));

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

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) 
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			
			if (pos <9)
				prefs.edit().putInt(widgetMode + "." + SettingsManager.MAX_LINES, pos+1).commit();
			else
				prefs.edit().putInt(widgetMode + "." + SettingsManager.MAX_LINES, 999).commit();
				
			refreshPreview();
			// refresh widget
			getActivity().sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));

		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) 
		{
			// do nothing
		}

	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) 
	{
		int descId = 0; 
		
		switch (position)
		{
		case 0:
			widgetMode = SettingsManager.COLLAPSED_WIDGET_MODE;
			descId = R.string.widget_mode_collapsed_desc;
			break;
		case 1:
			widgetMode = SettingsManager.EXPANDED_WIDGET_MODE;
			descId = R.string.widget_mode_expanded_desc;
			break;
		case 2:
			widgetMode = SettingsManager.HOME_WIDGET_MODE;
			descId = R.string.widget_mode_home_desc;
			break;
		}

        TextView widgetModeDesc = (TextView) findViewById(R.id.widgetmode_desc);
        if (widgetModeDesc != null) widgetModeDesc.setText(descId);

		if (clockSettingsFragment!=null)
		{					
			clockSettingsFragment.loadSettings();
			clockSettingsFragment.refreshPreview();
		}
		if (notificationsSettingsFragment!=null)
		{
			notificationsSettingsFragment.loadSettings();
			notificationsSettingsFragment.refreshPreview();
		}
		return true;
	}
}
