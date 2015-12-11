package com.midas.hashmessenger;

import com.androidquery.AQuery;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

public class eula extends Activity {

	private AQuery aq;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.eula);

		aq = new AQuery(this);
		aq.id(R.id.button1).clicked(this, "buttonClicked");
	};

	/*
	 * @Override public void onBackPressed() { }
	 */

	public void buttonClicked(View v) {
		SharedPreferences prefs = this.getSharedPreferences("APPSETTINGS",
				Activity.MODE_PRIVATE);
		prefs.edit().putString("EULA", "true").commit();

		Intent intent = new Intent(eula.this, signup.class);
		eula.this.startActivity(intent);
		finish();
	}
}
