/*
 * Created by Krishnamurthy T
 * Copyright (c) 2019 .  V V Technologies All rights reserved.
 * Last modified 27/6/18 1:09 PM
 */

package com.vvt.andon.utils;

/**
 * Created by Shashi on 10/9/2017.
 */

public interface OnResponseListener<T> {

    void onResponse(T response, WebServices.ApiType URL, boolean isSucces, int code);

}