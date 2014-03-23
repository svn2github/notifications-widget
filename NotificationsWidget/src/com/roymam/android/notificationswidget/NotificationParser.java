package com.roymam.android.notificationswidget;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.roymam.android.common.BitmapCache;
import com.roymam.android.common.IconPackManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NotificationParser
{
    private final Context context;
    public int notification_image_id = 0;
    public int notification_title_id = 0;
    public int notification_text_id = 0;
    public int notification_info_id = 0;
    public int notification_subtext_id = 0;
    public int big_notification_summary_id = 0;
    public int big_notification_title_id = 0;
    public int big_notification_content_title = 0;
    public int big_notification_content_text = 0;
    public int inbox_notification_title_id = 0;
    public int inbox_notification_event_1_id = 0;
    public int inbox_notification_event_2_id = 0;
    public int inbox_notification_event_3_id = 0;
    public int inbox_notification_event_4_id = 0;
    public int inbox_notification_event_5_id = 0;
    public int inbox_notification_event_6_id = 0;
    public int inbox_notification_event_7_id = 0;
    public int inbox_notification_event_8_id = 0;
    public int inbox_notification_event_9_id = 0;
    public int inbox_notification_event_10_id = 0;
    private int mInboxLayoutId = 0;
    private int mBigTextLayoutId = 0;

    public NotificationParser(Context context)
    {
        this.context = context;
        detectNotificationIds();
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public List<NotificationData> parseNotification(Notification n, String packageName, int notificationId, String tag)
    {
        if (n != null)
        {
            // handle only dismissable notifications
            if (!isPersistent(n, packageName) && !shouldIgnore(n, packageName))
            {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

                // build notification data object
                NotificationData nd = new NotificationData();

                float maxIconSize = context.getResources().getDimension(R.dimen.max_icon_size);

                // extract notification & app icons
                Resources res;
                PackageInfo info;
                ApplicationInfo ai;
                try
                {
                    res = context.getPackageManager().getResourcesForApplication(packageName);
                    info = context.getPackageManager().getPackageInfo(packageName,0);
                    ai = context.getPackageManager().getApplicationInfo(packageName,0);
                }
                catch(PackageManager.NameNotFoundException e)
                {
                    info = null;
                    res = null;
                    ai = null;
                }

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.outHeight = (int) maxIconSize;
                opts.outWidth = (int) maxIconSize;

                String notificationIcon = sharedPref.getString(packageName + "." + SettingsActivity.NOTIFICATION_ICON,
                                        sharedPref.getString(SettingsActivity.NOTIFICATION_ICON, SettingsActivity.DEFAULT_NOTIFICATION_ICON));

                if (res != null && info != null)
                {
                    nd.appicon = BitmapCache.getInstance(context).getBitmap(packageName, n.icon);
                    if (notificationIcon.equals(SettingsActivity.NOTIFICATION_MONO_ICON))
                    {
                        nd.icon = nd.appicon;
                    }
                    else
                    {
                        nd.icon = BitmapCache.getInstance(context).getBitmap(packageName, info.applicationInfo.icon);
                    }

                    String iconPack = sharedPref.getString(SettingsActivity.ICON_PACK, SettingsActivity.DEFAULT_ICON_PACK);
                    if (!iconPack.equals(SettingsActivity.DEFAULT_ICON_PACK))
                    {
                        // load app icon from icon pack
                        IconPackManager.IconPack ip = IconPackManager.getInstance(context).getAvailableIconPacks(false).get(iconPack);
                        nd.icon = ip.getIconForPackage(packageName, nd.icon);
                    }
                    if (nd.appicon == null)
                    {
                        nd.appicon = nd.icon;
                    }
                }
                if (n.largeIcon != null && notificationIcon.equals(SettingsActivity.NOTIFICATION_ICON))
                {
                    nd.icon = n.largeIcon;
                }

                // get time of the event
                if (n.when != 0)
                    nd.received = n.when;
                else
                    nd.received = System.currentTimeMillis();

                nd.action = n.contentIntent;
                nd.count = 1;
                nd.packageName = packageName;

                // if possible - try to extract actions from expanded notification
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                {
                    nd.actions = getActionsFromNotification(context, n, packageName);
                }

                // extract expanded text
                nd.text = null;
                nd.title = null;
                if (sharedPref.getBoolean(nd.packageName+"."+AppSettingsActivity.USE_EXPANDED_TEXT, sharedPref.getBoolean(AppSettingsActivity.USE_EXPANDED_TEXT, true)))
                {
                    getExpandedText(n,nd);
                    // replace text with content if no text
                    if (nd.text == null || nd.text.equals("") &&
                            nd.content != null && !nd.content.equals(""))
                    {
                        nd.text = nd.content;
                        nd.content = null;
                    }
                    // keep only text if it's duplicated
                    if (nd.text != null && nd.content != null && nd.text.toString().equals(nd.content.toString()))
                    {
                        nd.content = null;
                    }
                }

                    // use default notification text & title - if no info found on expanded notification
                    if (nd.text == null)
                    {
                        nd.text = n.tickerText;
                    }
                    if (nd.title == null)
                    {
                        if (nd.text == null)
                        {
                            // if both text and title are null - that's non imformative notification - ignore it
                            return new ArrayList<NotificationData>();
                        }
                        if (info != null)
                            nd.title = context.getPackageManager().getApplicationLabel(ai);
                        else
                            nd.title = packageName;
                    }

                    nd.id = notificationId;
                    nd.tag = tag;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        nd.priority = getPriority(n);
                    }
                    else
                    {
                        nd.priority = 0;
                    }
                    int apppriority = Integer.parseInt(sharedPref.getString(nd.packageName+"."+AppSettingsActivity.APP_PRIORITY, "-9"));
                    if (apppriority != -9) nd.priority = apppriority;

                    // check if this is a multiple events notificatio
                    String notificationMode = SettingsActivity.getNotificationMode(context, packageName);

                    List<NotificationData> notifications = new ArrayList<NotificationData>();
                    notifications.add(nd);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && notificationMode.equals(SettingsActivity.MODE_SEPARATED))
                    {
                        RemoteViews bigContentView = n.bigContentView;
                        if (bigContentView != null &&
                            bigContentView.getLayoutId() == mInboxLayoutId)
                        {
                            List<NotificationData> separatedNotifications = getMultipleNotificationsFromInboxView(n.bigContentView, nd);
                            // make sure we've at least one notification
                            if (separatedNotifications.size() > 0) notifications = separatedNotifications;
                        }
                    }
                    return notifications;
                }
        }
        return new ArrayList<NotificationData>();
    }

    private List<NotificationData> getMultipleNotificationsFromInboxView(RemoteViews bigContentView, NotificationData baseNotification)
    {
        ArrayList<NotificationData> notifications = new ArrayList<NotificationData>();
        HashMap<Integer, CharSequence> strings = getNotificationStringFromRemoteViews(bigContentView);

        // build event list from notification content
        ArrayList<CharSequence> events = new ArrayList<CharSequence>();
        if (strings.containsKey(inbox_notification_event_10_id)) events.add(strings.get(inbox_notification_event_10_id));
        if (strings.containsKey(inbox_notification_event_9_id)) events.add(strings.get(inbox_notification_event_9_id));
        if (strings.containsKey(inbox_notification_event_8_id)) events.add(strings.get(inbox_notification_event_8_id));
        if (strings.containsKey(inbox_notification_event_7_id)) events.add(strings.get(inbox_notification_event_7_id));
        if (strings.containsKey(inbox_notification_event_6_id)) events.add(strings.get(inbox_notification_event_6_id));
        if (strings.containsKey(inbox_notification_event_5_id)) events.add(strings.get(inbox_notification_event_5_id));
        if (strings.containsKey(inbox_notification_event_4_id)) events.add(strings.get(inbox_notification_event_4_id));
        if (strings.containsKey(inbox_notification_event_3_id)) events.add(strings.get(inbox_notification_event_3_id));
        if (strings.containsKey(inbox_notification_event_2_id)) events.add(strings.get(inbox_notification_event_2_id));
        if (strings.containsKey(inbox_notification_event_1_id)) events.add(strings.get(inbox_notification_event_1_id));

        // create a notification for each event
        for(CharSequence event : events)
        {
            NotificationData nd = new NotificationData();
            nd.icon = baseNotification.icon;
            nd.appicon = baseNotification.appicon;
            nd.id = baseNotification.id;
            nd.packageName = baseNotification.packageName;
            nd.pinned = baseNotification.pinned;
            nd.priority = baseNotification.priority;
            nd.tag = baseNotification.tag;
            nd.received = baseNotification.received;
            nd.action = baseNotification.action;
            nd.content = baseNotification.content;
            nd.title = strings.get(notification_title_id);
            nd.event = true;
            nd.text = event;

            // extract title from content for first/last event
            if (event != null)
            {
                // TODO: extract time from the start of the text (if available - e.g Textra, Businins calendar)

                SpannableStringBuilder ssb = new SpannableStringBuilder(event);
                // try to split it by text style
                TextAppearanceSpan[] spans = ssb.getSpans(0, event.length(), TextAppearanceSpan.class);
                if (spans.length == 2)
                {
                    int s0start = ssb.getSpanStart(spans[0]);
                    int s0end = ssb.getSpanEnd(spans[0]);
                    nd.title = event.subSequence(s0start, s0end).toString();
                    int s1start = ssb.getSpanStart(spans[1]);
                    int s1end = ssb.getSpanEnd(spans[1]);
                    nd.text = event.subSequence(s1start, s1end).toString();

                }
                else
                {
                    // try to split it by ":" delimiter
                    String[] parts = event.toString().split(":", 2);
                    if (parts.length == 2 && parts[1].length()>2) // parts[1].length()>2 special exception for missed calls time 
                    {
                        nd.title = parts[0];
                        nd.text = parts[1];
                    }
                }
                notifications.add(nd);
            }
        }
        return notifications;
    }

    private boolean shouldIgnore(Notification n, String packageName)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (!prefs.getBoolean(SettingsActivity.COLLECT_ON_UNLOCK, true) &&
            !km.inKeyguardRestrictedInputMode() &&
            !prefs.getBoolean("widgetlocker", false) ||
                prefs.getBoolean(packageName + "." + AppSettingsActivity.IGNORE_APP, false) ||
                packageName.equals("com.android.providers.downloads"))
            return true;
        return false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private int getPriority(Notification n)
    {
        return n.priority;
    }

    // extract actions from notification
    private NotificationData.Action[] getActionsFromNotification(Context context, Notification n, String packageName)
    {
        ArrayList<NotificationData.Action> returnActions = new ArrayList<NotificationData.Action>();
        try
        {
            Object[] actions = null;
            Field fs = n.getClass().getDeclaredField("actions");
            if (fs != null)
            {
                fs.setAccessible(true);
                actions = (Object[]) fs.get(n);
            }
            if (actions != null)
            {
                for(int i=0; i<actions.length; i++)
                {
                    NotificationData.Action a = new NotificationData.Action();
                    Class<?> actionClass=Class.forName("android.app.Notification$Action");
                    a.icon = actionClass.getDeclaredField("icon").getInt(actions[i]);
                    a.title = (CharSequence) actionClass.getDeclaredField("title").get(actions[i]);;
                    a.actionIntent = (PendingIntent) actionClass.getDeclaredField("actionIntent").get(actions[i]);;

                    // find drawable
                    // extract app icons
                    a.drawable = BitmapCache.getInstance(context).getBitmap(packageName, a.icon);
                    returnActions.add(a);
                }
            }
        }
        catch(Exception exp)
        {

        }
        NotificationData.Action[] returnArray = new NotificationData.Action[returnActions.size()];
        returnActions.toArray(returnArray);
        return returnArray;
    }

    private void getExpandedText(Notification n, NotificationData nd)
    {
        RemoteViews view = n.contentView;

        // first get information from the original content view
        extractTextFromView(view, nd);

        // then try get information from the expanded view
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            view = getBigContentView(n);
            extractTextFromView(view, nd);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private RemoteViews getBigContentView(Notification n)
    {
        if (n.bigContentView == null)
            return n.contentView;
        else
        {
            return n.bigContentView;
        }
    }

    private void extractTextFromView(RemoteViews view, NotificationData nd)
    {
        CharSequence title = null;
        CharSequence text = null;
        CharSequence content = null;

        HashMap<Integer, CharSequence> notificationStrings;
        notificationStrings = getNotificationStringFromRemoteViews(view);

        if (notificationStrings.size() > 0)
        {
            // try to get big text
            if (notificationStrings.containsKey(big_notification_content_text))
            {
                text = notificationStrings.get(big_notification_content_text);
            }

            // get title string if available
            if (notificationStrings.containsKey(notification_title_id))
            {
                title = notificationStrings.get(notification_title_id);
            } else if (notificationStrings.containsKey(big_notification_title_id))
            {
                title = notificationStrings.get(big_notification_title_id);
            } else if (notificationStrings.containsKey(inbox_notification_title_id))
            {
                title =  notificationStrings.get(inbox_notification_title_id);
            }

            // try to extract details lines
            content = null;
            CharSequence firstEventStr = null;
            CharSequence lastEventStr = null;

            if (notificationStrings.containsKey(inbox_notification_event_1_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_1_id);
                if (!s.equals(""))
                {
                    firstEventStr = s;
                    content = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_2_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_2_id);
                if (!s.equals(""))
                {
                    content = TextUtils.concat(content, "\n", s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_3_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_3_id);
                if (!s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_4_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_4_id);
                if (!s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_5_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_5_id);
                if (!s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_6_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_6_id);
                if (!s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_7_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_7_id);
                if (!s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_8_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_8_id);
                if (!s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_9_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_9_id);
                if (!s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_10_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_10_id);
                if (!s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            // if there is no text - make the text to be the content
            if (text == null || text.equals(""))
            {
                text = content;
                content = null;
            }

            // if no content lines, try to get subtext
            //if (content == null)
            //{
                if (notificationStrings.containsKey(notification_subtext_id))
                {
                    CharSequence s = notificationStrings.get(notification_subtext_id);

                    if (!s.equals(""))
                    {
                        if (content == null) content = s;
                        else content = content + "\n" + s;
                    }
                }
            //}
        }

        if (title!=null)
        {
            nd.title = title;
        }
        if (text != null)
        {
            nd.text = text;
        }
        if (content != null)
        {
            nd.content = content;
        }
    }

    /*


     */

    // use reflection to extract string from remoteviews object
    private HashMap<Integer, CharSequence> getNotificationStringFromRemoteViews(RemoteViews view)
    {
        HashMap<Integer, CharSequence> notificationText = new HashMap<Integer, CharSequence>();

        try
        {
            ArrayList<Object> actions = null;
            Field fs = view.getClass().getDeclaredField("mActions");
            if (fs != null)
            {
                fs.setAccessible(true);
                actions = (ArrayList<Object>) fs.get(view);
            }
            if (actions != null)
            {
                final int STRING = 9;
                final int CHAR_SEQUENCE = 10;

                for(Object action : actions)
                {
                    if (action.getClass().getName().equals("android.widget.RemoteViews$ReflectionAction"))
                    {
                        Class<?> reflectionActionClass=action.getClass();
                        Class<?> actionClass=Class.forName("android.widget.RemoteViews$Action");

                        Field methodNameField = reflectionActionClass.getDeclaredField("methodName");
                        Field typeField = reflectionActionClass.getDeclaredField("type");
                        Field valueField = reflectionActionClass.getDeclaredField("value");
                        Field viewIdField = actionClass.getDeclaredField("viewId");

                        methodNameField.setAccessible(true);
                        typeField.setAccessible(true);
                        valueField.setAccessible(true);
                        viewIdField.setAccessible(true);

                        String methodName = (String) methodNameField.get(action);
                        int type = typeField.getInt(action);
                        Object value = valueField.get(action);
                        int viewId = viewIdField.getInt(action);

                        if (type == CHAR_SEQUENCE)
                            notificationText.put(new Integer(viewId), (CharSequence) value);
                        else if (type == STRING)
                            notificationText.put(new Integer(viewId), (String) value);
                    }
                }
            }
        }
        catch(Exception exp)
        {
        }
        return notificationText;
    }

    // use localview to get strings
    private HashMap<Integer, CharSequence> getNotificationStringsFromView(ViewGroup localView)
    {
        HashMap<Integer, CharSequence> notificationStrings = new HashMap<Integer, CharSequence>();

        View v = localView.findViewById(notification_title_id);
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(notification_title_id, ((TextView) v).getText());
        }
        v = localView.findViewById(notification_text_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(notification_text_id , ((TextView) v).getText());
        }
        v = localView.findViewById(notification_info_id);
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(notification_info_id , ((TextView) v).getText());
        }
        v = localView.findViewById(notification_subtext_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(notification_subtext_id  , ((TextView) v).getText());
        }
        v = localView.findViewById(big_notification_summary_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(big_notification_summary_id, ((TextView) v).getText());
        }
        v = localView.findViewById(big_notification_title_id);
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(big_notification_title_id, ((TextView) v).getText());
        }
        v = localView.findViewById(big_notification_content_title );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(big_notification_content_title, ((TextView) v).getText());
        }
        v = localView.findViewById(big_notification_content_text);
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(big_notification_content_text , ((TextView) v).getText());
        }
        v = localView.findViewById(inbox_notification_title_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(inbox_notification_title_id , ((TextView) v).getText());
        }
        v = localView.findViewById(inbox_notification_event_1_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(inbox_notification_event_1_id  , ((TextView) v).getText());
        }
        v = localView.findViewById(inbox_notification_event_2_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(inbox_notification_event_2_id  , ((TextView) v).getText());
        }
        v = localView.findViewById(inbox_notification_event_3_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(inbox_notification_event_3_id  , ((TextView) v).getText());
        }
        v = localView.findViewById(inbox_notification_event_4_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(inbox_notification_event_4_id  , ((TextView) v).getText());
        }
        v = localView.findViewById(inbox_notification_event_5_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(inbox_notification_event_5_id  , ((TextView) v).getText());
        }
        v = localView.findViewById(inbox_notification_event_6_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(inbox_notification_event_6_id  , ((TextView) v).getText());
        }
        v = localView.findViewById(inbox_notification_event_7_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(inbox_notification_event_7_id  , ((TextView) v).getText());
        }
        v = localView.findViewById(inbox_notification_event_8_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(inbox_notification_event_8_id  , ((TextView) v).getText());
        }
        v = localView.findViewById(inbox_notification_event_9_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(inbox_notification_event_9_id  , ((TextView) v).getText());
        }
        v = localView.findViewById(inbox_notification_event_10_id );
        if (v != null && v instanceof TextView)
        {
            notificationStrings.put(inbox_notification_event_10_id  , ((TextView) v).getText());
        }

        return notificationStrings;
    }

    private void detectNotificationIds()
    {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.appicon)
                .setContentTitle("1")
                .setContentText("2")
                .setContentInfo("3")
                .setSubText("4");

        Notification n = mBuilder.build();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup localView;

        // detect id's from normal view
        localView = (ViewGroup) inflater.inflate(n.contentView.getLayoutId(), null);
        n.contentView.reapply(context, localView);
        recursiveDetectNotificationsIds(localView);

        // detect id's from expanded views
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            NotificationCompat.BigTextStyle bigtextstyle = new NotificationCompat.BigTextStyle();
            bigtextstyle.setSummaryText("5");
            bigtextstyle.setBigContentTitle("6");
            bigtextstyle.bigText("7");
            mBuilder.setContentTitle("8");
            mBuilder.setStyle(bigtextstyle);
            n = mBuilder.build();
            mBigTextLayoutId = n.bigContentView.getLayoutId();
            detectExpandedNotificationsIds(n);

            NotificationCompat.InboxStyle inboxStyle =
                    new NotificationCompat.InboxStyle();
            String[] events = {"10","11","12","13","14","15","16","17","18","19"};
            inboxStyle.setBigContentTitle("6");
            mBuilder.setContentTitle("9");
            inboxStyle.setSummaryText("5");

            for (int i=0; i < events.length; i++)
            {
                inboxStyle.addLine(events[i]);
            }
            mBuilder.setStyle(inboxStyle);
            n = mBuilder.build();
            mInboxLayoutId = n.bigContentView.getLayoutId();
            detectExpandedNotificationsIds(n);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void detectExpandedNotificationsIds(Notification n)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup localView = (ViewGroup) inflater.inflate(n.bigContentView.getLayoutId(), null);
        n.bigContentView.reapply(context, localView);
        recursiveDetectNotificationsIds(localView);
    }

    private void recursiveDetectNotificationsIds(ViewGroup v)
    {
        for(int i=0; i<v.getChildCount(); i++)
        {
            View child = v.getChildAt(i);
            if (child instanceof ViewGroup)
                recursiveDetectNotificationsIds((ViewGroup)child);
            else if (child instanceof TextView)
            {
                String text = ((TextView)child).getText().toString();
                int id = child.getId();
                if (text.equals("1")) notification_title_id = id;
                else if (text.equals("2")) notification_text_id = id;
                else if (text.equals("3")) notification_info_id = id;
                else if (text.equals("4")) notification_subtext_id = id;
                else if (text.equals("5")) big_notification_summary_id = id;
                else if (text.equals("6")) big_notification_content_title = id;
                else if (text.equals("7")) big_notification_content_text = id;
                else if (text.equals("8")) big_notification_title_id = id;
                else if (text.equals("9")) inbox_notification_title_id = id;
                else if (text.equals("10")) inbox_notification_event_1_id = id;
                else if (text.equals("11")) inbox_notification_event_2_id = id;
                else if (text.equals("12")) inbox_notification_event_3_id = id;
                else if (text.equals("13")) inbox_notification_event_4_id = id;
                else if (text.equals("14")) inbox_notification_event_5_id = id;
                else if (text.equals("15")) inbox_notification_event_6_id = id;
                else if (text.equals("16")) inbox_notification_event_7_id = id;
                else if (text.equals("17")) inbox_notification_event_8_id = id;
                else if (text.equals("18")) inbox_notification_event_9_id = id;
                else if (text.equals("19")) inbox_notification_event_10_id = id;
            }
            else if (child instanceof ImageView)
            {
                Drawable d = ((ImageView)child).getDrawable();
                if (d!=null)
                {
                    this.notification_image_id = child.getId();
                }
            }
        }
    }

    public boolean isPersistent(Notification n, String packageName)
    {
        boolean isPersistent = (((n.flags & Notification.FLAG_NO_CLEAR) == Notification.FLAG_NO_CLEAR) ||
                                ((n.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT));
        if (!isPersistent)
        {
            // check if user requested to treat all notifications as persistent
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean(packageName+"."+PersistentNotificationSettingsActivity.SHOW_PERSISTENT_NOTIFICATION, false) &&
                prefs.getBoolean(packageName+"."+PersistentNotificationSettingsActivity.CATCH_ALL_NOTIFICATIONS, true))
                isPersistent = true;
        }
        return isPersistent;
    }

    public PersistentNotification parsePersistentNotification(Notification n, String packageName, int notificationId)
    {
        // keep only the last persistent notification for the app
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useExpanded = (sharedPref.getBoolean(packageName + "." + AppSettingsActivity.USE_EXPANDED_TEXT,
                               sharedPref.getBoolean(AppSettingsActivity.USE_EXPANDED_TEXT, true)));

        PersistentNotification pn = new PersistentNotification();
        if (useExpanded && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            pn.expandedContent = getExpandedContent(n);
        }
        pn.content = n.contentView;
        Time now = new Time();
        now.setToNow();
        pn.received = now.toMillis(true);
        pn.packageName = packageName;
        pn.contentIntent = n.contentIntent;
        pn.id = notificationId;
        return pn;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private RemoteViews getExpandedContent(Notification n)
    {
        if (n.bigContentView != null)
            return n.bigContentView;
        else
            return n.contentView;
    }
}
