package my.yousendit;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.Map;
import my.helper.SimpleHttp;
import my.yousendit.UserProfile.ReviewAdapter;
import my.yousendit.peripherals.DrawerActivity;
import my.yousendit.peripherals.MyException;
import my.yousendit.peripherals.Shared;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MyReview extends Activity implements ApiHandler{
	
	ListView list;
	SimpleHttp req;
	ReviewAdapter adapter;
	
    @Override protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		if(Shared.id==0) System.exit(0);
    	list = new ListView(this);
    	adapter = new ReviewAdapter(this, list, R.id.publicreviewmsg);
    	list.setAdapter(adapter);
    	setContentView(list);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(android.R.color.transparent);
    	list.setPadding(15, 15, 15, 15);
    }
    
    @Override protected void onResume() {
    	super.onResume();
    	Shared.screen = this;
		HashMap<String,Object> pars = new HashMap<String, Object>();
		pars.put("loginkey",Shared.loginkey);
		pars.put("userId", Shared.id);
		req = SimpleHttp.request("reviews",Shared.url+"profile/getReview",pars,this);
    }
    @Override protected void onPause() {
    	super.onPause();
    	if(req!=null) req.abort();
    }
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {
    	super.onOptionsItemSelected(item);
    	if(item.getItemId() == android.R.id.home) finish();
    	return super.onOptionsItemSelected(item);
    }
    
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		if(result==null){
			Toast.makeText(this, "Unexpected error. Please retry later", Toast.LENGTH_SHORT).show();
			onBackPressed();
		}else try{
			JSONObject obj = new JSONObject(result);
			if(obj.getInt("success")==1){
				adapter.clear();
				JSONArray data = obj.getJSONArray("post");
				for(int i=0; i<data.length(); i++) adapter.add(data.getJSONObject(i));
				adapter.notifyDataSetChanged();
			}else{
				onBackPressed();
			}
		}catch(JSONException ex){
			onBackPressed();
		}
	}
}
