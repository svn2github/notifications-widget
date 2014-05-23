package com.roymam.android.nilsplus.ui;

import android.content.res.Resources;
import android.view.MotionEvent;
import android.view.View;

import com.roymam.android.notificationswidget.R;

public class EditModeTouchListener implements View.OnTouchListener
{
    private final Callbacks mCallbacks;
    private final View mView;
    private final View mTopHandler;
    private final View mBottomHandler;
    private final View mLeftHandler;
    private final View mRightHandler;
    private final int mAnimationDuration;
    private float mTouchX;
    private float mTouchY;
    private boolean mTouch = false;

    public interface Callbacks
    {
        public void onEndResizeHeight();
        public void onStartResizeHeight();
        public void onResizeHeight(float top, float bottom);
        public void onEndResizeWidth();
        public void onStartResizeWidth();
        public void onResizeWidth(float left, float right);
        public void onCancel();
    }

    private View.OnTouchListener mTopHandlerListener = new View.OnTouchListener()
    {
        private float mTouchY;

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
            {
                mTouchY = event.getRawY();
                if (mCallbacks != null)
                {
                    mCallbacks.onStartResizeHeight();
                }
                mBottomHandler.animate().alpha(0).setDuration(mAnimationDuration);
                mLeftHandler.animate().alpha(0).setDuration(mAnimationDuration);
                mRightHandler.animate().alpha(0).setDuration(mAnimationDuration);
                return true;
            }
            else if (event.getAction() == MotionEvent.ACTION_MOVE)
            {
                float offset = event.getRawY() - mTouchY;
                if (mCallbacks != null)
                {
                    mCallbacks.onResizeHeight(-offset,-offset);
                }
                return true;
            }
            else if (event.getAction() == MotionEvent.ACTION_UP)
            {
                if (mCallbacks != null)
                {
                    mCallbacks.onEndResizeHeight();
                }

                mBottomHandler.animate().alpha(1).setDuration(mAnimationDuration);
                mLeftHandler.animate().alpha(1).setDuration(mAnimationDuration);
                mRightHandler.animate().alpha(1).setDuration(mAnimationDuration);
                return true;
            }
            return false;
        }
    };

    private View.OnTouchListener mBottomHandlerListener = new View.OnTouchListener()
    {
        private float mTouchY;

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
            {
                mTouchY = event.getRawY();
                if (mCallbacks != null)
                    mCallbacks.onStartResizeHeight();
                mTopHandler.animate().alpha(0).setDuration(mAnimationDuration);
                mLeftHandler.animate().alpha(0).setDuration(mAnimationDuration);
                mRightHandler.animate().alpha(0).setDuration(mAnimationDuration);
                return true;
            }
            else if (event.getAction() == MotionEvent.ACTION_MOVE)
            {
                float offset = event.getRawY() - mTouchY;
                if (mCallbacks != null)
                    mCallbacks.onResizeHeight(0, offset);
                return true;
            }
            else if (event.getAction() == MotionEvent.ACTION_UP)
            {
                if (mCallbacks != null)
                    mCallbacks.onEndResizeHeight();

                mTopHandler.animate().alpha(1).setDuration(mAnimationDuration);
                mLeftHandler.animate().alpha(1).setDuration(mAnimationDuration);
                mRightHandler.animate().alpha(1).setDuration(mAnimationDuration);
                return true;
            }
            return false;
        }
    };

    private View.OnTouchListener mLeftHandlerListener = new View.OnTouchListener()
    {
        private float mTouchX;

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
            {
                mTouchX = event.getRawX();
                if (mCallbacks != null)
                {
                    mCallbacks.onStartResizeWidth();
                }
                mBottomHandler.animate().alpha(0).setDuration(mAnimationDuration);
                mTopHandler.animate().alpha(0).setDuration(mAnimationDuration);
                mRightHandler.animate().alpha(0).setDuration(mAnimationDuration);
                return true;
            }
            else if (event.getAction() == MotionEvent.ACTION_MOVE)
            {
                float offset = event.getRawX() - mTouchX;
                if (mCallbacks != null)
                {
                    mCallbacks.onResizeWidth(offset, 0);
                }
                return true;
            }
            else if (event.getAction() == MotionEvent.ACTION_UP)
            {
                if (mCallbacks != null)
                {
                    mCallbacks.onEndResizeWidth();
                }

                mBottomHandler.animate().alpha(1).setDuration(mAnimationDuration);
                mTopHandler.animate().alpha(1).setDuration(mAnimationDuration);
                mRightHandler.animate().alpha(1).setDuration(mAnimationDuration);
                return true;
            }
            return false;
        }
    };

    private View.OnTouchListener mRightHandlerListener = new View.OnTouchListener()
    {
        private float mTouchX;

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
            {
                mTouchX = event.getRawX();
                if (mCallbacks != null)
                {
                    mCallbacks.onStartResizeWidth();
                }
                mBottomHandler.animate().alpha(0).setDuration(mAnimationDuration);
                mTopHandler.animate().alpha(0).setDuration(mAnimationDuration);
                mLeftHandler.animate().alpha(0).setDuration(mAnimationDuration);
                return true;
            }
            else if (event.getAction() == MotionEvent.ACTION_MOVE)
            {
                float offset = event.getRawX() - mTouchX;
                if (mCallbacks != null)
                {
                    mCallbacks.onResizeWidth(0, -offset);
                }
                return true;
            }
            else if (event.getAction() == MotionEvent.ACTION_UP)
            {
                if (mCallbacks != null)
                {
                    mCallbacks.onEndResizeWidth();
                }

                mBottomHandler.animate().alpha(1).setDuration(mAnimationDuration);
                mTopHandler.animate().alpha(1).setDuration(mAnimationDuration);
                mLeftHandler.animate().alpha(1).setDuration(mAnimationDuration);
                return true;
            }
            return false;
        }
    };

    public EditModeTouchListener(View v, Callbacks callbacks)
    {
        mCallbacks = callbacks;
        mView = v;

        // find handler points views
        mTopHandler = mView.findViewById(R.id.top_handler);
        mBottomHandler = mView.findViewById(R.id.bottom_handler);
        mLeftHandler = mView.findViewById(R.id.left_handler);
        mRightHandler = mView.findViewById(R.id.right_handler);

        mTopHandler.setOnTouchListener(mTopHandlerListener);
        mBottomHandler.setOnTouchListener(mBottomHandlerListener);
        mLeftHandler.setOnTouchListener(mLeftHandlerListener);
        mRightHandler.setOnTouchListener(mRightHandlerListener);

        mAnimationDuration = Resources.getSystem().getInteger(android.R.integer.config_shortAnimTime);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            if (mCallbacks != null) mCallbacks.onCancel();
        }
        return false;
    }
}
