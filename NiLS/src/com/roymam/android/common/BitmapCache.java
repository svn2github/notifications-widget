package com.roymam.android.common;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

public class BitmapCache
{
    private Context context;
    private LruCache<String, Bitmap> cache;

    private static BitmapCache instance = null;

    private BitmapCache(Context context)
    {
        this.context = context;

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        cache = new LruCache<String, Bitmap>(cacheSize)
        {
            @Override
            protected int sizeOf(String key, Bitmap bitmap)
            {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static BitmapCache getInstance(Context context)
    {
        if (instance == null)
            instance = new BitmapCache(context);
        return instance;
    }

    public void putBitmap(String key, Bitmap bitmap)
    {
        cache.put(key, bitmap);
    }

    public Bitmap getBitmap(String key)
    {
        return cache.get(key);
    }

    public Bitmap getBitmap(String packageName, int resourceId)
    {
        if (resourceId == 0) return null;

        String key = packageName + "#" + resourceId;
        // if the bitmap is already on cache
        if (cache.get(key) != null)
        {
            return cache.get(key);
        }
        else // not in the cache - load it into the cache
        {
            Resources res;
            try
            {
                res = context.getPackageManager().getResourcesForApplication(packageName);
                Drawable icon = res.getDrawable(resourceId);
                if (BitmapDrawable.class.isInstance(icon))
                {
                    Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
                    cache.put(key, bitmap);
                    return bitmap;
                }
                else
                {
                    return null;
                }
            }
            catch (PackageManager.NameNotFoundException e)
            {
                // package not found - return null
                return null;
            }
            catch (Exception e)
            {
                // other exception - flush error and return null
                e.printStackTrace();
                return null;
            }

        }
    }
}
