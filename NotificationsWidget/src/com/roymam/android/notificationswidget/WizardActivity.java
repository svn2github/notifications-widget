package com.roymam.android.notificationswidget;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
//import android.app.DialogFragment;

public class WizardActivity extends Activity 
{
	public static class AboutDialogFragment extends DialogFragment 
	{
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) 
	    {
	    	LayoutInflater inflater = getActivity().getLayoutInflater();
	    	String versionString = "";
	    	String appName = "";
	    	try {
	    		appName = getString(R.string.about_title);
				versionString = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
			}
	        
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setIcon(R.id.appIcon)
	        		.setTitle(appName + " v" + versionString)
	        		.setView(inflater.inflate(R.layout.dialog_about, null))
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

	private int step2desc;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
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
				Toast.makeText(WizardActivity.this, getResources().getText(R.string.tutorial_toast_1), Toast.LENGTH_LONG).show();
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
				Intent intent = new Intent(WizardActivity.this, SettingsActivity.class);
			    startActivity(intent);
			}
		});
		button2.setClickable(true);
		button2.setOnClickListener(new OnClickListener() 
		{			
			@Override
			public void onClick(View v) 
			{
				Toast.makeText(WizardActivity.this, getResources().getText(R.string.tutorial_toast_2), Toast.LENGTH_LONG).show();
			}
		});
		
		step2desc = R.string.tutorial_help_2;
		
		// check if android version is 4.2 or higher
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
		{
			// if not - check if WidgetLocker installed
			try 
			{
				getPackageManager().getPackageInfo("com.teslacoilsw.widgetlocker", 0);

				// if yes - change step 2 description
				step2desc = R.string.widget_locker_add_widget;
				
			} catch (NameNotFoundException e) 
			{				
				step2desc = R.string.no_widget_locker_add_widget;
				
				// if no - show a dialog that recommend using WidgetLocker
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.widget_locker_not_installed)
				.setMessage(R.string.widget_locker_not_installed_summary)
				.setPositiveButton(R.string.widget_locker_purchase, new DialogInterface.OnClickListener() 
	               {
	                   public void onClick(DialogInterface dialog, int id) 
	                   {
	                	   Intent widgetLockerIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.teslacoilsw.widgetlocker"));
	               		   startActivity(widgetLockerIntent);
	                   }
	               })
				.setNegativeButton(R.string.about_close, new DialogInterface.OnClickListener() 
	               {
	                   public void onClick(DialogInterface dialog, int id) 
	                   {
	                	   // do nothing
	                   }
	               })
				.show();
			}			
		}
	}

	public void openAd(View v)
    {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.about_ad_url)));
		startActivity(browserIntent);
    }
	
	public void showAbout()
	{	
		DialogFragment dialog = new AboutDialogFragment();
		dialog.show(getFragmentManager(), "AboutDialogFragment");
	}
	
	public void showSettings()
	{
		Intent intent = new Intent(WizardActivity.this, SettingsActivity.class);
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
					desc.setText(step2desc);
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