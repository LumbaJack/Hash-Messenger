package com.midas.hashmessenger;

import java.util.ArrayList;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;

import com.androidquery.AQuery;
import com.midas.hashmessenger.XMPPClient.NoConfigurationException;
import com.midas.hashmessenger.api.DateUtilsApi;
import com.midas.hashmessenger.api.NotificationsAPI;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ContactsListFragment extends ListFragment implements
		XMPPClient.XMPPClientListener {
	private final static String TAG = ContactsListFragment.class.getName();
	private ArrayList<HashContactData> m_contactData;
	private HashContactDataAdapter m_adapter;
	private ContactsFragmentDataListener m_getDatalistener;
	private AQuery aq;

	// Empty public constructor, required by the system
	public ContactsListFragment() {
	}

	// A UI Fragment must inflate its View
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater
				.inflate(R.layout.contacts_list_layout, container, false);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {
			m_getDatalistener = (ContactsFragmentDataListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement ContactsFragmentDataListener");
		}
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		bindData();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		m_getDatalistener.onItemClicked(m_contactData.get(position), position,
				id);
	}

	public void bindData() {
		aq = new AQuery(this.getActivity());
		m_contactData = m_getDatalistener.onContactsListGetData();
		if (m_contactData == null || m_contactData.size() == 0) {
			aq.id(android.R.id.list).invisible();
			aq.id(android.R.id.empty).visible();
		} else {
			aq.id(android.R.id.list).visible();
			aq.id(android.R.id.empty).invisible();
			m_adapter = new HashContactDataAdapter(getActivity(), m_contactData);
			setListAdapter(m_adapter);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		try {
			HashApplication.getXmpp().connect();
		} catch (NoConfigurationException e) {
			// ignored
		}

		HashApplication.getXmpp().setXMPPListener(this);

		bindData();
	}

	private class HashContactDataAdapter extends ArrayAdapter<HashContactData> {
		private final Context context;
		private final ArrayList<HashContactData> values;

		public HashContactDataAdapter(Context context,
				ArrayList<HashContactData> values) {
			super(context, R.layout.contacts_list_item, values);
			this.context = context;
			this.values = values;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = convertView;
			RowWrapper wrapper;
			if (null == rowView) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.contacts_list_item, parent,
						false);
				wrapper = new RowWrapper(rowView);
				rowView.setTag(wrapper); // save for next time
			} else {
				wrapper = (RowWrapper) rowView.getTag();
			}

			HashContactData currentRecord = getItem(position);

			wrapper.getName().setText(
					currentRecord.DISPLAY_NAME + "(" + currentRecord.USERID
							+ ")");

			Uri imguri = Uri.parse(currentRecord.PHOTO_URI);
			if (imguri != null) {
				wrapper.getImage().setImageURI(imguri);
			}

			if (currentRecord.UNREAD_MESSAGES_COUNT > 0) {
				wrapper.getUnread().setText(
						String.valueOf(currentRecord.UNREAD_MESSAGES_COUNT));
				wrapper.getUnread().setVisibility(View.VISIBLE);
			} else {
				wrapper.getUnread().setVisibility(View.INVISIBLE);
			}

			if (currentRecord.LASTMSG != null
					&& currentRecord.LASTDATEMSG != null) {

				wrapper.getLastmsg().setText(currentRecord.LASTMSG);
				wrapper.getDate().setText(
						DateUtilsApi.showelapsedtime(currentRecord.LASTDATEMSG,
								context));
				wrapper.getLastmsg().setVisibility(View.VISIBLE);
				wrapper.getDate().setVisibility(View.VISIBLE);

			} else {
				wrapper.getLastmsg().setVisibility(View.INVISIBLE);
				wrapper.getDate().setVisibility(View.INVISIBLE);
			}

			if (!ContactsListFragment.this.m_getDatalistener.showHistory()) {
				wrapper.getLastmsg().setVisibility(View.INVISIBLE);
			}
			return rowView;
		}
	}

	// Container Activity must implement this interface
	public interface ContactsFragmentDataListener {
		public ArrayList<HashContactData> onContactsListGetData();

		public void onItemClicked(HashContactData item, int position, long id);

		public boolean showHistory();
	}

	/*
	 * RowWrapper class is used to cache our views so we don't have to look them
	 * up each time the scroll into view with findById (which is a performance
	 * killer)
	 */
	private class RowWrapper {
		private View base;
		private TextView Name = null;
		private ImageView Image = null;
		private TextView Unread = null;
		private TextView Datemsg = null;
		private TextView Lastmsg = null;

		RowWrapper(View base) {
			this.base = base;
		}

		TextView getName() {
			if (null == this.Name)
				this.Name = (TextView) base.findViewById(android.R.id.text1);
			return this.Name;
		}

		ImageView getImage() {
			if (null == this.Image)
				this.Image = (ImageView) base.findViewById(R.id.contactImage);
			return this.Image;
		}

		TextView getUnread() {
			if (null == this.Unread)
				this.Unread = (TextView) base.findViewById(R.id.unread);
			return this.Unread;
		}

		TextView getDate() {
			if (null == this.Datemsg)
				this.Datemsg = (TextView) base.findViewById(R.id.datemsg);
			return this.Datemsg;
		}

		TextView getLastmsg() {
			if (null == this.Lastmsg)
				this.Lastmsg = (TextView) base.findViewById(R.id.lastmsg);
			return this.Lastmsg;
		}

	}

	@Override
	public void onConnect() {
	}

	@Override
	public void onMessageSent(HashMessageData[] msgs) {
	}

	@Override
	public void onMessageSentFail(HashMessageData msg, XMPPException ex) {
	}

	@Override
	public void onConnectError(XMPPException ex) {
	}

	@Override
	public void onLoginError(XMPPException ex) {
	}

	@Override
	public void onChatCreated(Chat chat, boolean arg) {

	}

	@Override
	public void onProcessMessage(Chat chat, HashMessageData message) {
		HashContactData fromContact = null;
		for (HashContactData cd : m_contactData) {
			String from = chat.getParticipant();
			if (from == null) {
				continue;
			}
			from = XMPPClient.useridOnly(from);
			if (from.equals(cd.USERID)) {
				cd.UNREAD_MESSAGES_COUNT++;
				fromContact = cd;
			}
		}

		if (fromContact != null) {
			NotificationsAPI Notification = new NotificationsAPI();
			Notification.sendNotification(fromContact.USERID,
					"New Message from " + message.message.getSender(), 1,
					getActivity().getBaseContext());
		}

		this.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				m_adapter.notifyDataSetChanged();
			}
		});
	}

}