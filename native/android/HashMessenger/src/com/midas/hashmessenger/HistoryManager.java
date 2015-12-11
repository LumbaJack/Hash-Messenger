package com.midas.hashmessenger;

import org.jivesoftware.smack.util.Base64;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class HistoryManager {
	private static Context m_appcontext = null;
	private static SharedPreferences m_prefs = null;
	private static byte[] m_historyPassword = null;
	private static boolean m_isUserHistoryAuthenticated = false;

	private static Context getContext() {
		if (m_appcontext == null) {
			m_appcontext = HashApplication.getContext();
		}
		return m_appcontext;
	}

	private static SharedPreferences getPrefs() {
		if (m_prefs == null) {
			m_prefs = HistoryManager.getContext().getSharedPreferences(
					"APPSETTINGS", Activity.MODE_PRIVATE);
		}
		return m_prefs;
	}

	public static byte[] getHistoryPassword() {
		if (m_historyPassword == null) {
			String savedPassword = HistoryManager.getPrefs().getString(
					HashConstants.PREF_ENCRYPT_HISTORY_PASSWORD, null);
			if (savedPassword != null && !savedPassword.isEmpty()) {
				m_historyPassword = Base64.decode(savedPassword);
			} else {
				savedPassword = HistoryManager.getPrefs().getString(
						HashConstants.PREF_ENCRYPT_HISTORY_GENPASSWORD, null);
				if (savedPassword != null && !savedPassword.isEmpty()) {
					m_historyPassword = Base64.decode(savedPassword);
				}
			}
		}
		return m_historyPassword;
	}

	public static boolean hasUserHistoryPassword() {
		String savedPassword = HistoryManager.getUserHistoryPasswordString();
		if (savedPassword != null && !savedPassword.isEmpty()) {
			return true;
		}
		return false;
	}

	public static byte[] getUserHistoryPassword() {
		return Base64.decode(HistoryManager.getUserHistoryPasswordString());
	}

	public static String getUserHistoryPasswordString() {
		String savedPassword = HistoryManager.getPrefs().getString(
				HashConstants.PREF_ENCRYPT_HISTORY_PASSWORD, null);
		if (savedPassword != null && !savedPassword.isEmpty()) {
			return savedPassword;
		}
		return null;
	}

	public static boolean checkUserHistoryPassword(String cleartext) {

		String savedPassword = HistoryManager.getUserHistoryPasswordString();
		if (cleartext == null || savedPassword == null) {
			return false;
		}
		String hashedtxt = CryptoManager.sha1Base64String(cleartext);
		if (savedPassword.equals(hashedtxt)) {
			return true;
		}
		return false;
	}

	public static boolean authenticateUserHistory(String cleartext) {
		if (!HistoryManager.hasUserHistoryPassword()) {
			HistoryManager.m_isUserHistoryAuthenticated = true;

		} else {
			HistoryManager.m_isUserHistoryAuthenticated = HistoryManager
					.checkUserHistoryPassword(cleartext);
		}
		return HistoryManager.m_isUserHistoryAuthenticated;
	}

	public static boolean isUserHistoryAuthenticated() {
		return m_isUserHistoryAuthenticated;
	}

	public static boolean reencrypt_history(byte[] prevPassword,
			byte[] newPassword) {
		HashApplication.getDatabaseAdapter().reencrypt_messages(prevPassword, newPassword);
		return true;
	}
}
