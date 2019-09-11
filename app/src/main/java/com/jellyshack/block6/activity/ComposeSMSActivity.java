package com.jellyshack.block6.activity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.jellyshack.block6.R;
import com.jellyshack.block6.SMSListAdapter;
import com.jellyshack.block6.data.DB;
import com.jellyshack.block6.data.Number;
import com.jellyshack.block6.util.PhoneNumber;
import com.jellyshack.block6.util.SimpleSMSMessage;
import com.jellyshack.block6.util.SmsObservable;
import com.jellyshack.block6.util.SmsUtil;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class ComposeSMSActivity extends AppCompatActivity implements Observer {
	private boolean activeView = false;
	private String normalizedNumber;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_compose_sms);

		// Get data from the intent that started this activity.
		Intent intent = getIntent();

		if(intent.getAction().equals(Intent.ACTION_SENDTO) && intent.getData().getScheme().equals("smsto")) {
			// Compose activity started from an smsto intent. Parse out the phone number.
			this.normalizedNumber = PhoneNumber.normalizeNumber(intent.getData().getSchemeSpecificPart());

			// The intent could also have set the SMS body that it wants to be sent.
			if(intent.hasExtra("sms_body")) {
				String message = intent.getStringExtra("sms_body");

				EditText composeText = (EditText)findViewById(R.id.composeText);
				composeText.setText(message);
			}
		} else if(intent.hasExtra(Telephony.Sms.ADDRESS)) {
			this.normalizedNumber = PhoneNumber.normalizeNumber(intent.getStringExtra(Telephony.Sms.ADDRESS));
		} else {
			// Leave activity.
			this.finish();
			return;
		}

		String contactInfoText = this.normalizedNumber;

		TextView contactInfo = findViewById(R.id.contactInfo);
		contactInfo.setText(contactInfoText);

		boolean showMessageAddress = false;
		boolean highlightUnread = false;

		ListView smsItemList = findViewById(R.id.smsItemList);
		SMSListAdapter adapter = new SMSListAdapter(this, showMessageAddress, highlightUnread);
		smsItemList.setAdapter(adapter);

		// Load all SMS messages from this number.
		this.loadMessages();

		// Register as an observer if the SMSReceiver gets an incoming message.
		SmsObservable.getInstance().addObserver(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		this.activeView = true;
	}

	@Override
	protected void onPause() {
		super.onPause();

		this.activeView = false;
	}

	private void loadMessages() {
		long since = 0;
		List<SimpleSMSMessage> messages = SmsUtil.getMessagesFromNumber(this, this.normalizedNumber, since);

		if(!messages.isEmpty()) {
			// Mark all unread messages from this number as read.
			SmsUtil.markMessagesFromNumberAsRead(this, this.normalizedNumber);

			ListView smsItemList = findViewById(R.id.smsItemList);
			((SMSListAdapter)smsItemList.getAdapter()).loadMessages(messages);
		}

		showOrHideMessages();
	}

	private void showOrHideMessages() {
		View smsItemListHolder = findViewById(R.id.smsItemListHolder);
		View smsNoMessages = findViewById(R.id.smsNoMessages);
		ListView smsItemList = findViewById(R.id.smsItemList);

		if(smsItemList.getAdapter().getCount() == 0) {
			smsItemListHolder.setVisibility(View.GONE);
			smsNoMessages.setVisibility(View.VISIBLE);
		} else {
			smsNoMessages.setVisibility(View.GONE);
			smsItemListHolder.setVisibility(View.VISIBLE);
		}
	}

	public void sendMessageButtonClick(View v) {
		v.playSoundEffect(android.view.SoundEffectConstants.CLICK);

		EditText composeText = (EditText)findViewById(R.id.composeText);

		String recipient = this.normalizedNumber;
		String message = composeText.getText().toString();

		if(TextUtils.isEmpty(recipient) || TextUtils.isEmpty(message)) {
			return;
		}

		// Send SMS.
		SmsManager smsManager = SmsManager.getDefault();
		ContentResolver contentResolver = getContentResolver();

		smsManager.sendTextMessage(recipient, null, message, null, null);

		// Save in SMS sent table.
		ContentValues map = new ContentValues();
		map.put(Telephony.Sms.ADDRESS, recipient);
		map.put(Telephony.Sms.BODY, message);

		contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, map);

		composeText.getText().clear();

		// Add sent SMS to list view.
		SimpleSMSMessage sms = new SimpleSMSMessage();
		sms.put(Telephony.Sms._ID, "0"); // Fake value.
		sms.put(Telephony.Sms.TYPE, String.valueOf(Telephony.Sms.MESSAGE_TYPE_SENT));
		sms.put(Telephony.Sms.DATE, String.valueOf(System.currentTimeMillis()));
		sms.put(Telephony.Sms.ADDRESS, recipient);
		sms.put(Telephony.Sms.BODY, message);

		ListView smsItemList = findViewById(R.id.smsItemList);
		((SMSListAdapter) smsItemList.getAdapter()).addMessage(sms, true);

		showOrHideMessages();
	}

	public void blockNumberButtonClick(View v) {
		AlertDialog alert = new AlertDialog.Builder(this)
				.setTitle("Confirm")
				.setMessage("Are you sure you want to block this number?")
				.setPositiveButton("Yes", (dialog, id) -> {
					AsyncTask.execute(() -> {
						Number number = new Number();
						number.setNumber(this.normalizedNumber);
						number.setBlockedCount(0);

						long numberID = DB.getInstance(this).numberDAO().insert(number);
						number.setId(numberID);
					});

					Toast.makeText(this, "You have blocked " + this.normalizedNumber, Toast.LENGTH_LONG).show();

					// Exit this activity (go back).
					this.finish();
				})
				.setNegativeButton("Cancel", (dialog, id) -> {
					// Do nothing.
				})
				.create();

		alert.show();
	}

	/**
	 * Called when the SMSReceiver gets a new SMS message.
	 * @param smsObservable
	 * @param data
	 */
	@Override
	public void update(Observable smsObservable, Object data) {
		SmsObservable.NewMessageMarker marker = (SmsObservable.NewMessageMarker)data;

		if(marker.address.equals(this.normalizedNumber)) {
			// The new message was addressed to the number we're currently viewing.
			List<SimpleSMSMessage> messages = SmsUtil.getMessagesFromNumber(this, this.normalizedNumber, marker.ts);

			if(this.activeView) {
				// Mark all unread messages from this number as read.
				SmsUtil.markMessagesFromNumberAsRead(this, this.normalizedNumber);
			}

			/**
			 * The UI can only be updated from the UI thread, and the update() method was called
			 * from the BroadcastReceiver on a separate thread.
			 */
			runOnUiThread(() -> {
				ListView smsItemList = findViewById(R.id.smsItemList);
				((SMSListAdapter) smsItemList.getAdapter()).addMessages(messages, true);

				showOrHideMessages();
			});
		}
	}
}
