package com.roymam.android.nilsplus.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ListView;

import com.google.android.apps.swipedismiss.SwipeDismissListViewTouchListener;

public class SwipeIconTouchListener extends SwipeDismissListViewTouchListener
{
    private final Context ctx;
    private final ListView mListView;
    private final DismissCallbacks mCallbacks;
    private final int mTouchSlop;
    private final SharedPreferences mPrefs;
    private float mDownX;
    private boolean mSwiping = false;
    private int mIconId;
    private float mDownY;
    private View mListItemView;
    private int mSelectedPosition;

    public SwipeIconTouchListener(ListView listView, DismissCallbacks callbacks, int iconId)
    {
        super(listView, callbacks);
        mListView = listView;
        mIconId = iconId;
        mCallbacks = callbacks;
        ctx = listView.getContext();
        mTouchSlop = ViewConfiguration.get(ctx).getScaledTouchSlop();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public interface DismissCallbacks extends  SwipeDismissListViewTouchListener.DismissCallbacks
    {
        void onIconDown(int position, int x, int y, MotionEvent touchEvent);
        void onIconDrag(int position, int x, int y, MotionEvent touchEvent);
        void onIconUp(int position, MotionEvent touchEvent);
        void onClick(int position);
    }

    private boolean isClick(float localX, float localY, float slop)
    {
        return Math.abs(localX-mDownX) < slop &&
               Math.abs(localY-mDownY) < slop;
    }

    public boolean isSwiping()
    {
        return mSwiping;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent)
    {
        switch (motionEvent.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
            {
                // Find the child view that was touched (perform a hit test)
                Rect rect = new Rect();
                int childCount = mListView.getChildCount();
                int[] listViewCoords = new int[2];
                mListView.getLocationOnScreen(listViewCoords);
                int x = (int) motionEvent.getRawX() - listViewCoords[0];
                int y = (int) motionEvent.getRawY() - listViewCoords[1];
                View child;
                mListItemView = null;

                for (int i = 0; i < childCount; i++)
                {
                    child = mListView.getChildAt(i);
                    child.getHitRect(rect);
                    if (rect.contains(x, y))
                    {
                        mListItemView = child;
                        mSelectedPosition = mListView.getPositionForView(mListItemView);
                        break;
                    }
                }

                if (mListItemView != null)
                {
                    // check if the icon was clicked
                    View iconView = mListItemView.findViewById(mIconId);
                    if (iconView != null)
                    {
                        int[] iconViewCoords = new int[2];
                        iconView.getHitRect(rect);
                        iconView.getLocationOnScreen(iconViewCoords);
                        x = (int) motionEvent.getRawX() - iconViewCoords[0];
                        y = (int) motionEvent.getRawY() - iconViewCoords[1];
                        if (rect.contains(x,y))
                        {
                            mSwiping = true;
                        }
                    }

                    mDownX = motionEvent.getRawX();
                    mDownY = motionEvent.getRawY();

                    if (mSwiping)
                    {
                        int[] vrect = new int[2];
                        mListItemView.getLocationOnScreen(vrect);
                        float vx = (int) motionEvent.getRawX() - vrect[0];
                        float vy = (int) motionEvent.getRawY() - vrect[1];
                        mCallbacks.onIconDown(mSelectedPosition, (int)vx, (int)vy, motionEvent);
                        return true;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_MOVE:
            {
                if (mSwiping && mListItemView != null)
                {
                    int[] rect = new int[2];
                    mListItemView.getLocationOnScreen(rect);
                    float x = (int) motionEvent.getRawX() - rect[0];
                    float y = (int) motionEvent.getRawY() - rect[1];

                    if (mCallbacks != null)
                    {
                        mCallbacks.onIconDrag(mSelectedPosition, (int)x, (int)y, motionEvent);
                    }
                    return true;
                }/*
                else
                {
                    if (mIsSwipeToOpenEnabled && mListItemView != null)
                    {
                        if (motionEvent.getRawX() - mDownX > mTouchSlop)
                        {
                            mSwiping = true;
                            int[] vrect = new int[2];
                            mListItemView.getLocationOnScreen(vrect);
                            float vx = (int) motionEvent.getRawX() - vrect[0];
                            float vy = (int) motionEvent.getRawY() - vrect[1];
                            mCallbacks.onIconDown(mSelectedPosition, (int)vx, (int)vy, motionEvent);
                            return true;
                        }
                    }
                }*/
                break;
            }
            case MotionEvent.ACTION_UP:
            {
                if (mSwiping)
                {
                    mDownX = 0;
                    View mDownView = null;
                    mSwiping = false;
                    mCallbacks.onIconUp(mSelectedPosition, motionEvent);
                    return true;
                }
                else
                {
                    float newx = motionEvent.getRawX();
                    float newy = motionEvent.getRawY();
                    if (isClick(newx, newy, mTouchSlop))
                        mCallbacks.onClick(mSelectedPosition);
                }
            }
        }
        return super.onTouch(view, motionEvent);
    }
}
