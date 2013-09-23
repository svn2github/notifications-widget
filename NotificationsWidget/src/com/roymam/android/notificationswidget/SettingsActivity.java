package com.roymam.android.notificationswidget;

import android.app.AlertDialog;
import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.roymam.android.common.ListPreferenceChangeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	public static final String WIDGET_MODE = "widgetmode";  
	public static final String COLLAPSED_WIDGET_MODE = "collapsed";
    public static final String EXPANDED_WIDGET_MODE = "expanded";
	public static final String HOME_WIDGET_MODE = "home";
	public static final String SHOW_CLEAR_BUTTON = "showclearbutton";
	public static final String CLOCK_IS_CLICKABLE = "clockisclickable";
	public static final String BOLD_HOURS = "boldhours";
	public static final String BOLD_MINUTES = "boldminutes";
	public static final String CLOCK_BG_OPACITY = "clockbgopacity";
	public static final String TURNSCREENON = "turnscreenon";
	public static final String DISABLE_PROXIMITY = "disableproximity";
	public static final String DELAYED_SCREEON = "delayed_screenon";
	public static final String NOTIFICATIONS_ORDER = "order_notifications_by";
	public static final String CLOCK_STYLE = "clockstyle";
	public static final String NOTIFICATION_STYLE="notification_style";
	public static final String MAX_LINES="max_lines";
	public static final String SHOW_ACTIONBAR="show_actionbar";
	public static final String NOTIFICATION_BG_OPACITY="notification_bg_opacity";
	public static final String TEXT_COLOR = "notification_text_color";
	public static final String CLOCK_COLOR = "clock_text_color";
	public static final String CLOCK_BG_COLOR = "clock_bg_color";	
	public static final String CLOCK_ALARM_COLOR = "clock_alarm_color";	
	public static final String CLOCK_DATE_COLOR = "clock_date_color";
	public static final String NOTIFICATION_IS_CLICKABLE = "notification_is_clickable";	
	public static final String NOTIFICATION_ICON_IS_CLICKABLE = "notificationicon_is_clickable";
	public static final String TITLE_COLOR = "notification_title_color";	
	public static final String CONTENT_COLOR = "notification_content_color";
	public static final String NOTIFICATION_BG_COLOR = "notification_bg_color";
	public static final String LAST_WIDGET_MODE = "last_widget_mode";
	public static final String DISABLE_AUTO_SWITCH = "disable_auto_switch";
    public static final String NOTIFICATION_ICON_BG_COLOR = "notification_icon_bg_color";
    public static final String SHOW_PERSISTENT_NOTIFICATIONS = "show_persistent";
    public static final String WIDGET_PRESENT = "widget_present";
    public static final String CLEAR_ON_UNLOCK = "clearonunlock";
    public static final String SYNC_NOTIFICATIONS = "sync_notifications";
    public static final String SYNC_NOTIFICATIONS_DISABLED = "none";
    public static final String SYNC_NOTIFICATIONS_ONEWAY = "oneway";
    public static final String SYNC_NOTIFICATIONS_TWOWAY = "twoway";
    public static final String FORCE_CLEAR_ON_OPEN = "force_clear_on_open";
	public static final String COLLECT_ON_UNLOCK = "collectonunlock";
	public static final String CLEAR_APP_NOTIFICATIONS = "clear_app_notifications";
    public static final String CLOCK_SMALL = "small";
	public static final String CLOCK_MEDIUM = "medium";
	public static final String CLOCK_LARGE = "large";
	public static final String CLOCK_HIDDEN = "clockhidden";
	public static final String CLOCK_AUTO = "auto";
	public static final String APPS_SETTINGS = "specificapps";

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

    public static class PrefsGeneralFragment extends PreferenceFragment
	{
	    @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        super.onCreate(savedInstanceState);
	        
	        // Load the preferences from an XML resource
	        addPreferencesFromResource(R.xml.preferences);

		    // proximity sensor listener
	        Preference proxPref = findPreference(DISABLE_PROXIMITY);	        
	        proxPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
	        {
				@Override
				public boolean onPreferenceChange(final Preference preference,
						Object newValue) 
				{
					Boolean value = (Boolean)newValue;
					if (value == false && Build.MODEL.equals("Nexus 4"))
					{
						AlertDialog.Builder builder = new AlertDialog.Builder(preference.getContext());
						builder.setTitle(R.string.disableproximity_nexus4_warning_title)
						.setMessage(R.string.disableproximity_nexus4_warning)
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() 
			               {
			                   public void onClick(DialogInterface dialog, int id) 
			                   {
			                	   preference.getEditor().putBoolean(SettingsActivity.DISABLE_PROXIMITY, false).commit();
			                	   getActivity().recreate();
			                   }
			               })
						.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() 
			               {
			                   public void onClick(DialogInterface dialog, int id) 
			                   {
			                	   // do nothing
			                   }
			               })
						.show();
						return false;
					}
					return true;
				}	        	
	        });
	        
	        // notification order by 
	        ListPreferenceChangeListener listener = new ListPreferenceChangeListener(
	        		getResources().getStringArray(R.array.settings_orderby_entries),
	        		getResources().getStringArray(R.array.settings_orderby_values));
	        
	        Preference orderPref = findPreference(NOTIFICATIONS_ORDER);
	        String currValue = getPreferenceScreen().getSharedPreferences().getString(NOTIFICATIONS_ORDER, "time");	        
	        listener.setPrefSummary(orderPref, (String)currValue);
	        orderPref.setOnPreferenceChangeListener(listener);

            // set up notifications sync
            listener = new ListPreferenceChangeListener(
                    getResources().getStringArray(R.array.sync_notifications_entries),
                    getResources().getStringArray(R.array.sync_notifications_values));

            Preference pref = findPreference(SYNC_NOTIFICATIONS);
            String syncDefaultValue = SYNC_NOTIFICATIONS_ONEWAY;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                syncDefaultValue = SYNC_NOTIFICATIONS_TWOWAY;
            currValue = getPreferenceScreen().getSharedPreferences().getString(SYNC_NOTIFICATIONS, syncDefaultValue);
            listener.setPrefSummary(pref, (String)currValue);
            pref.setOnPreferenceChangeListener(listener);

            // disable clear_app_notifications on Android 4.3+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            {
                pref = findPreference(CLEAR_APP_NOTIFICATIONS);
                getPreferenceScreen().removePreference(pref);
                getPreferenceScreen().getSharedPreferences().edit().putBoolean(CLEAR_APP_NOTIFICATIONS, false).commit();
            }
        }
	}		

	public static class PrefsContactFragment extends PreferenceFragment 
	{
	    @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        super.onCreate(savedInstanceState);

	        // Load the preferences from an XML resource
	        addPreferencesFromResource(R.xml.contactpreferences);
	    }
	}

	public static class PrefsAppSpecificFragment extends PreferenceFragment
	{
        HashMap<String, Boolean> appsDisplayed = new HashMap<String, Boolean>();
        Handler handler = new Handler();

        @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        super.onCreate(savedInstanceState);

	        // add app specific settings
			final PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
			
			// Specfic app list 
			String specificApps = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(APPS_SETTINGS, "");

            // add apps that already have app specific settings
			for (String packageName : specificApps.split(",")) 
			{		
				if (!packageName.equals(""))
				{				
					PreferenceScreen intentPref = getPreferenceManager().createPreferenceScreen(getActivity());
					Intent runAppSpecificSettings = new Intent(getActivity(), AppSettingsActivity.class);
					runAppSpecificSettings.putExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME, packageName);
					intentPref.setIntent(runAppSpecificSettings);
                    intentPref.setSummary(packageName);

                    // get package title
					try 
					{
						ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(packageName, 0);
						String appName = getActivity().getPackageManager().getApplicationLabel(ai).toString();
						if (appName == null) appName = packageName;
						intentPref.setTitle(appName);
						intentPref.setIcon(getActivity().getPackageManager().getApplicationIcon(ai));
					} catch (NameNotFoundException e) 
					{
						intentPref.setTitle(packageName);
					}
			        root.addPreference(intentPref);
                    appsDisplayed.put(packageName, true);
				}				
			}

            // add the rest of the apps
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    loadAllApps();
                }
            },500);

            Preference loading = new Preference(getActivity());
            loading.setTitle(R.string.loading_title);
            loading.setSummary(R.string.loading_summary);
            loading.setKey("loading");
            root.addPreference(loading);
            /*
			DialogPreference howToUse = new DialogPreference(getActivity(), null) {};
			howToUse.setTitle(R.string.how_to_use);
			howToUse.setIcon(android.R.drawable.ic_menu_help);
			howToUse.setDialogTitle(R.string.app_specific_help);
			howToUse.setDialogMessage(R.string.app_specific_help_details);
			howToUse.setNegativeButtonText(null);
			root.addPreference(howToUse);*/
	        setPreferenceScreen(root);
	    }

        private void loadAllApps()
        {
            PreferenceScreen root = getPreferenceScreen();

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
                final PreferenceScreen intentPref = getPreferenceManager().createPreferenceScreen(getActivity());
                Intent runAppSpecificSettings = new Intent(getActivity(), AppSettingsActivity.class);
                runAppSpecificSettings.putExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME, packageName);
                intentPref.setIntent(runAppSpecificSettings);
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
    }

	public static class PrefsPersistentNotificationsFragment extends PreferenceFragment
	{
		@Override
	    public void onCreate(Bundle savedInstanceState)
	    {
	        super.onCreate(savedInstanceState);

	        // add app specific settings
			PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

			// Persistent notifications list
			NotificationsProvider ns = NotificationsService.getSharedInstance(getActivity());
			if (ns != null)
    		{
    			List<String> apps = new ArrayList<String>();

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

    			// build preferences list
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
			DialogPreference howToUse = new DialogPreference(getActivity(), null) {};
			howToUse.setTitle(R.string.how_to_use);
			howToUse.setIcon(android.R.drawable.ic_menu_help);
			howToUse.setDialogTitle(R.string.persistent_notifications_help);
			howToUse.setDialogMessage(R.string.persistent_notifications_help_details);
			howToUse.setNegativeButtonText(null);
			root.addPreference(howToUse);

	        setPreferenceScreen(root);
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
        if (NotificationsService.getSharedInstance(this) != null)
        {
            target.get(0).iconRes = android.R.drawable.presence_online;
            target.get(0).summaryRes = R.string.service_is_active;
        }
        else
        {
            target.get(0).iconRes = android.R.drawable.presence_offline;
            target.get(0).summaryRes = R.string.service_is_inactive;

            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            {
                intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            }
            else
            {
                intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            }
            target.get(0).intent = intent;
            target.get(0).fragment = null;
        }

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, NotificationsWidgetProvider.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(WIDGET_PRESENT, false) || widgetIds.length > 0)
        {
            target.get(1).iconRes = android.R.drawable.presence_online;
            target.get(1).summaryRes = R.string.widget_is_present;
        }
        else
        {
            target.get(1).iconRes = android.R.drawable.presence_offline;
            target.get(1).summaryRes = R.string.widget_is_not_present;
            target.get(1).fragment = "com.roymam.android.notificationswidget.SettingsActivity$HowToAddWidgetFragment";
        }

        // check widget status

        // setting last "about" button summary
        String versionString = "";
    	try 
    	{
    		versionString = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
	        target.get(target.size()-1).summary = getText(R.string.version) + " " + versionString;
	        		
		} catch (NameNotFoundException e) 
		{
		}               
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);        
    }

	@Override
	protected void onResume() 
	{
	    super.onResume();	    
	    PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() 
	{
	    super.onPause();
	    PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(final SharedPreferences prefs, String key) 
	{
        NotificationsProvider ns = NotificationsService.getSharedInstance(this);
		if ((key.equals(DISABLE_PROXIMITY) || key.equals(TURNSCREENON)) && ns != null)
        {
			if (!prefs.getBoolean(SettingsActivity.DISABLE_PROXIMITY, false) &&
				 prefs.getBoolean(SettingsActivity.TURNSCREENON, true))
			{
                ns.getNotificationEventListener().registerProximitySensor();
			}
			else 
			{
                ns.getNotificationEventListener().stopProximityMontior();
			}
        }
	}
	
	// openSettings is launched from the custom checkbox in persistent notifications settings 
	public void openSettings(View v)
	{		
		// this is a dirty hack to get the package name within the settings button
		String packageName = ((TextView)((View)v.getParent()).findViewById(android.R.id.summary)).getText().toString();
		
		// open persistent notification settings
		Intent runAppSpecificSettings = new Intent(this, PersistentNotificationSettingsActivity.class);
		runAppSpecificSettings.putExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME, packageName);
		startActivity(runAppSpecificSettings);
		
	}
}
