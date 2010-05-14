package com.snaptic.WODWhacker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncTask.Status;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.snaptic.account.AccountPreferences;
import com.snaptic.account.AccountUtil;
import com.snaptic.account.AndroidAccount;
import com.snaptic.api.SnapticAPI;
import com.snaptic.api.SnapticAccount;

public class SignInActivity extends Activity {
	// Message IDs
	private static final int LOGIN_SUCCESS = 0;
	private static final int LOGIN_FAIL = 1;
	private static final int LOGIN_ERROR = 2;
	private static final int LOGIN_ERROR_RESPONSE = 3;
	private static final int RESET_ERROR = 4;
	private static final int RESET_SUCCESS = 5;
	private static final int DO_LOGIN = 6;
	
	// Dialog IDs
	private static final int DIALOG_VERIFY_EMAIL = 0;
	
	private static final String SIGN_IN = "signin";
	private static final String FORGOT = "forgot";
	
	private SignInTask taskSignIn;
	private Dialog mVerifyEmailDialog = null;
	
	private String email = null;
	
	// Our message handler.
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Button signInButton = (Button)findViewById(R.id.sign_in_button);
			
			switch (msg.what) {
				case LOGIN_FAIL:
				    Toast.makeText(getApplicationContext(),
				    		R.string.sign_in_failed,
				    		Toast.LENGTH_LONG).show();

					signInButton.setEnabled(true);
					signInButton.setText(R.string.snaptic_sign_in_button);
				    
					break;
				case LOGIN_SUCCESS:
					Toast.makeText(getApplicationContext(),
							R.string.sign_in_success,
							Toast.LENGTH_SHORT).show();
					setResult(RESULT_OK);
					finish();
					break;
				case LOGIN_ERROR:
				    Toast.makeText(getApplicationContext(),
				    		R.string.sign_in_error,
				    		Toast.LENGTH_LONG).show();
					signInButton.setEnabled(true);
					signInButton.setText(R.string.snaptic_sign_in_button);
					break;
				case LOGIN_ERROR_RESPONSE:
				    Toast.makeText(getApplicationContext(),
				    		R.string.sign_in_error,
				    		Toast.LENGTH_LONG).show();
				    Log.e(getString(R.string.app_name), "problem with API response attempting sign-in");
					signInButton.setEnabled(true);
					signInButton.setText(R.string.snaptic_sign_in_button);
					break;
				case RESET_SUCCESS:
					Toast.makeText(getApplicationContext(),
							R.string.sign_in_reset_password_success,
							Toast.LENGTH_LONG).show();
					break;
				case RESET_ERROR:
				    Toast.makeText(getApplicationContext(),
				    		R.string.sign_in_reset_password_error,
				    		Toast.LENGTH_LONG).show();
					break;
				case DO_LOGIN:
				    signIn();
					break;
				default:
					super.handleMessage(msg);
					break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.sign_in);

		Object task = getLastNonConfigurationInstance();
		
		if (task != null && task instanceof SignInTask) {
			try {
				taskSignIn = (SignInTask) task;
				
				if (taskSignIn != null && taskSignIn.getStatus() == Status.RUNNING) {
					setProgressBarIndeterminateVisibility(true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		final SignInActivity self = this;

		if (taskSignIn != null) {
			taskSignIn.setActivity(self);
		}

		final EditText user = (EditText)findViewById(R.id.sign_in_user);
		EditText password = (EditText)findViewById(R.id.sign_in_password);
		Button signInButton = (Button)findViewById(R.id.sign_in_button);
		TextView forgotPassword = (TextView)findViewById(R.id.sign_in_forgot_password);
		TextView google = (TextView)findViewById(R.id.sign_in_google);
		
		password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					mHandler.sendEmptyMessage(DO_LOGIN);
					return true;
				}
				
				return false;
			}
		});
		
		signInButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mHandler.sendEmptyMessage(DO_LOGIN);
			}
		});
		
		forgotPassword.setText(Html.fromHtml(getString(R.string.snaptic_sign_in_forgot_password_text)));
		
		forgotPassword.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View view, MotionEvent event) {
				String userText = user.getText().toString().trim();
				
				if (userText.contains("@")) {
					email = userText;
				} else {
					email = null;
				}
				
				showDialog(DIALOG_VERIFY_EMAIL);
				return true;
			}
		});
		
		google.setText(Html.fromHtml(getString(R.string.snaptic_sign_in_google_text)));
		
		google.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View view, MotionEvent event) {
				// TODO: make this point to the permanent OpenID blog post
				Intent intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse("https://snaptic.com/blog/"));
				startActivity(intent);
				return true;
			}
		});
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return taskSignIn;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	private void signIn() {
		if (taskSignIn != null && taskSignIn.getStatus() == Status.RUNNING) {
			return;
		}
		
		String user = ((EditText)findViewById(R.id.sign_in_user)).getText().toString();
		String password = ((EditText)findViewById(R.id.sign_in_password)).getText().toString();
		ConnectivityManager cm =
		 	   (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if (cm != null && cm.getActiveNetworkInfo() != null) {
			taskSignIn = new SignInTask(this);
			taskSignIn.execute(user, password, SIGN_IN);
			setProgressBarIndeterminateVisibility(true);
			Button signInButton = (Button)findViewById(R.id.sign_in_button);
			signInButton.setEnabled(false);
			signInButton.setText(R.string.sign_in_start);
		} else {
			Toast.makeText(this,
    				R.string.snaptic_sign_in_toast_no_network,
    				Toast.LENGTH_SHORT).show();
		}

	}

	private void resetPassword(String email) {
		if (email == null || email.length() == 0 ||
			(taskSignIn != null && taskSignIn.getStatus() == Status.RUNNING)) {
			return;
		}
		
		ConnectivityManager cm =
		 	   (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if (cm != null && cm.getActiveNetworkInfo() != null) {
			taskSignIn = new SignInTask(this);
			taskSignIn.execute(email, null, FORGOT);
			setProgressBarIndeterminateVisibility(true);
		} else {
			Toast.makeText(this,
    				R.string.snaptic_sign_in_toast_no_network,
    				Toast.LENGTH_SHORT).show();
		}

	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
			case DIALOG_VERIFY_EMAIL:
				mVerifyEmailDialog = dialog;
				EditText emailEditText = (EditText) dialog.findViewById(R.id.verify_email_dialog_edittext);
				
				if (emailEditText != null && email != null) {
					emailEditText.setText(email);
				}
			
				break;
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		
		switch (id) {
			case DIALOG_VERIFY_EMAIL:
				final View verifyLayout =
					LayoutInflater.from(this).inflate(R.layout.verify_email, null);
				final EditText emailEditText =
					(EditText) verifyLayout.findViewById(R.id.verify_email_dialog_edittext);
				
				emailEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_GO) {
							String enteredEmail = emailEditText.getText().toString().trim();
							
							// Dismiss the dialog
							if (mVerifyEmailDialog != null) {
								mVerifyEmailDialog.dismiss();
							}
							
							resetPassword(enteredEmail);
						}
						
						return false;
					}
				});
				
				dialog = new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.snaptic_verify_email_dialog_title)
				.setView(verifyLayout)
				.setPositiveButton(R.string.snaptic_verify_email_dialog_button,
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int
							whichButton) {
						String enteredEmail = emailEditText.getText().toString().trim();
						resetPassword(enteredEmail);
					}
				})
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						if (email != null) {
							emailEditText.setText(email);
						} else {
							emailEditText.setText("");
						}
					}
				})
				.create();
				
				dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						mVerifyEmailDialog = null;
					}
				});

				break;
		}
		
		return dialog;
	}
	
	private class SignInTask extends AsyncTask<String, Integer, Integer> {
		private static final String appName = "aknotepad";
		private String versionName = "x.xx";
		private SnapticAPI snapticAPI;
		private SignInActivity mActivity = null;
		
		public SignInTask(SignInActivity activity) {
			mActivity = activity;
		}
		
		@Override
		protected Integer doInBackground(String... params) {
			int retval = LOGIN_ERROR;
			//This is where I need to perform surgery to get the thing to use our username and password.
			if ("signin".equals(params[2])) {
				try {
					snapticAPI = new SnapticAPI(params[0], params[1]);
					
					SnapticAccount snapticAccount = new SnapticAccount();
					
					int result = snapticAPI.getAccountInfo(snapticAccount);
	
					switch (result) {
						case SnapticAPI.RESULT_UNAUTHORIZED:
							// Likely incorrect username and/or password
							retval = LOGIN_FAIL;
							break;
						case SnapticAPI.RESULT_ERROR:
							// Uncategorized error
							retval = LOGIN_ERROR;
							break;
						case SnapticAPI.RESULT_ERROR_RESPONSE:
							// API response not what we expected to parse
							retval = LOGIN_ERROR_RESPONSE;
							break;
						case SnapticAPI.RESULT_OK:
							// Sign-in was successful
		    				AndroidAccount account = new AndroidAccount();
		    				account.setId(snapticAccount.id);
		    				account.setEmail(snapticAccount.email);
		    				account.setUsername(snapticAccount.username);
		    				account.setPassword(params[1]);
		    				account.setCreatedOn(snapticAccount.accountCreatedOn);
		    				//account.setAuthToken(snapticAccount.auth_token);
		    				account.setLoginType(AccountPreferences.LOGINTYPE_SNAPTIC);
		    				AccountUtil.saveAndroidAccount(getApplicationContext(), account);
		    				retval = LOGIN_SUCCESS;
							break;
						default:
							break;
					}
				
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if ("forgot".equals(params[2])) {
				retval = RESET_ERROR;
				
				try {
					int result = snapticAPI.resetPassword(params[0]);
	
					switch (result) {
						case SnapticAPI.RESULT_OK:
		    				retval = RESET_SUCCESS;
							break;
						case SnapticAPI.RESULT_ERROR:
						default:
							break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return new Integer(retval);
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			if (mActivity != null) {
				mActivity.mHandler.sendEmptyMessage(result.intValue());
				mActivity.setProgressBarIndeterminateVisibility(false);
				mActivity = null;
			}
		}
		
		public void setActivity(SignInActivity activity) {
			mActivity = activity;
		}
	}
}
