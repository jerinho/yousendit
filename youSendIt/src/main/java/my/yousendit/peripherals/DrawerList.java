package my.yousendit.peripherals;

import java.util.ArrayList;
import java.util.HashMap;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import my.helper.ApiRequest;
import my.helper.R;
import my.helper.ApiRequest.ApiHandler;
import my.yousendit.Account;
import my.yousendit.Chat;
import my.yousendit.FragsJobs;
import my.yousendit.FragsLogin;
import my.yousendit.Home;
import my.yousendit.MyProfile;
import my.yousendit.Notifications;
import my.yousendit.Payment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class DrawerList extends ListView implements android.widget.AdapterView.OnItemClickListener, ApiHandler{
	
	ArrayAdapter<String> adapter;
    public static String[] navitem = new String[]{"Home","My Profile","Notifications","My Job","Job History","Account History","Payment","Chat","Ratings And Reviews","Logout"};
    public static Class[] classes = new Class[]{Home.class,MyProfile.class,Notifications.class,FragsJobs.class,FragsJobs.class,Account.class,Payment.class,Chat.class};
    public static int current;

	public DrawerList(Context context) {
		super(context);
		init(context);
	}
    
	public DrawerList(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	public DrawerList(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public void init(Context context){
        adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, navitem);
        setAdapter(adapter);
        setOnItemClickListener(this);
	}
	
	@Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	}
	
	public void onItemClick(int position){
		if(navitem[position].equals("Logout")){
	    	Shared.editor.remove("loginkey").apply();
	    	Shared.editor.remove("istransporter").apply();
	    	Shared.istransporter = false;
			Intent intent = new Intent(getContext(), FragsLogin.class);
		    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    getContext().startActivity(intent);
		    ((Activity) getContext()).finish();
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("loginkey",Shared.loginkey);
			ApiRequest.request("logout", Shared.url+"auth/logout", params, this);
	    	Shared.loginkey = null;
		}else if(position<classes.length&&position!=current){
			Intent intent = new Intent(getContext(), classes[position]);
		    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    if(navitem[position].equals("Job History")) intent.putExtra("active", false);
		    current = position;
		    getContext().startActivity(intent);
		    ((Activity) getContext()).finish();
		}else{
			SlidingMenu drawer = (SlidingMenu) getParent().getParent();
			if(drawer!=null) drawer.toggle();
		}
	}

	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
	}
}