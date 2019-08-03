/*
 * Created by Krishnamurthy T
 * Copyright (c) 2019 .  V V Technologies All rights reserved.
 * Last modified 16/7/19 2:51 PM
 */

package com.vvt.andon.utils;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Adapter;

import com.vvt.andon.activities.HomeActivity;
import com.vvt.andon.activities.LoginActivity;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Created by Krish on 08-03-2018.
 */

public class WebServices<T> {
    T t;
    Call<T> call=null;
    public T getT() {
        return t;
    }

    public void setT(T t) {

        this.t = t;
    }
    ApiType apiTypeVariable;
    private Context context;
    OnResponseListener<T> onResponseListner;
    private static OkHttpClient.Builder builder;

    public enum ApiType {

       userlogin,allNotifications,allAvailableUsers,acceptError,giveCA,getCAGiven,giveMOEComment,checklistConfirm,logOut

    }

    public static final String BASE_URL="http://"+HomeActivity.ipAddress+":8080/AndonWebservices/rest/";

    public WebServices(OnResponseListener<T> onResponseListner) {
        this.onResponseListner = onResponseListner;

        if (onResponseListner instanceof Activity) {
            this.context = (Context) onResponseListner;
        } else if (onResponseListner instanceof IntentService) {
            this.context = (Context) onResponseListner;
        } else if (onResponseListner instanceof android.app.DialogFragment) {
            android.app.DialogFragment dialogFragment = (android.app.DialogFragment) onResponseListner;
            this.context = dialogFragment.getActivity();
        }else if (onResponseListner instanceof android.app.Fragment) {
            android.app.Fragment fragment = (android.app.Fragment) onResponseListner;
            this.context = fragment.getActivity();
        }
         else if (onResponseListner instanceof Adapter) {

            this.context = (Context) onResponseListner;
        }
            else {
            android.support.v4.app.Fragment fragment = (android.support.v4.app.Fragment) onResponseListner;
            this.context = fragment.getActivity();
        }

        builder = getHttpClient();
    }

    public WebServices(Context context, OnResponseListener<T> onResponseListner) {
        this.onResponseListner = onResponseListner;
        this.context = context;
        builder = getHttpClient();
    }


    public OkHttpClient.Builder getHttpClient() {

        if (builder == null) {

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder client = new OkHttpClient.Builder();
            client.connectTimeout(10000, TimeUnit.SECONDS);
            client.readTimeout(10000, TimeUnit.SECONDS).build();
            client.addInterceptor(loggingInterceptor);

            return client;
        }
        return builder;
    }

    private Retrofit getRetrofitClient(String api)
    {
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl(api)
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }


    public void userLogIn(String api,ApiType apiTypes,String username,String userID)
    {
        apiTypeVariable = apiTypes;
        Retrofit retrofit=getRetrofitClient(api);

        AndonAPI andonAPI=retrofit.create(AndonAPI.class);

        call=(Call<T>)andonAPI.logIn(username,userID);
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                t=(T)response.body();
                onResponseListner.onResponse(t, apiTypeVariable, true,response.code());
                //Toast.makeText(context, "Success"+response.code(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                //Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show();
                onResponseListner.onResponse(null, apiTypeVariable, false, 0);

            }
        });
    }

    public void logOut(String api,ApiType apiTypes,String employeeId)
    {
        apiTypeVariable = apiTypes;
        Retrofit retrofit=getRetrofitClient(api);

        AndonAPI andonAPI=retrofit.create(AndonAPI.class);
        call=(Call<T>)andonAPI.logOut(employeeId);
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                t=(T)response.body();
                onResponseListner.onResponse(t, apiTypeVariable, true,response.code());
                //Toast.makeText(context, "Success"+response.code(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                //Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show();
                onResponseListner.onResponse(null, apiTypeVariable, false,0);

            }
        });
    }


    public void getAllNotifications(String api,ApiType apiTypes,String dept,String valueStream)
    {
        apiTypeVariable = apiTypes;
        Retrofit retrofit=getRetrofitClient(api);

        AndonAPI andonAPI=retrofit.create(AndonAPI.class);
        call=(Call<T>)andonAPI.getAllNotifications(dept,valueStream);
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                t=(T)response.body();
                onResponseListner.onResponse(t, apiTypeVariable, true,response.code());
                //Toast.makeText(context, "Success"+response.code(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                //Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show();
                onResponseListner.onResponse(null, apiTypeVariable, false, 0);

            }
        });
    }

    public void getAllUsers(String api,ApiType apiTypes,String dept,String valueStream)
    {
        apiTypeVariable = apiTypes;
        Retrofit retrofit=getRetrofitClient(api);

        AndonAPI andonAPI=retrofit.create(AndonAPI.class);
        call=(Call<T>)andonAPI.getAllCurrentUsers(dept,valueStream);
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                t=(T)response.body();
                onResponseListner.onResponse(t, apiTypeVariable, true,response.code());
                //Toast.makeText(context, "Success"+response.code(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                //Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show();
                onResponseListner.onResponse(null, apiTypeVariable, false, 0);

            }
        });
    }

    public void acceptError(String api,ApiType apiTypes,String notificationId,String employeeId,String team)
    {
        apiTypeVariable = apiTypes;
        Retrofit retrofit=getRetrofitClient(api);

        AndonAPI andonAPI=retrofit.create(AndonAPI.class);
        call=(Call<T>)andonAPI.notificationAccept(notificationId,employeeId,team);
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                t=(T)response.body();
                onResponseListner.onResponse(t, apiTypeVariable, true,response.code());
                //Toast.makeText(context, "Success"+response.code(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                //Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show();
                onResponseListner.onResponse(null, apiTypeVariable, false, 0);

            }
        });
    }


    public void giveCA(String api,ApiType apiTypes,String notificationId,String enteredMessage,String actionType,String employeeId,String team)
    {
        apiTypeVariable = apiTypes;
        Retrofit retrofit=getRetrofitClient(api);

        AndonAPI andonAPI=retrofit.create(AndonAPI.class);
        call=(Call<T>)andonAPI.giveCA(notificationId,enteredMessage,actionType,employeeId,team);
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                t=(T)response.body();
                onResponseListner.onResponse(t, apiTypeVariable, true,response.code());
                //Toast.makeText(context, "Success"+response.code(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                //Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show();
                onResponseListner.onResponse(null, apiTypeVariable, false, 0);

            }
        });
    }

    public void getCAGiven(String api,ApiType apiTypes,String notificationId,String team)
    {
        apiTypeVariable = apiTypes;
        Retrofit retrofit=getRetrofitClient(api);

        AndonAPI andonAPI=retrofit.create(AndonAPI.class);
        call=(Call<T>)andonAPI.getCAGiven(notificationId,team);
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                t=(T)response.body();
                onResponseListner.onResponse(t, apiTypeVariable, true,response.code());
                //Toast.makeText(context, "Success"+response.code(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                //Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show();
                onResponseListner.onResponse(null, apiTypeVariable, false, 0);

            }
        });
    }

    public void giveMOEComment(String api,ApiType apiTypes,String notificationId,String action,String employeeId,String team)
    {
        apiTypeVariable = apiTypes;
        Retrofit retrofit=getRetrofitClient(api);

        AndonAPI andonAPI=retrofit.create(AndonAPI.class);
        call=(Call<T>)andonAPI.giveMOEComment(notificationId,action,employeeId,team);
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                t=(T)response.body();
                onResponseListner.onResponse(t, apiTypeVariable, true,response.code());
                //Toast.makeText(context, "Success"+response.code(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                //Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show();
                onResponseListner.onResponse(null, apiTypeVariable, false, 0);

            }
        });
    }

    public void checkList(String api,ApiType apiTypes,String notificationId,String response,String employeeId)
    {
        apiTypeVariable = apiTypes;
        Retrofit retrofit=getRetrofitClient(api);

        AndonAPI andonAPI=retrofit.create(AndonAPI.class);
        call=(Call<T>)andonAPI.confirmCheckList(notificationId,response,employeeId);
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                t=(T)response.body();
                onResponseListner.onResponse(t, apiTypeVariable, true,response.code());
                //Toast.makeText(context, "Success"+response.code(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                //Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show();
                onResponseListner.onResponse(null, apiTypeVariable, false, 0);

            }
        });
    }


}
