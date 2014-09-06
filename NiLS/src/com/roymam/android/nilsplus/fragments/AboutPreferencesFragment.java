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
    private int notificationId = 0;

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
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(mContext);

        String randWord = "";
        switch (notificationId)
        {
            case 0:
                randWord = "foo";
                break;
            case 1:
                randWord = "bar";
                break;
            case 2:
                randWord = "baz";
                break;
            case 3:
                randWord = "qux";
                break;
        }

        // build an individual notification
        Notification n1 = new NotificationCompat.Builder(mContext)
                .setContentTitle("NiLS Test Notification")
                .setContentText(randWord+":Single notification test line 1\nSingle notification test line 2\nSingle notification test line 3\n" +
                        "Single notification test line 4\n" +
                        "Single notification test line 5\n" +
                        "Single notification test line 6\n" +
                        "Single notification test line 7\n" +
                        "Single notification test line 8\n" +
                        "Single notification test line 9\n" +
                        "Single notification test line 10")
                .setLargeIcon(((BitmapDrawable) mContext.getResources().getDrawable(R.drawable.ic_launcher)).getBitmap())
                .setSmallIcon(R.drawable.nilsfp_icon_mono)
                .setContentIntent(PendingIntent.getActivity(mContext, 0, new Intent(mContext, NiLSActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true)
                .build();

        // Issue the notification
        notificationManager.notify(0, n1);

        // build a grouped notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                .setContentTitle("NiLS Grouped Notification")
                .setLargeIcon(((BitmapDrawable) mContext.getResources().getDrawable(R.drawable.ic_launcher)).getBitmap())
                .setSmallIcon(R.drawable.nilsfp_icon_mono)
                .setContentIntent(PendingIntent.getActivity(mContext, 0, new Intent(mContext, NiLSActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true);

        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("NiLS Grouped Notification");
        builder.setContentTitle("NiLS Grouped Notification");
        inboxStyle.setSummaryText("3 test events");
        inboxStyle.addLine(randWord+":NiLS Test Notification - Event 1");
        inboxStyle.addLine(randWord+":NiLS Test Notification - Event 2");
        inboxStyle.addLine(randWord+":NiLS Test Notification - Event 3");
        builder.setStyle(inboxStyle);
        Notification n2 = builder.build();

        // Issue the notification
        notificationManager.notify(1, n2);

        notificationId++;
        if (notificationId == 4) notificationId = 0;
    }
}
