package com.roymam.android.notificationswidget;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.roymam.android.common.SysUtils;
import com.roymam.android.nilsplus.services.PopupNotificationService;
import com.roymam.android.nilsplus.ui.PopupNotification;

import java.util.List;

public class NotificationEventsAdapter implements NotificationEventListener
{
    private static final String TAG = NotificationEventsAdapter.class.getSimpleName();
    private final SysUtils mSysUtils;
    private Context context = null;
    private boolean newNotificationsAvailable = false;
    private Boolean mDeviceCovered = null;
    private boolean proximityRegistered = false;

    // extensions API
    public static final String ADD_NOTIFICATION = "com.roymam.android.nils.add_notification";
    public static final String UPDATE_NOTIFICATION = "com.roymam.android.nils.update_notification";
    public static final String REMOVE_NOTIFICATION = "com.roymam.android.nils.remove_notification";
    private Handler mHandler = null;
    private PowerManager.WakeLock mWakeLock = null;
    private int mDeviceTimeout;

    public NotificationEventsAdapter(Context context, Handler handler)
    {
        this.context = context;
        this.mHandler = handler;
        mSysUtils = SysUtils.getInstance(context, handler);
    }

    @Override
    public void onNotificationAdded(NotificationData nd, final boolean wake, Boolean deviceCovered)
    {
        // turn screen on (if needed)
        if (wake)
        {
            handleWakeupMode(nd.packageName, deviceCovered);
        }
        notifyNotificationAdd(nd);
    }

    private void handleWakeupMode(final String packageName, Boolean deviceCovered)
    {
        String wakeupMode = SettingsManager.getWakeupMode(context, packageName);

        // set covered status (if we got it)
        mDeviceCovered = deviceCovered;

        if (wakeupMode.equals(SettingsManager.WAKEUP_ALWAYS)) {
            Log.d(TAG, "wakeup mode is ALWAYS, turning screen on");
            turnScreenOn();
        }
        else if (!wakeupMode.equals(SettingsManager.WAKEUP_NEVER))
        {
            registerProximitySensor(packageName);
            if (mHandler == null) mHandler = new Handler();
            if (wakeupMode.equals(SettingsManager.WAKEUP_NOT_COVERED))
            {
                // if wakeup mode is when not covered, stop proximity monitoring after few seconds
                mHandler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        stopProximityMontior("5 seconds passed");
                    }
                }, 5000);

                // turn screen on immediately if the device is known to be not covered
                if (deviceCovered != null && !deviceCovered)
                {
                    Log.d(TAG, "device is not covered (parameter), turning screen on");
                    turnScreenOn();
                }
            }
        }
    }

    @Override
    public void onNotificationUpdated(NotificationData nd, boolean changed, Boolean covered)
    {
        // turn screen on (if needed)
        if (changed)
            handleWakeupMode(nd.packageName, covered);

        notifyNotificationUpdated(nd);
    }

    private void notifyNotificationAdd(NotificationData nd)
    {
        Log.d(TAG, "notification add uid:" + nd.uid);

        // add the package to the app specific settings page
        AppSettingsActivity.addAppToAppSpecificSettings(nd.packageName, context);

        // show a popup message
        if (SettingsManager.getBoolean(context, SettingsManager.POPUP_ENABLED, SettingsManager.DEFAULT_POPUP_ENABLED)) {
            Intent popupNotificationIntent = new Intent(context, PopupNotificationService.class);
            popupNotificationIntent.setAction(PopupNotificationService.POPUP_NOTIFICATION);
            popupNotificationIntent.putExtra(PopupNotificationService.EXTRA_NOTIFICATION, nd);
            context.startService(popupNotificationIntent);
        }

        // re-transmit notification (if needed)
        if (SettingsManager.getBoolean(context, nd.packageName, AppSettingsActivity.RETRANSMIT, AppSettingsActivity.DEFAULT_RETRANSMIT))
        {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.nilsfp_icon_mono)
                    .setLargeIcon(nd.largeIcon)
                    .setContentTitle(nd.title)
                    .setContentText(nd.text)
                    .setContentIntent(nd.action);

            NotificationCompat.BigTextStyle bigtextstyle = new NotificationCompat.BigTextStyle();
            bigtextstyle.setBigContentTitle(nd.title);
            bigtextstyle.bigText(nd.text);
            mBuilder.setStyle(bigtextstyle);

            Notification n = mBuilder.build();
            nm.notify(nd.uid, n);
        }
    }

    private void notifyNotificationUpdated(NotificationData nd)
    {
        Log.d(TAG, "notification update #" + nd.uid);
    }

    private void notifyNotificationRemove(NotificationData nd)
    {
        Log.d(TAG, "NotificationAdapter:notifyNotificationRemove uid:" + nd.uid);

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
        mHandler = new Handler();
        updateWidget(true);
    }

    @Override
    public void onServiceStopped()
    {
        stopProximityMontior("service stopped");
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
/*
    private Runnable mReleaseWakelock = new Runnable()
    {
        @Override
        public void run()
        {
            if (mWakeLock != null && mWakeLock.isHeld())
            {
                mWakeLock.release();
            }

            // restore previous timeout settings
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, mDeviceTimeout);
        }
    };
*/
    private void turnScreenOn()
    {
        mSysUtils.turnScreenOn(false);
        newNotificationsAvailable = false;
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

    /*
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
    }*/

    // Proximity Sensor Monitoring
    SensorEventListener sensorListener = null;

    public void registerProximitySensor(final String packageName)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d(TAG, "registerProximitySensor");
        String wakeupMode = SettingsManager.getWakeupMode(context, packageName);

        if (wakeupMode.equals(SettingsManager.WAKEUP_NOT_COVERED) || wakeupMode.equals(SettingsManager.WAKEUP_UNCOVERED))
        {
            final SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
            final Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (sensorListener == null)
                sensorListener = new SensorEventListener()
                {
                    final Runnable turnOnScreen = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            // turn on screen and stop monitoring proximity
                            turnScreenOn();
                            stopProximityMontior("screen was turned on");
                        }
                    };

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy)
                    {
                    }

                    @Override
                    public void onSensorChanged(SensorEvent event)
                    {
                        boolean newCoverStatus = (event.values[0] < event.sensor.getMaximumRange());
                        Log.d(TAG, "proximity:"+event.values[0]+" device covered:"+newCoverStatus+" time:"+ SystemClock.uptimeMillis());

                        // if transition happened
                        if (mDeviceCovered == null || newCoverStatus != mDeviceCovered)
                        {
                            if (mHandler == null) mHandler = new Handler();

                            mDeviceCovered = newCoverStatus;
                            if (!mDeviceCovered)
                            {
                                long timeout = prefs.getLong("proximity_timeout", 500);
                                // turn on screen in 500ms (give it a chance to cancel)
                                Log.d(TAG, "Turning screen on within "+timeout+"ms");
                                mHandler.postDelayed(turnOnScreen, timeout);
                            }
                            else
                            {
                                // cancel turning on screen
                                Log.d(TAG, "Canceling turning screen on");
                                mHandler.removeCallbacks(turnOnScreen);
                            }
                        }
                    }
                };

            // start with unknown cover status
            if (!proximityRegistered)
            {
                mDeviceCovered = null;
                sensorManager.registerListener(sensorListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
                proximityRegistered = true;
            }
        }
    }

    public void stopProximityMontior(String reason)
    {
        Log.d(TAG, "unregisterProximitySensor (reason: "+reason+")");
        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(sensorListener);
        mDeviceCovered = null;
        proximityRegistered = false;
    }

}
