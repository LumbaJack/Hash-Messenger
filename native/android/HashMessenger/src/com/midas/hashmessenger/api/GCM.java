package com.midas.hashmessenger.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GCM {
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "app_version";
	private static final String TAG = "GCM_API";
	protected static final String SENDER_ID = "215473675714";
	private static final String DEVREG_URL = "http://23.21.90.204/api/push_reg.php";

	private AQuery aq;

	public boolean checkPlayServices(Context context, Activity activity) {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(context);
		if (resultCode != ConnectionResult.SUCCESS
				&& resultCode != ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(TAG, "This device is not supported.");
				activity.finish();
			}
			return false;
		}
		return true;
	}

	public String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGCMPreferences(context);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {
			Log.i(TAG, "Registration not found.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
				Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i(TAG, "App version changed.");
			return "";
		}
		return registrationId;
	}

	private SharedPreferences getGCMPreferences(Context context) {
		// This sample app persists the registration ID in shared preferences,
		// but
		// how you store the regID in your app is up to you.
		return context
				.getSharedPreferences("APPSETTINGS", Context.MODE_PRIVATE);
	}

	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	public void registerInBackground(final Context context) {
		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				String msg = "";
				try {

					GoogleCloudMessaging gcm = GoogleCloudMessaging
							.getInstance(context);

					String regid = gcm.register(SENDER_ID);
					msg = "Device registered, registration ID=" + regid;

					sendRegistrationIdToBackend(context, regid);

					storeRegistrationId(context, regid);
				} catch (IOException ex) {
					msg = "Error :" + ex.getMessage();

				}
				return msg;
			}

			@Override
			protected void onPostExecute(String msg) {
				Log.i(TAG, msg + "\n");
			}
		}.execute(null, null, null);

	}

	private void sendRegistrationIdToBackend(Context context, String regid) {
		aq = new AQuery(context);
		SharedPreferences prefs = context.getSharedPreferences("APPSETTINGS",
				Activity.MODE_PRIVATE);
		String value = prefs.getString("username", "");

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("user", value + "@hashmessenger.com");
		params.put("id", regid);
		aq.ajax(DEVREG_URL, params, JSONObject.class,
				new AjaxCallback<JSONObject>() {
					@Override
					public void callback(String url, JSONObject json,
							AjaxStatus status) {
						Log.i(TAG, "Status:" + status.toString());
					}
				});

	}

	private void storeRegistrationId(Context context, String regId) {
		final SharedPreferences prefs = getGCMPreferences(context);
		int appVersion = getAppVersion(context);
		Log.i(TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}
}
