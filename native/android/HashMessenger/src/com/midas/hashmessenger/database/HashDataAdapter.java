package com.midas.hashmessenger.database;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smackx.packet.StreamInitiation.File;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.midas.hashmessenger.CryptoManager;
import com.midas.hashmessenger.HashConstants;
import com.midas.hashmessenger.HashContactData;
import com.midas.hashmessenger.HashMessageData;
import com.midas.hashmessenger.database.Tables.ChatsTable;
import com.midas.hashmessenger.database.Tables.ContactsTable;
import com.midas.hashmessenger.database.Tables.MessagesTable;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class HashDataAdapter {
	private SimpleDateFormat mSimpleDateFormat;

	private HashDatabaseHelper mHelper;
	private SQLiteDatabase db;
	private Context mContext;
	private SharedPreferences mPrefs;

	private class RetExtraData {
		private String msg;
		private int cnt;
		private Date msgdate;

		private RetExtraData(String string, Integer unreadcount, Date curdate) {
			this.msg = string;
			this.cnt = unreadcount;
			this.msgdate = curdate;
		}

	}

	public HashDataAdapter(Context context) {
		mContext = context;
		mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		mHelper = new HashDatabaseHelper(mContext);

		mPrefs = mContext.getSharedPreferences("APPSETTINGS",
				Activity.MODE_PRIVATE);

		// open the database is write mode so that
		// onCreate is called
		db = mHelper.getWritableDatabase();
		mHelper.initTables(db);
	}

	public void close() {
		db.close();
	}

	private String getCurentUserId() {
		return mPrefs.getString("username", null);
	}

	public long reset() {
		db.delete(MessagesTable.NAME, null, null);
		return Log.i(HashConstants.TAG, "data cleared");
	}

	public Cursor get_conversations(String from) {
		Cursor cursor;

		cursor = db
				.rawQuery(
						String.format(
								"select * from (select * from %s where %s = ? or %s = ? order by id desc limit 15) order by id asc",
								MessagesTable.NAME,
								MessagesTable.SENDER_COLUMN_NAME,
								MessagesTable.RECEIVER_COLUMN_NAME),
						new String[] { from, from });

		return cursor;

	}

	synchronized public int delete_conversations() {
		return db.delete(MessagesTable.NAME, null, null);
	}

	public Cursor get_chats() {
		Cursor cursor;
		cursor = db.rawQuery(String.format(
				"SELECT * FROM %s, %s WHERE %s.%s = %s.%s", ChatsTable.NAME,
				ContactsTable.NAME, ContactsTable.NAME,
				ContactsTable.USERID_COLUMN_NAME, ChatsTable.NAME,
				ChatsTable.USERID_COLUMN_NAME), null);

		return cursor;
	}

	public Cursor get_chats(String from) {
		Cursor cursor;

		cursor = db.rawQuery(String.format(
				"SELECT * FROM %s, %s WHERE %s.%s = %s.%s AND %s.%s = \"%s\"",
				ChatsTable.NAME, ContactsTable.NAME, ContactsTable.NAME,
				ContactsTable.USERID_COLUMN_NAME, ChatsTable.NAME,
				ChatsTable.USERID_COLUMN_NAME, ChatsTable.NAME,
				ChatsTable.USERID_COLUMN_NAME, from), null);

		return cursor;
	}

	private HashContactData getHashContactData(Cursor cursor) {
		return getHashContactData(cursor, false);
	}

	private HashContactData getHashContactData(Cursor cursor,
			boolean GetExtraData) {
		String localid = cursor.getString(cursor
				.getColumnIndex(ContactsTable.LOCALID_COLUMN_NAME));
		if (localid == null)
			return null;
		localid = localid.trim();
		if (localid.isEmpty()) {
			return null;
		}
		String displayName = cursor.getString(cursor
				.getColumnIndex(ContactsTable.DISPLAYNAME_COLUMN_NAME));
		String localName = cursor.getString(cursor
				.getColumnIndex(ContactsTable.LOCALNAME_COLUMN_NAME));
		String icon = cursor.getString(cursor
				.getColumnIndex(ContactsTable.ICON_COLUMN_NAME));
		String localIcon = cursor.getString(cursor
				.getColumnIndex(ContactsTable.LOCALICON_COLUMN_NAME));
		String pubKey = cursor.getString(cursor
				.getColumnIndex(ContactsTable.PUBKEY_COLUMN_NAME));
		String uid = cursor.getString(cursor
				.getColumnIndex(ContactsTable.USERID_COLUMN_NAME));
		int blockedint = cursor.getInt(cursor
				.getColumnIndex(ContactsTable.BLOCKED_COLUMN_NAME));

		displayName = displayName == null ? "" : displayName.trim();
		localName = localName == null ? "" : localName.trim();
		icon = icon == null ? "" : icon.trim();
		localIcon = localIcon == null ? "" : localIcon.trim();
		pubKey = pubKey == null ? "" : pubKey.trim();
		uid = uid == null ? "" : uid.trim();

		HashContactData newdata = new HashContactData();
		newdata._ID = localid;

		if (!localName.isEmpty()) {
			newdata.DISPLAY_NAME = localName;
		} else {
			newdata.DISPLAY_NAME = displayName;
		}
		if (!localIcon.isEmpty()) {
			newdata.PHOTO_URI = localIcon;
		} else {
			newdata.PHOTO_URI = icon;
		}
		newdata.PUBKEY = pubKey;
		newdata.USERID = uid;
		if (GetExtraData) {

			RetExtraData extdata = get_unread_message_count(uid);
			newdata.UNREAD_MESSAGES_COUNT = extdata.cnt;

			try {
				String msgbody = CryptoManager.passwordDecrypt(extdata.msg);
				newdata.LASTMSG = msgbody;
			} catch (Exception e) {
				e.printStackTrace();
				newdata.LASTMSG = extdata.msg; // show encryped text so we can
												// spot problems
			}

			newdata.LASTDATEMSG = extdata.msgdate;
		}
		newdata.BLOCKED = blockedint > 0 ? true : false;
		return newdata;
	}

	private RetExtraData get_unread_message_count(String uid) {
		Integer unreadcount = 0;
		String msg = "";
		Date msgdate = null;
		Cursor cursor = null;
		cursor = db
				.rawQuery(
						String.format(
								"SELECT %s, %s, count(case when %s = %s then 1 else null end) as cnt  FROM %s WHERE %s = ? or %s = ?",
								MessagesTable.MESSAGE_COLUMN_NAME,
								MessagesTable.MSGDATE_COLUMN_NAME,
								MessagesTable.STATUS_COLUMN_NAME,
								MessagesTable.MSGSTATUS.MSGRECEIVEUNREAD
										.getCode(), MessagesTable.NAME,
								MessagesTable.SENDER_COLUMN_NAME,
								MessagesTable.RECEIVER_COLUMN_NAME),
						new String[] { uid, uid });

		if (cursor.moveToNext()) {
			unreadcount = cursor.getInt(cursor.getColumnIndex("cnt"));
			msg = cursor.getString(cursor
					.getColumnIndex(MessagesTable.MESSAGE_COLUMN_NAME));
			msgdate = new Date((long) cursor.getDouble(cursor
					.getColumnIndex(MessagesTable.MSGDATE_COLUMN_NAME)));
		}
		return new RetExtraData(msg, unreadcount, msgdate);

	}

	public ArrayList<HashContactData> get_chat_contacts() {
		Cursor cursor = null;
		String sql = String.format("SELECT * FROM %s, %s WHERE %s.%s = %s.%s",
				ChatsTable.NAME, ContactsTable.NAME, ContactsTable.NAME,
				ContactsTable.LOCALID_COLUMN_NAME, ChatsTable.NAME,
				ChatsTable.USERID_COLUMN_NAME);

		cursor = db.rawQuery(sql, null);

		ArrayList<HashContactData> result = new ArrayList<HashContactData>();
		while (cursor.moveToNext()) {
			HashContactData newdata = getHashContactData(cursor, true);
			if (newdata == null) {
				continue;
			}
			result.add(newdata);
		}
		return result;
	}

	public long create_chat(String contactid) {
		return create_chat(contactid, "");
	}

	public long create_chat(String contactid, String subject) {
		long rowid = -1;
		long nowtime = new Date().getTime();
		ContentValues valmap = new ContentValues();
		valmap.put(ChatsTable.USERID_COLUMN_NAME, contactid);
		valmap.put(ChatsTable.SUBJECT_COLUMN_NAME, subject);
		valmap.put(ChatsTable.CREATED_AT_COLUMN_NAME, nowtime);
		valmap.put(ChatsTable.UPDATED_AT_COLUMN_NAME, nowtime);

		rowid = db.replaceOrThrow(ChatsTable.NAME, null, valmap);

		return rowid;
	}

	public long insert_message(String sender, String receiver, String subject,
			String message, Date msgdate, int status) {
		long rowid = -1;

		ContentValues valmap = new ContentValues();
		valmap.put(MessagesTable.SENDER_COLUMN_NAME, sender);
		valmap.put(MessagesTable.RECEIVER_COLUMN_NAME, receiver);
		valmap.put(MessagesTable.SUBJECT_COLUMN_NAME, subject);
		valmap.put(MessagesTable.MESSAGE_COLUMN_NAME, message);
		valmap.put(MessagesTable.MSGDATE_COLUMN_NAME, msgdate.getTime());
		valmap.put(MessagesTable.STATUS_COLUMN_NAME, status);
		rowid = db.insertOrThrow(MessagesTable.NAME, null, valmap);

		return rowid;
	}

	public synchronized long update_message_read(HashMessageData msg) {

		ContentValues valmap = new ContentValues();
		// valmap.put(MessagesTable.READAT_COLUMN_NAME, msg.readat.getTime());
		valmap.put(MessagesTable.STATUS_COLUMN_NAME,
				MessagesTable.MSGSTATUS.MSGRECEIVEREAD.getCode());
		return db.update(MessagesTable.NAME, valmap, String.format(
				"%s == rowid", MessagesTable.ID_COLUMN_NAME, msg.rowid), null);
	}

	public synchronized long update_message_sent(HashMessageData msg) {

		ContentValues valmap = new ContentValues();
		valmap.put(MessagesTable.MSGDATE_COLUMN_NAME, msg.sentat.getTime());
		valmap.put(MessagesTable.STATUS_COLUMN_NAME,
				MessagesTable.MSGSTATUS.MSGSENTREAD.getCode());

		return db.update(MessagesTable.NAME, valmap, String.format(
				"%s == rowid", MessagesTable.ID_COLUMN_NAME, msg.rowid), null);
	}

	public synchronized long[] sync_contacts(JSONArray data)
			throws JSONException {
		List<Long> rowids = new ArrayList<Long>();

		if (data != null) {
			for (int i = 0; i < data.length(); i++) {
				ContentValues valmap = new ContentValues();

				JSONObject rec = data.getJSONObject(i);
				valmap.put(ContactsTable.LOCALID_COLUMN_NAME,
						rec.getString("id"));
				valmap.put(ContactsTable.DISPLAYNAME_COLUMN_NAME,
						rec.getString("disp"));
				valmap.put(ContactsTable.ICON_COLUMN_NAME,
						rec.getString("icon"));
				valmap.put(ContactsTable.PUBKEY_COLUMN_NAME,
						rec.getString("pubkey"));
				valmap.put(ContactsTable.USERID_COLUMN_NAME,
						rec.getString("phone"));

				long rowid = db
						.replaceOrThrow(ContactsTable.NAME, null, valmap);
				rowids.add(rowid);

			}
		}

		long[] results = new long[rowids.size()];
		for (int index = 0; index < rowids.size(); index++) {
			results[index] = rowids.get(index);
		}
		return results;
	}

	public long update_contact_name(String contactid, String name) {
		long rowid = -1;

		ContentValues valmap = new ContentValues();
		valmap.put(ContactsTable.DISPLAYNAME_COLUMN_NAME, name);

		rowid = db.update(ContactsTable.NAME, valmap,
				ContactsTable.DISPLAYNAME_COLUMN_NAME + " = ?",
				new String[] { contactid });

		return rowid;
	}

	public long update_contact_localname(String contactid, String localname) {
		long rowid = -1;

		ContentValues valmap = new ContentValues();
		valmap.put(ContactsTable.LOCALNAME_COLUMN_NAME, localname);

		rowid = db.update(ContactsTable.NAME, valmap,
				ContactsTable.LOCALID_COLUMN_NAME + " = ?",
				new String[] { contactid });

		return rowid;
	}

	public long update_contact_icon(String contactid, String icon) {
		long rowid = -1;

		ContentValues valmap = new ContentValues();
		valmap.put(ContactsTable.ICON_COLUMN_NAME, icon);

		rowid = db.update(ContactsTable.NAME, valmap,
				ContactsTable.LOCALID_COLUMN_NAME + " = ?",
				new String[] { contactid });

		return rowid;
	}

	public long update_contact_localicon(String contactid, String icon) {
		long rowid = -1;

		ContentValues valmap = new ContentValues();
		valmap.put(ContactsTable.LOCALICON_COLUMN_NAME, icon);

		rowid = db.update(ContactsTable.NAME, valmap,
				ContactsTable.LOCALID_COLUMN_NAME + " = ?",
				new String[] { contactid });

		return rowid;
	}

	public long block_contact(String contactid) {
		long rowid = -1;

		ContentValues valmap = new ContentValues();
		valmap.put(ContactsTable.BLOCKED_COLUMN_NAME, 1);

		rowid = db.update(ContactsTable.NAME, valmap,
				ContactsTable.LOCALID_COLUMN_NAME + " = ?",
				new String[] { contactid });

		return rowid;
	}

	public long unblock_contact(String contactid) {
		long rowid = -1;

		ContentValues valmap = new ContentValues();
		valmap.put(ContactsTable.BLOCKED_COLUMN_NAME, 0);

		rowid = db.update(ContactsTable.NAME, valmap,
				ContactsTable.LOCALID_COLUMN_NAME + " = ?",
				new String[] { contactid });

		return rowid;
	}

	public ArrayList<HashContactData> get_contact(String userid) {
		Cursor cursor = null;
		cursor = db.rawQuery(String.format("SELECT * FROM %s WHERE %s = ?",
				ContactsTable.NAME, ContactsTable.LOCALID_COLUMN_NAME),
				new String[] { userid });
		ArrayList<HashContactData> result = new ArrayList<HashContactData>();
		while (cursor.moveToNext()) {
			HashContactData newdata = getHashContactData(cursor);
			result.add(newdata);
		}

		return result;
	}

	public ArrayList<HashContactData> get_contact_by_userid(String userid) {
		Cursor cursor = null;
		cursor = db.rawQuery(String.format("SELECT * FROM %s WHERE %s = ?",
				ContactsTable.NAME, ContactsTable.USERID_COLUMN_NAME),
				new String[] { userid });
		ArrayList<HashContactData> result = new ArrayList<HashContactData>();
		while (cursor.moveToNext()) {
			HashContactData newdata = getHashContactData(cursor);
			result.add(newdata);
		}

		return result;
	}

	public ArrayList<HashContactData> get_contacts() {
		Cursor cursor = null;
		String selfid = getCurentUserId();

		if (selfid != null) {
			cursor = db.rawQuery(String.format(
					"SELECT * FROM %s WHERE %s = ? and %s != ?",
					ContactsTable.NAME, ContactsTable.BLOCKED_COLUMN_NAME,
					ContactsTable.USERID_COLUMN_NAME), new String[] { "0",
					selfid });
		} else {
			cursor = db.rawQuery(String.format(
					"SELECT * FROM %s WHERE %s = ? ", ContactsTable.NAME,
					ContactsTable.BLOCKED_COLUMN_NAME), new String[] { "0" });
		}

		ArrayList<HashContactData> result = new ArrayList<HashContactData>();
		while (cursor.moveToNext()) {
			HashContactData newdata = getHashContactData(cursor);
			result.add(newdata);
		}

		return result;
	}

	public ArrayList<HashContactData> get_contacts_blocked() {
		Cursor cursor = null;
		String selfid = getCurentUserId();

		if (selfid != null) {
			cursor = db.rawQuery(String.format(
					"SELECT * FROM %s WHERE %s = ? and %s != ?",
					ContactsTable.NAME, ContactsTable.BLOCKED_COLUMN_NAME,
					ContactsTable.USERID_COLUMN_NAME), new String[] { "1",
					selfid });
		} else {
			cursor = db.rawQuery(String.format(
					"SELECT * FROM %s WHERE %s = ? ", ContactsTable.NAME,
					ContactsTable.BLOCKED_COLUMN_NAME), new String[] { "1" });
		}
		ArrayList<HashContactData> result = new ArrayList<HashContactData>();
		while (cursor.moveToNext()) {
			HashContactData newdata = getHashContactData(cursor);
			result.add(newdata);
		}

		return result;
	}

	public String get_public_key(String contactid) {

		Cursor cursor = db.rawQuery(String.format(
				"SELECT %s FROM %s WHERE %s = ? ",
				ContactsTable.PUBKEY_COLUMN_NAME, ContactsTable.NAME,
				ContactsTable.USERID_COLUMN_NAME), new String[] { contactid });
		String pubkeystr = null;
		while (cursor.moveToNext()) {
			pubkeystr = cursor.getString(cursor
					.getColumnIndex(ContactsTable.PUBKEY_COLUMN_NAME));
			break;
		}
		return pubkeystr;
	}

	public int reencrypt_messages(byte[] prevPassword, byte[] newPassword) {
		Cursor cursor = db.query(MessagesTable.NAME,
				new String[] { MessagesTable.ID_COLUMN_NAME,
						MessagesTable.MESSAGE_COLUMN_NAME }, null, null, null,
				null, null);

		// TODO this can probably done more efficiently, but I'm tired

		Hashtable<Integer, String> values = new Hashtable<Integer, String>();
		while (cursor.moveToNext()) {
			int id = cursor.getInt(cursor
					.getColumnIndex(MessagesTable.ID_COLUMN_NAME));
			String msgtxt = cursor.getString(cursor
					.getColumnIndex(MessagesTable.MESSAGE_COLUMN_NAME));

			try {
				String cleartxt = CryptoManager.passwordDecrypt(prevPassword,
						msgtxt);
				String newmsg = CryptoManager.passwordEncrypt(newPassword,
						cleartxt);
				values.put(id, newmsg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		int updatecount = 0;
		for (Integer key : values.keySet()) {
			ContentValues valmap = new ContentValues();
			valmap.put(MessagesTable.MESSAGE_COLUMN_NAME, values.get(key));

			updatecount += db.update(MessagesTable.NAME, valmap, "id = ?",
					new String[] { key.toString() });
		}

		return updatecount;
	}
}
