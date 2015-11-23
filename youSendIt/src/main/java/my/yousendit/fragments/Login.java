package my.yousendit.fragments;

import java.util.ArrayList;
import java.util.HashMap;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.Notifier;
import my.helper.Notifier.GcmBroadcastReceiver;
import my.helper.Notifier.MessageHandler;
import my.helper.SimpleHttp;
import my.yousendit.Home;
import my.yousendit.R;
import my.yousendit.ResetRequest;
import my.yousendit.R.color;
import my.yousendit.peripherals.ApiButton;
import my.yousendit.peripherals.MyTextBox;
import my.yousendit.peripherals.Shared;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Login extends Fragment implements ApiHandler, MessageHandler, OnGlobalLayoutListener, OnClickListener{

	MyTextBox txtpw, txtun;
	TextView loginerror;
	String em, pw;
	Builder dialog;
	ApiButton btnlogin;
	SimpleHttp request;
	Activity activity;
	LinearLayout wrap;
	ImageView icon;
	Button btnforgot;
	
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.login, null);
		activity = getActivity();
		txtun = ((MyTextBox) view.findViewById(R.id.txtem));
		txtpw = ((MyTextBox) view.findViewById(R.id.txtpw));
		loginerror = (TextView) view.findViewById(R.id.loginerror);
		wrap = (LinearLayout) view.findViewById(R.id.loginwrapper);
		icon = (ImageView) view.findViewById(R.id.loginicon);
		wrap.getViewTreeObserver().addOnGlobalLayoutListener(this);
		loginerror.setOnClickListener(this);
	    dialog = new Builder(activity);
		return view;
	}
	
	public void onShow(){
		//if(txtun!=null) txtun.getTextBox().requestFocus();
	}
	
	@Override public void onResume() {
		super.onResume();
		if(btnforgot!=null) btnforgot.setTextColor(Color.DKGRAY);
	}
	
	@Override public boolean onNotifierMessage(Bundle bundle, GcmBroadcastReceiver receiver) {
		return true;
	}
	
	@Override public void onNotifierKey(String gcmid) {
		Shared.gcmid = gcmid;
		requestlogin();
	}
	
	@Override public void onNotifierError(String message) {
		//Toast.makeText(getActivity(), "Error : "+message, Toast.LENGTH_SHORT).show();
		requestlogin();
	}
	
	@Override public void onClick(View v) {
		v.setVisibility(View.GONE);
	}
	
    @Override public void onPause() {
    	super.onPause();
    	if(request!=null) request.abort();
    }
	
	@Override public void onGlobalLayout() {
        int hwrap = wrap.getHeight();
        int hscreen = getResources().getDisplayMetrics().heightPixels;
        final int hicon = hscreen - hwrap - 350;
        wrap.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        /*
        icon.post(new Runnable() {
			
			@Override public void run() {
		        icon.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,hicon));
			}
		});
		*/
	}
	
	public void showerror(String msg){
		loginerror.setVisibility(msg==null?View.GONE:View.VISIBLE);
		if(msg!=null&&msg.length()!=0) loginerror.setText(msg);
	}
	
	public void login(View view){
		if(btnlogin==null) btnlogin = (ApiButton) view;
		if(!btnlogin.isEnabled()) return;
		em = txtun.getText().toString();
		pw = txtpw.getText().toString();
		if(em.length()==0||pw.length()==0) showerror("Please insert both login name and password");
		else if(!Patterns.EMAIL_ADDRESS.matcher(em).matches()) showerror("Invalid email address");
		else if(!pw.matches(Shared.regexpassword)) showerror("Invalid password");
		else{
			showerror(null);
			if(Shared.gcmid==null) Notifier.setHandler(getActivity(), this, Shared.projectkey);
			else requestlogin();
		}
	}
	
	public void requestlogin(){
		btnlogin.setBusy(true);
    	HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("email",em);
		params.put("password",pw);
		if(Shared.gcmid!=null) params.put("regId", Shared.gcmid);
		request = SimpleHttp.request("login", Shared.url+"auth/login", params, this);
	}
	
	public void forgot(View view){
		//If opt by user, App will send API request to reset the password, before server send the email contains password reset code to the user
		//User will be redirected to the Password Reset Screen
		//User can either enter the reset code manually or redirected to the Reset Screen via link
		if(btnlogin!=null&&!btnlogin.isEnabled()) return;
		btnforgot = ((Button) view);
		btnforgot.setTextColor(getResources().getColor(R.color.theme));
    	Intent intent = new Intent(getActivity(),ResetRequest.class);
    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    	startActivity(intent);
	}
	
	public void facebook(View view){
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		if(tag.equals("login")){
			btnlogin.setBusy(false);
			if(result==null) showerror("Unexpected error. Please retry");
			else try{
				JSONObject obj = new JSONObject(result);
				if(obj.getInt("success")==1){
					JSONObject post = obj.getJSONObject("post");
					Shared.editor.putString("loginkey",Shared.loginkey = post.isNull("loginkey")?null:post.getString("loginkey"));
					Shared.editor.putString("name", Shared.name = post.isNull("name")?null:post.getString("name"));
					Shared.editor.putString("email", Shared.email = post.isNull("email")?null:post.getString("email"));
					Shared.editor.putBoolean("transporter",Shared.istransporter = post.isNull("isTransporter")?false:(post.getInt("isTransporter")==1));
					Shared.editor.putBoolean("payment",Shared.haspayment = post.isNull("hasCreditCard")?false:(post.getInt("hasCreditCard")==1));
					Shared.editor.putInt("id",Shared.id = post.isNull("id")?null:post.getInt("id"));
					Shared.editor.putString("image", Shared.image = post.isNull("image")?null:post.getString("image"));
					Shared.editor.apply();
					Shared.global.edit().putString("key",Shared.loginkey).apply();
					if(activity==null) return;
			    	Intent intent = new Intent(activity,Home.class);
			    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			    	startActivity(intent);
			    	btnlogin.setEnabled(false);
			    	btnlogin.setText("SUCCESSFUL !");
			    	activity.finish();
			    	//System.out.println("Your login key is "+Shared.loginkey);
				}else{
					String code = obj.getString("code");
					if(code.equals("wrong_login")) showerror("Invalid email or password");
					else throw new JSONException("Server error");
				}
			}catch(JSONException jsex){
				showerror("Unexpected error. Please retry");
			}
		}
	}
}