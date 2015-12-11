package com.midas.hashmessenger;

import android.media.MediaPlayer;
import android.provider.Settings;

public class SoundManager {
	private static boolean mute = false;

	public static void play_push_received() {
		if (!mute) {
			MediaPlayer mp = MediaPlayer.create(HashApplication.getContext(),
					Settings.System.DEFAULT_NOTIFICATION_URI);
			mp.start();
		}
	}

	public static void play_message_received() {
		MediaPlayer mp = MediaPlayer.create(HashApplication.getContext(),
				Settings.System.DEFAULT_NOTIFICATION_URI);
		mp.start();
	}

	public static void play_keygen_complete() {
		play_sound(R.raw.notification);
	}

	public static void play_sound(int resId) {
		if (!mute) {
			MediaPlayer mp = MediaPlayer.create(HashApplication.getContext(),
					resId);
			mp.start();
		}
	}
}
