package com.vvt.andon.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vvt.andon.R;
import com.vvt.andon.adapters.EmployeesAdapter;
import com.vvt.andon.adapters.NotificationAdapter;
import com.vvt.andon.api_responses.allnotifications.AllNotificationsResponse;
import com.vvt.andon.api_responses.allnotifications.NotificationList;
import com.vvt.andon.api_responses.allusers.AllAvailableUsersResponse;
import com.vvt.andon.api_responses.allusers.EmployeeStatusList;
import com.vvt.andon.customs.customViewGroup;
import com.vvt.andon.mqtt.MQTTservice;
import com.vvt.andon.utils.APIServiceHandler;
import com.vvt.andon.utils.AndonUtils;
import com.vvt.andon.utils.OnResponseListener;
import com.vvt.andon.utils.WebServices;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.NotificationCompat;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener,
        OnResponseListener,SwipeRefreshLayout.OnRefreshListener
{

    RecyclerView mNotificationsRecyclerView,mUsersRecyclerView;
    SwipeRefreshLayout mSwipeRefreshLayout;
    LinearLayout mAcceptErrorLayout;
    Button mAccept,mLogOut;
    LinearLayoutManager layoutManager;
    GridLayoutManager gridLayoutManager;
    NotificationAdapter notificationAdapter;
    EmployeesAdapter employeesAdapter;

    //ProgressBar mProgressBar;

    List<NotificationList> notificationList;
    List<EmployeeStatusList> employeeStatusList;

    TextView mEmployeeName,mErrorId,mCancel;
    public static String employeeTeam="";
    //GridView mGridView;

    public static Vibrator vibrator;

    AlertDialog.Builder builder;
    AlertDialog alertDialog;

    //public  static String department="";
    String team,notificationID;
    public static String employeeName,employeeID,employeeDepartment,employeValueStream,employeeLineID,employeeDesignamtion;
    public static String ipAddress;
    boolean isLoggedIn=false;
    String TAG="HomeActivity";
    int count = 0;

    Snackbar snackbar;
    Toast mToast;

    private Messenger service = null;
    private final Messenger serviceHandler = new Messenger(new ServiceHandler());
    private IntentFilter intentFilter = null;
    private PushReceiver pushReceiver;


    public String BASE_URL="";

    PowerManager pm;
    PowerManager.WakeLock wl;

    Handler cacheHandler=new Handler();

    PackageInfo pinfo = null;
    String versionName;


    @Override
    protected void onStart() {
        super.onStart();

        NotificationManager notif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notif != null) {
            notif.cancelAll();
        }

        callAllNotificationsAPI();
        callAllAvailableUsers();

        //clearCacheRunnable.run();

        bindService(new Intent(this, MQTTservice.class), serviceConnection, 0);

        //subscribeToMQTT();

        if (android.os.Build.VERSION.SDK_INT > 9) {

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

        }
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(HomeActivity.this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1234);
            }
        } else {
            Intent intent = new Intent(HomeActivity.this, Service.class);
            startService(intent);
        }

        // implement setOnDrawerOpenListener event
        WindowManager manager = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE));
        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
        localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        localLayoutParams.gravity = Gravity.TOP;
        localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
// this is to enable the notification to recieve touch events
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
// Draws over status bar
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        localLayoutParams.height = (int) (50 * getResources().getDisplayMetrics().scaledDensity);
        localLayoutParams.format = PixelFormat.TRANSPARENT;
        customViewGroup view = new customViewGroup(this);
        manager.addView(view, localLayoutParams);


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getLogInSharedPreferenceData();
        initializeViews();


        try {
            pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            // int versionNumber = pinfo.versionCode;
             versionName = pinfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.MQTT.PushReceived");

        pushReceiver = new PushReceiver();
        registerReceiver(pushReceiver, intentFilter, null, null);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelock");
           /* wl.acquire(45*60*1000L *//*45 minutes*//*);*/
        }


        startService(new Intent(this, MQTTservice.class));
    }


    public void getLogInSharedPreferenceData() {

        SharedPreferences sharedPreferences=getSharedPreferences(LoginActivity.IP_ADDRESS_PREFERENCE,MODE_PRIVATE);
        ipAddress=sharedPreferences.getString("IPADDRESS",null);

        SharedPreferences preferences=getSharedPreferences(LoginActivity.LOGIN_PREFERENCE,MODE_PRIVATE);
        isLoggedIn=preferences.getBoolean("IS_LOGGEDIN",false);

        employeeName=preferences.getString("EMPLOYEE_NAME",null);
        employeeID= preferences.getString("EMPLOYEE_ID",null);
        employeeDepartment=preferences.getString("EMPLOYEE_DEPARTMENT",null);
        employeValueStream=preferences.getString("EMPLOYEE_VALUESTREAM",null);
        employeeLineID=preferences.getString("EMPLOYEE_LINEID",null);
        employeeDesignamtion=preferences.getString("EMPLOYEE_DESIGNATION",null);

        BASE_URL="http://"+ipAddress+":8080/AndonWebservices/rest/";
        
    }

    private void initializeViews() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        notificationList=new ArrayList<>();
        employeeStatusList=new ArrayList<>();

        //mProgressBar=findViewById(R.id.vP_ah_progressbar);
        //mProgressBar.setVisibility(View.GONE);

        mEmployeeName=findViewById(R.id.vT_ah_employee_name);
        mEmployeeName.setText(employeeName);

        mAcceptErrorLayout=findViewById(R.id.vL_ah_accept_layout);
        mErrorId=findViewById(R.id.vT_ah_error_id);

        //mGridView=findViewById(R.id.vG_employee_list);


        mNotificationsRecyclerView =findViewById(R.id.vR_recycler_view);
        mNotificationsRecyclerView.setHasFixedSize(true);
        layoutManager=new LinearLayoutManager(this);
        mNotificationsRecyclerView.setLayoutManager(layoutManager);

        notificationAdapter=new NotificationAdapter(this,notificationList,mErrorId);
        mNotificationsRecyclerView.setAdapter(notificationAdapter);

        mUsersRecyclerView=findViewById(R.id.vR_employee_list);
        mUsersRecyclerView.setHasFixedSize(true);
        gridLayoutManager=new GridLayoutManager(this,4);
        mUsersRecyclerView.setLayoutManager(gridLayoutManager);

        employeesAdapter=new EmployeesAdapter(this,employeeStatusList);
        mUsersRecyclerView.setAdapter(employeesAdapter);


       /* notificationAdapter=new NotificationAdapter(this,notificationList);
        employeesAdapter=new EmployeesAdapter(this,employeeStatusList);*/

       mCancel=findViewById(R.id.vT_ah_cancel);
       mAccept=findViewById(R.id.vB_ah_accept);
       mLogOut=findViewById(R.id.vB_ah_logout);

       mCancel.setOnClickListener(this);
       mAccept.setOnClickListener(this);
       mLogOut.setOnClickListener(this);

        mSwipeRefreshLayout=findViewById(R.id.vS_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        if (employeeDepartment.contains("MOE")) {
            mAcceptErrorLayout.setVisibility(View.INVISIBLE);
            //department = "MOE31";
        }  else if (employeeDepartment.contains("TEF")) {
            mAcceptErrorLayout.setVisibility(View.VISIBLE);
            //department = "TEF11";
        } else if (employeeDepartment.contains("LOM")) {
            mAcceptErrorLayout.setVisibility(View.VISIBLE);
            //department = "LOM";
        } else if (employeeDepartment.contains("FCM")) {
            mAcceptErrorLayout.setVisibility(View.VISIBLE);
            //department = "FCM";
        }

        /*callAllNotificationsAPI();
        callAllAvailableUsers();*/
    }

    @Override
    public void onClick(View v) {

        switch (v.getId())
        {
            case R.id.vB_ah_accept:
                vibrator.vibrate(200);
                notificationID =mErrorId.getText().toString();
                if(TextUtils.isEmpty(notificationID))
                {
                    showSnackBar(this,"Please select one issue from the above list");
                }
                else {
                    callAcceptMessageAPI(notificationID);
                }

                break;

            case R.id.vB_ah_logout:
                callLogOutAPI();
                break;

            case R.id.vT_ah_cancel:
                mErrorId.setText("");
                break;
        }

    }

    private void callLogOutAPI() {

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.logout_dialog, null);
        Button cancel = dialogView.findViewById(R.id.vB_lod_cancel);
        Button logOut = dialogView.findViewById(R.id.vB_lod_logout);

        builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        alertDialog = builder.create();
        alertDialog.show();

        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrator.vibrate(50);
                alertDialog.dismiss();

                if(AndonUtils.isConnectedToInternet(HomeActivity.this))
                {
                    if(isLoggedIn)
                    {
                        new LogOut().execute();
                        //showToast(employeeName+"  "+employeeID);
                    }

                }
                else {
                    showToast(getResources().getString(R.string.err_msg_nointernet));
                }

            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Toast.makeText(HomeActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();

            }
        });


    }

    private void callAcceptMessageAPI(String notificationID)
    {
        if(notificationID !=null && employeeTeam!=null && employeeID!=null)
        {
            if(AndonUtils.isConnectedToInternet(this))
            {
                new AcceptMessage().execute();


            }
            else {
                showToast(getResources().getString(R.string.err_msg_nointernet));
            }
        }
        else {
            showSnackBar(this,"errorID or team empty");
        }

    }

    private void callAllNotificationsAPI()
    {
        
        
        if(AndonUtils.isConnectedToInternet(this))
        {
            //mProgressBar.setVisibility(View.VISIBLE);
            WebServices<AllNotificationsResponse> webServices = new WebServices<AllNotificationsResponse>(this);
            webServices.getAllNotifications(BASE_URL, WebServices.ApiType.allNotifications,employeeDepartment,employeValueStream);


        }
        else {
            showToast(getResources().getString(R.string.err_msg_nointernet));
        }
    }

    private void callAllAvailableUsers()
    {
        if(AndonUtils.isConnectedToInternet(this))
        {
            WebServices<AllAvailableUsersResponse> webServices = new WebServices<AllAvailableUsersResponse>(this);
            webServices.getAllUsers(BASE_URL, WebServices.ApiType.allAvailableUsers,employeeDepartment,employeValueStream);


        }
        else {
            showToast(getResources().getString(R.string.err_msg_nointernet));
        }
    }

    public class LogOut extends AsyncTask<Void,Void,String>
    {
        private String url;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(employeeID!=null && ipAddress!=null)
            {
                url=BASE_URL+"loginprocess/"+employeeID;
                url = url.replaceAll(" ", "%20");
                url = url.replaceAll(" ", "%20");
            }
            else {
                showToast("User is not logedin");
            }


        }

        @Override
        protected String doInBackground(Void... voids) {
            APIServiceHandler sh = new APIServiceHandler();
            String jsonStr = sh.makeServiceCall(url, APIServiceHandler.GET);
            return jsonStr;
        }

        @Override
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

                    if(wl.isHeld())
                    {
                        wl.release();
                    }
                    stopService(new Intent(HomeActivity.this,MQTTservice.class));

                    trimCache(HomeActivity.this);
                    SharedPreferences preferences=getSharedPreferences(LoginActivity.LOGIN_PREFERENCE,MODE_PRIVATE);
                    SharedPreferences.Editor editor=preferences.edit();
                    editor.putBoolean("IS_LOGGEDIN",false);
                    //editor.clear();
                    editor.apply();

                    finish();
                    Intent logOutIntent=new Intent(HomeActivity.this,LoginActivity.class);
                    //logOutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(logOutIntent);
                }
                else {
                    showToast(resString);
                }
            }

        }
    }

    private class AcceptMessage extends AsyncTask<Void,Void,String>
    {

        private String url;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(employeeID!=null && ipAddress!=null &&employeeTeam!=null && notificationID!=null)
            {
                url=BASE_URL+"notification/accept/"+ notificationID + "/" + employeeID + "/" + employeeTeam;;
                url = url.replaceAll(" ", "%20");
                url = url.replaceAll(" ", "%20");

                Log.d("accepturl",url);

                mErrorId.setText("");
            }
            else {
                showToast("you cant accept this issue");
            }


        }

        @Override
        protected String doInBackground(Void... voids) {
            APIServiceHandler sh = new APIServiceHandler();
            String jsonStr = sh.makeServiceCall(url, APIServiceHandler.GET);
            return jsonStr;
        }

        @Override
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
                    recreate();
                }
                else if(resString.equalsIgnoreCase("false"))
                {
                    showToast("server busy wait for a while");
                }
                else {
                    showToast(resString);
                }
            }

        }
    }

    private void subscribeToMQTT() {
        String topic = employeeDepartment.trim();

        if (topic != null && topic.isEmpty() == false)
        {
            Bundle data = new Bundle();
            data.putCharSequence(MQTTservice.TOPIC, topic);
            Message msg = Message.obtain(null, MQTTservice.SUBSCRIBE);
            msg.setData(data);
            msg.replyTo = serviceHandler;
            try
            {
                if(service!=null)
                {
                    service.send(msg);

                }

            }
            catch (RemoteException e)
            {
                e.printStackTrace();
                showToast("Subscribe failed with exception:" + e.getMessage());
                //result.setText("Subscribe failed with exception:" + e.getMessage());
            }
        }
        else
        {
            showToast("Topic required.");
           /* result.setText("Topic required.");*/
        }


    }


    public void notificationBox(String msg) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        Intent notificationIntent = new Intent(getApplicationContext(), LoginActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
        NotificationManager notif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notif != null) {
            notif.cancelAll();
        }
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification notify = new Notification.Builder(getApplicationContext())
                .setContentTitle("Andon")
                .setContentText(msg)
                .setSound(alarmSound)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.abc).build();

        notify.flags |= Notification.FLAG_AUTO_CANCEL;
        if (notif != null) {
            notif.notify(0, notify);
        }
              /*  finish();
                startActivity(getIntent());*/
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        showExitDialog();
        //super.onBackPressed();
    }

    private void showExitDialog() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.exit_dialog, null);
        Button mYes = dialogView.findViewById(R.id.vB_ed_yes);
        Button mNo = dialogView.findViewById(R.id.vB_ed_no);

        builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        alertDialog = builder.create();
        alertDialog.show();

        mYes.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                vibrator.vibrate(200);
                alertDialog.dismiss();
                //freeMemory();
                finish();
                finishAffinity();

               /* Visibility returnTransition = buildReturnTransition();
                getWindow().setReturnTransition(returnTransition);*/

                finishAfterTransition();


            }
        });
        mNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                alertDialog.dismiss();

            }
        });
    }


    public void showToast(String message) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        mToast.show();
    }
    private void showSnackBar(Context context,String message)
    {
        Activity activity = (Activity) context;
        if(snackbar!=null)
        {
            snackbar.dismiss();
        }
        snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message,Snackbar.LENGTH_LONG);
        View view = snackbar.getView();
        view.setBackgroundColor(Color.BLACK);
        TextView tv = (TextView)view.findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16);
        snackbar.show();
    }


    @Override
    public void onResponse(Object response, WebServices.ApiType URL, boolean isSucces, int code) {

        switch (URL)
        {
            case allNotifications:
               /* if(mProgressBar.isShown())
                {
                    mProgressBar.setVisibility(View.GONE);
                }*/
               if(mSwipeRefreshLayout!=null)
               {
                   if(mSwipeRefreshLayout.isRefreshing())
                   {
                       mSwipeRefreshLayout.setRefreshing(false);
                   }

               }

                AllNotificationsResponse notificationsResponse= (AllNotificationsResponse) response;
                if(isSucces)
                {

                    if(code!=0)
                    {
                        if(code==200)
                        {
                            if(notificationsResponse!=null)
                            {
                                notificationList=notificationsResponse.getNotificationList();
                                setNotificationAdapter(notificationList);

                            }
                            else {
                                showSnackBar(this,"allNotificationsResponse null");

                            }
                            //showSnackBar(this," outside allNotificationsResponse block");


                        }
                        else {
                            showSnackBar(this,"Something went wrong try again later");
                        }
                    }
                }
                else {
                    //API call failed
                    showSnackBar(this,"API call failed");
                }
                break;
            case allAvailableUsers:
                if(isSucces)
                {
                    AllAvailableUsersResponse allAvailableUsersResponse= (AllAvailableUsersResponse) response;
                    if(code!=0)
                    {
                        if(code==200)
                        {
                            if(allAvailableUsersResponse!=null)
                            {
                                subscribeToMQTT();
                                employeeStatusList=allAvailableUsersResponse.getEmployeeStatusList();
                                setAllUsersAdapter(employeeStatusList);

                            }
                        }
                        else {
                            showSnackBar(this,"Something went wrong try again later");
                        }
                    }

                }
                else {
                    //API call failed
                    showSnackBar(this,"API call failed");

                }
                break;
        }

    }

    private void setAllUsersAdapter(List<EmployeeStatusList> employeeStatusList) {
        if(employeeStatusList!=null)
        {
            employeesAdapter=new EmployeesAdapter(this,employeeStatusList);
            mUsersRecyclerView.setAdapter(employeesAdapter);
            employeesAdapter.notifyDataSetChanged();
        }

    }

    private void setNotificationAdapter(List<NotificationList> notificationList) {
        if(notificationList!=null)
        {

            //Log.d("notificationdetails","error id=>"+notificationList.get(0).getNotificationId()+"error=>"+notificationList.get(0).getError());
            //Toast.makeText(this, "notificationList not null", Toast.LENGTH_SHORT).show();
            if(!notificationList.isEmpty())
            {
                notificationAdapter=new NotificationAdapter(this,notificationList,mErrorId);
                mNotificationsRecyclerView.setAdapter(notificationAdapter);
                notificationAdapter.notifyDataSetChanged();

            }
            else {
                showToast("Currently there are no issues");

            }

            //showSnackBar(this,"size=>"+mNotificationsRecyclerView.getAdapter()+"");

        }
        else {
            showToast("List is empty");
        }

    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary),getResources().getColor(R.color.colorAccent),
                getResources().getColor(R.color.green));
        refreshContent();

    }

    public void refreshContent() {
        callAllNotificationsAPI();
        notificationAdapter.notifyDataSetChanged();
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                callAllNotificationsAPI();
                mSwipeRefreshLayout.setRefreshing(false);

            }
        },3000);*/
    }


    @Override
    protected void onStop()
    {
        super.onStop();
        //unbindService(serviceConnection);
//        trimCache(HomeActivity.this);
        freeMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        freeMemory();

        /*if(wl!=null)
        {
            wl.release();
        }*/
        //trimCache(HomeActivity.this);

        if(serviceConnection!=null)
        {
            unbindService(serviceConnection);
           // stopService(new Intent(this, MQTTservice.class));
            unregisterReceiver(pushReceiver);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        /*subscribeToMQTT();*/
        if(wl!=null)
        {
            wl.acquire();//5*60*60*1000L /*5 hours*/
            if(wl.isHeld())
            {
                Log.d("wakelock","Wake lock acquired in activity");
            }

        }
        registerReceiver(pushReceiver, intentFilter);
    }


    @Override
    protected void onPause()
    {
        super.onPause();
        //unregisterReceiver(pushReceiver);
    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder)
        {
            service = new Messenger(binder);
            Bundle data = new Bundle();
            //data.putSerializable(MQTTservice.CLASSNAME, MainActivity.class);
            data.putCharSequence(MQTTservice.INTENTNAME, "com.example.MQTT.PushReceived");
            Message msg = Message.obtain(null, MQTTservice.REGISTER);
            msg.setData(data);
            msg.replyTo = serviceHandler;
            try
            {
                service.send(msg);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {

        }
    };
    public class ServiceHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MQTTservice.SUBSCRIBE: 	break;
                case MQTTservice.PUBLISH:		break;
                case MQTTservice.REGISTER:		break;
                default:
                    super.handleMessage(msg);
                    return;
            }

            Bundle b = msg.getData();
            if (b != null)
            {
                //TextView result = (TextView) findViewById(R.id.textResultStatus);
                Boolean status = b.getBoolean(MQTTservice.STATUS);
                if (!status)
                {
                    //Log.d("messagedata","false");
                    //showToast("fail");
                    /*result.setText("Fail");*/
                }
                else
                {
                    //Log.d("messagedata",b+"");
                    //showToast("Success");
                    //showToast(b+"");
                    /*result.setText("Success");*/
                }
            }
        }
    }



    public class PushReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent i)
        {
            String topic = i.getStringExtra(MQTTservice.TOPIC);
            String message = i.getStringExtra(MQTTservice.MESSAGE);

            Log.d("notificationcheck","meaasge body in home activity"+message);
            if(message.startsWith("#"))
            {
                //mUsersRecyclerView.refreshDrawableState();
                //callAllAvailableUsers();

               // Log.d("notificationcheck","Inside Alert from   # if block of activity");
                vibrator.vibrate(900);
                recreate();
            }
            else if (message.contains(employeeID + "/")) {
                // Log.d("notificationcheck","Inside Alert from   employeeid/ if block of activity");
                recreate();
                vibrator.vibrate(1000);
            } else if (message.equals("$" + employeeID)) {
               // Log.d("notificationcheck","Inside Alert from  $ employeeid if block of activity");
                vibrator.vibrate(1000);
                recreate();

            } else if (message.contains(employeeDepartment)) {

               // Log.d("notificationcheck","Inside Alert from  employeeDepartment if block of activity");
                recreate();
                vibrator.vibrate(5000);
                notificationBox("");


               /* Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                Intent in=new Intent(getApplicationContext(),LoginActivity.class);
                PendingIntent pi=PendingIntent.getActivity(getApplicationContext(), 0, in, 0);
                //build the notification
                Notification.Builder nc = new Notification.Builder(context);
                nc.setAutoCancel(true)
                        .setContentTitle("Containment Action done")
                        .setContentIntent(pi)
                        .setContentText("expecting MOE comment to close ticket")
                        .setSmallIcon(R.drawable.abc)
                        .setSound(alarmSound);

                Notification notification = nc.build();
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(0, notification);*/
            }
            else if(message.contains("Alert from")){

                //Log.d("notificationcheck","Inside Alert from if block of activity");
                vibrator.vibrate(9000);
                notificationBox(message);
                recreate();

            }

           // callAllAvailableUsers();
           // Toast.makeText(context, "Push message received - " + topic + ":" + message, Toast.LENGTH_LONG).show();
        }
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
    private class CallPushUserDetailsToServerAPI extends AsyncTask<Void,Void,String>
    {
        String url="";
        String imeiNumber,ipaddress,ntUserId,userName;

        public CallPushUserDetailsToServerAPI(String imeiNumber, String ipaddress, String ntUserId, String userName) {
            this.imeiNumber = imeiNumber;
            this.ipaddress = ipaddress;
            this.ntUserId = ntUserId;
            this.userName = userName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(imeiNumber!=null && ipAddress!=null && ipaddress!=null && ntUserId!=null && userName!=null)
            {
                url=BASE_URL+"userinfo/"+userName+"/"+ntUserId+"/"+imeiNumber+"/"+ipaddress;
                url = url.replaceAll(" ", "%20");
                url = url.replaceAll(" ", "%20");
            }
            else {
                showToast("User is not logedin");
            }
        }

        @Override
        protected String doInBackground(Void... voids) {
            APIServiceHandler sh = new APIServiceHandler();
            String jsonStr = sh.makeServiceCall(url, APIServiceHandler.GET);
            return jsonStr;
        }

        @Override
        protected void onPostExecute(String jsonStr) {
            super.onPostExecute(jsonStr);

            if(jsonStr!=null)
            {
                if (jsonStr.equals("Server TimeOut")) {
                    Toast.makeText(getApplicationContext(), jsonStr, Toast.LENGTH_LONG).show();
                }

                String resString=jsonStr.replaceAll("^\"|\"$", "");
                if(resString.equalsIgnoreCase("success"))
                {
                    showToast(resString);
                }
                else {
                    showToast("Fail to push user details");
                }
            }

        }
    }

   /* private final Runnable clearCacheRunnable =new Runnable() {
        @Override
        public void run() {
            trimCache(HomeActivity.this);
            //showToast("cache cleared");

            cacheHandler.postDelayed(clearCacheRunnable,10*60*1000); //10 minutes
        }
    };*/

   /*This will clear the cache of our app*/
    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            File externalCacheDir=context.getExternalCacheDir();
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            {
                deleteExternalDir(externalCacheDir);

            }

            deleteDir(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean deleteExternalDir(File dir) {

        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteExternalDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        }
        else {
            return false;
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        }
        else {
            return false;
        }
    }

    public void freeMemory(){
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();
    }

}
