package com.midas.hashmessenger.activities;

import com.midas.hashmessenger.BaseActivity;

import com.midas.hashmessenger.CryptoManager;
import com.midas.hashmessenger.HashApplication;
import com.midas.hashmessenger.HashConstants;
import com.midas.hashmessenger.HistoryManager;

import com.midas.hashmessenger.R;
import com.midas.hashmessenger.dialogs.PromptPasswordDialog;
import com.midas.hashmessenger.dialogs.PromptPasswordDialog.PromptPasswordDialogListener;
import com.midas.hashmessenger.dialogs.SetPasswordDialog;
import com.midas.hashmessenger.dialogs.SetPasswordDialog.SetPasswordDialogListener;

import android.os.Bundle;
import android.preference.PreferenceManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;

import android.content.SharedPreferences;

import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HistorySettingsActivity extends BaseActivity implements
		SetPasswordDialogListener, PromptPasswordDialogListener {
	private CheckBox cbSaveHistory = null;
	private CheckBox cbEncryptHistory = null;
	private LinearLayout llDaysHistoryFields = null;
	private TextView txtDaysHistory = null;
	private Button btnDeleteHistory = null;
	private Button btnSetPassword = null;
	private Button btnClearPassword = null;
	private SharedPreferences m_prefs = null;
	private String m_nextDialog = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history_settings);

		m_prefs = getSharedPreferences("APPSETTINGS", Activity.MODE_PRIVATE);
		cbSaveHistory = (CheckBox) findViewById(R.id.cbSaveHistory);
		cbEncryptHistory = (CheckBox) findViewById(R.id.cbEncryptHistory);
		llDaysHistoryFields = (LinearLayout) findViewById(R.id.llDaysHistoryFields);
		txtDaysHistory = (TextView) findViewById(R.id.txtDaysHistory);
		btnDeleteHistory = (Button) findViewById(R.id.btnDeleteHistory);
		btnSetPassword = (Button) findViewById(R.id.btnSetPassword);
		btnClearPassword = (Button) findViewById(R.id.btnClearPassword);

		int saveDays = m_prefs.getInt(HashConstants.PREF_SAVE_HISTORY_DAYS, 0);

		txtDaysHistory.setText(Integer.toString(saveDays));
		cbSaveHistory.setChecked(m_prefs.getBoolean(
				HashConstants.PREF_SAVE_HISTORY, true));
		if (cbSaveHistory.isChecked()) {
			llDaysHistoryFields.setVisibility(View.VISIBLE);
			txtDaysHistory.requestFocus();
		} else {
			llDaysHistoryFields.setVisibility(View.INVISIBLE);
		}

		cbSaveHistory.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					llDaysHistoryFields.setVisibility(View.VISIBLE);
					txtDaysHistory.requestFocus();
				} else {
					llDaysHistoryFields.setVisibility(View.INVISIBLE);
				}
			}
		});

		cbEncryptHistory.setChecked(m_prefs.getBoolean(
				HashConstants.PREF_ENCRYPT_HISTORY, true));

		btnSetPassword.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				FragmentManager fm = getSupportFragmentManager();
				if (HistoryManager.hasUserHistoryPassword()
						&& !HistoryManager.isUserHistoryAuthenticated()) {
					m_nextDialog = "set_password";
					PromptPasswordDialog promptPasswordDialog = new PromptPasswordDialog();
					promptPasswordDialog.show(fm, "dialog_promptpassword");
				} else {
					SetPasswordDialog setPasswordDialog = new SetPasswordDialog();
					setPasswordDialog.show(fm, "dialog_setpassword");
				}
			}
		});

		String history_password = m_prefs.getString(
				HashConstants.PREF_ENCRYPT_HISTORY_PASSWORD, null);
		if (history_password == null || history_password.isEmpty()) {
			btnClearPassword.setVisibility(View.INVISIBLE);
		}

		btnClearPassword.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				FragmentManager fm = getSupportFragmentManager();
				if (HistoryManager.hasUserHistoryPassword()
						&& !HistoryManager.isUserHistoryAuthenticated()) {
					m_nextDialog = "clear_password";
					PromptPasswordDialog promptPasswordDialog = new PromptPasswordDialog();
					promptPasswordDialog.show(fm, "dialog_promptpassword");
				} else {

					AlertDialog.Builder b = new AlertDialog.Builder(
							HistorySettingsActivity.this)
							.setTitle(R.string.clear_password)
							.setPositiveButton(android.R.string.ok,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int whichButton) {
											m_prefs.edit()
													.remove(HashConstants.PREF_ENCRYPT_HISTORY_PASSWORD)
													.commit();
										}
									})
							.setNegativeButton(android.R.string.cancel,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int whichButton) {
										}
									});
					b.show();
				}
			}
		});

		btnDeleteHistory.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						HistorySettingsActivity.this);
				builder.setTitle(R.string.delete_history_title);
				builder.setMessage(R.string.delete_history_message);
				builder.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int item) {
								HashApplication.getDatabaseAdapter()
										.delete_conversations();
								Toast.makeText(HistorySettingsActivity.this,
										R.string.history_deleted_message,
										Toast.LENGTH_LONG).show();

							}
						});
				builder.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int item) {
								dialog.dismiss();
							}
						});
				builder.show();
			}
		});
	}

	@Override
	protected void onStop() {
		super.onStop();

		m_prefs.edit()
				.putBoolean(HashConstants.PREF_SAVE_HISTORY,
						cbSaveHistory.isChecked()).commit();

		String strDaysHistory = txtDaysHistory.getText().toString();
		if (strDaysHistory != null && strDaysHistory.length() > 0) {
			m_prefs.edit().putInt(HashConstants.PREF_SAVE_HISTORY_DAYS,
					Integer.parseInt(strDaysHistory));
		}

		m_prefs.edit()
				.putBoolean(HashConstants.PREF_ENCRYPT_HISTORY,
						cbEncryptHistory.isChecked()).commit();

		if (!cbEncryptHistory.isChecked()) {
			m_prefs.edit().remove(HashConstants.PREF_ENCRYPT_HISTORY_PASSWORD)
					.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.history_settings, menu);
		return true;
	}

	@Override
	public void onSetPasswordDialogPositive(String passwordText) {
		byte[] prevPasswordBytes = null;
		Editor editor = m_prefs.edit();
		if (passwordText == null || passwordText.isEmpty()) {
			cbEncryptHistory.setChecked(false);
			editor.remove(HashConstants.PREF_ENCRYPT_HISTORY_PASSWORD);
			editor.commit();
		} else {
			if (HistoryManager.hasUserHistoryPassword()) {
				prevPasswordBytes = HistoryManager.getUserHistoryPassword();
			} else {
				prevPasswordBytes = HistoryManager.getHistoryPassword();
			}
			editor.putString(HashConstants.PREF_ENCRYPT_HISTORY_PASSWORD,
					CryptoManager.sha1Base64String(passwordText));
			editor.commit();
			cbEncryptHistory.setChecked(true);
			final ProgressDialog progress = ProgressDialog.show(this,
					getResources().getString(R.string.encrypting_title),
					getResources().getString(R.string.reencrypting_history));

			final byte[] prevbyes = prevPasswordBytes;
			final byte[] newbytes = HistoryManager.getHistoryPassword();
			new Thread(new Runnable() {
				@Override
				public void run() {
					HistoryManager.reencrypt_history(prevbyes, newbytes);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							progress.dismiss();
						}
					});
				}
			}).start();
		}
	}

	@Override
	public void onSetPasswordDialogNegative() {
		String currentPass = m_prefs.getString(
				HashConstants.PREF_ENCRYPT_HISTORY_PASSWORD, null);
		if (currentPass == null || currentPass.isEmpty()) {
			cbEncryptHistory.setChecked(false);
		}
	}

	@Override
	public void onPromptPasswordDialogPositive(String cleartext) {
		if (HistoryManager.authenticateUserHistory(cleartext)) {
			FragmentManager fm = getSupportFragmentManager();
			if ( m_nextDialog == "set_password" ) {
				btnSetPassword.callOnClick();
			}
			else if ( m_nextDialog == "clear_password" ) {
				btnClearPassword.callOnClick();
			}
		}

	}

	@Override
	public void onPromptPasswordDialogNegative() {

	}
}
