package com.midas.hashmessenger.api;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.midas.hashmessenger.ChatActivity;
import com.midas.hashmessenger.R;
import com.midas.hashmessenger.SoundManager;

public class NotificationsAPI {
	private NotificationManager mNotificationManager;
	private static final int NOTIFICATION_ID_DEFAULT = 1;


	public void sendNotification(String userId, String msg, Context context)  {
		sendNotification(userId, msg, NOTIFICATION_ID_DEFAULT, context);
	}
	
	public void sendNotification( String msg, Context context)  {
		sendNotification(null, msg, NOTIFICATION_ID_DEFAULT, context);
	}
	
	public void sendNotification(String userId, String msg, int NOTIFICATION_ID,
			Context context) {

		mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent newIntent = new Intent(context, ChatActivity.class);
		if (userId != null) {
			newIntent.putExtra("USER_ID", userId);
		}
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				newIntent, Intent.FLAG_ACTIVITY_NEW_TASK);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(context.getString(R.string.apptitle))
				.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
				.setContentText(msg);

		mBuilder.setContentIntent(contentIntent);
		mBuilder.setAutoCancel(true);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
		SoundManager.play_push_received();
	}

	public void cancelNotificaction(int NOTIFICATION_ID, Context context) {
		mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(NOTIFICATION_ID);
	}

	public void cancelAllNotificaction(Context context) {
		mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
	}

}
