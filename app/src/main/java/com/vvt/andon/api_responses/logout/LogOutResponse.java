package com.vvt.andon.api_responses.logout;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LogOutResponse {

    @SerializedName("message")
    @Expose
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
