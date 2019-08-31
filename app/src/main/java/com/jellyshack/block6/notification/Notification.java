package com.jellyshack.block6.notification;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.jellyshack.block6.R;
import com.jellyshack.block6.activity.ComposeSMSActivity;

import java.util.Random;

public class Notification {
	public static final String CHANNEL_ID = "smsNotification"; // Must be unique per package

	public static void create(Context context, String phoneNumber, String message) {
		createNotificationChannel(context); // This is safe to call multiple times. Subsequent calls are a no op.

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
				.setSmallIcon(R.drawable.smsicon)
				.setContentTitle("SMS from " + phoneNumber)
				.setContentText(message)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setAutoCancel(true); // Close the notification when the user taps it.

		/**
		 * Start the ComposeSMSActivity when the user clicks the notification.
		 * See: https://developer.android.com/training/notify-user/navigation
		 */
		Intent intent = new Intent(context, ComposeSMSActivity.class);
		intent.putExtra(Telephony.Sms.ADDRESS, phoneNumber);

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		stackBuilder.addNextIntentWithParentStack(intent);
		PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		builder.setContentIntent(pendingIntent);

		Random ran = new Random();
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
		notificationManager.notify(ran.nextInt(), builder.build());
	}

	/**
	 * Only create the NotificationChannel for API 26+ because the
	 * NotificationChannel class is new and not in the support lib.
	 * @param context
	 */
	@TargetApi(26)
	private static void createNotificationChannel(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
			String name = "SMS notification channel";
			String description = "SMS notifications";
			int importance = NotificationManager.IMPORTANCE_DEFAULT;

			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
			channel.setDescription(description);

			// Register the channel with the system.
			NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
			notificationManager.createNotificationChannel(channel);
		}
	}
}
