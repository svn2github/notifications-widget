package com.roymam.android.notificationswidget;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		final MenuInflater inflater = getMenuInflater();
	    final Intent[] menuIntents = new Intent[] {
	    		new Intent(this, SettingsActivity.class) };
	    
	    inflater.inflate(R.menu.activity_main, menu);
	    final int ms = menu.size();
	    for (int i=0; i < ms; i++) {
	        menu.getItem(i).setIntent(menuIntents[i]);
	    }
	    
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
