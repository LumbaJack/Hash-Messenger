package com.midas.hashmessenger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.midas.hashmessenger.database.HashDataAdapter;

public class HashApiClient {
	private static final String SYNC_URL = "http://23.21.90.204/api/sync_contacts.php";
	private static final String TAG = HashApiClient.class.getName();

	private Context m_context;
	private AQuery aq;

	HashApiClient(Context context) {
		m_context = context;
		aq = new AQuery(m_context);
	}

	public void syncContacts(final HashDataAdapter db,
			final SyncContactsListener callback) {
		Map<String, Object> params = new HashMap<String, Object>();
		final List<IdPhonePair> dataSent = new ArrayList<IdPhonePair>();
		try {
			JSONArray jsonreq = new JSONArray();
			PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

			List<IdPhonePair> idPhoneList = getContactIdTels();
			for (IdPhonePair idPhoneMap : idPhoneList) {
				try {
					if (idPhoneMap.phones == null
							|| idPhoneMap.phones.length == 0)
						continue;

					JSONObject json = new JSONObject();
					json.put("id", idPhoneMap.id);
					JSONArray jarray = new JSONArray();

					for (String phone : idPhoneMap.phones) {
						PhoneNumber phoneProto = phoneUtil.parse(phone, "US");
						jarray.put(phoneUtil.format(phoneProto,
								PhoneNumberFormat.INTERNATIONAL));
					}
					json.put("phone", jarray);
					json.put("disp", "");
					jsonreq.put(json);
					dataSent.add(idPhoneMap);
				} catch (NumberParseException e) {
					Log.e(TAG,
							"NumberParseException was thrown: " + e.toString());
					continue;
				} catch (JSONException e) {
					Log.e(TAG,
							"NumberParseException was thrown: " + e.toString());
					continue;
				}
			}
			params.put("data", jsonreq.toString());
			aq.ajax(SYNC_URL, params, JSONArray.class,
					new AjaxCallback<JSONArray>() {
						@Override
						public void callback(String url, JSONArray json,
								AjaxStatus status) {

							if (status.getCode() == 200) {
								for (IdPhonePair idPhoneMap : dataSent) {
									for (int i = 0; i < json.length(); i++) {
										JSONObject jsonobj = null;
										try {
											jsonobj = (JSONObject) json.get(i);
											String id = jsonobj.getString("id");
											if (!idPhoneMap.id.equals(id)) {
												continue;
											}

											jsonobj.put("disp", idPhoneMap.name);
											jsonobj.put("icon",
													idPhoneMap.photo_uri);
										} catch (JSONException e) {
											// TODO Auto-generated catch block
											Log.e(TAG, e.toString());
											continue;
										}
									}
								}
								try {
									db.sync_contacts(json);
								} catch (JSONException e) {
									new AlertDialog.Builder(
											HashApiClient.this.m_context)
											.setTitle("Unable to sync contacts")
											.setMessage(e.getLocalizedMessage())
											.setPositiveButton(
													"OK",
													new DialogInterface.OnClickListener() {
														public void onClick(
																DialogInterface dialog,
																int which) {
														}
													}).show();
								}
							}
							callback.onSyncContactsComplete(json);
						}
					});
		} finally {

		}
	}

	private class IdPhonePair {
		public String id = null;
		public String name = null;
		public String[] phones = null;
		public String photo_uri = null;
	}

	private List<IdPhonePair> getContactIdTels() {
		String contactNumber = null;
		List<IdPhonePair> cdata = new ArrayList<IdPhonePair>();

		ContentResolver cr = m_context.getContentResolver();
		Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
				null, null, null);
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				String id = cur.getString(cur.getColumnIndex(BaseColumns._ID));
				String contactName = cur
						.getString(cur
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

				if (Integer
						.parseInt(cur.getString(cur
								.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
					Cursor phones = cr.query(
							ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
							null,
							ContactsContract.CommonDataKinds.Phone.CONTACT_ID
									+ " = ?", new String[] { id }, null);

					ArrayList<String> phonesFound = new ArrayList<String>();
					while (phones.moveToNext()) {
						contactNumber = phones.getString(phones
								.getColumnIndex(Phone.NUMBER));
						phonesFound.add(contactNumber);
					}
					phones.close();

					String photoUri = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));

					if (photoUri == null) {
						Resources resources = m_context.getResources();
						Uri uri = Uri
								.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
										+ "://"
										+ resources
												.getResourcePackageName(R.drawable.nopicture_thumbnail)
										+ '/'
										+ resources
												.getResourceTypeName(R.drawable.nopicture_thumbnail)
										+ '/'
										+ resources
												.getResourceEntryName(R.drawable.nopicture_thumbnail));
						photoUri = uri.toString();
					}

					IdPhonePair newpair = new IdPhonePair();
					newpair.id = id;
					newpair.name = contactName;
					newpair.phones = new String[phonesFound.size()];
					newpair.photo_uri = photoUri;
					phonesFound.toArray(newpair.phones);
					cdata.add(newpair);
				}

			}
		}// end of contact name cursor
		cur.close();
		return cdata;
	}

	public interface SyncContactsListener {
		public void onSyncContactsComplete(JSONArray sync_results);
	}

}
