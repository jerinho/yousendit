package my.yousendit;

import android.view.View;
import my.helper.Fragmentivity;
import my.yousendit.fragments.Login;
import my.yousendit.fragments.Register;

public class FragsLogin extends Fragmentivity{
	
	Login login;
	Register register;
	
	//by default, if user swipe to the left fragment, focus go to and start with bottom item
	
	protected void onCreate(android.os.Bundle args) {
		super.onCreate(args);
		login = new Login();
		register = new Register();
		setSwipable(false);
		setActionBar(false);
		setTabBar(true);
		setStateBar(false);
		add("register",register,"Sign Up");
		addandjump("login",login,"Login",false);
	}
	
	@Override protected void onResume() {
		super.onResume();
		if(register!=null) register.onShow();
		if(login!=null) login.onShow();
	}
	
	public void login(View view){
		login.login(view);
	}
	public void forgot(View view){
		login.forgot(view);
	}
	public void facebook(View view){
		login.facebook(view);
	}
}