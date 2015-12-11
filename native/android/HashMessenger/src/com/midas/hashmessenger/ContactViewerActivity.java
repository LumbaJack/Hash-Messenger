package com.midas.hashmessenger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class ContactViewerActivity extends BaseActivity {

	private final int SELECT_PHOTO = 1;

	private HashContactData m_contactData = null;
	private ImageView m_contactEntryImage = null;
	private EditText m_contactEntryEdit = null;
	private Button m_useDefaultBtn = null;
	private Button m_blockContactBtn = null;
	private String m_newPhotoUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_viewer);

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String contact_id = extras.getString("CONTACT_ID");
			ArrayList<HashContactData> contactList = HashApplication
					.getDatabaseAdapter().get_contact(contact_id);
			if (contactList.size() > 0) {
				m_contactData = contactList.get(0);
			}
		}
		if (m_contactData == null) {
			m_contactData = new HashContactData();
			m_contactData._ID = "0";
			m_contactData.DISPLAY_NAME = "Not found";
			m_contactData.EMAIL = "";
			m_contactData.PHOTO_URI = "";
			m_contactData.PUBKEY = "";
		}

		m_contactEntryImage = (ImageView) findViewById(R.id.contactPhoto);
		m_contactEntryImage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				openImageIntent();
			}
		});

		m_contactEntryEdit = (EditText) findViewById(R.id.contactText);

		m_useDefaultBtn = (Button) findViewById(R.id.useDefaultBtn);
		m_useDefaultBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ContentResolver cr = ContactViewerActivity.this
						.getContentResolver();
				Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
						null, null, null, null);
				if (cur.getCount() > 0) {
					while (cur.moveToNext()) {
						String id = cur.getString(cur
								.getColumnIndex(BaseColumns._ID));
						if (id == null) {
							continue;
						}

						if (!id.equals(m_contactData._ID)) {
							continue;
						}

						m_contactData.DISPLAY_NAME = cur.getString(cur
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
						m_contactData.PHOTO_URI = cur.getString(cur
								.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));

						if (m_contactData.PHOTO_URI == null) {
							Resources resources = ContactViewerActivity.this
									.getResources();
							Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
									+ "://"
									+ resources
											.getResourcePackageName(R.drawable.nopicture_thumbnail)
									+ '/'
									+ resources
											.getResourceTypeName(R.drawable.nopicture_thumbnail)
									+ '/'
									+ resources
											.getResourceEntryName(R.drawable.nopicture_thumbnail));
							m_contactData.PHOTO_URI = uri.toString();
						}

						HashApplication.getDatabaseAdapter()
								.update_contact_name(id,
										m_contactData.DISPLAY_NAME);
						HashApplication.getDatabaseAdapter()
								.update_contact_localname(id, "");
						HashApplication.getDatabaseAdapter()
								.update_contact_icon(id,
										m_contactData.PHOTO_URI);
						HashApplication.getDatabaseAdapter()
								.update_contact_localicon(id, "");
					}

				}
				ContactViewerActivity.this.bindData();
			}
		});

		m_blockContactBtn = (Button) findViewById(R.id.blockContactBtn);
		
		if ( m_contactData.BLOCKED) {
			m_blockContactBtn.setText(R.string.unblock_contact);
		}
		else {
			m_blockContactBtn.setText(R.string.block_contact);
		}
		m_blockContactBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (m_contactData.BLOCKED) {
					HashApplication.getDatabaseAdapter().unblock_contact(
							m_contactData._ID);
					m_contactData.BLOCKED = false;
					m_blockContactBtn.setText(R.string.block_contact);
				} else {
					HashApplication.getDatabaseAdapter().block_contact(
							m_contactData._ID);
					m_contactData.BLOCKED = true;
					m_blockContactBtn.setText(R.string.unblock_contact);
				}

				ContactViewerActivity.this.bindData();
			}
		});

		bindData();

	}

	private void bindData() {
		if (m_contactData.DISPLAY_NAME != null) {
			m_contactEntryEdit.setText(m_contactData.DISPLAY_NAME);
		}
		if (m_contactData.PHOTO_URI != null) {
			Uri imguri = Uri.parse(m_contactData.PHOTO_URI);
			if (imguri != null) {
				m_contactEntryImage.setImageURI(imguri);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.contact_viewer, menu);
		return true;
	}

	private Uri outputFileUri;

	private void openImageIntent() {
		// Determine Uri of camera image to save.
		final File root = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "HashMessenger" + File.separator);
		root.mkdirs();
		final String fname = "fname"; // Utils.getUniqueImageFilename();
		final File sdImageMainDirectory = new File(root, fname);
		outputFileUri = Uri.fromFile(sdImageMainDirectory);

		// Camera.
		final List<Intent> cameraIntents = new ArrayList<Intent>();
		final Intent captureIntent = new Intent(
				android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		if (hasImageCaptureBug()) {
			captureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(Environment.getExternalStorageDirectory()));
		} else {
			captureIntent
					.putExtra(
							android.provider.MediaStore.EXTRA_OUTPUT,
							android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		}
		final PackageManager packageManager = getPackageManager();
		final List<ResolveInfo> listCam = packageManager.queryIntentActivities(
				captureIntent, 0);
		for (ResolveInfo res : listCam) {
			final String packageName = res.activityInfo.packageName;
			final Intent intent = new Intent(captureIntent);
			intent.setComponent(new ComponentName(res.activityInfo.packageName,
					res.activityInfo.name));
			intent.setPackage(packageName);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
			cameraIntents.add(intent);
		}

		// Filesystem.
		final Intent galleryIntent = new Intent();
		galleryIntent.setType("image/*");
		galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

		// Chooser of filesystem options.
		final Intent chooserIntent = Intent.createChooser(galleryIntent,
				"Select Source");

		// Add the camera options.
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
				cameraIntents.toArray(new Parcelable[] {}));

		startActivityForResult(chooserIntent, SELECT_PHOTO);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

		switch (requestCode) {
		case SELECT_PHOTO:
			if (resultCode == RESULT_OK) {
				final Uri imageUri = imageReturnedIntent.getData();
				if (imageUri != null) {
					m_contactEntryImage.setImageURI(imageUri);
					m_newPhotoUri = imageUri.toString();
				}

			}
		}
	}

	public boolean hasImageCaptureBug() {

		// list of known devices that have the bug
		ArrayList<String> devices = new ArrayList<String>();
		devices.add("android-devphone1/dream_devphone/dream");
		devices.add("generic/sdk/generic");
		devices.add("vodafone/vfpioneer/sapphire");
		devices.add("tmobile/kila/dream");
		devices.add("verizon/voles/sholes");
		devices.add("google_ion/google_ion/sapphire");

		return devices.contains(android.os.Build.BRAND + "/"
				+ android.os.Build.PRODUCT + "/" + android.os.Build.DEVICE);

	}

	@Override
	protected void onPause() {
		super.onPause();
		String inputText = m_contactEntryEdit.getText().toString();
		if (m_contactData.DISPLAY_NAME != null && inputText != null) {
			if (!m_contactData.DISPLAY_NAME.equals(inputText)) {
				HashApplication.getDatabaseAdapter().update_contact_localname(
						m_contactData._ID, inputText);
			}
		}
		if (m_contactData.PHOTO_URI != null && m_newPhotoUri != null) {
			if (!m_contactData.PHOTO_URI.equals(m_newPhotoUri)) {
				HashApplication.getDatabaseAdapter().update_contact_localicon(
						m_contactData._ID, m_newPhotoUri);
			}
		}
	}

}
