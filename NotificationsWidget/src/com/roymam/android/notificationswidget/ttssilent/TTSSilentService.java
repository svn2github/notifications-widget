package com.roymam.android.notificationswidget.ttssilent;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeechService;

// Dummy TTS Service - Used to silent TTS on Samsung devices
public class TTSSilentService extends TextToSpeechService
{
    @Override
    protected int onIsLanguageAvailable(String s, String s2, String s3)
    {
        return 1;
    }

    @Override
    protected String[] onGetLanguage()
    {
        return new String[] {"en-US"};
    }

    @Override
    protected int onLoadLanguage(String s, String s2, String s3)
    {
        return 1;
    }

    @Override
    protected void onStop()
    {

    }

    @Override
    protected void onSynthesizeText(SynthesisRequest synthesisRequest, SynthesisCallback synthesisCallback)
    {

    }

    public IBinder onBind(Intent intent)
    {
        return null;
    }


}
