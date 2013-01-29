package com.roymam.android.notificationswidget;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
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

			final Button button1 = (Button) findViewById(R.id.button1);
			final Button button2 = (Button) findViewById(R.id.button2);
			final Button button3 = (Button) findViewById(R.id.button3);
			
			switch (currentStep)
			{
				case 1:
					button1.setEnabled(true);
					button1.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
					button2.setEnabled(false);
					button2.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
					button3.setEnabled(false);
					button3.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
					break;
				case 2:
					button1.setEnabled(false);
					button1.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
					button2.setEnabled(true);
					button2.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
					button3.setEnabled(false);
					button3.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
					break;
				case 3:
					button1.setEnabled(false);
					button1.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
					button2.setEnabled(false);
					button2.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
					button3.setEnabled(true);
					button3.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));					
					break;
			}
		}
		
		super.onWindowFocusChanged(hasFocus);
	}
	
	

}
