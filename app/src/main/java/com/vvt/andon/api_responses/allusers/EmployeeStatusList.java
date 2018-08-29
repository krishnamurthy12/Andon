
package com.vvt.andon.api_responses.allusers;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vvt.andon.activities.HomeActivity;

public class EmployeeStatusList {

    private String ipaddress= HomeActivity.ipAddress;

    @SerializedName("deptName")
    @Expose
    private Object deptName;
    @SerializedName("designation")
    @Expose
    private String designation;
    @SerializedName("employeeId")
    @Expose
    private String employeeId;
    @SerializedName("employeeName")
    @Expose
    private String employeeName;
    @SerializedName("error")
    @Expose
    private Object error;
    @SerializedName("imageUrl")
    @Expose
    private String imageUrl;
    @SerializedName("loginstatus")
    @Expose
    private String loginstatus;
    @SerializedName("mailId")
    @Expose
    private String mailId;
    @SerializedName("ntuserId")
    @Expose
    private String ntuserId;
    @SerializedName("slno")
    @Expose
    private Integer slno;

    public Object getDeptName() {
        return deptName;
    }

    public void setDeptName(Object deptName) {
        this.deptName = deptName;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public String getImageUrl() {
        //return imageUrl;
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        String url="http://"+ipaddress+":8080/AndonWebservices/";
        this.imageUrl = url+imageUrl;
    }

    public String getLoginstatus() {
        return loginstatus;
    }

    public void setLoginstatus(String loginstatus) {
        this.loginstatus = loginstatus;
    }

    public String getMailId() {
        return mailId;
    }

    public void setMailId(String mailId) {
        this.mailId = mailId;
    }

    public String getNtuserId() {
        return ntuserId;
    }

    public void setNtuserId(String ntuserId) {
        this.ntuserId = ntuserId;
    }

    public Integer getSlno() {
        return slno;
    }

    public void setSlno(Integer slno) {
        this.slno = slno;
    }

}
