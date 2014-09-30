package com.roymam.android.notificationswidget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class NotificationData implements Parcelable
{
    private final String TAG = this.getClass().getSimpleName();

    public static int nextUID = 0;
    public int uid;
    public int id;
    CharSequence 	text;
    CharSequence	title;
    CharSequence	content;
    Bitmap 	icon;
    Bitmap 	appicon;
    long	received;
    String	packageName;
    PendingIntent action;
    int 	count;
    boolean pinned = false;
    boolean selected = false;
    private boolean deleted = false;
    boolean protect = false;
    public Action[] actions = new Action[0];
    public int priority;
    public String tag;
    public boolean event = false;
    public Bitmap largeIcon = null;
    public int appColor;
    public ArrayList<Bitmap> bitmaps;
    public String group = null;
    public String groupOrder = null;
    public boolean sideLoaded = false;
    public CharSequence additionalText = null;

    public NotificationData()
    {
        uid = nextUID;
        nextUID++;
    }

    public boolean isSimilar(NotificationData nd, boolean compareContent)
    {
        CharSequence otherTitle = nd.title;
        CharSequence myTitle = this.title;
        CharSequence otherText = nd.text;
        CharSequence myText = this.text;
        CharSequence otherContent = nd.content;
        CharSequence myContent = this.content;

        if (otherTitle == null) otherTitle = "";
        if (myTitle == null) myTitle = "";
        if (otherText == null) otherText = "";
        if (myText == null) myText = "";
        if (otherContent == null) otherContent = "";
        if (myContent == null) myContent = "";

        boolean titlesdup = otherTitle.toString().trim().equals(myTitle.toString().trim());
        boolean textdup = otherText.toString().trim().startsWith(myText.toString().trim());
        boolean contentsdup = otherContent.toString().trim().startsWith(myContent.toString().trim());
        boolean allDup = titlesdup && textdup && (contentsdup || !compareContent);

        if (nd.packageName.equals(this.packageName) && allDup &&
            !(sideLoaded && !nd.sideLoaded) // sideloaded notifications cannot be replaced with non-sideloaded notifications)
           ) {
            Log.d(TAG, "notification is similar to "+ packageName + ":" + id + "T" + tag);
            return true;
        }
        else
        {
            return false;
        }
    }

    // Parcelling part
    public NotificationData(Parcel in)
    {
        uid = in.readInt();
        id = in.readInt();
        if (in.readInt() != 0)
            text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        if (in.readInt() != 0)
            title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        if (in.readInt() != 0)
            content = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        if (in.readInt() != 0)
            additionalText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        if (in.readInt() != 0)
            icon = Bitmap.CREATOR.createFromParcel(in);
        if (in.readInt() != 0)
            appicon = Bitmap.CREATOR.createFromParcel(in);
        if (in.readInt() != 0)
            largeIcon = Bitmap.CREATOR.createFromParcel(in);

        received = in.readLong();
        packageName = in.readString();
        if (in.readInt() != 0)
            action = PendingIntent.CREATOR.createFromParcel(in);
        count = in.readInt();

        boolean[] ba = new boolean[6];
        in.readBooleanArray(ba);
        pinned = ba[0];
        selected = ba[1];
        deleted = ba[2];
        protect = ba[3];
        event = ba[4];
        sideLoaded = ba[5];

        if (in.readInt() != 0)
            actions = in.createTypedArray(Action.CREATOR);

        priority = in.readInt();
        tag = in.readString();
        appColor = in.readInt();
        group = in.readString();
        groupOrder = in.readString();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(uid);
        dest.writeInt(id);
        if (text != null)
        {
            dest.writeInt(1);
            TextUtils.writeToParcel(text, dest, flags);
        }
        else dest.writeInt(0);

        if (title != null)
        {
            dest.writeInt(1);
            TextUtils.writeToParcel(title, dest, flags);
        }
        else dest.writeInt(0);

        if (content != null)
        {
            dest.writeInt(1);
            TextUtils.writeToParcel(content, dest, flags);
        }
        else dest.writeInt(0);

        if (additionalText != null)
        {
            dest.writeInt(1);
            TextUtils.writeToParcel(additionalText, dest, flags);
        }
        else dest.writeInt(0);

        if (icon != null)
        {
            dest.writeInt(1);
            icon.writeToParcel(dest, flags);
        }
        else dest.writeInt(0);

        if (appicon != null)
        {
            dest.writeInt(1);
            appicon.writeToParcel(dest, flags);
        }
        else dest.writeInt(0);
        if (largeIcon != null)
        {
            dest.writeInt(1);
            largeIcon.writeToParcel(dest, flags);
        }
        else dest.writeInt(0);

        dest.writeLong(received);
        dest.writeString(packageName);
        if (action != null)
        {
            dest.writeInt(1);
            action.writeToParcel(dest, flags);
        }
        else
        {
            dest.writeInt(0);
        }
        dest.writeInt(count);

        boolean[] ba = new boolean[6];
        ba[0] = pinned;
        ba[1] = selected;
        ba[2] = deleted;
        ba[3] = protect;
        ba[4] = event;
        ba[5] = sideLoaded;
        dest.writeBooleanArray(ba);

        if (actions != null)
        {
            dest.writeInt(1);
            dest.writeTypedArray(actions, 0);
        }
        else
            dest.writeInt(0);

        dest.writeInt(priority);
        dest.writeString(tag);
        dest.writeInt(appColor);
        dest.writeString(group);
        dest.writeString(groupOrder);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
    {
        public NotificationData createFromParcel(Parcel in)
        {
            return new NotificationData(in);
        }

        public NotificationData[] newArray(int size)
        {
            return new NotificationData[size];
        }
    };

    public boolean isEqual(NotificationData nd)
    {
        CharSequence title1 = nd.title;
        CharSequence title2 = this.title;
        CharSequence text1 = nd.text;
        CharSequence text2 = this.text;
        CharSequence content1 = nd.content;
        CharSequence content2 = this.content;
        if (title1 == null) title1 = "";
        if (title2 == null) title2 = "";
        if (text1 == null) text1 = "";
        if (text2 == null) text2 = "";
        if (content1 == null) content1 = "";
        if (content2 == null) content2 = "";

        return  (title1.toString().equals(title2.toString()) && text1.toString().equals(text2.toString()) && content1.toString().equals(content2.toString()));
    }

    public void cleanup()
    {
    }

    public void delete()
    {
        // mark the notification as deleted
        deleted = true;

        // free up some big memory consumers
        largeIcon = null;
        icon = null;
        appicon = null;
        bitmaps = null;
        action = null;
        actions = null;
    }

    public Action getQuickReplyAction()
    {
        if (actions != null)
            for(int i = 0; i < actions.length; i++)
            {
                Log.d(TAG, "action:"+actions[i].title+ "remoteInputs:"+actions[i].remoteInputs);
                if (actions[i].remoteInputs != null) {
                    return actions[i];
                }
            }
        // no action - return null
        return null;
    }

    public static class Action implements Parcelable
	{
        public Action() {};

        public int icon;

        public CharSequence title;
        public PendingIntent actionIntent;
        public Bitmap drawable;
        public RemoteInput[] remoteInputs = null;

        public Action(Parcel in)
        {
            icon = in.readInt();
            if (in.readInt() != 0)
                title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            if (in.readInt() != 0)
                actionIntent = PendingIntent.readPendingIntentOrNullFromParcel(in);
            if (in.readInt() != 0)
                drawable = Bitmap.CREATOR.createFromParcel(in);
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeInt(icon);
            if (title != null)
            {
                dest.writeInt(1);
                TextUtils.writeToParcel(title, dest, flags);
            }
            else
            {
                dest.writeInt(0);
            }

            if (actionIntent != null)
            {
                dest.writeInt(1);
                PendingIntent.writePendingIntentOrNullToParcel(actionIntent, dest);
            }
            else dest.writeInt(0);

            if (drawable != null)
            {
                dest.writeInt(1);
                drawable.writeToParcel(dest, flags);
            }
            else
            {
                dest.writeInt(0);
            }
        }

        public static final Parcelable.Creator<Action> CREATOR = new Parcelable.Creator<Action>()
        {
            public Action createFromParcel(Parcel parcel)
            {
                return new Action(parcel);
            }

            public Action[] newArray(int size)
            {
                return new Action[size];
            }
        };
    }

    public void launch(Context context)
    {
        try
        {
            action.send();
        } catch (Exception e)
        {
            // if cannot launch intent, create a new one for the app
            try
            {
                Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                context.startActivity(LaunchIntent);
            }
            catch(Exception e2)
            {
                // cannot launch intent - do nothing...
                e2.printStackTrace();
                Toast.makeText(context, "Error - cannot launch app", Toast.LENGTH_SHORT).show();
            }
        }
        // clear notification from notifications list
        NotificationsService.getSharedInstance().clearNotification(uid);
    }

    // getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public CharSequence getText() {
        return text;
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    public CharSequence getTitle() {
        return title;
    }

    public void setTitle(CharSequence title) {
        this.title = title;
    }

    public CharSequence getContent() {
        return content;
    }

    public void setContent(CharSequence content) {
        this.content = content;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }

    public Bitmap getAppIcon() {
        return appicon;
    }

    public void setAppIcon(Bitmap appicon) {
        this.appicon = appicon;
    }

    public long getReceived() {
        return received;
    }

    public void setReceived(long received) {
        this.received = received;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public PendingIntent getAction() {
        return action;
    }

    public void setAction(PendingIntent action) {
        this.action = action;
    }

    public Action[] getActions() {
        return actions;
    }

    public void setActions(Action[] actions) {
        this.actions = actions;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isEvent() {
        return event;
    }

    public void setEvent(boolean event) {
        this.event = event;
    }

    public boolean isProtect() {
        return protect;
    }

    public void setProtect(boolean protect) {
        this.protect = protect;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public CharSequence getTimeText(Context ctxt)
    {
        // set time
        Time t = new Time();
        t.set(received);
        String timeFormat = "%H:%M";
        if (!DateFormat.is24HourFormat(ctxt)) timeFormat = "%l:%M%P";
        return t.format(timeFormat);
    }

}
