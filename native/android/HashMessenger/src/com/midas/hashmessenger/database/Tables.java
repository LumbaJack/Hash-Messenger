package com.midas.hashmessenger.database;

public class Tables {

	/*
	 * Put table info in a class so that its easier to use with intellisense
	 */
	public static class ChatsTable {
		public static final String NAME = "tbl_chats";
		public static final String USERID_COLUMN_NAME = "userid";
		public static final String SUBJECT_COLUMN_NAME = "subject";
		public static final String CREATED_AT_COLUMN_NAME = "created_at";
		public static final String UPDATED_AT_COLUMN_NAME = "updated_at";

		public static String createTableSQL() {
			return "CREATE TABLE IF NOT EXISTS " + ChatsTable.NAME + " ("
					+ ChatsTable.USERID_COLUMN_NAME
					+ " INTEGER NOT NULL PRIMARY KEY,"
					+ ChatsTable.SUBJECT_COLUMN_NAME + " TEXT,"
					+ ChatsTable.CREATED_AT_COLUMN_NAME + " REAL,"
					+ ChatsTable.UPDATED_AT_COLUMN_NAME + " REAL" + ")";
		}
	}

	public static class MessagesTable {
		public static final String NAME = "tbl_messages";
		public static final String ID_COLUMN_NAME = "id";
		public static final String SENDER_COLUMN_NAME = "sender";
		public static final String RECEIVER_COLUMN_NAME = "receiver";
		public static final String SUBJECT_COLUMN_NAME = "subject";
		public static final String MESSAGE_COLUMN_NAME = "message";
		public static final String MSGDATE_COLUMN_NAME = "msgdate";
		public static final String STATUS_COLUMN_NAME = "status";

		public static enum MSGSTATUS {
			MSGUNSENT(0), MSGSENTUNREAD(1), MSGSENTREAD(2), MSGRECEIVEUNREAD(3), MSGRECEIVEREAD(4);
			private int code;
			private MSGSTATUS(int c) {
				code = c;
			}
			public int getCode() {
				return code;
			}
		}

		public static String createTableSQL() {
			return "CREATE TABLE IF NOT EXISTS " + MessagesTable.NAME + " ("
					+ MessagesTable.ID_COLUMN_NAME
					+ " INTEGER NOT NULL PRIMARY KEY,"
					+ MessagesTable.SENDER_COLUMN_NAME + " TEXT,"
					+ MessagesTable.RECEIVER_COLUMN_NAME + " TEXT,"
					+ MessagesTable.SUBJECT_COLUMN_NAME + " TEXT,"
					+ MessagesTable.MESSAGE_COLUMN_NAME + " TEXT,"
					+ MessagesTable.MSGDATE_COLUMN_NAME + " REAL,"
					+ MessagesTable.STATUS_COLUMN_NAME + " INTEGER" + ")";
		}
	}

	public static class ContactsTable {
		public static final String NAME = "tbl_contacts";
		public static final String LOCALID_COLUMN_NAME = "localid";
		public static final String USERID_COLUMN_NAME = "userid";
		public static final String DISPLAYNAME_COLUMN_NAME = "displayname";
		public static final String LOCALNAME_COLUMN_NAME = "localname";
		public static final String ICON_COLUMN_NAME = "icon";
		public static final String LOCALICON_COLUMN_NAME = "localicon";
		public static final String PUBKEY_COLUMN_NAME = "pubkey";
		public static final String BLOCKED_COLUMN_NAME = "blocked";

		public static String createTableSQL() {
			return "CREATE TABLE IF NOT EXISTS " + ContactsTable.NAME + " ("
					+ ContactsTable.LOCALID_COLUMN_NAME + " INTEGER,"
					+ ContactsTable.USERID_COLUMN_NAME + " INTEGER,"
					+ ContactsTable.DISPLAYNAME_COLUMN_NAME + " TEXT,"
					+ ContactsTable.LOCALNAME_COLUMN_NAME + " TEXT,"
					+ ContactsTable.ICON_COLUMN_NAME + " TEXT,"
					+ ContactsTable.LOCALICON_COLUMN_NAME + " TEXT,"
					+ ContactsTable.PUBKEY_COLUMN_NAME + " TEXT,"
					+ ContactsTable.BLOCKED_COLUMN_NAME + " INTEGER DEFAULT 0,"
					+ " PRIMARY KEY(" + ContactsTable.USERID_COLUMN_NAME
					+ ") ) ";

		}
	}

	public static class SettingsTable {
		public static final String NAME = "tbl_settings";
		public static final String KEY_COLUMN_NAME = "key";
		public static final String VALUE_COLUMN_NAME = "value";

		public static String createTableSQL() {
			return "CREATE TABLE IF NOT EXISTS " + SettingsTable.NAME + " ("
					+ SettingsTable.KEY_COLUMN_NAME + " TEXT,"
					+ SettingsTable.VALUE_COLUMN_NAME + " TEXT,"
					+ " PRIMARY KEY(" + SettingsTable.KEY_COLUMN_NAME + ") ) ";
		}
	}

}
