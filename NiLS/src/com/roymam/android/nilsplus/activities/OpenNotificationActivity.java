package com.roymam.android.nilsplus.activities;

import android.R;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.roymam.android.common.SysUtils;
import com.roymam.android.notificationswidget.NotificationsService;
import com.roymam.android.notificationswidget.SettingsManager;

public class OpenNotificationActivity extends Activity
{
    private final String UNLOCK_HACK_DEVICES = "ls980,l01f,m7wls,m7cdug,m7vzw,m7spr,m7,g3,g2";
    private BroadcastReceiver mReceiver;

    private void openNotification(PendingIntent action, String packageName, int uid, Intent paramIntent)
    {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        // directly open notification
        if (action != null)
        try
        {
            action.send(this, 0, paramIntent);
        } catch (Exception e)
        {
            // if cannot launch intent, create a new one for the app
            try
            {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                startActivity(launchIntent);
            }
            catch(Exception e2)
            {
                // cannot launch intent - do nothing...
                e2.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error - cannot launch app:"+packageName, Toast.LENGTH_SHORT).show();
            }
        }

        dismiss(uid);
        finish();
    }

    private void dismiss(int uid)
    {
        // notify dismiss
        Log.d("NiLS+", "dismissing uid:" + uid);
        Intent intent = new Intent(NotificationsService.REMOVE_NOTIFICATION);
        intent.putExtra("uid", uid);
        sendBroadcast(intent);
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final PendingIntent action = getIntent().getParcelableExtra("action");
        final String packageName = getIntent().getStringExtra("package");
        final Intent paramIntent = getIntent().getParcelableExtra("paramIntent");
        final int uid = getIntent().getIntExtra("uid",-1);

        if (SysUtils.isKeyguardLocked(this) &&
            prefs.getBoolean(SettingsManager.UNLOCK_ON_OPEN, SettingsManager.DEFAULT_UNLOCK_ON_OPEN))
        {
            // unlock the device and then open the notification
            Log.d("NiLS", "Requesting device to be unlocked");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

            // set default unlock workaround for specific devices
            if (UNLOCK_HACK_DEVICES.contains(Build.DEVICE) && !prefs.getAll().containsKey(SettingsManager.UNLOCK_WORKAROUND))
                prefs.edit().putBoolean(SettingsManager.UNLOCK_WORKAROUND, true).commit();

            if (prefs.getBoolean(SettingsManager.UNLOCK_WORKAROUND, SettingsManager.DEFAULT_UNLOCK_WORKAROUND))
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

            mReceiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    openNotification(action, packageName, uid, paramIntent);
                }
            };

            registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
        }
        else
        {
            // open notification on background
            Log.d("NiLS", "Opening a notification (no need to unlock device)");
            openNotification(action, packageName, uid, paramIntent);
        }
    }

    @Override
    protected void onDestroy()
    {
        if (mReceiver != null)
            unregisterReceiver(mReceiver);
        super.onDestroy();
    }

}