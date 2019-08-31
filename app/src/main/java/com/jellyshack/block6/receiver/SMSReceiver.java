package com.jellyshack.block6.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import androidx.room.Room;

import com.jellyshack.block6.data.DB;
import com.jellyshack.block6.data.Number;
import com.jellyshack.block6.notification.Notification;
import com.jellyshack.block6.util.PhoneNumber;
import com.jellyshack.block6.util.SmsObservable;

import java.util.List;

/**
 * See: https://android-developers.googleblog.com/2013/10/getting-your-sms-apps-ready-for-kitkat.html
 */
public class SMSReceiver extends BroadcastReceiver {
	public static final String SMS_DELIVER = Telephony.Sms.Intents.SMS_DELIVER_ACTION;
	public static final String SMS_RECEIVED = Telephony.Sms.Intents.SMS_RECEIVED_ACTION;

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals(SMS_DELIVER)) {
			/**
			 * Only the user's default SMS app will receive the SMS_DELIVER action,
			 * and only this app can abort the SMS message.
			 *
			 * We can't use AsyncTask.execute() directly in the BroadcastReceiver because the
			 * system could kill the process after onReceive() has finished. This is the
			 * recommended way to do it: https://developer.android.com/guide/components/broadcasts.html
			 */
			PendingResult pendingResult = goAsync();
			Task task = new Task(context, pendingResult, intent);
			task.execute();
		} else if(intent.getAction().equals(SMS_RECEIVED)) {
			/**
			 * This action is broadcast to all apps that want to receive it.
			 *
			 * Beginning with Android 4.4, attempting to abort the SMS_RECEIVED_ACTION broadcast
			 * will be ignored so all apps interested have the chance to receive it.
			 */
		}
	}

	private static class Task extends AsyncTask<String, Void, Void> {
		private Context context;
		private PendingResult pendingResult;
		private Intent intent;

		private Task(Context context, PendingResult pendingResult, Intent intent) {
			this.context = context;
			this.pendingResult = pendingResult;
			this.intent = intent;
		}

		/**
		 * This method will run in a separate thread.
		 * @param v
		 * @return
		 */
		@Override
		protected Void doInBackground(String... v) {
			handleSMS(context, intent);

			return null;
		}

		/**
		 * This method will be invoked on the main UI thread after the background process finishes.
		 * @param v
		 */
		@Override
		protected void onPostExecute(Void v) {
			super.onPostExecute(v);

			// Call finish so BroadcastReceiver can be recycled.
			pendingResult.finish();
		}

		private void handleSMS(Context context, Intent intent) {
			Bundle extras = intent.getExtras();

			if(extras == null) {
				return;
			}

			List<Number> blockedNumbers = DB.getInstance(context).numberDAO().getAll();

			Object[] pdus = (Object[])extras.get("pdus");

			for(int i = 0; i < pdus.length; i++) {
				SmsMessage sms = SmsMessage.createFromPdu((byte[])pdus[i]);
				String address = sms.getDisplayOriginatingAddress();
				String body = sms.getMessageBody();

				boolean blocked = false;

				for(Number number : blockedNumbers) {
					if(number.getNumber().equals(PhoneNumber.normalizeNumber(address))) {
						// Block the message.
						blocked = true;

						// Update blocked count.
						number.setBlockedCount(number.getBlockedCount() + 1);

						DB.getInstance(context).numberDAO().update(number);

						break;
					}
				}

				if(!blocked) {
					/**
					 * Get a marker prior to inserting the message into the SMS inbox.
					 * The marker's timestamp will be used to query for the message later.
					 */
					SmsObservable.NewMessageMarker marker = new SmsObservable.NewMessageMarker(address);

					// Save the message in the SMS inbox table.
					ContentValues map = new ContentValues();
					map.put(Telephony.Sms.ADDRESS, address);
					map.put(Telephony.Sms.BODY, body);

					context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, map);

					if(SmsObservable.getInstance().countObservers() == 0) {
						// Send user a notification.
						Notification.create(context, address, body);
					} else {
						// The app is running. Pass the SMS message to observers.
						SmsObservable.getInstance().update(marker);
					}
				} else {
					/**
					 * Abort the broadcast so no other apps receive it. Also note that we aren't
					 * storing the message at all. It will truly be blocked and inaccessible at
					 * a later date.
					 */
					pendingResult.abortBroadcast();
				}
			}
		}
	}
}
