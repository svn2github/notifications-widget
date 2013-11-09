package com.roymam.android.notificationswidget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class NotificationData 
{
    public static int nextUID = 0;
    public int uid;
    public int id;
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

        /*Log.d("NiLS", "Title1 '"+title1+"'");
        Log.d("NiLS", "Title2 '"+title2+"'");
        Log.d("NiLS", "Title Dup:"+titlesdup);
        Log.d("NiLS", "Text1 '"+text1+"'");
        Log.d("NiLS", "Text2 '"+text2+"'");
        Log.d("NiLS", "Text Dup:"+textdup);
        Log.d("NiLS", "Content1 '"+content1+"'");
        Log.d("NiLS", "Content2 '"+content2+"'");
        Log.d("NiLS", "Content Dup:"+contentsdup);
*/
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
        // clear notification from notifications list
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.FORCE_CLEAR_ON_OPEN, false) &&
            NotificationsService.getSharedInstance(context) != null)
        {
            // request service to clear itself
            NotificationsService.getSharedInstance(context).clearNotification(packageName, id);
        }
    }
}
