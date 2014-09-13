package com.roymam.android.nilsplus.ui.theme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;

import com.roymam.android.notificationswidget.SettingsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThemeManager
{
    private static ThemeManager instance = null;
    private final Context mContext;
    private Theme mCurrentTheme = null;

    public ThemeManager(Context context)
    {
        mContext = context;
    }

    public static ThemeManager getInstance(Context context)
    {
        if (instance == null)
            instance = new ThemeManager(context);
        return instance;
    }

    public List<ThemeInfo> getAvailableThemes()
    {
        ArrayList<ThemeInfo> themes = new ArrayList<ThemeInfo>();

        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> rinfo = pm.queryBroadcastReceivers(new Intent("com.roymam.android.nilsplus.theme"),PackageManager.GET_META_DATA);

        //List<PackageInfo> packages = pm.getPgetPackagesHoldingPermissions(new String[]{"com.roymam.android.nilsplus.theme"}, 0);

        for (ResolveInfo ri : rinfo)
        {
            try
            {
                String packageName = ri.activityInfo.packageName;
                Resources res = pm.getResourcesForApplication(packageName);
                int id = res.getIdentifier("themeName", "string", packageName);
                String themeName;
                if (id > 0)
                    themeName = res.getString(id);
                else
                {
                    ApplicationInfo ai = null;
                    ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                    themeName = mContext.getPackageManager().getApplicationLabel(ai).toString();
                }

                themes.add(new ThemeInfo(packageName, themeName));
            }
            catch (PackageManager.NameNotFoundException e)
            {
                // shouldn't happen
                e.printStackTrace();
            }
        }
        return themes;
    }

    public Theme loadTheme(String packageName)
    {
        PackageManager pm = mContext.getPackageManager();
        Resources res = null;
        ApplicationInfo ai = null;
        try
        {
            res = pm.getResourcesForApplication(packageName);
            ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            // shouldn't happen
            e.printStackTrace();
            return null;
        }

        // create the theme object (based on the default theme)
        Theme theme = Theme.getDefault(mContext);
        theme.res = res;
        theme.packageName = packageName;
        theme.title = loadThemeString(res, packageName, "themeName", pm.getApplicationLabel(ai).toString());

        // load bg
        theme.background = loadThemeDrawable(res, packageName, "background", theme.background);
        theme.altBackground = loadThemeDrawable(res, packageName, "alt_background", theme.altBackground);
        if (theme.altBackground == null) theme.altBackground = theme.background;

        // load preview bg
        theme.previewBG = loadThemeDrawable(res, packageName, "preview_bg", theme.previewBG);

        // load icon bg
        theme.iconBg = loadThemeDrawable(res, packageName, "icon_bg", theme.iconBg);
        theme.appIconBg = loadThemeDrawable(res, packageName, "app_icon_bg", theme.iconBg);
        theme.altIconBg = loadThemeDrawable(res, packageName, "alt_icon_bg", theme.altIconBg);
        if (theme.altIconBg  == null) theme.altIconBg = theme.iconBg;

        // load text bg
        theme.textBG = loadThemeDrawable(res, packageName, "text_bg", theme.textBG);
        theme.altTextBG = loadThemeDrawable(res, packageName, "alt_text_bg", theme.altTextBG);
        if (theme.altTextBG == null) theme.altTextBG = theme.textBG;

        // load preview text bg
        theme.previewTextBG  = loadThemeDrawable(res, packageName, "preview_text_bg", theme.previewTextBG);

        // load icon mask
        theme.iconMask = loadThemeDrawable(res, packageName, "icon_bg_mask", theme.iconMask);

        // load icon fg
        theme.iconFg = loadThemeDrawable(res, packageName, "icon_fg", theme.iconFg);

        // load divider
        theme.divider = loadThemeDrawable(res, packageName, "divider", theme.divider);

        // load colors
        theme.titleColor = loadThemeColor(res, packageName, "titleColor", theme.titleColor);
        theme.textColor  = loadThemeColor(res, packageName, "textColor", theme.textColor);
        theme.timeColor = loadThemeColor(res, packageName, "timeColor", theme.textColor);
        theme.bgColor = loadThemeColor(res, packageName, "bgColor", theme.bgColor);
        theme.altBgColor = loadThemeColor(res, packageName, "altBgColor", theme.bgColor);
        theme.iconBGColor = loadThemeColor(res, packageName, "iconBGColor", theme.iconBGColor);
        theme.altIconBGColor = loadThemeColor(res, packageName, "altIconBGColor", theme.iconBGColor );

        // load dimens
        theme.iconSize = loadThemeDimen(res, packageName, "iconSize", theme.iconSize);
        theme.notificationSpacing = loadThemeDimen(res, packageName, "notifications_list_spacing", theme.notificationSpacing);
        theme.titleFontSize = loadThemeDimen(res, packageName, "title_font_size", theme.titleFontSize);
        theme.textFontSize = loadThemeDimen(res, packageName, "text_font_size", theme.textFontSize);
        theme.timeFontSize = loadThemeDimen(res, packageName, "time_font_size", theme.timeFontSize);

        // load font style
        theme.titleTypeface = loadThemeTypeface(res, packageName, "title_family_name", "title_style", theme.titleTypeface);
        theme.textTypeface = loadThemeTypeface(res, packageName, "text_family_name", "text_style", theme.titleTypeface);
        theme.timeTypeface = loadThemeTypeface(res, packageName, "time_family_name", "time_style", theme.textTypeface);

        // load booleans
        theme.prominentIconBg = loadBoolean(res, packageName, "prominentIconBg", theme.prominentIconBg);
        theme.prominentAppIconBg = loadBoolean(res, packageName, "prominentIconBg", theme.prominentAppIconBg);
        theme.notificationLayout = loadThemeLayout(res, packageName, "notification_layout");
        theme.previewLayout = loadThemeLayout(res, packageName, "notification_preview");

        if (theme.notificationLayout != null || theme.previewLayout != null)
            theme.customLayoutIdMap = loadCustomThemeLayoutIds(res, packageName);

        return theme;
    }

    private boolean loadBoolean(Resources res, String packageName, String name, boolean defaultValue)
    {
        int id = res.getIdentifier(name, "bool", packageName);
        if (id > 0)
            return res.getBoolean(id);
        else
            return defaultValue;
    }

    private Map<String, Integer> loadCustomThemeLayoutIds(Resources res, String packageName) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("front", res.getIdentifier("front", "id", packageName));
        map.put("notification_image", res.getIdentifier("notification_image", "id", packageName));
        map.put("notification_title", res.getIdentifier("notification_title", "id", packageName));
        map.put("notification_text", res.getIdentifier("notification_text", "id", packageName));
        map.put("notification_time", res.getIdentifier("notification_time", "id", packageName));
        map.put("notification_text_container", res.getIdentifier("notification_text_container", "id", packageName));
        map.put("notification_bg", res.getIdentifier("notification_bg", "id", packageName));
        map.put("icon_bg", res.getIdentifier("icon_bg", "id", packageName));
        map.put("icon_fg", res.getIdentifier("icon_fg", "id", packageName));
        map.put("app_icon", res.getIdentifier("app_icon", "id", packageName));
        map.put("app_icon_bg", res.getIdentifier("app_icon_bg", "id", packageName));

        map.put("full_notification", res.getIdentifier("full_notification", "id", packageName));
        map.put("notification_body", res.getIdentifier("notification_body", "id", packageName));
        map.put("notification_preview", res.getIdentifier("notification_preview", "id", packageName));
        map.put("notification_image", res.getIdentifier("notification_image", "id", packageName));
        map.put("notification_text_scrollview", res.getIdentifier("notification_text_scrollview", "id", packageName));
        map.put("notification_big_picture", res.getIdentifier("notification_big_picture", "id", packageName));

        map.put("quick_reply_box", res.getIdentifier("quick_reply_box", "id", packageName));
        map.put("quick_reply_label", res.getIdentifier("quick_reply_label", "id", packageName));
        map.put("quick_reply_text", res.getIdentifier("quick_reply_text", "id", packageName));
        map.put("quick_reply_button", res.getIdentifier("quick_reply_button", "id", packageName));


        return map;
    }

    private Typeface loadThemeTypeface(Resources res, String packageName, String family_name, String style, Typeface defaultTypeface)
    {
        String fname;
        int fstyle;

        int fNameId = res.getIdentifier(family_name, "string", packageName);
        int fStyleId = res.getIdentifier(style, "int", packageName);
        if (fNameId > 0)
            fname = res.getString(fNameId);
        else
            return defaultTypeface;

        if (fStyleId > 0)
            fstyle = res.getInteger(fStyleId);
        else
            fstyle = Typeface.NORMAL;

        return Typeface.create(fname, fstyle);
    }

    private XmlResourceParser loadThemeLayout(Resources res, String packageName, String name)
    {
        int id = res.getIdentifier(name, "layout", packageName);
        if (id > 0)
            return res.getLayout(id);
        else
            return null;
    }

    private float loadThemeDimen(Resources res, String packageName, String name, float defaultValue)
    {
        int id = res.getIdentifier(name, "dimen", packageName);
        if (id > 0)
            return res.getDimension(id);
        else
            return defaultValue;
    }

    private int loadThemeColor(Resources res, String packageName, String name, int defaultValue)
    {
        int colorId = res.getIdentifier(name, "color", packageName);
        if (colorId > 0)
            return res.getColor(colorId);
        return defaultValue;
    }

    private String loadThemeString(Resources res, String packageName, String name, String defaultValue)
    {
        String value = defaultValue;
        int id = res.getIdentifier(name, "string", packageName);
        if (id > 0)
            value = res.getString(id);

        return value;
    }

    private Drawable loadThemeDrawable(Resources res, String packageName, String name, Drawable defaultValue)
    {
        int id = res.getIdentifier(name, "drawable", packageName);
        if (id > 0)
            return res.getDrawable(id);
        return defaultValue;
    }

    public Theme getCurrentTheme()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String currTheme = prefs.getString(SettingsManager.THEME, SettingsManager.DEFAULT_THEME);

        if (currTheme.equals(SettingsManager.DEFAULT_THEME)) return Theme.getDefault(mContext);

        if (mCurrentTheme == null || !mCurrentTheme.packageName.equals(currTheme))
        {
            mCurrentTheme = loadTheme(currTheme);
        }

        return mCurrentTheme;
    }

    public void reloadDrawables()
    {
        if (mCurrentTheme != null)
        {
            Theme defaultTheme = Theme.getDefault(mContext);
            mCurrentTheme.textBG = loadThemeDrawable(mCurrentTheme.res, mCurrentTheme.packageName, "text_bg", defaultTheme.textBG);
            mCurrentTheme.altTextBG = loadThemeDrawable(mCurrentTheme.res, mCurrentTheme.packageName, "alt_text_bg", mCurrentTheme.textBG);
            mCurrentTheme.background = loadThemeDrawable(mCurrentTheme.res, mCurrentTheme.packageName, "background", defaultTheme.background);
            mCurrentTheme.altBackground = loadThemeDrawable(mCurrentTheme.res, mCurrentTheme.packageName, "alt_background", mCurrentTheme.background);
        }
    }

    public void reloadLayouts(Theme theme)
    {
        try
        {
            PackageManager pm = mContext.getPackageManager();
            Resources res = pm.getResourcesForApplication(theme.packageName);
            theme.notificationLayout = loadThemeLayout(res, theme.packageName, "notification_layout");
            theme.previewLayout = loadThemeLayout(res, theme.packageName, "notification_preview");
            loadCustomThemeLayoutIds(res, theme.packageName);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            theme.notificationLayout = null;
            theme.previewLayout = null;
        }
    }
}
