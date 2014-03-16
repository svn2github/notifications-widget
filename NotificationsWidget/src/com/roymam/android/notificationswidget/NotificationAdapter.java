package com.roymam.android.notificationswidget;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;

public class NotificationAdapter implements NotificationEventListener
{
    private Context context = null;
    private boolean newNotificationsAvailable = false;
    private Boolean deviceCovered = null;
    private boolean proximityRegistered = false;

    // extensions API
    public static final String ADD_NOTIFICATION = "com.roymam.android.nils.add_notification";
    public static final String UPDATE_NOTIFICATION = "com.roymam.android.nils.update_notification";
    public static final String REMOVE_NOTIFICATION = "com.roymam.android.nils.remove_notification";
    private Handler mHandler = null;
    private PowerManager.WakeLock mWakeLock = null;

    public NotificationAdapter(Context context)
    {
        this.context = context;
    }

    @Override
    public void onNotificationAdded(NotificationData nd, final boolean wake)
    {
        // turn screen on (if needed)
        if (wake)
        {
            handleWakeupMode(nd.packageName);
        }
        notifyNotificationAdd(nd);
    }

    private void handleWakeupMode(final String packageName)
    {
        String wakeupMode = SettingsActivity.getWakeupMode(context, packageName);

        if (wakeupMode.equals(SettingsActivity.WAKEUP_ALWAYS))
            turnScreenOn(packageName);
        else
        {
            registerProximitySensor(packageName);

            if (mHandler == null) mHandler = new Handler();

            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    if (deviceCovered == null || !deviceCovered)
                    {
                        // the device is not covered or we don't know yet so we turn the screen on
                        turnScreenOn(packageName);
                        stopProximityMontior();
                    }
                    else  // the device is covered
                    {
                        String wakeupMode = SettingsActivity.getWakeupMode(context, packageName);
                        // stop proximity monitoring if delayed screen on is inactive, otherwise it will stopped later when uncovered
                        if (!wakeupMode.equals(SettingsActivity.WAKEUP_UNCOVERED))
                            stopProximityMontior();
                    }
                }
            }, 1000);
        }
    }

    @Override
    public void onNotificationUpdated(NotificationData nd, boolean changed)
    {
        // turn screen on (if needed)
        if (changed)
            handleWakeupMode(nd.packageName );

        notifyNotificationUpdated(nd);
    }

    private void notifyNotificationAdd(NotificationData nd)
    {
        Log.d("Nils", "notification add uid:" + nd.uid);

        // add the package to the app specific settings page
        AppSettingsActivity.addAppToAppSpecificSettings(nd.packageName, context);

        // send notification to nilsplus
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(SettingsActivity.FP_ENABLED, SettingsActivity.DEFAULT_FP_ENABLED))
        {
            Intent npsIntent = new Intent();
            npsIntent.setComponent(new ComponentName("com.roymam.android.nilsplus", "com.roymam.android.nilsplus.NPService"));
            npsIntent.setAction(ADD_NOTIFICATION);
            npsIntent.putExtra("title", nd.title);
            npsIntent.putExtra("text", nd.text);
            npsIntent.putExtra("time", nd.received);
            npsIntent.putExtra("package", nd.packageName);
            npsIntent.putExtra("id", nd.id);
            npsIntent.putExtra("uid", nd.uid);
            npsIntent.putExtra("action", nd.action);
            npsIntent.putExtra("icon", nd.icon);
            npsIntent.putExtra("appicon", nd.appicon);

            if (nd.actions != null)
            {
                for (int i=0; i<nd.actions.length; i++)
                {
                    npsIntent.putExtra("action"+i+"intent", nd.actions[i].actionIntent);
                    npsIntent.putExtra("action"+i+"icon", nd.actions[i].drawable);
                    npsIntent.putExtra("action"+i+"label", nd.actions[i].title);
                }
                npsIntent.putExtra("actions",nd.actions.length);
            }
            else
                npsIntent.putExtra("actions",0);

            context.startService(npsIntent);
        }
    }

    private void notifyNotificationUpdated(NotificationData nd)
    {
        Log.d("Nils", "notification update #" + nd.uid);

        // send notification to nilsplus
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(SettingsActivity.FP_ENABLED, SettingsActivity.DEFAULT_FP_ENABLED))
        {
            Intent npsIntent = new Intent();
            npsIntent.setComponent(new ComponentName("com.roymam.android.nilsplus", "com.roymam.android.nilsplus.NPService"));
            npsIntent.setAction(UPDATE_NOTIFICATION);
            npsIntent.putExtra("title", nd.title);
            npsIntent.putExtra("text", nd.text);
            npsIntent.putExtra("time", nd.received);
            npsIntent.putExtra("package", nd.packageName);
            npsIntent.putExtra("id", nd.id);
            npsIntent.putExtra("uid", nd.uid);
            npsIntent.putExtra("action", nd.action);
            npsIntent.putExtra("icon", nd.icon);
            npsIntent.putExtra("appicon", nd.appicon);

            if (nd.actions != null)
            {
                for (int i=0; i<nd.actions.length; i++)
                {
                    npsIntent.putExtra("action"+i+"intent", nd.actions[i].actionIntent);
                    npsIntent.putExtra("action"+i+"icon", nd.actions[i].drawable);
                    npsIntent.putExtra("action"+i+"label", nd.actions[i].title);
                }
                npsIntent.putExtra("actions",nd.actions.length);
            }
            else
            {
                npsIntent.putExtra("actions",0);
            }

            context.startService(npsIntent);
        }
    }

    private void notifyNotificationRemove(NotificationData nd)
    {
        Log.d("Nils", "notification remove #" + nd.id);

        // send notification to nilsplus
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(SettingsActivity.FP_ENABLED, SettingsActivity.DEFAULT_FP_ENABLED))
        {
            Intent npsIntent = new Intent();
            npsIntent.setComponent(new ComponentName("com.roymam.android.nilsplus", "com.roymam.android.nilsplus.NPService"));
            npsIntent.setAction(REMOVE_NOTIFICATION);
            npsIntent.putExtra("id", nd.id);
            npsIntent.putExtra("uid", nd.uid);
            npsIntent.putExtra("package", nd.packageName);
            context.startService(npsIntent);
        }

        // notify FloatingNotifications for clearing this notification
        Intent intent = new Intent();
        intent.setAction("robj.floating.notifications.dismiss");
        intent.putExtra("package", nd.packageName);
        context.sendBroadcast(intent);

        // free memory used by the notification
        nd.cleanup();
    }

    @Override
    public void onNotificationCleared(NotificationData nd)
    {
        notifyNotificationRemove(nd);
    }

    @Override
    public void onNotificationsListChanged()
    {
        updateWidget(true);
    }

    @Override
    public void onPersistentNotificationAdded(PersistentNotification pn)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // update widget if it is need to display this persistent notification;
        if (prefs.getBoolean(pn.packageName + "." + PersistentNotificationSettingsActivity.SHOW_PERSISTENT_NOTIFICATION, false))
            updateWidget(true);
    }

    @Override
    public void onPersistentNotificationCleared(PersistentNotification pn)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // update widget if it is need to display this persistent notification;
        if (prefs.getBoolean(pn.packageName + "." + PersistentNotificationSettingsActivity.SHOW_PERSISTENT_NOTIFICATION, false))
            updateWidget(true);
    }

    @Override
    public void onServiceStarted()
    {
        // first run preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean firstRun = prefs.getBoolean("com.roymam.android.notificationswidget.firstrun", true);
        prefs.edit().putBoolean("com.roymam.android.notificationswidget.firstrun", false).commit();
//        if (firstRun)
//        {
//            if (Build.MODEL.equals("Nexus 4"))
//            {
//                prefs.edit().putBoolean(SettingsActivity.DISABLE_PROXIMITY, true).commit();
//            }
//        }

        //registerProximitySensor();
        mHandler = new Handler();
        updateWidget(true);
    }

    @Override
    public void onServiceStopped()
    {
        stopProximityMontior();
        updateWidget(true);
    }

    private void updateWidget(boolean refreshList)
    {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, NotificationsWidgetProvider.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);

        if (refreshList)
        {
            for (int i=0; i<widgetIds.length; i++)
            {
                AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(widgetIds[i], R.id.notificationsListView);
            }
        }
        context.sendBroadcast(new Intent(NotificationsWidgetProvider.UPDATE_CLOCK));
    }

    private Runnable mReleaseWakelock = new Runnable()
    {
        @Override
        public void run()
        {
            if (mWakeLock != null && mWakeLock.isHeld())
            {
                mWakeLock.release();
            }
            turnScreenOff();
        }
    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void turnScreenOn(String packageName)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        // check if need to turn screen on
        String wakeupMode = SettingsActivity.getWakeupMode(context, packageName);
        Boolean turnScreenOn = !wakeupMode.equals(SettingsActivity.WAKEUP_NEVER);

        if (turnScreenOn && (deviceCovered == null || !deviceCovered))
        {
            final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            int timeout = Integer.parseInt(sharedPref.getString(SettingsActivity.TURNSCREENON_TIMEOUT, String.valueOf(SettingsActivity.DEFAULT_TURNSCREENON_TIMEOUT))) * 1000;

            // turn the screen on only if it was off or acquired by previous wakelock
            if (!pm.isScreenOn() || mWakeLock != null && mWakeLock.isHeld())
            {
                // release previously held wake lock
                if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release();

                // release previously callback
                mHandler.removeCallbacks(mReleaseWakelock);

                // create and aquire a new wake lock
                // @SuppressWarnings("deprecation")
                mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Notification");
                mWakeLock.acquire();

                // release wake lock on timeout ends
                mHandler.postDelayed(mReleaseWakelock, timeout);
            }

            newNotificationsAvailable = false;
        }
        else
            newNotificationsAvailable = true;
    }

    private boolean isAppOnForeground(String packageName)
    {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null)
        {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses)
        {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName))
            {
                return true;
            }
        }
        return false;
    }

    private void turnScreenOff()
    {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean isLocked = km.inKeyguardRestrictedInputMode();

        if (!isLocked)
        {
            // check if another lock screen is currently used
            String[] lockscreepApps = context.getResources().getStringArray(R.array.lockscreenapps);
            for (String lockscreen : lockscreepApps)
            {
                if (isAppOnForeground(lockscreen))
                    isLocked = true;
            }
        }

        // turn screen of only if the device is still locked
        if (isLocked)
        {
            Intent screenoffApp = context.getPackageManager().getLaunchIntentForPackage("com.cillinsoft.scrnoff");
            if (screenoffApp == null)
                screenoffApp = context.getPackageManager().getLaunchIntentForPackage("com.katecca.screenofflock");

            if (screenoffApp != null) context.startActivity(screenoffApp);
        }
    }

    // Proximity Sensor Monitoring
    SensorEventListener sensorListener = null;

    @Override
    public void registerProximitySensor(final String packageName)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d("NiLS", "registerProximitySensor");
        String wakeupMode = SettingsActivity.getWakeupMode(context, packageName);

        if (wakeupMode.equals(SettingsActivity.WAKEUP_NOT_COVERED) || wakeupMode.equals(SettingsActivity.WAKEUP_UNCOVERED))
        {
            final SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
            final Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (sensorListener == null)
                sensorListener = new SensorEventListener()
                {
                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy)
                    {
                    }

                    @Override
                    public void onSensorChanged(SensorEvent event)
                    {
                        boolean newCoverStatus = (event.values[0] == 0);
                        // unknown -> something
                        if (deviceCovered == null)
                            deviceCovered = newCoverStatus;
                        // uncovered -> covered
                        else if (!deviceCovered && newCoverStatus)
                        {
                            deviceCovered = true;
                        }
                        // covered -> uncovered
                        else if (deviceCovered && !newCoverStatus)
                        {
                            deviceCovered = false;
                            String wakeupMode = SettingsActivity.getWakeupMode(context, packageName);

                            // stop proximity monitoring if delayed screen on is inactive
                            if (wakeupMode.equals(SettingsActivity.WAKEUP_UNCOVERED))
                            {
                                turnScreenOn(packageName);
                            }

                            stopProximityMontior();
                        }
                        // else (cover -> cover , uncover -> uncover) - do nothing
                        Log.d("NiLS", "device covered:"+deviceCovered);
                    }
                };

            // start with unknown cover status
            if (!proximityRegistered)
            {
                deviceCovered = null;
                sensorManager.registerListener(sensorListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
                proximityRegistered = true;
            }
        }
    }

    @Override
    public void stopProximityMontior()
    {
        Log.d("NiLS", "unregisterProximitySensor");
        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(sensorListener);
        deviceCovered = null;
        proximityRegistered = false;
    }

}
