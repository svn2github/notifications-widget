package com.roymam.android.notificationswidget;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.roymam.android.common.SysUtils;

public class UnlockDeviceActivity extends Activity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d("NiLS", "UnlockDeviceActivity:onCreate");
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        if (SysUtils.isKeyguardLocked(this))
        {
            // if keyguard is active, wait until the device will be unlocked, then finish the activity
            registerReceiver(new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    unregisterReceiver(this);
                    finish();
                }
            }, new IntentFilter(Intent.ACTION_USER_PRESENT));
        }
        else
        {
            finish();
        }
    }
}
