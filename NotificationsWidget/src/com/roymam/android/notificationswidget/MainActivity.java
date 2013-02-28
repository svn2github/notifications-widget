package com.roymam.android.notificationswidget;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
//import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity 
{
	public class AboutDialogFragment extends DialogFragment 
	{
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) 
	    {
	    	LayoutInflater inflater = getActivity().getLayoutInflater();
	    	String versionString = "";
	    	String appName = "";
	    	try {
	    		appName = getString(R.string.about_title);
				versionString = MainActivity.this.getPackageManager().getPackageInfo(MainActivity.this.getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
			}
	        
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setIcon(R.id.appIcon)
	        		.setTitle(appName + " v" + versionString)
	        		.setView(inflater.inflate(R.layout.about, null))
	        		.setPositiveButton(R.string.about_contactus_title, new DialogInterface.OnClickListener() 
	               {
	                   public void onClick(DialogInterface dialog, int id) 
	                   {
	                	   Intent emailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.about_contactus_email)));
	               			startActivity(emailIntent);
	                   }
	               })
	               .setNegativeButton(R.string.about_close, new DialogInterface.OnClickListener() 
	               {
	                   public void onClick(DialogInterface dialog, int id) 
	                   {
	                       // do nothing
	                   }
	               });	        
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		View button1 = findViewById(R.id.button1);
		View button2 = findViewById(R.id.button2);
		View button3 = findViewById(R.id.button3);
		button1.setClickable(true);
		button1.setOnClickListener(new OnClickListener() 
		{			
			@Override
			public void onClick(View v) 
			{
				Toast.makeText(MainActivity.this, getResources().getText(R.string.tutorial_toast_1), Toast.LENGTH_LONG).show();
				Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
			    startActivity(intent);
			}
		});
		button3.setClickable(true);
		button3.setOnClickListener(new OnClickListener() 
		{			
			@Override
			public void onClick(View v) 
			{
				Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
			    startActivity(intent);
			}
		});
		button2.setClickable(true);
		button2.setOnClickListener(new OnClickListener() 
		{			
			@Override
			public void onClick(View v) 
			{
				Toast.makeText(MainActivity.this, getResources().getText(R.string.tutorial_toast_2), Toast.LENGTH_LONG).show();
			}
		});
		
		//TODO
		// check if android version is 4.2 or higher
		// if not - check if WidgetLocker installed
		// if yes - change step 2 description
		// if no - show a dialog that recommend using WidgetLocker
	}

	public void openAd(View v)
    {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.about_ad_url)));
		startActivity(browserIntent);
    }
	
	public void showAbout()
	{
		DialogFragment dialog = new AboutDialogFragment();
		dialog.show(getSupportFragmentManager(), "AboutDialogFragment");
	}
	
	public void showSettings()
	{
		Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
	    startActivity(intent);
	}
    
	//@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		final MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_main, menu);
	    
	    OnMenuItemClickListener menuListener = new OnMenuItemClickListener()
	    {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) 
			{
				if (arg0.getItemId() == R.id.menu_settings)
				{
					showSettings();
					return true;
				}
				else if (arg0.getItemId() == R.id.menu_about)
				{
					showAbout();
					return true;
				}
				return false;
			}
	    };
	    for(int i=0;i<menu.size();i++)
	    	menu.getItem(i).setOnMenuItemClickListener(menuListener);
	    
	    return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) 
	{
		if (hasFocus)
		{
			int currentStep;
			
			// check if service is activated
			if (NotificationsService.getSharedInstance() == null)
				currentStep = 1;
			else
			{
				// check if a widget has been added				
				if (!NotificationsWidgetProvider.widgetActive)
					currentStep = 2;
				else
					currentStep = 3;
			}

			final View button1 = findViewById(R.id.button1);
			final View button2 = findViewById(R.id.button2);
			final View button3 = findViewById(R.id.button3);
			final TextView desc = (TextView)findViewById(R.id.TutorialDescription);
			switch (currentStep)
			{
				case 1:
					button1.setEnabled(true);
					button1.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
					button2.setEnabled(false);
					button2.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
					button3.setEnabled(false);
					button3.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
					desc.setText(R.string.tutorial_help_1);
					break;
				case 2:
					button1.setEnabled(false);
					button1.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
					button2.setEnabled(true);
					button2.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
					button3.setEnabled(false);
					button3.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
					desc.setText(R.string.tutorial_help_2);
					break;
				case 3:
					button1.setEnabled(false);
					button1.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
					button2.setEnabled(false);
					button2.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
					button3.setEnabled(true);
					button3.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
					desc.setText(R.string.tutorial_help_3);
					break;
			}
		}
		
		super.onWindowFocusChanged(hasFocus);
	}
	
	

}
