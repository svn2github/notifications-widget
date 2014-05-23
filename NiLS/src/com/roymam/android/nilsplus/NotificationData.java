package com.roymam.android.nilsplus;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.format.DateFormat;
import android.text.format.Time;

public class NotificationData
{
    private Bitmap icon;
    private Bitmap appIcon;
    private CharSequence title;
    private CharSequence text;
    private String packageName;
    private int id;
    private int uid;
    private long recieved;
    private PendingIntent action;
    private Action[] actions;

    public void cleanup()
    {
        // TODO: need to understand better when to use this
        //if (icon != null) icon.recycle();
        //if (appIcon != null) appIcon.recycle();

        icon = null;
        appIcon = null;
    }

    public static class Action
    {
        public Bitmap icon;
        public CharSequence label;
        public PendingIntent intent;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getUid()
    {
        return uid;
    }

    public void setUid(int uid)
    {
        this.uid = uid;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public void setPackageName(String packageName) 
    {
        this.packageName = packageName;
    }

    public CharSequence getTitle()
    {
        return title;
    }

    public void setTitle(CharSequence title)
    {
        this.title = title;
    }

    public CharSequence getText()
    {
        return text;
    }

    public void setText(CharSequence text)
    {
        this.text = text;
    }

    public Bitmap getIcon()
    {
        return icon;
    }

    public void setIcon(Bitmap icon)
    {
        this.icon = icon;
    }

    public long getRecieved()
    {
        return recieved;
    }

    public void setRecieved(long recieved)
    {
        this.recieved = recieved;
    }

    public CharSequence getTimeText(Context ctxt)
    {
        // set time
        Time t = new Time();
        t.set(recieved);
        String timeFormat = "%H:%M";
        if (!DateFormat.is24HourFormat(ctxt)) timeFormat = "%l:%M%P";
            return t.format(timeFormat);
    }

    public void setAction(PendingIntent action)
    {
        this.action = action;
    }

    public PendingIntent getAction()
    {
        return action;
    }

    public Bitmap getAppIcon()
    {
        return appIcon;
    }

    public void setAppIcon(Bitmap appIcon)
    {
        this.appIcon = appIcon;
    }

    public Action[] getActions()
    {
        return actions;
    }

    public void setActions(Action[] actions)
    {
        this.actions = actions;
    }
}
