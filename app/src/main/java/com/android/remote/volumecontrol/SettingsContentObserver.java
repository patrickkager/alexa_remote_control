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
    private boolean isCheckRunning = false;

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
                        ((MainActivity)context).ChangeVolume(0,"mute");
                    }else{
                        ((MainActivity)context).ChangeVolume(previousVolume,"unmute");
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
        if (isCheckRunning || currentVolume == previousVolume)
            return;

        if(((MainActivity)context).debugOutPut){
            Log.d("VolumeListener","Received Volume Current:"+currentVolume+ " Previous:"+previousVolume);
        }

        isCheckRunning = true;
        int waitTime = ((MainActivity)this.context).keyPressWait;
        while (true) {
            try {
                Thread.sleep(waitTime);

                int tmpVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (tmpVolume == currentVolume)
                    break;

                currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }

        String type = "";
        int delta = previousVolume - currentVolume;
        if (delta > 0) {
            type = "dec";
        } else if (delta < 0) {
            type = "inc";
        }

        if(((MainActivity)context).debugOutPut){
            Log.d("VolumeListener","Set Volume Type: "+type+" Current:"+currentVolume+ " Previous:"+previousVolume);
        }

        previousVolume = currentVolume;
        ((MainActivity) context).ChangeVolume(currentVolume,type);
        isCheckRunning = false;
    }

    private boolean isMuted() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audio.isStreamMute(0);
    }
}