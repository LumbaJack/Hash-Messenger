package com.midas.hashmessenger;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jivesoftware.smack.util.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.midas.hashmessenger.api.SMSReceiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;
import android.widget.Toast;

public class signup extends Activity {

	private static final String VAL_URL = "http://23.21.90.204/api/validate.php";
	private static final String VER_URL = "http://23.21.90.204/api/verify.php";
	private List<CountryData> m_countryNames = null;
	private AQuery aq;
	private TextView txtc = null;
	SMSReceiver receiver = null;
	IntentFilter filter = null;
	int status = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signup);

		aq = new AQuery(this);
		aq.id(R.id.button1).clicked(this, "sendsms");
		aq.id(R.id.button2).clicked(this, "validatesms");
		txtc = (TextView) findViewById(R.id.textView3);
		final Spinner spinner = (Spinner) findViewById(R.id.spinner1);

		int defpos = -1;
		int position = 0;
		String isocode = Locale.getDefault().getISO3Country();

		m_countryNames = new ArrayList<CountryData>();
		XmlResourceParser parser = getResources().getXml(R.xml.countries);
		try {
			while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
				if (parser.getEventType() == XmlPullParser.START_TAG) {
					if (parser.getName().equals("country")) {
						String cca3code = parser
								.getAttributeValue(null, "cca3");
						String phonecode = parser.getAttributeValue(null,
								"calling-code");
						String name = parser.getAttributeValue(null, "name");

						m_countryNames.add(new CountryData(name, phonecode,
								cca3code));

						if (cca3code != null
								&& isocode != null
								&& cca3code.toUpperCase().equals(
										isocode.toUpperCase())) {

							txtc.setText(phonecode);
							defpos = position;
						}
						position++;
					}
				}
				parser.next();
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ArrayAdapter<CountryData> adapter = new ArrayAdapter<CountryData>(this,
				android.R.layout.simple_spinner_item, m_countryNames);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		if (defpos >= 0) {
			spinner.setSelection(defpos);
		}

		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				Object item = parent.getItemAtPosition(pos);
				if (item != null) {
					CountryData countryData = (CountryData) item;
					txtc.setText(countryData.PHONECODE);
				}
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

	}

	public void sendsms(View button) {
		final EditText numtel = (EditText) findViewById(R.id.editText1);
		final EditText smscode = (EditText) findViewById(R.id.editText2);
		final TextView txtc = (TextView) findViewById(R.id.textView3);
		final Button smsbto = (Button) findViewById(R.id.button2);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("tel", txtc.getText().toString()
				+ numtel.getText().toString());
		aq.ajax(VER_URL, params, JSONObject.class,
				new AjaxCallback<JSONObject>() {

					@Override
					public void callback(String url, JSONObject json,
							AjaxStatus status) {
						numtel.setEnabled(false);
						smscode.setEnabled(true);
						smsbto.setEnabled(true);
					}
				});
		//startLogic();
	}

	public void validatesms(View button) {
		final EditText numtel = (EditText) findViewById(R.id.editText1);
		final EditText smscode = (EditText) findViewById(R.id.editText2);
		final TextView txtc = (TextView) findViewById(R.id.textView3);

		// data: { cc: countycode, tel:tel, smscode: smscode, device:
		// device.platform },

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("cc", txtc.getText().toString());
		params.put("tel", numtel.getText().toString());
		params.put("smscode", smscode.getText().toString());
		params.put("device", "android");

		aq.ajax(VAL_URL, params, JSONObject.class,
				new AjaxCallback<JSONObject>() {

					@Override
					public void callback(String url, JSONObject json,
							AjaxStatus status) {

						if (status.getCode() == 200) {
							try {
								if (json.getInt("retcode") == 200) {
									SharedPreferences prefs = getSharedPreferences(
											"APPSETTINGS",
											Activity.MODE_PRIVATE);
									prefs.edit()
											.putString(
													HashConstants.PREF_USERNAME,
													txtc.getText().toString()
															+ numtel.getText()
																	.toString())
											.commit();
									prefs.edit()
											.putString(
													HashConstants.PREF_PASSWORD,
													json.getString("pass"))
											.commit();

									/**
									 * Generate a random password if it doesn't
									 * already exist
									 */
									if (prefs
											.getString(
													HashConstants.PREF_ENCRYPT_HISTORY_GENPASSWORD,
													null) == null) {

										prefs.edit()
												.putString(
														HashConstants.PREF_ENCRYPT_HISTORY_GENPASSWORD,
														CryptoManager
																.sha1Base64String(CryptoManager
																		.randomPassword()))
												.commit();
									}

									Intent intent = new Intent(signup.this,
											gen_keys.class);
									signup.this.startActivity(intent);
									finish();

								} else {
									Show_error("Invalid sms code");
								}
							} catch (JSONException e) {
								Show_error("General Error");
								finish();
								System.exit(0);
							}
						} else {
							Show_error("Network error,please try again.");

						}

					}
				});
	}

	void Show_error(String msg) {
		new AlertDialog.Builder(this).setTitle("Error").setMessage(msg)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}

	private class CountryData {
		public String Name = null;
		public String CCA3 = null;
		public String PHONECODE = null;

		public CountryData(String name, String phone, String cca3) {
			Name = name;
			PHONECODE = phone;
			CCA3 = cca3;
		}

		public String toString() {
			return Name;
		}
	}

	private void startLogic() {

		try {
			final Handler handler = new Handler() {
				public void handleMessage(Message msg) {
					try {
						int status = msg.getData().getInt("status");
						if (status < 0)
							Toast.makeText(signup.this,
									"Unregistering receiver for 20 sec",
									Toast.LENGTH_SHORT).show();
						else
							Toast.makeText(signup.this,
									"Registering receiver for 20sec",
									Toast.LENGTH_SHORT).show();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			new Thread() {
				public void run() {
					try {
						while (true) {
							Bundle b = new Bundle();
							b.putInt("status", status);
							Message msg = new Message();
							msg.setData(b);
							handler.sendMessage(msg);
							changeReceiverStatus(status);
							status = status * -1;
							Thread.sleep(20000);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}.start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void changeReceiverStatus(int status) {
		try {
			if (receiver == null)
				receiver = new SMSReceiver();
			if (filter == null) {
				filter = new IntentFilter();
				filter.addAction("android.provider.Telephony.SMS_RECEIVED");
			}
			if (status < 0)
				this.unregisterReceiver(receiver);
			else
				this.registerReceiver(receiver, filter);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
