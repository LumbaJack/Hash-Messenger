package com.midas.hashmessenger;

import java.util.ArrayList;

import org.json.JSONArray;

import com.midas.hashmessenger.ContactsListFragment.ContactsFragmentDataListener;
import com.midas.hashmessenger.HashApiClient.SyncContactsListener;
import com.midas.hashmessenger.dialogs.PromptPasswordDialog;
import com.midas.hashmessenger.dialogs.PromptPasswordDialog.PromptPasswordDialogListener;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

public class ChatListActivity extends BaseActivity implements
		ContactsFragmentDataListener, SyncContactsListener,
		PromptPasswordDialogListener {
	private static final String TAG = ChatListActivity.class.getName();
	private boolean m_displayHistory = false;
	private LinearLayout m_unlockHistory = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat_list);

		m_unlockHistory = (LinearLayout) this.findViewById(R.id.unlockHistory);
	}

	private void refreshUi() {
		if (HistoryManager.hasUserHistoryPassword()
				&& !HistoryManager.isUserHistoryAuthenticated()) {
			m_unlockHistory.setVisibility(View.VISIBLE);
		} else {
			m_unlockHistory.setVisibility(View.GONE);
			m_displayHistory = true;
		}

		m_unlockHistory.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fm = getSupportFragmentManager();
				PromptPasswordDialog promptPasswordDialog = new PromptPasswordDialog();
				promptPasswordDialog.show(fm, "dialog_promptpassword");
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences prefs = getSharedPreferences("APPSETTINGS",
				Activity.MODE_PRIVATE);
		String value = prefs.getString("EULA", "");
		if (!value.equals("true")) {
			Intent intent = new Intent(this, eula.class);
			startActivity(intent);
		}
		HashApiClient apiClient = new HashApiClient(ChatListActivity.this);
		apiClient.syncContacts(HashApplication.getDatabaseAdapter(),
				ChatListActivity.this);

		Log.i(TAG, "onResume");

		refreshUi();
	}

	@Override
	protected void onStop() {
		HashApplication.XMMPClose();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_addchat:
			Intent chatIntent = new Intent(ChatListActivity.this,
					ContactListActivity.class);
			ChatListActivity.this.startActivity(chatIntent);
			return true;
		case R.id.action_settings:
			Intent settingsIntent = new Intent(ChatListActivity.this,
					SettingsActivity.class);
			ChatListActivity.this.startActivity(settingsIntent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public ArrayList<HashContactData> onContactsListGetData() {
		return HashApplication.getDatabaseAdapter().get_chat_contacts();
	}

	@Override
	public void onItemClicked(HashContactData item, int position, long id) {
		HashApplication.getDatabaseAdapter().create_chat(
				String.valueOf(item._ID));
		Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
		intent.putExtra("CONTACT_ID", String.valueOf(item._ID));
		ChatListActivity.this.startActivity(intent);

	}

	@Override
	public void onSyncContactsComplete(JSONArray sync_results) {

	}

	@Override
	public void onPromptPasswordDialogPositive(String cleartext) {
		HistoryManager.authenticateUserHistory(cleartext);
		ContactsListFragment frag = (ContactsListFragment) getSupportFragmentManager()
				.findFragmentById(R.id.listFragment);
		if (frag != null) {
			frag.bindData();
		}
		refreshUi();
	}

	@Override
	public void onPromptPasswordDialogNegative() {
		AlertDialog.Builder b = new AlertDialog.Builder(this)
			.setTitle(R.string.wrong_password)
			.setMessage(R.string.wrong_password)
			.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				})
		;
		b.create().show();
	}

	@Override
	public boolean showHistory() {
		return m_displayHistory;
	}

}
