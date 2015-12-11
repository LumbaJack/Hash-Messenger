package com.midas.hashmessenger.api;

import java.util.Calendar;
import java.util.Date;

import com.midas.hashmessenger.R;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateFormat;

public class DateUtilsApi {

	private static boolean CheckDay(Date date1, Date date2, int days) {

		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(date1);
		cal2.setTime(date2);
		if (days > 0) {
			cal1.add(Calendar.DAY_OF_YEAR, -1); // yesterday
		}
		boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
				&& cal1.get(Calendar.DAY_OF_YEAR) == cal2
						.get(Calendar.DAY_OF_YEAR);
		return sameDay;
	}

	public static String showelapsedtime(Date oldate, Context context) {
	
		Resources res = context.getResources();
		Date curdate = new Date();
		Long nosecs = (curdate.getTime() - oldate.getTime()) / 1000;
		if (nosecs < 60) {
			return res.getString(R.string.now);
		} else if (nosecs < 3600) {
			return String.valueOf((int) (nosecs / 60)) + "min";
		} else if (CheckDay(curdate, oldate, 0)) {
			return (String) DateFormat.format("HH:mm", oldate);
		} else if (CheckDay(curdate, oldate, 1)) { //is Yesterday
			return res.getString(R.string.yesterday);
		}
		return (String) DateFormat.format("MMM d", oldate);
	}
}
