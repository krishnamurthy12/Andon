package com.vvt.andon.utils;

import com.vvt.andon.api_responses.allnotifications.AllNotificationsResponse;
import com.vvt.andon.api_responses.allusers.AllAvailableUsersResponse;
import com.vvt.andon.api_responses.general.InteractionResponse;
import com.vvt.andon.api_responses.login.UserLoginResponse;
import com.vvt.andon.api_responses.logout.LogOutResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AndonAPI {


    @GET("loginprocess/{ntuserId}/{employeeId}")
    Call<UserLoginResponse> logIn(@Path("ntuserId")String ntuserId,@Path("employeeId")String employeeID);

    @GET("loginprocess/{employeeId}")
    Call<LogOutResponse> logOut(@Path("employeeId")String employeeID);

    @GET("notification/list/{dept}/{valueStream}")
    Call<AllNotificationsResponse> getAllNotifications(@Path("dept")String department, @Path("valueStream")String valueStream);

    @GET("loginstatus/status/{dept}/{valueStream}")
    Call<AllAvailableUsersResponse> getAllCurrentUsers(@Path("dept")String department, @Path("valueStream")String valueStream);



    @GET("notification/accept/{notificationId}/{employeeId}/{team}")
    Call<InteractionResponse> notificationAccept(@Path("notificationId")String notificationID, @Path("employeeId")String employeeID, @Path("team") String team);

   /* @GET("action/{notificationId}/{action}/{actionType}/{employeeId}/{team}")
    Call<InteractionResponse> giveCA(@Path("notificationId")String notificationId,
                                   @Path("action")String action,
                                     @Path("actionType")String actionType,
                                   @Path("employeeId") String employeeId,
                                   @Path("team") String team);*/
   @GET("action/{notificationId}/{action}/{employeeId}/{team}")
   Call<InteractionResponse> giveCA(@Path("notificationId")String notificationId,
                                    @Path("action")String action,
                                    @Path("employeeId") String employeeId,
                                    @Path("team") String team);

    @GET("action/{notificationId}/{team}")
    Call<InteractionResponse> getCAGiven(@Path("notificationId")String notificationID,@Path("team") String team);

    @GET("action/closeIssue/{notificationId}/{comment}/{employeeId}/{team}")
    Call<InteractionResponse> giveMOEComment(@Path("notificationId")String notificationId,
                                           @Path("comment")String action,
                                           @Path("employeeId")String employeeId,
                                           @Path("team") String team);

    @GET("action/checklist/{notificationId}/{checklist}/{employeeId}")
    Call<InteractionResponse> confirmCheckList(@Path("notificationId")String notificationId,@Path("checklist")String response,@Path("employeeId") String employeeId);

}
