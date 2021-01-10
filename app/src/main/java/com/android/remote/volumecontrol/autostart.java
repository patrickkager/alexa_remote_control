package com.android.remote.volumecontrol;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class autostart extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {

        if (intent.getAction() == null) {
            return;
        }

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
                Intent i = new Intent(context, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
        }
    }
}