package com.vvt.andon.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class AndonUtils {

    public static String IP_ADDRESS_PREFERENCE = "IPADDRESS_SHARED_PREFERENCE";
    public static String filename="AndonTextFile.txt";
    public static File directoryPath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS);

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

    public static void saveIPtoExternalDirectory(Context context,String textToWrite)
    {
        try {
            if(directoryPath.exists())
            {
                boolean isdeleted=directoryPath.delete();

                boolean isCreated=directoryPath.mkdirs();
                Log.d("creatingdeleting","Is deleted ?=?"+isdeleted+" is created ?=>"+isCreated);
            }
            else {
                boolean isCreated=directoryPath.mkdirs();
                Log.d("creatingdeleting",isCreated+"");
            }

            File myFile;
            myFile= new File(directoryPath, filename);
            if(myFile.exists())
            {
                myFile.delete();
                myFile = new File(directoryPath, filename);
            }
            FileOutputStream fOut = new FileOutputStream(myFile,true);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

            // to clear previous text
            //myOutWriter.write("");
            //myOutWriter.flush();

            myOutWriter.append(textToWrite);
            myOutWriter.close();
            fOut.close();

            Toast.makeText(context,"Text file Saved !",Toast.LENGTH_LONG).show();
        }

        catch (java.io.IOException e) {

            Log.d("writingtextintofile",e.getMessage());
            //do something if an IOException occurs.
            Toast.makeText(context,"ERROR - Text could't be added",Toast.LENGTH_LONG).show();
        }
    }
    public static String getIPFromExternalDirectory(Context context)
    {
         //Get the text file
        File file = new File(directoryPath,filename);
        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
            return text.toString();

        } catch (IOException e) {
            return null;
            //You'll need to add proper error handling here
        }
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public File getPublicAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            /*Log.e(LOG_TAG, "Directory not created");*/
        }
        return file;
    }
}
