package com.roymam.android.notificationswidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
            handleWakeupMode();
        }
        notifyNotificationAdd(nd);
    }

    private void handleWakeupMode()
    {
        String wakeupMode = SettingsActivity.getWakeupMode(context);

        if (wakeupMode.equals(SettingsActivity.WAKEUP_ALWAYS))
            turnScreenOn();
        else
        {
            registerProximitySensor();

            if (mHandler == null) mHandler = new Handler();

            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    if (deviceCovered == null || !deviceCovered)
                    {
                        // the device is not covered or we don't know yet so we turn the screen on
                        turnScreenOn();
                        stopProximityMontior();
                    }
                    else  // the device is covered
                    {
                        String wakeupMode = SettingsActivity.getWakeupMode(context);
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
            handleWakeupMode();

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
        if (nd.appicon != null)
        {
            nd.appicon.recycle();
            nd.appicon = null;
        }

        if (nd.icon != null)
        {
            nd.icon.recycle();
            nd.icon = null;
        }
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

    private void turnScreenOn()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        // check if need to turn screen on
        String wakeupMode = SettingsActivity.getWakeupMode(context);
        Boolean turnScreenOn = !wakeupMode.equals(SettingsActivity.WAKEUP_NEVER);

        if (turnScreenOn && (deviceCovered == null || !deviceCovered))
        {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            // turn the screen on only if it was off
            if (!pm.isScreenOn())
            {
                @SuppressWarnings("deprecation")
                final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Notification");
                wl.acquire();

                // release after 10 seconds
                final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
                Runnable task = new Runnable()
                {
                    public void run()
                    {
                        wl.release();
                    }
                };
                worker.schedule(task, Integer.parseInt(sharedPref.getString(SettingsActivity.TURNSCREENON_TIMEOUT, String.valueOf(SettingsActivity.DEFAULT_TURNSCREENON_TIMEOUT))), TimeUnit.SECONDS);
            }
            newNotificationsAvailable = false;
        }
        else
            newNotificationsAvailable = true;
    }

    // Proximity Sensor Monitoring
    SensorEventListener sensorListener = null;

    @Override
    public void registerProximitySensor()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d("NiLS", "registerProximitySensor");
        String wakeupMode = SettingsActivity.getWakeupMode(context);

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
                            String wakeupMode = SettingsActivity.getWakeupMode(context);

                            // stop proximity monitoring if delayed screen on is inactive
                            if (wakeupMode.equals(SettingsActivity.WAKEUP_UNCOVERED))
                            {
                                turnScreenOn();
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
