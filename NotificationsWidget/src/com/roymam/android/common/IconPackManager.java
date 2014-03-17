package com.roymam.android.common;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class IconPackManager
{
    private final Context mContext;

    public class IconPack
    {
        public String packageName;
        public String name;
        private boolean loaded = false;
        HashMap<String, String> packagesDrawables = new HashMap<String, String>();
        Resources iconPackres = null;

        public void load()
        {
            // 1. load appfilter.xml from the icon pack package
            PackageManager pm = mContext.getPackageManager();
            try
            {
                iconPackres = pm.getResourcesForApplication(packageName);
                XmlResourceParser xpp = iconPackres.getXml(iconPackres.getIdentifier("appfilter", "xml", packageName));

                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT)
                {
                    if(eventType == XmlPullParser.START_TAG)
                    {
                        System.out.println(xpp.getName() + ":" + xpp.getAttributeCount());
                        // TODO: load iconback, iconmask, iconupon and the app drawables
                        if (xpp.getName().equals("iconback"))
                        {
                            // img1,2,3,4
                        }
                        else if (xpp.getName().equals("iconmask"))
                        {
                            // img1
                        }
                        else if (xpp.getName().equals("iconupon"))
                        {
                            // img1
                        }
                        else if (xpp.getName().equals("scale"))
                        {
                            // factor
                        }
                        else if (xpp.getName().equals("item"))
                        {
                            String packageName = null;
                            String drawableName = null;

                            for(int i=0; i<xpp.getAttributeCount(); i++)
                            {
                                Log.d("NiLS", xpp.getAttributeName(i) + ":" + xpp.getAttributeValue(i));
                                if (xpp.getAttributeName(i).equals("component"))
                                {
                                    String compName = xpp.getAttributeValue(i);
                                    int start = compName.indexOf("{") + 1;
                                    int end = compName.indexOf("/");
                                    if (end > start) packageName = compName.substring(start, end);
                                }
                                else if (xpp.getAttributeName(i).equals("drawable"))
                                {
                                    drawableName = xpp.getAttributeValue(i);
                                }
                            }
                            packagesDrawables.put(packageName, drawableName);
                        }
                    }
                    eventType = xpp.next();
                }

                loaded = true;
            }
            catch (PackageManager.NameNotFoundException e)
            {
                Log.d("NiLS", "Cannot load icon pack");
            } catch (XmlPullParserException e)
            {
                Log.d("NiLS", "Cannot parse icon pack appfilter.xml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public Bitmap getIconForPackage(String appPackageName)
        {
            if (!loaded)
                load();

            String drawable = packagesDrawables.get(appPackageName);
            if (drawable !=null)
            {
                int id = iconPackres.getIdentifier(drawable, "drawable", packageName);
                if (id > 0)
                {
                    Drawable d = iconPackres.getDrawable(id);
                    if (d instanceof BitmapDrawable)
                    {
                        return ((BitmapDrawable)d).getBitmap();
                    }
                }
            }
            return null;
        }
    }

    private static IconPackManager instance = null;
    private HashMap<String, IconPack> iconPacks = null;

    private IconPackManager(Context context)
    {
        mContext = context;
    }

    public static IconPackManager getInstance(Context context)
    {
        if (instance == null) instance = new IconPackManager(context);
        return instance;
    }

    public HashMap<String, IconPack> getAvailableIconPacks(boolean forceReload)
    {
        if (iconPacks == null || forceReload)
        {
            iconPacks = new HashMap<String, IconPack>();

            // find apps with intent-filter "com.gau.go.launcherex.theme" and return build the HashMap
            PackageManager pm = mContext.getPackageManager();
            List<ResolveInfo> rinfo = pm.queryIntentActivities(new Intent("com.gau.go.launcherex.theme"), PackageManager.GET_META_DATA);

            for(ResolveInfo ri  : rinfo)
            {
                IconPack ip = new IconPack();
                ip.packageName = ri.activityInfo.packageName;

                ApplicationInfo ai = null;
                try
                {
                    ai = pm.getApplicationInfo(ip.packageName, PackageManager.GET_META_DATA);
                    ip.name  = mContext.getPackageManager().getApplicationLabel(ai).toString();
                    iconPacks.put(ip.packageName, ip);
                }
                catch (PackageManager.NameNotFoundException e)
                {
                    // shouldn't happen
                    e.printStackTrace();
                }
            }
        }
        return iconPacks;
    }
}
