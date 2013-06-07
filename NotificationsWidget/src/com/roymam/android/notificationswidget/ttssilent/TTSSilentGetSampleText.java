package com.roymam.android.notificationswidget.ttssilent;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/*
 * Returns the sample text string for the language requested
 */
public class TTSSilentGetSampleText extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Intent returnData = new Intent();
        returnData.putExtra("sampleText", "hello");
        setResult(TextToSpeech.LANG_AVAILABLE, returnData);

        finish();
    }
}