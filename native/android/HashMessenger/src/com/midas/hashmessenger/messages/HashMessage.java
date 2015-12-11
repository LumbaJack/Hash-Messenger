package com.midas.hashmessenger.messages;

import java.util.Date;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Our Hash specific message format
 * 
 */
public class HashMessage extends BaseMessage {

	public HashMessage(String message, boolean isMine) {
		this.setIsMine(isMine);
		this.setBody(message);
		this.setMessageType(HashMessageType.CHAT);
		this.setDate(null);
	}

	public HashMessage(HashMessageType type, String message) {
		this.setIsMine(false);
		this.setBody(message);
		this.setMessageType(type);
		this.setDate(null);
	}

	public HashMessage(HashMessageType type, String message, boolean isMine) {
		this.setIsMine(isMine);
		this.setBody(message);
		this.setMessageType(type);
		this.setDate(null);
	}

	public HashMessage(HashMessageType type, String message, boolean isMine,
			Date msgdate) {
		this.setIsMine(isMine);
		this.setBody(message);
		this.setMessageType(type);
		this.setDate(msgdate);
	}

	public HashMessage(String m_recipient, HashMessageType type) {
		this.setRecipient(m_recipient);
		this.setMessageType(type);
		this.setDate(null);
	}

	public HashMessage(Message xmppmsg) {
		this.setSender(xmppmsg.getFrom());
		this.setRecipient(xmppmsg.getTo());
		this.setBody(xmppmsg.getBody());
		this.setSubject(xmppmsg.getSubject());

		PacketExtension pe = xmppmsg.getExtension(HmExtension.HM_ELEMENT_NAME,
				HmExtension.NAMESPACE);
		HmExtension hm = null;
		if (pe != null) {
			hm = (HmExtension) pe;
			this.setFakeMessage(hm.get_fakeMessage());
			this.setAudioUrl(hm.get_audioUrl());
			this.setImgUrl(hm.get_imgUrl());
			this.setExpiresSeconds(hm.get_expires_seconds());
			this.setVideoUrl(hm.get_videoUrl());
		}
	}
}
