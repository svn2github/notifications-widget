package com.roymam.android.notificationswidget;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

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
            //noinspection ConstantConditions
            versionString = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException ignored)
		{
		}
        
        setTitle(title + " v" + versionString);
	}
}
