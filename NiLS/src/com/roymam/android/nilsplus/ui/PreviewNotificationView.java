package com.roymam.android.nilsplus.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.roymam.android.common.BitmapUtils;
import com.roymam.android.notificationswidget.NotificationData;
import com.roymam.android.notificationswidget.R;
import com.roymam.android.notificationswidget.SettingsManager;
import com.roymam.android.nilsplus.ui.theme.Theme;
import com.roymam.android.nilsplus.ui.theme.ThemeManager;

import static java.lang.Math.abs;

public class PreviewNotificationView extends RelativeLayout implements View.OnTouchListener
{
    private static final String TAG = PreviewNotificationView.class.getSimpleName();
    private final View mPreviewNotificationView;
    private final View mPreviewBackground;
    private ImageView mAppIconBGImage;
    private TextView mPreviewTitle;
    private TextView mPreviewText;
    private ImageView mPreviewIcon;
    private TextView mPreviewTime;
    private final int mAnimationDuration;
    private View mPreviewBody;
    private final DotsSwipeView mDotsView;
    private View mPreviewIconBG;
    private View mScrollView;
    private ImageView mPreviewIconImageBG;
    private ImageView mPreviewIconImageFG;
    private View mNotificationContent;
    private final int mMinFlingVelocity;
    private final int mMaxFlingVelocity;
    private ImageView mPreviewBigPicture;
    private final Theme mTheme;
    private ImageView mAppIconImage;
    private Context context;
    private int mTouchSlop;
    private int mViewWidth;
    private boolean mIsClick;
    private boolean mTouch;
    private NotificationData ni;
    private Callbacks mCallbacks;
    private VelocityTracker mVelocityTracker;
    private int mPrimaryTextColor;
    private int mNotificationBGColor;
    private boolean mIsSwipeToOpenEnabled;
    private boolean mIconSwiping = false;
    private int mLastPosY = 0;
    private int mLastPosX = 0;
    private int mLastSizeX = 0;
    private int mLastSizeY = 0;
    private int mIconSize;
    private int mStatusBarHeight;

    public void setIconSwiping(boolean mIconSwiping)
    {
        this.mIconSwiping = mIconSwiping;
    }

    public void updateSizeAndPosition(Point pos, Point size)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrimaryTextColor = prefs.getInt(SettingsManager.PRIMARY_TEXT_COLOR, SettingsManager.DEFAULT_PRIMARY_TEXT_COLOR);
        mIconSize = prefs.getInt(SettingsManager.PREVIEW_ICON_SIZE, SettingsManager.DEFAULT_PREVIEW_ICON_SIZE);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size.x, size.y);
        params.leftMargin = pos.x;
        params.topMargin = pos.y;

        mLastPosX = pos.x;
        mLastPosY = pos.y;
        mLastSizeX = size.x;
        mLastSizeY = size.y;

        mPreviewNotificationView.setLayoutParams(params);

        mStatusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
        {
            mStatusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        // set vertical alignment of the preview box
        LayoutParams bgParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        String yAlignment = prefs.getString(SettingsManager.VERTICAL_ALIGNMENT, SettingsManager.DEFAULT_VERTICAL_ALIGNMENT);

        if (yAlignment.equals("center"))
            bgParams.addRule(CENTER_VERTICAL);
        else if (yAlignment.equals("bottom"))
            bgParams.addRule(ALIGN_PARENT_BOTTOM);

        mPreviewBackground.setLayoutParams(bgParams);
    }

    public interface Callbacks
    {
        public void onDismiss(NotificationData ni);
        public void onOpen(NotificationData ni);
        public void onClick();
        public void onAction(NotificationData ni, int actionPos);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        // pass the event to the scrolling text too
        mScrollView.dispatchTouchEvent(ev);

        return super.dispatchTouchEvent(ev);
    }

    public PreviewNotificationView(Context context, Point size, Point pos)
    {
        super(context);
        this.context = context;
        mTheme = ThemeManager.getInstance(context).getCurrentTheme();

        // build view from resource
        LayoutInflater inflater = LayoutInflater.from(context);
        if (mTheme != null && mTheme.previewLayout != null) {
            ThemeManager.getInstance(context).reloadLayouts(mTheme);
            mPreviewNotificationView = inflater.inflate(mTheme.previewLayout, null);
        }
        else
            mPreviewNotificationView = inflater.inflate(R.layout.notification_preview, null);

        mDotsView = new DotsSwipeView(context, pos, size);
        mDotsView.setAlpha(0);
        mDotsView.setVisibility(View.GONE);
        addView(mDotsView, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(mPreviewNotificationView, new LayoutParams(size.x, size.y));

        if (mTheme != null && mTheme.previewLayout != null)
            mPreviewBackground = mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("full_notification"));
        else
            mPreviewBackground = mPreviewNotificationView.findViewById(R.id.full_notification);

        updateSizeAndPosition(pos, size);
        hideImmediate();

        // get fields
        if (mTheme != null && mTheme.previewLayout != null) {
            mNotificationContent = mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_body"));
            mPreviewBody = mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_preview"));
            mPreviewTitle = (TextView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_title"));
            mPreviewText = (TextView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_text"));
            mPreviewIconBG = mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_bg"));
            mPreviewIcon = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_image"));
            mPreviewIconImageBG = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("icon_bg"));
            mPreviewIconImageFG = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("icon_fg"));
            mPreviewTime = (TextView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_time"));
            mScrollView = mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_text_scrollview"));
            mPreviewBigPicture = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_big_picture"));        }

            if (mTheme.customLayoutIdMap != null && mTheme.customLayoutIdMap.get("app_icon") != null)
                mAppIconImage = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("app_icon"));

            if (mTheme.customLayoutIdMap != null && mTheme.customLayoutIdMap.get("app_icon_bg") != null)
                mAppIconBGImage = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("app_icon_bg"));

        else {
            mNotificationContent = mPreviewNotificationView.findViewById(R.id.notification_body);
            mPreviewBody = mPreviewNotificationView.findViewById(R.id.notification_preview);
            mPreviewTitle = (TextView) mPreviewNotificationView.findViewById(R.id.notification_title);
            mPreviewText = (TextView) mPreviewNotificationView.findViewById(R.id.notification_text);
            mPreviewIconBG = mPreviewNotificationView.findViewById(R.id.notification_bg);
            mPreviewIcon = (ImageView) mPreviewNotificationView.findViewById(R.id.notification_image);
            mPreviewIconImageBG = (ImageView) mPreviewNotificationView.findViewById(R.id.icon_bg);
            mPreviewIconImageFG = (ImageView) mPreviewNotificationView.findViewById(R.id.icon_fg);
            mPreviewTime = (TextView) mPreviewNotificationView.findViewById(R.id.notification_time);
            mScrollView = mPreviewNotificationView.findViewById(R.id.notification_text_scrollview);
            mPreviewBigPicture = (ImageView) mPreviewNotificationView.findViewById(R.id.notification_big_picture);
        }

        // set listeners
        mScrollView.setOnTouchListener(this);

        // set touch listener for swiping out
        setOnTouchListener(this);

        ViewConfiguration vc = ViewConfiguration.get(context);
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationDuration = Resources.getSystem().getInteger(android.R.integer.config_shortAnimTime);

        mPreviewIcon.setOnTouchListener(new OnTouchListener()
        {
            public int mActionSelected;
            private boolean mDown = false;

            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    mDown = true;
                    mPreviewNotificationView.animate().alpha(0).setDuration(mAnimationDuration).setListener(null);

                    // init dots view
                    int loc[] = new int[2];
                    mPreviewBackground.getLocationInWindow(loc);
                    int w = mPreviewBackground.getWidth();
                    int h = mPreviewBackground.getHeight();
                    mDotsView.updateSizeAndPosition(new Point(loc[0],loc[1]), new Point(w,h));
                    Rect r = new Rect(0,0, BitmapUtils.dpToPx(mIconSize), BitmapUtils.dpToPx(mIconSize));
                    mDotsView.setIcons(r, ni.getAppIcon(),
                            ni.getActions().length > 0?ni.getActions()[0].drawable:null,
                            ni.getActions().length > 1?ni.getActions()[1].drawable:null);

                    mDotsView.setVisibility(View.VISIBLE);
                    mDotsView.animate().alpha(1).setDuration(mAnimationDuration).setListener(null);
                    mDotsView.dispatchTouchEvent(event);
                    mActionSelected = -1;
                }
                else if (event.getAction() == MotionEvent.ACTION_MOVE)
                {
                    mDotsView.dispatchTouchEvent(event);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    mDown = false;
                    mIconSwiping = false;

                    mDotsView.dispatchTouchEvent(event);
                    mDotsView.animate().alpha(0).setDuration(mAnimationDuration).setListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            mDotsView.setVisibility(View.GONE);
                        }
                    });
                    mPreviewNotificationView.animate().alpha(1).setDuration(mAnimationDuration).setListener(null);
                    mActionSelected = mDotsView.getSelected();

                    if (mActionSelected == 0)
                    {
                        mCallbacks.onOpen(ni);
                    }
                    else if (mActionSelected == 1)
                    {
                        try
                        {
                            if (mCallbacks != null)
                                mCallbacks.onAction(ni, 0);
                            else
                                ni.getActions()[0].actionIntent.send();
                        } catch (PendingIntent.CanceledException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else if (mActionSelected == 2)
                    {
                        try
                        {
                            if (mCallbacks != null)
                                mCallbacks.onAction(ni, 1);
                            else
                                ni.getActions()[1].actionIntent.send();

                        } catch (PendingIntent.CanceledException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
                return true;
            }
        });

        setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mCallbacks != null) mCallbacks.onClick();
            }
        });
    }

    public void hideImmediate()
    {
        mPreviewNotificationView.setAlpha(0);
        mPreviewNotificationView.setScaleY(0);
        mPreviewNotificationView.setVisibility(View.GONE);
        setVisibility(View.GONE);
    }

    public void hide(int yOffset)
    {
        mPreviewNotificationView.animate().translationY(yOffset).alpha(0).scaleY(0).setDuration(mAnimationDuration).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation)
                {
                    mPreviewNotificationView.setVisibility(View.GONE);
                    setVisibility(View.GONE);
                }
            });
    }

    public void setSizeAndPosition(Point size, Point pos)
    {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size.x,size.y);
        params.leftMargin = pos.x;
        params.topMargin = pos.y;
        mPreviewNotificationView.setLayoutParams(params);
    }

    public void show(Rect startRect)
    {
        setVisibility(View.VISIBLE);

        mPreviewNotificationView.setTranslationY(startRect.top - mLastPosY - mLastSizeY/2);
        mPreviewNotificationView.setTranslationX(0);

        int minheight = startRect.bottom - startRect.top;
        int maxheight = mLastSizeY;
        mPreviewNotificationView.setScaleY(minheight/maxheight);

        mPreviewNotificationView.setAlpha(1);
        mPreviewNotificationView.setVisibility(View.VISIBLE);
        mPreviewNotificationView.animate().scaleY(1.0f).alpha(1).translationY(0).setDuration(mAnimationDuration).setListener(null);
    }

    public void setContent(NotificationData ni, Callbacks callbacks)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Theme theme = ThemeManager.getInstance(context).getCurrentTheme();

        // set appearance settings and theme
        mPrimaryTextColor = prefs.getInt(SettingsManager.PRIMARY_TEXT_COLOR, SettingsManager.DEFAULT_PRIMARY_TEXT_COLOR);
        int secondaryTextColor = prefs.getInt(SettingsManager.SECONDARY_TEXT_COLOR, SettingsManager.DEFAULT_SECONDARY_TEXT_COLOR);
        mNotificationBGColor = prefs.getInt(SettingsManager.MAIN_BG_COLOR, SettingsManager.DEFAULT_MAIN_BG_COLOR);
        int iconBGColor = prefs.getInt(SettingsManager.ICON_BG_COLOR, SettingsManager.DEFAULT_ICON_BG_COLOR);

        this.ni = ni;
        this.mCallbacks = callbacks;

        mPreviewTitle.setText(ni.getTitle()!=null?ni.getTitle().toString():null);
        mPreviewText.setText(ni.getText() != null ? ni.getText().toString() : null);
        mPreviewTitle.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);
        mPreviewText.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);
        mPreviewTime.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);
        mPreviewTitle.setTextSize(prefs.getInt(SettingsManager.TITLE_FONT_SIZE, SettingsManager.DEFAULT_TITLE_FONT_SIZE));
        mPreviewText.setTextSize(prefs.getInt(SettingsManager.TEXT_FONT_SIZE, SettingsManager.DEFAULT_TEXT_FONT_SIZE));
        mPreviewTime.setTextSize(prefs.getInt(SettingsManager.TEXT_FONT_SIZE, SettingsManager.DEFAULT_TEXT_FONT_SIZE));
        Bitmap icon = NotificationAdapter.createThemedIcon(ni.getIcon(), theme, (int) context.getResources().getDimension(R.dimen.notification_icon_size_large));
        mPreviewIcon.setImageDrawable(new BitmapDrawable(getResources(), icon));

        if (theme.iconBg != null)
            theme.iconBg.setAlpha(255 * prefs.getInt(SettingsManager.MAIN_BG_OPACITY, SettingsManager.DEFAULT_MAIN_BG_OPACITY) / 100);

        if (mAppIconImage != null)
        {
            Bitmap appIcon = ni.getAppIcon();
            // show app icon only if the primary icon is a large icon
            if (ni.largeIcon != null)
            {
                mAppIconImage.setImageDrawable(new BitmapDrawable(appIcon));
                if (mAppIconBGImage != null && theme.appIconBg != null)
                {
                    Drawable appIconBgDrawable = theme.appIconBg;

                    if (theme.prominentAppIconBg)
                    {
                        if (theme.appIconBg instanceof BitmapDrawable)
                        {
                            appIconBgDrawable = new BitmapDrawable(BitmapUtils.colorBitmap(((BitmapDrawable)theme.appIconBg).getBitmap(), ni.appColor));
                        }
                        else
                        {
                            Log.w(TAG, "invalid theme. prominent app icon background works only with BitmapDrawable");
                        }
                    }
                    mAppIconBGImage.setImageDrawable(appIconBgDrawable);
                }
            }
            else
            {
                if (mAppIconBGImage != null) mAppIconBGImage.setImageDrawable(null);
                mAppIconImage.setImageDrawable(null);

                // for Android L notifications - set main icon as the app icon (the small monochrome one) instead of the colored one)
                // TODO: make it optional in the theme booleans
                mPreviewIcon.setImageDrawable(new BitmapDrawable(NotificationAdapter.createThemedIcon(ni.getAppIcon(), theme, BitmapUtils.dpToPx(prefs.getInt(SettingsManager.ICON_SIZE, SettingsManager.DEFAULT_ICON_SIZE)))));
            }
        }

        Drawable iconBgImage = theme.iconBg;
        if (iconBgImage != null)
            iconBgImage.setAlpha(255 * prefs.getInt(SettingsManager.MAIN_BG_OPACITY, SettingsManager.DEFAULT_MAIN_BG_OPACITY) / 100);

        if (theme.prominentIconBg)
        {
            if (iconBgImage instanceof BitmapDrawable)
            {
                iconBgImage = new BitmapDrawable(BitmapUtils.colorBitmap(((BitmapDrawable)iconBgImage).getBitmap(), ni.appColor));
            }
            else
            {
                Log.w(TAG, "invalid theme. prominent icon background works only with BitmapDrawable");
            }
        }

        mPreviewIconImageBG.setImageDrawable(iconBgImage);
        mPreviewIconImageFG.setImageDrawable(theme.iconFg);
        if (theme.previewTextBG != null)
            theme.previewTextBG.setAlpha(255 * prefs.getInt(SettingsManager.MAIN_BG_OPACITY, SettingsManager.DEFAULT_MAIN_BG_OPACITY) / 100);
        mNotificationContent.setBackgroundDrawable(theme.previewTextBG);
        mPreviewTime.setText(ni.getTimeText(context));

        // set colors
        mPreviewTitle.setTextColor(mPrimaryTextColor);
        if (ni.appColor != 0 && prefs.getBoolean(SettingsManager.AUTO_TITLE_COLOR, false))
            mPreviewTitle.setTextColor(ni.appColor);
        mPreviewText.setTextColor(secondaryTextColor);
        mPreviewTime.setTextColor(secondaryTextColor);
        mPreviewBackground.setBackgroundColor(mNotificationBGColor);
        mPreviewIconBG.setBackgroundColor(iconBGColor);

        // apply theme
        if (theme.previewBG != null)
        {
            theme.previewBG.setAlpha(255 * prefs.getInt(SettingsManager.MAIN_BG_OPACITY, SettingsManager.DEFAULT_MAIN_BG_OPACITY) / 100);
            mPreviewBackground.setBackgroundDrawable(theme.previewBG);
        }

        Bitmap largestBitmap = null;
        if (ni.bitmaps != null)
            for(Bitmap bitmap : ni.bitmaps)
            {
                if (largestBitmap == null)
                    largestBitmap = bitmap;
                else
                    if (largestBitmap.getHeight()*largestBitmap.getWidth() <
                        bitmap.getHeight()*bitmap.getWidth())
                        largestBitmap = bitmap;
            }

        if (largestBitmap != null &&
                (largestBitmap.getWidth() > context.getResources().getDimension(R.dimen.big_picture_min_size) ||
                 largestBitmap.getHeight() > context.getResources().getDimension(R.dimen.big_picture_min_size)))
            mPreviewBigPicture.setImageBitmap(largestBitmap);
        else
            mPreviewBigPicture.setImageBitmap(null);

        // apply font style and size if available
        if (theme.timeFontSize != -1) mPreviewTime.setTextSize(theme.timeFontSize);
        if (theme.titleTypeface != null) mPreviewTitle.setTypeface(theme.titleTypeface);
        if (theme.textTypeface != null) mPreviewText.setTypeface(theme.titleTypeface);
        if (theme.timeTypeface != null) mPreviewTime.setTypeface(theme.titleTypeface);

    }

    // touch handling
    float mTouchStartX;
    float mTouchStartY;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        // if the touch is on the icon - pass the event to it
        Rect iconHitRect = new Rect();
        mPreviewIcon.getHitRect(iconHitRect);

        int[] parentCords = new int[2];
        mPreviewBackground.getLocationOnScreen(parentCords);
        int x = (int)event.getRawX() - parentCords[0];
        int y = (int)event.getRawY() - parentCords[1];

        // if the app icon is currently dragging - pass the touch event to it
        if (iconHitRect.contains(x,y) && event.getAction() == MotionEvent.ACTION_DOWN)
            mIconSwiping = true;

        if (mIconSwiping)
        {
            mPreviewIcon.dispatchTouchEvent(event);
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            mTouchStartX = event.getRawX();
            mTouchStartY = event.getRawY();
            mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            mViewWidth = mPreviewNotificationView.getWidth();
            mIsClick = true;
            mTouch = true;
            mVelocityTracker = VelocityTracker.obtain();
            mVelocityTracker.addMovement(event);

            // store pref for use later
            mIsSwipeToOpenEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsManager.SWIPE_TO_OPEN, SettingsManager.DEFAULT_SWIPE_TO_OPEN);
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE)
        {
            if (mTouch)
            {
                mVelocityTracker.addMovement(event);
                float deltaX = event.getRawX() - mTouchStartX;
                float deltaY = event.getRawY() - mTouchStartY;
                if (abs(deltaX) > mTouchSlop)
                {
                    mIsClick = false;
                }

                if ((abs(deltaY) > mTouchSlop) && mIsClick)
                {
                    // cancel swipe & click - keep only scroll text
                    mIsClick = false;
                    mTouch = false;
                    mPreviewNotificationView.setTranslationX(0);
                    mPreviewNotificationView.setAlpha(1);
                    return false;
                }
                if (!mIsClick)
                {
                    mPreviewNotificationView.setTranslationX(deltaX);
                    mPreviewNotificationView.setAlpha((mViewWidth- abs(deltaX))/mViewWidth);
                    return true;
                }
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            if (mTouch)
            {
                mTouch = false;

                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float absVelocityX = Math.abs(velocityX);
                float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                float deltaX = event.getRawX() - mTouchStartX;
                if (abs(deltaX) > mViewWidth/2 ||
                    mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity && absVelocityY < absVelocityX)
                {
                    // animate dismiss
                    int w;
                    final boolean swipeRight = (deltaX > 0);
                    if (swipeRight)
                        w = mViewWidth;
                    else
                        w = -mViewWidth;

                    // swipe animation
                    mPreviewNotificationView.animate().translationX(w).alpha(0).setDuration(mAnimationDuration)
                    .setListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            // dismiss notification
                            if (mCallbacks != null)
                            {
                                if (mIsSwipeToOpenEnabled && swipeRight) mCallbacks.onOpen(ni);
                            }
                        }
                    });

                    mIsClick = false;
                }
                else
                {
                    mPreviewNotificationView.animate().translationX(0).alpha(1).setDuration(mAnimationDuration).setListener(null);
                }
            }

            if (mIsClick)
            {
                callOnClick();
                return false;
            }
            else
            {
                return true;
            }
        }
        return false;
    }

    public void cleanup()
    {
        mScrollView.setOnTouchListener(null);
        setOnTouchListener(null);
        mPreviewIcon.setOnTouchListener(null);
        setOnClickListener(null);
    }
}