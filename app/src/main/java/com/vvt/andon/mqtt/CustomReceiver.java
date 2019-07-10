package com.vvt.andon.mqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class CustomReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //Toast.makeText(context, "Service restarting", Toast.LENGTH_SHORT).show();
       // Log.d(getClass().getCanonicalName(), "onReceive");
        //context.startService(new Intent(context, MQTTService1.class));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent serviceIntent = new Intent(context, MQTTService1.class);
            ContextCompat.startForegroundService(context, serviceIntent );

        }
        else {
            context.startService(new Intent(context,MQTTService1.class));

        }

    }
}
