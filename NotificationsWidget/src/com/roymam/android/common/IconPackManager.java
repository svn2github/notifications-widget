package com.roymam.android.common;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class IconPackManager
{
    private final Context mContext;

    public class IconPack
    {
        public String packageName;
        public String name;
        private boolean loaded = false;

        HashMap<String, String> packagesDrawables = new HashMap<String, String>();
        HashMap<String, Bitmap> generatedBitmaps = new HashMap<String, Bitmap>();

        List<Bitmap> backimages = new ArrayList<Bitmap>();
        Bitmap iconmask = null;
        Bitmap iconupon = null;
        private float factor = 1.0f;

        Resources iconPackres = null;

        public void load()
        {
            // load appfilter.xml from the icon pack package
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

                        if (xpp.getName().equals("iconback"))
                        {
                            for(int i=0; i<xpp.getAttributeCount(); i++)
                            {
                                if (xpp.getAttributeName(i).startsWith("img"))
                                {
                                    String drawableName = xpp.getAttributeValue(i);
                                    Bitmap iconback = loadBitmap(drawableName);
                                    if (iconback != null)
                                        backimages.add(iconback);
                                }
                            }
                        }
                        else if (xpp.getName().equals("iconmask"))
                        {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1"))
                            {
                                String drawableName = xpp.getAttributeValue(0);
                                iconmask = loadBitmap(drawableName);
                            }
                        }
                        else if (xpp.getName().equals("iconupon"))
                        {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1"))
                            {
                                String drawableName = xpp.getAttributeValue(0);
                                iconupon = loadBitmap(drawableName);
                            }
                        }
                        else if (xpp.getName().equals("scale"))
                        {
                            // factor
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("factor"))
                            {
                                factor = xpp.getAttributeFloatValue(0, 1.0f);
                            }
                        }
                        else if (xpp.getName().equals("item"))
                        {
                            String packageName = null;
                            String drawableName = null;

                            for(int i=0; i<xpp.getAttributeCount(); i++)
                            {
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
                            if (!packagesDrawables.containsKey(packageName))
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

        private Bitmap loadBitmap(String drawableName)
        {
            int id = iconPackres.getIdentifier(drawableName, "drawable", packageName);
            if (id > 0)
            {
                Drawable bitmap = iconPackres.getDrawable(id);
                if (bitmap instanceof BitmapDrawable)
                    return ((BitmapDrawable)bitmap).getBitmap();
            }
            return null;
        }

        public Bitmap getIconForPackage(String appPackageName, Bitmap defaultBitmap)
        {
            if (!loaded)
                load();

            String drawable = packagesDrawables.get(appPackageName);
            if (drawable != null)
            {
                return loadBitmap(drawable);
            }
            return generateBitmap(appPackageName, defaultBitmap);
        }

        private Bitmap generateBitmap(String appPackageName, Bitmap defaultBitmap)
        {
            // if generated bitmaps cache already contains the package name return it
            if (generatedBitmaps.containsKey(appPackageName))
                return generatedBitmaps.get(appPackageName);

            // if no support images in the icon pack return the bitmap itself
            if (backimages.size() == 0)
                return defaultBitmap;

            Random r = new Random();
            int backImageInd = r.nextInt(backimages.size());
            Bitmap backImage = backimages.get(backImageInd);
            int w = backImage.getWidth();
            int h = backImage.getHeight();

            // create a bitmap for the result
            Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas mCanvas = new Canvas(result);

            // draw the background first
            mCanvas.drawBitmap(backImage, 0, 0, null);

            // create a mutable mask bitmap with the same mask
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(defaultBitmap, (int)(w * factor), (int)(h * factor), false);

            if (iconmask != null)
            {
                // draw the scaled bitmap with mask
                Bitmap mutableMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas maskCanvas = new Canvas(mutableMask);
                maskCanvas.drawBitmap(iconmask,0, 0, new Paint());

                // paint the bitmap with mask into the result
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                mCanvas.drawBitmap(scaledBitmap, (w - (w * factor))/2, (h - (h * factor))/2, null);
                mCanvas.drawBitmap(mutableMask, 0, 0, paint);
                paint.setXfermode(null);
            }
            else // draw the scaled bitmap without mask
            {
                mCanvas.drawBitmap(scaledBitmap, (w - (w * factor))/2, (h - (h * factor))/2, null);
            }

            // paint the front
            if (iconupon != null)
            {
                mCanvas.drawBitmap(iconupon, 0, 0, null);
            }

            // store the bitmap in cache
            generatedBitmaps.put(packageName, result);

            // return it
            return result;
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
