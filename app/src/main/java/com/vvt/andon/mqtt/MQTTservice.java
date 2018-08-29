package com.vvt.andon.mqtt;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.vvt.andon.activities.LoginActivity;
import com.vvt.andon.R;
import com.vvt.andon.activities.HomeActivity;
import com.vvt.andon.utils.APIServiceHandler;
import com.vvt.andon.utils.WebServices;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//import static com.example.preetham.activities.HomeActivity.employeeID;
import static com.vvt.andon.activities.HomeActivity.ipAddress;


public class MQTTservice extends Service
{
	private static boolean serviceRunning = false;
	private static int mid = 0;
	private static MQTTConnection connection = null;
	private final Messenger clientMessenger = new Messenger(new ClientHandler());

	private List<String> topics = new ArrayList<>();
	String SUBSCRIPTION_TOPIC="";
	String employeeID;

	PowerManager pm;
	PowerManager.WakeLock wl;

	public MqttClient client = null;

	@Override
	public void onCreate()
	{
		super.onCreate();

		SharedPreferences preferences=getSharedPreferences("LOGIN_SHARED_PREFERENCE",MODE_PRIVATE);
		SUBSCRIPTION_TOPIC=preferences.getString("EMPLOYEE_DEPARTMENT",null);
		employeeID= preferences.getString("EMPLOYEE_ID",null);

		Log.d("mqttflowcheck","SUBSCRIPTION_TOPIC=>"+SUBSCRIPTION_TOPIC);

		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (pm != null) {
			wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelock");
			wl.acquire();   //45*60*1000L /*45 minutes*/
            if(wl.isHeld())
            {
                Log.d("wakelock","Wake lock acquired in mqtt service oncreate");
            }

		}


		connection = new MQTTConnection();


	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
	    if(wl!=null  )
        {
            if(!wl.isHeld())
            {
                wl.acquire();

                if(wl.isHeld())
                {
                    Log.d("wakelock","Wake lock acquired in mqtt service onStartcommand");
                }

            }


        }
        if (isRunning())
        {

            /*Used for services which manages their own state and do not depend on the Intent data*/
            return START_STICKY;
        }


        super.onStartCommand(intent, flags, startId);
        /*
         * Start the MQTT Thread.
         */

        connection.start();

        return START_STICKY;



	}

	@Override
	public void onDestroy()
	{

		//connection.end();
		if(wl!=null)
		{
			wl.release();
			Log.d("wakelock","Wake lock released in mqtt service ondestroy()");
		}
		try {
			client.unsubscribe(SUBSCRIPTION_TOPIC);
		} catch (MqttException e) {
			e.printStackTrace();
		}

	}

	@Override
	public IBinder onBind(Intent intent)
	{
		/*
		 * Return a reference to our client handler.
		 */
		return clientMessenger.getBinder();
	}

	 private synchronized static boolean isRunning()
	 {
		 /*
		  * Only run one instance of the service.
		  */
		 if (serviceRunning == false)
		 {
			 serviceRunning = true;
			 return false;
		 }
		 else
		 {
			 return true;
		 }
	 }

	 /*
	  * These are the supported messages from bound clients
	  */
	 public static final int REGISTER = 0;
	 public static final int SUBSCRIBE = 1;
	 public static final int PUBLISH = 2;

	 /*
	  * Fixed strings for the supported messages.
	  */
	 public static final String TOPIC = "topic";
	 public static final String MESSAGE = "message";
	 public static final String STATUS = "status";
	 public static final String CLASSNAME = "classname";
	 public static final String INTENTNAME = "intentname";

	 /*
	  * This class handles messages sent to the service by
	  * bound clients.
	  */
	 class ClientHandler extends Handler
	 {
         @Override
         public void handleMessage(Message msg)
         {
        	 boolean status = false;

        	 switch (msg.what)
        	 {
        	 case SUBSCRIBE:
           	 case PUBLISH:
           		 	/*
           		 	 * These two requests should be handled by
           		 	 * the connection thread, call makeRequest
           		 	 */
           		 	connection.makeRequest(msg);
           		 	break;
           	 case REGISTER:
        	 {
        		 Bundle b = msg.getData();
        		 if (b != null)
        		 {
        			 Object target = b.getSerializable(CLASSNAME);
        			 if (target != null)
        			 {

        			 	Log.d("mqttflowcheck","inside REGISTER");
        				 /*
        				  * This request can be handled in-line
        				  * call the API
        				  */
        				 connection.setPushCallback((Class<?>) target);
        				 status = true;
        			 }
        			 CharSequence cs = b.getCharSequence(INTENTNAME);
        			 if (cs != null)
        			 {
        				 String name = cs.toString().trim();
        				 if (name.isEmpty() == false)
        				 {
            				 /*
            				  * This request can be handled in-line
            				  * call the API
            				  */
        					 connection.setIntentName(name);
        					 status = true;
        				 }
        			 }
        		 }
        		 ReplytoClient(msg.replyTo, msg.what, status);
        		 break;
        	 }
        	 }
         }
	 }

	 private void ReplytoClient(Messenger responseMessenger, int type, boolean status)
	 {
		 /*
		  * A response can be sent back to a requester when
		  * the replyTo field is set in a Message, passed to this
		  * method as the first parameter.
		  */
		 if (responseMessenger != null)
		 {
			 Bundle data = new Bundle();
			 data.putBoolean(STATUS, status);
			 Message reply = Message.obtain(null, type);
			 reply.setData(data);

			 try {
				 responseMessenger.send(reply);
			 } catch (RemoteException e) {
				 // TODO Auto-generated catch block
				 e.printStackTrace();
			 }
		 }
	 }

	enum CONNECT_STATE
	{
		DISCONNECTED,
		CONNECTING,
		CONNECTED
	}

	private class MQTTConnection extends Thread
	{
		private Class<?> launchActivity = null;
		private String intentName = null;
		private MsgHandler msgHandler = null;
		private static final int STOP = PUBLISH + 1;
		private static final int CONNECT = PUBLISH + 2;
		private static final int RESETTIMER = PUBLISH + 3;
		private CONNECT_STATE connState = CONNECT_STATE.DISCONNECTED;

		MQTTConnection()
		{
			msgHandler = new MsgHandler();
			msgHandler.sendMessage(Message.obtain(null, CONNECT));
		}



		public void end()
		{
			msgHandler.sendMessage(Message.obtain(null, STOP));
		}

		public void makeRequest(Message msg)
		{
			/*
			 * It is expected that the caller only invokes
			 * this method with valid msg.what.
			 */
			msgHandler.sendMessage(Message.obtain(msg));
		}

		public void setPushCallback(Class<?> activityClass)
		{
			launchActivity = activityClass;
		}

		public void setIntentName(String name)
		{
			intentName = name;
		}


		//Everything related to MQTT connection to Message received is handled by this class
		private class MsgHandler extends Handler implements MqttCallback
		{
		    /*private final String HOST = "iot.eclipse.org";*/
			private final String HOST = "10.166.1.164";
		    private final int PORT = 1883;
		    private final String uri = "tcp://"+HOST+":"+PORT;
			private final int MINTIMEOUT = 2000;
			private final int MAXTIMEOUT = 32000;
			private int timeout = MINTIMEOUT;

			private MqttConnectOptions options = new MqttConnectOptions();
			/*private Vector<String> topics = new Vector<String>();*/


			MsgHandler()
			{
				options.setCleanSession(true);
			    try
				{
					client = new MqttClient(uri, MqttClient.generateClientId(), null);
					client.setCallback(this);
				}
			    catch (MqttException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			@Override
			public void handleMessage(Message msg)
			{
				switch (msg.what)
				{
				case STOP:
				{
					/*
					 * Clean up, and terminate.
					 */

					Log.d("mqttflowcheck","Client is still connected");
					client.setCallback(null);
					if (client.isConnected())
					{
						try {
							Log.d("mqttflowcheck","disconnecting Client");
							client.disconnect();
							client.close();
						} catch (MqttException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					getLooper().quit();
					break;
				}
				case CONNECT:
				{
					if (connState != CONNECT_STATE.CONNECTED)
					{
					    try
						{
							client.connect(options);
							subscribe(SUBSCRIPTION_TOPIC);
							connState = CONNECT_STATE.CONNECTED;
							Log.d("mqttflowcheck","connected Client");
							//Log.d("mqttflowcheck", "Connected");
							//Toast.makeText(MQTTservice.this, "Connected", Toast.LENGTH_SHORT).show();
							timeout = MINTIMEOUT;
						}
					    catch (MqttException e)
						{
					    	Log.d("mqttflowcheck", "Connection attemp failed with reason code = " + e.getReasonCode() + e.getCause());
							if (timeout < MAXTIMEOUT)
							{
								timeout *= 2;
							}
					    	this.sendMessageDelayed(Message.obtain(null, CONNECT), timeout);
					    	return;
						}

					    /*
					     * Re-subscribe to previously subscribed topics
					     */


					    Log.d("mqttflowcheck", "topics size "+topics.size());

					    Iterator<String> i = topics.iterator();

						Log.d("mqttflowcheck", "has next? "+i.hasNext());
					    while (i.hasNext())
					    {
					    	subscribe(i.next());
					    }
					}
					break;
				}
				case RESETTIMER:
				{
					timeout = MINTIMEOUT;
					break;
				}
	        	case SUBSCRIBE:
	        	{
	        		boolean status = false;
	        		Bundle b = msg.getData();
	        		if (b != null)
	        		{
	        			CharSequence cs = b.getCharSequence(TOPIC);
	        			if (cs != null)
	        			{
	        				String topic = cs.toString().trim();
	        				if (!topic.isEmpty())
	        				{
	        					status = subscribe(topic);
								Log.d("mqttflowcheck","Subscribed to Client");
	        					/*
	        					 * Save this topic for re-subscription if needed.
	        					 */
	        					if (status)
	        					{
	        						topics.add(topic);
									Log.d("mqttflowcheck","topics size "+topics.size());
	        					}
	        				}
	        			}
	        		}
	        		ReplytoClient(msg.replyTo, msg.what, status);
	        		break;
	        	}
	        	case PUBLISH:
	        	{
	        		boolean status = false;
	        		Bundle b = msg.getData();
	        		if (b != null)
	        		{
	        			CharSequence cs = b.getCharSequence(TOPIC);
	        			if (cs != null)
	        			{
	        				String topic = cs.toString().trim();
	        				if (!topic.isEmpty())
	        				{
	        					cs = b.getCharSequence(MESSAGE);
		            			if (cs != null)
		            			{
		            				String message = cs.toString().trim();
		            				if (!message.isEmpty())
		            				{
		            					status = publish(topic, message);
		            				}
		            			}
	        				}
	        			}
	        		}
	        		ReplytoClient(msg.replyTo, msg.what, status);
	        		break;
	        	}
				}
			}

			private boolean subscribe(String topic)
			{
				try
				{
					Log.d("mqttflowcheck","Subscribed to Client");
					Log.d("mqttflowcheck","Subscription topic=>"+topic);
					client.subscribe(topic);
				}
				catch (MqttException e)
				{
					Log.d("mqttflowcheck", "Subscribe failed with reason code = " + e.getReasonCode());
					return false;
				}
				return true;
			}

			private boolean publish(String topic, String msg)
			{
				try
				{
					MqttMessage message = new MqttMessage();
					message.setPayload(msg.getBytes());
					client.publish(topic, message);
				}
				catch (MqttException e)
				{
					Log.d("mqttflowcheck", "Publish failed with reason code = " + e.getReasonCode());
					return false;
				}
				return true;
			}

			@Override
			public void connectionLost(Throwable arg0)
			{
                Log.d("mqttflowcheck","connectionLost with Client");
				Log.d("mqttflowcheck", "connectionLost");
				connState = CONNECT_STATE.DISCONNECTED;
				sendMessageDelayed(Message.obtain(null, CONNECT), timeout);
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken arg0)
			{
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception
			{
				Log.d("mqttflowcheck", topic + ":" + message.toString());
                String body = new String(message.getPayload());

				Log.d("notificationcheck","meaasge body"+body);

				if (intentName != null )
				{
					Intent intent = new Intent();
					intent.setAction(intentName);
					intent.putExtra(TOPIC, topic);
					intent.putExtra(MESSAGE, message.toString());
					sendBroadcast(intent);
				}
               /* if (body.contains("Alert from")) {
                    //  new GetContentOne().execute();

                    notificationBox(body);


                }*/

				Context context = getBaseContext();
				PendingIntent pendingIntent = null;
				Vibrator vibrator= (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);



				if (launchActivity != null)
				{
					Intent intent = new Intent(context, launchActivity);
					intent.setAction(Intent.ACTION_MAIN);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);

					//build the pending intent that will start the appropriate activity
					pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
				}

				Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


				//hardcoding pending intent
				Intent i=new Intent(getApplicationContext(),LoginActivity.class);
				PendingIntent pI=PendingIntent.getActivity(getApplicationContext(), 0, i, 0);
				//build the notification
				Builder notificationCompat = new Builder(context);
				notificationCompat.setAutoCancel(true)
						.setContentTitle("Andon System")
				        .setContentIntent(pI)
				        .setContentText( message.toString())
				        .setSmallIcon(R.drawable.abc)
				.setSound(alarmSound);

				/*Notification notification = notificationCompat.build();
				NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				nm.notify(mid++, notification);*/



				if (body.equals("#")) {
					vibrator.vibrate(900);
					Log.d("notificationcheck","Inside Alert from   # if block of service");
					//notificationBox(body);
				} else if (body.contains(employeeID + "/")) {
					Log.d("notificationcheck","Inside Alert from   employeeid/ if block of service");
					vibrator.vibrate(1000);

				} else if (body.equals("$" + employeeID)) {

					Log.d("notificationcheck","Inside Alert from  $ employeeid if block of service");

					vibrator.vibrate(1000);


				} else if (body.contains(SUBSCRIPTION_TOPIC)) {
					vibrator.vibrate(5000);

					notificationBox("Containment Action done");
					Log.d("notificationcheck","Inside TOPIC if block of service");
					//String[] msg = body.split("/");

					//hardcoding pending intent
					/*Intent in=new Intent(getApplicationContext(),LoginActivity.class);
					PendingIntent pi=PendingIntent.getActivity(getApplicationContext(), 0, in, 0);
					//build the notification
					Builder nc = new Builder(context);
					nc.setAutoCancel(true)
							.setContentTitle("Containment Action done")
							.setContentIntent(pi)
							.setContentText("expecting MOE comment to close ticket")
							.setSmallIcon(R.drawable.abc)
							.setSound(alarmSound);

					Notification notification = nc.build();
					NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					nm.notify(mid++, notification);*/

				} else if (body.contains("Alert from")) {
					Log.d("notificationcheck","Inside Alert from if block of service");
					Notification notification = notificationCompat.build();
					NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					if (nm != null) {
						nm.cancelAll();
					}
					nm.notify(mid++, notification);
					//  new GetContentOne().execute();
					//notificationBox(body);
					vibrator.vibrate(9000);
					pushUserDetailsToServer();
					//HomeActivity.vibrator.vibrate(6000);


				}

			}
		}
	}
    public void notificationBox(String msg) {
        Intent notificationIntent = new Intent(getApplicationContext(), HomeActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
        NotificationManager notif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (notif != null) {
			notif.cancelAll();
		}
		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification notify = new Notification.Builder
                (getApplicationContext()).setContentTitle("V V").setContentText(msg).
                setContentTitle("message").setSound(alarmSound).setContentIntent(contentIntent).setSmallIcon(R.drawable.abc).build();

        notify.flags |= Notification.FLAG_AUTO_CANCEL;
        notif.notify(0, notify);
              /*  finish();
                startActivity(getIntent());*/
    }

	private void pushUserDetailsToServer() {
		String imeiNumber,ipaddress,ntUserId,userName;
		SharedPreferences devicePreferences=getSharedPreferences("DEVICE_PREFERENCES",MODE_PRIVATE);
		imeiNumber=devicePreferences.getString("IMEI_NUMBER",null);
		ipaddress=devicePreferences.getString("IP_ADDRESS",null);
		ntUserId=devicePreferences.getString("NT_USERID",null);
		userName=devicePreferences.getString("USER_NAME",null);

		CallPushUserDetailsToServerAPI obj=new CallPushUserDetailsToServerAPI(imeiNumber,ipaddress,ntUserId,userName);
		obj.execute();

	}

	@SuppressLint("StaticFieldLeak")
	private class CallPushUserDetailsToServerAPI extends AsyncTask<Void,Void,Void>
	{
		String url="";
		String imeiNumber,ipaddress,ntUserId,userName;
		String ip;

		public CallPushUserDetailsToServerAPI(String imeiNumber, String ipaddress, String ntUserId, String userName) {
			this.imeiNumber = imeiNumber;
			this.ipaddress = ipaddress;
			this.ntUserId = ntUserId;
			this.userName = userName;

			SharedPreferences sharedPreferences=getSharedPreferences(LoginActivity.IP_ADDRESS_PREFERENCE,MODE_PRIVATE);
			ip=sharedPreferences.getString("IPADDRESS",null);

			if(imeiNumber!=null && ipAddress!=null && ipaddress!=null && ntUserId!=null && userName!=null)
			{

				Log.d("ipaddresscheck", "ip in service=>"+ip);
				url= "http://"+ip+":8080/AndonWebservices/rest/userinfo/"+userName+"/"+ntUserId+"/"+imeiNumber+"/"+ipaddress;
				url = url.replaceAll(" ", "%20");
				url = url.replaceAll(" ", "%20");
			}
		}

		@Override
		protected Void doInBackground(Void... voids) {
			APIServiceHandler sh = new APIServiceHandler();
			String jsonStr = sh.makeServiceCall(url, APIServiceHandler.GET);

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

}
