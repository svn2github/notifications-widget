package com.roymam.android.nilsplus.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.roymam.android.nilsplus.ui.PopupNotification;
import com.roymam.android.notificationswidget.NotificationData;
import com.roymam.android.notificationswidget.NotificationsService;

import java.util.HashMap;
import java.util.List;

public class PopupNotificationService extends Service {
    private static final String TAG = PopupNotificationService.class.getSimpleName() ;
    public static final String POPUP_NOTIFICATION = "popup_notification";
    public static final String EXTRA_NOTIFICATION = "extra_notification";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand intent:"+intent);
        if (intent != null) {
            if (intent.getAction() != null) {
                String action = intent.getAction();
                if (action.equals(POPUP_NOTIFICATION)) {
                    NotificationData nd = intent.getParcelableExtra(EXTRA_NOTIFICATION);
                    if (nd != null) PopupNotification.create(this, nd).show();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
