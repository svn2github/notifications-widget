package com.roymam.android.common;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

public class BitmapUtils
{
    public static int spToPx(int dp)
    {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().scaledDensity);
    }

    public static int pxToSp(int px)
    {
        return (int) (px / Resources.getSystem().getDisplayMetrics().scaledDensity);
    }

    public static int dpToPx(int dp)
    {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px)
    {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static Bitmap createDots(int width, int height, int color, int radius, int spacing)
    {
        Bitmap bmp = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint paint = new Paint();
        paint.setColor(color);

        for (int i=spacing/2; i<width+spacing/2; i+=spacing)
        {
            for (int j=spacing/2; j<height+spacing/2; j+=spacing)
            {
                canvas.drawCircle(i,j, radius, paint);
            }
        }

        return bmp;
    }

    public static Bitmap drawBitmapOnBitmap(Bitmap front, Bitmap back)
    {
        Bitmap result = Bitmap.createBitmap(back);
        Canvas resultCanvas = new Canvas(result);
        resultCanvas.drawBitmap(front, 0, 0, null);
        return result;
    }

    public static Bitmap createCenteredBitmap(Bitmap bitmap, int w, int h)
    {
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas resultCanvas = new Canvas(result);

        if (bitmap != null)
        {
            int x = w / 2 - bitmap.getWidth() / 2;
            int y = h / 2 - bitmap.getHeight() / 2;

            if (x >= 0 && y >= 0)
            {
                resultCanvas.drawBitmap(bitmap, x, y, null);
            }
            else
            {
                return Bitmap.createScaledBitmap(bitmap, w, h, false);
            }
        }
        return result;
    }

    public static Bitmap drawMaskOnBitmap(Bitmap bitmap, Bitmap mask, int dx, int dy)
    {
        // create a bitmap for the result
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),Bitmap.Config.ARGB_8888);

        // create a mutable mask bitmap with the same mask
        Bitmap mutableMask = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas maskCanvas = new Canvas(mutableMask);
        maskCanvas.drawBitmap(mask,dx,dy, new Paint());

        // paint the bitmap with mask into the result
        Canvas mCanvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mCanvas.drawBitmap(bitmap, 0, 0, null);
        mCanvas.drawBitmap(mutableMask, 0, 0, paint);
        paint.setXfermode(null);

        return result;
    }

    public static Bitmap drawBitmapOnMask(Bitmap bitmap, Bitmap mask, int dx, int dy)
    {
        // create a bitmap for the result
        Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(),Bitmap.Config.ARGB_8888);

        // create a mutable mask bitmap with the same mask
        Bitmap mutableMask = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas maskCanvas = new Canvas(mutableMask);
        maskCanvas.drawBitmap(mask,0,0, new Paint());

        // paint the bitmap with mask into the result
        Canvas mCanvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mCanvas.drawBitmap(bitmap, -dx, -dy, null);
        mCanvas.drawBitmap(mutableMask, 0, 0, paint);
        paint.setXfermode(null);

        return result;
    }

    public static Bitmap colorBitmap(Bitmap sourceBitmap, int color)
    {
        if (sourceBitmap == null) return null;

        float r = (float) Color.red(color),
                g = (float) Color.green(color),
                b = (float) Color.blue(color);

        float[] colorTransform =
                {
                        r/255, 0    , 0    , 0, 0,  // R color
                        0    , g/255, 0    , 0, 0,  // G color
                        0    , 0    , b/255, 0, 0,  // B color
                        0    , 0    , 0    , 1, 0
                };

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0f); // Remove colour
        colorMatrix.set(colorTransform);

        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        Paint paint = new Paint();
        paint.setColorFilter(colorFilter);

        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap);
        Bitmap mutableBitmap = resultBitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(mutableBitmap);
        canvas.drawBitmap(mutableBitmap, 0, 0, paint);

        return mutableBitmap;
    }
}
