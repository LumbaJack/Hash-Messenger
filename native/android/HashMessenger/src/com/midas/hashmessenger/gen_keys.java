package com.midas.hashmessenger;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.util.Base64;
import org.json.JSONObject;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

public class gen_keys extends Activity implements SensorEventListener {

	private AQuery aq;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private final float NOISE = (float) 2.0;
	private boolean mInitialized;
	private float mLastX, mLastY, mLastZ;
	private float accuracy;
	private String seed = "";
	private ProgressBar mProgress;
	private static final String REGPUB_URL = "http://23.21.90.204/api/pubkey_reg.php";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_genkeys);

		mProgress = (ProgressBar) findViewById(R.id.progressBar1);
		mProgress.setMax(20);
		aq = new AQuery(this);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);

		aq.id(R.id.button1).clicked(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSensorManager.unregisterListener(gen_keys.this);
				try {
					SecureRandom random = new SecureRandom();
					CreateKeys(new BigInteger(130, random).toString(32));
					Intent intent = new Intent(gen_keys.this,
							ChatListActivity.class);
					gen_keys.this.startActivity(intent);
					finish();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		if (sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
			return;
		}
		this.accuracy = accuracy;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (this.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {

			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];
			if (!mInitialized) {
				mLastX = x;
				mLastY = y;
				mLastZ = z;
				aq.id(R.id.textView3).text("0.0");
				aq.id(R.id.textView5).text("0.0");
				aq.id(R.id.textView7).text("0.0");
				mInitialized = true;
			} else {
				float deltaX = Math.abs(mLastX - x);
				float deltaY = Math.abs(mLastY - y);
				float deltaZ = Math.abs(mLastZ - z);
				if (deltaX < NOISE)
					deltaX = (float) 0.0;
				if (deltaY < NOISE)
					deltaY = (float) 0.0;
				if (deltaZ < NOISE)
					deltaZ = (float) 0.0;
				mLastX = x;
				mLastY = y;
				mLastZ = z;
				aq.id(R.id.textView3).text(Float.toString(deltaX));
				aq.id(R.id.textView5).text(Float.toString(deltaY));
				aq.id(R.id.textView7).text(Float.toString(deltaZ));

				if (deltaX > 1 && deltaY > 1 && deltaZ > 1 && deltaX <= 10
						&& deltaY <= 10 && deltaZ <= 10) {
					char chr = (char) Math.abs(((25.5 * deltaX)
							+ (25.5 * deltaX) + (25.5 * deltaX)) / 3);
					seed = seed + chr;
					mProgress.setProgress(seed.length());
					if (seed.length() == 10) {
						mSensorManager.unregisterListener(this);
						try {
							CreateKeys(seed);
							Intent intent = new Intent(gen_keys.this,
									ChatListActivity.class);
							gen_keys.this.startActivity(intent);
							finish();
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			}
		}
	}

	public void CreateKeys(String seed) throws NoSuchAlgorithmException {
		KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
		SecureRandom ss = null;
		ss = new SecureRandom(seed.getBytes());
		keyGenerator.initialize(1024, ss);
		KeyPair myKeyPair = keyGenerator.generateKeyPair();

		SharedPreferences prefs = getSharedPreferences("APPSETTINGS",
				Activity.MODE_PRIVATE);
		String username = prefs.getString("username", "");

		prefs.edit()
				.putString("PrivateKey",
						Base64.encodeBytes(myKeyPair.getPrivate().getEncoded()))
				.commit();

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("tel", username);
		params.put("rsapub",
				Base64.encodeBytes(myKeyPair.getPublic().getEncoded()));

		aq.ajax(REGPUB_URL, params, JSONObject.class,
				new AjaxCallback<JSONObject>() {

					@Override
					public void callback(String url, JSONObject json,
							AjaxStatus status) {
						Log.i("RSAKEY", "RSA public upload");
						SoundManager.play_keygen_complete();
					}
				});
	}
}
