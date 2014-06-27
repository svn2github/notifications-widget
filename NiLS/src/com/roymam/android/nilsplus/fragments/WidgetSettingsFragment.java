package com.roymam.android.nilsplus.fragments;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;

import com.roymam.android.common.BitmapUtils;
import com.roymam.android.nilsplus.ui.NiLSActivity;
import com.roymam.android.notificationswidget.AppSettingsActivity;
import com.roymam.android.notificationswidget.NotificationsWidgetProvider;
import com.roymam.android.notificationswidget.R;
import com.roymam.android.notificationswidget.SettingsManager;

import net.margaritov.preference.colorpicker.AlphaPatternDrawable;
import net.margaritov.preference.colorpicker.ColorPickerDialog;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WidgetSettingsFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener, View.OnClickListener, ActionBar.OnNavigationListener {
    private Context mContext;
    private View notificationsSettingsView;
    private View clockSettingsView;
    private Switch showNotifications;
    private Switch showClock;
    private Switch showPersistent;
    private Button persistentNotificationsSettingsButton;
    private CheckBox mShowActionsCheckbox;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.widget_settings, null);

        notificationsSettingsView = view.findViewById(R.id.notifications_settings_view);
        clockSettingsView = view.findViewById(R.id.clock_settings_view);

        // building clock settings view
        View smallClockView = inflater.inflate(R.layout.small_clock, null);
        View mediumClockView = inflater.inflate(R.layout.medium_clock, null);
        View largeClockView = inflater.inflate(R.layout.large_clock, null);
        ViewAnimator v = (ViewAnimator) view.findViewById(R.id.clockViewAnimator);
        v.addView(smallClockView);
        v.addView(mediumClockView);
        v.addView(largeClockView);
        Drawable gridbg = new AlphaPatternDrawable((int) (5 * getActivity().getResources().getDisplayMetrics().density));
        gridbg.setAlpha(64);
        v.setBackgroundDrawable(gridbg);
        smallClockView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mediumClockView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        largeClockView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

        // main show_clock switch
        showClock = (Switch) view.findViewById(R.id.show_clock);
        showClock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                if (isChecked) {
                    clockSettingsView.setVisibility(View.VISIBLE);
                    prefs.edit().putBoolean(widgetMode + "." + SettingsManager.CLOCK_HIDDEN, false).commit();
                } else {
                    clockSettingsView.setVisibility(View.GONE);
                    prefs.edit().putBoolean(widgetMode + "." + SettingsManager.CLOCK_HIDDEN, true).commit();
                }
            }
        });

        // set views events
        clockStyleView = v;

        // clock style radio group
        clockStyleRG = (RadioGroup) clockSettingsView.findViewById(R.id.clockStyleRG);

        // auto switch toggle
        autoSwitch = (CheckBox)clockSettingsView.findViewById(R.id.autoSizeCheckbox);
        autoSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
                refreshPreview();

                // refresh widget
                getActivity().sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));
            }
        });

        // show clear all toggle
        showClearAll = (CheckBox)clockSettingsView.findViewById(R.id.showClearButtonCheckbox);
        showClearAll.setOnClickListener(this);

        // is clock clickable toggle
        clockClickable = (ToggleButton)clockSettingsView.findViewById(R.id.clockIsClickableToggle);
        clockClickable.setOnClickListener(this);

        // bold hours / minutes
        boldHours = (ToggleButton)clockSettingsView.findViewById(R.id.boldHoursToggle);
        boldHours.setOnClickListener(this);
        boldMinutes = (ToggleButton)clockSettingsView.findViewById(R.id.boldMinutesToggle);
        boldMinutes.setOnClickListener(this);

        // color buttons
        bgClockOpacitySlider = (SeekBar)clockSettingsView.findViewById(R.id.clockBGTransparencySeekBar);
        bgClockOpacitySlider.setOnSeekBarChangeListener(this);

        bgColorButton = (ViewGroup)clockSettingsView.findViewById(R.id.clockBGColorButton);
        bgColorButton.setOnClickListener(this);

        clockColorButton = (ViewGroup)clockSettingsView.findViewById(R.id.clockColorButton);
        clockColorButton.setOnClickListener(this);

        dateColorButton = (ViewGroup)clockSettingsView.findViewById(R.id.dateColorButton);
        dateColorButton.setOnClickListener(this);

        alarmColorButton = (ViewGroup)clockSettingsView.findViewById(R.id.alarmColorButton);
        alarmColorButton.setOnClickListener(this);

        bgColorView = clockSettingsView.findViewById(R.id.clockBGColorView);
        clockColorView = clockSettingsView.findViewById(R.id.clockColorView);
        dateColorView = clockSettingsView.findViewById(R.id.dateColorView);
        alarmColorView = clockSettingsView.findViewById(R.id.alarmColorView);

        // load notification settings view from layouts
        View ncView = inflater.inflate(R.layout.notification_compact, null);
        View nnView = inflater.inflate(R.layout.notification_normal, null);
        View nlView = inflater.inflate(R.layout.notification_large, null);
        ViewAnimator v2 = (ViewAnimator) view.findViewById(R.id.notificationViewFlipper);
        v2.addView(ncView);
        v2.addView(nnView);
        v2.addView(nlView);
        v2.setBackgroundDrawable(gridbg);

        ncView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        nnView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        nlView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

        notificationStyleRG = (RadioGroup) view.findViewById(R.id.notificationStyleRG);
        notificationStyleRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                onNotificationStyleChanged(checkedId);
            }
        });
        notificationStyleCompact = (RadioButton) view.findViewById(R.id.notificationStyleCompactRadio);
        notificationStyleNormal = (RadioButton) view.findViewById(R.id.notificationStyleNormalRadio);
        notificationStyleLarge = (RadioButton) view.findViewById(R.id.notificationStyleOriginal);
        notificationStyleView = (ViewAnimator) view.findViewById(R.id.notificationViewFlipper);

        // toggles
        showNotifications = (Switch) view.findViewById(R.id.show_notifications);
        showNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                if (isChecked)
                {
                    notificationsSettingsView.setVisibility(View.VISIBLE);
                    prefs.edit().putBoolean(widgetMode + "." + SettingsManager.HIDE_NOTIFICATIONS, false).commit();
                }
                else
                {
                    notificationsSettingsView.setVisibility(View.GONE);
                    prefs.edit().putBoolean(widgetMode + "." + SettingsManager.HIDE_NOTIFICATIONS, true).commit();
                }
            }
        });

        notificationClickable = (ToggleButton) view.findViewById(R.id.notificationClickableToggle);
        notificationClickable.setOnClickListener(this);

        useExpandedText = (ToggleButton) view.findViewById(R.id.notificationExpandedToggle);
        useExpandedText.setOnClickListener(this);

        iconClickable = (ToggleButton) view.findViewById(R.id.iconClickableToggle);
        iconClickable.setOnClickListener(this);

        // color buttons
        notificationBgColorView = view.findViewById(R.id.bgColorView);
        titleColorView = view.findViewById(R.id.titleColorView);
        textColorView = view.findViewById(R.id.textColorView);
        contentColorView = view.findViewById(R.id.contentColorView);

        notificationBgColorButton = (ViewGroup) view.findViewById(R.id.bgColorButton);
        notificationBgColorButton.setOnClickListener(this);

        titleColorButton = (ViewGroup) view.findViewById(R.id.titleColorButton);
        titleColorButton.setOnClickListener(this);

        textColorButton = (ViewGroup) view.findViewById(R.id.textColorButton);
        textColorButton.setOnClickListener(this);

        contentColorButton = (ViewGroup) view.findViewById(R.id.contentColorButton);
        contentColorButton.setOnClickListener(this);

        // seekbar
        notificationBgClockOpacitySlider = (SeekBar) view.findViewById(R.id.bgTransparencySeekBar);
        notificationBgClockOpacitySlider.setOnSeekBarChangeListener(this);

        // maxlines spinner
        maxLinesSpinner = (Spinner) view.findViewById(R.id.maxLinesSpinner);
        maxLinesSpinner.setOnItemSelectedListener(this);

        // persistent notifications
        persistentNotificationsSettingsButton = (Button) view.findViewById(R.id.persistent_notifications_settings_button);
        persistentNotificationsSettingsButton.setOnClickListener(this);

        showPersistent = (Switch) view.findViewById(R.id.show_persistent);
        showPersistent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                if (isChecked) {
                    persistentNotificationsSettingsButton.setVisibility(View.VISIBLE);
                    prefs.edit().putBoolean(widgetMode + "." + SettingsManager.SHOW_PERSISTENT_NOTIFICATIONS, true).commit();
                } else {
                    persistentNotificationsSettingsButton.setVisibility(View.GONE);
                    prefs.edit().putBoolean(widgetMode + "." + SettingsManager.SHOW_PERSISTENT_NOTIFICATIONS, false).commit();
                }
            }
        });


        // load settings from global_settings
        loadClockSettings();
        loadNotificationsSettings();
        loadPersistentSettings();

        setupActionBar();

        return view;

    }

    private void loadPersistentSettings()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        boolean showPersistentBool = prefs.getBoolean(widgetMode + "." + SettingsManager.SHOW_PERSISTENT_NOTIFICATIONS, true);
        showPersistent.setChecked(showPersistentBool);
        if (!showPersistentBool)
        {
            persistentNotificationsSettingsButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
    }

    // selected widget mode
    private static String 	 widgetMode = SettingsManager.COLLAPSED_WIDGET_MODE;

    // fragements
    private int updateViewId = 0;
    private ColorPickerDialog colorDialog;

    // clock form fields
    private static RadioGroup clockStyleRG;
    private static CheckBox autoSwitch;
    private static CheckBox showClearAll;
    //private static CheckBox hideClock;
    //private static CheckBox showPersistent;
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

    private void setupActionBar()
    {
        // Show the Up button in the action bar.
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        CharSequence[] modes = getResources().getStringArray(R.array.widget_mode_entries);

        ArrayAdapter<CharSequence> list = new ArrayAdapter<CharSequence>(getActivity(), R.layout.spinner_widget_mode, android.R.id.text1, modes);
        list.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        getActivity().getActionBar().setListNavigationCallbacks(list, this);
        getActivity().getActionBar().setDisplayShowTitleEnabled(false);

        // select the last widget mode that was changed
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String widgetMode = prefs.getString(SettingsManager.LAST_WIDGET_MODE, SettingsManager.EXPANDED_WIDGET_MODE);
        int itemPosition;
        if (widgetMode.equals(SettingsManager.COLLAPSED_WIDGET_MODE))
            itemPosition = 0;
        else if (widgetMode.equals(SettingsManager.EXPANDED_WIDGET_MODE))
            itemPosition = 1;
        else
            itemPosition = 2;
        getActivity().getActionBar().setSelectedNavigationItem(itemPosition);
    }

    public void onClockStyleChanged(int id)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        String clockStyle = null;
        int position = 0;

        switch(id)
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

        refreshPreview();

        // refresh widget
        mContext.sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));
    }

    public void onNotificationStyleChanged(int id)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        String notificationStyle = null;
        int position = 0;

        switch(id)
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
        refreshPreview();

        // refresh widget
        mContext.sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));
    }

    public void onAutoSwitchChanged(View v)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
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
        refreshPreview();

        // refresh widget
        mContext.sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));

    }

    public void onColorChoose(int updateViewId, int colorId)
    {
        if (updateViewId != 0)
        {
            // set background color using the color button background
            View v2 = getActivity().findViewById(updateViewId);
            v2.setBackgroundColor(colorId);

            // store value using the view preference
            String settings = (String) v2.getTag();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            prefs.edit().putInt(widgetMode + "." + settings, colorId).commit();
            refreshPreview();

            // refresh widget
            mContext.sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));

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
        final int updateViewId = viewId;

        View v = getActivity().findViewById(updateViewId);
        ColorDrawable bg = (ColorDrawable) v.getBackground();
        colorDialog = new ColorPickerDialog(getActivity(), bg.getColor());
        colorDialog.setHexValueEnabled(true);
        colorDialog.setAlphaSliderVisible(true);
        colorDialog.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener()
        {
            @Override
            public void onColorChanged(int color)
            {
                onColorChoose(updateViewId, color);
            }
        });
        colorDialog.show();
    }

    private Bitmap getClockIcon(int color)
    {
        Bitmap sourceBitmap = BitmapFactory.decodeResource(Resources.getSystem(), android.R.drawable.ic_lock_idle_alarm);
        return BitmapUtils.colorBitmap(sourceBitmap, color);
    }

        public void refreshPreview()
        {
            // refresh clock preview
            List<View> clockPreviews = Arrays.asList(clockSettingsView.findViewById(R.id.smallClock),
                    clockSettingsView.findViewById(R.id.mediumClock),
                    clockSettingsView.findViewById(R.id.largeClock));

            for(View v : clockPreviews)
            {
                    clockSettingsView.findViewById(R.id.clockViewAnimator).setVisibility(View.VISIBLE);
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

            // refresh notification preview
            int bgColor = ((ColorDrawable)notificationBgColorView.getBackground()).getColor();
            int bgAlpha = notificationBgClockOpacitySlider.getProgress();
            int titleColor = ((ColorDrawable)titleColorView.getBackground()).getColor();
            int textColor = ((ColorDrawable)textColorView.getBackground()).getColor();
            int contentColor = ((ColorDrawable) contentColorView.getBackground()).getColor();
            boolean expanded = useExpandedText.isChecked();
            boolean isIconClickable = iconClickable.isChecked();
            int maxLines = maxLinesSpinner.getSelectedItemPosition()+1;
            if (maxLines > 9) maxLines = 999;

            List<View> notificationPreviews = Arrays.asList(notificationsSettingsView.findViewById(R.id.compactNotification),
                    notificationsSettingsView.findViewById(R.id.normalNotification),
                    notificationsSettingsView.findViewById(R.id.largeNotification));

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
                    textDemoStr = TextUtils.concat(titleDemoStr, " ", textDemoStr);

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
                    if (isIconClickable)
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

        public void loadClockSettings()
        {
            Context ctx = getActivity();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            // setup clock
            setupClockStyle();

            // setup checkboxes
            showClearAll.setChecked(prefs.getBoolean(widgetMode + "." + SettingsManager.SHOW_CLEAR_BUTTON, widgetMode.equals(SettingsManager.COLLAPSED_WIDGET_MODE) ? false : true));

            boolean hideClock = prefs.getBoolean(widgetMode + "." + SettingsManager.CLOCK_HIDDEN, false);
            showClock.setChecked(!hideClock);
            if (hideClock) clockSettingsView.setVisibility(View.GONE);

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
                    prefs.getString(SettingsManager.CLOCK_STYLE, SettingsManager.CLOCK_AUTO);

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
            else //if (clockStyle.equals(SettingsManager.CLOCK_AUTO))
            {
                clockStyleRG.check(R.id.radioLargeClock);
                autoSwitch.setChecked(true);
                clockStyleView.setDisplayedChild(2);
            }

            // set on change listener so auto switch will be turned off when user change style
            clockStyleRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId)
                {
                    onClockStyleChanged(checkedId);
                }
            });
        }


        public void loadNotificationsSettings()
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

            mShowActionsCheckbox = ((CheckBox)notificationsSettingsView.findViewById(R.id.showActionBarCheckbox));
            mShowActionsCheckbox.setChecked(prefs.getBoolean(widgetMode + "." + SettingsManager.SHOW_ACTIONBAR, false));
            mShowActionsCheckbox.setOnClickListener(this);

            boolean hideNotifications = SettingsManager.shouldHideNotifications(getActivity(), widgetMode);
            showNotifications.setChecked(!hideNotifications);
            if (hideNotifications) notificationsSettingsView.setVisibility(View.GONE);
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

    @Override
    public void onClick(View v)
    {
        if (v instanceof  CompoundButton)
        {
            boolean checked = ((CompoundButton)v).isChecked();
            String settings = (String) v.getTag();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            prefs.edit().putBoolean(widgetMode + "." + settings, checked).commit();
            refreshPreview();

            // refresh widget
            mContext.sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));
        }
        else if (v.getId() == R.id.clockBGColorButton)
        {
            onClockBGColorClick(v);
        }
        else if (v.getId() == R.id.clockColorButton)
        {
            onClockColorClick(v);
        }
        else if (v.getId() == R.id.alarmColorButton)
        {
            onAlarmColorClick(v);
        }
        else if (v.getId() == R.id.dateColorButton)
        {
            onDateColorClick(v);
        }
        else if (v.getId() == R.id.bgColorButton)
        {
            onNotificationkBGColorClick(v);
        }
        else if (v.getId() == R.id.titleColorButton)
        {
            onTitleColorClick(v);
        }
        else if (v.getId() == R.id.textColorButton)
        {
            onTextColorClick(v);
        }
        else if (v.getId() == R.id.contentColorButton)
        {
            onContentColorClick(v);
        }
        else if (v.getId() == R.id.persistent_notifications_settings_button)
        {
            NiLSActivity activity = (NiLSActivity) getActivity();

            // Insert the fragment by replacing any existing fragment
            activity.replaceFragment(new SettingsManager.PrefsPersistentNotificationsFragment());
            activity.getActionBar().setDisplayShowTitleEnabled(true);
            activity.setTitle(R.string.persistent_notifications);
            activity.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            activity.getDrawerToggle().setDrawerIndicatorEnabled(false);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // re-create the action bar and the back to drawer icon
        NiLSActivity activity = (NiLSActivity) getActivity();
        activity.getDrawerToggle().setDrawerIndicatorEnabled(true);
        setupActionBar();
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id)
    {
        int descId = 0;
        Log.d("NiLS", "pos:" + position + " id:" + id);
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

        ((TextView) getActivity().findViewById(R.id.widgetmode_desc)).setText(descId);

        loadClockSettings();
        loadNotificationsSettings();
        refreshPreview();

        return true;
    }
}