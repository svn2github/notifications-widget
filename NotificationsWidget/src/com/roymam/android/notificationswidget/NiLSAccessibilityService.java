package com.roymam.android.notificationswidget;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
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

public class NiLSAccessibilityService extends AccessibilityService implements NotificationsProvider
{
    public static final String SHOW_NOTIFICATIONS = "com.roymam.android.nils.show_notifications";
    public static final String HIDE_NOTIFICATIONS = "com.roymam.android.nils.hide_notifications";

    private ArrayList<NotificationData> notifications = new ArrayList<NotificationData>();
    private NotificationParser parser;
    private int notificationId = 0;
    private String clearButtonName = "Clear all notifications.";

    private static NotificationsProvider sSharedInstance = null;
    private NotificationEventListener listener = null;

    public static NotificationsProvider getSharedInstance() { return sSharedInstance; }

    @Override
    protected void onServiceConnected()
    {
        parser = new NotificationParser(getApplicationContext());
        findClearAllButton();
        sSharedInstance = this;
        // notify that the service has been started
        getApplicationContext().sendBroadcast(new Intent(NotificationsProvider.ACTION_SERVICE_READY));
    }

    @Override
    public void onDestroy()
    {
        sSharedInstance = null;
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

                        if (!parser.isPersistent(n, packageName))
                        {
                            NotificationData nd = parser.parseNotification(n, packageName, notificationId, null);
                            if (nd != null)
                            {
                                // check for duplicated notification
                                boolean keepOnlyLastNotification = prefs.getBoolean(nd.packageName+"."+AppSettingsActivity.KEEP_ONLY_LAST, false);
                                int duplicated = -1;
                                for(int i=0;i<notifications.size();i++)
                                {
                                    CharSequence title1 = nd.title;
                                    CharSequence title2 = notifications.get(i).title;
                                    CharSequence text1 = nd.text;
                                    CharSequence text2 = notifications.get(i).text;
                                    CharSequence content1 = nd.content;
                                    CharSequence content2 = notifications.get(i).content;
                                    boolean titlesdup = (title1 != null && title2 != null && title1.toString().equals(title2.toString()) || title1 == null && title2 == null);
                                    boolean textdup = (text1 != null && text2 != null && text1.toString().startsWith(text2.toString()) || text1 == null && text2 == null);
                                    boolean contentsdup = (content1 != null && content2 != null && content1.toString().startsWith(content2.toString())  || content1 == null && content2 == null);
                                    boolean allDup = titlesdup && textdup && contentsdup;

                                    if (nd.packageName.equals(notifications.get(i).packageName) &&
                                            (allDup || keepOnlyLastNotification))
                                    {
                                        duplicated = i;
                                    }
                                }
                                if (duplicated >= 0)
                                {
                                    NotificationData dup = notifications.get(duplicated);
                                    nd.id = dup.id;
                                    nd.uid = dup.uid;
                                    nd.pinned = dup.pinned;
                                    notifications.remove(duplicated);
                                }

                                notifications.add(nd);
                                if (listener != null)
                                {
                                    if (duplicated >= 0)
                                        listener.onNotificationUpdated(nd);
                                    else
                                        listener.onNotificationAdded(nd, true);
                                    listener.onNotificationsListChanged();
                                }

                                notificationId++;
                            }
                        }
                        else
                        {
                            PersistentNotification pn = parser.parsePersistentNotification(n, packageName, notificationId);
                            if (pn != null)
                            {
                                persistentNotifications.put(packageName, pn);
                                if (listener != null) listener.onPersistentNotificationAdded(pn);
                            }
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

                    if (node != null)
                    {
                        if (hasClickables(node))
                        {
                            HashMap<Integer, NotificationData> notificationsToKeep = new HashMap<Integer, NotificationData>();

                            List<String> titles = recursiveGetStrings(node);
                            for(String title: titles)
                            {
                                //Log.d("NiLS","Notification Title:"+ title);
                                for (NotificationData nd : notifications)
                                {
                                    if (nd.title.toString().equals(title.toString()))
                                    {
                                        notificationsToKeep.put(nd.id, nd);
                                    }
                                }
                            }

                            if (notifications.size()!= notificationsToKeep.size())
                            {
                                Iterator<NotificationData> iter = notifications.iterator();
                                while(iter.hasNext())
                                {
                                    NotificationData nd = iter.next();
                                    if (!notificationsToKeep.containsKey(nd.id))
                                    {
                                        if (listener != null) listener.onNotificationCleared(nd);
                                        iter.remove();
                                    }
                                }

                                if (listener != null) listener.onNotificationsListChanged();
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
        Iterator<NotificationData> i = notifications.iterator();
        while (i.hasNext())
        {
            NotificationData nd = i.next();
            if (!nd.pinned)
            {
                if (listener != null) listener.onNotificationCleared(nd);
                i.remove();
            }
        }

        if (listener != null) listener.onNotificationsListChanged();
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

    public void clearNotificationsForApps(String[] pkgList)
    {
        boolean changed = false;
        for(String packageName : pkgList)
        {
            Iterator<NotificationData> i = notifications.iterator();
            while (i.hasNext())
            {
                NotificationData nd = i.next();
                if (!nd.pinned && nd.packageName.equals(packageName))
                {
                    if (listener != null) listener.onNotificationCleared(nd);
                    i.remove();
                    changed = true;
                }
            }
        }
        if (changed)
        {
            if (listener != null) listener.onNotificationsListChanged();
        }
    }

    @Override
    public void clearNotification(int uid)
    {
        // first, find it on list
        Iterator<NotificationData> iter = notifications.iterator();
        boolean removed = false;
        while (iter.hasNext() && !removed)
        {
            NotificationData nd = iter.next();
            if (nd.uid == uid)
            {
                iter.remove();
                removed = true;
                if (listener != null) listener.onNotificationCleared(nd);
            }
        }
        if (removed && listener != null) listener.onNotificationsListChanged();
    }

    @Override
    public void onInterrupt()
    {
    }

    @Override
    public List<NotificationData> getNotifications()
    {
        return notifications;
    }

//    @Override
//    public void clearNotification(String packageName, int notificationId)
//    {
//        Iterator<NotificationData> iter = notifications.iterator();
//
//        boolean changed = false;
//
//        while (iter.hasNext())
//        {
//            NotificationData nd = iter.next();
//            if (nd.id == notificationId)
//            {
//                if (listener != null) listener.onNotificationCleared(nd);
//                iter.remove();
//                changed = true;
//            }
//        }
//
//        if (changed && listener != null) listener.onNotificationsListChanged();
//    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        if (listener != null) listener.onServiceStopped();
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent)
    {
        if (listener != null) listener.onServiceStarted();
        super.onRebind(intent);
    }

    @Override
    public void setNotificationEventListener(NotificationEventListener listener)
    {
        this.listener = listener;
    }

    @Override
    public NotificationEventListener getNotificationEventListener()
    {
        return listener;
    }

    // persistent notifications - currently unavailable feature
    private HashMap<String, PersistentNotification> persistentNotifications = new HashMap<String, PersistentNotification>();

    @Override
    public HashMap<String, PersistentNotification> getPersistentNotifications()
    {
        return persistentNotifications;
    }

    // this feature was removed
    private void monitorRunningApps()
    {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> l = am.getRunningAppProcesses();
        Iterator<ActivityManager.RunningAppProcessInfo> i = l.iterator();
        ArrayList<String> runningApps = new ArrayList<String>();
        while(i.hasNext())
        {
            ActivityManager.RunningAppProcessInfo info = i.next();
            if (info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE)
                for(String packageName : info.pkgList)
                    runningApps.add(packageName);
        }
        purgePersistentNotifications(runningApps);
    }

    public void purgePersistentNotifications(ArrayList<String> runningApps)
    {
        HashMap<String,PersistentNotification> newPN = new HashMap<String, PersistentNotification>();
        for(String packageName : runningApps)
        {
            if (persistentNotifications.containsKey(packageName))
            {
                newPN.put(packageName, persistentNotifications.get(packageName));
            }
        }
        if (newPN.size()!=persistentNotifications.size())
        {
            persistentNotifications = newPN;
            if (listener != null) listener.onNotificationsListChanged();
        }
    }
    ////////////////
}
