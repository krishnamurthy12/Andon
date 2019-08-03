/*
 * Created by Krishnamurthy T
 * Copyright (c) 2019 .  V V Technologies All rights reserved.
 * Last modified 31/10/18 8:05 PM
 */

package com.vvt.andon.mqtt;



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.vvt.andon.activities.LoginActivity;

public class BootReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		//context.startActivity(new Intent(context,LoginActivity.class));

		Intent startIntent = context
				.getPackageManager()
				.getLaunchIntentForPackage(context.getPackageName());

		startIntent.setFlags(
				Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
						Intent.FLAG_ACTIVITY_NEW_TASK |
						Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
		);
		context.startActivity(startIntent);
	}
}