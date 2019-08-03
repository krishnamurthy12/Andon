/*
 * Created by Krishnamurthy T
 * Copyright (c) 2019 .  V V Technologies All rights reserved.
 * Last modified 17/7/19 4:34 PM
 */

package com.vvt.andon.mqtt;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.vvt.andon.R;
import com.vvt.andon.events.NotificationEvent;

import com.vvt.andon.utils.APIServiceHandler;
import com.vvt.andon.utils.AndonUtils;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.vvt.andon.utils.NotificationClass.showNotificationToMOE;
import static com.vvt.andon.utils.NotificationClass.showNotificationToUser;

public class MQTTService1 extends Service {

    private static final String TAG = "MQTTService";
    private static boolean hasWifi = false;
    private static boolean hasMmobile = false;
    private ConnectivityManager mConnMan;
    public static IMqttAsyncClient mqttClient;

    private Timer mTimer1;
    private Handler mTimerHandler = new Handler();

    public static String SUBSCRIPTION_TOPIC;
    String employeeID;
   // boolean isLoggedIn=false;

    MQTTBroadcastReceiver mqttBroadcastReceiver;

    AudioManager manager ;
    class MQTTBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            IMqttToken token;
            boolean hasConnectivity = false;
            boolean hasChanged = false;
            NetworkInfo infos[] = mConnMan.getAllNetworkInfo();

            for (NetworkInfo info : infos) {
                if (info.getTypeName().equalsIgnoreCase("MOBILE")) {
                    if ((info.isConnected() != hasMmobile)) {
                        hasChanged = true;
                        hasMmobile = info.isConnected();
                    }
                    //Log.d(TAG, info.getTypeName() + " is " + info.isConnected());
                } else if (info.getTypeName().equalsIgnoreCase("WIFI")) {
                    if ((info.isConnected() != hasWifi)) {
                        hasChanged = true;
                        hasWifi = info.isConnected();
                    }
                    //Log.d(TAG, info.getTypeName() + " is " + info.isConnected());
                }
            }

            hasConnectivity = hasMmobile || hasWifi;
            Log.v(TAG, "hasConn: " + hasConnectivity + " hasChange: " + hasChanged + " - "+(mqttClient == null || !mqttClient.isConnected()));
            if (hasConnectivity && hasChanged && (mqttClient == null || !mqttClient.isConnected())) {
                doConnect();
                //startTimer();
            } else if (!hasConnectivity && mqttClient != null && mqttClient.isConnected()) {
                //Log.d(TAG, "doDisconnect()");
                try {
                    token = mqttClient.disconnect();
                    token.waitForCompletion(1000);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    };

   /* public class MQTTBinder extends Binder {
        public MQTTService1 getService(){
            return MQTTService1.this;
        }
    }*/
   int lastStartedID=0;

    @Override
    public void onCreate() {
       // Log.d("flowcheck","inside onCreate() of MQTTService1");

        int NOTIFICATION_ID = (int) (System.currentTimeMillis()%10000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

           startMyOwnForeground();

        }
        else {
            String CHANNEL_ID = "my_channel_for_andon";

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Andon")
                    .setContentText("Running").build();

            startForeground(1, notification);
        }
        mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        IntentFilter intentf = new IntentFilter();
        /*setClientID();*/
        intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mqttBroadcastReceiver=new MQTTBroadcastReceiver();
        registerReceiver(mqttBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        EventBus.getDefault().register(this);

       /* mqttBroadcastReceiver=new MQTTBroadcastReceiver();
        registerReceiver(mqttBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));*/

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.vvt.andon";
        String channelName = "Andon Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*Creating seperate thread for running MQTT operations*/

        try {
            if(lastStartedID!=0)
            {

                Log.d("adghjj","inside onstart command stop self method");
                stopSelf(lastStartedID);
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        lastStartedID=startId;

        manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        Log.d("adghjj","inside onstart command restarting");

        new Thread(new MyThread(startId)).start();
       // Log.d("flowcheck","inside onStartCommand() of MQTTService1");

        //Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();

       // Log.v(TAG, "onStartCommand()");

       // Log.v(TAG, "subscriptiontopic=>"+SUBSCRIPTION_TOPIC+" emp id=>"+employeeID);
        //setClientID();

        return START_STICKY;
    }

    final class MyThread implements Runnable
    {
        private volatile boolean running = true;
        int service_id;

         MyThread(int service_id) {
            this.service_id = service_id;
            Log.d("adghjj","inside back ground thread constructor");
        }

        @Override
        public void run() {

            Log.d("adghjj","inside back ground thread run  method");

            SharedPreferences preferences=getSharedPreferences("LOGIN_SHARED_PREFERENCE",MODE_PRIVATE);
            SUBSCRIPTION_TOPIC=preferences.getString("EMPLOYEE_DEPARTMENT",null);
            employeeID= preferences.getString("EMPLOYEE_ID",null);
            boolean isLoggedIn=preferences.getBoolean("IS_LOGGEDIN",false);

            /*If the user logged out stop the thread*/
            if(!isLoggedIn)
            {
                return;  //this leaves the above run method
            }
            else
            {
                if(AndonUtils.isConnectedToInternet(MQTTService1.this))
                {
                    startTimer();
                    doConnect();
                }
            }


        }


    }

    private void startTimer(){
        if(mTimer1!=null)
        {
            stopTimer();
        }
        mTimer1 = new Timer();
        TimerTask mTt1 = new TimerTask() {
            public void run() {
                mTimerHandler.post(new Runnable() {
                    public void run() {
                        //TODO
                       // Log.d(TAG, "inside startTimer run method");
                        Log.d("adghjj","inside startTimer run method");
                        MQTTService1 mqttService1 = new MQTTService1();
                        if (!isMyServiceRunning(mqttService1.getClass())) {
                           // Log.d(TAG, "inside service not running method");

                            Log.d("adghjj","inside service not running method");

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                Intent serviceIntent = new Intent(MQTTService1.this, MQTTService1.class);
                                ContextCompat.startForegroundService(MQTTService1.this, serviceIntent );

                            }
                            else {
                                startService(new Intent(MQTTService1.this,MQTTService1.class));

                            }
                            //startService(new Intent(MQTTService1.this, MQTTService1.class));
                        }
                        else {
                            Log.d("adghjj","inside service running... method");
                        }
                        //doConnect();
                    }
                });
            }
        };

        mTimer1.schedule(mTt1, 1, 60*1000); // 1 minute delay
    }
/* to check whether the service is currently running or not */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                   // Log.i ("isMyServiceRunning?", true+"");
                    return true;
                }
            }
        }
       // Log.i ("isMyServiceRunning?", false+"");
        return false;
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      //  Log.d(TAG, "onConfigurationChanged()");
        android.os.Debug.waitForDebugger();
        super.onConfigurationChanged(newConfig);

    }

   /* private void setClientID(){

        deviceId = MqttAsyncClient.generateClientId();

        if(deviceId!=null)
        {
            if(deviceId.isEmpty())
            {
                deviceId=MqttClient.generateClientId();
            }
        }


        *//*WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        deviceId = wInfo.getMacAddress();
        if(deviceId == null){
            deviceId = MqttAsyncClient.generateClientId();
        }*//*
    }*/


    /*Connection process  to MQTT*/
    private void doConnect(){

       // Log.d("flowcheck","inside doConnect() of MQTTService1");
        String deviceId = MqttAsyncClient.generateClientId();

        if(deviceId !=null)
        {
            if(deviceId.isEmpty())
            {
                deviceId =MqttClient.generateClientId();
            }
        }

       // Log.d(TAG, "doConnect()");
        IMqttToken token;
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
       // final String HOST = "10.166.1.164";
        final String HOST = AndonUtils.getIPAddress(MQTTService1.this);



        //final String HOST = "192.168.1.114";
        //final String HOST = "iot.eclipse.org";
         final int PORT =1883;
         final String uri = "tcp://"+HOST+":"+PORT;

        SharedPreferences preferences=getSharedPreferences("LOGIN_SHARED_PREFERENCE",MODE_PRIVATE);
        SUBSCRIPTION_TOPIC=preferences.getString("EMPLOYEE_DEPARTMENT",null);
        employeeID= preferences.getString("EMPLOYEE_ID",null);


       // Log.d(TAG, "URI=> "+uri);
        try {
            mqttClient = new MqttAsyncClient(uri, deviceId, new MemoryPersistence());
            token = mqttClient.connect();
            token.waitForCompletion(3500);
            mqttClient.setCallback(new MqttEventCallback());
            token = mqttClient.subscribe(SUBSCRIPTION_TOPIC, 0);
            token.waitForCompletion(5000);
           // Log.d(TAG, "token.isComplete() ?=> "+token.isComplete()+" Subscription response=>"+token.getResponse()+" to SUBSCRIPTION_TOPIC=>"+SUBSCRIPTION_TOPIC);
        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            switch (e.getReasonCode()) {
                case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
                    Log.v(TAG, "REASON_CODE_BROKER_UNAVAILABLE " +e.getMessage());
                    e.printStackTrace();
                    break;
                case MqttException.REASON_CODE_CLIENT_TIMEOUT:
                    Log.v(TAG, "REASON_CODE_CLIENT_TIMEOUT" +e.getMessage());
                    e.printStackTrace();
                    break;
                case MqttException.REASON_CODE_CONNECTION_LOST:
                    Log.v(TAG, "REASON_CODE_CONNECTION_LOST" +e.getMessage());
                    e.printStackTrace();
                    break;
                case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
                    Log.v(TAG, "REASON_CODE_SERVER_CONNECT_ERROR" +e.getMessage());
                    e.printStackTrace();
                    break;
                case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
                    Intent i = new Intent("RAISEALLARM");
                    i.putExtra("ALLARM", e);
                    Log.e(TAG, "REASON_CODE_FAILED_AUTHENTICATION"+ e.getMessage());
                    break;
                default:
                    Log.e(TAG, "default case: " + e.getMessage());
            }
        }
    }


/*Event callbacks of MQTT */
    private class MqttEventCallback implements MqttCallback {



        @Override
        public void connectionLost(Throwable arg0) {

           // Log.i(TAG, "Connection Lost with " + arg0.getMessage());
            doConnect();

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
           // Log.i(TAG, "Deliverycompleted" + arg0.toString());
        }

        @Override
        public void messageArrived(String topic, final MqttMessage msg) throws Exception {

            String body = new String(msg.getPayload());
            Log.i(TAG, "Message arrived from topic" + topic);
            Log.i(TAG, "Message arrived is" + body);


            SharedPreferences preferences=getSharedPreferences("LOGIN_SHARED_PREFERENCE",MODE_PRIVATE);

             boolean isLoggedIn=preferences.getBoolean("IS_LOGGEDIN",false);


            if(isLoggedIn) {
               /* AudioManager manager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

                //To bring back to ringing mode
                if (manager != null) {
                    manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    int valuess = 15;//range(0-15)
                    manager.setStreamVolume(AudioManager.STREAM_MUSIC, manager.getStreamMaxVolume(valuess), 0);
                    manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, manager.getStreamMaxVolume(valuess), 0);
                    manager.setStreamVolume(AudioManager.STREAM_ALARM, manager.getStreamMaxVolume(valuess), 0);
                }*/


                if (body.contains("#")) {
                    //refresh case

                    //To mute ringing
                    if (manager != null) {
                        manager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    }

                    EventBus.getDefault().post(new NotificationEvent("#",topic));
                    showNotificationToUser(MQTTService1.this);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //To bring back to ringing mode

                            if (manager != null) {
                                manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            }
                        }
                    },1000);


                } else if (body.startsWith("Alert from")) {
                    //Initial case

                  /*  if (manager != null) {
                        int streamMaxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_RING);
                        //Toast.makeText(this, Integer.toString(streamMaxVolume), Toast.LENGTH_LONG).show(); //I got 7
                        manager.setStreamVolume(AudioManager.STREAM_RING, streamMaxVolume, AudioManager.FLAG_ALLOW_RINGER_MODES|AudioManager.FLAG_PLAY_SOUND);
                    }*/

                    //To bring back to ringing mode
                    if (manager != null) {
                        manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    }
                    showNotificationToUser(MQTTService1.this,body,topic);
                    EventBus.getDefault().post(new NotificationEvent(body,topic));
                    pushUserDetailsToServer();

                } else if (body.startsWith(employeeID)) {
                    //Acknowledge case
                    showNotificationToUser(MQTTService1.this);
                    EventBus.getDefault().post(new NotificationEvent(employeeID,topic));

                } else if (body.startsWith("$" + employeeID)) {
                    //check list case
                    showNotificationToUser(MQTTService1.this);
                    EventBus.getDefault().post(new NotificationEvent("$" + employeeID,topic));

                } else if (body.contains("MOE")) {
                    //This will occur when CA is Done
                    //push moe_notification only to MOE Team
                    EventBus.getDefault().post(new NotificationEvent("MOE",topic));
                    showNotificationToMOE(MQTTService1.this,"Containment Action done");
                    //showNotificationToUser("Containment Action done");
                }
                else {
                    //To mute ringing
                    if (manager != null) {
                        manager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    }

                    EventBus.getDefault().post(new NotificationEvent("#",topic));
                    showNotificationToUser(MQTTService1.this);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //To bring back to ringing mode

                            if (manager != null) {
                                manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            }
                        }
                    },500);
                }
            }

            //To bring back to ringing mode
            if (manager != null) {
                manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }
        }
    }

    public static boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }


    /*Push user details along with device IMEI number to confirm delivery of notifications*/
    private void pushUserDetailsToServer() {
        SharedPreferences sharedPreferences=getSharedPreferences("DEVICE_PREFERENCES",MODE_PRIVATE);
        String PUSH_URL=sharedPreferences.getString("PUSH_URL",null);
        PUSH_URL=PUSH_URL.replaceAll("\\s","%20");
        PUSH_URL=PUSH_URL.replaceAll("\\s","");
        PUSH_URL=PUSH_URL.replaceAll("\\s","+");
        //Log.d("pushurl",PUSH_URL);

        new CallPushUserDetailsToServerAPI().execute(PUSH_URL);

    }

    private class CallPushUserDetailsToServerAPI extends AsyncTask<String,Void,Void>
    {

        @Override
        protected Void doInBackground(String... voids) {
            APIServiceHandler sh = new APIServiceHandler();
            String jsonStr = sh.makeServiceCall(voids[0], APIServiceHandler.GET);

            return null;
        }

		/*@Override
		protected void onPostExecute(String jsonStr) {
			super.onPostExecute(jsonStr);

			if(jsonStr!=null)
			{
				if (jsonStr.equals("Server TimeOut")) {
					Toast.makeText(getApplicationContext(), jsonStr, Toast.LENGTH_LONG).show();
				}

				String resString=jsonStr.replaceAll("^\"|\"$", "");
				if(resString.equalsIgnoreCase("true"))
				{
					//showToast(resString);
				}
				else {
					//showToast(resString);
				}
			}

		}*/
    }

   /* public String getThread(){
        return Long.valueOf(thread.getId()).toString();
    }*/

    @Override
    public IBinder onBind(Intent intent) {
      //  Log.i(TAG, "onBind called");
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimer();

       // Log.d("flowcheck","inside onDestroy() of MQTTService1");

        EventBus.getDefault().unregister(this);

        unregisterReceiver(mqttBroadcastReceiver);
       // Log.i(TAG, "ondestroy!");

        Intent broadcastIntent = new Intent("uk.ac.shef.oak.ActivityRecognition.RestartSensor");
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        Log.d("flowcheck","inside onTaskRemoved() of MQTTService1");
        Intent broadcastIntent = new Intent("uk.ac.shef.oak.ActivityRecognition.RestartSensor");
        sendBroadcast(broadcastIntent);
        //Toast.makeText(getApplicationContext(), "Task removed", Toast.LENGTH_SHORT).show();

       // Log.i(TAG, "TaskRemoved()");

        super.onTaskRemoved(rootIntent);

    }


    public static void unsubscribeMQTT(){

       // Log.d("flowcheck","inside unsubscribeMQTT() of MQTTService1");

        IMqttToken token;

        try {
            token=mqttClient.unsubscribe(SUBSCRIPTION_TOPIC);
            token.waitForCompletion(2000);

           // Log.d(TAG,"unscribe success ?=>"+token.isComplete()+" unscribe status"+token.getResponse());
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(NotificationEvent event) {

       // Log.d("flowcheck","inside onMessageEvent()(Event bus) of MQTTService1");
        /* Do something */
        //Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
    };

    private void stopTimer(){
        if(mTimer1 != null){
            mTimer1.cancel();
            mTimer1.purge();
        }
    }


}