package com.midas.hashmessenger;

import java.util.ArrayList;

import com.midas.hashmessenger.ContactsListFragment.ContactsFragmentDataListener;

import android.os.Bundle;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;

public class ContactBlockedListActivity extends BaseActivity implements
		ContactsFragmentDataListener {
	private static final String TAG = ContactBlockedListActivity.class
			.getName();

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
	public ArrayList<HashContactData> onContactsListGetData() {
		return HashApplication.getDatabaseAdapter().get_contacts_blocked();
	}

	@Override
	public void onItemClicked(HashContactData item, int position, long id) {
		HashApplication.getDatabaseAdapter().create_chat(
				String.valueOf(item._ID));
		Intent intent = new Intent(ContactBlockedListActivity.this,
				ContactViewerActivity.class);
		intent.putExtra("CONTACT_ID", String.valueOf(item._ID));
		ContactBlockedListActivity.this.startActivity(intent);
	}

	@Override
	public boolean showHistory() {
		return false;
	}
}
