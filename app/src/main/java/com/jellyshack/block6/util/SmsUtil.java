package com.jellyshack.block6.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SmsUtil {
	private static DateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a");

	private static List<SimpleSMSMessage> getMessages(Context context, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if(sortOrder == null) {
			sortOrder = Telephony.Sms.DEFAULT_SORT_ORDER; // Default is date desc
		}

		List<SimpleSMSMessage> messages = new ArrayList<>();

		try(Cursor cursor = context.getContentResolver().query(Telephony.Sms.CONTENT_URI, projection, selection, selectionArgs, sortOrder)) {
			if (cursor != null) {
				while(cursor.moveToNext()) {
					SimpleSMSMessage message = new SimpleSMSMessage();

					for(String column : projection) {
						int idx = cursor.getColumnIndex(column);
						String value = cursor.getString(idx);

						message.put(column, value);
					}

					messages.add(message);
				}
			}
		}

		return messages;
	}

	public static List<SimpleSMSMessage> getAllMessages(Context context) {
		String[] projection = SimpleSMSMessage.PROJECTION;
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = Telephony.Sms.DEFAULT_SORT_ORDER;

		return getMessages(context, projection, selection, selectionArgs, sortOrder);
	}

	public static List<SimpleSMSMessage> getUnreadMessages(Context context) {
		String[] projection = SimpleSMSMessage.PROJECTION;
		String selection = "read = 0"; // Unread condition
		String[] selectionArgs = null;
		String sortOrder = Telephony.Sms.DEFAULT_SORT_ORDER;

		return getMessages(context, projection, selection, selectionArgs, sortOrder);
	}

	public static List<SimpleSMSMessage> getMessagesFromNumber(Context context, String address, long since) {
		String[] projection = SimpleSMSMessage.PROJECTION;
		String selection = "address = ? and date > ?";
		String[] selectionArgs = { address, String.valueOf(since) };
		String sortOrder = "date asc";

		return getMessages(context, projection, selection, selectionArgs, sortOrder);
	}

	/**
	 * Gets the most recent message from every number in the past limitDays days.
	 * If limitDays = 0, there is effectively no limit.
	 * @param context
	 * @return
	 */
	public static List<SimpleSMSMessage> getTopMessages(Context context, int limitDays) {
		List<SimpleSMSMessage> messages = new ArrayList<>();

		String ts = limitDays > 0 ? String.valueOf(System.currentTimeMillis() - ((1000L * 60 * 60 * 24) * limitDays)) : "0";

		String[] projection = { "distinct " + Telephony.Sms.ADDRESS };
		String selection = "date > ?";
		String[] selectionArgs = new String[] { ts };
		String sortOrder = null;

		// Get list of all addresses (numbers) that we've sent or received a message from within the past n days.
		List<String> addresses = new ArrayList<>();

		try(Cursor cursor = context.getContentResolver().query(Telephony.Sms.CONTENT_URI, projection, selection, selectionArgs, sortOrder)) {
			if (cursor != null) {
				while(cursor.moveToNext()) {
					int idx = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
					String value = cursor.getString(idx);
					addresses.add(value);
				}
			}
		}

		// Get the most recently sent or received message from every address within the past n days.
		projection = SimpleSMSMessage.PROJECTION;
		selection = "address = ? and date > ?";
		sortOrder = "date desc limit 1";

		for(String address : addresses) {
			if(address == null) {
				continue;
			}

			selectionArgs = new String[] { address, ts };

			try (Cursor cursor = context.getContentResolver().query(Telephony.Sms.CONTENT_URI, projection, selection, selectionArgs, sortOrder)) {
				if (cursor != null) {
					while (cursor.moveToNext()) {
						SimpleSMSMessage message = new SimpleSMSMessage();

						for (String column : projection) {
							int idx = cursor.getColumnIndex(column);
							String value = cursor.getString(idx);

							message.put(column, value);
						}

						messages.add(message);
					}
				}
			}
		}

		// Sort the messages by date received.
		Collections.sort(messages);

		return messages;
	}

	public static void markMessagesFromNumberAsRead(Context context, String address) {
		ContentValues values = new ContentValues();
		values.put(Telephony.Sms.READ, 1);

		String where = "address = ? and read = 0";
		String[] selectionArgs = { address };

		context.getContentResolver().update(Telephony.Sms.Inbox.CONTENT_URI, values, where, selectionArgs);
	}

	public static String formatUnixTimestamp(long ts) {
		Date dt = Calendar.getInstance().getTime();
		dt.setTime(ts);
		return DATE_FORMAT.format(dt);
	}
}
