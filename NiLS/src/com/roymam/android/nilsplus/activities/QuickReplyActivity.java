package com.roymam.android.nilsplus.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.roymam.android.notificationswidget.NotificationData;
import com.roymam.android.notificationswidget.NotificationsService;
import com.roymam.android.notificationswidget.R;

public class QuickReplyActivity extends Activity {

    private static final String TAG = QuickReplyActivity.class.getSimpleName();

    private int mAnimationDuration;
    private View mMainView;
    private TextView mTitleText;
    private NotificationData nd;
    private int actionPos;
    private ImageButton mSendButton;
    private EditText mReplyText;
    private boolean focused = false;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setWindowAnimations(0);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setContentView(R.layout.activity_quick_reply);
        mContext = getApplicationContext();
    }

    @Override
    protected void onStart() {
        super.onStart();
        int uid = getIntent().getIntExtra("uid", -1);
        actionPos = getIntent().getIntExtra("actionPos", -1);
        Log.d(TAG, "onStart with uid:" + uid + " and actionPos:" + actionPos);

        if (nd == null) {
            if (uid != -1 && actionPos != -1) {
                nd = NotificationsService.getSharedInstance().findNotificationByUid(uid);
                if (nd == null) {
                    Log.w(TAG, "notification is no longer available (already dismissed?)");
                    finish();
                    return;
                }
            } else {
                Log.w(TAG, "missing notification uid/action pos");
                finish();
                return;
            }
        }

        mAnimationDuration = Resources.getSystem().getInteger(android.R.integer.config_shortAnimTime);
        mMainView = findViewById(R.id.quick_reply_window);
        mTitleText = (TextView) mMainView.findViewById(R.id.quick_text_label);
        mReplyText = (EditText) mMainView.findViewById(R.id.quick_reply_text);
        mSendButton = (ImageButton) mMainView.findViewById(R.id.quick_reply_button);
        final NotificationData.Action action = nd.getActions()[actionPos];
        mTitleText.setText(action.title);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();
                Intent intent = new Intent();
                Bundle params = new Bundle();
                params.putCharSequence(action.remoteInputs[0].getResultKey(), mReplyText.getText());
                RemoteInput.addResultsToIntent(action.remoteInputs, intent, params);
                try {
                    action.actionIntent.send(getApplicationContext(), 0, intent);
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
        });
        NotificationsService.getSharedInstance().hide(true);
        show();
    }

    private void show()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mMainView.setAlpha(0);
        mMainView.animate().alpha(1.0f).setDuration(mAnimationDuration).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                View textview = findViewById(R.id.quick_reply_text);
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(textview, InputMethodManager.SHOW_IMPLICIT);
                textview.requestFocus();
            }
        });
    }

    private void hide()
    {
        mMainView.animate().alpha(0).setDuration(mAnimationDuration).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (focused) {
            hide();
            NotificationsService.getSharedInstance().show(false);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d(TAG," onFocus:"+hasFocus);
        focused = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onPause");
        //hide();
        //NotificationsService.getSharedInstance().show(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.quick_reply, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
