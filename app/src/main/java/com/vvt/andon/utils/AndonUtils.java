package com.vvt.andon.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.List;

public class AndonUtils {

    public static String IP_ADDRESS_PREFERENCE = "IPADDRESS_SHARED_PREFERENCE";

    public static boolean isConnectedToInternet(Context con){
        ConnectivityManager connectivity = (ConnectivityManager) con.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null){
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (NetworkInfo anInfo : info)
                    if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }

    /*This is to identify whether the app is currently  running or closed*/
    public static boolean isAppRunning(final Context context, final String packageName) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        if (procInfos != null)
        {
            for (final ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
                if (processInfo.processName.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }


    /*For saving IP Address
     * -------------------------------------------------------------------------------------------------------------------
     * */
    public static void saveIPAddressPreference(Context context,String ipAddress)
    {
        SharedPreferences sharedPreferences=context.getSharedPreferences(IP_ADDRESS_PREFERENCE,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();

       /* String ipaddress=sharedPreferences.getString("IPADDRESS",null);
        if(ipaddress!=null)*/
        if(sharedPreferences.contains("IPADDRESS"))
        {
            editor.clear();
            editor.apply();
        }
        editor.putString("IPADDRESS",ipAddress);
        editor.apply();
    }


    public static String getIPAddress(Context context)
    {
        SharedPreferences sharedPreferences=context.getSharedPreferences(IP_ADDRESS_PREFERENCE,Context.MODE_PRIVATE);
        return sharedPreferences.getString("IPADDRESS",null);

    }
}
