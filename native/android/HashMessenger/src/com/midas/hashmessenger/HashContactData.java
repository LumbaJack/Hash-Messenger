package com.midas.hashmessenger;

import java.util.Date;


/* our "struct" for holding contact data */
public class HashContactData {
	public String _ID = "";
	public String DISPLAY_NAME = "";
	public String PHOTO_URI = "";
	public String EMAIL = "";
	public String EMAIL_TYPE = "";
	public String USERID = "";
	public String PUBKEY = "";
	public int UNREAD_MESSAGES_COUNT = 0;
	public boolean BLOCKED = false;
	public String LASTMSG = "";
	public Date LASTDATEMSG = null;
	public HashContactData() {
	}
	
	HashContactData(String id, String name, String photo_uri, String userid) {
		this._ID = id;
		this.DISPLAY_NAME = name;
		this.PHOTO_URI = photo_uri;
		this.USERID = userid;
	}
	
	HashContactData(String id, String name, String photo_uri, String userid, int unreadcount) {
		this._ID = id;
		this.DISPLAY_NAME = name;
		this.PHOTO_URI = photo_uri;
		this.USERID = userid;
		this.UNREAD_MESSAGES_COUNT = unreadcount;
	}
	
	HashContactData(String id, String name, String photo_uri, String userid, int unreadcount, boolean blocked) {
		this._ID = id;
		this.DISPLAY_NAME = name;
		this.PHOTO_URI = photo_uri;
		this.USERID = userid;
		this.UNREAD_MESSAGES_COUNT = unreadcount;
		this.BLOCKED = blocked;
	}
}
