package com.midas.hashmessenger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.midas.hashmessenger.HashApiClient;
import com.midas.hashmessenger.api.GCM;

public class MainActivity extends BaseActivity implements
		HashApiClient.SyncContactsListener {
	private static final String TAG = MainActivity.class.getName();
	protected static final int REQUEST_CAMERA = 0;
	protected static final int SELECT_FILE = 1;

	public AQuery aq;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		aq = new AQuery(this);

		setContentView(R.layout.activity_main);

		SharedPreferences prefs = getSharedPreferences("APPSETTINGS",
				Activity.MODE_PRIVATE);
		String value = prefs.getString("EULA", "");
		if (value.equals("true")) {

			aq.id(R.id.textView1).text("EULA accepted");
		} else {
			aq.id(R.id.textView1).text("EULA not accepted");

		}
		final Button contactListButton = (Button) findViewById(R.id.button3);
		contactListButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,
						ContactListActivity.class);
				MainActivity.this.startActivity(intent);
			}
		});

		final Button chatButton = (Button) findViewById(R.id.button4);
		chatButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,
						ChatActivity.class);
				MainActivity.this.startActivity(intent);
			}
		});

		final Button chatListButton = (Button) findViewById(R.id.button5);
		chatListButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,
						ChatListActivity.class);
				MainActivity.this.startActivity(intent);
			}
		});

		final Button bto1 = (Button) findViewById(R.id.button1);
		bto1.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, eula.class);
				MainActivity.this.startActivity(intent);

			}
		});

		aq.id(R.id.button7).clicked(this, "uploadpic");

		aq.id(R.id.button6).clicked(new View.OnClickListener() {

			public void onClick(View v) {
				HashApiClient apiClient = new HashApiClient(MainActivity.this);
				apiClient.syncContacts(HashApplication.getDatabaseAdapter(),
						MainActivity.this);
				/*
				 * SyncContacts syncContacts = new
				 * SyncContacts(MainActivity.this);
				 * syncContacts.setListener(MainActivity.this);
				 * syncContacts.execute();
				 */
			}
		});

		GCM gcmapi = new GCM();
		Context context = getApplicationContext();
		if (gcmapi.checkPlayServices(context, this)) {
			// gcm = GoogleCloudMessaging.getInstance(this);
			String regid = gcmapi.getRegistrationId(context);
			if (regid.isEmpty()) {
				gcmapi.registerInBackground(context);
			}
		} else {
			Log.i(TAG, "No valid Google Play Services APK found.");
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
		MainActivity.this.startActivity(intent);
		return true;
	}

	@Override
	public void onSyncContactsComplete(JSONArray sync_results) {
		Log.i(TAG, "onSyncContactsComplete");
		new AlertDialog.Builder(this).setTitle("Sync Contacts complete")
				.setMessage("Yeahhh")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}

	public void uploadpic(View button) {

		/*
		 * Intent intent = new Intent(); intent.setType("image/*");
		 * intent.setAction(Intent.ACTION_GET_CONTENT);
		 * startActivityForResult(Intent.createChooser(intent,
		 * "Select Picture"), 0);
		 */
		final CharSequence[] items = { "Take Photo", "Choose from Library",
				"Cancel" };

		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Add Photo!");
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				if (items[item].equals("Take Photo")) {
					Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					File f = new File(android.os.Environment
							.getExternalStorageDirectory(), "temp.jpg");
					intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
					startActivityForResult(intent, REQUEST_CAMERA);
				} else if (items[item].equals("Choose from Library")) {
					Intent intent = new Intent(
							Intent.ACTION_GET_CONTENT,
							android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					intent.setType("image/*");
					startActivityForResult(
							Intent.createChooser(intent, "Select File"),
							SELECT_FILE);
				} else if (items[item].equals("Cancel")) {
					dialog.dismiss();
				}
			}
		});
		builder.show();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			Bitmap bitmap = null;
			String imageFilePath = null;
			if (requestCode == REQUEST_CAMERA) {
				File f = new File(Environment.getExternalStorageDirectory()
						.toString());
				for (File temp : f.listFiles()) {
					if (temp.getName().equals("temp.jpg")) {
						f = temp;
						break;
					}
				}
				imageFilePath = f.getAbsolutePath();
				bitmap = BitmapFactory.decodeFile(imageFilePath);
			} else {
				Uri uri = data.getData();
				imageFilePath = getPath(uri, getContentResolver());
				bitmap = BitmapFactory.decodeFile(imageFilePath);
			}
			aq.id(R.id.imageView1);
			ImageView imageView = aq.getImageView();
			imageView.setImageBitmap(bitmap);
			upload_async(imageFilePath, bitmap);
		}
	}

	private void upload_async(String fname, Bitmap data) {

		// Upload the image
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		data.compress(CompressFormat.JPEG, 60, bos);

		byte[] imgdata = bos.toByteArray();

		String url = "http://23.21.90.204/api/upload.php";

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", fname);
		params.put("source", imgdata);

		aq.ajax(url, params, JSONObject.class, new AjaxCallback<JSONObject>() {
			public void callback(String url, JSONObject json, AjaxStatus status) {
				try {
					Log.i(TAG, "Uploaded" + json.getString("msg"));
				} catch (JSONException e) {
					Log.i(TAG, "Upload fail");
				}
			}
		});
	}

	public String getPath(Uri uri, ContentResolver contentResolver) {
		String[] projection = { MediaStore.MediaColumns.DATA };
		Cursor cursor;
		try {
			cursor = contentResolver.query(uri, projection, null, null, null);
		} catch (SecurityException e) {
			String path = uri.getPath();
			String result = tryToGetStoragePath(path);
			return result;
		}
		if (cursor != null) {
			// HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
			// THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
			cursor.moveToFirst();
			int columnIndex = cursor
					.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
			String filePath = cursor.getString(columnIndex);
			cursor.close();
			return filePath;
		} else
			return uri.getPath(); // FOR OI/ASTRO/Dropbox etc
	}

	private String tryToGetStoragePath(String path) {
		int actualPathStart = path.indexOf("//storage");
		String result = path;

		if (actualPathStart != -1 && actualPathStart < path.length())
			result = path.substring(actualPathStart + 1, path.length());

		return result;
	}
}
