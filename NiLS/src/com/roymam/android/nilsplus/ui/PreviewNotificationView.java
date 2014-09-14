package com.roymam.android.nilsplus.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.roymam.android.common.BitmapUtils;
import com.roymam.android.nilsplus.ui.theme.Theme;
import com.roymam.android.nilsplus.ui.theme.ThemeManager;
import com.roymam.android.notificationswidget.NotificationData;
import com.roymam.android.notificationswidget.R;
import com.roymam.android.notificationswidget.SettingsManager;

import static java.lang.Math.abs;

public class PreviewNotificationView extends RelativeLayout {
    private static final String TAG = PreviewNotificationView.class.getSimpleName();
    private final SharedPreferences prefs;
    private ImageButton mQuickReplySendButton;
    private TextView mQuickReplyLabel;
    private View mPreviewNotificationView;
    private View mPreviewBackground;
    private View mQuickReplyBox;
    private EditText mQuickReplyText;
    private ImageView mAppIconBGImage;
    private TextView mPreviewTitle;
    private TextView mPreviewText;
    private ImageView mPreviewIcon;
    private TextView mPreviewTime;
    private int mAnimationDuration;
    private View mPreviewBody;
    private DotsSwipeView mDotsView;
    private View mPreviewIconBG;
    private ScrollView mScrollView;
    private ImageView mPreviewIconImageBG;
    private ImageView mPreviewIconImageFG;
    private View mNotificationContent;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private ImageView mPreviewBigPicture;
    private Theme mTheme;
    private ImageView mAppIconImage;
    private Context context;
    private int mTouchSlop;
    private int mViewWidth;
    private boolean mTouch;
    private NotificationData ni;
    private Callbacks mCallbacks;
    private VelocityTracker mVelocityTracker;
    private int mPrimaryTextColor;
    private int mNotificationBGColor;
    private boolean mIsSwipeToOpenEnabled;
    private int mLastPosY = 0;
    private int mLastPosX = 0;
    private int mLastSizeX = 0;
    private int mLastSizeY = 0;
    private int mIconSize;
    private int mStatusBarHeight;
    private boolean mVerticalDrag;
    private boolean mHorizontalDrag;
    private boolean mIgnoreTouch;
    private boolean mIsSoftKeyVisible = false;

    public void updateSizeAndPosition(Point pos, Point size)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrimaryTextColor = prefs.getInt(SettingsManager.PRIMARY_TEXT_COLOR, SettingsManager.DEFAULT_PRIMARY_TEXT_COLOR);
        mIconSize = prefs.getInt(SettingsManager.PREVIEW_ICON_SIZE, SettingsManager.DEFAULT_PREVIEW_ICON_SIZE);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size.x, size.y);
        params.leftMargin = pos.x;
        params.topMargin = 0;

        mLastPosX = pos.x;
        mLastPosY = pos.y;
        mLastSizeX = size.x;
        mLastSizeY = size.y;


        mStatusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
        {
            mStatusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        // set vertical alignment of the preview box
        //LayoutParams bgParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        String yAlignment = prefs.getString(SettingsManager.VERTICAL_ALIGNMENT, SettingsManager.DEFAULT_VERTICAL_ALIGNMENT);

        if (yAlignment.equals("center"))
            params.addRule(CENTER_VERTICAL);
        else if (yAlignment.equals("bottom"))
            params.addRule(ALIGN_PARENT_BOTTOM);

        mPreviewNotificationView.setLayoutParams(params);

        //mPreviewBackground.setLayoutParams(bgParams);
    }

    public void showQuickReplyBox() {
        if (mQuickReplyBox != null && prefs.getBoolean(SettingsManager.SHOW_QUICK_REPLY_ON_PREVIEW, SettingsManager.DEFAULT_SHOW_QUICK_REPLY_ON_PREVIEW)) {
            mQuickReplyBox.setVisibility(View.VISIBLE);
            mQuickReplyBox.setScaleY(0);
            mQuickReplyBox.animate().scaleY(1).setDuration(mAnimationDuration).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    showSoftKeyboard();

                    // scroll the text down again if there is additional text (because the textbox hide part of it)
                    if (ni.additionalText != null ) {
                        mScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                }
            });
            mQuickReplyLabel.setText(ni.getQuickReplyAction().title);
            mQuickReplyText.setText("");
        }
    }

    public void hideQuickReplyBox() {
        if (mQuickReplyBox != null)
        {
            mQuickReplyBox.setVisibility(View.GONE);
            hideSoftKeyboard();
        }
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
        // handling touching the view without interfering with the standard touch handling by the scrollview, textbox, etc..
        if (handleTouch(this, ev))
            return true;
        else
            return super.dispatchTouchEvent(ev);
    }

    public PreviewNotificationView(final Context ctxt, Point size, Point pos, DotsSwipeView dotsView)
    {
        super(ctxt);
        this.context = ctxt;
        prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);

        mDotsView = dotsView;

        mTheme = ThemeManager.getInstance(context).getCurrentTheme();

        // build view from resource
        LayoutInflater inflater = LayoutInflater.from(context);
        if (mTheme != null && mTheme.previewLayout != null) {
            ThemeManager.getInstance(context).reloadLayouts(mTheme);
            mPreviewNotificationView = inflater.inflate(mTheme.previewLayout, null);
        }
        else
            mPreviewNotificationView = inflater.inflate(R.layout.notification_preview, null);

        addView(mPreviewNotificationView, new RelativeLayout.LayoutParams(size.x, size.y));

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
            mScrollView = (ScrollView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_text_scrollview"));
            mPreviewBigPicture = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_big_picture"));

            if (mTheme.customLayoutIdMap != null && mTheme.customLayoutIdMap.get("app_icon") != 0)
                mAppIconImage = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("app_icon"));

            if (mTheme.customLayoutIdMap != null && mTheme.customLayoutIdMap.get("app_icon_bg") != 0)
                mAppIconBGImage = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("app_icon_bg"));

            if (mTheme.customLayoutIdMap != null && mTheme.customLayoutIdMap.get("quick_reply_box") != 0) {
                mQuickReplyBox = mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("quick_reply_box"));
                mQuickReplyText = (EditText) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("quick_reply_text"));
                mQuickReplyLabel = (TextView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("quick_reply_label"));
                mQuickReplySendButton = (ImageButton) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("quick_reply_button"));
            }
        } else {
            mNotificationContent = mPreviewNotificationView.findViewById(R.id.notification_body);
            mPreviewBody = mPreviewNotificationView.findViewById(R.id.notification_preview);
            mPreviewTitle = (TextView) mPreviewNotificationView.findViewById(R.id.notification_title);
            mPreviewText = (TextView) mPreviewNotificationView.findViewById(R.id.notification_text);
            mPreviewIconBG = mPreviewNotificationView.findViewById(R.id.notification_bg);
            mPreviewIcon = (ImageView) mPreviewNotificationView.findViewById(R.id.notification_image);
            mPreviewIconImageBG = (ImageView) mPreviewNotificationView.findViewById(R.id.icon_bg);
            mPreviewIconImageFG = (ImageView) mPreviewNotificationView.findViewById(R.id.icon_fg);
            mPreviewTime = (TextView) mPreviewNotificationView.findViewById(R.id.notification_time);
            mScrollView = (ScrollView) mPreviewNotificationView.findViewById(R.id.notification_text_scrollview);
            mPreviewBigPicture = (ImageView) mPreviewNotificationView.findViewById(R.id.notification_big_picture);
            mQuickReplyBox = mPreviewNotificationView.findViewById(R.id.quick_reply_box);
            mQuickReplyText = (EditText) mPreviewNotificationView.findViewById(R.id.quick_reply_text);
            mQuickReplyLabel = (TextView) mPreviewNotificationView.findViewById(R.id.quick_text_label);
            mQuickReplySendButton = (ImageButton) mPreviewNotificationView.findViewById(R.id.quick_reply_button);
         }

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
                    loc[0]+=mLastPosX;
                    loc[1]+=mLastPosY;
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

        if (mQuickReplySendButton != null)
            mQuickReplySendButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent();
                    Bundle params = new Bundle();
                    final NotificationData.Action action = ni.getQuickReplyAction();
                    params.putCharSequence(action.remoteInputs[0].getResultKey(), mQuickReplyText.getText());
                    RemoteInput.addResultsToIntent(action.remoteInputs, intent, params);
                    try {
                        action.actionIntent.send(context, 0, intent);
                        mCallbacks.onDismiss(ni);
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
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

        Log.d(TAG, "mLastPosY:" + mLastPosY + " mLastSizeY:" + mLastSizeY);
        //mPreviewNotificationView.setTranslationY(startRect.top - mLastPosY - mLastSizeY/2);
        int minheight = startRect.bottom - startRect.top;
        int maxheight = mLastSizeY;
        mPreviewNotificationView.setTranslationY(mPreviewNotificationView.getHeight()/2);
        mPreviewNotificationView.setTranslationX(0);
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
        if (ni.additionalText != null ) {
            mPreviewText.setText(ni.additionalText);
            mScrollView.fullScroll(View.FOCUS_DOWN);
        }
        else
        {
            mScrollView.fullScroll(View.FOCUS_UP);
        }
        mPreviewTitle.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);
        mPreviewText.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);
        mPreviewTime.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);
        if (mQuickReplyLabel != null) mQuickReplyLabel.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);
        if (mQuickReplyText != null) mQuickReplyText.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);

        mPreviewTitle.setTextSize(prefs.getInt(SettingsManager.TITLE_FONT_SIZE, SettingsManager.DEFAULT_TITLE_FONT_SIZE));
        mPreviewText.setTextSize(prefs.getInt(SettingsManager.TEXT_FONT_SIZE, SettingsManager.DEFAULT_TEXT_FONT_SIZE));
        if (mQuickReplyLabel != null) mQuickReplyLabel.setTextSize(prefs.getInt(SettingsManager.TITLE_FONT_SIZE, SettingsManager.DEFAULT_TITLE_FONT_SIZE));
        if (mQuickReplyText != null) mQuickReplyText.setTextSize(prefs.getInt(SettingsManager.TEXT_FONT_SIZE, SettingsManager.DEFAULT_TEXT_FONT_SIZE));

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
        if (mQuickReplyLabel != null) mQuickReplyLabel.setTextColor(mPrimaryTextColor);

        if (ni.appColor != 0 && prefs.getBoolean(SettingsManager.AUTO_TITLE_COLOR, false)) {
            mPreviewTitle.setTextColor(ni.appColor);
            if (mQuickReplyLabel != null) mQuickReplyLabel.setTextColor(ni.appColor);
        }
        mPreviewText.setTextColor(secondaryTextColor);
        if (mQuickReplyText != null) mQuickReplyText.setTextColor(secondaryTextColor);

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

    private boolean isTouchHitView(View v, MotionEvent ev)
    {
        // if view is not visible - return false
        if (v == null || v.getVisibility() != View.VISIBLE) return false;

        int[] parentCords = new int[2];
        mPreviewBackground.getLocationOnScreen(parentCords);
        int x = (int)ev.getRawX() - parentCords[0];
        int y = (int)ev.getRawY() - parentCords[1];

        Rect rect = new Rect();
        v.getHitRect(rect);

        if (rect.contains(x, y)) {
            return true;
        }
        return false;
    }

    public boolean handleTouch(View v, MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            // ignore start dragging quick reply box or the notification icon
            if (isTouchHitView(mQuickReplyBox, event)) {
                showSoftKeyboard();
                mIgnoreTouch = true;
                return false;
            }
            if (isTouchHitView(mPreviewIcon,event)) {
                mIgnoreTouch = true;
                return false;
            }
            mIgnoreTouch = false;

            mTouchStartX = event.getRawX();
            mTouchStartY = event.getRawY();
            mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            mViewWidth = mPreviewNotificationView.getWidth();
            mTouch = true;
            mHorizontalDrag = false;
            mVerticalDrag = false;
            mVelocityTracker = VelocityTracker.obtain();
            mVelocityTracker.addMovement(event);

            // store pref for use later
            mIsSwipeToOpenEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsManager.SWIPE_TO_OPEN, SettingsManager.DEFAULT_SWIPE_TO_OPEN);
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE)
        {
            if (mTouch && !mIgnoreTouch)
            {
                mVelocityTracker.addMovement(event);
                float deltaX = event.getRawX() - mTouchStartX;
                float deltaY = event.getRawY() - mTouchStartY;

                if (abs(deltaX) > mTouchSlop)
                    mHorizontalDrag = true;

                if (abs(deltaY) > mTouchSlop)
                    mVerticalDrag = true;

                if (mVerticalDrag && !mHorizontalDrag)
                {
                    // cancel swipe & click - keep only scroll text
                    mTouch = false;

                    // reset horizontal dragging
                    mPreviewNotificationView.setTranslationX(0);
                    mPreviewNotificationView.setAlpha(1);

                    // pass this event so the scrollview will handle the vertical scrolling
                    return false;
                }
                if (mHorizontalDrag)
                {
                    // update position and opacity according the swiping gesture
                    mPreviewNotificationView.setTranslationX(deltaX);
                    mPreviewNotificationView.setAlpha((mViewWidth- abs(deltaX))/mViewWidth);

                    // prevent other controls receiving this event
                    return true;
                }
            }
            else // unhandled - pass it to the child views
                return false;
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            if (mTouch && !mIgnoreTouch) {
                mTouch = false;

                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float absVelocityX = Math.abs(velocityX);
                float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                float deltaX = event.getRawX() - mTouchStartX;
                if (abs(deltaX) > mViewWidth / 2 ||
                        mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity && absVelocityY < absVelocityX) {
                    // animate dismiss
                    int w;
                    final boolean swipeRight = (deltaX > 0);
                    if (swipeRight)
                        w = mViewWidth;
                    else
                        w = -mViewWidth;

                    // swipe animation
                    mPreviewNotificationView.animate().translationX(w).alpha(0).setDuration(mAnimationDuration)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    // dismiss notification
                                    if (mCallbacks != null) {
                                        if (mIsSwipeToOpenEnabled && swipeRight)
                                            mCallbacks.onOpen(ni);
                                        else
                                            mCallbacks.onDismiss(ni);
                                    }
                                }
                            });
                } else // the swipe wasn't fast enough - restoring the item to the original position
                {
                    mPreviewNotificationView.animate().translationX(0).alpha(1).setDuration(mAnimationDuration).setListener(null);
                }

                // if the user actually didn't drag at all - it is a click
                if (!mHorizontalDrag && !mVerticalDrag) {
                    InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    // if soft keyboard is visible - hide it
                    if (mIsSoftKeyVisible) {
                        hideSoftKeyboard();
                    } else if (mCallbacks != null) mCallbacks.onClick();

                    // make sure other views won't get this event
                    return true;
                }
            }
            else
                // unhandled - pass it to the child views
                return false;
        }

        // unhandled - pass it to the child views
        return false;
    }

    private void showSoftKeyboard() {
        mQuickReplyText.requestFocus();
        InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(mQuickReplyText, InputMethodManager.SHOW_IMPLICIT);
        mgr.restartInput(mQuickReplyText);
        mIsSoftKeyVisible = true;
    }

    private void hideSoftKeyboard() {
        InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(mQuickReplyText.getWindowToken(), 0);
        mIsSoftKeyVisible = false;
    }

    public void cleanup()
    {
        mScrollView.setOnTouchListener(null);
        mPreviewNotificationView.setOnTouchListener(null);
        mPreviewIcon.setOnTouchListener(null);
        mPreviewNotificationView.setOnClickListener(null);
    }
}