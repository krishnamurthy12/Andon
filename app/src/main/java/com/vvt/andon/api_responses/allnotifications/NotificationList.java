
package com.vvt.andon.api_responses.allnotifications;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vvt.andon.activities.HomeActivity;

public class NotificationList {

    @SerializedName("acceptedBy")
    @Expose
    private String acceptedBy;
    @SerializedName("createdBy")
    @Expose
    private String createdBy;
    @SerializedName("createdDate")
    @Expose
    private String createdDate;
    @SerializedName("error")
    @Expose
    private String error;
    @SerializedName("imageLink")
    @Expose
    private String imageLink;
    @SerializedName("lineCode")
    @Expose
    private String lineCode;
    @SerializedName("notificationId")
    @Expose
    private Integer notificationId;
    @SerializedName("notificationStatus")
    @Expose
    private String notificationStatus;
    @SerializedName("station")
    @Expose
    private String station;
    @SerializedName("team")
    @Expose
    private String team;

    public String getAcceptedBy() {
        return acceptedBy;
    }

    public void setAcceptedBy(String acceptedBy) {
        this.acceptedBy = acceptedBy;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public String getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(String notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

}
