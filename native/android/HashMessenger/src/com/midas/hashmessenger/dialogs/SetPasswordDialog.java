package com.midas.hashmessenger.dialogs;

import com.midas.hashmessenger.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.Log;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.EditText;
import android.widget.TextView;

public class SetPasswordDialog extends DialogFragment {

	public interface SetPasswordDialogListener {
		void onSetPasswordDialogPositive(String passwordText);

		void onSetPasswordDialogNegative();
	}

	private EditText txtPassword1 = null;
	private EditText txtPassword2 = null;
	private TextView lblPasswordError = null;
	private AlertDialog thisDialog = null;

	public SetPasswordDialog() {

	}

	private boolean doPasswordsMatch() {
		if (txtPassword1.getText().toString()
				.equals(txtPassword2.getText().toString())) {
			return true;
		} else {
			return false;
		}
	}

	private void checkPasswords() {
		if (doPasswordsMatch()) {
			lblPasswordError.setVisibility(View.INVISIBLE);
			if (thisDialog != null) {
				thisDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
			}
		} else {
			lblPasswordError.setVisibility(View.VISIBLE);
			if (thisDialog != null) {
				thisDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
			}
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.set_password)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								SetPasswordDialogListener activity = (SetPasswordDialogListener) getActivity();
								activity.onSetPasswordDialogPositive(txtPassword2
										.getText().toString());
								SetPasswordDialog.this.dismiss();

							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								SetPasswordDialogListener activity = (SetPasswordDialogListener) getActivity();
								activity.onSetPasswordDialogNegative();
								SetPasswordDialog.this.dismiss();
							}
						});

		LayoutInflater i = getActivity().getLayoutInflater();

		View v = i.inflate(R.layout.dialog_setpassword, null);

		txtPassword1 = (EditText) v.findViewById(R.id.txtPassword1);
		txtPassword2 = (EditText) v.findViewById(R.id.txtPassword2);
		lblPasswordError = (TextView) v.findViewById(R.id.lblPasswordError);

		txtPassword1.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				checkPasswords();
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		});

		txtPassword2.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				checkPasswords();
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		});

		b.setView(v);
		thisDialog = b.create();
		return thisDialog;
	}
}