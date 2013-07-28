package com.roymam.android.notificationswidget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.Toast;

public class NotificationData 
{
    public int id;
    public String tag;

    public static class Action
	{
		public Action() {};
		public int icon;
		public CharSequence title;
		public PendingIntent actionIntent;
		public Bitmap drawable;
	}

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
    }
}
