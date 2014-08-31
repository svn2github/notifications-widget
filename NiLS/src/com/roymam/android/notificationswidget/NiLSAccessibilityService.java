package com.roymam.android.notificationswidget;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.roymam.android.common.BitmapUtils;
import com.roymam.android.common.SysUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class NiLSAccessibilityService extends AccessibilityService
{
    private final String TAG = this.getClass().getSimpleName();
    public static final String LAST_OPENED_WINDOW_PACKAGENAME = "last_opened_window_packagename";
    private NotificationParser parser;
    private int notificationId = 0;
    private String clearButtonName = "Clear all notifications.";
    private PowerManager pm;

    private NotificationsService mService;
    boolean mBound = false;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service)
        {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NotificationsService.LocalBinder binder = (NotificationsService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            mBound = false;
        }
    };

    @Override
    protected void onServiceConnected()
    {
        Log.d(TAG,"NiLSAccessibilityService:onServiceConnected");

        // create a notification parser
        parser = new NotificationParser(getApplicationContext());

        findClearAllButton();

        // Bind to NotificationsService
        Intent intent = new Intent(this, NotificationsService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG,"NiLSAccessibilityService:onDestroy");

        // Unbind from the service
        if (mBound)
        {
            unbindService(mConnection);
            mBound = false;
        }

        super.onDestroy();
    }

    private void findClearAllButton()
    {
        // find "clear all notifications." button text
        Resources res;
        try
        {
            res = getPackageManager().getResourcesForApplication("com.android.systemui");
            int i = res.getIdentifier("accessibility_clear_all", "string", "com.android.systemui");
            if (i!=0)
            {
                clearButtonName = res.getString(i);
            }
        }
        catch (Exception exp)
        {
            Toast.makeText(this, R.string.failed_to_monitor_clear_button, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent)
    {
        boolean newApi = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        switch(accessibilityEvent.getEventType())
        {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                if (accessibilityEvent.getParcelableData() != null &&
                    accessibilityEvent.getParcelableData() instanceof Notification &&
                        !newApi)
                    {
                        Notification n = (Notification) accessibilityEvent.getParcelableData();
                        String packageName = accessibilityEvent.getPackageName().toString();
                        int id = notificationId++;
                        Log.d(TAG,"NotificationsListener:onNotificationPosted #" + id);
                        if (!mBound)
                            Log.e(TAG, "Notifications Service is not bounded. stop and restart NiLS on Accessibility Services to rebind it");
                        else
                            mService.onNotificationPosted(n, packageName, id, null, false);
                    }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if (accessibilityEvent.getPackageName() != null)
                {
                    String packageName = accessibilityEvent.getPackageName().toString();
                    Log.d(TAG, "window state has been changed:" + packageName);
                    // auto clear notifications when app is opened (Android < 4.3 only)
                    if (!newApi) {
                        if (!packageName.equals("com.android.systemui") &&
                                SettingsManager.shouldClearWhenAppIsOpened(getApplicationContext())) {
                            NotificationsProvider ns = NotificationsService.getSharedInstance();
                            if (ns != null)
                                ns.clearNotificationsForApps(new String[]{packageName.toString()});
                        }
                    }
                    handleAutoHideWhenWindowChanged(packageName);
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                // auto clear notifications when cleared from notifications bar (old api, Android < 4.3)
                if (accessibilityEvent.getPackageName().equals("com.android.systemui") &&
                    SettingsManager.shouldClearWhenClearedFromNotificationsBar(getApplicationContext()) &&
                        !newApi)
                {
                    //Log.d(TAG,"SystemUI content changed. windowid:"+event.getWindowId()+" source:"+event.getSource());
                    AccessibilityNodeInfo node = accessibilityEvent.getSource();

                    NotificationsProvider ns = NotificationsService.getSharedInstance();
                    if (ns != null && node != null)
                    {
                        if (hasClickables(node))
                        {
                            HashMap<Integer, NotificationData> notificationsToKeep = new HashMap<Integer, NotificationData>();
                            ArrayList<NotificationData> notificationsToRemove = new ArrayList<NotificationData>();

                            // find which notifications still appear on the status bar
                            List<String> titles = recursiveGetStrings(node);
                            for(String title: titles)
                            {
                                for (NotificationData nd : ns.getNotifications())
                                {
                                    if (nd.title != null &&
                                        title != null &&
                                        nd.title.toString().equals(title.toString()))
                                    {
                                        notificationsToKeep.put(nd.id, nd);
                                    }
                                }
                            }

                            // finding notifications to be cleared
                            if (ns.getNotifications().size()!= notificationsToKeep.size())
                            {
                                Iterator<NotificationData> iter = ns.getNotifications().iterator();
                                while(iter.hasNext())
                                {
                                    NotificationData nd = iter.next();
                                    if (!notificationsToKeep.containsKey(nd.id))
                                    {
                                        notificationsToRemove.add(nd);
                                    }
                                }
                            }

                            // clear all notifications that didn't appear on status bar
                            for(NotificationData nd : notificationsToRemove)
                            {
                                if (!mBound)
                                    Log.e(TAG, "Notifications Service is not bounded. stop and restart NiLS on Accessibility Services to rebind it");
                                else
                                    mService.onNotificationRemoved(null, nd.packageName, nd.id, null);
                            }
                        }
                    }
                }
                // auto hide notifications list
                handleAutoHideWhenWindowContentChanged(accessibilityEvent);

                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                if (    accessibilityEvent != null &&
                        accessibilityEvent.getPackageName() != null &&
                        accessibilityEvent.getPackageName().equals("com.android.systemui") &&
                        !newApi)
                {
                    // clear notifications button clicked
                    if (prefs != null && SettingsManager.shouldClearWhenClearedFromNotificationsBar(getApplicationContext()))
                    {
                        if (accessibilityEvent.getClassName() != null &&
                                accessibilityEvent.getClassName().equals(android.widget.ImageView.class.getName()) &&
                                accessibilityEvent.getContentDescription() != null &&
                                accessibilityEvent.getContentDescription().equals(clearButtonName))
                            {
                                if (!mBound)
                                    Log.e(TAG, "Notifications Service is not bounded. stop and restart NiLS on Accessibility Services to rebind it");
                                else
                                    mService.clearAllNotifications();
                            }
                    }
                }
                break;
        }
    }

    private boolean mLocked = false;

    private void handleAutoHideWhenWindowChanged(String packageName)
    {
        // systemui is not really a window - ignore it
        if (packageName.equals("com.android.systemui") || packageName.equals(getPackageName()))  return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (packageName != null && mBound)
        {
            // stock lock screen detection
            if (packageName.equals("android") && SysUtils.isKeyguardLocked(this))
                packageName = SettingsManager.STOCK_LOCKSCREEN_PACKAGENAME;

            boolean dontHide = prefs.getBoolean(SettingsManager.DONT_HIDE, SettingsManager.DEFAULT_DONT_HIDE);
            boolean shouldHide = NotificationsService.shouldHideNotifications(getApplicationContext(), packageName.toString(), false);
            boolean isPackageInstaller = packageName.equals("com.android.packageinstaller");

            // request notifications list to hide/show notifications list
            // force hide when package installer is displayed
            if (isPackageInstaller)
            {
                mService.hide(true);
            }
            else if (shouldHide)
            {
                if (!dontHide)
                {
                    mService.hide(false);
                }
            }
            else
            {
                mService.show(false);
            }

            if (mLocked && shouldHide && pm.isScreenOn())
            {
                Log.d(TAG, "device is not not locked - sending UNLOCKED event, package name:"+packageName);
                sendBroadcast(new Intent(NotificationsService.DEVICE_UNLOCKED));
            }

            // store new locked status
            if (shouldHide)
                mLocked = false;
            else
                mLocked = true;

            prefs.edit().putString(LAST_OPENED_WINDOW_PACKAGENAME, packageName).commit();
        }
    }

    private String lastWindowContentPackageName = "";
    private void handleAutoHideWhenWindowContentChanged(AccessibilityEvent accessibilityEvent)
    {
        CharSequence packageName = accessibilityEvent.getPackageName();

        if (packageName != null && mBound)
        {
            if (!packageName.toString().equals(lastWindowContentPackageName))
            {
                lastWindowContentPackageName = packageName.toString();
                Log.d(TAG,"window content has been changed:" + lastWindowContentPackageName);
            }

            //if (packageName.equals("android")) packageName = SettingsManager.STOCK_LOCKSCREEN_PACKAGENAME;

            // hide FP when WidgetLocker side menu appears
            if (packageName.equals(NotificationsService.WIDGET_LOCKER_PACKAGENAME) &&
                    accessibilityEvent.getSource() != null &&
                    accessibilityEvent.getClassName().equals("android.widget.ListView"))
            {
                Rect rect = new Rect();
                accessibilityEvent.getSource().getBoundsInScreen(rect);
                if (rect.left >= -BitmapUtils.dpToPx(60))
                {
                    Log.d(TAG,"window content has been changed:" + packageName.toString());
                    mService.hide(false);
                }
                else
                {
                    Log.d(TAG,"window content has been changed:" + packageName.toString());
                    mService.show(false);
                }
            }

            // show NiLS back the lock screen app is displayed back
            else if (mHiddenBecauseOfSystemUI &&
                    !NotificationsService.shouldHideNotifications(getApplicationContext(), packageName.toString(), false))
            {
                mHiddenBecauseOfSystemUI = false;
                Log.d(TAG,"window content has been changed:" + packageName.toString());
                mService.show(false);
            }

            // hide NiLS when status bar or power menu are displayed
            else if (packageName.equals("com.android.systemui") /*|| packageName.equals("android")*/)
            {
                mHiddenBecauseOfSystemUI = true;
                Log.d(TAG,"window content has been changed:" + packageName.toString());
                mService.hide(false);
            }
        }
    }

    private List<String> recursiveGetStrings(AccessibilityNodeInfo node)
    {
        ArrayList<String> strings = new ArrayList<String>();
        if (node!= null)
        {
            if (node.getText()!=null)
                strings.add(node.getText().toString());
            for(int i=0;i<node.getChildCount();i++)
            {
                strings.addAll(recursiveGetStrings(node.getChild(i)));
            }
        }
        return strings;
    }

    private boolean hasClickables(AccessibilityNodeInfo node)
    {
        if (node != null && node.isClickable())
            return true;
        else
        {
            boolean hasClickables = false;
            if (node != null)
                for(int i=0;i<node.getChildCount();i++)
                {
                    if (hasClickables(node.getChild(i))) hasClickables = true;
                }
            return hasClickables;
        }
    }

    @Override
    public void onInterrupt()
    {
    }

    private boolean mHiddenBecauseOfSystemUI = false;

    @Override
    public boolean onUnbind(Intent intent)
    {
        // Unbind from the service
        if (mBound)
        {
            unbindService(mConnection);
            mBound = false;
        }
        return super.onUnbind(intent);
    }

    public static boolean isServiceRunning(Context context)
    {
        return SysUtils.isServiceRunning(context, NiLSAccessibilityService.class);
    }
}
