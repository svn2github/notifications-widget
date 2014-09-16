package com.roymam.android.nilsplus.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.roymam.android.notificationswidget.NotificationData;
import com.roymam.android.nilsplus.ui.theme.Theme;
import com.roymam.android.nilsplus.ui.theme.ThemeManager;
import com.roymam.android.notificationswidget.R;

import java.util.ArrayList;
import java.util.List;

public class PopupNotification {
    private final String TAG = PopupNotification.class.getName();
    private int mPopupTimeout;
    private Handler mHandler;
    private Theme mTheme;
    private Context mContext;
    private boolean mVisible = false;
    private LinearLayout mWindowView;
    private View mView;
    private WindowManager.LayoutParams mLayoutParams;
    private static List<PopupNotification> queue = new ArrayList<PopupNotification>();
    private final long mAnimationTime = Resources.getSystem().getInteger(android.R.integer.config_shortAnimTime);

    private PopupNotification() {
        // default constructor - prevent creating this class without the "create" method
    }

    private PopupNotification(Context context) {
        Log.d(TAG, "PopupNotification");
        mContext = context;
        mHandler = new Handler();

        mWindowView = new LinearLayout(context);
        mLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        mLayoutParams.gravity = Gravity.TOP;

        // create the popup dialog view
        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mTheme = ThemeManager.getInstance(context).getCurrentTheme();
        if (mTheme != null && mTheme.notificationLayout != null) {
            ThemeManager.getInstance(context).reloadLayouts(mTheme);
            mView = li.inflate(mTheme.notificationLayout, null);
        }
        else {
            mView = li.inflate(R.layout.notification_row, null);
        }

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

        mPopupTimeout = 5000;
    }

    public static PopupNotification create(final Context context, final NotificationData nd)
    {
        final PopupNotification pn = new PopupNotification(context);
        NotificationAdapter.applySettingsToView(context, pn.mView, nd, 0, pn.mTheme, true);
        pn.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    pn.hide();
                    if (nd != null && nd.getAction() != null) nd.getAction().send();
                    else if (nd != null) {
                        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(nd.getPackageName());
                        context.startActivity(launchIntent);
                    }
                } catch (PendingIntent.CanceledException e) {
                    // opening notification failed, try to open the app
                    try
                    {
                        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(nd.getPackageName());
                        context.startActivity(launchIntent);
                    }
                    catch(Exception e2)
                    {
                        // cannot launch intent - do nothing...
                        e2.printStackTrace();
                        Toast.makeText(context, "Error - cannot launch app:" + nd.getPackageName(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        return pn;
    }

    public PopupNotification show() {
        if (mVisible) {
            Log.d(TAG, "PopupNotification already visible");
            return this;
        }

        queue.add(0, this);

        if (queue.size() == 1)
            popupNotification();

        return this;
    }

    private void popupNotification() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(mWindowView, mLayoutParams);
        mView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mView.getViewTreeObserver().removeOnPreDrawListener(this);
                mView.setTranslationY(-mView.getHeight());
                mView.animate()
                        .translationY(0)
                        .setDuration(mAnimationTime)
                        .setListener(null);
                mVisible = true;

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hide();
                    }
                }, mPopupTimeout);

                return true;
            }
        });
    }

    public PopupNotification hide() {
        if (!mVisible) {
            Log.d(TAG, "PopupNotification is not visible");
            return this;
        }

        mView.animate()
                .translationY(-mView.getHeight())
                .setDuration(mAnimationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                        try {
                            wm.removeView(mWindowView);
                            queue.remove(queue.size()-1);
                            if (queue.size()>0)
                                queue.get(queue.size()-1).popupNotification();
                        }
                        catch(Exception exp)
                        {
                            // something went wrong - but we don't want to interrupt the user
                            exp.printStackTrace();
                        };
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
