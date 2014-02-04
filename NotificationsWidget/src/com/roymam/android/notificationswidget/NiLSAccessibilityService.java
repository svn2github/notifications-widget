package com.roymam.android.notificationswidget;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
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

    @Override
    protected void onServiceConnected()
    {
        Log.d("NiLS","NiLSAccessibilityService:onServiceConnected");

        // start NotificationsService
        Intent intent = new Intent(getApplicationContext(), NotificationsService.class);
        getApplicationContext().startService(intent);

        // create a notification parser
        parser = new NotificationParser(getApplicationContext());

        findClearAllButton();

        // notify that the service has been started
        getApplicationContext().sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_READY));
    }

    @Override
    public void onDestroy()
    {
        Log.d("NiLS","NiLSAccessibilityService:onDestroy");
        getApplicationContext().sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_DIED));
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

                        if (!parser.isPersistent(n, packageName))
                        {
                            Log.d("NiLS","NewNotificationsListener:onNotificationPosted #" + id);

                            Intent intent = new Intent(getApplicationContext(), NotificationsService.class);
                            intent.setAction(NotificationsService.NOTIFICATION_POSTED);
                            NotificationData notification = parser.parseNotification(n, packageName, id, null);
                            intent.putExtra(NotificationsService.EXTRA_NOTIFICATION, notification);
                            getApplicationContext().startService(intent);
                        }
                        else
                        {
                            PersistentNotification pn = parser.parsePersistentNotification(n, packageName, 0);

                            Intent intent = new Intent(getApplicationContext(), NotificationsService.class);
                            intent.setAction(NotificationsService.PERSISTENT_NOTIFICATION_POSTED);
                            intent.putExtra(NotificationsService.EXTRA_NOTIFICATION, pn);
                            getApplicationContext().startService(intent);
                        }
                    }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if (accessibilityEvent.getPackageName() != null)
                {
                    Log.d("NiLS", "TYPE_WINDOW_STATE_CHANGED " + accessibilityEvent.getPackageName().toString());
                    if (!accessibilityEvent.getPackageName().equals("com.android.systemui") &&
                         prefs.getBoolean(SettingsActivity.CLEAR_APP_NOTIFICATIONS, true))
                    {
                        NotificationsProvider ns = NotificationsService.getSharedInstance();
                        if (ns != null) ns.clearNotificationsForApps(new String[]{accessibilityEvent.getPackageName().toString()});
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                if (accessibilityEvent.getPackageName().equals("com.android.systemui") &&
                    !prefs.getString(SettingsActivity.SYNC_NOTIFICATIONS, SettingsActivity.SYNC_NOTIFICATIONS_ONEWAY).equals(SettingsActivity.SYNC_NOTIFICATIONS_DISABLED))
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
                                Intent intent = new Intent(getApplicationContext(), NotificationsService.class);
                                intent.setAction(NotificationsService.NOTIFICATION_REMOVED);
                                intent.putExtra(NotificationsService.EXTRA_PACKAGENAME, nd.packageName);
                                intent.putExtra(NotificationsService.EXTRA_ID, nd.id);
                                getApplicationContext().startService(intent);
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
                    if (prefs != null &&
                        !prefs.getString(SettingsActivity.SYNC_NOTIFICATIONS, SettingsActivity.SYNC_NOTIFICATIONS_ONEWAY).equals(SettingsActivity.SYNC_NOTIFICATIONS_DISABLED))
                    {
                        if (accessibilityEvent.getClassName() != null &&
                                accessibilityEvent.getClassName().equals(android.widget.ImageView.class.getName()) &&
                                accessibilityEvent.getContentDescription() != null &&
                                accessibilityEvent.getContentDescription().equals(clearButtonName))
                            {
                                clearAllNotifications();
                            }
                    }
                }
                break;
        }
    }

    public void clearAllNotifications()
    {
        NotificationsProvider ns = NotificationsService.getSharedInstance();
        if (ns != null) ns.clearAllNotifications();
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
