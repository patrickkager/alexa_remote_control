package com.android.remote.volumecontrol;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class SettingsContentObserver extends ContentObserver {

    private  int previousVolume;
    private Context context;
    private AudioManager audioManager;
    private boolean lastMuteState = false;

    public SettingsContentObserver(Context c, Handler handler) {
        super(handler);

        this.context = c;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                boolean b = isMuted();
                if(b != lastMuteState){
                    lastMuteState = b;
                    if(b && previousVolume <= 0) {
                        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    }

                    if(b){
                        ((MainActivity)context).ChangeVolume(String.valueOf(0));
                    }else{
                        ((MainActivity)context).ChangeVolume(String.valueOf(previousVolume));
                    }
                }
            }
        },500,1000);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return false;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int delta = previousVolume - currentVolume;

        if (delta > 0) {
            Log.d("AlexaVolumeControl", "Decreased");
        } else if (delta < 0) {
            Log.d("AlexaVolumeControl", "Increased");
            delta = delta * (-1);
        }

        previousVolume = currentVolume;
        //Send Command if delta volume difference is greater then
        if(delta >= ((MainActivity)this.context).changeVolumeDiff){
            ((MainActivity)this.context).ChangeVolume(String.valueOf(currentVolume));
        }
    }

    private boolean isMuted() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audio.isStreamMute(0);
    }
}