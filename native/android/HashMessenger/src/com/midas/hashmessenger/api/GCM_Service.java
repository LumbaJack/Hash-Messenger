package com.midas.hashmessenger.api;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.midas.hashmessenger.api.NotificationsAPI;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;


public class GCM_Service extends IntentService {
	public static final String TAG = "GCM_Service";
	private NotificationsAPI Notification  = new  NotificationsAPI();
	
	public GCM_Service() {
		super("GCM_Service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		// The getMessageType() intent parameter must be the intent you received
		// in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) { // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that
			 * GCM will be extended in the future with new message types, just
			 * ignore any message types you're not interested in, or that you
			 * don't recognize.
			 */
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
					.equals(messageType)) {
				Notification.sendNotification("Send error: " + extras.toString(),this);
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
					.equals(messageType)) {
				Notification.sendNotification("Deleted messages on server: "
						+ extras.toString(),this);
				// If it's a regular GCM message, do some work.
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
					.equals(messageType)) {
				// This loop represents the service doing some work.
				for (int i = 0; i < 5; i++) {
					Log.i(TAG,
							"Working... " + (i + 1) + "/5 @ "
									+ SystemClock.elapsedRealtime());
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
					}
				}
				Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
				String from_userid = null;
				
				// Need to parse the userid from the Bundle message
				// String from_userid = extras.getString("message");
				// Log.i(TAG, "fromuser => " + from_userid );
				
				// Post notification of received message.
				Notification.sendNotification("Received: " + extras.toString(),this);
				Log.i(TAG, "Received: " + extras.toString());
			}
		}
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

}