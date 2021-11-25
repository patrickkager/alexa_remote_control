package com.android.remote.volumecontrol;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class SettingsContentObserver extends ContentObserver {

    private static final String TAG = "SettingsContentObserver";
    private  int previousVolume;
    private AudioManager audioManager;
    private Context context;
    private boolean lastMuteState = false;
    private boolean isCheckRunning = false;

    public SettingsContentObserver(Handler handler) {
        super(handler);

        this.context = MyApp.getContext();

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        /*
        AudioDeviceInfo[] outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device: outputDevices) {
            Log.d(TAG, "AudioDeviceInfo type "+deviceTypeToString(device.getType()));
            if(device.getType() == AudioDeviceInfo.TYPE_LINE_DIGITAL){
                audioManager.setSpeakerphoneOn(false);
                if (Build.VERSION.SDK_INT >= 31)
                    audioManager.setCommunicationDevice(device);
                break;
            }
        }
        */

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
                        AlexaCommands.ChangeVolume(0,"mute");
                    }else{
                        AlexaCommands.ChangeVolume(previousVolume,"unmute");
                    }
                }
            }
        },500,1000);
    }

    @Override
    public boolean deliverSelfNotifications() { return false; }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        Log.d("VolumeListener","Received Volume onChange event");

        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (isCheckRunning || currentVolume == previousVolume)
            return;

        if(MyApp.debugOutPut()){
            Log.d("VolumeListener","Received Volume Current:"+currentVolume+ " Previous:"+previousVolume);
        }

        isCheckRunning = true;
        int waitTime = MyApp.getSettings().getInt("keyPressWait",250);
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

        if(MyApp.debugOutPut()){
            Log.d("VolumeListener","Set Volume Type: "+type+" Current:"+currentVolume+ " Previous:"+previousVolume);
        }

        previousVolume = currentVolume;
        AlexaCommands.ChangeVolume(currentVolume,type);
        isCheckRunning = false;
    }

    private boolean isMuted() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audio.isStreamMute(0);
    }

    private String deviceTypeToString(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_UNKNOWN:
                return "TYPE_UNKNOWN";
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "TYPE_BUILTIN_EARPIECE";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "TYPE_BUILTIN_SPEAKER";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "TYPE_WIRED_HEADSET";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "TYPE_WIRED_HEADPHONES";
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
                return "TYPE_LINE_ANALOG";
            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                return "TYPE_LINE_DIGITAL";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "TYPE_BLUETOOTH_SCO";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "TYPE_BLUETOOTH_A2DP";
            case AudioDeviceInfo.TYPE_HDMI:
                return "TYPE_HDMI";
            case AudioDeviceInfo.TYPE_HDMI_ARC:
                return "TYPE_HDMI_ARC";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "TYPE_USB_DEVICE";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return "TYPE_USB_ACCESSORY";
            case AudioDeviceInfo.TYPE_DOCK:
                return "TYPE_DOCK";
            case AudioDeviceInfo.TYPE_FM:
                return "TYPE_FM";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                return "TYPE_BUILTIN_MIC";
            case AudioDeviceInfo.TYPE_FM_TUNER:
                return "TYPE_FM_TUNER";
            case AudioDeviceInfo.TYPE_TV_TUNER:
                return "TYPE_TV_TUNER";
            case AudioDeviceInfo.TYPE_TELEPHONY:
                return "TYPE_TELEPHONY";
            case AudioDeviceInfo.TYPE_AUX_LINE:
                return "TYPE_AUX_LINE";
            case AudioDeviceInfo.TYPE_IP:
                return "TYPE_IP";
            case AudioDeviceInfo.TYPE_BUS:
                return "TYPE_BUS";
            case AudioDeviceInfo.TYPE_USB_HEADSET:
                return "TYPE_USB_HEADSET";
            default:
                return "TYPE_UNKNOWN";
        }
    }

}