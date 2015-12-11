package com.midas.hashmessenger;

import org.jivesoftware.smack.util.Base64;

import com.midas.hashmessenger.database.HashDataAdapter;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class HashApplication extends Application {
	private static HashDataAdapter db;
	private static XMPPClient m_xmpp = null;
	private static Context m_appcontext = null;


	public void onCreate() {
		m_appcontext = getApplicationContext();
		db = new HashDataAdapter(m_appcontext);
		m_xmpp = new XMPPClient(m_appcontext);
	}

	public static void XMMPClose() {
		try {
			m_xmpp.disconnect();
		} catch (Exception e) {
		}
	}

	public static Context getContext() {
		return m_appcontext;
	}

	public static HashDataAdapter getDatabaseAdapter() {
		return db;
	}

	public static XMPPClient getXmpp() {
		return m_xmpp;
	}


}
