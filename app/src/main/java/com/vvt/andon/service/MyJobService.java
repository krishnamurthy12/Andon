/*
 * Created by Krishnamurthy T
 * Copyright (c) 2019 .  V V Technologies All rights reserved.
 * Last modified 27/2/19 11:55 AM
 */

package com.vvt.andon.service;

import android.app.ActivityManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.vvt.andon.mqtt.MQTTService1;

public class MyJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {

      /* String receivedString= params.getExtras().getString("KEY");*/
      //  Log.d("MyJobService","MyJobService started");

       /* MyAsyntask myAsyntask=new MyAsyntask(params);
        myAsyntask.execute();*/

        MQTTService1 mqttService1=new MQTTService1();
        if (!isMyServiceRunning(mqttService1.getClass())) {
            //Log.d(TAG,"inside service not running method");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent serviceIntent = new Intent(MyJobService.this, MQTTService1.class);
                ContextCompat.startForegroundService(MyJobService.this, serviceIntent );

            }
            else {
                startService(new Intent(MyJobService.this,MQTTService1.class));

            }

        }

        //startService(new Intent(MyJobService.this, MQTTService1.class));
        jobFinished(params,false);

        return true;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    //Log.i ("isMyServiceRunning?", true+"");
                    return true;
                }
            }
        }
        //Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

   /* private class MyAsyntask extends AsyncTask<Void,Void,String>
    {
        JobParameters params;

        MyAsyntask(JobParameters params) {
            this.params = params;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... voids) {
            startService(new Intent(MyJobService.this, MQTTService1.class));

            return "Job started";
        }

        @Override
        protected void onPostExecute(String string) {
            super.onPostExecute(string);

            //Toast.makeText(MyJobService.this, string, Toast.LENGTH_SHORT).show();
            jobFinished(params,false);
        }
    }*/



    @Override
    public boolean onStopJob(JobParameters params) {
       // Log.d("MyJobService","MyJobService Stoped");
        //Toast.makeText(getApplicationContext(), "MyJobService stopped", Toast.LENGTH_SHORT).show();
        return false;
    }
}
