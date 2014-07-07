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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.roymam.android.common.LimitedViewPager;
import com.roymam.android.nilsplus.ui.NiLSActivity;
import com.roymam.android.notificationswidget.R;
import com.viewpagerindicator.LinePageIndicator;

import java.util.ArrayList;
import java.util.List;

public class WhatsNewActivity extends Activity implements ViewPager.OnPageChangeListener
{
    private LimitedViewPager mViewPager;
    private List<Fragment> fragments;
    private Button mNext;
    private Button mSkip;
    private Button mOpenAndroidSettings;
    private int mCurrentVer;

    public void next()
    {
        if (mViewPager.getCurrentItem() < mViewPager.getAdapter().getCount()-1)
            mViewPager.setCurrentItem(mViewPager.getCurrentItem()+1, true);
        else
            finishWhatsNew();
    }

    public void finishWhatsNew()
    {
        finish();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // save the updated version code so this dialog won't appear again
        prefs.edit().putInt("installed_version", mCurrentVer).commit();

        startActivity(new Intent(getApplicationContext(), NiLSActivity.class));
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int installedVer = prefs.getInt("installed_version", -1);
        mCurrentVer = NiLSActivity.getCurrentVersion(this);

        fragments = new ArrayList<Fragment>();
        for(int i=mCurrentVer; i>installedVer; i--)
        {
            int titleId = getResources().getIdentifier("v"+i+"_title","string", getPackageName());
            if (titleId > 0)
            {
                Fragment fragment = new WhatsNewFragment();
                Bundle args = new Bundle();
                args.putInt("version", i);
                fragment.setArguments(args);
                fragments.add(fragment);
            }
        }

        if (fragments.size() == 0) finishWhatsNew();

        setContentView(R.layout.tutorial_layout);
        mViewPager = (LimitedViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(new WizardPageAdapter(getFragmentManager()));

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
                    finishWhatsNew();
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

    public static class WhatsNewFragment extends StartupWizardActivity.GenericNextSkipTutorial
    {
        public WhatsNewFragment()
        {
            setLayout(R.layout.whats_new_layout);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            View v = super.onCreateView(inflater, container, savedInstanceState);

            int version = getArguments().getInt("version");

            int titleId = getResources().getIdentifier("v"+version+"_title","string", getActivity().getPackageName());
            int textId = getResources().getIdentifier("v"+version,"string", getActivity().getPackageName());

            CharSequence whatsnewTitle = null;
            if (titleId > 0)
                whatsnewTitle = Html.fromHtml(getString(titleId));

            CharSequence whatsnewText = null;
            if (textId > 0)
                whatsnewText = Html.fromHtml(getString(textId));

            ((TextView)v.findViewById(R.id.whats_new_subtitle)).setText(whatsnewTitle);
            ((TextView)v.findViewById(R.id.whats_new_text)).setText(whatsnewText);

            return v;
        }
    }
}