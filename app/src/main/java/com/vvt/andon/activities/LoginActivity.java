package com.vvt.andon.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.vvt.andon.R;
import com.vvt.andon.api_responses.login.UserLoginResponse;
import com.vvt.andon.utils.AndonUtils;
import com.vvt.andon.utils.OnResponseListener;
import com.vvt.andon.utils.WebServices;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements OnClickListener, OnResponseListener {

    LinearLayout mEditIpaddressLayout, mIpaddressLayout, mPasswordLayout;
    CheckBox mShowPassword;
    Button mLoginButton;
    EditText mEmPloyeeNameEditText, mEmployeeIDEditText, mPasswordEditText, mIPAddressEditText;
    TextView mPasswordOkButton, mPasswordCancelButton, mIpaddressOkButton, mIpaddressCancelButton;
    TextView mVersionName;
    ProgressBar mProgressbar;

    String employeeName, employeeID, password, ipAddress;
    Toast mToast;
    Snackbar snackbar;
    public static String IP_ADDRESS_PREFERENCE = "IPADDRESS_SHARED_PREFERENCE";
    public static String LOGIN_PREFERENCE = "LOGIN_SHARED_PREFERENCE";


    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;

    public static String DEVICE_UNIQUE_NUMBER="",DEVICE_IPADDRESS="";


    boolean isLoggedIn = false;

    PackageInfo pinfo = null;
    String versionName;

    int PERMISSION_ALL = 1;

    String[] PERMISSIONS = { Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onStart() {
        super.onStart();

        checkPowerOptimizationPermission();

        ipAddress=AndonUtils.getIPAddress(this);
      // ipAddress=sharedPreferences.getString("IPADDRESS",null);
        if(ipAddress==null)
        {
         showToast("IP address is empty");
        }
        loadIMEI();

        //AndonUtils.saveIPtoExternalDirectory(this,"123456789");

        //String savedIP=AndonUtils.getIPFromExternalDirectory(this);
        //Log.d("savedip",savedIP);
       // getBattery_percentage();
       // this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        DEVICE_IPADDRESS=getLocalIpAddress();

        if(!hasPermissions(this, PERMISSIONS)){
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showToast("storage permission has not granted");
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }

        NotificationManager notif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notif != null) {
            notif.cancelAll();
        }

        Log.d("devive details","IMEI/UNIQUEID=>"+DEVICE_UNIQUE_NUMBER+"IP address=>"+DEVICE_IPADDRESS);

        try {
            pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            // int versionNumber = pinfo.versionCode;
            versionName = pinfo.versionName;
            mVersionName.setText("V:"+versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    void getBattery_percentage()
    {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level / (float)scale;
        float p = batteryPct * 100;
        Toast.makeText(this, "Battery level is=>"+String.valueOf(Math.round(p)), Toast.LENGTH_SHORT).show();

        Log.d("Battery percentage",String.valueOf(Math.round(p)));
    }

    @Override
    protected void onStop() {
        super.onStop();
        //unregisterReceiver(mBatInfoReceiver);
    }

    /*private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level / (float)scale;
            float p = batteryPct * 100;

            Toast.makeText(LoginActivity.this, "Battery level is=>"+String.valueOf(Math.round(p)), Toast.LENGTH_SHORT).show();

            Log.d("Battery percentage",String.valueOf(Math.round(p)));

        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*to preventing from taking screen shots*/
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_login);

        SharedPreferences preferences = getSharedPreferences(LOGIN_PREFERENCE, MODE_PRIVATE);
        isLoggedIn = preferences.getBoolean("IS_LOGGEDIN", false);
        if (isLoggedIn) {
            gotoHomeActivity();
        }
        initializeViews();
    }

    private void initializeViews() {
        mVersionName=findViewById(R.id.version_name);
        mEmPloyeeNameEditText = findViewById(R.id.vE_employee_name);
        mEmployeeIDEditText = findViewById(R.id.vE_employee_id);
        mPasswordEditText = findViewById(R.id.vE_password);
        mIPAddressEditText = findViewById(R.id.vE_ipaddress);

        mShowPassword = findViewById(R.id.vC_show_password);

        mLoginButton = findViewById(R.id.vB_login);
        mLoginButton.setOnClickListener(this);

        mPasswordOkButton = findViewById(R.id.vT_password_ok);
        mPasswordCancelButton = findViewById(R.id.vT_password_cancel);
        mIpaddressOkButton = findViewById(R.id.vT_ipaddress_ok);
        mIpaddressCancelButton = findViewById(R.id.vT_ipaddress_cancel);
        mPasswordOkButton.setOnClickListener(this);
        mPasswordCancelButton.setOnClickListener(this);
        mIpaddressCancelButton.setOnClickListener(this);
        mIpaddressOkButton.setOnClickListener(this);

        mEditIpaddressLayout = findViewById(R.id.vL_edit_ipaddress_layout);
        mIpaddressLayout = findViewById(R.id.vL_ipaddress_layout);
        mPasswordLayout = findViewById(R.id.vL_password_layout);
        mEditIpaddressLayout.setOnClickListener(this);

        mEditIpaddressLayout.setVisibility(View.VISIBLE);
        mPasswordLayout.setVisibility(View.GONE);
        mIpaddressLayout.setVisibility(View.GONE);

        mProgressbar = findViewById(R.id.vP_progressbar);
        mProgressbar.setVisibility(View.GONE);


        // add onCheckedListener on checkbox
        // when user clicks on this checkbox, this is the handler.
        mShowPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // checkbox status is changed from uncheck to checked.
                if (!isChecked) {
                    // show password
                    mEmployeeIDEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    // hide password
                    mEmployeeIDEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
            }
        });

    }

    @SuppressLint("BatteryLife")
    private void checkPowerOptimizationPermission() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
            else {
                //Permissions granted do whatever you want to do
            }
        }
    }

    private void CheckLogInStatus() {
        if (isLoggedIn) {
            //Goto next page
        } else {
            //initialize views
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.vB_login:
                if(mProgressbar!=null)
                {
                    if(mProgressbar.getVisibility()==View.VISIBLE)
                    {
                        showSnackBar(this,"wait untill the current process to finish");

                    }
                    else {
                        validateFields();
                    }
                }


                //callLoginAPI();
                //gotoHomeActivity();
                break;

            case R.id.vL_edit_ipaddress_layout:
                showPasswordLayout();
                break;

            case R.id.vT_password_cancel:
                hidePasswordLayout();
                break;

            case R.id.vT_password_ok:
                validatePassword();
                break;

            case R.id.vT_ipaddress_cancel:
                hideIPAddressLayout();
                break;

            case R.id.vT_ipaddress_ok:
                saveIpAddress();
                break;
        }

    }

    private void validateFields() {
        if (!validateEmployeeName()) {
            return;
        } else if (!validateEmployeeID()) {
            return;
        }
        callLoginAPI();

    }

    private boolean validateEmployeeName() {

        employeeName = mEmPloyeeNameEditText.getText().toString().trim();

        if (employeeName.isEmpty() || employeeName.length() < 3 /*|| !isValidUserName(employeeName)*/) {
            //Snackbar.make(mLoginButton,"Please enter a valid user name", Snackbar.LENGTH_SHORT).show();
            showSnackBar(this, "Please enter a valid user name");
            return false;
        }

        return true;
    }

    private boolean validateEmployeeID() {
        if (mEmployeeIDEditText.getText().toString().trim().isEmpty()) {
            //Snackbar.make(mLoginButton,"Password Field should not be empty", Snackbar.LENGTH_SHORT).show();
            showSnackBar(this, "Password Field should not be empty");
            return false;
        } else if (mEmployeeIDEditText.getText().toString().trim().length() < 3) {
            //Snackbar.make(mLoginButton,"Invalid password", Snackbar.LENGTH_SHORT).show();
            showSnackBar(this, "Invalid password");
            return false;
        }

        return true;
    }

    private boolean isValidUserName(String name) {
        String regexUserName = "^[A-Za-z\\s]+$";
        Pattern p = Pattern.compile(regexUserName, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(name);
        return m.matches();
    }

    private void callLoginAPI() {

        employeeName = mEmPloyeeNameEditText.getText().toString();
        employeeID = mEmployeeIDEditText.getText().toString();

        SharedPreferences sharedPreferences=getSharedPreferences(LoginActivity.IP_ADDRESS_PREFERENCE,MODE_PRIVATE);
        String ip=sharedPreferences.getString("IPADDRESS",null);
        String BASE_URL="http://"+ip+":8080/AndonWebservices/rest/";

        if (AndonUtils.isConnectedToInternet(getApplicationContext())) {
            mProgressbar.setVisibility(View.VISIBLE);
            WebServices<UserLoginResponse> webServices = new WebServices<UserLoginResponse>(LoginActivity.this);
            webServices.userLogIn(BASE_URL, WebServices.ApiType.userlogin, employeeName, employeeID);

        } else {
            //Snackbar.make(mSignup,R.string.err_msg_nointernet, Snackbar.LENGTH_SHORT).show();
            Toast.makeText(this, getResources().getString(R.string.err_msg_nointernet) + "", Toast.LENGTH_SHORT).show();
        }
    }


    private void showPasswordLayout() {
        mEditIpaddressLayout.setVisibility(View.GONE);
        mIpaddressLayout.setVisibility(View.GONE);
        mPasswordLayout.setVisibility(View.VISIBLE);

    }

    private void hidePasswordLayout() {
        hideKeyBoard();
        mEditIpaddressLayout.setVisibility(View.VISIBLE);
        mIpaddressLayout.setVisibility(View.GONE);
        mPasswordLayout.setVisibility(View.GONE);

    }

    private void validatePassword() {

        String password = mPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(password) || password.length() < 4) {
            showSnackBar(this, "Enter a valid password");
            /* Snackbar snackbar = Snackbar.make(mLoginButton,"Enter a valid password",Snackbar.LENGTH_SHORT);
            View view = snackbar.getView();
            view.setBackgroundColor(Color.BLACK);
            TextView tv = (TextView)view.findViewById(android.support.design.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            snackbar.show();*/

        } else if (password.equalsIgnoreCase("123456")) {
            //success
            mPasswordEditText.setText("");
            showIpAddressLayout();
        } else {
            showSnackBar(this, "Password incorrect");
            //showToast("Password incorrect");
        }
    }

    private void showIpAddressLayout() {
        mEditIpaddressLayout.setVisibility(View.GONE);
        mPasswordLayout.setVisibility(View.GONE);
        mIpaddressLayout.setVisibility(View.VISIBLE);

        mIPAddressEditText.requestFocus();

        SharedPreferences preferences = getSharedPreferences(IP_ADDRESS_PREFERENCE, MODE_PRIVATE);
       /* String currentIp = preferences.getString("IPADDRESS", null);*/
        String currentIp = AndonUtils.getIPAddress(LoginActivity.this);
        if (currentIp != null) {
            mIPAddressEditText.setText(currentIp);
        } else {
            mIPAddressEditText.setText("");
        }


    }

    private void hideIPAddressLayout() {
        hideKeyBoard();
        mEditIpaddressLayout.setVisibility(View.VISIBLE);
        mPasswordLayout.setVisibility(View.GONE);
        mIpaddressLayout.setVisibility(View.GONE);

    }

    private void saveIpAddress() {
        String ip = mIPAddressEditText.getText().toString();
        if (TextUtils.isEmpty(ip) || ip.length() < 12 || !ip.contains(".")) {
            showToast("enter a valid IP address");
            hideKeyBoard();

        } else {
            ipAddress = ip;
            AndonUtils.saveIPAddressPreference(LoginActivity.this,ip);
           /* SharedPreferences preferences = getSharedPreferences(IP_ADDRESS_PREFERENCE, MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("IPADDRESS", ip);
            editor.apply();*/

            hideIPAddressLayout();
            hideKeyBoard();

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

    public void showToast(String message) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        mToast.show();
    }

    @Override
    public void onResponse(Object response, WebServices.ApiType URL, boolean isSucces, int responseCode) {
        switch (URL) {
            case userlogin:
                if (mProgressbar.isShown()) {
                    mProgressbar.setVisibility(View.GONE);
                }
                UserLoginResponse userLoginResponse = (UserLoginResponse) response;
                if (isSucces) {
                    if (responseCode != 0) {
                        if (responseCode == 200) {
                            if (userLoginResponse != null) {
                                if (userLoginResponse.getEmployeeId() != null && userLoginResponse.getEmployeeName() != null && userLoginResponse.getError() == null) {
                                    saveLogInSession(userLoginResponse);
                                } else {
                                    showSnackBar(this, userLoginResponse.getError() + "");
                                }

                            } else {
                                Toast.makeText(this, "No response fron server", Toast.LENGTH_SHORT).show();
                            }

                        } else if (responseCode == 404) {
                            showSnackBar(this, responseCode + " Something went wrong please try again");

                        }


                    }


                } else {
                    //API call failed
                    Toast.makeText(this, "Something went wrong please try again", Toast.LENGTH_SHORT).show();
                }
        }

    }

    private void gotoHomeActivity() {

        Intent intent = new Intent(this, HomeActivity.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();

    }

    private void saveLogInSession(UserLoginResponse userLoginResponse) {
        String employeeName = userLoginResponse.getEmployeeName();
        String employeeID = userLoginResponse.getEmployeeId();
        String employeeDepartment = userLoginResponse.getDeptName();
        String employeeValueStream = userLoginResponse.getValueStream();
        String employeelineID = userLoginResponse.getLineId();
        String employeeDesignation = userLoginResponse.getDesignation();
        String ntUserID=userLoginResponse.getNtuserId();


        SharedPreferences preferences = getSharedPreferences(LOGIN_PREFERENCE, MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        if (preferences.contains("IS_LOGGEDIN")) {
            Log.d("savingvalues","inside preferences if block");
            editor.clear();
            editor.apply();
        }
        editor.putBoolean("IS_LOGGEDIN", true);
        editor.putString("EMPLOYEE_NAME", employeeName);
        editor.putString("EMPLOYEE_ID", employeeID);
        editor.putString("EMPLOYEE_DEPARTMENT", employeeDepartment);
        editor.putString("EMPLOYEE_VALUESTREAM", employeeValueStream);
        editor.putString("EMPLOYEE_LINEID", employeelineID);
        editor.putString("EMPLOYEE_DESIGNATION", employeeDesignation);
        editor.putString("NT_USERID", ntUserID);
        editor.apply();

        SharedPreferences devicePreferences=getSharedPreferences("DEVICE_PREFERENCES",MODE_PRIVATE);
        SharedPreferences.Editor editor1=devicePreferences.edit();
        if(devicePreferences.contains("PUSH_URL"))
        {
            Log.d("savingvalues","inside devicePreferences if block");
            editor1.clear();
            editor1.apply();
        }

        Log.d("savingvalues","imei=>"+DEVICE_UNIQUE_NUMBER+" ip=>"+DEVICE_IPADDRESS+" ntuid=>"+ntUserID+" empname=>"+employeeName);

        String pushDetailsURL= "http://"+ipAddress+":8080/AndonWebservices/rest/userinfo/"+employeeName+"/"+ntUserID+"/"+DEVICE_UNIQUE_NUMBER+"/"+DEVICE_IPADDRESS;

        //editor1.putString("IMEI_NUMBER",DEVICE_UNIQUE_NUMBER);
        //editor1.putString("IP_ADDRESS",DEVICE_IPADDRESS);
        //editor1.putString("NT_USERID",ntUserID);
        //editor1.putString("USER_NAME",employeeName);
        editor1.putString("PUSH_URL",pushDetailsURL);
        editor1.apply();

        gotoHomeActivity();


    }

    private void showSnackBar(Context context, String message) {
        Activity activity = (Activity) context;
        if (snackbar != null) {
            snackbar.dismiss();
        }
        snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        View view = snackbar.getView();
        view.setBackgroundColor(Color.BLACK);
        TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16);
        snackbar.show();
    }

    @Override
    public void onBackPressed() {
        finish();
        finishAffinity();
        //super.onBackPressed();
    }


    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the unique identifier for the device
     *
     * @return unique identifier for the device
     */
   /* public String getDeviceIMEI() {
        String deviceUniqueIdentifier = null;
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (null != tm) {
            deviceUniqueIdentifier = tm.getDeviceId();
        }
        if (null == deviceUniqueIdentifier || 0 == deviceUniqueIdentifier.length()) {
            deviceUniqueIdentifier = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return deviceUniqueIdentifier;
    }*/

    @SuppressLint("HardwareIds")
    public void loadIMEI() {
        // Check if the READ_PHONE_STATE permission is already available.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {
//                get_imei_data();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE},
                        MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
            }
        } else {

            TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            if (null != tm) {
                DEVICE_UNIQUE_NUMBER = tm.getDeviceId();
            }
            if (null == DEVICE_UNIQUE_NUMBER || 0 == DEVICE_UNIQUE_NUMBER.length()) {
                DEVICE_UNIQUE_NUMBER = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
           // Log.d("devive details","IMEI/UNIQUEID=>"+DEVICE_UNIQUE_NUMBER+"IP address=>"+DEVICE_IPADDRESS);
        }
    }


    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    @SuppressLint({"MissingPermission", "HardwareIds"})
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ALL) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED )
            {
               //Location permission granted

            }
            else {
                //finish();
                //recreate();
            }
        }
        else if (requestCode == MY_PERMISSIONS_REQUEST_READ_PHONE_STATE) {

            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
                if (null != tm) {
                    DEVICE_UNIQUE_NUMBER = tm.getDeviceId();
                }
                if (null == DEVICE_UNIQUE_NUMBER || 0 == DEVICE_UNIQUE_NUMBER.length()) {
                    DEVICE_UNIQUE_NUMBER = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
                }

            }
        }
    }
}

