package com.levkopo.apps.nashi.activities;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.levkopo.apps.nashi.Application;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.activities.BaseActivity;
import com.levkopo.vksdk.VKSdk;
import org.json.JSONObject;
import com.levkopo.vksdk.VKError;

public class LoginActivity extends AppBaseActivity
{
	
	public View loginButton;
	public View signupLink;
	public TextInputEditText username_text;
	public TextInputEditText password_text;

	private String code;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		//setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
		setTitle(R.string.login);
		
		loginButton = findViewById(R.id.btn_login);
		signupLink = findViewById(R.id.link_signup);
		username_text = findViewById(R.id.username);
		password_text = findViewById(R.id.input_password);
		loginButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					login();
				}
			});

        signupLink.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					// Start the Signup activity
					//Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
					//startActivityForResult(intent, REQUEST_SIGNUP);
				}
			});
	}
	
	public void login() {
        if (!validate()) {
            onLoginFailed();
            return;
        }

        loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        final String username = username_text.getText().toString();
        final String password = password_text.getText().toString();

        // TODO: Implement your own authentication logic here.

        new android.os.Handler().postDelayed(
			new Runnable() {
				public void run() {
					new VKSdk().auth(username, password, new VKSdk.RequestListener(){

							@Override
							public void onComplete(final VKSdk.VKResponse response) {
								VKSdk sdk = new VKSdk();
								sdk.setAccessToken(response.json.optString("access_token"));
								sdk.request("users.get", new Object[]{}, new VKSdk.RequestListener(){

										@Override
										public void onComplete(VKSdk.VKResponse r) {
											Log.d("Nashi", r.json.toString());
											JSONObject userdata_vk = r.json.optJSONArray("response").optJSONObject(0);
											AccountManager accountManager = AccountManager.get(LoginActivity.this); //this is Activity
											Account account = new Account(userdata_vk.optString("first_name")+userdata_vk.optString("last_name"), Application.accounts);
											Bundle userdata = new Bundle();
											userdata.putString("token", response.json.optString("access_token"));
											userdata.putString("id", ""+response.json.optInt("user_id"));
											accountManager.addAccountExplicitly(account, password, userdata);
											app.refresh();
											onLoginSuccess();
											progressDialog.dismiss();
										}

										@Override
										public void onError(VKError error) {
											onLoginFailed();
											progressDialog.dismiss();
										}
								});
							}

							@Override
							public void onError(VKError error) {
								Log.wtf("Nashi", error.toString());
								if(error.error_msg.equals("need_validation")){
									startActivityForResult(new Intent(LoginActivity.this, LoginCodeActivity.class), 1);
								}else
									onLoginFailed();
								progressDialog.dismiss();
							}
						}, code);
					
					// On complete call either onLoginSuccess or onLoginFailed
					// onLoginFailed()
				}
			}, 3000);
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode==1){
			if(data.getStringExtra("code")!=null){
				this.code = ""+data.getStringExtra("code");
				login();
			}else
				onLoginFailed();
		}
	}
	
	@Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        loginButton.setEnabled(true);
		startActivity(MainActivity.class);
        finish();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = username_text.getText().toString();
        String password = password_text.getText().toString();

        if (email.isEmpty()) {
            username_text.setError("enter a valid username");
            valid = false;
        } else {
            username_text.setError(null);
        }

        if (password.isEmpty() || password.length() < 4) {
            password_text.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            password_text.setError(null);
        }

        return valid;
    }
}
