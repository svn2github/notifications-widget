package com.roymam.android.notificationswidget;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.roymam.android.common.IconPackManager;
import com.roymam.android.common.ListPreferenceChangeListener;
import com.roymam.android.nilsplus.CardPreferenceFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class SettingsManager
{
    private static final String TAG = SettingsManager.class.getSimpleName();

    // main settings
    public static final String NILS_SERVICE = "nils_service";
    public static final String AUTO_HIDE_SERVICE = "auto_hide_service";

    // widget settings - notifications
    public static final String DISABLE_AUTO_SWITCH = "disable_auto_switch"; // deprecated

    public static final String WIDGET_MODE = "widgetmode";
    public static final String COLLAPSED_WIDGET_MODE = "collapsed";
    public static final String EXPANDED_WIDGET_MODE = "expanded";
    public static final String HOME_WIDGET_MODE = "home";
    public static final String WIDGET_PRESENT = "widget_present";

    public static final String SHOW_CLEAR_BUTTON = "showclearbutton";
    public static final String NOTIFICATIONS_ORDER = "order_notifications_by";
    public static final String NOTIFICATION_STYLE="notification_style";
    public static final String MAX_LINES="max_lines";
    public static final String SHOW_ACTIONBAR="show_actionbar";
    public static final String NOTIFICATION_BG_OPACITY="notification_bg_opacity";
    public static final String TEXT_COLOR = "notification_text_color";
    public static final String NOTIFICATION_IS_CLICKABLE = "notification_is_clickable";
    public static final String NOTIFICATION_ICON_IS_CLICKABLE = "notificationicon_is_clickable";
    public static final String TITLE_COLOR = "notification_title_color";
    public static final String CONTENT_COLOR = "notification_content_color";
    public static final String NOTIFICATION_BG_COLOR = "notification_bg_color";
    public static final String LAST_WIDGET_MODE = "last_widget_mode";
    public static final String NOTIFICATION_ICON_BG_COLOR = "notification_icon_bg_color";
    public static final String SHOW_PERSISTENT_NOTIFICATIONS = "show_persistent";
    public static final String HIDE_NOTIFICATIONS = "hide_notifications";

    // widget settings - clock
    public static final String CLOCK_IS_CLICKABLE = "clockisclickable";
    public static final String BOLD_HOURS = "boldhours";
    public static final String BOLD_MINUTES = "boldminutes";
    public static final String CLOCK_BG_OPACITY = "clockbgopacity";
    public static final String CLOCK_STYLE = "clockstyle";
    public static final String CLOCK_COLOR = "clock_text_color";
    public static final String CLOCK_BG_COLOR = "clock_bg_color";
    public static final String CLOCK_ALARM_COLOR = "clock_alarm_color";
    public static final String CLOCK_DATE_COLOR = "clock_date_color";
    public static final String CLOCK_SMALL = "small";
    public static final String CLOCK_MEDIUM = "medium";
    public static final String CLOCK_LARGE = "large";
    public static final String CLOCK_HIDDEN = "clockhidden";
    public static final String CLOCK_AUTO = "auto";

    // wake up settings
    private static final String TURNSCREENON = "turnscreenon";
    private static final String DISABLE_PROXIMITY = "disableproximity";
    private static final String DELAYED_SCREEON = "delayed_screenon";
    public static final String WAKEUP_MODE = "wakeup_mode";
    public static final String WAKEUP_ALWAYS = "always";
    public static final String WAKEUP_NEVER = "never";
    public static final String WAKEUP_UNCOVERED = "when_uncovered";
    public static final String WAKEUP_NOT_COVERED = "when_not_covered";
    public static final String TURNSCREENOFF = "turnscreenoff";
    public static final String TURNSCREENOFF_DEFAULT = "default";

    // notification mode
    public static final String NOTIFICATION_MODE = "notification_mode";
    public static final String MODE_GROUPED = "grouped";
    public static final String MODE_SEPARATED = "separated";
    public static final String DEFAULT_NOTIFICATION_MODE = MODE_SEPARATED;

    // notification icon
    public static final String NOTIFICATION_ICON = "notification_icon";
    public static final String NOTIFICATION_MONO_ICON = "mono_icon";
    public static final String APP_ICON = "app_icon";
    public static final String DEFAULT_NOTIFICATION_ICON = NOTIFICATION_ICON;
    public static final String ICON_PACK = "icon_pack";
    public static final String DEFAULT_ICON_PACK = "none";

    // auto clear
    public static final String AUTO_CLEAR = "auto_clear";
    public static final String WHEN_CLEARED = "when_cleared";
    public static final String WHEN_APP_IS_OPENED ="when_app_is_opened";
    public static final String WHEN_NOTIFICATION_IS_OPENED = "when_notification_is_opened";
    public static final String WHEN_DEVICE_IS_UNLOCKED = "when_device_unlocked";

    // auto detect lock screen
    public static final String  AUTO_DETECT_LOCKSCREEN_APP="auto_detect_lock_screen_app";
    public static final boolean AUTO_DETECT_LOCKSCREEN_APP_DEFAULT=false;

    // other settings
    public static final String SYNC_BACK = "sync_back";
    public static final boolean DEFAULT_SYNC_BACK = true;

    public static final String COLLECT_ON_UNLOCK = "collectonunlock";
    public static final String APPS_SETTINGS = "specificapps";

    // deprecated
    private static final String CLEAR_ON_UNLOCK = "clearonunlock";
    private static final String CLEAR_APP_NOTIFICATIONS = "clear_app_notifications";
    private static final String FIRST_RUN = "first_run";
    private static final String SYNC_NOTIFICATIONS = "sync_notifications";
    private static final String SYNC_NOTIFICATIONS_DISABLED = "none";
    private static final String SYNC_NOTIFICATIONS_ONEWAY = "oneway";
    private static final String SYNC_NOTIFICATIONS_TWOWAY = "twoway";
    private static final String SYNC_NOTIFICATIONS_SMART = "smart";
    private static final String USE_MONO_ICON = "use_mono_icon";

    public static final String FP_ENABLED = "fp_enabled";
    public static final String POPUP_ENABLED ="popup_enabled";
    public static final boolean DEFAULT_FP_ENABLED = false;

    // settings strings
    public static final String LOCKSCREEN_APP = "lockscreenapp";
    public static final String PRIMARY_TEXT_COLOR = "primary_text_color";
    public static final String AUTO_TITLE_COLOR = "auto_title_color";
    public static final String SECONDARY_TEXT_COLOR = "secondary_text_color";
    public static final String MAIN_BG_COLOR = "main_bg_color";
    public static final String ICON_BG_COLOR = "icon_bg_color";
    public static final String ALT_MAIN_BG_COLOR = "alt_main_bg_color";
    public static final String ALT_ICON_BG_COLOR = "alt_icon_bg_color";
    public static final String VERTICAL_ALIGNMENT = "yalignment";
    public static final String FP_MAX_LINES = "maxlines";
    public static final String MAX_TEXT_LINES = "max_text_lines";
    public static final String SWIPE_TO_OPEN = "swipe_to_open";
    public static final String CLICK_TO_OPEN = "click_to_open";
    public static final String SHOW_QUICK_REPLY_ON_PREVIEW = "show_quick_reply_on_preview";
    public static final String UNLOCK_ON_OPEN = "unlock_on_open";
    public static final String DONT_HIDE = "dont_hide";
    public static final String SCREEN_ON_TIMEOUT = "screenon_timeout";
    public static final String ENABLE_RESIZE_MODE = "enable_resize_mode";
    public static final String TEMPORARY_UNLOCKED = "temporary_unlocked";
    public static final String UNLOCKED = "unlocked";
    public static final String SHOW_NEXT_PREVIEW = "show_next_preview";
    public static final String THEME = "theme";
    public static final String ICON_SIZE = "icon_size";
    public static final String PREVIEW_ICON_SIZE = "preview_icon_size";
    public static final String HIDE_ON_CLICK = "hide_on_click";
    public static final String HALO_MODE = "halo_mode";
    public static final String UNLOCK_WORKAROUND = "unlock_workaround";
    public static final String PREVIEW_HEIGHT = "preview_height";
    public static final String HEIGHT = "height";
    public static final String TITLE_FONT_SIZE = "title_size_sp";
    public static final String TEXT_FONT_SIZE = "text_size_sp";
    public static final String SINGLE_LINE = "single_line";
    public static final String SHOW_TIME = "show_time";
    public static final String FIT_HEIGHT_TO_CONTENT = "fit_height";
    public static final String SWIPE_DOWN_TO_DISMISS_ALL = "swipe_down_to_dismiss_all" ;
    public static final String MAIN_BG_OPACITY = "main_bg_opacity";

    // default values
    public static final int DEFAULT_PRIMARY_TEXT_COLOR = 0xffffffff;
    public static final int DEFAULT_SECONDARY_TEXT_COLOR = 0xffaaaaaa;
    public static final int DEFAULT_MAIN_BG_COLOR = 0x80000000;
    public static final int DEFAULT_ICON_BG_COLOR = 0xff1d3741;
    public static final int DEFAULT_ALT_MAIN_BG_COLOR = 0x80000000;
    public static final int DEFAULT_ALT_ICON_BG_COLOR = 0xff1d3741;
    public static final int DEFAULT_MAX_LINES = 4;
    public static final int DEFAULT_MAX_TEXT_LINES = 1;
    public static final boolean DEFAULT_SWIPE_TO_OPEN = false;
    public static final boolean DEFAULT_DONT_HIDE = false;
    public static final String DEFAULT_SCREEN_ON_TIMEOUT = "10";
    public static final boolean DEFAULT_CLICK_TO_OPEN = false;
    public static final boolean DEFAULT_SHOW_QUICK_REPLY_ON_PREVIEW = false;
    public static final boolean DEFAULT_UNLOCK_ON_OPEN = true;
    public static final boolean DEFAULT_ENABLE_RESIZE_MODE = true;
    public static final boolean DEFAULT_UNLOCKED = false;
    public static final boolean DEFAULT_SHOW_NEXT_PREVIEW = true;
    public static final String DEFAULT_THEME = "default";
    public static final int DEFAULT_ICON_SIZE=48;
    public static final int DEFAULT_ROW_HEIGHT=48;
    public static final int DEFAULT_SPACING = 4;
    public static final int DEFAULT_HEIGHT = DEFAULT_ROW_HEIGHT * DEFAULT_MAX_LINES + (DEFAULT_SPACING)*(DEFAULT_MAX_LINES-1);
    public static final int DEFAULT_PREVIEW_HEIGHT = DEFAULT_HEIGHT;
    public static final boolean DEFAULT_HALO_MODE = false;
    public static final boolean DEFAULT_UNLOCK_WORKAROUND = false;
    public static final boolean DEFAULT_HIDE_ON_CLICK = false;
    public static final String STOCK_LOCKSCREEN_PACKAGENAME = "com.android.keyguard";
    public static final String DEFAULT_LOCKSCREEN_APP = STOCK_LOCKSCREEN_PACKAGENAME;
    public static final String STOCK_PHONE_PACKAGENAME = "com.android.phone";
    public static final int DEFAULT_PREVIEW_ICON_SIZE = 64;
    public static final int DEFAULT_TITLE_FONT_SIZE = 18;
    public static final int DEFAULT_TEXT_FONT_SIZE = 14;
    public static final boolean DEFAULT_SINGLE_LINE = false;
    public static final String DEFAULT_VERTICAL_ALIGNMENT = "top";
    public static final boolean DEFAULT_SHOW_TIME = false;
    public static final boolean DEFAULT_FIT_HEIGHT_TO_CONTENT = true;
    public static final boolean DEFAULT_SWIPE_DOWN_TO_DISMISS_ALL = true;
    public static final int DEFAULT_MAIN_BG_OPACITY = 100;
    public static final String[] BLACKLIST_PACKAGENAMES =
            {       "com.google.android.valvet.ui",
                    "com.sec.android.app.clockpackage",
                    "com.sec.android.app.clock",
                    "com.gadgetjuice.dockclock",
                    "com.android.camera2",
                    "com.htc.camera",
                    "com.HTC.camera",
                    "com.jedga.peek",
                    "com.roymam.android.notificationswidget",
                    "com.roymam.android.notificationswidget.debug",
                    "com.android.systemui",
                    "cz.mpelant.deskclock",
                    "com.mobitobi.android.gentlealarm",
                    "com.android.dialer",
                    "com.lge.clock",
                    "com.lge.camera",
                    "com.lge.email",
                    "com.thinkleft.eightyeightsms.mms",
                    "com.whatsapp",
                    "com.tbig.playerpro",
                    "com.android.phone",
                    "com.android.deskclock",
                    "com.google.android.deskclock",
                    "ch.bitspin.timely",
                    "com.alarmclock.xtreme.free",
                    "com.achep.activedisplay",
                    "sg.com.mcd.mcdalarm",
                    "com.achep.acdisplay",
                    "com.sonyericsson.organizer",
                    "com.handcent.nextsms",
                    "com.supertext.phone",
                    "com.p1.chompsms",
                    "com.webascender.callerid"};
    public static final String SHOW_WELCOME_WIZARD = "show_welcome_wizard";

    // privacy options
    public static final String NOTIFICATION_PRIVACY = "notification_privacy";
    public static final String PRIVACY_SHOW_TITLE_ONLY = "show_title";
    public static final String PRIVACY_SHOW_TICKER_ONLY = "show_ticker";
    public static final String PRIVACY_SHOW_APPNAME_ONLY = "show_appname";
    public static final String PRIVACY_SHOW_ALL = "none";
    public static final String DEFAULT_NOTIFICATION_PRIVACY = PRIVACY_SHOW_ALL;
    public static final String IMMEDIATE_PROXIMITY = "immediate_proximity";
    public static final boolean DEFAULT_POPUP_ENABLED = false;
    public static String NUMBER_OF_LS_DETECT_REFUSES = "num_of_refuses";

    public static boolean shouldHideNotifications(Context context, String widgetMode)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean fpEnabled = prefs.getBoolean(SettingsManager.FP_ENABLED, SettingsManager.DEFAULT_FP_ENABLED);
        boolean overrideHideNotifications = prefs.getAll().containsKey(widgetMode + "." + SettingsManager.HIDE_NOTIFICATIONS);
        boolean hideNotifications = prefs.getBoolean(widgetMode + "." + SettingsManager.HIDE_NOTIFICATIONS, false);

        if ((fpEnabled && !overrideHideNotifications &&
                (widgetMode.equals(SettingsManager.EXPANDED_WIDGET_MODE) || widgetMode.equals(SettingsManager.COLLAPSED_WIDGET_MODE)))
                ||
                hideNotifications)
            return true;
        else
            return false;
    }

    public static String getWakeupMode(Context context, String packageName)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultWakeupMode;

        // setting default according to old settings
        if (!prefs.getBoolean(SettingsManager.TURNSCREENON, true))
            defaultWakeupMode = WAKEUP_NEVER;
        else
        if (prefs.getBoolean(SettingsManager.DISABLE_PROXIMITY, false))
            defaultWakeupMode = WAKEUP_ALWAYS;
        else
        if (prefs.getBoolean(SettingsManager.DELAYED_SCREEON, false))
            defaultWakeupMode = WAKEUP_UNCOVERED;
        else
        {
            // if the proximity sensor has immediate response, set wake up mode to use it by default
            if (prefs.getBoolean(SettingsManager.IMMEDIATE_PROXIMITY, true))
                defaultWakeupMode = WAKEUP_NOT_COVERED;
            else
                // otherwise - don't use proximity sensor
                defaultWakeupMode = WAKEUP_ALWAYS;
        }
        return prefs.getString(packageName + "." + WAKEUP_MODE, prefs.getString(WAKEUP_MODE, defaultWakeupMode));
    }

    public static String getNotificationMode(Context context, String packageName)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String mode = DEFAULT_NOTIFICATION_MODE;

        // first see if there is any app specific old settings
        String multipleEventsHandling = prefs.getString(packageName + "." + "multiple_events_handling", "");
        if (multipleEventsHandling.equals("show_first") || multipleEventsHandling.equals("show_last"))
            mode = MODE_SEPARATED;
        else if (multipleEventsHandling.equals("show_all"))
            mode = MODE_GROUPED;

        // get app specific settings or global settings if not exists
        mode = prefs.getString(packageName + "." + NOTIFICATION_MODE, prefs.getString(NOTIFICATION_MODE, mode));

        return mode;
    }

    private static HashSet<String> getDefaultAutoClear(Context context)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        HashSet<String> defaultAutoClear = new HashSet<String>(Arrays.asList(context.getResources().getStringArray(R.array.auto_clear_default_values)));

        if (prefs.getBoolean(CLEAR_ON_UNLOCK, false))
            defaultAutoClear.add(WHEN_DEVICE_IS_UNLOCKED);

        if (!prefs.getBoolean(CLEAR_APP_NOTIFICATIONS, true))
            defaultAutoClear.remove(WHEN_APP_IS_OPENED);

        if (prefs.getString(SYNC_NOTIFICATIONS, SYNC_NOTIFICATIONS_TWOWAY).equals(SYNC_NOTIFICATIONS_DISABLED))
            defaultAutoClear.remove(WHEN_CLEARED);

        return defaultAutoClear;
    }

    public static boolean shouldClearOnUnlock(Context context)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> autoClear = prefs.getStringSet(AUTO_CLEAR, getDefaultAutoClear(context));
        return (autoClear.contains(WHEN_DEVICE_IS_UNLOCKED));
    }

    public static boolean shouldClearWhenAppIsOpened(Context context)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> autoClear = prefs.getStringSet(AUTO_CLEAR, getDefaultAutoClear(context));
        return (autoClear.contains(WHEN_APP_IS_OPENED));
    }

    public static boolean shouldClearWhenClearedFromNotificationsBar(Context context)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> autoClear = prefs.getStringSet(AUTO_CLEAR, getDefaultAutoClear(context));
        return (autoClear.contains(WHEN_CLEARED));
    }

    public static String getPrivacy(Context context, String packageName)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(packageName + "." + NOTIFICATION_PRIVACY, prefs.getString(NOTIFICATION_PRIVACY, DEFAULT_NOTIFICATION_PRIVACY));
    }

    public static class HowToAddWidgetFragment extends Fragment
    {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                return inflater.inflate(R.layout.view_help_add_widget, null);
            else
                return inflater.inflate(R.layout.view_help_no_lswidgets, null);
        }
    }

    public static class PrefsGeneralFragment extends CardPreferenceFragment
	{
        //ComponentName mDeviceAdmin;
        //DevicePolicyManager mDPM;

        @Override
	    public void onCreate(Bundle savedInstanceState)
	    {
	        super.onCreate(savedInstanceState);

	        // Load the global_settings from an XML resource
	        addPreferencesFromResource(R.xml.global_settings);

            // auto wake up mode
            ListPreferenceChangeListener listener = new ListPreferenceChangeListener(
                    null,
                    getResources().getStringArray(R.array.wakeup_mode_entries),
                    getResources().getStringArray(R.array.wakeup_mode_values));

            Preference wakeupPref = findPreference(WAKEUP_MODE);
            String currValue = SettingsManager.getWakeupMode(getActivity(), null);
            wakeupPref.setDefaultValue(currValue);
            listener.setPrefSummary(wakeupPref, currValue);
            final ListPreferenceChangeListener finalListener = listener;
            wakeupPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    NotificationsService ns = (NotificationsService) NotificationsService.getSharedInstance();
                    String wakeupMode = (String)newValue;

                    if (wakeupMode.equals(WAKEUP_NOT_COVERED) || wakeupMode.equals(WAKEUP_UNCOVERED))
                    {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                        if (!prefs.getBoolean(SettingsManager.IMMEDIATE_PROXIMITY, true) || wakeupMode.equals(WAKEUP_UNCOVERED)) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.battery_usage_warning)
                                    .setMessage(R.string.battery_usage_warning_summary)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // continue
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();

                            if (ns != null) ns.startProximityMonitoring();
                        }
                    }
                    else
                        // stop proximity monitoring if its running
                        if (ns != null) ns.stopProximityMonitoring();

                    return finalListener.onPreferenceChange(preference, newValue);

                }
            });

            // notification mode
            listener = new ListPreferenceChangeListener(
                    null,
                    getResources().getStringArray(R.array.notification_modes_entries),
                    getResources().getStringArray(R.array.notification_modes_values));

            Preference pref = findPreference(NOTIFICATION_MODE);
            currValue = getPreferenceScreen().getSharedPreferences().getString(NOTIFICATION_MODE, DEFAULT_NOTIFICATION_MODE);
            listener.setPrefSummary(pref, currValue);
            pref.setOnPreferenceChangeListener(listener);

            // notification icon
            listener = new ListPreferenceChangeListener(
                    null,
                    getResources().getStringArray(R.array.notification_icon_entries),
                    getResources().getStringArray(R.array.notification_icon_values));

            pref = findPreference(NOTIFICATION_ICON);
            currValue = getPreferenceScreen().getSharedPreferences().getString(NOTIFICATION_ICON, DEFAULT_NOTIFICATION_ICON);
            listener.setPrefSummary(pref, currValue);
            pref.setOnPreferenceChangeListener(listener);

            // notification order by
	        listener = new ListPreferenceChangeListener(
                    null,
	        		getResources().getStringArray(R.array.auto_clear_entries),
	        		getResources().getStringArray(R.array.auto_clear_values));

            pref = findPreference(AUTO_CLEAR);
            pref.setDefaultValue(getDefaultAutoClear(getActivity()));
            Set<String> currValues = getPreferenceScreen().getSharedPreferences().getStringSet(AUTO_CLEAR, getDefaultAutoClear(getActivity()));
            listener.setPrefSummary(pref, currValues);
            pref.setOnPreferenceChangeListener(listener);

            // notification order by
            listener = new ListPreferenceChangeListener(
                    null,
                    getResources().getStringArray(R.array.settings_orderby_entries),
                    getResources().getStringArray(R.array.settings_orderby_values));

            pref = findPreference(NOTIFICATIONS_ORDER);
	        currValue = getPreferenceScreen().getSharedPreferences().getString(NOTIFICATIONS_ORDER, "time");
	        listener.setPrefSummary(pref, currValue);
	        pref.setOnPreferenceChangeListener(listener);

            // icon pack handling
            List<String> iconPackEntries = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.icon_pack_entries)));
            List<String> iconPackValues = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.icon_pack_values)));

            IconPackManager ipm = IconPackManager.getInstance(getActivity());
            HashMap<String, IconPackManager.IconPack> iconpacks = ipm.getAvailableIconPacks(true);

            for (Entry<String, IconPackManager.IconPack> iconpack : iconpacks.entrySet())
            {
                String value  = iconpack.getKey();
                String entry = iconpack.getValue().name;
                iconPackEntries.add(entry);
                iconPackValues.add(value);
            }

            String[] ipEntries = new String[iconPackEntries.size()];
            String[] ipValues = new String[iconPackValues.size()];

            iconPackEntries.toArray(ipEntries);
            iconPackValues.toArray(ipValues);
            listener = new ListPreferenceChangeListener(null, ipEntries, ipValues);

            ListPreference iconPackPref = (ListPreference) findPreference(ICON_PACK);
            currValue = getPreferenceScreen().getSharedPreferences().getString(ICON_PACK, DEFAULT_ICON_PACK);
            listener.setPrefSummary(iconPackPref, currValue);
            iconPackPref.setOnPreferenceChangeListener(listener);
            iconPackPref.setEntries(ipEntries);
            iconPackPref.setEntryValues(ipValues);

            // auto lock screen detection
            CheckBoxPreference autoDetectLockScreenAppPref = (CheckBoxPreference) findPreference(AUTO_DETECT_LOCKSCREEN_APP);
            String currentLockScreenApp = getCurrentLockScreenAppName(getActivity());
            String currentLockScreenAppString = getResources().getString(R.string.current_lock_screen_app, currentLockScreenApp);
            autoDetectLockScreenAppPref.setSummary(currentLockScreenAppString);
            autoDetectLockScreenAppPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((Boolean)newValue)
                        // mark that the user requested to auto detect
                        getPreferenceScreen().getSharedPreferences().edit().putBoolean("user_defined_auto_detect", true).commit();
                    return true;
                }
            });
        }

        public static String getAppName(Context context, String packageName)
        {
            Log.d(TAG, "getAppName(" + packageName + ")");
            String appName = context.getString(R.string.stock_lock_screen);

            if (!packageName.equals(STOCK_LOCKSCREEN_PACKAGENAME))
                try {
                    PackageManager pm = context.getPackageManager();
                    ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                    appName = pm.getApplicationLabel(ai).toString();
                } catch (NameNotFoundException e)
                {
                    appName = "Unknown";
                }

            return appName;
        }

        public static String getCurrentLockScreenAppName(Context context)
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String currentLSpackagename = prefs.getString(LOCKSCREEN_APP, STOCK_LOCKSCREEN_PACKAGENAME);

            return getAppName(context, currentLSpackagename);
        }
    }

    public static class PrefsContactFragment extends CardPreferenceFragment
	{
	    @Override
	    public void onCreate(Bundle savedInstanceState)
	    {
	        super.onCreate(savedInstanceState);

	        // Load the global_settings from an XML resource
	        addPreferencesFromResource(R.xml.contactpreferences);
            PreferenceGroup rateusPref = ((PreferenceGroup) findPreference("rate_us"));

            if (getActivity().getResources().getBoolean(R.bool.amazon))
            {
                rateusPref.removePreference(rateusPref.findPreference("rate_on_google"));
            }
            else
            {
                rateusPref.removePreference(rateusPref.findPreference("rate_on_amazon"));
            }
	    }
	}

	public static class PrefsAppSpecificFragment extends CardPreferenceFragment implements OnPreferenceChangeListener, ViewClickable
    {
        HashMap<String, Boolean> appsDisplayed = new HashMap<String, Boolean>();
        Handler handler = new Handler();

        @Override
	    public void onCreate(Bundle savedInstanceState)
	    {
	        super.onCreate(savedInstanceState);

	        // add app specific settings
			final PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

			// Specfic app list
			String specificApps = prefs.getString(APPS_SETTINGS, "");

            // add apps that already have app specific settings
            ArrayList<HashMap<String, Object>> apps = new ArrayList<HashMap<String, Object>>();

            // build a list of the current used apps
			for (String packageName : specificApps.split(","))
			{
				if (!packageName.equals(""))
				{
                    boolean checked = !prefs.getBoolean(packageName + "." + AppSettingsActivity.IGNORE_APP, false);

                    prefs.edit().putBoolean(packageName, checked).commit();

                    String appName = packageName;

                    // get package information
					try
					{
						ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(packageName, 0);
						appName = getActivity().getPackageManager().getApplicationLabel(ai).toString();
						if (appName == null) appName = packageName;

                        Drawable icon = getActivity().getPackageManager().getApplicationIcon(ai);
                        HashMap<String, Object> appData = new HashMap<String, Object>();

                        appData.put("package", packageName);
                        appData.put("title", appName);
                        appData.put("checked", new Boolean(checked));
                        appData.put("icon", icon);

                        apps.add(appData);
                    }
                    catch (NameNotFoundException e)
					{
                        // app is no longer installed, ignore it
					}
				}
			}

            // sort apps list - checked apps first
            Collections.sort(apps, new Comparator<HashMap<String, Object>>()
            {
                @Override
                public int compare(HashMap<String, Object> lhs, HashMap<String, Object> rhs)
                {
                    Boolean lchecked = (Boolean) lhs.get("checked");
                    Boolean rchecked = (Boolean) rhs.get("checked");
                    String ltitle = (String) lhs.get("title");
                    String rtitle = (String) rhs.get("title");

                    int bCompare = lchecked.compareTo(rchecked);
                    if (bCompare == 0)
                        return ltitle.compareTo(rtitle);
                    else
                        return bCompare * -1;
                }
            });

            // build global_settings list
            for(HashMap<String, Object> appData : apps)
            {
                String packageName = (String) appData.get("package");
                String title = (String) appData.get("title");
                Drawable icon = (Drawable) appData.get("icon");

                CheckBoxPreference intentPref = new CheckBoxPreference(getActivity());
                intentPref.setKey(packageName);
                intentPref.setOnPreferenceChangeListener(this);
                intentPref.setLayoutResource(R.layout.checkbox_preference_with_settings);
                intentPref.setSummary(packageName);
                intentPref.setTitle(title);
                intentPref.setIcon(icon);

                root.addPreference(intentPref);
                appsDisplayed.put(packageName, true);
            }

            Preference loading = new Preference(getActivity());
            loading.setTitle(R.string.loading_title);
            loading.setSummary(R.string.loading_summary);
            loading.setLayoutResource(R.layout.card_pref_layout);
            loading.setKey("loading");
            loading.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    loadAllApps();
                    return true;
                }
            });
            root.addPreference(loading);
            setPreferenceScreen(root);
	    }

        private void loadAllApps()
        {
            PreferenceScreen root = getPreferenceScreen();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            final PackageManager pm = getActivity().getPackageManager();
            //get a list of installed apps.
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            ArrayList<String[]> apps = new ArrayList<String[]>();

            // build a list of packages with app names
            for (ApplicationInfo ai : packages)
            {
                String packageName = ai.packageName;
                if (!appsDisplayed.containsKey(packageName))
                {
                    apps.add(new String[]{packageName, pm.getApplicationLabel(ai).toString()});
                }
            }

            // sort the list alphabetically
            Collections.sort(apps, new Comparator<String[]>()
            {
                @Override
                public int compare(String[] lhs, String[] rhs)
                {
                    String name1 = lhs[1];
                    String name2 = rhs[1];
                    return name1.compareTo(name2);
                }
            });

            // add pereference item for each app
            for (String[] app : apps)
            {
                String packageName = app[0];
                String appName = app[1];
                prefs.edit().remove(packageName).commit();
                final CheckBoxPreference intentPref = new CheckBoxPreference(getActivity());
                intentPref.setKey(packageName);
                intentPref.setDefaultValue(true);
                intentPref.setOnPreferenceChangeListener(this);
                intentPref.setLayoutResource(R.layout.checkbox_preference_with_settings);
                intentPref.setTitle(appName);
                intentPref.setSummary(packageName);
                intentPref.setKey(packageName);
                root.addPreference(intentPref);
            }

            root.removePreference(root.findPreference("loading"));
            // a-synchronically load icons
            final Iterator<String[]> iter = apps.iterator();
            if (iter.hasNext())
            {
                handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        String[] app = iter.next();
                        String packageName = app[0];

                        // load app icon
                        try
                        {
                            if (getActivity() != null)
                            {
                                ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(packageName, 0);
                                Preference pref = findPreference(packageName);
                                if (pref != null)
                                {
                                    pref.setIcon(getActivity().getPackageManager().getApplicationIcon(ai));
                                }
                            }
                        } catch (NameNotFoundException e)
                        {
                            // do nothing - shouldn't happen
                        }

                        if (iter.hasNext())
                            // load next app
                            handler.postDelayed(this, 0);
                    }
                }, 0);
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
            String packageName = preference.getKey();
            boolean ignoreApp = !((Boolean) newValue);

            // set the app to be ignored if unchecked
            preference.getSharedPreferences().edit().putBoolean(packageName + "." + AppSettingsActivity.IGNORE_APP, ignoreApp).commit();

            // add the app to app specific settings
            AppSettingsActivity.addAppToAppSpecificSettings(packageName, getActivity());

            return true;
        }

        @Override
        public void onClick(View v)
        {
            // this is a dirty hack to get the package name within the settings button
            String packageName = ((TextView)((View)v.getParent()).findViewById(android.R.id.summary)).getText().toString();

            // open persistent notification settings
            Intent runAppSpecificSettings = new Intent(getActivity(), AppSettingsActivity.class);
            runAppSpecificSettings.putExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME, packageName);
            startActivity(runAppSpecificSettings);
        }
    }

    public static interface ViewClickable
    {
        public void onClick(View v);
    }

	public static class PrefsPersistentNotificationsFragment extends CardPreferenceFragment implements ViewClickable
	{
        private View mNoPersistentNotificationsView;
        private ArrayList<String> apps = new ArrayList<String>();

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            super.onCreateView(inflater, container, savedInstanceState);
            View v = inflater.inflate(R.layout.appearance_settings_view, null);
            ViewGroup header = (ViewGroup) v.findViewById(R.id.preview_container);
            header.addView(inflater.inflate(R.layout.view_persistent_notifications_help, null));
            mNoPersistentNotificationsView = v.findViewById(R.id.no_persistent_notifications);

            if (apps.size() > 0)
                mNoPersistentNotificationsView.setVisibility(View.GONE);

            return v;
        }

        @Override
	    public void onCreate(Bundle savedInstanceState)
	    {
	        super.onCreate(savedInstanceState);

            // add app specific settings
			PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

			// Persistent notifications list
			NotificationsProvider ns = NotificationsService.getSharedInstance();
			if (ns != null)
    		{
    			apps = new ArrayList<String>();

    			// show first the enabled persistent apps
    			String packages = prefs.getString(PersistentNotificationSettingsActivity.PERSISTENT_APPS, "");
    			for (String packageName : packages.split(","))
    			{
    				if (!packageName.isEmpty())
    					apps.add(packageName);
    			}

    			// then add the current persistent notifications apps
    			Iterator<Entry<String, PersistentNotification>> it = ns.getPersistentNotifications().entrySet().iterator();
    			while (it.hasNext())
    			{
    				Entry<String, PersistentNotification> e = it.next();
    				String packageName = e.getKey();
    				if (!apps.contains(packageName))
    					apps.add(packageName);
    			}

    			// build global_settings list
    			for (final String packageName : apps)
    			{
    				final Context ctx = getActivity();
    				CheckBoxPreference intentPref = new CheckBoxPreference(getActivity());
					getPreferenceManager();
					intentPref.setKey(packageName + "." + PersistentNotificationSettingsActivity.SHOW_PERSISTENT_NOTIFICATION);
					intentPref.setLayoutResource(R.layout.checkbox_preference_with_settings);
					intentPref.setChecked(prefs.getBoolean(packageName + "." + PersistentNotificationSettingsActivity.SHOW_PERSISTENT_NOTIFICATION, false));
					intentPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
					{
						@Override
						public boolean onPreferenceChange(Preference preference, Object newValue)
						{
							prefs.edit().putBoolean(preference.getKey(), (Boolean)newValue).commit();
							if ((Boolean)newValue)
								PersistentNotificationSettingsActivity.addAppToPersistentNotifications(packageName, ctx);
							else
								PersistentNotificationSettingsActivity.removeAppFromPersistentNotifications(packageName, ctx);
							return true;
						}
					});

					// get package title
					try
					{
						ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(packageName, 0);
						String appName = getActivity().getPackageManager().getApplicationLabel(ai).toString();
						if (appName == null) appName = packageName;
						intentPref.setTitle(appName);
						intentPref.setIcon(getActivity().getPackageManager().getApplicationIcon(ai));
					} catch (NameNotFoundException e2)
					{
						intentPref.setTitle(packageName);
					}
					intentPref.setSummary(packageName);
			        root.addPreference(intentPref);
    			}
			}

	        setPreferenceScreen(root);
	    }

        @Override
        public void onClick(View v)
        {
            // this is a dirty hack to get the package name within the settings button
            String packageName = ((TextView)((View)v.getParent()).findViewById(android.R.id.summary)).getText().toString();

            // open persistent notification settings
            Intent runAppSpecificSettings = new Intent(getActivity(), PersistentNotificationSettingsActivity.class);
            runAppSpecificSettings.putExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME, packageName);
            startActivity(runAppSpecificSettings);
        }
    }

    public static Intent getNotificationsServiesIntent()
    {
        Intent intent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        }
        else
        {
            intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        }

        return intent;
    }

    public static boolean getBoolean(Context context, String packageName, String keyName, boolean defaultValue)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(packageName + "." + keyName, getBoolean(context, keyName, defaultValue));
    }

    public static boolean getBoolean(Context context, String keyName, boolean defaultValue)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(keyName, defaultValue);
    }
}
