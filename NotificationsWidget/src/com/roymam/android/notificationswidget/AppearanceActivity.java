package com.roymam.android.notificationswidget;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
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
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_appearance);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		
		// Show the Up button in the action bar.
		actionBar.setDisplayHomeAsUpEnabled(true);

		actionBar.addTab(actionBar.newTab()
				.setText("Collapsed")
				.setTabListener(new ActionBar.TabListener() {			
					@Override
					public void onTabUnselected(Tab tab, FragmentTransaction ft) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onTabSelected(Tab tab, FragmentTransaction ft) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onTabReselected(Tab tab, FragmentTransaction ft) {
						// TODO Auto-generated method stub
						
					}
				}));
		actionBar.addTab(actionBar.newTab().setText("Expanded").setTabListener(new ActionBar.TabListener() {			
			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
				// TODO Auto-generated method stub
				
			}
		}));		
	}

}
