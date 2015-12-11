package com.midas.hashmessenger.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.midas.hashmessenger.HashConstants;
import com.midas.hashmessenger.database.Tables.ChatsTable;
import com.midas.hashmessenger.database.Tables.ContactsTable;
import com.midas.hashmessenger.database.Tables.MessagesTable;
import com.midas.hashmessenger.database.Tables.SettingsTable;

/**
 * This class helps open, create, and upgrade the database file.
 */
public class HashDatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "hash_messenger.db";
	private static final int DATABASE_VERSION = 1;
	
	HashDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void initTables(SQLiteDatabase db) {
		db.execSQL(ChatsTable.createTableSQL());
		db.execSQL(ContactsTable.createTableSQL());
		db.execSQL(MessagesTable.createTableSQL());
		db.execSQL(SettingsTable.createTableSQL());
	}

	/**
	 * all deletes in one place so they don't get lost
	 * 
	 * @param db
	 */
	public void dropTables(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS " + ChatsTable.NAME);
		db.execSQL("DROP TABLE IF EXISTS " + ContactsTable.NAME);
		db.execSQL("DROP TABLE IF EXISTS " + MessagesTable.NAME);
		db.execSQL("DROP TABLE IF EXISTS " + SettingsTable.NAME);
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);

		if (!db.isReadOnly()) {
			initTables(db);
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(HashConstants.TAG, "HashDataAdapter onCreate");
		initTables(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(HashConstants.TAG, "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");

		// nuke all tables
		dropTables(db);
		onCreate(db);
	}

}