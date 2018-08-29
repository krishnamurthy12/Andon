
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
