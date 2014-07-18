package com.roymam.android.nilsplus.ui.theme;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.roymam.android.notificationswidget.R;
import com.roymam.android.notificationswidget.SettingsManager;

import java.util.Map;

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
    public Map<String, Integer> customLayoutIdMap = null;

    public Resources res;
    public XmlResourceParser notificationLayout = null;
    public XmlResourceParser previewLayout = null;

    public boolean prominentIconBg = false;
    public boolean prominentAppIconBg = false;
    public Drawable appIconBg = null;

    public Theme(String packageName, String title)
    {
        super(packageName, title);
    }

    public static Theme getDefault(Context context)
    {
        Theme t = new Theme(SettingsManager.DEFAULT_THEME, "Default");
        t.titleColor = SettingsManager.DEFAULT_PRIMARY_TEXT_COLOR;
        t.textColor = SettingsManager.DEFAULT_SECONDARY_TEXT_COLOR;
        t.bgColor = SettingsManager.DEFAULT_MAIN_BG_COLOR;
        t.iconBGColor = SettingsManager.DEFAULT_ICON_BG_COLOR;
        t.altBgColor = SettingsManager.DEFAULT_MAIN_BG_COLOR;
        t.altIconBGColor = SettingsManager.DEFAULT_ICON_BG_COLOR;
        t.iconSize = context.getResources().getDimension(R.dimen.notification_default_icon_size);
        t.notificationSpacing = context.getResources().getDimension(R.dimen.notification_default_list_spacing);
        t.titleFontSize = context.getResources().getDimension(R.dimen.notification_default_title_size);
        t.textFontSize = context.getResources().getDimension(R.dimen.notification_default_text_size);
        t.notificationLayout = null;
        t.customLayoutIdMap = null;
        return t;
    }
}
