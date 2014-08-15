package com.roymam.android.common;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.roymam.android.notificationswidget.NiLSAccessibilityService;
import com.roymam.android.notificationswidget.NotificationsListener;
import com.roymam.android.notificationswidget.SettingsManager;

import java.util.Iterator;
import java.util.List;

public class SysUtils
{
    private static final String TAG = SysUtils.class.getSimpleName();

    private static SysUtils instance;
    private final Context context;
    private final Handler handler;
    private static int DEFAULT_DEVICE_TIMEOUT = 10000;
    private PowerManager.WakeLock mWakeLock = null;
    private boolean mPendingCallback = false;

    public SysUtils(Context context, Handler handler)
    {
        this.context = context;
        this.handler = handler;
    }

    public static SysUtils getInstance(Context context, Handler handler)
    {
        if (instance == null)
            instance = new SysUtils(context, handler);
        return instance;
    }

    public static boolean isServiceRunning(Context context, Class serviceClass)
    {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isServiceRunning(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            try {
                return isServiceRunning(context, NotificationsListener.class);
            }catch (Exception exp)
            {
                Log.wtf(TAG, "sdk_int:"+Build.VERSION.SDK_INT+ " but NotificationsListener is not found, trying old service");
                return isServiceRunning(context, NiLSAccessibilityService.class);
            }
        else
           return isServiceRunning(context, NiLSAccessibilityService.class);
    }

    public static String getForegroundApp(Context context)
    {
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
        Log.d(TAG, tasks.get(0).topActivity.getClassName());
        return tasks.get(0).topActivity.getPackageName();
    }

    public static String getForegroundActivity(Context context)
    {
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
        return tasks.get(0).topActivity.getClassName();
    }

    public static boolean isAppForground(Context context, String packageName)
    {
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> l = mActivityManager
                .getRunningAppProcesses();
        Iterator<ActivityManager.RunningAppProcessInfo> i = l.iterator();
        while (i.hasNext())
        {
            ActivityManager.RunningAppProcessInfo info = i.next();
            for (String p : info.pkgList)
            {
                if (p.equals(packageName) && info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
                    return true;
            }
        }
        return false;
    }

    public static boolean isKeyguardLocked(Context context)
    {
        KeyguardManager kmanager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            return kmanager.isKeyguardLocked();
        else
            return kmanager.inKeyguardRestrictedInputMode();
    }

    private boolean shouldChangeDeviceTimeout()
    {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String timeoutStr = sharedPref.getString(SettingsManager.TURNSCREENOFF, SettingsManager.TURNSCREENOFF_DEFAULT);
        return (!timeoutStr.equals("") && !timeoutStr.equals(SettingsManager.TURNSCREENOFF_DEFAULT));
    }

    public void turnScreenOn(boolean force, String reason)
    {
        turnScreenOn(force, false, reason);
    }

    public void turnScreenOn(boolean force, boolean defaultTimeout, String reason)
    {
        Log.d(TAG, "turnScreenOn called. reason:"+reason);
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        // read timeout preference - default - device settings
        int newTimeout = 5000;
        String timeoutStr = sharedPref.getString(SettingsManager.TURNSCREENOFF, SettingsManager.TURNSCREENOFF_DEFAULT);
        if (timeoutStr.equals("")) timeoutStr = SettingsManager.TURNSCREENOFF_DEFAULT;
        boolean deviceDefault = (timeoutStr.equals(SettingsManager.TURNSCREENOFF_DEFAULT));

        if (!defaultTimeout)
        {
            if (!deviceDefault)
                newTimeout = Integer.parseInt(timeoutStr) * 1000;
        }
        else
        {
            // if default timeout requested and NiLS is configured to keep default - use device settings
            if (deviceDefault)
                newTimeout = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, newTimeout);
            else
                // if NiLS changed device default, use the stored value
                newTimeout = sharedPref.getInt("device_timeout", newTimeout);
        }

        // turn the screen on only if it was off or acquired by previous wakelock
        if (    !pm.isScreenOn() ||
                mWakeLock != null && mWakeLock.isHeld() ||
                force)
        {
            // create and acquire a new wake lock (if not already held)
            if (mWakeLock == null || !mWakeLock.isHeld())
            {
                Log.d(TAG, "wake lock is not held, acquiring new one");
                // @SuppressWarnings("deprecation")
                mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
                mWakeLock.acquire(newTimeout);
            }
            else // mWakeLock != null && mWakeLock.isHeld()
            {
                // if screen is off, release the previous wake lock
                if (!pm.isScreenOn())
                {
                    Log.d(TAG, "wakelock is already held and screen is off, releasing and creating a new one");
                    mWakeLock.release();
                    mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
                }

                // acquire new one
                Log.d(TAG, "wake lock is already held, extending it");
                mWakeLock.acquire(newTimeout);
            }

            mPendingCallback = true;
        }
        else
        {
            Log.d(TAG, "turnScreenOn ignored, isScreenOn:" + pm.isScreenOn() + " mWakelock:"+mWakeLock);
        }
    }

    private void storeDeviceTimeout()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int deviceTimeout = prefs.getInt("device_timeout", -1);
        if (deviceTimeout == -1)
        {
            // store the original device timeout
            deviceTimeout = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_DEVICE_TIMEOUT);
            Log.d(TAG, "storing device timeout:" + deviceTimeout);
            prefs.edit().putInt("device_timeout", deviceTimeout).commit();
        }
        else
        {
            Log.d(TAG, "device timeout already stored (" + deviceTimeout + ")");
        }
    }

    public void setDeviceTimeout()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String timeoutStr = prefs.getString(SettingsManager.TURNSCREENOFF, SettingsManager.TURNSCREENOFF_DEFAULT);
        if (timeoutStr.equals("")) timeoutStr = SettingsManager.TURNSCREENOFF_DEFAULT;
        boolean deviceDefault = (timeoutStr.equals(SettingsManager.TURNSCREENOFF_DEFAULT));

        if (!deviceDefault)
        {
            // store current device timeout
            storeDeviceTimeout();

            // set the new (shorter) one
            int newTimeout = Integer.parseInt(timeoutStr) * 1000;
            Log.d(TAG, "changing device timeout to " + newTimeout);
            try
            {
                Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, newTimeout);
            }
            catch (Exception exp)
            {
                Log.e(TAG, "cannot change system settings, screen timeout won't be changed");
            }
        }
    }

    public void restoreDeviceTimeout()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int deviceTimeout = prefs.getInt("device_timeout", -1);

        // restore previous timeout settings
        if (deviceTimeout != -1)
        {
            int currTimeout = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, -1);
            // read timeout preference - default - device settings
            String timeoutStr = prefs.getString(SettingsManager.TURNSCREENOFF, SettingsManager.TURNSCREENOFF_DEFAULT);
            if (timeoutStr.equals("")) timeoutStr = SettingsManager.TURNSCREENOFF_DEFAULT;

            int newTimeout = 0;
            if (!timeoutStr.equals(SettingsManager.TURNSCREENOFF_DEFAULT))
                newTimeout = Integer.parseInt(timeoutStr) * 1000;

            if (currTimeout == newTimeout)
            {
                if (shouldChangeDeviceTimeout())
                {
                    Log.d(TAG, "restoring device timeout:" + deviceTimeout);

                    try
                    {
                        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, deviceTimeout);
                    }
                    catch (Exception exp)
                    {
                        Log.e(TAG, "cannot change system settings, screen timeout won't be changed");
                    }

                    resetDeviceTimeout();
                }
            }
            else
            {
                Log.d(TAG, "screen timeout was changed ("+currTimeout+") by another app, NiLS won't restore its own");
            }
        }
        else
        {
            Log.d(TAG, "restore device timeout called but device timeout wasn't stored. ignoring.");
        }
    }

    private void resetDeviceTimeout()
    {
        Log.d(TAG, "reset device timeout");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove("device_timeout").commit();
    }


}
