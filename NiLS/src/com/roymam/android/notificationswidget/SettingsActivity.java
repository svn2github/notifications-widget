package com.roymam.android.notificationswidget;

import android.app.AlertDialog;
import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
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
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.roymam.android.common.IconPackManager;
import com.roymam.android.common.ListPreferenceChangeListener;
import com.roymam.android.common.SwitchPrefsHeaderAdapter;
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

public class SettingsActivity extends PreferenceActivity
{
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
    public static final String TURNSCREENON_TIMEOUT = "turnscreenon_timeout";
    public static final int DEFAULT_TURNSCREENON_TIMEOUT = 10;

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

    // Floating Panel
    public static final String FP_ENABLED = "fp_enabled";
    public static final String FP_PACKAGE = "com.roymam.android.nilsplus";
    public static final boolean DEFAULT_FP_ENABLED = true;
    private static final int FIRST_INSTALLED_VERSION = 273;

    // settings strings
    public static final String LOCKSCREEN_APP = "lockscreenapp";
    public static final String LOCKSCREEN_APP_AUTO = "lockscreenapp_auto";
    public static final String PRIMARY_TEXT_COLOR = "primary_text_color";
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
    public static final boolean DEFAULT_HIDE_ON_CLICK = false;
    public static final String DEFAULT_LOCKSCREEN_APP_AUTO = "android";
    public static final String AUTO_LOCKSCREEN_APP = "auto";
    public static final String DEFAULT_LOCKSCREEN_APP = AUTO_LOCKSCREEN_APP;
    public static final String STOCK_LOCKSCREEN_PACKAGENAME = "com.android.keyguard";
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
    public static final String BLACKLIST_PACKAGENAMES = "com.android.dialer|com.google.android.dialer|ch.bitspin.timely|com.alarmclock.xtreme.free|com.achep.activedisplay";
    public static final String SHOW_WELCOME_WIZARD = "show_welcome_wizard";

    // privacy options
    public static final String NOTIFICATION_PRIVACY = "notification_privacy";
    public static final String PRIVACY_SHOW_TITLE_ONLY = "show_title";
    public static final String PRIVACY_SHOW_TICKER_ONLY = "show_ticker";
    public static final String PRIVACY_SHOW_APPNAME_ONLY = "show_appname";
    public static final String PRIVACY_SHOW_ALL = "none";
    public static final String DEFAULT_NOTIFICATION_PRIVACY = PRIVACY_SHOW_ALL;

    private List<Header> mHeaders = null;

    @Override
    protected boolean isValidFragment(String fragmentName)
    {
        // really dumb method that is required since api level 19, always return true,
        // there is no such a case that the fragment name won't be valid
        return true;
    }

    public static boolean shouldHideNotifications(Context context, String widgetMode)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean fpEnabled = prefs.getBoolean(SettingsActivity.FP_ENABLED, false);
        boolean overrideHideNotifications = prefs.getAll().containsKey(widgetMode + "." + SettingsActivity.HIDE_NOTIFICATIONS);
        boolean hideNotifications = prefs.getBoolean(widgetMode + "." + SettingsActivity.HIDE_NOTIFICATIONS, false);

        if ((fpEnabled && !overrideHideNotifications &&
                (widgetMode.equals(SettingsActivity.EXPANDED_WIDGET_MODE) || widgetMode.equals(SettingsActivity.COLLAPSED_WIDGET_MODE)))
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
        if (!prefs.getBoolean(SettingsActivity.TURNSCREENON, true))
            defaultWakeupMode = WAKEUP_NEVER;
        else
        if (prefs.getBoolean(SettingsActivity.DISABLE_PROXIMITY, false))
            defaultWakeupMode = WAKEUP_ALWAYS;
        else
        if (prefs.getBoolean(SettingsActivity.DELAYED_SCREEON, false))
            defaultWakeupMode = WAKEUP_UNCOVERED;
        else
            defaultWakeupMode = WAKEUP_NOT_COVERED;

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
            String currValue = SettingsActivity.getWakeupMode(getActivity(), null);
            wakeupPref.setDefaultValue(currValue);
            listener.setPrefSummary(wakeupPref, currValue);
            wakeupPref.setOnPreferenceChangeListener(listener);

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

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            invalidateHeaders();
    }

    @Override
    public void onBuildHeaders(List<Header> target)
	{
        loadHeadersFromResource(R.xml.preferences_headers, target);

        // check service status
        if (NotificationsService.getSharedInstance() != null)
        {
            //target.get(0).iconRes = android.R.drawable.presence_online;
            target.get(0).summaryRes = R.string.service_is_active;
        }
        else
        {
            //target.get(0).iconRes = android.R.drawable.presence_offline;
            target.get(0).summaryRes = R.string.service_is_inactive;

            Intent intent = getNotificationsServiesIntent();
            target.get(0).intent = intent;
            target.get(0).fragment = null;
        }

        // check widget status
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, NotificationsWidgetProvider.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(WIDGET_PRESENT, false) || widgetIds.length > 0)
        {
            //target.get(1).iconRes = android.R.drawable.presence_online;
            target.get(1).summaryRes = R.string.widget_is_present;
        }
        else
        {
            //target.get(1).iconRes = android.R.drawable.presence_offline;
            target.get(1).summaryRes = R.string.widget_is_not_present;
            target.get(1).fragment = "com.roymam.android.notificationswidget.SettingsActivity$HowToAddWidgetFragment";
        }

        // check NiLSPlus status
        if (isNiLSPlusInstalled())
        {
            // TODO: migrate license from nils floating panel
            /* Uninstall code:
            Uri packageUri = Uri.parse("package:com.roymam.android.nilsplus");
            Intent uninstallIntent =
                    new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
            startActivity(uninstallIntent);
            */
        }

        // set switch key and default value for Floating Panel option
        target.get(2).extras = new Bundle();
        target.get(2).extras.putInt(SwitchPrefsHeaderAdapter.HEADER_TYPE, SwitchPrefsHeaderAdapter.HEADER_TYPE_SWITCH);
        target.get(2).extras.putString(SwitchPrefsHeaderAdapter.HEADER_KEY, FP_ENABLED);
        target.get(2).extras.putBoolean(SwitchPrefsHeaderAdapter.HEADER_DEFAULT_VALUE, true);
        target.get(2).extras.putString(SwitchPrefsHeaderAdapter.SWITCH_ENABLED_MESSAGE, getString(R.string.nils_fp_enabled));
        target.get(2).extras.putString(SwitchPrefsHeaderAdapter.SWITCH_DISABLED_MESSAGE, getString(R.string.nils_fp_disabled));

        // setting last "about" button summary
        String versionString = "";
    	try 
    	{
    		versionString = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
	        target.get(target.size()-1).summary = getText(R.string.version) + " " + versionString;
	        		
		} catch (NameNotFoundException e) 
		{
		}

        mHeaders = target;
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

    private boolean isNiLSPlusInstalled()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try
        {
            PackageInfo info = getPackageManager().getPackageInfo(FP_PACKAGE, 0);
            if (info.versionCode < 32) return false;

            // if the user hasn't disabled fp yet, mark it as enabled
            if (!prefs.getAll().containsKey(SettingsActivity.FP_ENABLED))
                prefs.edit().putBoolean(SettingsActivity.FP_ENABLED, true).commit();
        } catch (PackageManager.NameNotFoundException e)
        {
            // if nils fp was uninstalled - removed this preference
            prefs.edit().remove(SettingsActivity.FP_ENABLED).commit();
            return false;
        }
        return true;
    }

    @Override
    public void setListAdapter(ListAdapter adapter)
    {
        int i, count;

        if (mHeaders == null)
        {
            mHeaders = new ArrayList<Header>();
            // When the saved state provides the list of headers,
            // onBuildHeaders is not called
            // so we build it from the adapter given, then use our own adapter
            count = adapter.getCount();
            for (i = 0; i < count; ++i)
                mHeaders.add((Header) adapter.getItem(i));
        }

        super.setListAdapter(new SwitchPrefsHeaderAdapter(this, mHeaders));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);

        if (NotificationsService.getSharedInstance() == null)
        {
            finish();
            startActivity(new Intent(getApplicationContext(), StartServiceActivity.class));
        }
        else
        {
            showWhatsNew();
        }
    }

    private void showWhatsNew()
    {
        CharSequence whatsnewString = "";

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int installedVer = prefs.getInt("installed_version", FIRST_INSTALLED_VERSION);
        int currentVer;

        // get current version
        try
        {
            currentVer = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e)
        {
            // shoudln't happen
            currentVer = installedVer;
        }

        // build "what's new" string
        for (int i=currentVer; i>installedVer; i--)
        {
            int id = getResources().getIdentifier("v"+i,"string", getPackageName());
            if (id > 0)
                whatsnewString = TextUtils.concat(whatsnewString, Html.fromHtml(getString(id)), "\n");
        }

        if (!whatsnewString.equals(""))
        {
            final int finalCurrentVer = currentVer;
            AlertDialog.Builder b = new AlertDialog.Builder( this );
            b.setTitle(R.string.whatsnew)
             .setMessage(whatsnewString)
             .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int which) {
                     // save the updated version code so this dialog won't appear again
                     prefs.edit().putInt("installed_version", finalCurrentVer).commit();
                 }
             })
             .setIcon(R.drawable.appicon)
             .show();
        }
    }

    @Override
	protected void onResume() 
	{
	    super.onResume();	    
	}

	@Override
	protected void onPause() 
	{
	    super.onPause();
	}
}
