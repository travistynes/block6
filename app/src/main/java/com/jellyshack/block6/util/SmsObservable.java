package com.jellyshack.block6.util;

import java.util.Observable;

public class SmsObservable extends Observable {
	private static SmsObservable instance = new SmsObservable();

	private SmsObservable() {

	}

	public static SmsObservable getInstance() {
		return instance;
	}

	public void update(NewMessageMarker marker) {
		synchronized (this) {
			this.setChanged();
			this.notifyObservers(marker);
		}
	}

	/**
	 * When a new SMS messages is received, a marker object will be created prior to the
	 * message being saved in the SMS inbox. The marker can then be used to query for the
	 * new message.
	 */
	public static class NewMessageMarker {
		public long ts = System.currentTimeMillis();
		public String address;

		public NewMessageMarker(String address) {
			this.address = address;
		}
	}
}
