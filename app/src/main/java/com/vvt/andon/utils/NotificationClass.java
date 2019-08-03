/*
 * Created by Krishnamurthy T
 * Copyright (c) 2019 .  V V Technologies All rights reserved.
 * Last modified 17/7/19 3:26 PM
 */

package com.vvt.andon.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.vvt.andon.R;
import com.vvt.andon.activities.HomeActivity;

public class NotificationClass {

    /*Trigger moe_notification with Message*/
    public static void showNotificationToUser(Context context,String msg,String department) {
        String CHANNEL_ID = "andon_channel_for_initial_alert";
        CharSequence name = "Andon_initial_alert_channel";
        String Description = "Andon moe_notification channel1";

        int NOTIFICATION_ID = 123;

        Intent notificationIntent = new Intent(context, HomeActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);
        //NotificationManagerCompat notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //To remove all the previous moe_notification icons from moe_notification bar
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        // Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)

                //Notification moe_notification = new Notification.Builder(this)
                .setContentTitle("Andon")
                .setContentText(msg)

                .setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE )
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setStyle(new NotificationCompat.InboxStyle());

        if (department.equalsIgnoreCase("TEFF")) {
            /*For EC TEF team*/
            builder.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.ec_alert));
        } else {
            /* Rest of TEF*/
            builder.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.regular_alert));
        }


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Creating an Audio Attribute
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

            builder.setDefaults(Notification.DEFAULT_VIBRATE);

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);

          if(department.equalsIgnoreCase("TEFF"))
          {
               /*
          Ringtone Only to EC TEF team
          */
              mChannel.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.ec_alert),audioAttributes);
          }
          else {
               /*
            Ringtone For rest others
            * */
              mChannel.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.regular_alert),audioAttributes);
          }

            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            //mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(true);

            if (notificationManager != null) {

                notificationManager.createNotificationChannel(mChannel);
            }

        }


        int id = (int)System.currentTimeMillis();
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }

    }

    /*Trigger moe_notification to MOE with Message*/
    public static void showNotificationToMOE(Context context,String msg) {
        String CHANNEL_ID = "andon_channel_for_moecomment";
        CharSequence name = "Andon_MOE_channel";
        String Description = "Andon moe_notification channel2";

        int NOTIFICATION_ID = 123;

        Intent notificationIntent = new Intent(context, HomeActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);
        //NotificationManagerCompat notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //To remove all the previous moe_notification icons from moe_notification bar
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            // mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(true);

            if (notificationManager != null) {

                notificationManager.createNotificationChannel(mChannel);
            }

        }

        // Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "Andon")

                //Notification moe_notification = new Notification.Builder(this)
                .setContentTitle("Andon")
                .setContentText(msg)
                .setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.moe_notification))
                .setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE )
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setStyle(new NotificationCompat.InboxStyle());

        int id = (int)System.currentTimeMillis();
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }

    }

    /*Trigger moe_notification without Message Just used to indicate refreshing */
    public static void showNotificationToUser(Context context) {
        String CHANNEL_ID = "andon_channel_for_refreshment";
        CharSequence name = "Andon_refreshment_channel";
        String Description = "Andon moe_notification channel3";

        int NOTIFICATION_ID = 123;

        Intent notificationIntent = new Intent(context, HomeActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);
        /* NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);*/
        //NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //To remove all the previous moe_notification icons from moe_notification bar
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(true);

            if (notificationManager != null) {

                notificationManager.createNotificationChannel(mChannel);
            }

        }

        // Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        /*Notification moe_notification = new Notification.Builder(this)*/
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentText("")
                .setContentTitle("Andon")
                // .setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.regular_alert))
                .setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(""))
                .setStyle(new NotificationCompat.InboxStyle());

        int id = (int)System.currentTimeMillis();

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

}
