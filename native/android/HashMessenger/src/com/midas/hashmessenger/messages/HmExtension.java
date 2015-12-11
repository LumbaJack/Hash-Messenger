package com.midas.hashmessenger.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;

import android.util.Log;

public class HmExtension implements PacketExtension {
	public static final String NAMESPACE = "urn:xmpp:hm:0";
	public static final String HM_ELEMENT_NAME = "hm";
	public static final String RETRANSMIT_ELEMENT_NAME = "rt";
	public static final String EXPIRES_ELEMENT_NAME = "xp";
	public static final String DELETE_ELEMENT_NAME = "del";
	public static final String DELETE_ALL_ELEMENT_NAME = "delall";
	public static final String FAKE_MESSAGE_ELEMENT_NAME = "fmsg";
	public static final String SCREENCAP_ELEMENT_NAME = "scap";
	public static final String IMAGE_ELEMENT_NAME = "img";
	public static final String LOCATION_ELEMENT_NAME = "geo";
	public static final String AUDIO_ELEMENT_NAME = "audio";
	public static final String VIDEO_ELEMENT_NAME = "video";

	private String id; // / original ID of the delivered message

	private boolean m_retransmit = false;
	private Long m_expires_seconds = null;
	private boolean m_delete = false;
	private boolean m_delete_all = false;
	private String m_fakeMessage = null;
	private String m_imgUrl = null;
	private String m_audioUrl = null;
	private String m_videoUrl = null;

	public HmExtension(BaseMessage hmsg) {
		this.id = hmsg.getMessage().getPacketID();
		this.set_audioUrl(hmsg.getAudioUrl());
		this.set_expires_seconds(hmsg.getExpiresSeconds());
		this.set_fakeMessage(hmsg.getFakeMessage());
		this.set_imgUrl(hmsg.getImgUrl());
		this.set_videoUrl(hmsg.getVideoUrl());
	}

	public HmExtension(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public boolean get_retransmit() {
		return m_retransmit;
	}

	public void set_retransmit(boolean retransmit) {
		this.m_retransmit = retransmit;
	}

	public Long get_expires_seconds() {
		return m_expires_seconds;
	}

	public boolean get_delete() {
		return m_delete;
	}

	public void set_delete(boolean delete) {
		this.m_delete = delete;
	}

	public boolean get_delete_all() {
		return m_delete_all;
	}

	public void set_delete_all(boolean deleteall) {
		this.m_delete_all = deleteall;
	}

	public void set_expires_seconds(Long m_expires_seconds) {
		this.m_expires_seconds = m_expires_seconds;
	}

	public String get_fakeMessage() {
		return m_fakeMessage;
	}

	public void set_fakeMessage(String m_fakeMessage) {
		this.m_fakeMessage = m_fakeMessage;
	}

	public String get_imgUrl() {
		return m_imgUrl;
	}

	public void set_imgUrl(String m_imgUrl) {
		this.m_imgUrl = m_imgUrl;
	}

	public String get_audioUrl() {
		return m_audioUrl;
	}

	public void set_audioUrl(String m_audioUrl) {
		this.m_audioUrl = m_audioUrl;
	}

	public String get_videoUrl() {
		return m_videoUrl;
	}

	public void set_videoUrl(String m_videoUrl) {
		this.m_videoUrl = m_videoUrl;
	}

	@Override
	public String getElementName() {
		return HM_ELEMENT_NAME;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public String toXML() {

		StringBuilder sb = new StringBuilder();
		sb.append("<" + HM_ELEMENT_NAME + ">");

		if (get_expires_seconds() > 0) {
			sb.append("<" + EXPIRES_ELEMENT_NAME + ">" + get_expires_seconds()
					+ "</" + EXPIRES_ELEMENT_NAME + ">");
		}

		if (get_fakeMessage() != null && !get_fakeMessage().isEmpty()) {
			sb.append("<" + FAKE_MESSAGE_ELEMENT_NAME + ">"
					+ StringUtils.escapeForXML(get_fakeMessage()) + "</"
					+ FAKE_MESSAGE_ELEMENT_NAME + ">");
		}

		if (get_imgUrl() != null && !get_imgUrl().isEmpty()) {
			sb.append("<" + IMAGE_ELEMENT_NAME + " src=\""
					+ StringUtils.escapeForXML(get_imgUrl()) + "\" />");
		}

		if (get_audioUrl() != null && !get_audioUrl().isEmpty()) {
			sb.append("<" + AUDIO_ELEMENT_NAME + " src=\""
					+ StringUtils.escapeForXML(get_audioUrl()) + "\" />");
		}

		if (get_videoUrl() != null && !get_videoUrl().isEmpty()) {
			sb.append("<" + VIDEO_ELEMENT_NAME + " src=\""
					+ StringUtils.escapeForXML(get_videoUrl()) + "\" />");
		}

		if (get_delete()) {
			sb.append("<" + DELETE_ELEMENT_NAME + ">1</" + DELETE_ELEMENT_NAME
					+ ">");
		}

		if (get_delete_all()) {
			sb.append("<" + DELETE_ALL_ELEMENT_NAME + ">1</"
					+ DELETE_ALL_ELEMENT_NAME + ">");
		}
		sb.append("</" + HM_ELEMENT_NAME + ">");
		return sb.toString();
	}

	public static class Provider implements PacketExtensionProvider {

		final public PacketExtension parseExtension(XmlPullParser parser)
				throws Exception {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			Map<String, String> attMap = new HashMap<String, String>();

			for (int i = 0; i < parser.getAttributeCount(); i++) {
				attMap.put(parser.getAttributeName(i),
						parser.getAttributeValue(i));
			}

			HmExtension result = null;
			boolean inhm = false;
			String tagname = null;

			String textContent = null;
			parser.nextText();
			int tag = 0;
			do {
				tag = parser.next();
				if (tag == XmlPullParser.START_TAG
						&& name.equals(parser.getName())) {
					inhm = true;
					result = new HmExtension(attMap.get("id"));
					Log.d("HmExtension", "inhm = true");
				}

				if (tag == XmlPullParser.END_TAG
						&& name.equals(parser.getName())) {
					inhm = false;
					Log.d("HmExtension", "inhm = false");
				}

				if (inhm) { // ignore everything outside of the hm element
					if (tag == XmlPullParser.START_TAG) {
						tagname = parser.getName(); // tagname is the name of
													// the current element
						Log.d("HmExtension", "START_TAG name is " + tagname);
					}

					if (tag == XmlPullParser.TEXT
							|| tag == XmlPullParser.CDSECT) {
						textContent = parser.getText();
						Log.d("HmExtension", "textContent is  " + textContent);
					}

					// when we're done with a tag apply the text content
					if (tagname != null && !tagname.isEmpty()
							&& tag == XmlPullParser.END_TAG) {

						Map<String, String> aMap = new HashMap<String, String>();
						for (int i = 0; i < parser.getAttributeCount(); i++) {
							aMap.put(parser.getAttributeName(i),
									parser.getAttributeValue(i));
						}

						if (tagname.equals(EXPIRES_ELEMENT_NAME)) {
							result.set_expires_seconds(Long
									.parseLong(textContent));
						}

						if (tagname.equals(FAKE_MESSAGE_ELEMENT_NAME)) {
							result.set_fakeMessage(textContent);
						}

						if (tagname.equals(IMAGE_ELEMENT_NAME)) {

							String src = aMap.get("src");
							result.set_imgUrl(src);
						}
					}
				}

			} while (tag != XmlPullParser.END_DOCUMENT);

			return result;
		}
	}
}
