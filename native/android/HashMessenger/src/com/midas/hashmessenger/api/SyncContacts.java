package com.midas.hashmessenger.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

public class SyncContacts extends AsyncTask<Void, Void, JSONArray> {
	private static final String SYNC_URL = "http://23.21.90.204/api/sync_contacts.php";

	private static final String TAG = SyncContacts.class.getName();
	private Context m_context;
	private SyncContactsListener m_listener;

	public SyncContacts(Context context) {
		super();
		m_context = context;
	}

	public void setListener(SyncContactsListener listener) {
		m_listener = listener;
	}

	@Override
	protected JSONArray doInBackground(Void... params) {
		try {
			JSONArray jsonreq = new JSONArray();
			PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

			List<IdPhonePair> idPhoneList = getContactIdTels();
			for (IdPhonePair idPhoneMap : idPhoneList) {
				try {
					JSONObject json = new JSONObject();
					json.put("id", idPhoneMap.id);
					JSONArray jarray = new JSONArray();
					for (String phone : idPhoneMap.phones) {
						PhoneNumber phoneProto = phoneUtil.parse(phone, "US");
						jarray.put(phoneUtil.format(phoneProto,
								PhoneNumberFormat.INTERNATIONAL));
					}
					json.put("phone", jarray);
					json.put("disp",  "");
					jsonreq.put(json);
				} catch (NumberParseException e) {
					Log.e(TAG,
							"NumberParseException was thrown: " + e.toString());
					continue;
				}
			}


			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(SYNC_URL);

			httppost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("data", jsonreq.toString()));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent(), "iso-8859-1"), 8);
			StringBuilder sb = new StringBuilder();
			sb.append(reader.readLine() + "\n");
			String line = "0";
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			reader.close();
			String result = sb.toString();

			return new JSONArray(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(JSONArray result) {
		m_listener.onSyncContactsComplete(result);
	}

	private class IdPhonePair {
		public String id = null;
		public String name = null;
		public String[] phones = null;
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

					IdPhonePair newpair = new IdPhonePair();
					newpair.id = id;
					newpair.name = contactName;
					newpair.phones = new String[phonesFound.size()];
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
