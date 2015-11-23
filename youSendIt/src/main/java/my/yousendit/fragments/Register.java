package my.yousendit.fragments;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.SimpleHttp;
import my.yousendit.FragsLogin;
import my.yousendit.R;
import my.yousendit.peripherals.ApiButton;
import my.yousendit.peripherals.MyTextBox;
import my.yousendit.peripherals.Shared;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Path.Direction;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class Register extends Fragment implements ApiHandler, OnClickListener, android.content.DialogInterface.OnClickListener, TextWatcher, OnFocusChangeListener{
	
	MyTextBox em, un, pw, pwr;
	CheckBox cb;
	ApiButton reg;
	TextView er, erem, erun, erpw, ervp, ercb;
	String message, failemail;
	ActionBar bar;
	SimpleHttp request;
	boolean visited; //true if visited for second time and forth
	
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.register, null);
		em = (MyTextBox) view.findViewById(R.id.regemail);
		un = (MyTextBox) view.findViewById(R.id.regname);
		pw = (MyTextBox) view.findViewById(R.id.regpass);
		pwr = (MyTextBox) view.findViewById(R.id.regretype);
		cb = (CheckBox) view.findViewById(R.id.regcheck);
		reg = (ApiButton) view.findViewById(R.id.regsignup);
		er = (TextView) view.findViewById(R.id.regerror);
		erem = (TextView) view.findViewById(R.id.regerroremail);
		erun = (TextView) view.findViewById(R.id.regerrorname);
		erpw = (TextView) view.findViewById(R.id.regerrorpassword);
		ervp = (TextView) view.findViewById(R.id.regerrorverify);
		ercb = (TextView) view.findViewById(R.id.regerroragree);
		em.getTextBox().addTextChangedListener(this);
		un.getTextBox().addTextChangedListener(this);
		pw.getTextBox().addTextChangedListener(this);
		pwr.getTextBox().addTextChangedListener(this);
		em.getTextBox().setOnFocusChangeListener(this);
		un.getTextBox().setOnFocusChangeListener(this);
		pw.getTextBox().setOnFocusChangeListener(this);
		pwr.getTextBox().setOnFocusChangeListener(this);
		reg.setOnClickListener(this);
		cb.setPadding(0, cb.getPaddingTop() + 20, 0, 0);
		cb.setOnClickListener(this);
		cb.setText(Html.fromHtml("I agree to <font color='#f44336'> terms and conditions </font> and <font color='#f44336'> privacy policy </font>"));
		ScaleDrawable sd = new ScaleDrawable(getResources().getDrawable(R.drawable.selectorcheckbox), Gravity.LEFT, 1f, 1f);
		sd.setLevel(10000);
		sd.setBounds(0, 0, 80, 80);
		cb.setCompoundDrawables(sd, null, null, null);
		em.getTextBox().requestFocus();
		return view;
	}
	
	public void onShow(){
		//if(em!=null) em.getTextBox().requestFocus();
	}

	@Override public void onClick(View v) {
		//Validate and fix input
		if(!visited){
			if (v == reg) visited = true;
			else return;
		}
		boolean emexist = em.getText().toString().equals(failemail);
		boolean emempty = em.getText().length()==0;
		boolean emwrong = !Patterns.EMAIL_ADDRESS.matcher(em.getText().toString()).matches();
		boolean pwempty = pw.getText().length()==0;
		boolean pwwrong = pw.getText().length()<6||pw.getText().length()>16;
		boolean pwunmatch = !pwempty&&!pwwrong&&!pw.getText().toString().equals(pwr.getText().toString());
		boolean unempty = un.getText().length()==0;
		boolean cbempty = !cb.isChecked();
		boolean errorem = emexist||emempty||emwrong;
		boolean errorany = errorem||pwempty||pwwrong||pwunmatch||cbempty||unempty;
		er.setVisibility(errorany?View.VISIBLE:View.GONE);
		erem.setVisibility(errorem?View.VISIBLE:View.INVISIBLE);
		erun.setVisibility(unempty?View.VISIBLE:View.INVISIBLE);
		erpw.setVisibility(pwwrong?View.VISIBLE:View.INVISIBLE);
		ervp.setVisibility(pwunmatch?View.VISIBLE:View.INVISIBLE);
		ercb.setVisibility(cbempty?View.VISIBLE:View.INVISIBLE);
		if(emexist) erem.setText("Email has already been registered");
		else if(emempty) erem.setText("Please insert the email");
		else if(emwrong) erem.setText("Invalid email format");
		if(pwempty) erpw.setText("Please insert the password");
		else if(pwwrong) erpw.setText("Use 6 to 16 characters");
		else if(pwunmatch) ervp.setText("Password don't match");
		if(cbempty) ercb.setText("You have to agree to proceed");
		if(unempty) erun.setText("Please insert your display name");
		ArrayList<String> errors = new ArrayList<String>();
		if(errorem) errors.add("email");
		if(pwwrong||pwempty) errors.add("password");
		if(!pwwrong&&!pwempty&&pwunmatch) errors.add("verify password");
		if(unempty) errors.add("display name");
		if(cbempty) errors.add("agree to our terms");
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<errors.size(); i++) builder.append((i==0?"":(i==errors.size()-1?" and ":", "))+errors.get(i));
		if(er.isShown()) er.setText("Correct the following errors : "+builder.toString());
		if(v==reg&&!errorany) register();
	}
	
	public void register(){
		String email = em.getText().toString();
		String name = un.getText().toString();
		String pass = pw.getText().toString();
    	HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("email",email);
		params.put("name", name);
		params.put("password",pass);
		reg.setBusy(true);
		request = SimpleHttp.request("register", Shared.url+"auth/register", params, this);
	}
	
	@Override public void onResume() {
		super.onResume();
	}
	
	@Override public void onPause() {
		super.onPause();
		if(request!=null) request.abort();
	}
	
	@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
	}
	
	@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	@Override public void afterTextChanged(Editable s) {
		//onClick(null);
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
        case android.R.id.home:
        	getActivity().onBackPressed();
            return true;
	    }
	    return(super.onOptionsItemSelected(item));
	}
	
	@Override public void onClick(DialogInterface dialog, int which) {
		visited = false;
		em.setText("");
		un.setText("");
		pw.setText("");
		pwr.setText("");
		cb.setChecked(false);
		er.setVisibility(View.GONE);
		erem.setVisibility(View.INVISIBLE);
		erun.setVisibility(View.INVISIBLE);
		erpw.setVisibility(View.INVISIBLE);
		ervp.setVisibility(View.INVISIBLE);
		ercb.setVisibility(View.INVISIBLE);
		((FragsLogin)getActivity()).jump(1);
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		if(tag.equals("register")){
			reg.setBusy(false);
			if(result==null) Toast.makeText(getActivity(), "Unexpected error. Please retry", Toast.LENGTH_LONG).show();
			else try{
				JSONObject obj = new JSONObject(result);
				if(obj.getInt("success")==1) new AlertDialog.Builder(getActivity()).setMessage("You've sign up succesfully. You can now login").setPositiveButton("OK",this).show();
				else{
					String code = obj.getString("code");
					if(code.equals("unavailable_email")||code.equals("invalid_email")){
						failemail = em.getText().toString();
						onClick(reg);
					}else throw new JSONException("Server error");
				}
			}catch(JSONException jsex){
				Toast.makeText(getActivity(), "Unexpected result", Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override public void onFocusChange(View v, boolean hasFocus) {
		if(!hasFocus) onClick(null);
	}
}