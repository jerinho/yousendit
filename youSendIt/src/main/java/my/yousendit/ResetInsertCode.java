package my.yousendit;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.SimpleHttp;
import my.yousendit.peripherals.ApiButton;
import my.yousendit.peripherals.MyException;
import my.yousendit.peripherals.MyTextBox;
import my.yousendit.peripherals.Shared;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ResetInsertCode extends Activity implements ApiHandler{
	
	TextView tverror;
	MyTextBox etcode;
	ApiButton btnset;
	String code;
	SimpleHttp request;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.resetcode);
		tverror = (TextView) findViewById(R.id.reserror);
		etcode = (MyTextBox) findViewById(R.id.rescode);
		getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(android.R.color.transparent);
	}
	
	public void reset(View view){
		if(btnset==null) btnset = (ApiButton) view;
		if(!btnset.isEnabled()) return;
		code = etcode.getText().toString().toLowerCase();
		if(code.length()!=6){
			showerror("Invalid password reset code");
			return;
		}
    	HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("code",code);
		request = SimpleHttp.request("check", Shared.url+"auth/getCodeOwner", params, this);
		btnset.setBusy(true);
	}
	
	public void showerror(String msg){
		tverror.setVisibility(msg==null?View.GONE:View.VISIBLE);
		if(msg!=null&&msg.length()!=0) tverror.setText(msg);
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) finish();
		return super.onOptionsItemSelected(item);
	}
	
    @Override protected void onPause() {
    	super.onPause();
    	if(request!=null) request.abort();
    }
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		if(tag.equals("check")){
			btnset.setBusy(false);
			if(result==null) showerror("An unexpected error occured");
			else try{
				JSONObject obj = new JSONObject(result);
				if(obj.getInt("success")==1){
					Intent intent = new Intent(this,ResetSetPassword.class);
			    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					intent.putExtra("code",code);
					intent.putExtra("user",obj.getJSONObject("post").getString("name"));
			    	startActivity(intent);
			    	finish();
				}else{
					String code = obj.getString("code");
					if(code.equals("codeowner_not_exist")) showerror("Invalid password reset code");
					else throw new JSONException("Server error");
				}
			}catch(JSONException jsex){
				showerror("Unexpected error. Please retry");
			}
		}
	}
}