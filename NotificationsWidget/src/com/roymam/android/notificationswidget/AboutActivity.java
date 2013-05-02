package com.roymam.android.notificationswidget;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.LayoutInflater;
import android.view.Menu;

public class AboutActivity extends Activity 
{

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_about);
		
		String versionString = "";
    	String title = "";
    	try 
    	{
    		title = getString(R.string.about_title);
			versionString = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) 
		{
		}
        
        setTitle(title + " v" + versionString);
	}
}
