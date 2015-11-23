package my.yousendit;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import my.helper.ApiRequest;
import my.helper.JrgAndroid;
import my.helper.Map;
import my.helper.Notifier;
import my.helper.ApiRequest.ApiHandler;
import my.helper.Notifier.GcmBroadcastReceiver;
import my.helper.Notifier.MessageHandler;
import my.helper.SimpleHttp;
import my.yousendit.peripherals.DrawerActivity;
import my.yousendit.peripherals.Shared;

public class Notifications extends DrawerActivity implements ApiHandler, MessageHandler{
	
	ListView list;
	NotifAdapter adapter;
	SimpleHttp request;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(Shared.id==0) System.exit(0);
		HashMap<String,Object> pars = new HashMap<String, Object>();
		pars.put("loginkey", Shared.loginkey);
		request = SimpleHttp.request("list",Shared.url+"notification/listNotification",pars,this);
	}
	
	@Override protected void onResume() {
		super.onResume();
		Notifier.setHandler(this, this, Shared.projectkey);
	}
	
	@Override protected void onPause() {
		super.onPause();
		Notifier.removeHandler();
	}
	
	@Override public boolean onNotifierMessage(Bundle bundle, GcmBroadcastReceiver receiver) {
		JSONObject obj = new JSONObject();
		try{
			obj.put("code", bundle.getInt("code")).put("jobid", bundle.getInt("jobId")).put("message", bundle.getString("message"))
			.put("image", bundle.getString("image")).put("created_at",bundle.getString("time")).put("post", bundle.getString("post"));
		}
		catch(JSONException ex){}
		adapter.insert(obj, 0);
		adapter.notifyDataSetChanged();
		return true;
	}
	
	@Override public void onNotifierError(String message) {
		Toast.makeText(this, "Fail to get the required information from your device. The app may become unresponsive", Toast.LENGTH_LONG).show();
	}

	@Override public void onNotifierKey(String gcmid) {
		if(gcmid!=null) Shared.gcmid = gcmid;
	}

	@Override public View setMainView() {
        list = new ListView(this);
		adapter = new NotifAdapter(this, R.id.notificationmessage);
		list.setAdapter(adapter);
		return list;
	}
	
	@Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add("Remove notification");
	}
	
	@Override public boolean onContextItemSelected(MenuItem item) {
		try{
			HashMap<String,Object> pars = new HashMap<String, Object>();
			pars.put("loginkey", Shared.loginkey);
			pars.put("id",adapter.getItem(adapter.selected).getInt("id"));
			ApiRequest.request("remove",Shared.url+"notification/removeNotification", pars,this);
		}catch(JSONException ex){}
		return false;
	}
	
	public static class NotifAdapter extends ArrayAdapter<JSONObject> implements OnLongClickListener, OnClickListener{
		
		Activity activity;
		LayoutInflater inflater;
		int selected;
	    Transformation transform = new Transformation() {
			
			@Override public Bitmap transform(Bitmap bmp) {
				Bitmap cropped = JrgAndroid.circleCrop(bmp,100,100);
				bmp.recycle();
				return cropped;
			}
			
			@Override public String key() {
				return "notifications";
			}
		};

		public NotifAdapter(Activity activity, int rid) {
			super(activity, rid);
			this.activity = activity;
			inflater = activity.getLayoutInflater();
		}
		
		@Override public View getView(int position, View reuse, ViewGroup parent){
			View view = reuse==null?inflater.inflate(R.layout.notificationitem, null):reuse;
			TextView viewtext = (TextView) view.findViewById(R.id.notificationmessage);
			TextView viewtime = (TextView) view.findViewById(R.id.notificationtime);
			ImageView viewimg = (ImageView) view.findViewById(R.id.notificationimage);
			JSONObject data = getItem(position);
			try{
				String text = data.getString("message");
				Shared.sqltime.setTimeZone(TimeZone.getTimeZone("UTC"));
				Date created = data.isNull("created_at")?Calendar.getInstance().getTime():Shared.sqltime.parse(data.getString("created_at"));
				Shared.sqltime.setTimeZone(TimeZone.getDefault());
				Calendar cal1 = Calendar.getInstance(), cal2 = Calendar.getInstance();
				cal2.setTime(created);
				boolean sameday = cal1.get(Calendar.YEAR)==cal2.get(Calendar.YEAR)&&cal1.get(Calendar.DAY_OF_YEAR)==cal2.get(Calendar.DAY_OF_YEAR);
				String img = data.isNull("image")?null:data.getString("image");
				if(img!=null) Picasso.with(activity).load(data.getString("image")).fit().centerInside().transform(transform).into(viewimg);
				view.setOnClickListener(this);
				view.setOnLongClickListener(this);
				view.setTag(position);
				viewtext.setText(text);
				viewtime.setText((sameday?Shared.timeonly:Shared.monthdate).format(created));
			}catch(JSONException ex){
			}catch(ParseException ex){
			}
			return view;
		}

		@Override public boolean onLongClick(View v) {
			selected = (Integer) v.getTag();
			activity.registerForContextMenu(v);
			activity.openContextMenu(v);
			return false;
		}

		@Override public void onClick(View v) {
			selected = (Integer) v.getTag();
			try{
				JSONObject obj = getItem(selected);
				int code = obj.getInt("code");
				int job = obj.has("jobid")?obj.getInt("jobid"):0;
				Intent intent = null;
				if(code==8){
					intent = new Intent(activity, Chat.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					DrawerActivity.current = 2;
				    activity.startActivity(intent);
				    activity.finish();
				}else if(code==7){
					intent = new Intent(activity, JobSearch.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
				    	.putExtra(Map.LISTENER, Home.class.getCanonicalName()).putExtra(Map.TITLE, "Find A Job")
				    	.putExtra(Map.CENTER, true).putExtra(Map.LOCATION, true).putExtra(Map.REVERSE, true)
				    	.putExtra(Map.BGCENTER, R.drawable.icon_centrelocation).putExtra(Map.BGCLEAR,R.drawable.selectorclear)
				    	.putExtra(Map.POSITIONCENTER, Gravity.CENTER);
				    activity.startActivity(intent);
				}else{
					intent = new Intent(activity, FragsSteps.class).putExtra("id", job).putExtra("code", code);
				    activity.startActivity(intent);
				}
			}catch(JSONException ex){
				//System.out.println("JSON Error : "+ex.getMessage());
			}
		}
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		if(result==null){
			Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show();
			return;
		}
		if(tag.equals("list")) try{
			JSONObject json = new JSONObject(result);
			if(json.getInt("success")==1){
				if(json.isNull("post")){
					Toast.makeText(this, "You don't have any unread notifications", Toast.LENGTH_SHORT).show();
				}else{
					JSONArray data = json.getJSONArray("post");
					adapter.clear();
					for(int i=0; i<data.length(); i++) adapter.add(data.getJSONObject(i));
					adapter.notifyDataSetChanged();
				}
			}
		}catch(JSONException ex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
		if(tag.equals("remove")) try{
			JSONObject json = new JSONObject(result);
			if(json.getInt("success")==1){
				adapter.remove(adapter.getItem(adapter.selected));
				adapter.notifyDataSetChanged();
			}else{
				String msg = json.getString("message");
				Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
			}
		}catch(JSONException ex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
	}
}