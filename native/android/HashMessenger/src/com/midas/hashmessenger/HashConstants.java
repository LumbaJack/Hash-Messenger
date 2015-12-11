package com.midas.hashmessenger;

public class HashConstants {
	public final static String TAG = "HashMessenger";

	/**
	 * Enable verbose logging including stack traces, etc. Set to false before
	 * releasing
	 * 
	 */
	public static final boolean LOGV = false;

	/**
	 * Preference strings so we don't introduce typos
	 */
	public static final String PREF_USERNAME = "username";
	public static final String PREF_PASSWORD = "password";
	public static final String PREF_PRIVATEKEY = "PrivateKey";
	public static final String PREF_PUBLICKEY = "PublicKey";
	public static final String PREF_SAVE_HISTORY = "save_history";
	public static final String PREF_SAVE_HISTORY_DAYS = "save_history_days";
	public static final String PREF_ENCRYPT_HISTORY = "encrypt_history";
	public static final String PREF_ENCRYPT_HISTORY_PASSWORD = "encrypt_history_password";
	public static final String PREF_ENCRYPT_HISTORY_GENPASSWORD = "encrypt_history_genpassword";
}
