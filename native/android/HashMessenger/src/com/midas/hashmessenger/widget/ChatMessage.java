package com.midas.hashmessenger.widget;

import java.util.Date;

public class ChatMessage {

	String message;

	private boolean isMine;
	private ChatMessageType messageType;
	private Date mDate;

	public enum ChatMessageType {
		CHAT, STATUS
	}

	public ChatMessage(String message, boolean isMine) {
		super();
		this.message = message;
		this.isMine = isMine;
		this.messageType = ChatMessageType.CHAT;
		this.mDate = null;
	}

	public ChatMessage(ChatMessageType type, String message) {
		super();
		this.message = message;
		this.isMine = false;
		this.messageType = type;
		this.mDate = null;
	}

	public ChatMessage(ChatMessageType type, String message, boolean isMine) {
		super();
		this.message = message;
		this.isMine = isMine;
		this.messageType = type;
		this.mDate = null;
	}

	public ChatMessage(ChatMessageType type, String message, boolean isMine, Date date) {
		super();
		this.message = message;
		this.isMine = isMine;
		this.messageType = type;
		this.mDate = date;
	}
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Date getDate() {
		return mDate;
	}

	public void setDate(Date date) {
		this.mDate = date;
	}
	
	public boolean isMine() {
		return isMine;
	}

	public void setMine(boolean isMine) {
		this.isMine = isMine;
	}

	public boolean isStatusMessage() {
		return (this.messageType == ChatMessageType.STATUS);
	}

	public void setMessageType(ChatMessageType type) {
		this.messageType = type;
	}

}
