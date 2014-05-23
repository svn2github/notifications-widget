package com.roymam.android.nilsplus.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.TextView;

import com.roymam.android.notificationswidget.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class LogViewerActivity extends Activity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                readLog();
            }
        }, 500);
   }

    protected void readLog()
    {
        super.onStart();
        TextView logText = (TextView) findViewById(R.id.log_textview);
        StringBuilder sb = new StringBuilder();

        Process process = null;
        BufferedReader reader = null;

        try {
            process = Runtime.getRuntime().exec("logcat -d -v time");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String nextLine = reader.readLine();

            while (nextLine != null)
            {
                sb.append(nextLine);
                nextLine = reader.readLine();
                if (nextLine != null) sb.append("\n");
            }
            reader.close();
        }
        catch (IOException e)
        {
            sb.append("Cannot read recent log messages");
        }
        logText.setText(sb.toString());

    }
}