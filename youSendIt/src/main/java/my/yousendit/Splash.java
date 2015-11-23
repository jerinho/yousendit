package my.yousendit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.SimpleHttp;
import my.yousendit.peripherals.Shared;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Splash extends Activity implements ApiHandler, OnClickListener{
	
	int pending = 2;
	TextView status;
	ImageView icon;
	SimpleHttp request;
	AlertDialog dialog;
	boolean doneloc, donecheck;
	
	@Override protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		if(!isNetworkConnected()){
			Toast.makeText(this, "Sorry the app require internet connection", Toast.LENGTH_SHORT).show();
			finish();
		}
		LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.splash, null);
		status = (TextView) view.findViewById(R.id.splashstatus);
		icon = (ImageView) view.findViewById(R.id.splashicon);
		setContentView(view);
		Shared.global = getSharedPreferences("globpref", MODE_WORLD_READABLE);
		Shared.userpref = getSharedPreferences("userpref", MODE_PRIVATE);
		Shared.editor = Shared.userpref.edit();
		Shared.loginkey = Shared.userpref.getString("loginkey", null);
		Shared.istransporter = Shared.userpref.getBoolean("transporter", false);
		Shared.haspayment = Shared.userpref.getBoolean("payment", false);
		Shared.image = Shared.userpref.getString("image", null);
		Shared.name = Shared.userpref.getString("name",null);
		Shared.email = Shared.userpref.getString("email", null);
		Shared.id = Shared.userpref.getInt("id", 0);
		Shared.countries = Shared.userpref.contains("sorted")?Shared.userpref.getString("sorted",null).split("\\,"):null;
		Shared.countriesbycode = Shared.userpref.contains("countries")?Shared.userpref.getString("countries",null).split("\\,"):null;
	    Shared.android = android.os.Build.VERSION.RELEASE;
	    status.setText("Get app content...");
	    status.setTextColor(Color.WHITE);
	    doneloc = donecheck = false;
	    if(Shared.countries!=null){
	    	doneloc = true;
	    	jump();
	    }else{
    		request = SimpleHttp.request("countries","http://www.yousendit.com.my/api/countries", null, this);
    		status.setText("Updating app content...");
    	}
	    if(Shared.loginkey==null){
	    	donecheck = true;
	    	logout();
	    }else{
	    	HashMap<String,Object> pars = new HashMap<String, Object>();
	    	pars.put("code",Shared.loginkey);
	    	request = SimpleHttp.request("check", Shared.url+"auth/checkcode", pars, this);
    		status.setText("Verifying your access authorization...");
	    }
	}
	
	@Override protected void onStart() {
		super.onStart();
		resume(null);
	}
	
    @Override protected void onPause() {
    	super.onPause();
    	if(request!=null) request.abort();
    	if(dialog!=null&&dialog.isShowing()) System.exit(0);
    }
    
	@Override protected void onResume() {
		super.onResume();
		Shared.screen = this;
	}

	public void resume(View view){
	    MyTimer timer = new MyTimer();
	    timer.ctx = this;
	    timer.start();
	}

	private static class MyTimer extends Thread{

		public Splash ctx;
        public void run(){
            try{
                sleep(2000);
            }catch (InterruptedException e) {
                e.printStackTrace();
            }finally{
            	ctx.jump();
            }
        }
	}
	
	public void jump(){
		//System.out.println("Request to jump to next screen. Count = "+pending);
		if(pending!=0){
			pending--;
			return;
		}
		Intent intent = new Intent(this, Shared.loginkey==null?FragsLogin.class:Home.class);
	    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		Bundle extras = getIntent().getExtras();
	    if(Shared.loginkey!=null&&extras!=null) intent.putExtras(extras);
	    startActivity(intent);
	    finish();
	}
	
	private boolean isNetworkConnected() {
		NetworkInfo ani = ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		return ani!=null&&ani.isConnected();
	}
	
	public void logout(){
    	Shared.editor.remove("loginkey").remove("transporter").apply();
    	Shared.istransporter = false;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("loginkey",Shared.loginkey);
		Shared.loginkey = null;
		request = SimpleHttp.request("logout", Shared.url+"auth/logout", params, this);
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		//Jump if logged out or login key is valid
		//Logout if login key is invalid, unexpected API result, or the timeout is expired (weak internet connection)
		if(result==null&&!tag.equals("logout")){
			dialog = new AlertDialog.Builder(this).setTitle("Connection Problem").setMessage("Either your connection is weak or the server is busy").setPositiveButton("RETRY",this).setNegativeButton("EXIT",this).create();
			dialog.setCancelable(false);
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();
		}
		else if(tag.equals("logout")){
			jump();
		}
		else if(tag.equals("check")) try{
			donecheck = true;
			JSONObject json = new JSONObject(result);
			if(json.getInt("success")==1){
				String code = json.getString("code");
				if(code.equals("code_exist")) jump();
				else logout();
			}else logout();
		}catch(JSONException ex){
			logout();
		}
		else if(tag.equals("countries")){
			doneloc = true;
			Shared.countriesbycode = result.split("\\,");
			for(int i=0; i<Shared.countriesbycode.length; i++) Shared.countriesbycode[i] = Shared.countriesbycode[i].trim();
			Shared.countries = Shared.countriesbycode.clone();
			Arrays.sort(Shared.countries);
			//System.out.println("Countries list : "+Shared.countries);
			//System.out.println("Countries list by code : "+Shared.countriesbycode);
			StringBuilder sb =null;
			sb = new StringBuilder();
			for(int i = 0; i < Shared.countriesbycode.length; i++) sb.append(Shared.countriesbycode[i]+(i<Shared.countriesbycode.length-1?",":""));
			Shared.editor.putString("countries",sb.toString()).apply();
			sb = new StringBuilder();
			for(int i = 0; i < Shared.countries.length; i++) sb.append(Shared.countries[i]+(i<Shared.countries.length-1?",":""));
			Shared.editor.putString("sorted",sb.toString()).apply();
			jump();
		}
	}
	
	@Override public void onClick(DialogInterface dialog, int which) {
		if(which==AlertDialog.BUTTON_NEGATIVE) finish();
		else{
			if(!doneloc){
	    		request = SimpleHttp.request("countries","http://www.yousendit.com.my/api/countries", null, this);
	    		status.setText("Updating app content...");
			}
			if(!donecheck){
		    	HashMap<String,Object> pars = new HashMap<String, Object>();
		    	pars.put("code",Shared.loginkey);
		    	request = SimpleHttp.request("check", Shared.url+"auth/checkcode", pars, this);
	    		status.setText("Verifying your access authorization...");
			}
		}
	}
}