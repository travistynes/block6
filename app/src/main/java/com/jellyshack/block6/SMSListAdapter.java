package com.jellyshack.block6;

import android.content.Context;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jellyshack.block6.util.SimpleSMSMessage;

import java.util.ArrayList;
import java.util.List;

public class SMSListAdapter extends BaseAdapter {
	private LayoutInflater layoutInflater;
	private List<SimpleSMSMessage> messages = new ArrayList<>();
	private boolean showMessageAddress;

	public SMSListAdapter(Context context, boolean showMessageAddress) {
		this.layoutInflater = LayoutInflater.from(context);
		this.showMessageAddress = showMessageAddress;
	}

	@Override
	public View getView(int position, View item, ViewGroup container) {
		if(item == null) {
			item = layoutInflater.inflate(R.layout.sms_item, container, false);
		}

		SimpleSMSMessage message = this.messages.get(position);

		int type = Integer.parseInt(message.get(Telephony.Sms.TYPE), 10);
		if(type == Telephony.Sms.MESSAGE_TYPE_SENT) {
			item.setBackgroundResource(R.drawable.sent_item_background);
		} else {
			item.setBackgroundResource(R.drawable.received_item_background);
		}

		TextView smsDate = (TextView)item.findViewById(R.id.smsDate);
		smsDate.setText(message.get(Telephony.Sms.DATE));

		TextView smsAddress = (TextView)item.findViewById(R.id.smsAddress);
		smsAddress.setText(message.get(Telephony.Sms.ADDRESS));

		TextView smsMessage = (TextView)item.findViewById(R.id.smsMessage);
		smsMessage.setText(message.get(Telephony.Sms.BODY));

		if(!showMessageAddress) {
			smsAddress.setVisibility(View.GONE);
		} else {
			smsAddress.setVisibility(View.VISIBLE);
		}

		return item;
	}

	public void loadMessages(List<SimpleSMSMessage> messages) {
		this.messages = messages;

		this.notifyDataSetChanged();
	}

	public void addMessages(List<SimpleSMSMessage> messages, boolean append) {
		if(append) {
			this.messages.addAll(messages);
		} else {
			this.messages.addAll(0, messages);
		}

		this.notifyDataSetChanged();
	}

	public void addMessage(SimpleSMSMessage message, boolean append) {
		if(append) {
			this.messages.add(message);
		} else {
			// Add to front of list.
			this.messages.add(0, message);
		}

		this.notifyDataSetChanged();
	}

	public void clear() {
		this.messages.clear();

		this.notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return this.messages.size();
	}

	@Override
	public View getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return Long.parseLong(this.messages.get(position).get(Telephony.Sms._ID), 10);
	}
}
