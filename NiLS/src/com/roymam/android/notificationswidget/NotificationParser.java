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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v7.graphics.Palette;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.google.android.clockwork.stream.LegacyNotificationUtil;
import com.roymam.android.common.BitmapCache;
import com.roymam.android.common.IconPackManager;
import com.roymam.android.common.SysUtils;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationParser
{
    private final String TAG = NotificationParser.class.getSimpleName();

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
    public int bigpictue_notification_id = 0;
    private int mInboxLayoutId = 0;
    private int mBigTextLayoutId = 0;

    public NotificationParser(Context context)
    {
        this.context = context;
        detectNotificationIds();
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public List<NotificationData> parseNotification(Notification n, String packageName, int notificationId, String tag, boolean sideLoaded)
    {
        if (n != null)
        {
            // handle only dismissable notifications
            if (!isPersistent(n, packageName) && !shouldIgnore(n, packageName))
            {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                NotificationCompat.WearableExtender wo = LegacyNotificationUtil.getWearableOptions(n);

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

                String notificationIcon = sharedPref.getString(packageName + "." + SettingsManager.NOTIFICATION_ICON,
                                        sharedPref.getString(SettingsManager.NOTIFICATION_ICON, SettingsManager.DEFAULT_NOTIFICATION_ICON));

                if (res != null && info != null)
                {
                    Bitmap packageIcon = BitmapCache.getInstance(context).getBitmap(packageName, info.applicationInfo.icon);
                    String iconPack = sharedPref.getString(SettingsManager.ICON_PACK, SettingsManager.DEFAULT_ICON_PACK);
                    if (!iconPack.equals(SettingsManager.DEFAULT_ICON_PACK))
                    {
                        // load app icon from icon pack
                        IconPackManager.IconPack ip = IconPackManager.getInstance(context).getAvailableIconPacks(false).get(iconPack);
                        if (ip != null)
                            packageIcon = ip.getIconForPackage(packageName, packageIcon);
                    }

                    if (packageIcon != null)
                    {
                        Palette p = Palette.generate(packageIcon);
                        if (p.getVibrantColor() != null)
                            nd.appColor = p.getVibrantColor().getRgb();
                        else
                            nd.appColor = 0;
                    }
                    nd.appicon = BitmapCache.getInstance(context).getBitmap(packageName, n.icon);
                    if (notificationIcon.equals(SettingsManager.NOTIFICATION_MONO_ICON))
                    {
                        nd.icon = nd.appicon;
                    }
                    else
                    {
                        nd.icon = packageIcon;
                    }

                    if (nd.appicon == null)
                    {
                        nd.appicon = nd.icon;
                    }
                }
                if (n.largeIcon != null && notificationIcon.equals(SettingsManager.NOTIFICATION_ICON))
                {
                    nd.icon = n.largeIcon;
                }
                nd.largeIcon = n.largeIcon;

                // if the icon is too large - resize it to smaller size
                if (nd.icon != null && (nd.icon.getWidth() > maxIconSize || nd.icon.getHeight() > maxIconSize))
                {
                    nd.icon = Bitmap.createScaledBitmap(nd.icon, (int) maxIconSize, (int) maxIconSize, true);
                }

                if (nd.appicon != null && (nd.appicon.getWidth() > maxIconSize || nd.appicon.getHeight() > maxIconSize))
                {
                    nd.appicon = Bitmap.createScaledBitmap(nd.appicon, (int) maxIconSize, (int) maxIconSize, true);
                }

                // get wearable background icon if available
                if (wo != null && wo.getBackground() != null && nd.largeIcon == null && notificationIcon.equals(SettingsManager.NOTIFICATION_ICON))
                {
                    nd.icon = wo.getBackground();
                    nd.largeIcon = nd.icon;
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
                String privacy = SettingsManager.getPrivacy(context, packageName);
                if (privacy.equals(SettingsManager.PRIVACY_SHOW_ALL) || privacy.equals(SettingsManager.PRIVACY_SHOW_TITLE_ONLY))
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

                    if (nd.title == null && nd.text == null)
                    {
                        Log.d(TAG, "missing text from:" + packageName);
                        printStringsFromNotification();
                    }

                    // try to get additional text from wear pages
                    if (wo.getPages() != null && wo.getPages().size() > 0)
                    {
                        // extract the second page details
                        Notification page = wo.getPages().get(0);
                        nd.additionalText = NotificationCompat.getExtras(page).getCharSequence("android.text");
                    }

                    // hide text on private mode
                    if (privacy.equals(SettingsManager.PRIVACY_SHOW_TITLE_ONLY))
                        nd.text = "";
                }

                    // use default notification text & title - if no info found on expanded notification
                    if (nd.text == null)
                    {
                        if (privacy.equals(SettingsManager.PRIVACY_SHOW_APPNAME_ONLY))
                            nd.text = "";
                        else
                            nd.text = n.tickerText;
                    }

                    if (nd.title == null)
                    {
                        if (info != null)
                            nd.title = context.getPackageManager().getApplicationLabel(ai);
                        else
                            nd.title = packageName;

                        if (nd.text == null)
                        {
                            // if both text and title are null - that's non informative notification - ignore it
                            Log.d(TAG, "ignoring notification with empty title & text from :" + packageName);
                            printStringsFromNotification();
                            return new ArrayList<NotificationData>();
                        }
                    }
                    else if (nd.text == null)
                    {
                        // if both text and title are null - that's non informative notification - ignore it
                        Log.d(TAG, "a notification with no text from:" + packageName);
                        printStringsFromNotification();
                    }

                    if (nd.text != null) removeTimePrefix(nd);

                    nd.id = notificationId;
                    nd.tag = tag;

                    // check if this notifications belong to a group of notifications
                    nd.group = NotificationCompat.getGroup(n);
                    nd.groupOrder = NotificationCompat.getSortKey(n);

                    if (nd.group != null)
                    {
                        Log.d(TAG, "notification has a group:" + nd.group + " with group order:" + nd.groupOrder);
                    }
                    else
                    if (nd.group == null)
                    {
                        Log.d(TAG, "notification doesn't have a group, trying to get from extras.");
                        Bundle localBundle = NotificationCompat.getExtras(n);
                        if (localBundle != null) {
                            if (localBundle.getString("android.support.wearable.groupKey") != null) {
                                nd.group = localBundle.getString("android.support.wearable.groupKey");
                                int groupOrder = localBundle.getInt("android.support.wearable.groupOrder");
                                nd.groupOrder = String.format("%010d", groupOrder + 2147483648L);
                                Log.d(TAG, "notification has a group:" + nd.group + " with group order:" + nd.groupOrder);
                            }
                            if (localBundle.getString("com.google.android.wearable.stream.CREATOR_NODE_ID") != null)
                            {
                                Log.d(TAG, "notification has a creator node id:" + localBundle.getString("com.google.android.wearable.stream.CREATOR_NODE_ID"));
                            }
                        }
                    }

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

                    nd.sideLoaded = sideLoaded;

                    // check if this is a multiple events notification
                    String notificationMode = SettingsManager.getNotificationMode(context, packageName);

                    List<NotificationData> notifications = new ArrayList<NotificationData>();
                    notifications.add(nd);

                    // ignore group individual events if notification mode is set to show grouped only
                    if (notificationMode.equals(SettingsManager.MODE_GROUPED))
                    {
                        // if the notification is a part of a group - don't show it on grouped mode
                        if (nd.sideLoaded) {
                            Log.d(TAG, "ignoring sideloaded notification packageName:"+packageName+ "id:"+nd.id+ " notification mode is grouped and this a single group item");
                            return new ArrayList<NotificationData>();
                        }
                    }
                    // ignore summary group if setting is set to separated
                    else if (notificationMode.equals(SettingsManager.MODE_SEPARATED) &&
                            nd.group != null && nd.groupOrder == null && !nd.sideLoaded && NotificationCompat.isGroupSummary(n) &&
                            n.bigContentView != null && n.bigContentView.getLayoutId() == mInboxLayoutId)
                    {
                        // storing the notification so it can be used to dismiss from Android notifications bar
                        NotificationsService.getSharedInstance().groupedNotifications.put(packageName, nd);

                        // ignoring it so it won't appear on NiLS
                        Log.d(TAG, "ignoring original notification packageName:"+packageName+ "id:"+nd.id+ " notification mode is separated and this a group summary");
                        return new ArrayList<NotificationData>();
                    }
                    // if no group info found and it still has a bigcontentview try to extract individual events from the summary
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                             n.bigContentView != null &&
                            (n.bigContentView.getLayoutId() == mInboxLayoutId || packageName.equals("com.whatsapp")) &&
                            (privacy.equals(SettingsManager.PRIVACY_SHOW_ALL) || privacy.equals(SettingsManager.PRIVACY_SHOW_TITLE_ONLY)))
                    {
                        List<NotificationData> separatedNotifications = getMultipleNotificationsFromInboxView(n.bigContentView, nd);
                        // make sure we've at least one notification
                        if (separatedNotifications.size() > 0) notifications = separatedNotifications;
                    }
                    return notifications;
                }
        }
        return new ArrayList<NotificationData>();
    }

    private void printStringsFromNotification()
    {
        if (notificationStrings != null)
        {
            for(int i : notificationStrings.keySet())
            {
                CharSequence text = notificationStrings.get(i);
                Log.d(TAG, "id:"+i+" string:"+text);
            }
        }
    }

    private List<NotificationData> getMultipleNotificationsFromInboxView(RemoteViews bigContentView, NotificationData baseNotification)
    {
        String privacy = SettingsManager.getPrivacy(context, baseNotification.packageName);
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
        if (strings.containsKey(notification_text_id)) events.add(strings.get(notification_text_id));
        if (strings.containsKey(big_notification_content_text)) events.add(strings.get(big_notification_content_text));

        int eventsOrder = 0;

        // create a notification for each event
        for(CharSequence event : events)
        {
            NotificationData nd = new NotificationData();
            nd.icon = baseNotification.icon;
            nd.appicon = baseNotification.appicon;
            nd.largeIcon = baseNotification.largeIcon;
            nd.appColor = baseNotification.appColor;
            nd.id = baseNotification.id;
            nd.packageName = baseNotification.packageName;
            nd.pinned = baseNotification.pinned;
            nd.priority = baseNotification.priority;
            nd.tag = baseNotification.tag;
            nd.received = baseNotification.received + eventsOrder;
            nd.action = baseNotification.action;
            nd.content = baseNotification.content;
            nd.title = strings.get(notification_title_id);
            nd.bitmaps = baseNotification.bitmaps;
            nd.group = baseNotification.group;
            nd.sideLoaded = baseNotification.sideLoaded;
            nd.actions = baseNotification.actions;
            nd.groupOrder = String.format("%010d", eventsOrder);
            nd.event = true;
            nd.protect = true;
            nd.text = event;

            // extract title from content for first/last event
            if (event != null)
            {
                SpannableStringBuilder ssb = new SpannableStringBuilder(event);

                // try to split it by text style
                CharacterStyle[] spans = ssb.getSpans(0, event.length(), CharacterStyle.class);

                // if there are spans and the first doesn't contain the whole text
                if (spans.length > 0 && ssb.getSpanEnd(spans[0]) < ssb.length() - 1) {
                    int s0start = ssb.getSpanStart(spans[0]);
                    int s0end = ssb.getSpanEnd(spans[0]);
                    nd.title = event.subSequence(s0start, s0end).toString();
                    int s1start = s0end + 1;
                    int s1end = ssb.length() - 1;
                    nd.text = event.subSequence(s1start, s1end).toString();
                }
                else
                {
                    // try to split it by ":" delimiter
                    // first make sure it's not having the time prefix
                    event = nd.text;
                    removeTimePrefix(nd);

                    String[] parts = event.toString().split(": ", 2);
                    if (parts.length == 1) parts = event.toString().split(":", 2);
                    if (parts.length == 2 && parts[1].length()>2) // parts[1].length()>2 special exception for missed calls time 
                    {
                        // a fix for whatsapp group messages
                        if (nd.packageName.equals("com.whatsapp") && nd.title != null && !nd.title.equals("WhatsApp") && !nd.title.equals("WhatsApp+"))
                        {
                            nd.title = parts[0] + " @ " + nd.title;
                        }
                        else
                            nd.title = parts[0];
                        nd.text = parts[1];
                    }
                }

                if (privacy.equals(SettingsManager.PRIVACY_SHOW_TITLE_ONLY))
                    nd.text = "";

                notifications.add(nd);
            }
            eventsOrder++;
        }
        return notifications;
    }

    private void removeTimePrefix(NotificationData nd) {
        String timeRegExp = "^(\\d\\d?:\\d\\d? ?([AP]M )?)(.*)";

        Pattern timePat = Pattern.compile(timeRegExp);

        // search for time label in the title
        Matcher match = timePat.matcher(nd.title);
        if (match.matches())
        {
            // if it has it - set it as the event time
            nd.received = parseTime(match.group(1));
            nd.title = nd.title.subSequence(match.start(3), match.end(3));
        }

        // search for time prefix in the text
        match = timePat.matcher(nd.text);
        if (match.matches())
        {
            // if it has it - set it as the event time
            nd.received = parseTime(match.group(1));
            nd.text = nd.text.subSequence(match.start(3), match.end(3));
        }
    }

    private long parseTime(String time)
    {
        String timeFormat = "HH:mm";
        if (!DateFormat.is24HourFormat(context)) timeFormat = "hh:mm a";

        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat);
        try
        {
            Date d = sdf.parse(time);
            return d.getTime();
        }
        catch (ParseException e)
        {
            // shouldn't happen - time is in the right format
        }
        return 0;
    }


    private boolean shouldIgnore(Notification n, String packageName)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (!prefs.getBoolean(SettingsManager.COLLECT_ON_UNLOCK, true) &&
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

        // get additional actions from wear api
        NotificationCompat.WearableExtender wo = LegacyNotificationUtil.getWearableOptions(n);
        for(NotificationCompat.Action a : wo.getActions())
        {
            NotificationData.Action a2 = new NotificationData.Action();
            a2.icon = a.icon;
            a2.actionIntent = a.actionIntent;
            a2.title = a.title;
            a2.drawable = BitmapCache.getInstance(context).getBitmap(packageName, a.icon);
            a2.remoteInputs = a.getRemoteInputs();
            returnActions.add(a2);
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

        nd.bitmaps = new ArrayList<Bitmap>();

        // then try get information from the expanded view
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            view = getBigContentView(n);
            extractTextFromView(view, nd);

            nd.bitmaps = getBitmapsFromRemoteViews(view);
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

    HashMap<Integer, CharSequence> notificationStrings;

    private void extractTextFromView(RemoteViews view, NotificationData nd)
    {
        CharSequence title = null;
        CharSequence text = null;
        CharSequence content = null;

        notificationStrings = getNotificationStringFromRemoteViews(view);

        if (notificationStrings.size() > 0)
        {
            // try to get big text
            if (notificationStrings.containsKey(big_notification_content_text))
            {
                text = notificationStrings.get(big_notification_content_text);
            }
            else if (notificationStrings.containsKey(notification_text_id))
            {
                text = notificationStrings.get(notification_text_id);
            }
            // gmail "archived" message
            else if (notificationStrings.containsKey(2131296299))
            {
                text = notificationStrings.get(2131296299);
            }
            // CNBC text
            else if (notificationStrings.containsKey(2131099791))
            {
                text = notificationStrings.get(2131099791);
            }

            // Xabber title
            if (notificationStrings.containsKey(2131558423))
            {
                title = notificationStrings.get(2131558423);
            }
            // Xabber text
            if (notificationStrings.containsKey(2131558425))
            {
                text = notificationStrings.get(2131558425);
            }

            // SuperText title
            if (notificationStrings.containsKey(2131558405))
            {
                title = notificationStrings.get(2131558405);
            }
            // SuperText text
            if (notificationStrings.containsKey(2131558657))
            {
                text = notificationStrings.get(2131558657);
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
                if (s!= null && !s.equals(""))
                {
                    firstEventStr = s;
                    content = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_2_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_2_id);
                if (s!= null && !s.equals(""))
                {
                    content = TextUtils.concat(content, "\n", s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_3_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_3_id);
                if (s!= null && !s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_4_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_4_id);
                if (s!= null && !s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_5_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_5_id);
                if (s!= null && !s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_6_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_6_id);
                if (s!= null && !s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_7_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_7_id);
                if (s!= null && !s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_8_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_8_id);
                if (s!= null && !s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_9_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_9_id);
                if (s!= null && !s.equals(""))
                {
                    content = TextUtils.concat(content,"\n",s);
                    lastEventStr = s;
                }
            }

            if (notificationStrings.containsKey(inbox_notification_event_10_id))
            {
                CharSequence s = notificationStrings.get(inbox_notification_event_10_id);
                if (s!= null && !s.equals(""))
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

            if (notificationStrings.containsKey(notification_subtext_id))
            {
                CharSequence s = notificationStrings.get(notification_subtext_id);

                if (s!= null && !s.equals(""))
                {
                    if (content == null) content = s;
                    else content = content + "\n" + s;
                }
            }
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

    // use reflection to extract string from remoteviews object
    private HashMap<Integer, CharSequence> getNotificationStringFromRemoteViews(RemoteViews view)
    {
        HashMap<Integer, CharSequence> notificationText = new HashMap<Integer, CharSequence>();

        try
        {
            ArrayList<Parcelable> actions = null;
            Field fs = view.getClass().getDeclaredField("mActions");
            if (fs != null)
            {
                fs.setAccessible(true);
                actions = (ArrayList<Parcelable>) fs.get(view);
            }
            if (actions != null)
            {
                final int STRING = 9;
                final int CHAR_SEQUENCE = 10;

                // Find the setText() and setTime() reflection actions
                for (Parcelable p : actions)
                {
                    Parcel parcel = Parcel.obtain();
                    p.writeToParcel(parcel, 0);
                    parcel.setDataPosition(0);

                    // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                    int tag = parcel.readInt();
                    if (tag != 2) continue;

                    // View ID
                    int viewId = parcel.readInt();

                    String methodName = parcel.readString();
                    if (methodName == null) continue;

                        // Save strings
                    else if (methodName.equals("setText"))
                    {
                        // Parameter type (10 = Character Sequence)
                        parcel.readInt();

                        // Store the actual string
                        CharSequence t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                        notificationText.put(viewId, t);
                    }

                    parcel.recycle();
                }
            }
        }
        catch(Exception exp)
        {
            exp.printStackTrace();
        }

        return notificationText;
    }

    // use reflection to extract string from remoteviews object
    private ArrayList<Bitmap> getBitmapsFromRemoteViews(RemoteViews view)
    {
        ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();

        try
        {
            ArrayList<Parcelable> actions = null;
            Field fs = view.getClass().getDeclaredField("mActions");
            if (fs != null)
            {
                fs.setAccessible(true);
                actions = (ArrayList<Parcelable>) fs.get(view);
            }

            Object bitmapCache = null;
            fs = view.getClass().getDeclaredField("mBitmapCache");
            if (fs != null)
            {
                fs.setAccessible(true);
                bitmapCache = fs.get(view);
                fs = bitmapCache.getClass().getDeclaredField("mBitmaps");
                if (fs != null)
                {
                    fs.setAccessible(true);
                    bitmaps = (ArrayList<Bitmap>) fs.get(bitmapCache);
                }
            }

            // Find the setText() and setTime() reflection actions
            for (Parcelable p : actions)
            {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                if (tag != 12) continue;

                // View ID
                int viewId = parcel.readInt();

                String methodName = parcel.readString();
                if (methodName == null) continue;

                parcel.recycle();
            }
        }
        catch(Exception exp)
        {
            exp.printStackTrace();
        }

        return bitmaps;
    }

    private void detectNotificationIds()
    {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
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
