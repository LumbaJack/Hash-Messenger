package com.midas.hashmessenger.messages;

import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.util.Log;

/**
 * Our Hash specific message format
 * 
 */
public abstract class BaseMessage {

	public enum HashMessageType {
		CHAT, STATUS
	}

	private Long m_expires_seconds = null;
	private Message m_message = null;
	private String m_subject = null;
	private String m_body = null;
	private String m_fakeMessage = null;
	private String m_imgUrl = null;
	private String m_audioUrl = null;
	private String m_videoUrl = null;
	private HashMessageType m_messageType = HashMessageType.CHAT;
	private boolean m_isMine = false;
	private Date m_date = null;
	private String m_sender = null;
	private String m_recipient = null;
	private Document m_document = null;
	private Element m_root = null;

	public Long getExpiresSeconds() {
		return m_expires_seconds;
	}

	public void setExpiresSeconds(Long m_expires_seconds) {
		this.m_expires_seconds = m_expires_seconds;
	}

	public Message getMessage() {
		if (m_message == null) {
			m_message = new Message();
			m_message.setTo(getRecipient());
			m_message.setSubject(getSubject());
			m_message.setBody(getBody());
			m_message.setType(Type.chat);

			HmExtension hm = new HmExtension(this);
			//m_message.getPacketID(), m_message);
			hm.set_fakeMessage("Hello world");
			hm.set_expires_seconds(100L);
			m_message.addExtension(hm);
		}
		return m_message;
	}

	public void setMessage(Message message) {
		this.m_message = message;
	}

	public String getFakeMessage() {
		return m_fakeMessage;
	}

	public void setFakeMessage(String m_fakeMessage) {
		this.m_fakeMessage = m_fakeMessage;
	}

	public String getSubject() {
		return m_subject;
	}

	public void setSubject(String subject) {
		this.m_subject = subject;
	}

	public String getBody() {
		return m_body;
	}

	public void setBody(String body) {
		this.m_body = body;
	}

	public String getImgUrl() {
		return m_imgUrl;
	}

	public void setImgUrl(String m_imgUrl) {
		this.m_imgUrl = m_imgUrl;
	}

	public String getAudioUrl() {
		return m_audioUrl;
	}

	public void setAudioUrl(String m_audioUrl) {
		this.m_audioUrl = m_audioUrl;
	}

	public String getVideoUrl() {
		return m_videoUrl;
	}

	public void setVideoUrl(String m_videoUrl) {
		this.m_videoUrl = m_videoUrl;
	}

	public HashMessageType getMessageType() {
		return m_messageType;
	}

	public void setMessageType(HashMessageType msgtype) {
		this.m_messageType = msgtype;
	}

	public boolean isMine() {
		return m_isMine;
	}

	public void setIsMine(boolean mine) {
		this.m_isMine = mine;
	}

	public Date getDate() {
		return m_date;
	}

	public void setDate(Date date) {
		this.m_date = date;
	}

	public String getSender() {
		return m_sender;
	}

	public void setSender(String sender) {
		this.m_sender = sender;
	}

	public String getRecipient() {
		return m_recipient;
	}

	public void setRecipient(String recipient) {
		this.m_recipient = recipient;
	}

	public boolean isStatusMessage() {
		return (this.m_messageType == HashMessageType.STATUS);
	}
}
