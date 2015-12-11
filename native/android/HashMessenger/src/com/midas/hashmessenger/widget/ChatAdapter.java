package com.midas.hashmessenger.widget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.midas.hashmessenger.R;
import com.midas.hashmessenger.messages.HashMessage;

public class ChatAdapter extends BaseAdapter {
	private Context mContext;
	private ArrayList<HashMessage> mMessages;

	public ChatAdapter(Context context, ArrayList<HashMessage> messages) {
		super();
		this.mContext = context;
		this.mMessages = messages;
	}

	@Override
	public int getCount() {
		return mMessages.size();
	}

	@Override
	public Object getItem(int position) {
		return mMessages.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		HashMessage message = (HashMessage) this.getItem(position);

		ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.sms_row, parent, false);
			holder.bubble = (LinearLayout) convertView
					.findViewById(R.id.message_bubble);
			holder.message = (TextView) convertView
					.findViewById(R.id.message_text);
			holder.message_date = (TextView) convertView
					.findViewById(R.id.message_date);
			convertView.setTag(holder);
		} else
			holder = (ViewHolder) convertView.getTag();

		holder.message.setText(message.getBody());
		if (message.getDate() != null ) {
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
			holder.message_date.setText(sdf.format(message.getDate()));
		}

		LayoutParams lp = (LayoutParams) holder.bubble.getLayoutParams();
		LayoutParams lpmessage = (LayoutParams) holder.message.getLayoutParams();
		LayoutParams lpdate = (LayoutParams) holder.message_date.getLayoutParams();

		if (message.isStatusMessage()) {
			holder.bubble.setBackgroundDrawable(null);
			lp.gravity = Gravity.LEFT;
			holder.message.setTextColor(mContext.getResources().getColor(
					R.color.sendLightColor));
			if (message.isMine()) {
				lp.gravity = Gravity.RIGHT;
			}
		} else {
			if (message.isMine()) {
				holder.bubble
						.setBackgroundResource(R.drawable.speech_bubble_green);
				lp.gravity = Gravity.RIGHT;
				lpdate.gravity = Gravity.RIGHT;
			} else {
				holder.bubble
						.setBackgroundResource(R.drawable.speech_bubble_orange);
				lp.gravity = Gravity.LEFT;
				lpdate.gravity = Gravity.LEFT;
			}
			holder.message.setLayoutParams(lp);
			holder.message.setTextColor(mContext.getResources().getColor(
					R.color.textFieldColor));
		}
		/*
		 * 
		 * DateFormat format = DateFormat.getDateInstance(); String sentAtText =
		 * format.format(sentAtDate); String readAtText =
		 * format.format(readAtDate);
		 */
		return convertView;
	}

	private static class ViewHolder {
		LinearLayout bubble;
		TextView message;
		TextView message_date;
	}

	@Override
	public long getItemId(int position) {
		// Unimplemented, because we aren't using Sqlite.
		return 0;
	}

}
