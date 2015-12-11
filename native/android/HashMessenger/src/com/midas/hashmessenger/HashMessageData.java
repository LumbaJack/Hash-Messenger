package com.midas.hashmessenger;

import java.util.Date;
import com.midas.hashmessenger.messages.HashMessage;

/* our "struct" for holding contact data */
public class HashMessageData {
	public long rowid;
	public HashMessage message;
	public Date readat = new Date(0);
	public Date sentat = new Date(0);
	

	public HashMessageData() {
	}
}
