package com.roymam.android.notificationswidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter implements NotificationEventListener
{
    private Context context = null;
    private boolean newNotificationsAvailable = false;
    private boolean deviceCovered = false;

    // extensions API
    public static final String ADD_NOTIFICATION = "com.roymam.android.nils.add_notification";
    public static final String REMOVE_NOTIFICATION = "com.roymam.android.nils.remove_notification";

    public NotificationAdapter(Context context)
    {
        this.context = context;
    }

    @Override
    public void onNotificationAdded(NotificationData nd)
    {
        // turn screen on (if needed)
        turnScreenOn();
        notifyNotificationAdd(nd);
    }

    private void notifyNotificationAdd(NotificationData nd)
    {
        Log.d("Nils", "notification add #" + nd.id);

        // send notification to nilsplus
        Intent npsIntent = new Intent();
        npsIntent.setComponent(new ComponentName("com.roymam.android.nilsplus", "com.roymam.android.nilsplus.NPService"));
        npsIntent.setAction(ADD_NOTIFICATION);
        npsIntent.putExtra("title", nd.title);
        npsIntent.putExtra("text", nd.text);
        npsIntent.putExtra("package", nd.packageName);
        npsIntent.putExtra("id", nd.id);

        // convert large icon to byte stream
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        nd.icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
        npsIntent.putExtra("icon", stream.toByteArray());

        // convert large icon to byte stream
        stream = new ByteArrayOutputStream();
        nd.appicon.compress(Bitmap.CompressFormat.PNG, 100, stream);
        npsIntent.putExtra("appicon", stream.toByteArray());

        context.startService(npsIntent);
    }

    private void notifyNotificationRemove(NotificationData nd)
    {
        // send notification to nilsplus
        Log.d("Nils", "notification remove #" + nd.id);
        Intent npsIntent = new Intent();
        npsIntent.setComponent(new ComponentName("com.roymam.android.nilsplus", "com.roymam.android.nilsplus.NPService"));
        npsIntent.setAction(REMOVE_NOTIFICATION);
        npsIntent.putExtra("id", nd.id);
        context.startService(npsIntent);

        // notify FloatingNotifications for clearing this notification
        Intent intent = new Intent();
        intent.setAction("robj.floating.notifications.dismiss");
        intent.putExtra("package", nd.packageName);
        context.sendBroadcast(intent);
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
        updateWidget(true);
    }

    @Override
    public void onServiceStarted()
    {
        registerProximitySensor();
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
        Boolean turnScreenOn = sharedPref.getBoolean(SettingsActivity.TURNSCREENON, true);
        if (turnScreenOn && !deviceCovered)
        {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            // turn the screen on only if it was off
            if (!pm.isScreenOn())
            {
                @SuppressWarnings("deprecation")
                final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Notification");
                wl.acquire();

                // release after 5 seconds
                final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
                Runnable task = new Runnable()
                {
                    public void run()
                    {
                        wl.release();
                    }
                };
                worker.schedule(task, 10, TimeUnit.SECONDS);
            }
            newNotificationsAvailable = false;
        }
        else
            newNotificationsAvailable = true;
    }

    // Proximity Sensor Monitoring
    SensorEventListener sensorListener = null;

    public void registerProximitySensor()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (!prefs.getBoolean(SettingsActivity.DISABLE_PROXIMITY, false) &&
                prefs.getBoolean(SettingsActivity.TURNSCREENON, true))
        {
            SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
            Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            sensorListener = new SensorEventListener()
            {
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy)
                {
                }

                @Override
                public void onSensorChanged(SensorEvent event)
                {
                    if (event.values[0] == 0)
                    {
                        deviceCovered = true;
                    }
                    else
                    {
                        if (deviceCovered)
                        {
                            deviceCovered = false;
                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                            if (sharedPref.getBoolean(SettingsActivity.DELAYED_SCREEON, false) && newNotificationsAvailable)
                            {
                                turnScreenOn();
                            }
                        }
                    }
                }
            };
            sensorManager.registerListener(sensorListener, proximitySensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stopProximityMontior()
    {
        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(sensorListener);
        deviceCovered = false;
    }

}
