package my.yousendit;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.SimpleHttp;
import my.yousendit.peripherals.ApiButton;
import my.yousendit.peripherals.MyException;
import my.yousendit.peripherals.MyTextBox;
import my.yousendit.peripherals.Shared;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ResetRequest extends Activity implements ApiHandler{
	
	MyTextBox etemail;
	TextView forgoterror;
	ApiButton btnrequest;
	SimpleHttp request;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.resetrequest);
		etemail = (MyTextBox) findViewById(R.id.resemail);
		forgoterror = (TextView) findViewById(R.id.reserror);
		getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(android.R.color.transparent);
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) finish();
		return super.onOptionsItemSelected(item);
	}
	
	public void request(View view){
		if(btnrequest==null) btnrequest = (ApiButton) view;
		if(!btnrequest.isEnabled()) return;
		String email = etemail.getText().toString();
		if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
			forgoterror.setVisibility(View.VISIBLE);
			forgoterror.setText("Invalid email address");
		}else{
			forgoterror.setVisibility(View.GONE);
	    	HashMap<String,Object> params = new HashMap<String, Object>();
			params.put("email",email);
			request = SimpleHttp.request("request", Shared.url+"auth/requestreset", params, this);
			btnrequest.setBusy(true);
		}
	}
	
	public void reset(View view){
    	Intent intent = new Intent(this,ResetInsertCode.class);
    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    	startActivity(intent);
    	finish();
	}
	
	public void showerror(String msg){
		forgoterror.setVisibility(msg==null?View.GONE:View.VISIBLE);
		if(msg!=null&&msg.length()!=0) forgoterror.setText(msg);
	}
	
    @Override protected void onPause() {
    	super.onPause();
    	if(request!=null) request.abort();
    }
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		if(tag.equals("request")){
			btnrequest.setBusy(false);
			if(result==null) showerror("An unexpected error occured");
			else try{
				JSONObject obj = new JSONObject(result);
				if(obj.getInt("success")==1){
					String msg = "We've email you the password reset code. Please check";
					new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("OK",null).show();
				}else{
					String code = obj.getString("code");
					if(code.equals("email_not_exist")) showerror("Email address does not exist");
					else throw new JSONException("Server error");
				}
			}catch(JSONException jsex){
				showerror("Unexpected error. Please retry");
			}
		}
	}
}