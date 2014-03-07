package com.roymam.android.notificationswidget;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class NiLSAccessibilityService extends AccessibilityService
{
    private NotificationParser parser;
    private int notificationId = 0;
    private String clearButtonName = "Clear all notifications.";

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
        Log.d("NiLS","NiLSAccessibilityService:onServiceConnected");

        // start NotificationsService
        //Intent intent = new Intent(getApplicationContext(), NotificationsService.class);
        //getApplicationContext().startService(intent);

        // create a notification parser
        parser = new NotificationParser(getApplicationContext());

        findClearAllButton();

        // Bind to NotificationsService
        Intent intent = new Intent(this, NotificationsService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy()
    {
        Log.d("NiLS","NiLSAccessibilityService:onDestroy");

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
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        switch(accessibilityEvent.getEventType())
        {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                if (accessibilityEvent.getParcelableData() != null &&
                    accessibilityEvent.getParcelableData() instanceof Notification)
                    {
                        Notification n = (Notification) accessibilityEvent.getParcelableData();
                        String packageName = accessibilityEvent.getPackageName().toString();
                        int id = notificationId++;
                        Log.d("NiLS","NewNotificationsListener:onNotificationPosted #" + id);
                        mService.onNotificationPosted(n, packageName, id, null);
                    }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if (accessibilityEvent.getPackageName() != null)
                {
                    Log.d("NiLS", "TYPE_WINDOW_STATE_CHANGED " + accessibilityEvent.getPackageName().toString());
                    if (!accessibilityEvent.getPackageName().equals("com.android.systemui") &&
                         SettingsActivity.shouldClearWhenAppIsOpened(getApplicationContext()))
                    {
                        NotificationsProvider ns = NotificationsService.getSharedInstance();
                        if (ns != null) ns.clearNotificationsForApps(new String[]{accessibilityEvent.getPackageName().toString()});
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                if (accessibilityEvent.getPackageName().equals("com.android.systemui") &&
                    SettingsActivity.shouldClearWhenClearedFromNotificationsBar(getApplicationContext()))
                {
                    //Log.d("NiLS","SystemUI content changed. windowid:"+event.getWindowId()+" source:"+event.getSource());
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
                                    if (nd.title.toString().equals(title.toString()))
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
                                mService.onNotificationRemoved(null, nd.packageName, nd.id);
                            }
                        }
                    }
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                if (    accessibilityEvent != null &&
                        accessibilityEvent.getPackageName() != null &&
                        accessibilityEvent.getPackageName().equals("com.android.systemui"))
                {
                    // clear notifications button clicked
                    if (prefs != null && SettingsActivity.shouldClearWhenClearedFromNotificationsBar(getApplicationContext()))
                    {
                        if (accessibilityEvent.getClassName() != null &&
                                accessibilityEvent.getClassName().equals(android.widget.ImageView.class.getName()) &&
                                accessibilityEvent.getContentDescription() != null &&
                                accessibilityEvent.getContentDescription().equals(clearButtonName))
                            {
                                mService.clearAllNotifications();
                            }
                    }
                }
                break;
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
}
