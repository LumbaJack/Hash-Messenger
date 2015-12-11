package com.midas.hashmessenger;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.AndroidConnectionConfiguration;
import org.jivesoftware.smack.BOSHConfiguration;
import org.jivesoftware.smack.BOSHConnection;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.proxy.ProxyInfo;

import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.HostAddress;

import com.midas.hashmessenger.database.Tables.MessagesTable;
import com.midas.hashmessenger.messages.HashMessage;
import com.midas.hashmessenger.messages.HmExtension;
import com.midas.hashmessenger.messages.BaseMessage.HashMessageType;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public class XMPPClient implements ConnectionListener, MessageListener,
		ChatManagerListener {
	private static final String TAG = XMPPClient.class.getName();
	private static final boolean ENCRYPTED = true;

	public static final String HOST = "23.21.90.204";
	public static final int PORT = 80;
	public static final String DOMAIN = "hashmessenger.com";

	private BOSHConnection m_connection;
	private Context m_context;
	private XMPPClientListener m_listener;
	private ChatManager m_chatManager;
	private Chat m_chat;
	private PacketListener m_packetListener;
	private PacketCollector m_packetCollector;

	private String m_username = null;
	private String m_password = null;
	private String m_recipient = null;
	private SharedPreferences m_prefs = null;

	public XMPPClient(Context context) {
		m_context = context;
		m_prefs = m_context.getSharedPreferences("APPSETTINGS",
				Activity.MODE_PRIVATE);

		ProviderManager.getInstance().addExtensionProvider(
				HmExtension.HM_ELEMENT_NAME, HmExtension.NAMESPACE,
				new HmExtension.Provider());

	}

	public void setXMPPListener(XMPPClientListener listener) {
		m_listener = listener;
	}

	public void send(String message) throws NotConnectedException {
		String text = message;
		if (m_connection == null) {
			throw new NotConnectedException();
		}

		if (m_chat != null) {
			Log.d(TAG, "Sending text " + text + " to " + m_recipient);
			HashMessage msg = new HashMessage(m_recipient, HashMessageType.CHAT);

			String from = useridOnly(m_username);
			String to = useridOnly(m_recipient);

			try {
				if (ENCRYPTED) {
					// all ways send messages encrypted
					msg.setBody(encrypt(to, text));
				} else {
					msg.setBody(text);
				}
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			long rowid = saveMessageToHistory(from, to, msg.getSubject(), text,
					MessagesTable.MSGSTATUS.MSGSENTUNREAD.getCode());

			if (rowid != -1) {
				HashMessageData msgwrap = new HashMessageData();
				msgwrap.rowid = rowid;
				msgwrap.message = msg;
				new SendTask().execute(msgwrap);
			}
		}
	}

	private String encrypt(String touser, String cleartxt)
			throws InvalidKeyException, Exception {
		String pubKeyStr = HashApplication.getDatabaseAdapter().get_public_key(
				touser);
		return CryptoManager.encrypt(pubKeyStr, cleartxt);
	}

	private String decrypt(String base64str) throws InvalidKeyException,
			Exception {

		SharedPreferences prefs = m_context.getSharedPreferences("APPSETTINGS",
				Activity.MODE_PRIVATE);
		String privateKeyStr = prefs.getString(HashConstants.PREF_PRIVATEKEY,
				null);
		return CryptoManager.decrypt(privateKeyStr, base64str);
	}

	private class SendTask extends
			AsyncTask<HashMessageData, Void, HashMessageData[]> {
		@Override
		protected HashMessageData[] doInBackground(HashMessageData... params) {
			Log.d(TAG, "Sending text  doInBackground ");
			for (int i = 0; i < params.length; i++) {
				try {
					Log.d(TAG, "Sending text  doInBackground send");
					m_chat.sendMessage(params[i].message.getMessage());
					Log.d(TAG, "Sending text  doInBackground after send");
				} catch (XMPPException e) {
					e.printStackTrace();
					m_listener.onMessageSentFail(params[i], e);
				}
			}
			Log.d(TAG, "Sending text  doInBackground done");
			return params;
		}

		@Override
		public void onProgressUpdate(Void... x) {
		}

		@Override
		protected void onPostExecute(HashMessageData[] msgs) {
			Log.d(TAG, "Sending text  onPostExecute");
			if (m_listener != null) {
				for (HashMessageData msg : msgs) {
					msg.sentat = new Date();
					HashApplication.getDatabaseAdapter().update_message_sent(
							msg);
				}

				m_listener.onMessageSent(msgs);
			}
		}

	}

	public void disconnect() {
		m_connection.disconnect();
	}

	public static String canonRecipient(String username) {
		;
		if (username.indexOf("@") == -1) {
			/* strip off everything upto after @ */
			return String.format("%s@%s/Smack", username, DOMAIN);
		} else {
			return username;
		}
	}

	public static String useridOnly(String username) {
		if (username.indexOf("@") == -1) {
			return username;
		} else {
			return username.substring(0, username.indexOf("@"));
		}
	}

	AndroidBOSHConnectionConfiguration m_connConfig;

	public void connect() throws NoConfigurationException {
		// connect but don't start a chat
		startChat(null);
	}

	public void startChat(String recipient) throws NoConfigurationException {
		if (recipient != null) {
			m_recipient = canonRecipient(recipient);
		}
		SharedPreferences prefs = m_context.getSharedPreferences("APPSETTINGS",
				Activity.MODE_PRIVATE);

		m_username = prefs.getString(HashConstants.PREF_USERNAME, null);
		m_password = prefs.getString(HashConstants.PREF_PASSWORD, null);

		if (m_username == null || m_password == null) {
			// not setup yet
			throw new NoConfigurationException();
		}

		// Create a connection
		SmackAndroid.init(XMPPClient.this.m_context);
		SmackConfiguration.setPacketReplyTimeout(15000);
		try {
			m_connConfig = new AndroidBOSHConnectionConfiguration(false, HOST,
					PORT, "/http-bind/", DOMAIN);
		} catch (XMPPException e) {
			e.printStackTrace();
		}

		SASLAuthentication.supportSASLMechanism("PLAIN");
		/*
		 * IMPORTANT! some kind of bug is preventing the hostname/port getting
		 * set in the BOSHConfiguration object so I have to set it explicitly
		 * with setUsedHostAddress below
		 */
		m_connConfig.setUsedHostAddress(new HostAddress(HOST, PORT));

		/* */
		System.setProperty("smack.debugEnabled", "true");
		XMPPConnection.DEBUG_ENABLED = true;
		BOSHConnection.DEBUG_ENABLED = true;

		m_connConfig.setDebuggerEnabled(true);

		m_connConfig.setExpiredCertificatesCheckEnabled(false);
		m_connConfig.setNotMatchingDomainCheckEnabled(false);
		m_connConfig.setServiceName(DOMAIN);
		m_connConfig.setSASLAuthenticationEnabled(true);
		m_connConfig.setReconnectionAllowed(true);

		m_connection = new BOSHConnection(m_connConfig);

		new ConnectTask().execute(m_username, m_password, m_recipient);

	}

	private class ConnectTask extends
			AsyncTask<String, XMPPException, XMPPException> {
		@Override
		protected XMPPException doInBackground(String... params) {
			String username = params[0];
			String password = params[1];
			String recipient = params[2];

			if (!m_connection.isConnected()) {
				try {
					m_connection.connect();
					Log.i(TAG, "Connected to " + m_connection.getHost());
				} catch (XMPPException ex) {
					Log.e(TAG, "Failed to connect to " + m_connection.getHost());
					Log.e(TAG, ex.toString());
					return ex;
				}
			}

			if (!m_connection.isAuthenticated()) {
				try {
					Log.i(TAG, "Logging in as " + username);
					m_connection.login(username, password);
					Log.i(TAG, "Successfully logged in as " + username);
					Presence.Type type = Type.available;
					Presence presence = new Presence(type);
					presence.setStatus("Online");
					m_connection.sendPacket(presence);

				} catch (XMPPException ex) {
					return ex;
				} catch (IllegalStateException isex) {
					return null;
				}
			}

			/*
			 * // Set the status to available Presence presence = new
			 * Presence(Presence.Type.available);
			 * m_connection.sendPacket(presence);
			 */

			m_chatManager = m_connection.getChatManager();
			if (recipient != null) {
				m_chat = m_chatManager.createChat(recipient, XMPPClient.this);
			}
			m_chatManager.addChatListener(XMPPClient.this);

			/*
			 * Roster roster = connection.getRoster(); Collection<RosterEntry>
			 * entries = roster.getEntries(); for (RosterEntry entry : entries)
			 * {
			 * 
			 * Log.d(TAG, "--------------------------------------"); Log.d(TAG,
			 * "RosterEntry " + entry); Log.d(TAG, "User: " + entry.getUser());
			 * Log.d(TAG, "Name: " + entry.getName()); Log.d(TAG, "Status: " +
			 * entry.getStatus()); Log.d(TAG, "Type: " + entry.getType());
			 * Presence entryPresence = roster.getPresence(entry.getUser());
			 * 
			 * Log.d(TAG, "Presence Status: " + entryPresence.getStatus());
			 * Log.d(TAG, "Presence Type: " + entryPresence.getType());
			 * 
			 * Presence.Type type = entryPresence.getType(); if (type ==
			 * Presence.Type.available) Log.d(TAG, "Presence AVIALABLE");
			 * Log.d(TAG, "Presence : " + entryPresence); }
			 */
			return null;
		}

		@Override
		public void onProgressUpdate(XMPPException... x) {
		}

		@Override
		protected void onPostExecute(XMPPException x) {

			if (m_listener != null) {
				if (x == null) {
					m_listener.onConnect();
				} else if (x instanceof LoginException) {
					m_listener.onLoginError(x);
				} else {
					m_listener.onConnectError(x);
				}

			}
		}

	}

	public interface XMPPClientListener {
		public void onConnect();

		public void onMessageSent(HashMessageData[] msgs);

		public void onMessageSentFail(final HashMessageData msg,
				final XMPPException ex);

		public void onConnectError(final XMPPException ex);

		public void onLoginError(final XMPPException ex);

		public void onChatCreated(Chat chat, boolean arg);

		public void onProcessMessage(Chat chat, HashMessageData message);

	}

	public class LoginException extends XMPPException {

	}

	public class NotConnectedException extends XMPPException {

	}

	public class NoConfigurationException extends Exception {

	}

	@Override
	public void chatCreated(Chat chat, boolean arg1) {

		Log.i(TAG,
				String.format(">>> Chat created (participant=%s): ",
						chat.getParticipant()));
		if (!chat.getListeners().contains(this)) {
			chat.addMessageListener(this);
		}

		if (m_listener != null) {
			m_listener.onChatCreated(chat, arg1);
		}
	}

	private long saveMessageToHistory(String from, String to, String subject,
			String msgtxt, int status) {
		String historytext = msgtxt;
		try {
			historytext = CryptoManager.passwordEncrypt(historytext);
		} catch (Exception e) {
			e.printStackTrace();
			new AlertDialog.Builder(this.m_context)
					.setMessage("Unable to save message to history")
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).show();
			historytext = null;
		}
		if (historytext != null) {
			long rowid = HashApplication.getDatabaseAdapter().insert_message(
					from, to, subject, historytext, new Date(), status);

			return rowid;
		}
		return -1;

	}

	@Override
	public void processMessage(Chat chat, Message message) {
		HashMessage msg = new HashMessage(message);
		String from = msg.getSender();
		String to = msg.getRecipient();

		from = useridOnly(from);
		to = useridOnly(to);
		String msgbody = msg.getBody();
		Log.i(TAG, String.format(">>> Message received (from=%s, to=%s): %s",
				from, to, msg.getBody()));

		if (XMPPClient.ENCRYPTED) {
			try {
				msgbody = decrypt(msg.getBody());
				// Log.i(TAG, "cleartxt => " + msgbody);
			} catch (InvalidKeyException e) {
				msgbody = e.getMessage();
				e.printStackTrace();
			} catch (Exception e) {
				msgbody = e.getMessage();
				e.printStackTrace();
			}
		}

		msg.setBody(null); // remove old body
		msg.setBody(msgbody);

		long rowid = saveMessageToHistory(from, to, msg.getSubject(), msgbody,
				MessagesTable.MSGSTATUS.MSGRECEIVEUNREAD.getCode());

		if (rowid != -1) {
			HashMessageData msgwrap = new HashMessageData();
			msgwrap.rowid = rowid;
			msgwrap.message = msg;
			if (m_listener != null) {
				m_listener.onProcessMessage(chat, msgwrap);
			}
		}
	}

	// NOT SURE IF WE WANT TO HANDLE THESE HERE OR IN THE ACTIVITY

	@Override
	public void connectionClosed() {
		Log.i(TAG, "connectionClosed");

	}

	@Override
	public void connectionClosedOnError(Exception arg0) {
		Log.i(TAG, "connectionClosedOnError");

	}

	@Override
	public void reconnectingIn(int arg0) {
		Log.i(TAG, "reconnectingIn");

	}

	@Override
	public void reconnectionFailed(Exception arg0) {
		Log.i(TAG, "reconnectionFailed");

	}

	@Override
	public void reconnectionSuccessful() {
		Log.i(TAG, "reconnectionSuccessful");

	}

	public class AndroidBOSHConnectionConfiguration extends BOSHConfiguration {
		private static final int DEFAULT_TIMEOUT = 15000;

		/**
		 * Creates a new BOSHConfiguration for the specified service name. A DNS
		 * SRV lookup will be performed to find out the actual host address and
		 * port to use for the connection.
		 * 
		 * @param serviceName
		 *            the name of the service provided by an XMPP server.
		 */
		public AndroidBOSHConnectionConfiguration(boolean ssl,
				String serviceName, int port, String path, String domain)
				throws XMPPException {
			super(ssl, serviceName, port, path, domain);
			AndroidInit(serviceName, DEFAULT_TIMEOUT);
		}

		private void AndroidInit() {
			// API 14 is Ice Cream Sandwich
			if (Build.VERSION.SDK_INT >= 14) {
				setTruststoreType("AndroidCAStore");
				setTruststorePassword(null);
				setTruststorePath(null);
			} else {
				setTruststoreType("BKS");
				String path = System.getProperty("javax.net.ssl.trustStore");
				if (path == null)
					path = System.getProperty("java.home") + File.separator
							+ "etc" + File.separator + "security"
							+ File.separator + "cacerts.bks";
				setTruststorePath(path);
			}
		}

		/**
		 * 
		 * @param serviceName
		 * @param timeout
		 * @throws XMPPException
		 */
		private void AndroidInit(String serviceName, int timeout)
				throws XMPPException {
			AndroidInit();
			class DnsSrvLookupRunnable implements Runnable {
				String serviceName;
				List<HostAddress> addresses;

				public DnsSrvLookupRunnable(String serviceName) {
					this.serviceName = serviceName;
				}

				@Override
				public void run() {
					addresses = DNSUtil.resolveXMPPDomain(serviceName);
				}

				public List<HostAddress> getHostAddresses() {
					return addresses;
				}
			}

			DnsSrvLookupRunnable dnsSrv = new DnsSrvLookupRunnable(serviceName);
			Thread t = new Thread(dnsSrv, "dns-srv-lookup");
			t.start();
			try {
				t.join(timeout);
			} catch (InterruptedException e) {
				throw new XMPPException("DNS lookup timeout after " + timeout
						+ "ms", e);
			}

			hostAddresses = dnsSrv.getHostAddresses();
			if (hostAddresses == null) {
				throw new XMPPException("DNS lookup failure");
			}

			ProxyInfo proxy = ProxyInfo.forDefaultProxy();

			init(serviceName, proxy);
		}
	}
}
