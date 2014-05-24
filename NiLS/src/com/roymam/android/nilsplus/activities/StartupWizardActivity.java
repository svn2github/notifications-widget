package com.roymam.android.nilsplus.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.roymam.android.common.LimitedViewPager;
import com.roymam.android.nilsplus.ui.NiLSActivity;
import com.roymam.android.notificationswidget.*;
import com.viewpagerindicator.LinePageIndicator;

import java.util.ArrayList;
import java.util.List;

public class StartupWizardActivity extends Activity implements ViewPager.OnPageChangeListener {
    public static final String EXTRA_FIRST_TIME = "extra_not_first_time";
    private static int WELCOME_PAGE_INDEX = 0;
    private static int START_SERVICE_PAGE_INDEX = 1;
    private static int FINISH_PAGE_INDEX = 5;
    private LimitedViewPager mViewPager;
    private List<Fragment> fragments;
    private Button mNext;
    private Button mSkip;
    private Button mOpenAndroidSettings;

    public void limitPaging(boolean limit)
    {
        if (limit)
            mViewPager.setLimit(1);
        else
            mViewPager.setLimit(-1);
    }

    public void next()
    {
        if (mViewPager.getCurrentItem() < mViewPager.getAdapter().getCount()-1)
            mViewPager.setCurrentItem(mViewPager.getCurrentItem()+1, true);
        else
            finishTutorial();
    }

    public void finishTutorial()
    {
        finish();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.edit().putBoolean(SettingsManager.SHOW_WELCOME_WIZARD, false).commit();
        startActivity(new Intent(getApplicationContext(), NiLSActivity.class));
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        boolean firstTime = getIntent().getBooleanExtra(EXTRA_FIRST_TIME, false);

        fragments = new ArrayList<Fragment>();
        if (firstTime)
        {
            fragments.add(new WelcomeFragment());
            fragments.add(new StartServiceFragment());
            fragments.add(new NiLSTutorial1());
            fragments.add(new NiLSTutorial2());
            fragments.add(new NiLSTutorial3());
            fragments.add(new NiLSTutorial4());
            WELCOME_PAGE_INDEX = 0;
            START_SERVICE_PAGE_INDEX = 1;
            FINISH_PAGE_INDEX = 5;
        }
        else
        {
            // set the activity to display only the "start service" page
            fragments.add(new StartServiceFragment());
            START_SERVICE_PAGE_INDEX = 0;
            FINISH_PAGE_INDEX = 0;
            WELCOME_PAGE_INDEX = -1;
        }

        setContentView(R.layout.tutorial_layout);
        mViewPager = (LimitedViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(new WizardPageAdapter(getFragmentManager()));

        if (!NotificationsService.isServiceRunning(getApplicationContext()))
            mViewPager.setLimit(1);

        LinePageIndicator indicator = (LinePageIndicator) findViewById(R.id.line_page_indicator);
        indicator.setViewPager(mViewPager);
        indicator.setOnPageChangeListener(this);

        // set buttons actions
        mNext = (Button) findViewById(R.id.next_button);
        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });
        mSkip = (Button) findViewById(R.id.skip_button);
        mSkip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finishTutorial();
                }
            });

        mOpenAndroidSettings =(Button) findViewById(R.id.open_android_settings_button);
        mOpenAndroidSettings.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = SettingsManager.getNotificationsServiesIntent();
                startActivity(intent);
                Toast.makeText(getApplicationContext(), R.string.enable_service_tip, Toast.LENGTH_LONG).show();
            }
        });
    }


    @Override
    public void onPageScrolled(int i, float v, int i2)
    {
    }

    @Override
    public void onPageSelected(int i)
    {
        // initial buttons state
        mOpenAndroidSettings.setVisibility(View.GONE);
        mNext.setVisibility(View.VISIBLE);
        mNext.setEnabled(true);
        mSkip.setVisibility(View.VISIBLE);
        mNext.setText(R.string.tutorial_next);

        if (i == WELCOME_PAGE_INDEX) {
            mSkip.setVisibility(View.GONE);
            mNext.setText(R.string.tutorial_1_lets_get_started);
        }
        if (i == START_SERVICE_PAGE_INDEX) {
            mSkip.setVisibility(View.GONE);
            mOpenAndroidSettings.setVisibility(View.VISIBLE);

            // NiLS Service Status
            boolean serviceRunning = NotificationsService.isServiceRunning(getApplicationContext());

            if (serviceRunning) {
                if (mNext != null) mNext.setEnabled(true);
                limitPaging(false);
            } else {
                if (mNext != null) mNext.setEnabled(false);
                limitPaging(true);
            }
        }
        if (i == FINISH_PAGE_INDEX) {
            mSkip.setVisibility(View.GONE);
            mNext.setText(R.string.tutorial_6_finish);
        }
    }

    @Override
    public void onPageScrollStateChanged(int i)
    {
    }

    @Override
    public void onResume()
    {
        super.onResume();
        onPageSelected(mViewPager.getCurrentItem());
    }

    private class WizardPageAdapter extends FragmentPagerAdapter
    {
        public WizardPageAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int i)
        {
            return fragments.get(i);
        }

        @Override
        public int getCount()
        {
            return fragments.size();
        }
    }

    public static class GenericNextSkipTutorial extends Fragment
    {
        private final int mLayout;

        public GenericNextSkipTutorial(int layout)
        {
            mLayout = layout;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            View v =  inflater.inflate(mLayout, null);
            return v;
        }
    }

    public static class WelcomeFragment extends GenericNextSkipTutorial
    {
        public WelcomeFragment()
        {
            super(R.layout.welcome_tutorial_1);
        }
    }

    public static class StartServiceFragment extends GenericNextSkipTutorial
    {
        public StartServiceFragment()
        {
            super(R.layout.welcome_tutorial_2);
        }
    }

    public static class NiLSTutorial1 extends GenericNextSkipTutorial
    {
        public NiLSTutorial1()
        {
            super(R.layout.welcome_tutorial_3);
        }
    }

    public static class NiLSTutorial2 extends GenericNextSkipTutorial
    {
        public NiLSTutorial2()
        {
            super(R.layout.welcome_tutorial_4);
        }
    }

    public static class NiLSTutorial3 extends GenericNextSkipTutorial
    {
        public NiLSTutorial3()
        {
            super(R.layout.welcome_tutorial_5);
        }
    }

    public static class NiLSTutorial4 extends GenericNextSkipTutorial
    {
        public NiLSTutorial4()
        {
            super(R.layout.welcome_tutorial_6);
        }
    }

}