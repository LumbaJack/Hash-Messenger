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

public class PromptPasswordDialog extends DialogFragment {

	public interface PromptPasswordDialogListener {
		void onPromptPasswordDialogPositive(String cleartext);

		void onPromptPasswordDialogNegative();
	}

	private EditText txtPassword1 = null;
	private AlertDialog thisDialog = null;

	public PromptPasswordDialog() {

	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater i = getActivity().getLayoutInflater();
		View v = i.inflate(R.layout.dialog_promptpassword, null);

		txtPassword1 = (EditText) v.findViewById(R.id.txtPassword1);

		AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.enter_password)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								PromptPasswordDialogListener activity = (PromptPasswordDialogListener) getActivity();
								activity.onPromptPasswordDialogPositive(txtPassword1
										.getText().toString());
								PromptPasswordDialog.this.dismiss();
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								PromptPasswordDialogListener activity = (PromptPasswordDialogListener) getActivity();
								activity.onPromptPasswordDialogNegative();
								PromptPasswordDialog.this.dismiss();
							}
						});

		b.setView(v);
		thisDialog = b.create();
		return thisDialog;
	}
}