/*
 * Created by Krishnamurthy T
 * Copyright (c) 2019 .  V V Technologies All rights reserved.
 * Last modified 13/10/18 9:46 PM
 */

package com.vvt.andon.api_responses.general;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class InteractionResponse {

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
