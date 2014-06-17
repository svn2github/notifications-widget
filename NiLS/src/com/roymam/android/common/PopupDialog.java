package com.roymam.android.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.roymam.android.notificationswidget.R;

public class PopupDialog {
    private final String TAG = PopupDialog.class.getName();
    private TextView mTitleView;
    private Context mContext;
    private boolean mVisible = false;
    private LinearLayout mWindowView;
    private View mView;
    private TextView mTextView;
    private Button mPositiveButton;
    private Button mNegativeButton;
    private ImageButton mCloseButton;
    private WindowManager.LayoutParams mLayoutParams;

    private PopupDialog() {
        // default constructor - prevent creating this class without the "create" method
    }

    private PopupDialog(Context context) {
        mContext = context;

        mWindowView = new LinearLayout(context);
        mLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        mLayoutParams.gravity = Gravity.BOTTOM;

        // create the popup dialog view
        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mView = li.inflate(R.layout.popup_dialog, null);
        mTitleView = (TextView) mView.findViewById(R.id.popup_title);
        mTextView = (TextView) mView.findViewById(R.id.popup_text);
        mCloseButton = (ImageButton) mView.findViewById(R.id.popup_x_button);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });

        mPositiveButton = (Button) mView.findViewById(R.id.popup_positive_button);
        mNegativeButton = (Button) mView.findViewById(R.id.popup_negative_button);

        // create listeners
        mWindowView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE)
                    hide();
                return false;
            }
        });

        mWindowView.addView(mView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    public static PopupDialog create(Context context) {
        return new PopupDialog(context);
    }

    public PopupDialog setTitle(CharSequence text) {
        mTitleView.setText(text);
        return this;
    }

    public PopupDialog setText(CharSequence text) {
        mTextView.setText(text);
        return this;
    }

    public PopupDialog setPositiveButton(CharSequence text, View.OnClickListener onclick) {
        mPositiveButton.setText(text);
        mPositiveButton.setOnClickListener(onclick);
        return this;
    }

    public PopupDialog setNegativeButton(CharSequence text, View.OnClickListener onclick) {
        mNegativeButton.setText(text);
        mNegativeButton.setOnClickListener(onclick);
        return this;
    }

    public PopupDialog show() {
        if (mVisible) {
            Log.d(TAG, "PopupDialog already visible");
            return this;
        }

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(mWindowView, mLayoutParams);
        mView.setTranslationY(mView.getHeight());
        mView.animate()
                .translationY(0)
                .setDuration(Resources.getSystem().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(null);
        mVisible = true;
        return this;
    }

    public PopupDialog hide() {
        if (!mVisible) {
            Log.d(TAG, "PopupDialog is not visible");
            return this;
        }

        mView.animate()
                .translationY(mView.getHeight())
                .setDuration(Resources.getSystem().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                        wm.removeView(mWindowView);
                        mVisible = false;
                    }
                });

        return this;
    }

    public boolean isVisible()
    {
        return mVisible;
    }
}
