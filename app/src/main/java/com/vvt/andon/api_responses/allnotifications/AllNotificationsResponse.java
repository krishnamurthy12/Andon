
/*
 * Created by Krishnamurthy T
 * Copyright (c) 2019 .  V V Technologies All rights reserved.
 * Last modified 27/6/18 1:09 PM
 */

package com.vvt.andon.api_responses.allnotifications;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AllNotificationsResponse {


    @SerializedName("notificationList")
    @Expose
    private List<NotificationList> notificationList = null;

    public List<NotificationList> getNotificationList() {
        return notificationList;
    }

    public void setNotificationList(List<NotificationList> notificationList) {
        this.notificationList = notificationList;
    }

}
