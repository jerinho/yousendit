package my.yousendit;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.SimpleHttp;
import my.yousendit.fragments.Login;
import my.yousendit.peripherals.ApiButton;
import my.yousendit.peripherals.MyException;
import my.yousendit.peripherals.MyTextBox;
import my.yousendit.peripherals.Shared;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ResetSetPassword extends Activity implements ApiHandler, OnClickListener{
	
	TextView tverror, tvgreet;
	MyTextBox etpass, etverify;
	ApiButton btnset;
	SimpleHttp request;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.resetpassword);
		tverror = (TextView) findViewById(R.id.reserror);
		tvgreet = (TextView) findViewById(R.id.resguide);
		etpass = (MyTextBox) findViewById(R.id.respassword);
		etverify = (MyTextBox) findViewById(R.id.resverify);
		getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(android.R.color.transparent);
	}
	
	public void reset(View view){
		if(btnset==null) btnset = (ApiButton) view;
		if(!btnset.isEnabled()) return;
		String pass = etpass.getText().toString();
		String verify = etverify.getText().toString();
		if(pass.length()==0) return;
		if(pass.length()!=verify.length()||!pass.equals(verify)){
			showerror("Passwords not matched");
			return;
		}
    	HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("code",getIntent().getExtras().getString("code"));
		params.put("password",pass.toLowerCase());
		params.put("verify",verify.toLowerCase());
		request = SimpleHttp.request("reset", Shared.url+"auth/reset", params, this);
		btnset.setBusy(true);
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) finish();
		return super.onOptionsItemSelected(item);
	}
	
	@Override public void onClick(DialogInterface dialog, int which) {
    	Intent intent = new Intent(this,FragsLogin.class);
    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    	startActivity(intent);
    	finish();
	}
	
    @Override protected void onPause() {
    	super.onPause();
    	if(request!=null) request.abort();
    }
	
	public void showerror(String msg){
		tverror.setVisibility(msg==null?View.GONE:View.VISIBLE);
		if(msg!=null&&msg.length()!=0) tverror.setText(msg);
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		if(tag.equals("reset")){
			btnset.setBusy(false);
			if(result==null) showerror("An unexpected error occured");
			else try{
				JSONObject obj = new JSONObject(result);
				if(obj.getInt("success")==1){
					String msg = "You've successfully changed your password. Please login using your new password";
					new AlertDialog.Builder(this).setPositiveButton("OK", this).setMessage(msg).show();
				}else{
					String code = obj.getString("code");
					if(code.equals("invalid_reset_code")) showerror("Invalid password reset code");
					else if(code.equals("invalid_password_format")) showerror("Invalid password format");
					else throw new JSONException("Server error");
				}
			}catch(JSONException jsex){
				showerror("Unexpected error. Please retry");
			}
		}
	}
}