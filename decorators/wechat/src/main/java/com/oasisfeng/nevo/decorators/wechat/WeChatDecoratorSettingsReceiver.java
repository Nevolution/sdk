package com.oasisfeng.nevo.decorators.wechat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by Oasis on 2018/4/26.
 */
public class WeChatDecoratorSettingsReceiver extends BroadcastReceiver {

	@Override public void onReceive(final Context context, final Intent intent) {
		Toast.makeText(context, "Settings still under construction", Toast.LENGTH_LONG).show();
	}
}
