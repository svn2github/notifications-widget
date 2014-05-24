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

import com.roymam.android.notificationswidget.NotificationsService;
import com.roymam.android.notificationswidget.SettingsManager;

public class OpenNotificationActivity extends Activity
{
    private BroadcastReceiver mReceiver;

    private void openNotification(PendingIntent action, String packageName, int id, int uid)
    {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        // directly open notification
        if (action != null)
        try
        {
            action.send();
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
        final String lockscreenPackageName = getIntent().getStringExtra("lockscreen_package");
        final int id = getIntent().getIntExtra("id",-1);
        final int uid = getIntent().getIntExtra("uid",-1);

        if (lockscreenPackageName.equals(NotificationsService.GO_LOCKER_PACKAGENAME) ||
            lockscreenPackageName.equals(NotificationsService.WIDGET_LOCKER_PACKAGENAME) ||
            !isKeyguardLocked() ||
            !prefs.getBoolean(SettingsManager.UNLOCK_ON_OPEN, SettingsManager.DEFAULT_UNLOCK_ON_OPEN))
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

            // open notification on background
            openNotification(action, packageName,id, uid);
        }
        else
        {
            // unlock the device and then open the notification
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);

            mReceiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    openNotification(action, packageName,id, uid);
                }
            };
            registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
        }
    }

    private boolean isKeyguardLocked()
    {
        KeyguardManager kmanager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            return kmanager.isKeyguardLocked();
        else
            return kmanager.inKeyguardRestrictedInputMode();
    }

    @Override
    protected void onDestroy()
    {
        if (mReceiver != null)
            unregisterReceiver(mReceiver);
        super.onDestroy();
    }

}