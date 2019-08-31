package com.jellyshack.block6.util;

import android.provider.Telephony;

import java.util.HashMap;
import java.util.Map;

public class SimpleSMSMessage implements Comparable<SimpleSMSMessage> {
	public static String[] PROJECTION = {
			Telephony.Sms.TYPE,
			Telephony.Sms._ID,
			Telephony.Sms.ADDRESS,
			Telephony.Sms.BODY,
			Telephony.Sms.DATE
	};

	private Map<String, String> map = new HashMap<>();

	public void put(String key, String value) {
		this.map.put(key, value);
	}

	public String get(String key) {
		String value = this.map.get(key);

		if(key.equals(Telephony.Sms.DATE)) {
			value = SmsUtil.formatUnixTimestamp(Long.parseLong(value));
		}

		return value;
	}

	public long getDate() {
		return Long.parseLong(this.map.get(Telephony.Sms.DATE));
	}

	/**
	 * This will allow sorting messages by date in descending order.
	 * @param m
	 * @return
	 */
	@Override
	public int compareTo(SimpleSMSMessage m) {
		long a = this.getDate();
		long b = m.getDate();

		return a < b ? 1 : a > b ? -1 : 0;
	}
}
