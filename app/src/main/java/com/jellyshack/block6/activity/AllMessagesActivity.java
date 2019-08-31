package com.jellyshack.block6.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.View;
import android.widget.AdapterView;
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
import com.jellyshack.block6.util.SmsUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AllMessagesActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_all_messages);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ListView smsItemList = findViewById(R.id.smsItemList);
		SMSListAdapter adapter = new SMSListAdapter(this, true);
		smsItemList.setAdapter(adapter);

		setSmsItemClickListener(smsItemList);
	}

	@Override
	protected void onResume() {
		super.onResume();

		loadMessages();
	}

	private void setSmsItemClickListener(ListView smsItemList) {
		smsItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// The view is the list item that was clicked.
				TextView smsAddress = view.findViewById(R.id.smsAddress);
				String address = smsAddress.getText().toString();

				startComposeActivity(address);
			}
		});
	}

	private void loadMessages() {
		ListView smsItemList = findViewById(R.id.smsItemList);
		SMSListAdapter adapter = (SMSListAdapter)smsItemList.getAdapter();
		adapter.clear();

		List<SimpleSMSMessage> messages = SmsUtil.getTopMessages(this);

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
		});

		// Load messages into list adapter.
		if(!messages.isEmpty()) {
			adapter.loadMessages(messages);
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

	private void startComposeActivity(String address) {
		// Check if this number is blocked.
		AsyncTask.execute(() -> {
			if(DB.getInstance(this).numberDAO().getByNumber(address) == null) {
				// Number is not blocked.
				runOnUiThread(() -> {
					Intent intent = new Intent(this, ComposeSMSActivity.class);
					intent.putExtra(Telephony.Sms.ADDRESS, address);

					startActivity(intent);
				});
			} else {
				runOnUiThread(() -> {
					// Number is blocked.
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
}
