package com.roymam.android.notificationswidget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.TextView;

import com.roymam.android.common.ListPreferenceChangeListener;
import com.roymam.android.notificationswidget.WizardActivity.AboutDialogFragment;

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
	public static final String KEEP_ON_FOREGROUND = "keep_on_foreground";
	public static final String CLOCK_STYLE = "clockstyle";
	public static final String CLEAR_BUTTON_MODE = "clearbuttonmode";
	public static final String SHOW_EDIT_BUTTON= "showeditbutton";
	public static final String NOTIFICATION_STYLE="notification_style";
	public static final String MAX_LINES="max_lines";
	public static final String NOTIFICATION_BG_OPACITY="notification_bg_opacity";
	public static final String TEXT_COLOR = "notification_text_color";
	public static final String TIME_COLOR = "notification_time_color";
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
	public static String CLEAR_ON_UNLOCK = "clearonunlock";
	public static String CLEAR_ON_LOCK = "clearonlock";
	public static String COLLECT_ON_UNLOCK = "collectonunlock";
	public static String CLEAR_ON_CLEAR = "clearonclear";
	public static String CLOCK_SMALL = "small";
	public static String CLOCK_MEDIUM = "medium";
	public static String CLOCK_LARGE = "large";
	public static String CLOCK_HIDDEN = "clockhidden";
	public static String CLOCK_AUTO = "auto";
	public static String APPS_SETTINGS = "specificapps";
	
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
	    }	    
	}		
	
	public static class PrefsAdvancedFragment extends PreferenceFragment 
	{
	    @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        super.onCreate(savedInstanceState);

	        // Load the preferences from an XML resource
	        addPreferencesFromResource(R.xml.advancedpreferences);
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
		@Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        super.onCreate(savedInstanceState);

	        // add app specific settings
			PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
			
			// Specfic app list 
			String specificApps = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(APPS_SETTINGS, "");

			for (String packageName : specificApps.split(",")) 
			{		
				if (!packageName.equals(""))
				{				
					PreferenceScreen intentPref = getPreferenceManager().createPreferenceScreen(getActivity());
					Intent runAppSpecificSettings = new Intent(getActivity(), AppSettingsActivity.class);
					runAppSpecificSettings.putExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME, packageName);
					intentPref.setIntent(runAppSpecificSettings);
					
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
				}				
			}
			DialogPreference howToUse = new DialogPreference(getActivity(), null) {};
			howToUse.setTitle(R.string.how_to_use);
			howToUse.setIcon(android.R.drawable.ic_menu_help);
			howToUse.setDialogTitle(R.string.app_specific_help);
			howToUse.setDialogMessage(R.string.app_specific_help_details);
			howToUse.setNegativeButtonText(null);
			root.addPreference(howToUse);
	        setPreferenceScreen(root);
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
			NotificationsService ns = NotificationsService.getSharedInstance();
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
    public void onBuildHeaders(List<Header> target) 
	{
        loadHeadersFromResource(R.xml.preferences_headers, target);
        
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
	
	/*private void showAbout() 
	{	
		AboutDialogFragment dialog = new WizardActivity.AboutDialogFragment();
		//TBD - find a way to show about dialog
		dialog.show(getFragmentManager(), "AboutDialogFragment");	
	}*/
	
	/*
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		final MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.settings_menu, menu);
	    
	    OnMenuItemClickListener menuListener = new OnMenuItemClickListener()
	    {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) 
			{
				if (arg0.getItemId() == R.id.menu_about)
				{
					showAbout();
					return true;
				}
				return false;
			}
	    };
	    for(int i=0;i<menu.size();i++)
	    	menu.getItem(i).setOnMenuItemClickListener(menuListener);
	    
	    return super.onCreateOptionsMenu(menu);
	}*/
	
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
		NotificationsService ns = NotificationsService.getSharedInstance();
		if ((key.equals(DISABLE_PROXIMITY) || key.equals(TURNSCREENON)) && ns != null) 
        {
			if (!prefs.getBoolean(SettingsActivity.DISABLE_PROXIMITY, false) &&
				 prefs.getBoolean(SettingsActivity.TURNSCREENON, true))
			{
				ns.registerProximitySensor();
			}
			else 
			{
				ns.stopProximityMontior();
			}
        }
		else if (key.equals(KEEP_ON_FOREGROUND))
		{
			if (prefs.getBoolean(SettingsActivity.KEEP_ON_FOREGROUND, false))
			{
				NotificationsService.getSharedInstance().keepOnForeground();
			}
			else
			{
				NotificationsService.getSharedInstance().removeFromForeground();
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
