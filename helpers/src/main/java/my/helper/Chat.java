package my.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.Notifier;
import my.helper.Notifier.GcmBroadcastReceiver;
import my.helper.Notifier.MessageHandler;
import my.helper.OverScrollListView;
import my.helper.OverScrollListView.OverScrollListener;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class Chat extends Activity implements OverScrollListener, ApiHandler, MessageHandler{
	
	OverScrollListView list;
	EditText text;
	ArrayAdapter<String> adapter;
	ArrayList<JSONObject> objs;
	int job, with;
	String name, earliest, latest, mygcmid, yourgcmid, lastlogin, loginkey, url;
	Button btnsend;
	boolean requesting; //on getting messages. to prevent multiple request
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = getIntent().getExtras();
		if(bundle==null||!bundle.containsKey("job")||!bundle.containsKey("with")) finish();
		with = bundle.getInt("with");
		job = bundle.getInt("job");
		setContentView(R.layout.chatbox);
		list = (OverScrollListView) findViewById(R.id.chatboxlist);
		objs = new ArrayList<JSONObject>();
		adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
		list.setAdapter(adapter);
		list.setListener(this);
		text = (EditText) findViewById(R.id.chatboxtext);
		btnsend = (Button) findViewById(R.id.chatboxsend);
    	HashMap<String, Object> pars = new HashMap<String, Object>();
    	pars.put("lk", loginkey);
    	pars.put("with", String.valueOf(with));
    	pars.put("job", String.valueOf(job));
    	ApiRequest.request("info", url+"chatinfo", pars, this);
    	requesting = true;
	}

	@Override public void onOverScroll(boolean istop) {
		if(requesting||!istop) return;
    	Toast.makeText(this, "Get previous messages...", Toast.LENGTH_SHORT).show();
    	HashMap<String, Object> pars = new HashMap<String, Object>();
    	pars.put("lk", loginkey);
    	pars.put("with", String.valueOf(with));
    	pars.put("job", String.valueOf(job));
    	pars.put("nom", String.valueOf(list.getLastVisiblePosition()-list.getFirstVisiblePosition()+1));
    	pars.put("time", earliest);
    	ApiRequest.request("list", url+"chatmessages", pars, this);
    	requesting = true;
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		requesting = false;
		if(result==null) return;
		else if(tag.equals("info")) try{
			btnsend.setEnabled(true);
			JSONObject obj = new JSONObject(result);
			if(obj.has("yourname")){
				name = obj.getString("yourname");
				setTitle(name);
			}
			if(obj.has("mygcm")) mygcmid = obj.getString("mygcm");
			if(obj.has("yourgcm")) yourgcmid = obj.getString("yourgcm");
			if(obj.has("lastlogin")) lastlogin = obj.getString("lastlogin");
			Notifier.setHandler(this, this, "454466486999");
	    	HashMap<String, Object> pars = new HashMap<String, Object>();
	    	pars.put("lk", loginkey);
	    	pars.put("with", String.valueOf(with));
	    	pars.put("job", String.valueOf(job));
	    	ApiRequest.request("list", url+"chatmessages", pars, this);
	    	requesting = true;
		}catch (JSONException e) {}
		else if(tag.equals("list")) try {
			int length = adapter.getCount();
			JSONArray jarr = new JSONArray(result);
			if(length==0) latest = jarr.getJSONObject(0).getString("time");
			earliest = jarr.getJSONObject(jarr.length()-1).getString("time");
			for(int i=0; i<jarr.length(); i++){
				JSONObject jobj = jarr.getJSONObject(i);
				String msg = (jobj.getInt("by")==1?"You\t: ":"Him/Her\t: ")+jobj.getString("text");
				adapter.insert(msg,0);
				objs.add(0,jobj);
			}
			if(length==0) list.setSelection(list.getCount()-1);
			else list.smoothScrollByOffset(list.getLastVisiblePosition()-list.getFirstVisiblePosition()-1);
		}catch (JSONException e) {}
		else if(tag.equals("send")) try{
			if(!result.equals("0")) Toast.makeText(this, "Message sending fail. Please try again", Toast.LENGTH_SHORT).show();
			else{
				String msg = "You\t: "+text.getText().toString();
				adapter.add(msg);
				JSONObject jobj = new JSONObject();
				jobj.put("time", Calendar.getInstance().toString());
				objs.add(jobj);
				text.setText("");
				btnsend.setEnabled(true);
				//Ping user to notify him
		    	HashMap<String, Object> pars = new HashMap<String, Object>();
		    	pars.put("lk", loginkey);
		    	pars.put("to", String.valueOf(with));
		    	ApiRequest.request("ping", url+"chatping", pars, this);
			}
		}catch (JSONException e) {}
		else if(tag.equals("get")) try{
			System.out.println("Result : "+result);
			int length = adapter.getCount();
			JSONArray jarr = new JSONArray(result);
			latest = jarr.getJSONObject(0).getString("time");
			for(int i=0; i<jarr.length(); i++){
				JSONObject jobj = jarr.getJSONObject(i);
				adapter.add("Him/Her\t: "+jobj.getString("text"));
				objs.add(jobj);
			}
			if(length==0) list.setSelection(list.getCount()-1);
			else list.smoothScrollByOffset(list.getLastVisiblePosition()-list.getFirstVisiblePosition()-1);
			adapter.notifyDataSetChanged();
		}catch (JSONException e) {}
		else if(tag.equals("update")&&!result.equals("0")){
			Toast.makeText(this, "Fail updating your user information. Please try again", Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	
	public void send(View view){
		btnsend = (Button) view;
		view.setEnabled(false);
		String msg = text.getText().toString();
		if(msg.length()==0) return;
    	HashMap<String, Object> pars = new HashMap<String, Object>();
    	pars.put("lk", loginkey);
    	pars.put("job", String.valueOf(job));
    	pars.put("to", String.valueOf(with));
    	pars.put("msg", msg);
    	ApiRequest.request("send", url+"chatsend", pars, this);
    	requesting = true;
	}

	@Override public boolean onNotifierMessage(Bundle bundle, GcmBroadcastReceiver receiver) {
		//Handle incoming ping to fetch the message
		System.out.println("New incoming message !");
		if(name==null||!bundle.containsKey("type")||!bundle.getString("type").equals("newmessage")) return true;
    	HashMap<String, Object> pars = new HashMap<String, Object>();
    	pars.put("lk", loginkey);
    	pars.put("job", String.valueOf(job));
    	pars.put("with", String.valueOf(with));
    	pars.put("time",latest);
    	ApiRequest.request("get", url+"chatget", pars, this);
    	requesting = true;
    	return true;
	}
	
	@Override public void onNotifierKey(String gcmid) {
		if(mygcmid!=null&&mygcmid.equals(gcmid)) return;
		System.out.println("GCM id : "+gcmid);
    	HashMap<String, Object> pars = new HashMap<String, Object>();
    	pars.put("lk", loginkey);
    	pars.put("gcm", gcmid);
    	ApiRequest.request("update", url+"setgcmid", pars, this);
	}

	@Override public void onNotifierError(String message) {
		System.out.println("GCM error : "+message);
	}
}