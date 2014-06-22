package com.roymam.android.nilsplus.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.roymam.android.common.BitmapUtils;
import com.roymam.android.notificationswidget.R;

import static java.lang.Math.sqrt;

public class DotsSwipeView extends RelativeLayout implements View.OnTouchListener
{
    private final Context mContext;
    private final Bitmap mCircleMask;
    private final Bitmap mDots;
    private final View mDotsView;
    private final View mHighlightView;
    private final Point mHighlightSize;
    private final int mIconSize;
    private Rect mAreaRect;

    private boolean mAction1selected;
    private boolean mAction2selected;
    private boolean mAction3selected;

    private final Point mCircleSize;
    private Point mPos;
    private Point mSize;

    // app actions icons
    private final RelativeLayout mAction1IconCotainer;
    private final View mAction1IconView;

    private Bitmap mAction1Bitmap;
    private final RelativeLayout mAction2IconCotainer;
    private final View mAction2IconView;

    private Bitmap mAction3Bitmap;
    private final RelativeLayout mAction3IconCotainer;
    private final View mAction3IconView;

    private Bitmap mAction2Bitmap;
    private int x1;
    private int x2;
    private int x3;
    private int y;
    private int mStatusBarHeight;

    public DotsSwipeView(Context context, Point pos, Point size)
    {
        super(context);

        // store parameters
        mContext = context;
        mPos = pos;
        mSize = size;

        // load mask image
        mCircleMask = BitmapFactory.decodeResource(context.getResources(), R.drawable.circlemask);

        // create dots bg
        mCircleSize = new Point(mCircleMask.getWidth(), mCircleMask.getHeight());
        mDots = BitmapUtils.createDots(mCircleSize.x*2, mCircleSize.y*2, Color.WHITE, 3, 16);

        // set touch listener
        setOnTouchListener(this);

        // add dots bg to the view
        mDotsView = new View(mContext);
        LayoutParams params = new LayoutParams(mCircleSize.x, mCircleSize.y);
        params.leftMargin = 0;
        params.topMargin = 0;
        addView(mDotsView, params);

        // white circle
        Bitmap whiteCircle = BitmapFactory.decodeResource(context.getResources(), R.drawable.whitecircle);
        mHighlightSize = new Point(whiteCircle.getWidth(), whiteCircle.getHeight());

        mHighlightView = new View(mContext);
        BitmapDrawable highlightBg = new BitmapDrawable(getResources(), whiteCircle);
        highlightBg.setGravity(Gravity.TOP | Gravity.LEFT);
        mHighlightView.setBackgroundDrawable(highlightBg);
        mHighlightView.setVisibility(View.INVISIBLE);

        addView(mHighlightView, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // action buttons
        mIconSize = (int) mContext.getResources().getDimension(R.dimen.notification_icon_size_small);

        LayoutParams appIconParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        appIconParams.addRule(CENTER_IN_PARENT);

        mAction1IconCotainer = new RelativeLayout(mContext);
        mAction1IconView = new View(mContext);
        mAction1IconCotainer.addView(mAction1IconView, appIconParams);
        addView(mAction1IconCotainer, new LayoutParams(mIconSize, mIconSize));

        mAction2IconCotainer = new RelativeLayout(mContext);
        mAction2IconView = new View(mContext);
        mAction2IconCotainer.addView(mAction2IconView, appIconParams);
        addView(mAction2IconCotainer, new LayoutParams(mIconSize, mIconSize));

        mAction3IconCotainer = new RelativeLayout(mContext);
        mAction3IconView = new View(mContext);
        mAction3IconCotainer.addView(mAction3IconView, appIconParams);
        addView(mAction3IconCotainer, new LayoutParams(mIconSize, mIconSize));
    }

    public void setIcons(Rect rect, Bitmap action1Icon, Bitmap action2Icon, Bitmap action3Icon)
    {
        // set icons position
        mAreaRect = rect;

        // update action buttons position
        int x0 = mPos.x + mHighlightSize.x / 2;
        x1 = mPos.x + mSize.x - mHighlightSize.x / 2;
        y = mPos.y + rect.top + rect.height() / 2;

        if (action3Icon != null) // 3 icons
        {
            x2 = x0 + (x1 - x0) * 2 / 3;
            x3 = x0 + (x1 - x0) * 1 / 3;
        }
        else // 2 icons
        {
            x2 = (x0 + x1) / 2;
            x3 = 0;
        }

        LayoutParams params = (LayoutParams) mAction1IconCotainer.getLayoutParams();
        params.leftMargin = x1 - mIconSize / 2;
        params.topMargin = y - mIconSize / 2;
        mAction1IconCotainer.requestLayout();


        params = (LayoutParams) mAction2IconCotainer.getLayoutParams();
        params.leftMargin = x2 - mIconSize / 2;
        params.topMargin = y - mIconSize / 2;
        mAction2IconCotainer.requestLayout();

        params = (LayoutParams) mAction3IconCotainer.getLayoutParams();
        params.leftMargin = x3 - mIconSize / 2;
        params.topMargin = y - mIconSize / 2;
        mAction3IconCotainer.requestLayout();

        LayoutParams hiparams = (LayoutParams) mHighlightView.getLayoutParams();
        hiparams.leftMargin = x1 - mHighlightSize.x / 2;
        hiparams.topMargin = y - mHighlightSize.y / 2;
        mHighlightView.requestLayout();

        mAction1Bitmap = action1Icon;
        mAction1IconView.setBackgroundDrawable(new BitmapDrawable(mAction1Bitmap));

        mAction2Bitmap = action2Icon;
        mAction2IconView.setBackgroundDrawable(new BitmapDrawable(mAction2Bitmap));

        mAction3Bitmap = action3Icon;
        mAction3IconView.setBackgroundDrawable(new BitmapDrawable(mAction3Bitmap));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
                mStatusBarHeight = 0;
                int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resourceId > 0)
                {
                    mStatusBarHeight = getResources().getDimensionPixelSize(resourceId);
                }

                mHighlightView.setVisibility(INVISIBLE);
                mDotsView.setVisibility(VISIBLE);

                // animate action buttons
                animateActionIcon(mAction1IconCotainer);
                animateActionIcon(mAction2IconCotainer);
                animateActionIcon(mAction3IconCotainer);
                mAction1selected = false;
                mAction2selected = false;
                mAction3selected = false;

                if (Settings.System.getInt(mContext.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1)
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

            case MotionEvent.ACTION_MOVE:
                // calc circle position
                int x = (int) event.getRawX() - mCircleSize.x / 2;
                int y = (int) event.getRawY() - mCircleSize.y / 2 - mStatusBarHeight;

                RelativeLayout.LayoutParams params = (LayoutParams) mDotsView.getLayoutParams();
                params.leftMargin = x;
                params.topMargin = y;

                // redraw circle with mask
                int xoffset = x % mCircleSize.x;
                int yoffset = y % mCircleSize.y;

                Bitmap circleWithMask = BitmapUtils.drawBitmapOnMask(mDots, mCircleMask, xoffset, yoffset);
                BitmapDrawable bg = new BitmapDrawable(getResources(), circleWithMask);
                bg.setGravity(Gravity.TOP | Gravity.LEFT);
                mDotsView.setBackgroundDrawable(bg);

                // refresh dots view
                mDotsView.requestLayout();

                // check if user reached app icon
                // now select the new selected icon
                mAction1selected = checkSelectAction(mAction1IconView, mAction1Bitmap, (int) event.getRawX(), (int) event.getRawY(), 0, mAction1selected);
                mAction2selected = checkSelectAction(mAction2IconView, mAction2Bitmap, (int) event.getRawX(), (int) event.getRawY(), 1, mAction2selected);
                mAction3selected = checkSelectAction(mAction3IconView, mAction3Bitmap, (int) event.getRawX(), (int) event.getRawY(), 2, mAction3selected);

                break;
            case MotionEvent.ACTION_UP:
                //mHighlightView.setVisibility(INVISIBLE);
                //mDotsView.setVisibility(VISIBLE);
                break;
        }
        return true;
    }

    private void animateActionIcon(RelativeLayout mAction1IconContainer)
    {
        mAction1IconContainer.setTranslationX(-mIconSize * 2);
        mAction1IconContainer.setAlpha(0);
        mAction1IconContainer.setScaleX(0.5f);
        mAction1IconContainer.setScaleY(0.5f);
        mAction1IconContainer.animate().alpha(1).translationX(0).scaleX(1).scaleY(1);
    }

    private boolean checkSelectAction(View actionIconView, Bitmap actionBitmap, int rawX, int rawY, int index, boolean selected)
    {
        float w = mHighlightSize.x;
        float h = mHighlightSize.y;
        int aix= index==0?x1:index==1?x2:x3;
        int aiy= y;
        int cx = aix - rawX;
        int cy = aiy - rawY;

        if (actionBitmap == null)
        {
            return false;
        }

        if (sqrt((cx) * (cx) + (cy) * (cy)) < sqrt((w/2) * (w/2) + (h/2) * (h/2)))
        {
            if (!selected)
            {
                // reset icons
                mAction1IconView.setBackgroundDrawable(new BitmapDrawable(getResources(), BitmapUtils.colorBitmap(mAction1Bitmap, Color.WHITE)));
                mAction2IconView.setBackgroundDrawable(new BitmapDrawable(getResources(), BitmapUtils.colorBitmap(mAction2Bitmap, Color.WHITE)));
                mAction3IconView.setBackgroundDrawable(new BitmapDrawable(getResources(), BitmapUtils.colorBitmap(mAction3Bitmap, Color.WHITE)));
                mAction1selected = false;
                mAction2selected = false;
                mAction3selected = false;

                // highlight the current action icon
                actionIconView.setBackgroundDrawable(new BitmapDrawable(getResources(), BitmapUtils.colorBitmap(actionBitmap, Color.BLACK)));
                    RelativeLayout.LayoutParams params = (LayoutParams) mHighlightView.getLayoutParams();
                    params.leftMargin = aix - mHighlightSize.x / 2;
                    mHighlightView.setVisibility(VISIBLE);
                    mHighlightView.requestLayout();
                    mDotsView.setVisibility(INVISIBLE);

                if (Settings.System.getInt(mContext.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1)
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                return true;
            }
        }
        else
        {
            if (selected)
            {
                actionIconView.setBackgroundDrawable(new BitmapDrawable(getResources(), BitmapUtils.colorBitmap(actionBitmap, Color.WHITE)));
                mHighlightView.setVisibility(INVISIBLE);
                mDotsView.setVisibility(VISIBLE);
                return false;
            }
        }
        return selected;
    }

    int getSelected()
    {
        if (mAction1selected) return 0;
        else if (mAction2selected) return 1;
        else if (mAction3selected) return 2;
        else return -1;
    }

    public void updateSizeAndPosition(Point pos, Point size)
    {
        mPos = pos;
        mSize = size;
    }
}
