package com.roymam.android.notificationswidget;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AppearanceActivity extends FragmentActivity 
{
	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_appearance);

		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// Create the adapter that will return a fragment for each of the two
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_appearance, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter 
	{

		public SectionsPagerAdapter(FragmentManager fm) 
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int position) 
		{
			switch (position)
			{
			case 0:
				return new ClockSectionFragment();
			case 1:
				return new NotificationSectionFragment();
			}
			return null;
		}

		@Override
		public int getCount() 
		{
			// Show 2 total pages.
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) 
		{
			switch (position) 
			{
			case 0:
				return getString(R.string.title_section1).toUpperCase();
			case 1:
				return getString(R.string.title_section2).toUpperCase();
			}
			return null;
		}
	}

	public static class ClockSectionFragment extends Fragment 
	{
		public ClockSectionFragment() 
		{
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) 
		{
			return inflater.inflate(R.layout.tab_clock_settings, null);
		}
	}
	
	public static class NotificationSectionFragment extends Fragment 
	{
		public NotificationSectionFragment() 
		{
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) 
		{
			return inflater.inflate(R.layout.tab_notification_settings, null);
		}
	}

}
