package com.roymam.android.notificationswidget;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.RemoteViews;

public class PersistentNotification implements Parcelable
{
	public String packageName;
	public RemoteViews content;
	public RemoteViews expandedContent;
	public PendingIntent contentIntent;
	public long received;
    public int id;

    public PersistentNotification()
    {

    }

    public PersistentNotification(Parcel in)
    {
        packageName = in.readString();
        if (in.readInt() != 0) content = RemoteViews.CREATOR.createFromParcel(in);
        if (in.readInt() != 0) expandedContent = RemoteViews.CREATOR.createFromParcel(in);
        if (in.readInt() != 0) contentIntent = PendingIntent.readPendingIntentOrNullFromParcel(in);
        received = in.readLong();
        id = in.readInt();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(packageName);
        if (content != null)
        {
            dest.writeInt(1);
            content.writeToParcel(dest, flags);
        }
        else
            dest.writeInt(0);
        if (expandedContent != null)
        {
            dest.writeInt(1);
            expandedContent.writeToParcel(dest, flags);
        }
        else
            dest.writeInt(0);
        if (contentIntent != null)
        {
            dest.writeInt(1);
            contentIntent.writeToParcel(dest, flags);
        }
        else
            dest.writeInt(0);
        dest.writeLong(received);
        dest.writeInt(id);
    }

    public static final Parcelable.Creator<PersistentNotification> CREATOR = new Parcelable.Creator<PersistentNotification>()
    {
        public PersistentNotification createFromParcel(Parcel parcel)
        {
            return new PersistentNotification(parcel);
        }

        public PersistentNotification[] newArray(int size)
        {
            return new PersistentNotification[size];
        }
    };

}
