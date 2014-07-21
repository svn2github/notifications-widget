package com.roymam.android.nilsplus.fragments;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.roymam.android.nilsplus.CardPreferenceFragment;
import com.roymam.android.nilsplus.ui.NiLSActivity;
import com.roymam.android.notificationswidget.AppSettingsActivity;
import com.roymam.android.notificationswidget.NotificationData;
import com.roymam.android.notificationswidget.R;
import com.roymam.android.notificationswidget.SettingsManager;

public class AboutPreferencesFragment extends CardPreferenceFragment
{
    private Context mContext;
    private int notificationId = 1;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mContext = getActivity();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Load the global_settings from an XML resource
        addPreferencesFromResource(R.xml.about_preferences);

        // setting "version" button summary
        Preference versionPref = findPreference("version");
        String versionString = "";
        try
        {
            versionString = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
            versionPref.setSummary(getText(R.string.version) + " " + versionString);
        } catch (PackageManager.NameNotFoundException e)
        {
        }

        ((PreferenceGroup) findPreference("debug")).findPreference("send_notification").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                sendTestNotification();
                return true;
            }
        });
    }

    private void sendTestNotification()
    {
        //NotificationCompat.WearableExtender wearableExtender =
        //        new NotificationCompat.WearableExtender()
        //                .setHintHideIcon(false);

        // Build the notification, setting the group appropriately
        Notification notif = new NotificationCompat.Builder(mContext)
                .setContentTitle("NiLS Test Notification " + notificationId)
                .setContentText("Notification text line 1\nNotification text line 2\nNotification text line 3")
                .setLargeIcon(((BitmapDrawable) mContext.getResources().getDrawable(R.drawable.ic_launcher)).getBitmap())
                .setSmallIcon(R.drawable.nilsfp_icon_mono)
                .setContentIntent(PendingIntent.getActivity(mContext, 0, new Intent(mContext, NiLSActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true)
        //        .extend(wearableExtender)
                .build();

        // Issue the notification
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(mContext);
        notificationManager.notify(++notificationId, notif);
    }
}
