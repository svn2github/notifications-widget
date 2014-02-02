package com.roymam.android.notificationswidget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class NotificationData implements Parcelable
{
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
    public Action[] actions = null;
    public int priority;

    public String tag;
    public NotificationData()
    {
        uid = nextUID;
        nextUID++;
    }

    public boolean isSimilar(NotificationData nd)
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
        boolean titlesdup = title1.toString().trim().equals(title2.toString().trim());
        boolean textdup = text1.toString().trim().startsWith(text2.toString().trim());
        boolean contentsdup = content1.toString().trim().startsWith(content2.toString().trim());
        boolean allDup = titlesdup && textdup && contentsdup;

        if (nd.packageName.equals(this.packageName) && allDup)
        {
            Log.d("NiLS", "All Dup");
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
            icon = Bitmap.CREATOR.createFromParcel(in);
        if (in.readInt() != 0)
            appicon = Bitmap.CREATOR.createFromParcel(in);

        received = in.readLong();
        packageName = in.readString();
        if (in.readInt() != 0)
            action = PendingIntent.CREATOR.createFromParcel(in);
        count = in.readInt();

        boolean[] ba = new boolean[2];
        in.readBooleanArray(ba);
        pinned = ba[0];
        selected = ba[1];

        if (in.readInt() != 0)
            actions = in.createTypedArray(Action.CREATOR);

        priority = in.readInt();
        tag = in.readString();
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

        boolean[] ba = new boolean[2];
        ba[0] = pinned;
        ba[1] = selected;
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

    public static class Action implements Parcelable
	{
        public Action() {};
        public int icon;
        public CharSequence title;
        public PendingIntent actionIntent;
        public Bitmap drawable;

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
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.FORCE_CLEAR_ON_OPEN, false) &&
            NotificationsService.getSharedInstance() != null)
        {
            // request service to clear itself
            NotificationsService.getSharedInstance().clearNotification(uid);
        }
    }
}
