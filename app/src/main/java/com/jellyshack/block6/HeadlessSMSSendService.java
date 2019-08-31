package com.jellyshack.block6;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

/**
 * See: https://developer.android.com/reference/android/telephony/TelephonyManager.html#ACTION_RESPOND_VIA_MESSAGE
 */
public class HeadlessSMSSendService extends IntentService {
	public HeadlessSMSSendService() {
		super(HeadlessSMSSendService.class.getName());

		this.setIntentRedelivery(true);
	}

	@Override
	public void onHandleIntent(Intent intent) {
		if(!intent.getAction().equals(TelephonyManager.ACTION_RESPOND_VIA_MESSAGE)) {
			return;
		}

		Bundle extras = intent.getExtras();

		if(extras == null) {
			return;
		}

		/**
		 * The intent contains a URI (available from Intent.getData()) describing the recipient,
		 * using either the sms:, smsto:, mms:, or mmsto: URI schema.
		 * Each of these URI schema carry the recipient information the same way: the path part of
		 * the URI contains the recipient's phone number or a comma-separated set of phone numbers
		 * if there are multiple recipients.
		 * For example, smsto:2065551234.
		 */
		Uri recipientUri = intent.getData();
		String recipientList = recipientUri.getSchemeSpecificPart();
		String[] recipients = TextUtils.split(recipientList, ",");
		String message = extras.getString(Intent.EXTRA_TEXT, "Message: None");
		String subject = extras.getString(Intent.EXTRA_SUBJECT, "Subject: None");

		SmsManager smsManager = SmsManager.getDefault();
		ContentResolver contentResolver = getContentResolver();
		Uri smsSentUri = Uri.parse("content://sms/sent");

		for(String recipient : recipients) {
			smsManager.sendTextMessage(recipient, null, message, null, null);

			ContentValues values = new ContentValues();
			values.put("address", recipient);
			values.put("body", message);

			contentResolver.insert(smsSentUri, values);
		}
	}
}
