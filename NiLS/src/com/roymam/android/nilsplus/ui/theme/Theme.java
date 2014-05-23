package com.roymam.android.nilsplus.ui.theme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.roymam.android.notificationswidget.R;
import com.roymam.android.notificationswidget.SettingsActivity;

public class Theme extends ThemeInfo
{
    public Drawable background = null;
    public Drawable altBackground = null;
    public Drawable previewBG = null;
    public Drawable textBG = null;
    public Drawable altTextBG = null;
    public Drawable previewTextBG = null;
    public Drawable iconBg = null;
    public Drawable altIconBg = null;
    public Drawable iconFg = null;
    public Drawable iconMask = null;
    public int titleColor = -1;
    public int textColor = -1;
    public int timeColor = -1;
    public int bgColor = -1;
    public int altIconBGColor = -1;
    public int altBgColor = -1;
    public int iconBGColor = -1;
    public float iconSize = -1;
    public float notificationSpacing = -1;
    public Drawable divider = null;
    public float titleFontSize = -1;
    public float textFontSize = -1;
    public float timeFontSize = -1;
    public Typeface titleTypeface = null;
    public Typeface textTypeface = null;
    public Typeface timeTypeface = null;

    public Resources res;

    public Theme(String packageName, String title)
    {
        super(packageName, title);
    }

    public static Theme getDefault(Context context)
    {
        Theme t = new Theme(SettingsActivity.DEFAULT_THEME, "Default");
        t.titleColor = SettingsActivity.DEFAULT_PRIMARY_TEXT_COLOR;
        t.textColor = SettingsActivity.DEFAULT_SECONDARY_TEXT_COLOR;
        t.bgColor = SettingsActivity.DEFAULT_MAIN_BG_COLOR;
        t.iconBGColor = SettingsActivity.DEFAULT_ICON_BG_COLOR;
        t.altBgColor = SettingsActivity.DEFAULT_MAIN_BG_COLOR;
        t.altIconBGColor = SettingsActivity.DEFAULT_ICON_BG_COLOR;
        t.iconSize = context.getResources().getDimension(R.dimen.notification_default_icon_size);
        t.notificationSpacing = context.getResources().getDimension(R.dimen.notification_default_list_spacing);
        t.titleFontSize = context.getResources().getDimension(R.dimen.notification_default_title_size);
        t.textFontSize = context.getResources().getDimension(R.dimen.notification_default_text_size);
        return t;
    }
}
