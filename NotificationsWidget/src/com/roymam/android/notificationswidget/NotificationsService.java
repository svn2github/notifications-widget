package com.roymam.android.notificationswidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class NotificationsService implements NotificationsProvider
{
    private static NotificationsProvider instance;
    private NotificationsProvider source;
    private Context context;

    private NotificationsService(Context context, NotificationsProvider source)
    {
        this.source = source;
        this.context = context;
        setNotificationEventListener(new NotificationAdapter(context));
    }

    public static NotificationsProvider getSharedInstance(Context context)
    {
        if (instance == null)
        {
            if (NiLSAccessibilityService.getSharedInstance() != null)
                instance = new NotificationsService(context, NiLSAccessibilityService.getSharedInstance());
        }
        return instance;
    }

    @Override
    public List<NotificationData> getNotifications()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        List<NotificationData> notifications = source.getNotifications();

        String sortBy = prefs.getString(SettingsActivity.NOTIFICATIONS_ORDER, "time");
        sortNotificationsList(notifications, sortBy);

        return notifications;
    }

    private void sortNotificationsList(List<NotificationData> notifications, String sortBy)
    {
        if (sortBy.equals("priority"))
        {
            // sort by priority
            Collections.sort(notifications, new Comparator<NotificationData>()
            {
                @Override
                public int compare(NotificationData n1, NotificationData n2)
                {
                    if (n1.priority < n2.priority)
                        return 1;
                    if (n1.priority > n2.priority)
                        return -1;
                    // if we reached here, the priorities are equal - sory by time
                    if (n1.received < n2.received)
                        return 1;
                    if (n1.received > n2.received)
                        return -1;
                    return 0;
                }
            });
        }
        else if (sortBy.equals("timeasc"))
        {
            // sort by time
            Collections.sort(notifications, new Comparator<NotificationData>()
            {
                @Override
                public int compare(NotificationData n1, NotificationData n2)
                {
                    if (n1.received > n2.received)
                        return 1;
                    if (n1.received < n2.received)
                        return -1;
                    return 0;
                }
            });
        }
        else //if (sortBy.equals("time"))
        {
            // sort by time
            Collections.sort(notifications, new Comparator<NotificationData>()
            {
                @Override
                public int compare(NotificationData n1, NotificationData n2)
                {
                    if (n1.received < n2.received)
                        return 1;
                    if (n1.received > n2.received)
                        return -1;
                    return 0;
                }
            });
        }
    }

    @Override
    public HashMap<String, PersistentNotification> getPersistentNotifications()
    {
        return source.getPersistentNotifications();
    }

    @Override
    public void clearAllNotifications()
    {
        source.clearAllNotifications();
    }

    @Override
    public void clearNotification(int notificationId)
    {
        source.clearNotification(notificationId);
    }

    @Override
    public void setNotificationEventListener(NotificationEventListener listener)
    {
        source.setNotificationEventListener(listener);
    }

    @Override
    public void clearNotificationsForApps(String[] apps)
    {
        source.clearNotificationsForApps(apps);
    }
}

/* Other stuff that need to be integrated

else if (intent.getAction().equals("com.teslacoilsw.widgetlocker.intent.LOCKED"))
    	{
    		NotificationsService ns = NotificationsService.getSharedInstance(context);
    		if (ns!=null)
    		{
    			ns.setDeviceIsLocked();
    			ns.setWidgetLockerEnabled(true);
    		}
    	}
    	else if (intent.getAction().equals("com.teslacoilsw.widgetlocker.intent.UNLOCKED"))
    	{
    		NotificationsService ns = NotificationsService.getSharedInstance(context);
    		if (ns != null)
    		{
    			ns.setDeviceIsUnlocked();
				ns.setSelectedIndex(-1);
				ns.setWidgetLockerEnabled(true);
    		}
    	}
    	else if (intent.getAction().equals("android.intent.action.SCREEN_ON"))
    	{
    		NotificationsService ns = NotificationsService.getSharedInstance(context);
    		if (ns != null)
    		{
    			// if the screen is on, so the device is currently locked (until USER_PRESENT will trigger)
    			ns.setDeviceIsLocked();
    		}
    	}
else if (intent.getAction().equals("com.teslacoilsw.widgetlocker.intent.DISABLED"))
        {
        NotificationsService ns = NotificationsService.getSharedInstance(context);
if (ns != null)
        {
        ns.setWidgetLockerEnabled(false);
}
        }
        else if (intent.getAction().equals("com.teslacoilsw.widgetlocker.intent.ENABLED"))
        {
        NotificationsService ns = NotificationsService.getSharedInstance(context);
if (ns != null)
        {
        ns.setWidgetLockerEnabled(true);
}
        }
        else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT))
        {
        NotificationsService ns = NotificationsService.getSharedInstance(context);
if (ns != null)
        {
        if (!ns.isWidgetLockerEnabled())
        {
        ns.setDeviceIsUnlocked();
ns.setSelectedIndex(-1);
}
        }
        }

private void updateClearOnUnlockState()
{
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    //NotificationsService ns = NotificationsService.getSharedInstance(context);

    // check if the screen has been turned on to start collecting
    if (!prefs.getBoolean(SettingsActivity.COLLECT_ON_UNLOCK, true) && ns != null)
    {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        boolean isScreenOn = powerManager.isScreenOn();
        if (!isScreenOn && ns.isDeviceIsUnlocked())
        {
            ns.setDeviceIsLocked();
        }
    }

}

****/
/*
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.roymam.android.notificationswidget.NotificationData.Action;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationsService extends AccessibilityService
{
	private static NotificationsService sSharedInstance;
	private List<NotificationData> notifications;
	private boolean deviceCovered = false;
	private boolean newNotificationsAvailable = false;
	private boolean widgetLockerEnabled = false;


	private int 	selectedIndex = -1;

    private PendingIntent runningAppsPendingIntent = null;
    private boolean overrideLocked = false;
    private int notificationId = 0;

    public static NotificationsService getSharedInstance() { return sSharedInstance; }


    @Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

    @Override
    public void onDestroy()
    {
        if (receiver != null) unregisterReceiver(receiver);
        if (sensorListener != null) stopProximityMontior();
        stopMonitorApps();
        removeFromForeground();
    }

    @Override
	protected void onServiceConnected() 
	{
		super.onServiceConnected();
	
		// first run preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean firstRun = prefs.getBoolean("com.roymam.android.notificationswidget.firstrun", true);
		prefs.edit().putBoolean("com.roymam.android.notificationswidget.firstrun", false).commit();
		if (firstRun)
		{
			if (Build.MODEL.equals("Nexus 4"))
			{
				prefs.edit().putBoolean(SettingsActivity.DISABLE_PROXIMITY, true).commit();
			}
		}
		
		sSharedInstance = this;		
		AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
		// check if "Clear Notifications Monitor" feature enabled, if so - monitor view clicks
		if (prefs.getBoolean(SettingsActivity.CLEAR_ON_CLEAR, false))
		{

		    info.eventTypes |= AccessibilityEvent.TYPE_VIEW_CLICKED;
		}

        if (prefs.getBoolean(SettingsActivity.CLEAR_APP_NOTIFICATIONS, true))
        {
            info.eventTypes |= AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        }
        if (prefs.getBoolean(SettingsActivity.MONITOR_NOTIFICATIONS_BAR, false))
        {
            info.eventTypes |= AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        }

		info.notificationTimeout = 100;
	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
	    setServiceInfo(info);

	    notifications = new ArrayList<NotificationData>();
	    persistentNotifications = new HashMap<String, PersistentNotification>();
	    
	    // register proximity change sensor
		registerProximitySensor();

        // keep app on foreground if requested
		keepOnForeground();
		
		// detect expanded notification id's 
		detectNotificationIds();

        // start monitor apps timer
        startMonitorApps();

        // register receivers
        registerReceivers();
	}

    public final static String FN_DISMISS_NOTIFICATIONS = "robj.floating.notifications.dismissed";
    public final static String DISMISS_NOTIFICATIONS = "com.roymam.android.nils.remove_notification";
    public final static String OPEN_NOTIFICATION = "com.roymam.android.nils.open_notification";
    private BroadcastReceiver receiver = null;

    private void registerReceivers()
    {
        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent.getAction() != null &&
                        (intent.getAction().equals(DISMISS_NOTIFICATIONS)) ||
                         intent.getAction().equals(FN_DISMISS_NOTIFICATIONS))
                {
                    String packageName = intent.getStringExtra("package");
                    int id = intent.getIntExtra("id",-1);
                    if (id > -1)
                    {
                        Log.d("NiLS", "remove notification #" + id);
                        removeNotificationById(id);
                    }
                    else
                        clearNotificationsForApps(new String[]{packageName});
                }
                else if (intent.getAction().equals(RESEND_ALL_NOTIFICATIONS))
                {
                    for(NotificationData nd : notifications)
                    {
                        notifyNotificationAdd(nd);
                    }
                }
                else if (intent.getAction().equals(OPEN_NOTIFICATION))
                {
                    int id = intent.getIntExtra("id",-1);
                    if (id > -1)
                    {
                        Log.d("NiLS", "open notification #" + id);
                        launchNotificationById(id);
                    }
                }
            }
        };
        registerReceiver(receiver,new IntentFilter(DISMISS_NOTIFICATIONS));
        registerReceiver(receiver,new IntentFilter(OPEN_NOTIFICATION));
        registerReceiver(receiver,new IntentFilter(FN_DISMISS_NOTIFICATIONS));
        registerReceiver(receiver,new IntentFilter(RESEND_ALL_NOTIFICATIONS));
    }



    private void removeNotificationById(int id)
    {
        for(int i=0; i<notifications.size(); i++)
            if (notifications.get(i).id == id)
            {
                removeNotification(i);
                break;
            }
    }

    private void startMonitorApps()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean(SettingsActivity.AUTO_KILL_PERSISTENT, false))
        {
            // register process monitoring
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent runningAppsService =new Intent(this, NotificationsWidgetService.class);
            runningAppsService.putExtra(NotificationsWidgetService.ACTION, NotificationsWidgetService.ACTION_MONITOR_APPS);
            runningAppsPendingIntent = PendingIntent.getService(this,0, runningAppsService, PendingIntent.FLAG_UPDATE_CURRENT);
            int interval = Integer.parseInt(prefs.getString(SettingsActivity.MONITOR_APPS_INTERVAL, "5"));
            am.setRepeating(AlarmManager.RTC, 0, interval*1000, runningAppsPendingIntent);
        };
    }

    private void stopMonitorApps()
    {
        if (runningAppsPendingIntent != null)
        {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            am.cancel(runningAppsPendingIntent);
            runningAppsPendingIntent = null;
        }
    }

	public void handleNotification(Notification n, String packageName)
	{
		if (n != null)
		{
			// handle only dismissable notifications
			if (!((n.flags & Notification.FLAG_NO_CLEAR) == Notification.FLAG_NO_CLEAR) &&
				!((n.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT))
			{
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
				boolean isScreenOn = false;
				if (!sharedPref.getBoolean(SettingsActivity.COLLECT_ON_UNLOCK, true))
				{
					PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
					isScreenOn = powerManager.isScreenOn();
				}

				// collect only on two sceneries: 1. the screen is off. 2. the screen is on but the device is unlocked.  
				if (!isScreenOn || !isDeviceIsUnlocked())
				{			
					boolean ignoreApp = sharedPref.getBoolean(packageName+"."+AppSettingsActivity.IGNORE_APP, false);
					if (!ignoreApp)
					{
						// set new notification flag (for delayed auto screen on feature)
						newNotificationsAvailable = true;																
						
						// build notification data object
						NotificationData nd = new NotificationData();
						
						// extract notification & app icons
						Resources res;
						PackageInfo info;
						ApplicationInfo ai;
						try 
						{
							res = getPackageManager().getResourcesForApplication(packageName);
							info = getPackageManager().getPackageInfo(packageName,0);
							ai = getPackageManager().getApplicationInfo(packageName,0);
						}
						catch(NameNotFoundException e)
						{
							info = null;
							res = null;
							ai = null;
						}

						if (res != null && info != null)
						{
							nd.appicon = BitmapFactory.decodeResource(res, n.icon);
							nd.icon = BitmapFactory.decodeResource(res, info.applicationInfo.icon);							
							if (nd.appicon == null)
							{
								nd.appicon = nd.icon;
							}							
						}						
						if (n.largeIcon != null)
						{
							nd.icon = n.largeIcon;
						}
														
						// get time of the event
						if (n.when != 0)
							nd.received = n.when;
						else
							nd.received = System.currentTimeMillis();
						
						nd.action = n.contentIntent;
						nd.count = 1;
						nd.packageName = packageName;
						
						// if possible - try to extract actions from expanded notification
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) 
						{
							nd.actions = getActionsFromNotification(n, packageName);
						}
						
						// extract expanded text
						nd.text = null;
						nd.title = null;
						if (sharedPref.getBoolean(nd.packageName+"."+AppSettingsActivity.USE_EXPANDED_TEXT, sharedPref.getBoolean(AppSettingsActivity.USE_EXPANDED_TEXT, true)))
						{							
							getExpandedText(n,nd, sharedPref.getString(nd.packageName + "." + AppSettingsActivity.MULTIPLE_EVENTS_HANDLING, "all"));
							// replace text with content if no text
							if (nd.text == null || nd.text.equals("") &&
								nd.content != null && !nd.content.equals(""))
							{
								nd.text = nd.content;
								nd.content = null;
							}
                            // keep only text if it's duplicated
                            if (nd.text != null && nd.content != null && nd.text.toString().equals(nd.content.toString()))
                            {
                                nd.content = null;
                            }
						}
						
						// use default notification text & title - if no info found on expanded notification
						if (nd.text == null)
						{							
							nd.text = n.tickerText;							
						}
						if (nd.title == null)
						{
							if (info != null)
								nd.title = getPackageManager().getApplicationLabel(ai);
							else
								nd.title = packageName;
						}
						
						// if still no text ignore it
						if (nd.title == null && nd.text == null)
							return;
						else if ((nd.title.equals(packageName) || nd.title.equals(getPackageManager().getApplicationLabel(ai))) &&
                                 (nd.text == null || nd.text.equals("")))
                        {
                            if (sharedPref.getBoolean(packageName + "." + AppSettingsActivity.IGNORE_EMPTY_NOTIFICATIONS,false))
                                return;
                        }

                        // turn the screen on
						turnScreenOn();

                        nd.id = ++notificationId;


						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
						{
							nd.priority = getPriority(n);
						}
						else
						{
							nd.priority = 0;
						}
						int apppriority = Integer.parseInt(sharedPref.getString(nd.packageName+"."+AppSettingsActivity.APP_PRIORITY, "-9"));						
						if (apppriority != -9) nd.priority = apppriority;


                        notifyNotificationAdd(nd);

                        // update widgets
						AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
						ComponentName widgetComponent = new ComponentName(this, NotificationsWidgetProvider.class);
						int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);

                        for (int widgetId : widgetIds) {
                            AppWidgetManager.getInstance(this).notifyAppWidgetViewDataChanged(widgetId, R.id.notificationsListView);
                        }
						sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));
					}
				}
			}
			// handle persistent notifications
			else 
			{
				// keep only the last persistent notification for the app
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
				boolean useExpanded = (sharedPref.getBoolean(packageName + "." + AppSettingsActivity.USE_EXPANDED_TEXT, 
									sharedPref.getBoolean(AppSettingsActivity.USE_EXPANDED_TEXT, true)));

				PersistentNotification pn = new PersistentNotification();
				if (useExpanded && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
				{
					pn.expandedContent = this.getExpandedContent(n);
				}
				pn.content = n.contentView;
				Time now = new Time();
				now.setToNow();
				pn.recieved = now.toMillis(true);
				pn.packageName = packageName;
				pn.contentIntent = n.contentIntent;
				persistentNotifications.put(packageName, pn);
				updateWidget(false);
			}
		}
	}


		





	


	@SuppressWarnings("unused")
	private int recursiveFindFirstImage(ViewGroup v)
	{
		for(int i=0; i<v.getChildCount(); i++)
		{
			View child = v.getChildAt(i);
			if (child instanceof ViewGroup)
				recursiveFindFirstImage((ViewGroup)child);			
			if (child instanceof ImageView)
			{
				Drawable d = ((ImageView)child).getDrawable();
				if (d!=null)
				{
					return child.getId();
				}
			}			
		}
		return -1;
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) 
	{
		if (event != null)
		{
			// if it's notification
			if (event.getClassName().equals(android.app.Notification.class.getName()))
			{
				Notification n = (Notification)event.getParcelableData();
				handleNotification(n, event.getPackageName().toString());
			}
			else if (event.getPackageName()!= null && event.getClassName() != null && event.getContentDescription() != null)
				{

                }
            else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            {

            }
            else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
            {

            }
		}
	}



    private void recursivePrintNodes(AccessibilityNodeInfo node, String padding)
    {
        Rect bounds = new Rect();
        node.getBoundsInParent(bounds);

        Log.d("NiLS", "NODE"+padding+"text:"+node.getText()+" desc:"+node.getContentDescription()+" bounds:"+bounds.width()+","+bounds.height()+" childs:"+node.getChildCount());
        for(int i=0;i<node.getChildCount();i++)
        {
            recursivePrintNodes(node.getChild(i), padding+"  ");
        }
    }
	
	public void keepOnForeground()
	{
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.KEEP_ON_FOREGROUND, false))
		{
			 Notification noti = new NotificationCompat.Builder(this)
	         .setContentTitle("Notifications Widget")
	         .setContentText("Notifications Widget Service is Active")
	         .setSmallIcon(R.drawable.appicon)
	         .setContentIntent(
	        		 PendingIntent.getActivity(this, 0, 
	        				 new Intent(this, MainActivity.class), 
	        				 PendingIntent.FLAG_UPDATE_CURRENT))
	         .build();
			noti.flags|=Notification.FLAG_NO_CLEAR;
			this.startForeground(0, noti);
		}
	}
	
	public void removeFromForeground()
	{
		this.stopForeground(true);
	}

	public int getNotificationsCount()
	{
		return notifications.size();
	}
	
	public NotificationData getNotification(int i)
	{
		if (i>=0 && i<notifications.size())
			return notifications.get(i);
		else
			return null;
	}
	
	public void removeNotification(int i)
	{
		if (i>=0 && i<notifications.size())
		{
			if (!notifications.get(i).pinned)
            {
                notifyNotificationRemove(notifications.get(i));
                notifications.remove(i);
                if (selectedIndex > i) selectedIndex--;
                else if (selectedIndex ==i) selectedIndex=-1;
            }
		}
	}





	public void setDeviceIsUnlocked()
	{
        overrideLocked = false;
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.CLEAR_ON_UNLOCK, false))
		{
			clearAllNotifications();
		}
	}

	@Override
	public void onInterrupt() 
	{
	}
	
	public boolean onUnbind(Intent intent) 
	{
	    sSharedInstance = null;
	    stopProximityMontior();
        stopMonitorApps();
        return super.onUnbind(intent);
	}
	
	public void togglePinNotification(int pos)
	{
		if (pos >=0 && pos < notifications.size())
		{
			NotificationData n = notifications.get(pos);
			if (!n.pinned)
			{				
				n.pinned = true;
			}
			else
			{
				n.pinned = false;
			}
            updateWidget(true);
		}
	}


	public HashMap<String, PersistentNotification> getPersistentNotifications() 
	{
		return persistentNotifications;
	}

	public boolean isWidgetLockerEnabled() 
	{
		return widgetLockerEnabled;
	}

	public void setWidgetLockerEnabled(boolean widgetLockerEnabled) 
	{
		this.widgetLockerEnabled = widgetLockerEnabled;
	}

	public int getSelectedIndex() 
	{
		return selectedIndex;
	}

	public void setSelectedIndex(int selectedIndex) 
	{
		this.selectedIndex = selectedIndex;
	}

	public boolean isDeviceIsUnlocked()
    {
        if (overrideLocked) return false;

        KeyguardManager kgMgr = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean onLockScreen = kgMgr.inKeyguardRestrictedInputMode();

        return !onLockScreen;
	}



    public void setDeviceIsLocked()
    {
        overrideLocked = true;
    }
}
*/