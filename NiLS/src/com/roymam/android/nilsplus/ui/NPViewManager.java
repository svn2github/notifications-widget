package com.roymam.android.nilsplus.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.roymam.android.common.BitmapUtils;
import com.roymam.android.common.SysUtils;
import com.roymam.android.notificationswidget.NotificationData;
import com.roymam.android.notificationswidget.NotificationsService;
import com.roymam.android.notificationswidget.R;
import com.roymam.android.notificationswidget.SettingsManager;

import java.util.ArrayList;
import java.util.List;

public class NPViewManager
{
    private static final int EDIT_MODE_PADDING = 32;
    private static final String TAG = NPViewManager.class.getSimpleName();
    private final int mAnimationDuration;
    private ListView mListView;
    private final Handler mHandler;
    private final Context mContext;
    private final Callbacks mCallbacks;

    private final View mResizeRect;
    private NPListView mNPListView;
    private PreviewNotificationView mPreviewView;
    private final View mEditModeView;
    private final View mTouchAreaView;
    private final DotsSwipeView mDotsView;

    private NotificationData mPreviewItem = null;
    private int yOffset = 0;
    private Point mWidgetSize;
    private Point mWidgetPosition;
    private final WindowManager mWindowManager;
    private Point mMaxWidgetSize;
    private Point mMaxWidgetPosition;
    private int mPrevHeight = 0;
    private boolean mVisible = false;

    private Runnable mUpdateTouchAreaSizeToMaximum = new Runnable()
    {
        @Override
        public void run()
        {
            // update touch area
            int height = getPreviewHeight();
            if (height != mPrevHeight)
            {
                safeUpdateView(mTouchAreaView, getLayoutParams(0));

                mPrevHeight = height;
            }
        }
    };

    private Runnable mUpdateTouchAreaSize = new Runnable()
    {
        @Override
        public void run()
        {
            {
                // update touch area
                int height = Math.min(getHeight(), mPreviewItem != null? getPreviewHeight():mNPListView.getItemsHeight());
                if (height != mPrevHeight)
                {
                    safeUpdateView(mTouchAreaView, getTouchAreaLayoutParams(true));

                    mPrevHeight = height;
                }
            }

            if (mVisible)
                mTouchAreaView.setVisibility(View.VISIBLE);
            else
                mTouchAreaView.setVisibility(View.GONE);
        }
    };
    private int mPreviewPosition = -1;

    public void destroy()
    {
        mNPListView.cleanup();
        mPreviewView.cleanup();
        mEditModeView.setOnTouchListener(null);
        mResizeRect.setOnTouchListener(null);
        mTouchAreaView.setOnTouchListener(null);
        removeAllViews();
    }

    public boolean isVisible()
    {
        return mVisible;
    }

    public void applyAppearance()
    {
        // recreate the adapter to apply the new appearance settings
        mNPListView.reloadAppearance();

    }

    public void saveNotificationsState()
    {
        mNPListView.saveNotificationsState();
    }

    /*
    private void animateNotificationsChange()
    {
        mNPListView.animateNotificationsChange();
    }*/

    // callbacks
    public interface Callbacks
    {
        public void onDismissed(NotificationData ni);
        public void onOpen(NotificationData ni);
        public void onAction(NotificationData ni, int actionPos);
    }

    public NPViewManager(Context context, Callbacks callbacks)
    {
        // store parameters
        mContext = context;
        mCallbacks = callbacks;

        // create an handler for scheduling tasks
        mHandler = new Handler();

        // load system configuration
        mAnimationDuration = Resources.getSystem().getInteger(android.R.integer.config_shortAnimTime);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        // calculate sizes
        Point maxSize = getWidgetSize();
        Point maxPos = getWidgetPosition(maxSize);

        // create list view
        createListView();

        // create dots view
        mDotsView = new DotsSwipeView(context, maxPos, maxSize);
        mDotsView.setAlpha(0);
        mDotsView.setVisibility(View.GONE);

        // create the preview view & add the view to the screen
        mPreviewView = new PreviewNotificationView(mContext, maxSize, maxPos, mDotsView);

        // create edit mode view
        mEditModeView = View.inflate(mContext, R.layout.editmode, null);
        mResizeRect = mEditModeView.findViewById(R.id.resize_rect);
        mEditModeView.setVisibility(View.GONE);
        mEditModeView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    disableEditMode();
                    return true;
                }
                else
                    return false;
            }
        });

        mResizeRect.setOnTouchListener(new EditModeTouchListener(mEditModeView, new EditModeTouchListener.Callbacks()
        {
            private RelativeLayout.LayoutParams mParams;
            private int mLeft;
            private int mRight;
            public Point mMinSize;
            public float lasttop;
            public float lastbottom;
            private Point mDisplaySize;

            @Override
            public void onStartResizeHeight()
            {
                mDisplaySize = getDisplaySize();

                mWidgetSize = getWidgetSize();
                mMaxWidgetSize = getWidgetSize();
                mWidgetPosition = getWidgetPosition(mWidgetSize);
                mMaxWidgetPosition = getWidgetPosition(mMaxWidgetSize);
                mMinSize = new Point(BitmapUtils.dpToPx(PreferenceManager.getDefaultSharedPreferences(mContext).getInt(SettingsManager.ICON_SIZE, SettingsManager.DEFAULT_ICON_SIZE))*2,
                        BitmapUtils.dpToPx(PreferenceManager.getDefaultSharedPreferences(mContext).getInt(SettingsManager.ICON_SIZE, SettingsManager.DEFAULT_ICON_SIZE)));

                mParams = new RelativeLayout.LayoutParams(mMaxWidgetSize.x+EDIT_MODE_PADDING*2,mMaxWidgetSize.y+EDIT_MODE_PADDING*2);
                mParams.leftMargin = mWidgetPosition.x - EDIT_MODE_PADDING;
                mParams.topMargin = calcYoffset(mWidgetPosition.y, mWidgetSize.y) - EDIT_MODE_PADDING;
            }

            @Override
            public void onResizeHeight(float top, float bottom)
            {
                lasttop = top;
                lastbottom = bottom;

                mParams.topMargin = (int) (mMaxWidgetPosition.y - top - EDIT_MODE_PADDING);
                if (mParams.topMargin < -EDIT_MODE_PADDING) mParams.topMargin = -EDIT_MODE_PADDING;
                mParams.height = (int) (mMaxWidgetSize.y + bottom + EDIT_MODE_PADDING * 2);

                if (mParams.height < mMinSize.y + EDIT_MODE_PADDING*2)
                {
                    mParams.height = mMinSize.y + EDIT_MODE_PADDING*2;
                    if (top!=0) mParams.topMargin = mMaxWidgetPosition.y + mMaxWidgetSize.y - mMinSize.y - EDIT_MODE_PADDING;
                }

                mResizeRect.setLayoutParams(mParams);
            }

            @Override
            public void onEndResizeHeight()
            {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

                // store new height
                prefs.edit().putInt(getRotationMode()+ SettingsManager.HEIGHT, BitmapUtils.pxToDp(mParams.height - EDIT_MODE_PADDING * 2)).commit();
                mResizeRect.setLayoutParams(mParams);

                // update notifications list widget
                endWidgetDraggin(-lasttop);
            }

            @Override
            public void onStartResizeWidth()
            {
                onStartResizeHeight();

                mLeft = mWidgetPosition.x;
                mRight = mDisplaySize.x - mWidgetSize.x - mLeft;
            }

            @Override
            public void onResizeWidth(float left, float right)
            {
                mParams.leftMargin = (int) (mLeft + left) - EDIT_MODE_PADDING;
                if (mParams.leftMargin < -EDIT_MODE_PADDING) mParams.leftMargin = -EDIT_MODE_PADDING;
                mParams.width = (int) (mDisplaySize.x - mParams.leftMargin - mRight - right) + EDIT_MODE_PADDING;
                if (mParams.width > mDisplaySize.x - mParams.leftMargin + EDIT_MODE_PADDING)
                    mParams.width = mDisplaySize.x - mParams.leftMargin + EDIT_MODE_PADDING;

                if (mParams.width < mMinSize.x + EDIT_MODE_PADDING*2)
                {
                    mParams.width = mMinSize.x + EDIT_MODE_PADDING*2;
                    if (left!=0) mParams.leftMargin = mMaxWidgetPosition.x + mMaxWidgetSize.x - mMinSize.x - EDIT_MODE_PADDING;
                }
                mResizeRect.setLayoutParams(mParams);
            }

            @Override
            public void onCancel()
            {
                disableEditMode();
            }

            @Override
            public void onEndResizeWidth()
            {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                prefs.edit().putInt(getRotationMode()+"left_margin", mParams.leftMargin + EDIT_MODE_PADDING)
                        .putInt(getRotationMode()+"right_margin", mDisplaySize.x - mParams.width - mParams.leftMargin + EDIT_MODE_PADDING)
                        .commit();

                // update notifications list widget
                endWidgetDraggin(0);
            }

        }));

        // create touch area view
        mTouchAreaView = new RelativeLayout(mContext);
        mTouchAreaView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        mTouchAreaView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

                if (event.getAction() == MotionEvent.ACTION_OUTSIDE && prefs.getBoolean(SettingsManager.HIDE_ON_CLICK, SettingsManager.DEFAULT_HIDE_ON_CLICK)) hide(true);
                if (event.getAction() == MotionEvent.ACTION_DOWN) keepScreenOn("user touch");

                if (mPreviewItem == null)
                    mListView.dispatchTouchEvent(event);
                //else
                    //    mPreviewView.dispatchTouchEvent(event);
                    return false;
            }
        });

        // add all views to window manager
        addAllViews();

        // make sure views are hidden by default
        mNPListView.setVisibility(View.GONE);
        mPreviewView.setVisibility(View.GONE);
    }

    public void createListView()
    {
        Point size = getWidgetSize();
        Point pos = getWidgetPosition(size);

        mNPListView = new NPListView(mContext, size, pos, new NPListView.Callbacks()
        {
            public float lasty;

            @Override
            public void notificationCleared(NotificationData ni)
            {
                if (mCallbacks != null) mCallbacks.onDismissed(ni);
                //notifyDataChanged();
            }

            @Override
            public void notificationOpen(NotificationData ni)
            {
                if (mCallbacks != null) mCallbacks.onOpen(ni);
            }

            @Override
            public void notificationClicked(NotificationData ni, int position, boolean iconSwiping)
            {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                if (prefs.getBoolean(SettingsManager.CLICK_TO_OPEN, SettingsManager.DEFAULT_CLICK_TO_OPEN))
                {
                    notificationOpen(ni);
                }
                else
                {
                    showNotificationPreview(ni, position);
                }
            }

            @Override
            public void onTouchAndHold()
            {
                disableEditMode();
                startWidgetDragging();
            }

            @Override
            public void onDrag(float x, float y)
            {
                lasty = y;
            }

            @Override
            public void onTouchRelease()
            {
                endWidgetDraggin(lasty);
            }

            @Override
            public void notificationRunAction(NotificationData ni, int i)
            {
                if (mCallbacks != null) mCallbacks.onAction(ni, i);
            }
        });
        mListView = mNPListView.getListView();
    }

    private Point getDisplaySize()
    {
        return getDisplaySize(mContext);
    }

    public static Point getDisplaySize(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);
        return displaySize;
    }

    private String getRotationMode()
    {
        return getRotationMode(mContext);
    }

    public static String getRotationMode(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int rotation = display.getRotation();
        String mode;
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
            mode = "";
        else
            mode = "landscape.";

        // check we currently on non lock screen
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(SettingsManager.DONT_HIDE, SettingsManager.DEFAULT_DONT_HIDE))
        {
            // separated mode for notifications list on home screen
            String lockScreenApp = prefs.getString(SettingsManager.LOCKSCREEN_APP, SettingsManager.DEFAULT_LOCKSCREEN_APP);

            String currentApp = SysUtils.getForegroundApp(context);

            boolean shouldHideNotificaitons = (!SysUtils.isKeyguardLocked(context) &&
                !currentApp.equals(lockScreenApp) ||
                currentApp.equals(SettingsManager.STOCK_PHONE_PACKAGENAME));

            if (shouldHideNotificaitons)
                mode = "home." + mode;
        }

        return mode;
    }

    public void keepScreenOn(String reason)
    {
        final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        Log.d(TAG, "keepScreeOn, isScreenOn:"+pm.isScreenOn());
        SysUtils sysUtils = SysUtils.getInstance(mContext,mHandler);
        if (pm.isScreenOn()) sysUtils.turnScreenOn(true, true, reason);
    }

    private int calcYoffset(int y, int height)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Point displaySize = getDisplaySize();

        String yAlignment = prefs.getString(SettingsManager.VERTICAL_ALIGNMENT, SettingsManager.DEFAULT_VERTICAL_ALIGNMENT);
        int maxHeight = getPreviewHeight();

        int yOffset;

        if (yAlignment.equals("center")) yOffset = y - (maxHeight/2 - height/2);
        else if (yAlignment.equals("bottom")) yOffset = y - (maxHeight - height);
        else yOffset = y;

        return yOffset;
    }

    private void endWidgetDraggin(float yOffset)
    {
        // store new offset
        // calculate sizes
        Point maxSize = getWidgetSize();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Point displaySize = getDisplaySize();

        int prevYOffset = prefs.getInt(getRotationMode()+"yoffset", displaySize.y / 2 - maxSize.y / 2);
        prefs.edit().putInt(getRotationMode()+"yoffset", (int) (prevYOffset + yOffset)).commit();

        // calculate updated sizes
        Point size = getWidgetSize();
        Point pos = getWidgetPosition(size);

        // update notifications list
        mNPListView.updateSizeAndPosition(pos, size);
        mPreviewView.updateSizeAndPosition(pos, size);

        // update touch area
        safeUpdateView(mTouchAreaView, getTouchAreaLayoutParams(true));

        // enable edit mode if not enabled already
        enableEditMode();
    }

    private void startWidgetDragging()
    {
        mWidgetSize = getWidgetSize();
        mMaxWidgetSize = getWidgetSize();
        mWidgetPosition = getWidgetPosition(mWidgetSize);
        mMaxWidgetPosition = getWidgetPosition(mMaxWidgetSize);
    }

    private void enableEditMode()
    {
        mEditModeView.setVisibility(View.VISIBLE);
        Point size = getWidgetSize();
        Point pos = getWidgetPosition(size);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size.x+EDIT_MODE_PADDING*2,size.y+EDIT_MODE_PADDING*2);
        params.leftMargin = pos.x - EDIT_MODE_PADDING;
        params.topMargin = pos.y - EDIT_MODE_PADDING;
        mResizeRect.setLayoutParams(params);
        mResizeRect.setAlpha(1.0f);
        //mResizeRect.animate().alpha(1.0f).setDuration(mAnimationDuration).setListener(null);
    }

    private void disableEditMode()
    {
        mResizeRect.animate().alpha(0.0f).setDuration(mAnimationDuration).setListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                mEditModeView.setVisibility(View.GONE);
            }
        });
    }

    public void updateTouchArea()
    {
        try
        {
            safeUpdateView(mTouchAreaView, getTouchAreaLayoutParams(true));
        }
        catch (Exception exception)
        {
            // view already exists - ignore
        }
    }

    private void showNotificationPreview(NotificationData ni, int position)
    {
        if (getMaxLines() >= 3)
        {
            // update the content of the preview
            mPreviewView.setContent(ni, new PreviewNotificationView.Callbacks()
            {
                @Override
                public void onDismiss(NotificationData ni)
                {
                    if (mPreviewItem.getQuickReplyAction() != null) mPreviewView.hideQuickReplyBox();
                    if (mCallbacks != null) mCallbacks.onDismissed(ni);
                }

                @Override
                public void onOpen(NotificationData ni)
                {
                    if (mCallbacks != null) mCallbacks.onOpen(ni);
                }

                @Override
                public void onClick()
                {
                    hideNotificationPreview();
                }

                @Override
                public void onAction(NotificationData ni, int actionPos)
                {
                    if (mCallbacks != null) mCallbacks.onAction(ni, actionPos);
                }
            });

            // calc offset (where to start animation)
            //yOffset = calcOffset();

            View rowView = mListView.getChildAt(position - mListView.getFirstVisiblePosition());
            if (rowView != null) {
                mPreviewItem = ni;
                mPreviewPosition = position;

                // animation fade out of list view
                mListView.animate().alpha(0).setDuration(mAnimationDuration).setListener(null);

                Rect rect = new Rect();

                Point size = getWidgetSize();
                Point pos = getWidgetPosition(size);

                rect.top = (int) (rowView.getY() + pos.y);
                rect.bottom = rect.top + rowView.getHeight();
                rect.left = (int) (rowView.getX() + pos.x);
                rect.right = rect.left + rect.left;

                Log.d(TAG, "animate to preview, start rect:" + rect);

                // animate pop in of the preview view
                mPreviewView.show(rect);

                // hide touch area
                mTouchAreaView.setVisibility(View.GONE);

                // show quick reply keyboard if needed
                showKeyboardOnPreview();
            }
            else
            {
                Log.w(TAG, "notification wasn't found on listview, probably was dismissed, cannot display a preview");
            }
        }
    }

    /*private void safeRemoveView(View v)
    {
        try
        {
            mWindowManager.removeViewImmediate(v);
        }
        catch (Exception exp)
        {
            //exp.printStackTrace();
            // the view probably wasn't attached, ignore this exception
        }
    }

    private boolean safeAddView(View v, WindowManager.LayoutParams params)
    {
        try
        {
            mWindowManager.addView(v, params);
            return true;
        }
        catch (Exception exp)
        {
           // exp.printStackTrace();
            // the view probably was already attached, ignore this exception
            // and update params
            safeUpdateView(v, params);
            return false;
        }
    }*/

    public void hide(boolean force)
    {
        Log.d(TAG, "NPViewManager.hide();");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (!prefs.getBoolean(SettingsManager.DONT_HIDE, SettingsManager.DEFAULT_DONT_HIDE) || force)
        {
            //Log.d(TAG, "Hiding NiLS");
            // hide preview if visibile
            if (mPreviewItem != null)
            {
                mHandler.postDelayed(mUpdateTouchAreaSize, mAnimationDuration*2);
            }
            mNPListView.setVisibility(View.GONE);
            mTouchAreaView.setVisibility(View.GONE);
            if (mVisible)
            {
                if (mPreviewItem != null)
                {
                    mPreviewView.animate().alpha(0).setDuration(mAnimationDuration).setListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            hideKeyboardOnPreview();
                            mPreviewItem = null;
                            mPreviewView.hideImmediate();
                            mTouchAreaView.setVisibility(View.VISIBLE);
                        }
                    });
                }
                else
                {
                    mNPListView.setVisibility(View.VISIBLE);
                    mListView.animate().alpha(0).setDuration(mAnimationDuration).setListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            mNPListView.setVisibility(View.GONE);
                        }
                    });
                }
                mVisible = false;
            }
        }

        // remove persistent notification
        //NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        //nm.cancel(0);
    }

    private void removeAllViews()
    {
        mWindowManager.removeViewImmediate(mNPListView);
        mWindowManager.removeViewImmediate(mPreviewView);
        mWindowManager.removeViewImmediate(mEditModeView);
        mWindowManager.removeViewImmediate(mTouchAreaView);
        mWindowManager.removeViewImmediate(mDotsView);
        //safeRemoveView(mNPListView);
        //safeRemoveView(mPreviewView);
        //safeRemoveView(mEditModeView);
    }

    public void showKeyboardOnPreview()
    {
        if (mPreviewItem != null && mPreviewItem.getQuickReplyAction() != null)
        {
            mWindowManager.updateViewLayout(mPreviewView, getPreviewWithKeyboardParams());
            //mWindowManager.removeViewImmediate(mTouchAreaView);
            mPreviewView.showQuickReplyBox();
        }
    }

    public void hideKeyboardOnPreview()
    {
        if (mPreviewItem != null && mPreviewItem.getQuickReplyAction() != null)
        {
            mWindowManager.updateViewLayout(mPreviewView, getPreviewWindowParams());
            //mWindowManager.addView(mTouchAreaView, getTouchAreaLayoutParams(true));
            mPreviewView.hideQuickReplyBox();
        }
    }

    private void addAllViews()
    {
        mWindowManager.addView(mNPListView, getFullScreenLayoutParams(true));
        mWindowManager.addView(mPreviewView, getPreviewWindowParams());
        mWindowManager.addView(mTouchAreaView, getTouchAreaLayoutParams(true));
        mWindowManager.addView(mEditModeView, getEditModeLayoutParams(true));
        mWindowManager.addView(mDotsView, getFullScreenLayoutParams(true));

        /*
        boolean newViews = true;
        newViews &= safeAddView(mNPListView, getFullScreenLayoutParams());
        newViews &= safeAddView(mPreviewView, getFullScreenLayoutParams());
        newViews &= safeAddView(mTouchAreaView, getTouchAreaLayoutParams());
        newViews &= safeAddView(mEditModeView, getEditModeLayoutParams());
        return newViews;*/
    }

    public void show(boolean immediate)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (prefs.getBoolean(SettingsManager.FP_ENABLED, SettingsManager.DEFAULT_FP_ENABLED))
        {
            Log.d(TAG, "NPViewManager.show();");

            // hide preview mode if it was opened
            if (mPreviewItem != null)
            {
                hideKeyboardOnPreview();
                hideNotificationPreview();
                mPreviewView.hideImmediate();
            }

            mNPListView.show();
            mTouchAreaView.setVisibility(View.VISIBLE);

            // hide edit mode if displayed
            if (mEditModeView.getVisibility() == View.VISIBLE)
                disableEditMode();

            if (!mVisible)
            {
                mVisible = true;
                if (!immediate) {
                    mListView.setAlpha(0);
                    mListView.setScaleY(0);
                    mListView.animate().alpha(1).scaleY(1).setDuration(mAnimationDuration).setListener(null);
                }
            }
        }
    }

    private WindowManager.LayoutParams getEditModeLayoutParams(boolean keyguard)
    {
        int type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        if (!keyguard) type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        return params;
    }

    private void hideNotificationPreview()
    {
        // animate showing of list view
        mNPListView.setVisibility(View.VISIBLE);
        mListView.animate().alpha(1).setDuration(mAnimationDuration).setListener(null);

        // animate hiding preview view
        mPreviewView.hide(yOffset);

        // show touch area
        mTouchAreaView.setVisibility(View.VISIBLE);

        hideKeyboardOnPreview();
        mPreviewItem = null;

        mHandler.postDelayed(mUpdateTouchAreaSize, mAnimationDuration*2);
    }

    private int getHeight()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int height = BitmapUtils.dpToPx(prefs.getInt(getRotationMode()+ SettingsManager.HEIGHT, SettingsManager.DEFAULT_HEIGHT));
        if (height > 0) return height;
        else return 0;
    }

    private int getPreviewHeight()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return BitmapUtils.dpToPx(prefs.getInt(getRotationMode()+ SettingsManager.PREVIEW_HEIGHT, SettingsManager.DEFAULT_PREVIEW_HEIGHT));
    }

    private int getMaxLines()
    {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getInt(SettingsManager.FP_MAX_LINES, SettingsManager.DEFAULT_MAX_LINES);
    }

    private Point getWidgetSize()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Point size = getDisplaySize();

        int leftMargin = prefs.getInt(getRotationMode()+"left_margin", (int) (size.x * 0.05f));
        int rightMargin = prefs.getInt(getRotationMode()+"right_margin", (int) (size.x * 0.05f));
        int width = size.x - leftMargin - rightMargin;
        int height = getHeight();

        //Log.d("NiLS+", "widget size:" + width + "," + height);
        return new Point(width, height);
    }

    private int getYoffset()
    {
        Point displaySize = getDisplaySize();

        int maxHeight = getHeight();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int yOffset = prefs.getInt(getRotationMode()+"yoffset", displaySize.y / 2 - maxHeight / 2);
        return yOffset;
    }

    private Point getWidgetPosition(Point size)
    {
        return getWidgetPosition(size, getYoffset());
    }

    private Point getWidgetPosition(Point size, int yOffset)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        String yAlignment = prefs.getString(SettingsManager.VERTICAL_ALIGNMENT, SettingsManager.DEFAULT_VERTICAL_ALIGNMENT);
        int maxHeight = getHeight();

        int x,y;
        x = prefs.getInt(getRotationMode()+"left_margin", (int) (size.x * 0.05f));

        if (yAlignment.equals("center")) y = yOffset + maxHeight/2 - size.y/2;
        else if (yAlignment.equals("bottom")) y = yOffset + maxHeight - size.y;
        else y = yOffset;

        //Log.d("NiLS+", "widget pos:" + x + "," + y);
        return new Point(x,y);
    }

    private void updateWidgetPosition(int xoffset, int yoffset, float scaleFactor)
    {
        Point screenSize = getDisplaySize();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String yAlignment = prefs.getString(SettingsManager.VERTICAL_ALIGNMENT, SettingsManager.DEFAULT_VERTICAL_ALIGNMENT);
        int maxHeight = mMaxWidgetSize.y;

        // move widget to correct position
        //mWidgetDragParams.gravity= Gravity.TOP | Gravity.LEFT;
        //mWidgetDragParams.x = (int) (mWidgetPosition.x - mWidgetSize.x * (scaleFactor - 1) / 2) + xoffset;
        //mWidgetDragParams.y = (int) (mWidgetPosition.y - mWidgetSize.y * (scaleFactor - 1) / 2) + yoffset;

        int miny,maxy;

        if (yAlignment.equals("center"))
        {
            miny = maxHeight/2 - mWidgetSize.y/2;
            maxy = screenSize.y - mWidgetSize.y/2;
        }
        else if (yAlignment.equals("bottom"))
        {
            miny = maxHeight - mWidgetSize.y;
            maxy = screenSize.y - mWidgetSize.y;
        }
        else
        {
            miny = 0;
            maxy = screenSize.y - maxHeight;
        }
    }

    private WindowManager.LayoutParams getPreviewWindowParams()
    {
        Point displaySize = getDisplaySize();
        Point size = getWidgetSize();
        Point pos = getWidgetPosition(size);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                size.y,
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        params.gravity= Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = pos.y;
        if (params.y < 0) params.y = 0;
        if (params.y > displaySize.y) params.y = displaySize.y;

        return params;
    }

    private WindowManager.LayoutParams getPreviewWithKeyboardParams()
    {
        Point displaySize = getDisplaySize();
        Point size = getWidgetSize();
        Point pos = getWidgetPosition(size);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                size.y,
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                0,
                PixelFormat.TRANSLUCENT
        );

        params.gravity= Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = pos.y;
        if (params.y < 0) params.y = 0;
        if (params.y > displaySize.y) params.y = displaySize.y;
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|
                                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;

        return params;
    }

    private WindowManager.LayoutParams getFullScreenLayoutParams(boolean keyguard)
    {
        int type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        if (!keyguard) type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                /*WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE*/0,
                PixelFormat.TRANSLUCENT
        );
        return params;
    }

    private WindowManager.LayoutParams getTouchAreaLayoutParams(boolean keyguard)
    {
        int type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        if (!keyguard) type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

        Point displaySize = getDisplaySize();
        Point size = getWidgetSize();
        size.y = Math.min(size.y, mNPListView.getItemsHeight());
        Point pos = getWidgetPosition(size);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size.x,
                size.y,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        params.gravity= Gravity.TOP | Gravity.LEFT;
        params.x = pos.x;
        params.y = pos.y;

        // fix positioning if of limit
        if (params.x < 0) params.x = 0;
        if (params.y < 0) params.y = 0;
        if (params.x > displaySize.x) params.x = displaySize.x;
        if (params.y > displaySize.y) params.y = displaySize.y;
        if (params.width > displaySize.x - params.x) params.width = displaySize.x - params.x;
        if (params.height> displaySize.y - params.y) params.height = displaySize.y - params.y;

        return params;
    }

    private WindowManager.LayoutParams getLayoutParams(int padding)
    {
        Point displaySize = getDisplaySize();
        Point size = getWidgetSize();
        Point pos = getWidgetPosition(size);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size.x + padding * 2,
                size.y + padding * 2,
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                /*WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | */WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        params.gravity= Gravity.TOP | Gravity.LEFT;
        params.x = pos.x - padding;
        params.y = pos.y - padding;

        // fix positioning if of limit
        if (params.x < 0) params.x = 0;
        if (params.y < 0) params.y = 0;
        if (params.x > displaySize.x) params.x = displaySize.x;
        if (params.y > displaySize.y) params.y = displaySize.y;
        if (params.width > displaySize.x - params.x) params.width = displaySize.x - params.x;
        if (params.height> displaySize.y - params.y) params.height = displaySize.y - params.y;

        return params;
    }

    private void safeUpdateView(View v, WindowManager.LayoutParams params)
    {
        try
        {
            mWindowManager.updateViewLayout(v, params);
        }
        catch(Exception exp)
        {
            exp.printStackTrace();
            // view wasn't exist, there is no need updating it
        }
    }

    public void notifyDataChanged(int uid)
    {
        mNPListView.notifyDataChanged();

        // scroll to the new notification position
        if (uid > -1) {
            List<NotificationData> data = NotificationsService.getSharedInstance().getNotifications();
            for (int i = 0; i < data.size(); i++)
            {
                if (data.get(i).uid == uid) {
                    mNPListView.getListView().smoothScrollToPosition(i);
                    break;
                }
            }
        }

        mHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                Point size = getWidgetSize();
                Point pos = getWidgetPosition(size);
                mNPListView.updateSizeAndPosition(pos, size);
            }
        }, 0);

        if (mPreviewItem == null)
        {
            // schedule update view after the animation
            mHandler.postDelayed(mUpdateTouchAreaSize, mAnimationDuration*2);
        }
        else
        {
            List<NotificationData> data = new ArrayList<NotificationData>();
            if (NotificationsService.getSharedInstance() != null)
                data = NotificationsService.getSharedInstance().getNotifications();

            // check if the preview item still exists
            boolean exists = false;
            for (NotificationData ni : data)
                if (ni == mPreviewItem) exists = true;

            // close preview if the item is no longer exists
            if (!exists)
            {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

                if (prefs.getBoolean(SettingsManager.SHOW_NEXT_PREVIEW, SettingsManager.DEFAULT_SHOW_NEXT_PREVIEW))
                {
                    final List<NotificationData> finalData = data;
                    mHandler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (mVisible)
                                {
                                    hideKeyboardOnPreview();

                                    // show next or prev item if there is
                                    if (mPreviewPosition < finalData.size())
                                        showNotificationPreview(finalData.get(mPreviewPosition), mPreviewPosition);
                                    else if (mPreviewPosition - 1 >= 0 && mPreviewPosition -1 < finalData.size())
                                        showNotificationPreview(finalData.get(mPreviewPosition - 1), mPreviewPosition - 1);
                                    else if (finalData.size() > 0)
                                        showNotificationPreview(finalData.get(finalData.size()-1), finalData.size()-1);
                                }
                            }
                        }, mAnimationDuration);
                }
                else
                    hideNotificationPreview();
            }
        }
    }

    public void refreshLayout(boolean recreate)
    {
        // re-create adapter
        if (recreate)
            mNPListView.reloadAppearance();

        // calculate updated sizes
        Point size = getWidgetSize();
        Point pos = getWidgetPosition(size);

        // update notifications list
        mNPListView.updateSizeAndPosition(pos, size);
        mPreviewView.updateSizeAndPosition(pos, size);

        // update touch area
        safeUpdateView(mTouchAreaView, getTouchAreaLayoutParams(true));
    }
}