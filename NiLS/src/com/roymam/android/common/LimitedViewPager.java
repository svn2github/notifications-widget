package com.roymam.android.common;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class LimitedViewPager extends ViewPager
{
    private int mLimit = -1;
    private float mDownPos = -1;

    public LimitedViewPager(Context context)
    {
        super(context);
    }

    public LimitedViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public void setLimit(int pos)
    {
        mLimit = pos;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        switch (ev.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
                mDownPos = ev.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                // block swipe right when current item is limited
                if (ev.getRawX() < mDownPos && mLimit != -1 && getCurrentItem() >= mLimit)
                    return false;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mDownPos = -1;
                break;
        }
        return super.onTouchEvent(ev);
    }
}
