package com.roymam.android.common;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.util.HashMap;

public class IconPackManager
{
    private final Context mContext;

    public class IconPack
    {
        String packageName;
        String name;

        public Drawable getIconForPackage(String packageName)
        {
            // TODO: 1. load appfilter.xml from the icon pack package
            //       2. find the packageName there and see which drawable it uses
            //       3. load iconback, iconmask, iconupon and the app drawables
            //       4. create the result icon
            //       5. return it
            return null;
        }
    }

    private static IconPackManager instance = null;

    private IconPackManager(Context context)
    {
        mContext = context;
    }

    public static IconPackManager getInstance(Context context)
    {
        if (instance == null) instance = new IconPackManager(context);
        return instance;
    }

    public HashMap<String, IconPack> getAvailableIconPacks()
    {
        HashMap<String, IconPack> iconPacks = new HashMap<String, IconPack>();

        // TODO - find apps with intent-filter "com.gau.go.launcherex.theme" and return build the HashMap

        return iconPacks;
    }
}
