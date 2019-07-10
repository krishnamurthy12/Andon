package com.vvt.andon.activities;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.vvt.andon.BuildConfig;
import com.vvt.andon.R;
import com.vvt.andon.utils.AndonUtils;

import java.io.File;

public class DownloadActivity extends AppCompatActivity {

    private DownloadManager mgr=null;
    private long lastDownload=-1L;

    ProgressDialog progressDialog;

    @Override
    protected void onStart() {
        super.onStart();
        startDownload();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_demo);

        mgr=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        registerReceiver(onComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(onNotificationClick,
                new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(onComplete);
        unregisterReceiver(onNotificationClick);
    }

    public void startDownload() {
        //  Uri uri=Uri.parse("http://commonsware.com/misc/test.mp4");

        String ipAddress=AndonUtils.getIPAddress(this);

        Uri uri=Uri.parse("http://"+ipAddress+":8080/AndroidApk/Andon/ANDON.apk");

        //Uri uri=Uri.parse("https://apkpure.com/sample-android-app-test/com.sabithpkcmnr.admobintegration/download?from=details");

        Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .mkdirs();

        lastDownload=
                mgr.enqueue(new DownloadManager.Request(uri)
                        .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                                DownloadManager.Request.NETWORK_MOBILE)
                        .setAllowedOverRoaming(false)
                        .setTitle("Andon")
                        .setDescription("Downloading new update")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setVisibleInDownloadsUi(true)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                                "ANDON.apk"));

//        v.setEnabled(false);
        //findViewById(R.id.query).setEnabled(true);

        final int UPDATE_PROGRESS = 5020;

        progressDialog=new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Andon new Update Downloading...");
        progressDialog.show();

        final Handler handler = new Handler(getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what==UPDATE_PROGRESS){
                    String downloaded = String.format("%.2f MB", (double)((msg.arg1)/1024)/1024);
                    //String total = String.format("%.2f MB", (double) ((msg.arg2)/1024)/1024))
                    String total=String.format("%.2f MB",(double)(msg.arg2/1024)/1024);
                    String status = downloaded + " / " + total;
                    progressDialog.setMessage(status);
                }
                super.handleMessage(msg);
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean downloading = true;
                while (downloading) {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(lastDownload);
                    Cursor cursor = mgr.query(q);
                    cursor.moveToFirst();
                    int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false;
                    }
                    //Post message to UI Thread
                    Message msg = handler.obtainMessage();
                    msg.what = UPDATE_PROGRESS;
                    //msg.obj = statusMessage(cursor);
                    msg.arg1 = bytes_downloaded;
                    msg.arg2 = bytes_total;
                    handler.sendMessage(msg);
                    cursor.close();
                }
            }
        }).start();
    }

    private String statusMessage(Cursor c) {
        String msg="???";

        switch(c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_FAILED:
                msg="Download failed!";
                break;

            case DownloadManager.STATUS_PAUSED:
                msg="Download paused!";
                break;

            case DownloadManager.STATUS_PENDING:
                msg="Download pending!";
                break;

            case DownloadManager.STATUS_RUNNING:
                msg="Download in progress!";
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                msg="Download complete!";
                break;

            default:
                msg="Download is nowhere in sight";
                break;
        }

        return(msg);
    }

    BroadcastReceiver onComplete=new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {

            // findViewById(R.id.start).setEnabled(true);
            progressDialog.dismiss();
            //mgr.getUriForDownloadedFile(lastDownload);



            File toInstall = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), "ANDON.apk");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && (!getPackageManager().canRequestPackageInstalls()) )
            {
                Toast.makeText(ctxt, "Please allow this option to enable installation process", Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse(String.format("package:%s", getPackageName()))), 1234);
            }
            else {
                Toast.makeText(ctxt, "Please select install option to instal new update", Toast.LENGTH_SHORT).show();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri apkUri = FileProvider.getUriForFile(DownloadActivity.this, BuildConfig.APPLICATION_ID + ".fileprovider", toInstall);
                    Intent intent1 = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    intent1.setData(apkUri);
                    intent1.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent1);
                } else {
                    Uri apkUri = Uri.fromFile(toInstall);
                    Intent intent1 = new Intent(Intent.ACTION_VIEW);
                    intent1.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent1);
                }
            }

        }
    };

    BroadcastReceiver onNotificationClick=new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            //Toast.makeText(ctxt, "Ummmm...hi!", Toast.LENGTH_LONG).show();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==Activity.RESULT_OK)
        {
            if(requestCode==1234)
            {
                if (getPackageManager().canRequestPackageInstalls()) {

                    File toInstall = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ANDON.apk");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Uri apkUri = FileProvider.getUriForFile(DownloadActivity.this, BuildConfig.APPLICATION_ID +".fileprovider", toInstall);
                        Intent intent1 = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        intent1.setData(apkUri);
                        intent1.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent1);
                    } else {
                        Uri apkUri = Uri.fromFile(toInstall);
                        Intent intent1 = new Intent(Intent.ACTION_VIEW);
                        intent1.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent1);
                    }
                }
            }

        }
        else {
            //give the error
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse(String.format("package:%s", getPackageName()))), 1234);
        }
    }
}
