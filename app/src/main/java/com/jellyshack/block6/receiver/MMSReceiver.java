package com.jellyshack.block6.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.util.Log;

/**
 * MMS functionality just passes messages through to a notification.
 */
public class MMSReceiver extends BroadcastReceiver {
	public static final String MMS_RECEIVED = Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals(MMS_RECEIVED)) {
			Log.d("MMS", "MMS received.");
		}
	}
}
