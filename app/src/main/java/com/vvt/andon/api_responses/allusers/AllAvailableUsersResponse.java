
/*
 * Created by Krishnamurthy T
 * Copyright (c) 2019 .  V V Technologies All rights reserved.
 * Last modified 27/6/18 1:09 PM
 */

package com.vvt.andon.api_responses.allusers;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AllAvailableUsersResponse {

    @SerializedName("employeeStatusList")
    @Expose
    private List<EmployeeStatusList> employeeStatusList = null;

    public List<EmployeeStatusList> getEmployeeStatusList() {
        return employeeStatusList;
    }

    public void setEmployeeStatusList(List<EmployeeStatusList> employeeStatusList) {
        this.employeeStatusList = employeeStatusList;
    }

}
