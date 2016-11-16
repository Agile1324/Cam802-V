package com.linkcard.cam802;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) 
		{ 
			Intent activityStart = new Intent(context , MainActivity.class);
			activityStart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(activityStart);  
			Log.v("TAG", "开机自动服务自动启动.....");  
	    }
	}

}
