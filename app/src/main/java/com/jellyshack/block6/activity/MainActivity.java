package com.jellyshack.block6.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements Observer {
	private static int CHOOSE_CONTACT_CALLBACK_ID = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		EditText directPhoneNumberBox = findViewById(R.id.directPhoneNumberBox);

		directPhoneNumberBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_DONE) {
					directPhoneNumberSelected(v);
				}

				return false;
			}
		});

		boolean showMessageAddress = true;
		boolean highlightUnread = true;

		ListView smsItemList = findViewById(R.id.smsItemList);
		SMSListAdapter adapter = new SMSListAdapter(this, showMessageAddress, highlightUnread);
		smsItemList.setAdapter(adapter);

		setSmsItemClickListener(smsItemList);

		// Register as an observer if the SMSReceiver gets an incoming message.
		SmsObservable.getInstance().addObserver(this);
	}

	/**
	 * onResume is a good place to load data and setup the UI. For example, if the
	 * user leaves this activity and goes to the settings activity, clicks "reset data" then
	 * presses the back button, onResume will be called on this activity and we can reload
	 * from the database and setup the UI with the default settings.
	 *
	 * See the activity lifecycle:
	 * https://developer.android.com/guide/components/activities/activity-lifecycle
	 */
	@Override
	protected void onResume() {
		super.onResume();

		// Check if this is the default SMS app.
		if(checkIfDefaultSMSApp()) {
			// Load unread messages.
			loadTopMessages();
		}
	}

	public void settingsButtonClick(View v) {
		Intent settings = new Intent(this, SettingsActivity.class);
		startActivity(settings);
	}

	private void setSmsItemClickListener(ListView smsItemList) {
		smsItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// The view is the list item that was clicked.
				TextView smsAddress = (TextView)view.findViewById(R.id.smsAddress);
				String address = smsAddress.getText().toString();

				startComposeActivity(address);
			}
		});
	}

	private void loadTopMessages() {
		ListView smsItemList = findViewById(R.id.smsItemList);
		SMSListAdapter adapter = (SMSListAdapter)smsItemList.getAdapter();
		adapter.clear();

		int limitDays = 30;
		List<SimpleSMSMessage> messages = SmsUtil.getTopMessages(this, limitDays);

		// Filter out messages from blocked numbers.
		AsyncTask.execute(() -> {
			List<Number> blockedNumbers = DB.getInstance(this).numberDAO().getAll();
			List<String> blocked = new ArrayList<>();

			for(Number number : blockedNumbers) {
				blocked.add(number.getNumber());
			}

			Iterator<SimpleSMSMessage> iterator = messages.iterator();
			while(iterator.hasNext()) {
				SimpleSMSMessage sms = iterator.next();
				String address = PhoneNumber.normalizeNumber(sms.get(Telephony.Sms.ADDRESS));

				if(blocked.contains(address)) {
					iterator.remove();
				}
			}

			runOnUiThread(() -> {
				// Load messages into list adapter.
				if(!messages.isEmpty()) {
					adapter.loadMessages(messages);
				}

				showOrHideMessages();
			});
		});
	}

	private void showOrHideMessages() {
		View smsItemListHolder = findViewById(R.id.smsItemListHolder);
		View centerImageHolder = findViewById(R.id.centerImageHolder);
		ListView smsItemList = findViewById(R.id.smsItemList);

		if(smsItemList.getAdapter().getCount() == 0) {
			smsItemListHolder.setVisibility(View.GONE);
			centerImageHolder.setVisibility(View.VISIBLE);
		} else {
			centerImageHolder.setVisibility(View.GONE);
			smsItemListHolder.setVisibility(View.VISIBLE);
		}
	}

	public void chooseContactButtonClick(View v) {
		v.playSoundEffect(android.view.SoundEffectConstants.CLICK);

		/**
		 * Display user's contacts so they can choose one.
		 * See: https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Phone.html
		 */
		Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
		startActivityForResult(intent, CHOOSE_CONTACT_CALLBACK_ID);
	}

	public void directPhoneNumberSelected(View v) {
		v.playSoundEffect(android.view.SoundEffectConstants.CLICK);

		EditText phoneNumberBox = findViewById(R.id.directPhoneNumberBox);

		// Close the soft input keyboard.
		phoneNumberBox.clearFocus();
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(phoneNumberBox.getWindowToken(), 0);

		String address = phoneNumberBox.getText().toString().trim();

		if(TextUtils.isEmpty(address)) {
			return;
		}

		phoneNumberBox.getText().clear();

		startComposeActivity(address);
	}

	public void allMessagesButtonClick(View v) {
		v.playSoundEffect(android.view.SoundEffectConstants.CLICK);

		Intent intent = new Intent(this, AllMessagesActivity.class);
		startActivity(intent);
	}

	private boolean checkIfDefaultSMSApp() {
		if(!Telephony.Sms.getDefaultSmsPackage(this).equals(this.getPackageName())) {
			// Not the default SMS app.
			findViewById(R.id.defaultSMSAppDialog).setVisibility(View.VISIBLE);
			findViewById(R.id.centerImageHolder).setVisibility(View.VISIBLE);
			findViewById(R.id.smsItemListHolder).setVisibility(View.GONE);

			return false;
		} else {
			// This is the active SMS app.
			findViewById(R.id.smsItemListHolder).setVisibility(View.VISIBLE);
			findViewById(R.id.defaultSMSAppDialog).setVisibility(View.GONE);
			findViewById(R.id.centerImageHolder).setVisibility(View.GONE);

			return true;
		}
	}

	public void makeDefaultSMSAppButtonClick(View v) {
		Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
		intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, this.getPackageName());

		startActivity(intent);
	}

	/**
	 * Callback that runs when the startActivityForResult intent finishes.
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == CHOOSE_CONTACT_CALLBACK_ID) {
			if(resultCode == Activity.RESULT_OK) {
				String[] projection = {
						ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
						ContactsContract.CommonDataKinds.Phone.NUMBER,
						ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
				};

				try(Cursor cursor = getContentResolver().query(data.getData(), projection, null, null, null)) {
					if (cursor != null) {
						if (cursor.moveToFirst()) {
							int displayNameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
							int numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
							int normalizedNumberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);

							String displayName = cursor.getString(displayNameIdx);
							String number = cursor.getString(numberIdx);
							String normalizedNumber = cursor.getString(normalizedNumberIdx);

							startComposeActivity(normalizedNumber != null ? normalizedNumber : number);
						} else {
							// No rows.
							Log.d("data", "No data.");
						}
					}
				}
			}
		}
	}

	private void startComposeActivity(String address) {
		// Check if this number is blocked.
		AsyncTask.execute(() -> {
			if(DB.getInstance(this).numberDAO().getByNumber(PhoneNumber.normalizeNumber(address)) == null) {
				// Number is not blocked.
				runOnUiThread(() -> {
					Intent intent = new Intent(this, ComposeSMSActivity.class);
					intent.putExtra(Telephony.Sms.ADDRESS, address);

					startActivity(intent);
				});
			} else {
				// Number is blocked.
				runOnUiThread(() -> {
					AlertDialog alert = new AlertDialog.Builder(this)
							.setTitle("Blocked Number")
							.setMessage("This number is blocked.")
							.setPositiveButton("OK", (dialog, id) -> {
								// Do nothing.
							})
							.create();

					alert.show();
				});
			}
		});
	}

	/**
	 * Called when the SMSReceiver gets a new SMS message.
	 * @param smsObservable
	 * @param data
	 */
	@Override
	public void update(Observable smsObservable, Object data) {
		SmsObservable.NewMessageMarker marker = (SmsObservable.NewMessageMarker) data;

		List<SimpleSMSMessage> messages = SmsUtil.getMessagesFromNumber(this, marker.address, marker.ts);

		/**
		 * The UI can only be updated from the UI thread, and the update() method was called
		 * from the BroadcastReceiver on a separate thread.
		 */
		runOnUiThread(() -> {
			boolean append = false; // Add message to beginning of list.

			ListView smsItemList = findViewById(R.id.smsItemList);
			((SMSListAdapter) smsItemList.getAdapter()).addMessages(messages, append);

			showOrHideMessages();
		});
	}
}
