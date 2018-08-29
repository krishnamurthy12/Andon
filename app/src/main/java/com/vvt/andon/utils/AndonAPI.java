package com.vvt.andon.utils;

import com.vvt.andon.api_responses.allnotifications.AllNotificationsResponse;
import com.vvt.andon.api_responses.allusers.AllAvailableUsersResponse;
import com.vvt.andon.api_responses.login.UserLoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AndonAPI {


    @GET("loginprocess/{ntuserId}/{employeeId}")
    Call<UserLoginResponse> logIn(@Path("ntuserId")String ntuserId,@Path("employeeId")String employeeID);

    /*@GET("loginprocess/{employeeId}")
    Call<UserLoginResponse> logOut(@Path("employeeId")String employeeID);*/

    @GET("notification/list/{dept}/{valueStream}")
    Call<AllNotificationsResponse> getAllNotifications(@Path("dept")String department, @Path("valueStream")String valueStream);

    @GET("loginstatus/status/{dept}/{valueStream}")
    Call<AllAvailableUsersResponse> getAllCurrentUsers(@Path("dept")String department, @Path("valueStream")String valueStream);

    @GET("notification/accept/{notificationId}/{employeeId}/{team}")
    Call<UserLoginResponse> notificationAccept(@Path("notificationId")String notificationID,@Path("employeeId")String employeeID,@Path("team") String team);


}
