package com.vvt.andon.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.vvt.andon.R;
import com.vvt.andon.adapters.EmployeesAdapter;
import com.vvt.andon.adapters.NotificationAdapter;
import com.vvt.andon.api_responses.allnotifications.AllNotificationsResponse;
import com.vvt.andon.api_responses.allnotifications.NotificationList;
import com.vvt.andon.api_responses.allusers.AllAvailableUsersResponse;
import com.vvt.andon.api_responses.allusers.EmployeeStatusList;
import com.vvt.andon.api_responses.general.InteractionResponse;
import com.vvt.andon.api_responses.logout.LogOutResponse;
import com.vvt.andon.customs.customViewGroup;
import com.vvt.andon.events.NotificationEvent;
import com.vvt.andon.mqtt.MQTTService1;
import com.vvt.andon.service.MyJobService;
import com.vvt.andon.utils.APIServiceHandler;
import com.vvt.andon.utils.AndonUtils;
import com.vvt.andon.utils.NotificationClass;
import com.vvt.andon.utils.OnResponseListener;
import com.vvt.andon.utils.WebServices;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener,
        OnResponseListener,SwipeRefreshLayout.OnRefreshListener,NotificationAdapter.NotificationInterface
{

    String TAG="HomeActivity";

    RecyclerView mNotificationsRecyclerView,mUsersRecyclerView;
    SwipeRefreshLayout mSwipeRefreshLayout;
    LinearLayout mAcceptErrorLayout;
    Button mAccept,mLogOut;

    FloatingActionButton mFlash;
    boolean isFlashEnabled=false;
    Animation zoomin,zoomout;

    LinearLayoutManager layoutManager;
    GridLayoutManager gridLayoutManager;
    NotificationAdapter notificationAdapter;
    EmployeesAdapter employeesAdapter;

    Handler refreshHandler;

    private Timer mTimer1;
    private Handler mTimerHandler = new Handler();

    List<NotificationList> notificationList;
    List<EmployeeStatusList> employeeStatusList;

    TextView mEmployeeName,mErrorId,mCancel;

    public static Vibrator vibrator;

    AlertDialog.Builder builder;
    AlertDialog alertDialog;

    ProgressDialog progressDialog;

    //public  static String department="";
    String notificationID;
    public static String employeeName,employeeID,employeeDepartment,employeValueStream,employeeLineID,employeeDesignamtion,notificationTeam;
    public static String ipAddress;
    boolean isLoggedIn=false;

    Snackbar snackbar;
    Toast mToast;

    private IntentFilter intentFilter = null;
   // private PushReceiver pushReceiver;

    public String BASE_URL="";

   public boolean IS_USER_INTERACTING=false;

   JobScheduler jobScheduler;
   JobInfo jobInfo;
    private static final int JOB_ID = 101;

    /*//Broadcast receiver to identify Screen OFF and Screen ON events
    BroadcastReceiver mybroadcast = new BroadcastReceiver() {

        //When Event is published, onReceive method is called
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
           // Log.i("[BroadcastReceiver]", "MyReceiver");

            if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
                //refreshList();
                Log.i("[BroadcastReceiver]", "Screen ON");
            }
            else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                Log.i("[BroadcastReceiver]", "Screen OFF");
            }

        }
    };*/

    String[] actionTypeArray = {"Containment action", "Preventive Action","Corrective action"};
    String selectedAction;
    String selectionPosition;
    String actionType="machine";;

    @Override
    protected void onStart() {
        super.onStart();
       // Log.d("flowcheck","inside onStart()");
        /*To deletes the previous downloaded apk file */

        trimCache(this);

        callAllNotificationsAPI();
        callAllAvailableUsersAPI();
        //startTimer();


        MQTTService1 mqttService1=new MQTTService1();
        if (!isMyServiceRunning(mqttService1.getClass())) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //  Log.d("versionchecck","Build.VERSION_CODES>=M");
                Intent serviceIntent = new Intent(this, MQTTService1.class);
                ContextCompat.startForegroundService(this, serviceIntent );
            }
            else {
                // Log.d("versionchecck","Build.VERSION_CODES<M");
                startService(new Intent(this,MQTTService1.class));

            }
            // startService(new Intent(this,MQTTService1.class));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
       // Log.d("flowcheck","inside onResume()");

        if(alertDialog!=null)
        {
            if(alertDialog.isShowing())
            {
                alertDialog.dismiss();
            }
        }

        if(progressDialog!=null)
        {
            if(progressDialog.isShowing())
            {
                progressDialog.dismiss();
            }
        }

        if (mSwipeRefreshLayout != null) {
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // Log.d("flowcheck","inside onCreate()");

        /*to preventing from taking screen shots*/
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_home);
        getLogInSharedPreferenceData();

       // jobScheduler= (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .permitAll()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());

        constructJob();
        initializeViews();
        this.refreshHandler = new Handler(Looper.getMainLooper());

       /* registerReceiver(mybroadcast, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(mybroadcast, new IntentFilter(Intent.ACTION_SCREEN_OFF));*/

    }


    private void constructJob()
    {
        JobInfo.Builder builder=new JobInfo.Builder(JOB_ID,new ComponentName(this, MyJobService.class));
      /*  //if we want to send data through bundle
       PersistableBundle persistableBundle=new PersistableBundle();
       persistableBundle.putString("KEY","value");*/

       // builder.setPeriodic(15*60 * 1000); /* Repeat job for every 15 minutes*/
        builder.setPeriodic(5*60 * 1000); /* Repeat job for every 5 minutes*/

        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // WIFI or ethernet network
        builder.setPersisted(true);

        jobInfo=builder.build();
        jobScheduler= (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.schedule(jobInfo);
        }

    }


    /*is to identify specified service is running or not*/
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    Log.i ("isMyServiceRunning?", true+"");
                    return true;
                }
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
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
                        MQTTService1 mqttService1 = new MQTTService1();
                        if(!isAppIsInBackground(HomeActivity.this))
                        {
                            if(!IS_USER_INTERACTING)
                            {
                                //Log.d(TAG, "inside inner if block");
                                if (!isMyServiceRunning(mqttService1.getClass())) {
                                  //  Log.d(TAG,"MyService not Running");

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        Intent serviceIntent = new Intent(HomeActivity.this, MQTTService1.class);
                                        ContextCompat.startForegroundService(HomeActivity.this, serviceIntent );
                                    }
                                    else {
                                        startService(new Intent(HomeActivity.this,MQTTService1.class));

                                    }
                                    // startService(new Intent(this,MQTTService1.class));
                                }
                                else {
                                    //Log.d(TAG,"MyService Running");
                                }
                            }

                        }

                        //doConnect();
                                            }
                });
            }
        };

        mTimer1.schedule(mTt1, 1, 1*60*1000); //5 minutes
    }

    private void stopTimer(){
        if(mTimer1 != null){
            mTimer1.cancel();
            mTimer1.purge();
        }
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

        EventBus.getDefault().register(this);

        try {
          //  File toDelete = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), "ANDON.apk");
           new DeleteRecursive().execute();

        }catch (Exception e)
        {
            e.printStackTrace();
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        notificationList=new ArrayList<>();
        employeeStatusList=new ArrayList<>();


        mFlash=findViewById(R.id.fab_flash);
        mFlash.setOnClickListener(this);

        zoomin = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        zoomout = AnimationUtils.loadAnimation(this, R.anim.zoom_out);
       /* mFlash.setAnimation(zoomin);
        mFlash.setAnimation(zoomout);*/

        mEmployeeName=findViewById(R.id.vT_ah_employee_name);
        mEmployeeName.setText(employeeName);

        mAcceptErrorLayout=findViewById(R.id.vL_ah_accept_layout);
        mErrorId=findViewById(R.id.vT_ah_error_id);

        mNotificationsRecyclerView =findViewById(R.id.vR_recycler_view);
        mNotificationsRecyclerView.setHasFixedSize(true);
        layoutManager=new LinearLayoutManager(this);
        mNotificationsRecyclerView.setLayoutManager(layoutManager);

        notificationAdapter=new NotificationAdapter(this,notificationList);
        mNotificationsRecyclerView.setAdapter(notificationAdapter);

        mUsersRecyclerView=findViewById(R.id.vR_employee_list);
        mUsersRecyclerView.setHasFixedSize(true);
        gridLayoutManager=new GridLayoutManager(this,4);
        mUsersRecyclerView.setLayoutManager(gridLayoutManager);

        employeesAdapter=new EmployeesAdapter(this,employeeStatusList);
        mUsersRecyclerView.setAdapter(employeesAdapter);

       mCancel=findViewById(R.id.vT_ah_cancel);
       mAccept=findViewById(R.id.vB_ah_accept);
       mLogOut=findViewById(R.id.vB_ah_logout);

       mCancel.setOnClickListener(this);
       mAccept.setOnClickListener(this);
       mLogOut.setOnClickListener(this);

        mSwipeRefreshLayout=findViewById(R.id.vS_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);


        /*
         *  If the Employee belongs to TEF or LOM or FCM then they are eligible to accept the error and work on it
         *  If the department is MOE they can only monitor the work progress and finally after issue fixing they are responsible
         *  to verify the fix and close the issue by  giving some closing comment
        */
        if (employeeDepartment.contains("MOE")) {
            mAcceptErrorLayout.setVisibility(View.GONE);

        }  else if (employeeDepartment.contains("TEF")) {
            mAcceptErrorLayout.setVisibility(View.VISIBLE);

        } else if (employeeDepartment.contains("LOM")) {
            mAcceptErrorLayout.setVisibility(View.VISIBLE);

        } else if (employeeDepartment.contains("FCM")) {
            mAcceptErrorLayout.setVisibility(View.VISIBLE);
        }
        else {
            mAcceptErrorLayout.setVisibility(View.GONE);
        }


    }


    @Override
    public void onClick(View v) {

        switch (v.getId())
        {
            case R.id.vB_ah_accept:
                hideKeyBoard();
                //vibrator.vibrate(100);
                notificationID = mErrorId.getText().toString();
                if (TextUtils.isEmpty(notificationID)) {
                    showSnackBar(this, "Please select one issue from the above list");
                } else {

                    if (notificationTeam != null) {

                        callAcceptMeaasgeAPI(notificationID, employeeID, notificationTeam);
                        mErrorId.setText("");

                    } else {
                        showSnackBar(this, "Please select issue from the above list again");
                    }

                }

                break;

            case R.id.vB_ah_logout:
                hideKeyBoard();
                showLogOutDialog();

                break;

            case R.id.vT_ah_cancel:
                hideKeyBoard();
                mErrorId.setText("");
                break;

            case R.id.fab_flash:
                if(isFlashEnabled)
                {
                    turnOFFFlash();
                }
                else {
                    turnONFlash();
                }
                break;
        }

    }

    /*Interface method receivad from Adapter*/
    @Override
    public void acceptError(String errorId,String team) {

        notificationTeam=team;

        mErrorId.setText(errorId);

       // Log.d("interfaceflowcheck","accept error with error id=>"+errorId);

    }

    /*Interface method receivad from Adapter*/
    @Override
    public void giveCA(final String errorId, final String team) {

       // Log.d("interfaceflowcheck","giveCA with error id=>"+errorId+""+team);
        showActionPopup(errorId,employeeID,team);

    }

    /*Interface method receivad from Adapter*/
    @Override
    public void giveMOEComment(String errorId, String team) {

       // Log.d("interfaceflowcheck","giveCA with error id=>"+errorId+""+team);
        callgetCaGivenAPI(errorId,team);

    }

    /*Interface method receivad from Adapter*/
    @Override
    public void checklist(String errorId) {
       // Log.d("interfaceflowcheck","checklist with error id=>"+errorId);
        showCheckListPopup(errorId,employeeID);

    }

    private void showLogOutDialog() {
        IS_USER_INTERACTING=true;

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
               // vibrator.vibrate(100);
                alertDialog.dismiss();
                IS_USER_INTERACTING=true;
                callLogoutAPI();

            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Toast.makeText(HomeActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
                IS_USER_INTERACTING=false;

            }
        });


    }

    public void callLogoutAPI() {

        if (AndonUtils.isConnectedToInternet(getApplicationContext())) {
            progressDialog=new ProgressDialog(HomeActivity.this);

            if(progressDialog!=null)
            {
                if(!progressDialog.isShowing())
                {

                    progressDialog.setCancelable(false);
                    progressDialog.setMessage("Please wait...");
                    progressDialog.show();

                    WebServices<LogOutResponse> webServices = new WebServices<LogOutResponse>(this);
                    webServices.logOut(BASE_URL, WebServices.ApiType.logOut,  employeeID);
                }
                else {
                    showSnackBar(this,"Please wait untill the current process to finish");
                }
            }


        }
        else {
            Toast.makeText(this, getResources().getString(R.string.err_msg_nointernet) + "", Toast.LENGTH_SHORT).show();
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

    private void callAllAvailableUsersAPI()
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

    private void callAcceptMeaasgeAPI(String notificationId,String employeeId,String team)
    {
        if(AndonUtils.isConnectedToInternet(this))
        {
            progressDialog=new ProgressDialog(HomeActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Accepting...");
            progressDialog.show();

            WebServices<InteractionResponse> webServices = new WebServices<InteractionResponse>(this);
            webServices.acceptError(BASE_URL, WebServices.ApiType.acceptError,notificationId,employeeId,team);

        }
        else {
            showToast(getResources().getString(R.string.err_msg_nointernet));
        }
    }

    private void callgiveCAAPI(String notificationId,String enteredMessage,String selectionPosition,String employeeId,String team)
    {
       // Log.d("entereddetails","entered msg=>"+enteredMessage+" selected pos=>"+selectionPosition);
        if(AndonUtils.isConnectedToInternet(this))
        {
            progressDialog=new ProgressDialog(HomeActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Updating...");
            progressDialog.show();

            WebServices<InteractionResponse> webServices = new WebServices<InteractionResponse>(this);
            webServices.giveCA(BASE_URL, WebServices.ApiType.giveCA,notificationId,enteredMessage,selectionPosition,employeeId,team);

        }
        else {
            showToast(getResources().getString(R.string.err_msg_nointernet));
        }
    }

    private void callgiveMOECommentAPI(String notificationId,String action,String employeeId,String team)
    {
        if(AndonUtils.isConnectedToInternet(this))
        {
            progressDialog=new ProgressDialog(HomeActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Updating...");
            progressDialog.show();

            WebServices<InteractionResponse> webServices = new WebServices<InteractionResponse>(this);
            webServices.giveMOEComment(BASE_URL, WebServices.ApiType.giveMOEComment,notificationId,action,employeeId,team);

        }
        else {
            showToast(getResources().getString(R.string.err_msg_nointernet));
        }
    }

    private void callgetCaGivenAPI(String notificationId,String team)
    {
        if(AndonUtils.isConnectedToInternet(this))
        {
            progressDialog=new ProgressDialog(HomeActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Retriving details...");
            progressDialog.show();

            WebServices<InteractionResponse> webServices = new WebServices<InteractionResponse>(this);
            webServices.getCAGiven(BASE_URL, WebServices.ApiType.getCAGiven,notificationId,team);

        }
        else {
            showToast(getResources().getString(R.string.err_msg_nointernet));
        }
    }

    private void callCheckListAPI(String notificationId,String response,String employeeId)
    {
        if(AndonUtils.isConnectedToInternet(this))
        {
            progressDialog=new ProgressDialog(HomeActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Updating...");
            progressDialog.show();

            WebServices<InteractionResponse> webServices = new WebServices<InteractionResponse>(this);
            webServices.checkList(BASE_URL, WebServices.ApiType.checklistConfirm,notificationId,response,employeeId);

        }
        else {
            showToast(getResources().getString(R.string.err_msg_nointernet));
        }
    }

    private void showMOEpoPup(String resolvedMessage, final String notificationID,final String employeeTeam) {

        IS_USER_INTERACTING=true;
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.moe_popup_layout, null);

        final EditText mComment = dialogView.findViewById(R.id.vE_mpl_entered_text);
        TextView mYes = dialogView.findViewById(R.id.vT_mpl_ok);
        TextView mNo = dialogView.findViewById(R.id.vT_mpl_cancel);
        TextView mMessage=dialogView.findViewById(R.id.vT_mpl_messagebody);

        mMessage.setText(resolvedMessage);

        builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        alertDialog = builder.create();
        alertDialog.show();
        mYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                //replaceAll(System.getProperty("line.separator"), "") is used to remove new line characters from entered text
                String enteredText = mComment.getText().toString().trim().replaceAll(System.getProperty("line.separator"), "");

                if(TextUtils.isEmpty(enteredText) || enteredText.length()<5)
                {
                    //Toast.makeText(context, "Closing comment length should be atleast 5 characters", Toast.LENGTH_SHORT).show();
                    showToast("Closing comment length should be atleast 5 characters");
                }
                else {
                    alertDialog.dismiss();
                    callgiveMOECommentAPI(notificationID,enteredText,employeeID,employeeTeam);
                   /* callMOEClosingAPI(notificationID,enteredText,employeeID,employeeTeam);*/
                    IS_USER_INTERACTING=false;
                }
                hideKeyBoard();

               /* refreshList();*/
            }
        });

        mNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                hideKeyBoard();
                IS_USER_INTERACTING=false;
                refreshList();
            }
        });

    }

    private void showActionPopup(final String notificationID, final String employeeID, final String employeeTeam) {
        IS_USER_INTERACTING=true;

        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.containment_action_layout, null);

        final EditText mComment = dialogView.findViewById(R.id.vMLT_entered_text);
        TextView mYes = dialogView.findViewById(R.id.vT_cal_ok);
        TextView mNo = dialogView.findViewById(R.id.vT_cal_cancel);
        final AppCompatSpinner spinner=dialogView.findViewById(R.id.vS_action_type);

        final RadioGroup mRadioGroup=(RadioGroup)dialogView.findViewById(R.id.vR_radiogroup);

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                switch (checkedId)
                {
                    case R.id.vRB_machine:
                        spinner.setVisibility(View.VISIBLE);
                        actionType="machine";
                        break;
                    case R.id.vRB_process:
                        spinner.setVisibility(View.VISIBLE);
                        actionType="process";
                        break;
                    case R.id.vRB_organization:
                        spinner.setVisibility(View.GONE);
                        selectionPosition= String.valueOf(7);
                        actionType="organization";
                        break;
                }

            }
        });

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,actionTypeArray);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(arrayAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAction=actionTypeArray[position];
                selectionPosition= String.valueOf(position);
                if(actionType.equals("machine"))
                {
                    selectionPosition= String.valueOf(position+1);
                }
                else if(actionType.equalsIgnoreCase("process"))
                {
                    selectionPosition= String.valueOf(position+4);
                }
                else
                {
                    selectionPosition= String.valueOf(7);

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        alertDialog = builder.create();
        alertDialog.show();

        mYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //replaceAll(System.getProperty("line.separator"), "") is used to remove new line characters from entered text

                // showToast("Selected position is =>"+selectionPosition);
               /* String enteredMessage = mComment.getText().toString().trim().replaceAll(System.getProperty("line.separator"), "");
                if (TextUtils.isEmpty(enteredMessage) || enteredMessage.length() < 5) {
                    //Toast.makeText(context, "Closing action should be atleast of 5 characters", Toast.LENGTH_SHORT).show();
                    showToast("Closing action should be atleast of 5 characters");
                } else {
                    alertDialog.dismiss();
                    callgiveCAAPI(notificationID,enteredMessage,employeeID,employeeTeam);
                    //callContainmentActionAPI(notificationID,enteredMessage,employeeID,employeeTeam);

                }*/

                String enteredMessage = mComment.getText().toString().trim().replaceAll("[^A-Za-z0-9]","");

                // showToast("Selected position is =>"+selectionPosition);
                if (TextUtils.isEmpty(enteredMessage) || enteredMessage.length() < 5) {
                    //Toast.makeText(context, "Closing action should be atleast of 5 characters", Toast.LENGTH_SHORT).show();
                    showToast("Closing action should be atleast of 5 characters");
                } else if (enteredMessage.length() > 1000) {
                    showToast("Closing action should not be more than 1000 characters");
                } else {
                    alertDialog.dismiss();
                    callgiveCAAPI(notificationID, enteredMessage, selectionPosition, employeeID, employeeTeam);
                    //callContainmentActionAPI(notificationID,enteredMessage,employeeID,employeeTeam);

                }

                hideKeyBoard();
                IS_USER_INTERACTING=false;

            }
        });

        mNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IS_USER_INTERACTING=false;
                refreshList();
                alertDialog.dismiss();
                hideKeyBoard();

            }
        });

    }

    private void showCheckListPopup(final String notificationID, final String employeeID)
    {
        IS_USER_INTERACTING=true;
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.check_list_layout, null);

        TextView mYes = dialogView.findViewById(R.id.vT_cll_yes);
        TextView mNo = dialogView.findViewById(R.id.vT_cll_no);

        builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        alertDialog = builder.create();
        alertDialog.show();

        mYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
                alertDialog.dismiss();
                callCheckListAPI(notificationID,"Yes",employeeID);
                IS_USER_INTERACTING=false;

            }
        });

        mNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
                alertDialog.dismiss();
                callCheckListAPI(notificationID,"No",employeeID);
                IS_USER_INTERACTING=false;

            }
        });

    }

    @Override
    public void onBackPressed() {
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
                //vibrator.vibrate(200);
                alertDialog.dismiss();
                //freeMemory();
                 HomeActivity.this.finish();
                 finishAffinity();
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
        TextView tv = view.findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16);
        snackbar.show();
    }


    @Override
    public void onResponse(Object response, WebServices.ApiType URL, boolean isSucces, int code) {

        switch (URL) {
            case allNotifications:
                if(progressDialog!=null)
                {
                    if(progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                    }
                }
                if (mSwipeRefreshLayout != null) {
                    if (mSwipeRefreshLayout.isRefreshing()) {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                }

                AllNotificationsResponse notificationsResponse = (AllNotificationsResponse) response;
                if (isSucces) {

                    if (code != 0) {
                        if (code == 200) {
                            if (notificationsResponse != null) {
                                notificationList = notificationsResponse.getNotificationList();
                                setNotificationAdapter(notificationList);

                            } else {
                                showSnackBar(this, "Invalid response from server");

                            }
                            //showSnackBar(this," outside allNotificationsResponse block");


                        } else {
                            showSnackBar(this, "Something went wrong try again later");
                        }
                    }
                } else {
                    //API call failed
                    showSnackBar(this, "Server is busy");
                }
                break;
            case allAvailableUsers:
                if(progressDialog!=null)
                {
                    if(progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                    }
                }
                if (isSucces) {
                    AllAvailableUsersResponse allAvailableUsersResponse = (AllAvailableUsersResponse) response;
                    if (code != 0) {
                        if (code == 200) {
                            if (allAvailableUsersResponse != null) {
                                //subscribeToMQTT();
                                employeeStatusList = allAvailableUsersResponse.getEmployeeStatusList();
                                setAllUsersAdapter(employeeStatusList);

                            }
                        } else {
                            showSnackBar(this, "Something went wrong try again later");
                        }
                    }

                } else {
                    //API call failed
                    showSnackBar(this, "Server is busy");

                }
                break;

            case acceptError:
                if(progressDialog!=null)
                {
                    if(progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                    }
                }

                InteractionResponse acceptErrorResponse = (InteractionResponse) response;
                if (isSucces) {
                    if (code == 200) {
                        if (acceptErrorResponse != null) {
                            if (acceptErrorResponse.getMessage().equalsIgnoreCase("true")) {
                                //success
                                refreshList();
                            } else {
                                showToast(acceptErrorResponse.getMessage());
                            }


                        } else {
                            showSnackBar(this, "No response from server");
                        }
                    } else {
                        //failure
                        showSnackBar(this, "Something went wrong try again later");
                    }
                } else {
                    //API call failed
                    showSnackBar(this, "Server is busy");
                }
                break;

            case giveCA:
                if(progressDialog!=null)
                {
                    if(progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                    }
                }

                InteractionResponse giveCAResponse = (InteractionResponse) response;
                if (isSucces) {
                    if (code == 200) {
                        if (giveCAResponse != null) {

                            if (giveCAResponse.getMessage().equalsIgnoreCase("true")) {
                                //success
                                showToast("Message saved");
                                refreshList();
                            } else {
                                showToast(giveCAResponse.getMessage());
                            }


                        } else {
                            showSnackBar(this, "No response from server");
                        }
                    } else {
                        //failure
                        showSnackBar(this, "Something went wrong try again later");
                    }
                } else {
                    //API call failed
                    showSnackBar(this, "Server is busy");
                }
                break;


            case checklistConfirm:
                if(progressDialog!=null)
                {
                    if(progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                    }
                }

                InteractionResponse checklistConfirmResponse = (InteractionResponse) response;
                if (isSucces) {
                    if (code == 200) {
                        if (checklistConfirmResponse != null) {

                            if (checklistConfirmResponse.getMessage().equalsIgnoreCase("true")) {
                                //success
                                refreshList();
                                showToast("Need to Fill the checklist in Line");
                            } else  if (checklistConfirmResponse.getMessage().equalsIgnoreCase("false")){
                                refreshList();
                            }
                            else {
                                showToast(checklistConfirmResponse.getMessage());
                            }

                        } else {
                            showSnackBar(this, "No response from server");
                        }

                    } else {
                        //failure
                        showSnackBar(this, "Something went wrong try again later");
                    }
                } else {
                    //API call failed
                    showSnackBar(this, "Server is busy");
                }
                break;

            case getCAGiven:
                if(progressDialog!=null)
                {
                    if(progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                    }
                }
                InteractionResponse getCAGivenResponse = (InteractionResponse) response;
                if (isSucces) {
                    if (code == 200) {
                        if (getCAGivenResponse != null) {

                                //success
                                String[] msg = getCAGivenResponse.getMessage().split("/");
                            if (msg.length > 2) {
                                String msgID = msg[1].replace("\"", "");
                                String resolvedMessage = "Resolver Error" + ": " + msg[2].replace("\"", "");
                                String team = msg[3].replace("\"", "");

                                showMOEpoPup(resolvedMessage, msgID, team);
                            } else {
                                showToast(getCAGivenResponse.getMessage());
                            }



                        } else {
                            showSnackBar(this, "No response from server");
                        }
                    } else {
                        //failure
                        showSnackBar(this, "Something went wrong try again later");
                    }
                } else {
                    //API call failed
                    showSnackBar(this, "Server is busy");
                }
                break;

            case giveMOEComment:
                if(progressDialog!=null)
                {
                    if(progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                    }
                }
                InteractionResponse giveMOECommentResponse = (InteractionResponse) response;
                if (isSucces) {
                    if (code == 200) {
                        if (giveMOECommentResponse != null) {
                            if (giveMOECommentResponse.getMessage().equalsIgnoreCase("true")) {
                                //success
                                showToast("Message saved");

                                //To recreate activity
                                refreshList();
                            } else {
                                showToast(giveMOECommentResponse.getMessage());
                            }

                        } else {
                            showSnackBar(this, "No response from server");
                        }


                    } else {
                        //failure
                        showSnackBar(this, "Something went wrong try again later");
                    }
                } else {
                    //API call failed
                    showSnackBar(this, "Server is busy");
                }
                break;

            case logOut:
                if(progressDialog!=null)
                {
                    if(progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                    }
                }
                LogOutResponse logOutResponse = (LogOutResponse) response;
                if (isSucces) {
                    if (logOutResponse != null) {
                        if (logOutResponse.getMessage() != null) {
                            if (logOutResponse.getMessage().equalsIgnoreCase("success")) {

                                MQTTService1.unsubscribeMQTT();
                                stopService(new Intent(HomeActivity.this, MQTTService1.class));

                                SharedPreferences preferences = getSharedPreferences(LoginActivity.LOGIN_PREFERENCE, MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putBoolean("IS_LOGGEDIN", false);
                                //editor.clear();
                                editor.apply();

                                jobScheduler.cancel(JOB_ID);

                                Intent logOutIntent = new Intent(HomeActivity.this, LoginActivity.class);
                              /*  logOutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                logOutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);*/
                                startActivity(logOutIntent);
                                finish();
                                trimCache(HomeActivity.this);

                            } else {
                                showSnackBar(this, "Logout failed please try again");

                            }
                        } else {
                            showSnackBar(this, "Logout failed please try again");
                        }
                    } else {
                        showSnackBar(this, "No response from server");
                    }
                } else {
                    //API call failed
                    showSnackBar(this, "Logout failed");
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
                notificationAdapter=new NotificationAdapter(this,notificationList);
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
        //refreshList();
        /*refreshContentData();*/
    }

    public void refreshContent() {
        /*Intent launchHomeActivity = new Intent(HomeActivity.this, HomeActivity.class);
        launchHomeActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(launchHomeActivity);*/

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        //To remove all the previous notification icons from notification bar
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
       /* callAllAvailableUsersAPI();
        callAllNotificationsAPI();*/


        startActivity(getIntent());
        overridePendingTransition(0, 0);
        finish();

    }
    public void refreshList()
    {
        startActivity(getIntent());
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Log.d("flowcheck","inside onPause()");
        if(alertDialog!=null)
        {
            if(alertDialog.isShowing())
            {
                alertDialog.dismiss();
            }
        }

        if(progressDialog!=null)
        {
            if(progressDialog.isShowing())
            {
                progressDialog.dismiss();
            }
        }

        if (mSwipeRefreshLayout != null) {
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

        }
    }


    @Override
    protected void onStop()
    {
        super.onStop();
        //stopTimer();
        //Log.d("flowcheck","inside onStop()");
       // EventBus.getDefault().unregister(this);
        hideKeyBoard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       // Log.d("flowcheck","inside onDestroy()");

        EventBus.getDefault().unregister(this);

        hideKeyBoard();
        turnOFFFlash();


       if(alertDialog!=null)
       {
           if(alertDialog.isShowing())
           {
               alertDialog.dismiss();
           }
       }

        refreshHandler.removeCallbacksAndMessages(null);
       /*If the user is still logedin send broadcast to start service*/
        if(isLoggedIn)
        {
            Intent broadcastIntent = new Intent("uk.ac.shef.oak.ActivityRecognition.RestartSensor");
            sendBroadcast(broadcastIntent);
        }
        //unregisterReceiver(mybroadcast);

        freeMemory();
       // trimCache(this);
    }

   /*This will clear the cache of our app presented in current context*/
   public static void trimCache(Context context) {
       try {

           File dir = context.getCacheDir();
           if (dir != null && dir.isDirectory()) {
               //deleteDir(dir);

               if(deleteDir(dir))
               {
                 //  Log.d("clearcache","cache cleared");

               }
               else {
                  // Log.d("clearcache","can not clear cache");
               }
           }

       } catch (Exception e) {
           e.printStackTrace();
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

           // Log.d("cachedfile",dir.getAbsolutePath());

            return dir.delete();
        }
        else if(dir.isFile())
        {
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


    /* To Receive Event from background service*/

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(NotificationEvent event) {
       /* Do something */
       final String message=event.getMessage();
       final String dept=event.getDepartment();
       final AudioManager manager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
      // Log.d("receivedevent","received event=>"+message);
        //Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

       /* //To bring back to ringing mode
        if (manager != null) {
            int valuess = 15;//range(0-15)
            manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, manager.getStreamMaxVolume(valuess), 0);
            manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, manager.getStreamMaxVolume(valuess), 0);
            manager.setStreamVolume(AudioManager.STREAM_ALARM, manager.getStreamMaxVolume(valuess), 0);
        }*/

        if(message.startsWith("Alert from"))
        {

            if (manager != null) {
               int streamMaxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_RING);
                //Toast.makeText(this, Integer.toString(streamMaxVolume), Toast.LENGTH_LONG).show(); //I got 7
                manager.setStreamVolume(AudioManager.STREAM_RING, streamMaxVolume, AudioManager.FLAG_ALLOW_RINGER_MODES|AudioManager.FLAG_PLAY_SOUND);
            }


            //To bring back to ringing mode
            if (manager != null) {
                manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }


            this.refreshHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!IS_USER_INTERACTING)
                    {
                        //refreshContent();
                        refreshList();
                    }
                    refreshHandler.removeCallbacksAndMessages(null);
                   /* pushUserDetailsToServer();*/
                }
            },2000);

            NotificationClass.showNotificationToUser(HomeActivity.this,message,dept);
           // pushUserDetailsToServer();

        }
        else if(message.contains("MOE"))
        {
           /* if (manager != null) {
                int streamMaxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_RING);
                //Toast.makeText(this, Integer.toString(streamMaxVolume), Toast.LENGTH_LONG).show(); //I got 7
                manager.setStreamVolume(AudioManager.STREAM_RING, streamMaxVolume, AudioManager.FLAG_ALLOW_RINGER_MODES|AudioManager.FLAG_PLAY_SOUND);
            }*/
            this.refreshHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!IS_USER_INTERACTING)
                    {
                        // refreshContent();
                        refreshList();
                    }

                  /*  showNotificationToUser("Containment Action done");*/
                    refreshHandler.removeCallbacksAndMessages(null);
                }
            },2000);
            //refreshContent();
            NotificationClass.showNotificationToMOE(HomeActivity.this,"Containment Action done");

        }
        else {
           /* if (manager != null) {
                int streamMaxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_RING);
               // Toast.makeText(this, Integer.toString(streamMaxVolume), Toast.LENGTH_LONG).show(); //I got 7
                manager.setStreamVolume(AudioManager.STREAM_RING, streamMaxVolume, AudioManager.FLAG_ALLOW_RINGER_MODES|AudioManager.FLAG_PLAY_SOUND);
            }*/

            //To mute ringing
            if (manager != null) {
                manager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            }

            NotificationClass.showNotificationToUser(HomeActivity.this);

            this.refreshHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    //To bring back to ringing mode
                    if (manager != null) {
                        manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    }

                    // refreshContent();
                    if (!IS_USER_INTERACTING) {
                       // refreshContent();
                        refreshList();
                        refreshHandler.removeCallbacksAndMessages(null);
                    }

                }
            }, 1000);

        }

   }

    private void hideKeyBoard() {
        try {
            //InputMethodManager is used to hide the virtual keyboard from the user after finishing the user input
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            if (imm.isAcceptingText()) {
                imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            }
        } catch (NullPointerException e) {
            Log.e("Exception", e.getMessage() + ">>");
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

    private void turnONFlash()
    {

        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String cameraId = null; // Usually front camera is at 0 position.
        try {
            cameraId = camManager.getCameraIdList()[0];
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                camManager.setTorchMode(cameraId, true);

                isFlashEnabled=true;

                mFlash.startAnimation(zoomin);

                mFlash.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary,this.getTheme())));
                mFlash.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_highlight_white));
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void turnOFFFlash()
    {

        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String cameraId = null; // Usually front camera is at 0 position.
        try {
            cameraId = camManager.getCameraIdList()[0];
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                camManager.setTorchMode(cameraId, false);

                isFlashEnabled=false;
                mFlash.startAnimation(zoomout);

                mFlash.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.dark_gray,this.getTheme())));
                mFlash.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_highlight_black));

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public class DeleteRecursive extends AsyncTask<Void,Void,Void>
    {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

    }

        @Override
        protected Void doInBackground(Void... voids) {
            try
            {
                File toDelete = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), "ANDON.apk");
                deleteRecursive(toDelete);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }


            return null;
        }
    }

    void deleteRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();

    }

}
