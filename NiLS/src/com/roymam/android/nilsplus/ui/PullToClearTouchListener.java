package com.roymam.android.nilsplus.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ListView;

import com.roymam.android.notificationswidget.SettingsManager;

public class PullToClearTouchListener implements View.OnTouchListener
{
    private final Context mContext;
    private final int mAnimationTime;
    private final int mSlop;
    private final int mLongTouchDelay;
    private boolean mOnTop = false;
    private float mTouchStartX;
    private float mTouchStartY;
    private float minDeltaToDismiss;
    private boolean mDismissAll;
    private int mPullDownViewHeight;
    private boolean mVerticalDrag;
    private boolean mHorizontalDrag;
    private View mPullDownView;
    private View mPullDownViewText;
    private View mReleaseViewText;
    private Callbacks mCallbacks;
    private View.OnTouchListener mFallbackListener;
    private boolean mTouch;
    private boolean mHold;

    public interface Callbacks
    {
        public void onRelease();
        public void onTouchAndHold();
        public void onDrag(float x, float y);
        public void onTouchRelease();
    }

    public  PullToClearTouchListener(Context context, View pullDownView, View pullDownText, View releaseText, Callbacks callbacks, View.OnTouchListener fallbackListener)
    {
        super();

        mContext = context;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        mPullDownView = pullDownView;
        mPullDownViewText = pullDownText;
        mReleaseViewText = releaseText;
        mAnimationTime = mContext.getResources().getInteger(android.R.integer.config_shortAnimTime);
        mCallbacks = callbacks;
        mFallbackListener = fallbackListener;
        ViewConfiguration vc = ViewConfiguration.get(mContext);
        mSlop = vc.getScaledTouchSlop();
        mLongTouchDelay = vc.getLongPressTimeout();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent)
    {
        final ListView listView = (ListView) view;
        final ViewGroup listViewContainer = (ViewGroup) listView.getParent();

        // ignore touch if the icon is currently beeing dragged
        if (mFallbackListener instanceof SwipeIconTouchListener &&
           ((SwipeIconTouchListener)mFallbackListener).isSwiping())
            {
                // cancel touch and hold
                mTouch = false;
                listView.clearAnimation();
                return mFallbackListener.onTouch(view, motionEvent);
            }

        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
        {
            // drag & drop handling
            mTouch = true;
            mHold = false;
            mTouchStartX = motionEvent.getRawX();
            mTouchStartY = motionEvent.getRawY();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            boolean resizeModeEnabled = prefs.getBoolean(SettingsManager.ENABLE_RESIZE_MODE, SettingsManager.DEFAULT_ENABLE_RESIZE_MODE);

            if (resizeModeEnabled)
            {
                listView.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // make sure the icon is not swiping
                        if (mFallbackListener instanceof SwipeIconTouchListener &&
                                ((SwipeIconTouchListener)mFallbackListener).isSwiping())
                        {
                            mTouch = false;
                        }

                        if (mTouch)
                        {
                                if (mCallbacks != null) mCallbacks.onTouchAndHold();

                                mHold = true;
                        }
                    }
                }, mLongTouchDelay);
            }

            // handling pulling down to clear
            boolean swipeDownToDismiss = prefs.getBoolean(SettingsManager.SWIPE_DOWN_TO_DISMISS_ALL, SettingsManager.DEFAULT_SWIPE_DOWN_TO_DISMISS_ALL);
            if (swipeDownToDismiss &&
                    listView != null && listView.getCount() > 0 &&
                    listView.getFirstVisiblePosition() == 0 &&
                    listView.getChildAt(0) != null &&
                    listView.getChildAt(0).getTop() == 0)
            {
                mOnTop = true;
                mTouchStartY = motionEvent.getRawY();
                mTouchStartX = motionEvent.getRawX();
                mPullDownViewHeight = mPullDownView.getHeight();
                minDeltaToDismiss = listView.getHeight();
                mPullDownView.setAlpha(0);
                mPullDownView.setTranslationY(-mPullDownViewHeight);
                mPullDownViewText.setVisibility(View.VISIBLE);
                mReleaseViewText.setVisibility(View.INVISIBLE);
                mVerticalDrag = false;
                mHorizontalDrag = false;
            }
            else
            {
                mOnTop = false;
            }
        }
        else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE)
        {
            // handle drag & drop
            if (mTouch && !mHold)
            {
                if (Math.abs(mTouchStartX - motionEvent.getRawX()) > mSlop ||
                        Math.abs(mTouchStartY - motionEvent.getRawY()) > mSlop)
                {
                    mTouch = false;
                }
            }
            else if (mHold)
            {
                if (mCallbacks != null)
                    mCallbacks.onDrag(motionEvent.getRawX()-mTouchStartX, motionEvent.getRawY()-mTouchStartY);
                return true;
            }

            if (mOnTop)
            {
                if (Math.abs(motionEvent.getRawX() - mTouchStartX) > mSlop && !mVerticalDrag)
                {
                    mHorizontalDrag = true;
                }
                if (Math.abs(motionEvent.getRawY() - mTouchStartY) > mSlop && !mHorizontalDrag)
                {
                    mVerticalDrag = true;
                }

                if (mVerticalDrag)
                {
                    float dy = (motionEvent.getRawY() - mTouchStartY);
                    if (dy > 0)
                    {
                        if (dy < mPullDownViewHeight)
                        {
                            float alpha = dy / mPullDownViewHeight;
                            mPullDownView.setAlpha(alpha);
                            mPullDownView.setTranslationY(dy-mPullDownViewHeight);
                        }
                        else
                        {
                            mPullDownView.setAlpha(1);
                            mPullDownView.setTranslationY(0);

                            float listAlpha = 1-((dy-mPullDownViewHeight) / (minDeltaToDismiss-mPullDownViewHeight));
                            if (listAlpha < 0) listAlpha = 0;

                            for(int i=0;i< listView.getChildCount();i++)
                            {
                                listView.getChildAt(i).setAlpha(listAlpha);
                            }
                        }

                        // move listview up
                        listViewContainer.setTranslationY(dy);

                        // change label if neccessery
                        if (dy > minDeltaToDismiss && !mDismissAll)
                        {
                            mPullDownViewText.setVisibility(View.INVISIBLE);
                            mReleaseViewText.setVisibility(View.VISIBLE);
                            mDismissAll = true;
                        }
                        else if (dy <= minDeltaToDismiss && mDismissAll)
                        {
                            mPullDownViewText.setVisibility(View.VISIBLE);
                            mReleaseViewText.setVisibility(View.INVISIBLE);
                            mDismissAll = false;
                        }
                        return true;
                    }
                    else
                    {
                        // reset position of everything
                        mPullDownView.setAlpha(0);
                        mPullDownView.setTranslationY(-mPullDownViewHeight);
                        listViewContainer.setTranslationY(0);
                        for(int i=0;i< listView.getChildCount();i++)
                        {
                            listView.getChildAt(i).setAlpha(1);
                        }
                    }
                }
            }
        }
        else if (motionEvent.getAction() == MotionEvent.ACTION_UP)
        {
            if (mTouch && !mHold)
            {
                mTouch = false;
            }
            else if (mHold)
            {
                if (mCallbacks != null) mCallbacks.onTouchRelease();
                return true;
            }

            if (mOnTop && mVerticalDrag)
            {
                if (mDismissAll)
                {
                    if (mCallbacks != null) mCallbacks.onRelease();
                    mPullDownView.setAlpha(0);
                    mPullDownView.setTranslationY(-mPullDownViewHeight);
                    listViewContainer.setTranslationY(0);
                    mDismissAll = false;
                }
                else
                {
                    // animate jump back of the list
                    mPullDownView.animate().alpha(0).translationY(-mPullDownViewHeight).setDuration(mAnimationTime);
                    listViewContainer.animate().translationY(0).setDuration(mAnimationTime);
                    for(int i=0;i< listView.getChildCount();i++)
                    {
                        listView.getChildAt(i).animate().alpha(1).setDuration(mAnimationTime).setListener(new AnimatorListenerAdapter()
                        {
                            @Override
                            public void onAnimationEnd(Animator animation)
                            {
                                // do nothing - just used to cancel current animation end event
                            }
                        });
                    }
                    return false;
                }
            }
        }

        if (mFallbackListener != null)
            return mFallbackListener.onTouch(view, motionEvent);
        else
            return false;
    }
}
