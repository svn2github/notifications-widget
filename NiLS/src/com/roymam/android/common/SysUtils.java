package com.roymam.android.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.roymam.android.notificationswidget.SettingsActivity;

public class SysUtils
{
    private static SysUtils instance;
    private final Context context;
    private final Handler handler;
    private static int DEFAULT_DEVICE_TIMEOUT = 10000;
    private PowerManager.WakeLock mWakeLock = null;
    private Runnable mReleaseWakelock = null;
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

    private boolean shouldChangeDeviceTimeout()
    {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String timeoutStr = sharedPref.getString(SettingsActivity.TURNSCREENOFF, SettingsActivity.TURNSCREENOFF_DEFAULT);
        return (!timeoutStr.equals("") && !timeoutStr.equals(SettingsActivity.TURNSCREENOFF_DEFAULT));
    }

    public void turnScreenOn(boolean force)
    {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        Log.d("NiLS", "turnScreenOn requested, force:" + force);
        // read timeout preference - default - device settings
        String timeoutStr = sharedPref.getString(SettingsActivity.TURNSCREENOFF, SettingsActivity.TURNSCREENOFF_DEFAULT);
        if (timeoutStr.equals("")) timeoutStr = SettingsActivity.TURNSCREENOFF_DEFAULT;

        int newTimeout = 0;
        if (!timeoutStr.equals(SettingsActivity.TURNSCREENOFF_DEFAULT))
            newTimeout = Integer.parseInt(timeoutStr) * 1000;

        // turn the screen on only if it was off or acquired by previous wakelock
        if (!pm.isScreenOn() || mWakeLock != null && mWakeLock.isHeld() || force)
        {
            // set device timeout to the desired timeout
            storeDeviceTimeout();
            if (shouldChangeDeviceTimeout())
            {
                Log.d("NiLS", "setting new timeout:" + newTimeout);
                Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, newTimeout);
            }

            // create the release wake lock runnable
            if (mReleaseWakelock == null) {
                mReleaseWakelock = new Runnable() {
                    @Override
                    public void run() {
                        mPendingCallback = false;
                        if (mWakeLock != null && mWakeLock.isHeld()) {
                            Log.d("NiLS", "releasing wake lock");
                            mWakeLock.release();
                        }

                        restoreDeviceTimeout();
                        resetDeviceTimeout();
                    }
                };
            }

            // create and acquire a new wake lock (if not already held)
            if (mWakeLock == null || !mWakeLock.isHeld())
            {
                Log.d("NiLS", "wake lock is not held, acquiring new one");
                // @SuppressWarnings("deprecation")
                mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "NiLS");
                mWakeLock.acquire();
            }

            // release wake lock on timeout ends
            if (mReleaseWakelock != null && mPendingCallback)
            {
                Log.d("NiLS", "pending callback found, remove it");

                // release previously callback
                handler.removeCallbacks(mReleaseWakelock);
            }

            Log.d("NiLS", "posting delayed callback within " + newTimeout);
            handler.postDelayed(mReleaseWakelock, newTimeout);
            mPendingCallback = true;
        }
        else
        {
            Log.d("NiLS", "turnScreenOn ignored, isScreenOn:" + pm.isScreenOn() + " mWakelock:"+mWakeLock);
        }
    }

    public void storeDeviceTimeout()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int deviceTimeout = prefs.getInt("device_timeout", -1);
        if (deviceTimeout == -1)
        {
            // store the original device timeout
            deviceTimeout = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_DEVICE_TIMEOUT);
            Log.d("NiLS", "storing device timeout:" + deviceTimeout);
            prefs.edit().putInt("device_timeout", deviceTimeout).commit();
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
            String timeoutStr = prefs.getString(SettingsActivity.TURNSCREENOFF, SettingsActivity.TURNSCREENOFF_DEFAULT);
            if (timeoutStr.equals("")) timeoutStr = SettingsActivity.TURNSCREENOFF_DEFAULT;

            int newTimeout = 0;
            if (!timeoutStr.equals(SettingsActivity.TURNSCREENOFF_DEFAULT))
                newTimeout = Integer.parseInt(timeoutStr) * 1000;

            if (currTimeout == newTimeout)
            {
                if (shouldChangeDeviceTimeout())
                {
                    Log.d("NiLS", "restoring device timeout:" + deviceTimeout);
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, deviceTimeout);
                }
            }
            else
            {
                Log.d("NiLS", "screen timeout was changed ("+currTimeout+") by another app, NiLS won't restore its own");
            }
        }
        else
        {
            Log.d("NiLS", "I don't know what the timeout was...");
        }
    }

    private void resetDeviceTimeout()
    {
        Log.d("NiLS", "reset device timeout");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove("device_timeout").commit();
    }
}
