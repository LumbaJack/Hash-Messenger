package com.midas.hashmessenger;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import com.androidquery.AQuery;
import com.midas.hashmessenger.XMPPClient.NoConfigurationException;
import com.midas.hashmessenger.XMPPClient.NotConnectedException;
import com.midas.hashmessenger.api.NotificationsAPI;
import com.midas.hashmessenger.database.Tables.MessagesTable;
import com.midas.hashmessenger.messages.BaseMessage.HashMessageType;
import com.midas.hashmessenger.messages.HashMessage;
import com.midas.hashmessenger.widget.ChatAdapter;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class ChatActivity extends ListActivity implements
		XMPPClient.XMPPClientListener {
	private static final String TAG = ChatActivity.class.getName();
	private static final int VIEW_CONTACT_RESULT = 1001;

	/** Called when the activity is first created. */
	ArrayList<HashMessage> m_messages;
	ChatAdapter m_adapter;
	EditText m_text;
	private HashContactData m_contactData = null;
	private String m_username = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		SharedPreferences prefs = this.getSharedPreferences("APPSETTINGS",
				Activity.MODE_PRIVATE);

		m_username = prefs.getString("username", null);
		AQuery aq = new AQuery(this);

		HashApplication.getXmpp().setXMPPListener(this);

		aq.id(android.R.id.title).clicked(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Bundle extras = getIntent().getExtras();
				if (extras != null) {
					String contact_id = extras.getString("CONTACT_ID");
					Intent intent = new Intent(ChatActivity.this,
							ContactViewerActivity.class);
					intent.putExtra("CONTACT_ID", contact_id);
					ChatActivity.this.startActivity(intent);
				}
			}
		});

		m_text = (EditText) this.findViewById(R.id.text);

		bindData();

		if (m_contactData.USERID != null && !m_contactData.USERID.isEmpty()) {
			try {
				HashApplication.getXmpp().startChat(m_contactData.USERID);
			} catch (NoConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new AlertDialog.Builder(ChatActivity.this)
						.setTitle("Configuration does not exist")
						.setMessage("Please repeat the registration process")
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
									}
								}).show();
			}
		} else {
			new AlertDialog.Builder(this)
					.setTitle("Error User id is undefined")
					.setMessage("Unable to start chat")
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).show();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_viewcontact:
			Intent intent = new Intent(ChatActivity.this,
					ContactViewerActivity.class);
			final Bundle extras = getIntent().getExtras();
			if (extras != null) {
				String contact_id = extras.getString("CONTACT_ID");
				intent.putExtra("CONTACT_ID", contact_id);
				startActivityForResult(intent, VIEW_CONTACT_RESULT);
			}
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case VIEW_CONTACT_RESULT:
				// TODO
				break;
			}
		} else {
			// gracefully handle failure
			Log.w(TAG, "Warning: activity result not ok");
		}
	}

	public void sendMessage(View v) {
		final String newMessage = m_text.getText().toString().trim();
		if (newMessage.length() > 0) {
			m_text.setText("");

			this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						HashApplication.getXmpp().send(newMessage);
					} catch (NotConnectedException e) {
						new AlertDialog.Builder(ChatActivity.this)
								.setTitle("Error sending message")
								.setMessage("Doesn't look like we're connected")
								.setPositiveButton("OK",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
											}
										}).show();
					}
				}

			});

			addNewMessage(new HashMessage(newMessage, true));
		}
	}

	void addNewMessage(HashMessage m) {
		m_messages.add(m);
		m_adapter.notifyDataSetChanged();
		getListView().setSelection(m_messages.size() - 1);
	}

	private void bindData() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String contact_id = extras.getString("CONTACT_ID", null);
			String user_id = extras.getString("USER_ID", null);
			ArrayList<HashContactData> contactList;
			if (contact_id != null) {
				contactList = HashApplication.getDatabaseAdapter().get_contact(
						contact_id);
				if (contactList.size() > 0) {
					m_contactData = contactList.get(0);
				}
			} else if (user_id != null) {
				contactList = HashApplication.getDatabaseAdapter()
						.get_contact_by_userid(user_id);
				if (contactList.size() > 0) {
					m_contactData = contactList.get(0);
				}
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

		setTitle(m_contactData.DISPLAY_NAME);
		m_messages = new ArrayList<HashMessage>();
		Cursor cursor = HashApplication.getDatabaseAdapter().get_conversations(
				m_contactData.USERID);
		Date yesterday = null;
		while (cursor.moveToNext()) {
			String msgbody = cursor.getString(cursor
					.getColumnIndex(MessagesTable.MESSAGE_COLUMN_NAME));

			String from = cursor.getString(cursor
					.getColumnIndex(MessagesTable.SENDER_COLUMN_NAME));
			Long mdate = cursor.getLong(cursor
					.getColumnIndex(MessagesTable.MSGDATE_COLUMN_NAME));
			if (from == null || msgbody == null) {
				continue;
			}

			Date msgdate = new Date(mdate);

			Date currDate = msgdate;
			if (msgdate.before(currDate)) {
				currDate = msgdate;
			}

			SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd yyyy");
			if (yesterday == null) {
				m_messages.add(new HashMessage(HashMessageType.STATUS, sdf
						.format(currDate)));
				yesterday = currDate;
			} else {
				SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
				boolean sameday = (fmt.format(currDate).equals(fmt
						.format(yesterday)));
				if (!sameday) {
					m_messages.add(new HashMessage(HashMessageType.STATUS, sdf
							.format(currDate)));
					yesterday = currDate;
				}
			}
			boolean isMine = false;
			if (from.equals(m_username)) {
				isMine = true;
			}

			if (HistoryManager.getHistoryPassword() != null) {
				try {
					msgbody = CryptoManager.passwordDecrypt(msgbody);
					if (isMine) {
						m_messages.add(new HashMessage(HashMessageType.CHAT,
								msgbody, isMine, msgdate));
					} else {
						m_messages.add(new HashMessage(HashMessageType.CHAT,
								msgbody, isMine, msgdate));
					}
				} catch (Exception e) {
					e.printStackTrace();
					if (isMine) {
						m_messages.add(new HashMessage(HashMessageType.CHAT,
								msgbody + "\n" + e.getMessage(), isMine,
								msgdate));
					} else {
						m_messages.add(new HashMessage(HashMessageType.CHAT,
								msgbody + "\n" + e.getMessage(), isMine,
								msgdate));
					}
				}
			}
		}
		m_adapter = new ChatAdapter(this, m_messages);
		setListAdapter(m_adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		bindData();
	}

	@Override
	public void onConnect() {
	}

	@Override
	public void onConnectError(XMPPException ex) {
	}

	@Override
	public void onLoginError(XMPPException ex) {
	}

	@Override
	public void onChatCreated(Chat chat, boolean arg) {
	}

	@Override
	public void onProcessMessage(final Chat chat, final HashMessageData message) {
		final String msgbody = message.message.getBody();
		if (msgbody == null || msgbody.isEmpty()) {
			return;
		}

		if (XMPPClient.useridOnly(message.message.getSender()).equals(
				m_contactData.USERID)) {

			this.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					addNewMessage(new HashMessage(HashMessageType.CHAT, msgbody));
					HashApplication.getDatabaseAdapter().update_message_read(
							message);
				}

			});

		} else {

			NotificationsAPI Notification = new NotificationsAPI();
			Notification.sendNotification(m_contactData.USERID,
					"New Message from " + message.message.getSender(), 1,
					getBaseContext());
		}
	}

	@Override
	public void onMessageSent(HashMessageData[] msgs) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				/*
				 * new AlertDialog.Builder(ChatActivity.this)
				 * .setTitle("Send successful") .setMessage("Message Sent")
				 * .setPositiveButton("OK", new
				 * DialogInterface.OnClickListener() { public void
				 * onClick(DialogInterface dialog, int which) { } }).show();
				 */

			}
		});

	}

	@Override
	public void onMessageSentFail(final HashMessageData msg,
			final XMPPException ex) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new AlertDialog.Builder(ChatActivity.this)
						.setTitle(ex.getLocalizedMessage())
						.setMessage(ex.getStackTrace().toString())
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
									}
								}).show();

			}
		});
	}

	/*
	 * private class SendMessage extends AsyncTask<Void, String, String> {
	 * 
	 * @Override protected String doInBackground(Void... params) { try {
	 * Thread.sleep(2000); // simulate a network call } catch
	 * (InterruptedException e) { e.printStackTrace(); }
	 * 
	 * this.publishProgress(String.format("%s started writing", sender)); try {
	 * Thread.sleep(2000); // simulate a network call } catch
	 * (InterruptedException e) { e.printStackTrace(); }
	 * this.publishProgress(String.format("%s has entered text", sender)); try {
	 * Thread.sleep(3000);// simulate a network call } catch
	 * (InterruptedException e) { e.printStackTrace(); }
	 * 
	 * return avail_messages[rand.nextInt(avail_messages.length - 1)];
	 * 
	 * }
	 * 
	 * @Override public void onProgressUpdate(String... v) {
	 * 
	 * if (messages.get(messages.size() - 1).isStatusMessage)// check // wether
	 * we // have // already // added a // status // message {
	 * messages.get(messages.size() - 1).setMessage(v[0]); // update // the //
	 * status // for that adapter.notifyDataSetChanged();
	 * getListView().setSelection(messages.size() - 1); } else {
	 * addNewMessage(new ChatMessage(true, v[0])); // add new message, // if //
	 * there is no existing // status message } }
	 * 
	 * @Override protected void onPostExecute(String text) { if
	 * (messages.get(messages.size() - 1).isStatusMessage)// check if // there
	 * is // any // status // message, // now // remove // it. {
	 * messages.remove(messages.size() - 1); }
	 * 
	 * addNewMessage(new ChatMessage(text, false)); // add the orignal //
	 * message // from server. }
	 * 
	 * }
	 */

}
