package com.android.remote.volumecontrol;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

public class ContentObserverService extends Service {

    SettingsContentObserver mSettingsContentObserver;

    public ContentObserverService(){
        mSettingsContentObserver = new SettingsContentObserver(new Handler());
        MainActivity.getContext().getApplicationContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mSettingsContentObserver!=null)
            MainActivity.getContext().getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
        mSettingsContentObserver = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
