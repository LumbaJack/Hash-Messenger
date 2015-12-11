package com.midas.hashmessenger;

import java.util.ArrayList;

import com.midas.hashmessenger.ContactsListFragment.ContactsFragmentDataListener;

import android.net.Uri;

import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class ContactListActivity extends BaseActivity implements
		ContactsFragmentDataListener {
	private static final String TAG = ContactListActivity.class.getName();
	private static final int CONTACT_PICKER_RESULT = 1001;
	private static String invite_email = "";
	private static String invite_subject = "";
	private static String invite_body = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_list);

		SharedPreferences prefs = getSharedPreferences("APPSETTINGS",
				Activity.MODE_PRIVATE);

		String username = prefs.getString("username", null);
		if (username != null) {
			setTitle("YOUR NUMBER IS " + username);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.contact_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_invite:
			Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
					Contacts.CONTENT_URI);
			startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
			return true;
		case R.id.action_settings:
			Intent settingsIntent = new Intent(ContactListActivity.this,
					SettingsActivity.class);
			ContactListActivity.this.startActivity(settingsIntent);
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case CONTACT_PICKER_RESULT:
				Cursor cursor = null;

				final AlertDialog.Builder menuAleart = new AlertDialog.Builder(
						ContactListActivity.this);
				Resources res = getResources();
				ContactListActivity.invite_subject = res
						.getString(R.string.invite_message_subject);
				ContactListActivity.invite_body = res
						.getString(R.string.invite_message_body);
				String[] allMenuListItems = res
						.getStringArray(R.array.invite_dialog_methods);
				String[] menuList;

				if (this.getPackageManager().hasSystemFeature(
						PackageManager.FEATURE_TELEPHONY)) {
					menuList = allMenuListItems;
				} else {
					menuList = new String[] { allMenuListItems[0] };
				}
				menuAleart.setTitle(R.string.invite_dialog_title);
				menuAleart.setItems(menuList,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								switch (item) {
								case 0:
									String to = ContactListActivity.invite_email;
									String subject = ContactListActivity.invite_subject;

									String message = ContactListActivity.invite_body;
									Intent email = new Intent(
											Intent.ACTION_SEND);
									email.putExtra(Intent.EXTRA_EMAIL,
											new String[] { to });
									email.putExtra(Intent.EXTRA_SUBJECT,
											subject);
									email.putExtra(Intent.EXTRA_TEXT, message);
									// need this to prompts email client only
									email.setType("message/rfc822");
									startActivity(Intent.createChooser(email,
											"Choose an Email client"));
									break;
								case 1:
									Intent sendIntent = new Intent(
											Intent.ACTION_VIEW);
									sendIntent.putExtra("sms_body",
											ContactListActivity.invite_body);
									sendIntent
											.setType("vnd.android-dir/mms-sms");
									startActivity(sendIntent);
									break;
								default:
									break;
								}
							}
						});
				AlertDialog menuDrop = menuAleart.create();
				menuDrop.show();

				break;
			}
		} else {
			// gracefully handle failure
			Log.w(TAG, "Warning: activity result not ok");
		}
	}

	@Override
	public ArrayList<HashContactData> onContactsListGetData() {
		return HashApplication.getDatabaseAdapter().get_contacts();
	}

	@Override
	public void onItemClicked(HashContactData item, int position, long id) {
		HashApplication.getDatabaseAdapter().create_chat(
				String.valueOf(item._ID));
		Intent intent = new Intent(ContactListActivity.this, ChatActivity.class);
		intent.putExtra("CONTACT_ID", String.valueOf(item._ID));
		ContactListActivity.this.startActivity(intent);
		finish();
	}

	@Override
	public boolean showHistory() {
		return false;
	}
}
