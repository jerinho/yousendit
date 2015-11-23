package my.yousendit;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.Marker;

import my.helper.ApiRequest.ApiHandler;
import my.helper.Map;
import my.helper.Notifier;
import my.helper.Notifier.GcmBroadcastReceiver;
import my.helper.SimpleHttp;
import my.yousendit.peripherals.DrawerActivity;
import my.yousendit.peripherals.Shared;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

public class Home extends DrawerActivity implements ApiHandler, OnClickListener{
	
	public static MyMapListener maplistener;
	
	public static String msgtrans = "Thank you for your interest to be a courier.";
	public static String msgver = "We need to go through some verification processes, before you can become our courier.";
	public static String msgpaymenttrans = "Before you can become a transporter, please add your credit card information";
	public static String msgpaymentsender = "Before proceed to your first job, please add your credit card information";
	public static String msgbills = "Email any TWO of the utility bills below to us at beacourier@xxx.com"
		+ "\n- Electrical bill \n- Water bill \n- Gas bill";
	public static String msgid = "Email your any of following to us as beacourier@xxx.com"
		+ "\n- Identification card \n- Driving license \n- Passport";
	public static String msgcharge =  "US$1.00 will be charged after we receive your email, and we will refund it to you after your first job is completed";
	
    @Override public View setMainView() {
    	return inflate(R.layout.home);
    }
    
    @Override protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		if(Shared.id==0) System.exit(0);
    	Bundle extras = getIntent().getExtras();
    	if(extras==null) return;
    	String strcode = extras.getString("code","0");
    	String strjob = extras.getString("jobId","0");
    	int code = strcode.equals("")?0:Integer.valueOf(strcode);
    	int job = strjob.equals("")?0:Integer.valueOf(strjob);
    	Class open;
    	if(code==0) open = Notifications.class;
    	else if(code==7) open = JobSearch.class;
    	else if(code==8) open = Chat.class;
    	else open = FragsSteps.class;
		Intent intent = new Intent(this, open);
    	String post = extras.getString("post",null);
    	if(code==0) DrawerActivity.current = 2;
    	else if(code==7) intent.putExtra("post",post).putExtra(Map.TITLE, "Find A Job");
    	else if(code==8) try{
        	JSONObject json = post==null?null:new JSONObject(post);
        	String name = json.getString("senderName");
        	int userid = json.getInt("sender");
        	intent.putExtra("chatonly", false).putExtra("id",userid).putExtra("name", name);
    	}catch(JSONException ex){}
    	else intent.putExtra("code", code).putExtra("id",job);
	    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    startActivity(intent);
    }
    
    public void showMessage(boolean reqtrans, boolean istrans, boolean payment){
    	if(!reqtrans&&!payment) new AlertDialog.Builder(this).setMessage(msgpaymentsender).setPositiveButton("Add credit card",this).setNegativeButton("Cancel", null).show();
    	else if(reqtrans&&!payment&&istrans) new AlertDialog.Builder(this).setMessage(msgpaymenttrans).setPositiveButton("Add credit card",this).setNegativeButton("Cancel", null).show();
    	else if(reqtrans&&!payment&&!istrans){
			String msg = msgtrans+"\n\n"+msgver+"\n\n1. "+msgpaymenttrans+"\n\n2. "+msgbills+"\n\n3. "+msgid+"\n\n4. "+msgcharge;
			new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("Add credit card",this).setNegativeButton("Cancel", null).show();
		}else if(reqtrans&&payment&&!istrans){
			String msg = msgtrans+"\n\n"+msgver+"\n\n1. "+msgbills+"\n\n2. "+msgid+"\n\n3. "+msgcharge;
			new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("OK",null).show();
		}
    }
    
    public void postjob(View view){
    	showMessage(false, Shared.istransporter, Shared.haspayment);
    	if(!Shared.haspayment) return;
		Intent intent = new Intent(this, JobForm.class);
	    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    startActivity(intent);
    }

    public void searchjob(View view){
    	showMessage(true, Shared.istransporter, Shared.haspayment);
    	if(!Shared.haspayment||!Shared.istransporter) return;
		if(maplistener==null) maplistener = new MyMapListener();
		Intent intent = new Intent(this, JobSearch.class);
	    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
	    	.putExtra(Map.LISTENER, getClass().getCanonicalName()).putExtra(Map.TITLE,"Find A Job").putExtra(Map.CENTER, true)
	    	.putExtra(Map.LOCATION, true).putExtra(Map.REVERSE, true).putExtra(Map.BGCLEAR,R.drawable.selectorclear)
	    	.putExtra(Map.BGCENTER, R.drawable.icon_centrelocation).putExtra(Map.POSITIONCENTER, Gravity.CENTER);
	    startActivity(intent);
    }
    
	@Override public void onClick(DialogInterface dialog, int which) {
		Intent intent = new Intent(this, Payment.class);
		intent.putExtra("add", true);
		startActivity(intent);
	}
    
    public static class MyMapListener extends JobSearch.MapListener implements ApiHandler{

    	ArrayList<String> multispickup = new ArrayList<String>();
    	ArrayList<String> multisdropoff = new ArrayList<String>();
		HashMap<String,String> geocodes =  new HashMap<String, String>();
    	String keypickup, keydropoff;
    	int index, count;
		Marker clicked;
		
    	public void viewjob(int id) throws JSONException{
			Intent intent = new Intent(map, FragsSteps.class);
		    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    intent.putExtra("id",id);
		    map.startActivity(intent);
    	}
    	
		@Override public boolean onMapDrag() {
			if(map==null||map.camera==null||map.camera.target==null) return false;
			HashMap<String, Object> pars = new HashMap<String, Object>();
	    	pars.put("loginkey",Shared.loginkey);
	    	pars.put("longitude",String.valueOf(map.camera.target.latitude));
	    	pars.put("latitude",String.valueOf(map.camera.target.longitude));
	    	//LatLngBounds bounds = activity.map.getProjection().getVisibleRegion().latLngBounds;
	    	//LatLng ne = bounds.northeast, sw = bounds.southwest;
	    	//pars.put("latmin",String.valueOf(sw.latitude));
	    	//pars.put("latmax",String.valueOf(ne.latitude));
	    	//pars.put("lonmin",String.valueOf(sw.longitude));
	    	//pars.put("lonmax",String.valueOf(ne.longitude));
	    	SimpleHttp.request("search", Shared.url+"jobs/nearlocation", pars, this);
			return false;
		}

		@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
			if(result==null) return;
			if(tag.equals("search")) try{
				map.clearMarks();
				JSONObject json = new JSONObject(result);
				if(json.getInt("success")==1){
					JSONArray jobs = json.getJSONArray("post");
					multispickup.clear();
					multisdropoff.clear();
					count = jobs.length();
					for(int i=0; i<jobs.length();i++){
						JSONObject obj = jobs.getJSONObject(i);
						String pickup = obj.getString("pick_up_location"), dropoff = obj.getString("drop_off_location");
						String[] locpickup = pickup.split("[\\s\\,]"), locdropoff = dropoff.split("[\\s\\,]");
						double latpickup = Double.parseDouble(locpickup[0]), lonpickup = Double.parseDouble(locpickup[1]);
						double latdropoff = Double.parseDouble(locdropoff[0]), londropoff = Double.parseDouble(locdropoff[1]);
						map.putMark(false, latpickup, lonpickup, null , obj, 0, R.drawable.icon_pickup);
						map.putMark(false, latdropoff, londropoff, null , obj, 0, R.drawable.icon_dropoff);
						multispickup.add(pickup);
						multisdropoff.add(dropoff);
					}
				}else{
					String code = json.getString("code");
					if(code.startsWith("unauthenticated")){
						Toast.makeText(map, json.getString("message"), Toast.LENGTH_SHORT).show();
						map.finish();
					}
				}
			}catch(JSONException ex){}
		}
		
		@Override public boolean onMarkerClick(int index) {
			try{
				this.index = index;
				boolean even = index%2==0;
			    JSONObject job = (JSONObject) map.getTag(index);
			    keypickup = job.getString("pick_up_location");
			    keydropoff = job.getString("drop_off_location");
			    boolean b = index%2==0 && multispickup.indexOf(keypickup) == multispickup.lastIndexOf(keypickup) || index%2!=0 && multisdropoff.indexOf(keydropoff) == multisdropoff.lastIndexOf(keydropoff);
			    if(b){
			    	//clicked = activity.markers.get(index);
			    	//clicked.setIcon(BitmapDescriptorFactory.fromResource(index%2==0?R.drawable.icon_pickup_yellow:R.drawable.icon_dropoff_yellow));
			    }
			    if(b) viewjob(job.getInt("id"));
			    else{
				    map.adaptermenu.clear();
			    	for(int i=0; i<count; i++){
						if(even&&i%2==0||!even&&i%2!=0) continue;
						JSONObject obj = (JSONObject) map.getTag(i);
						if(!obj.getString(even?"pick_up_location":"drop_off_location").equals(even?keypickup:keydropoff)) continue;
						String text = (even?"to":"from")+" "+obj.getString(even?"addrto":"addrfrom");
						map.adaptermenu.add(obj.put(Map.ITEM, text));
			    	}
				    map.dialog.show();
			    }
			}catch(JSONException ex){
				//System.out.println("JSON Exception : "+ex.getMessage());
			}
			return false;
		}
		
		@Override public void onMenuItemClick(int position){
			map.dialog.hide();
			try {viewjob(map.adaptermenu.getItem(position).getInt("id"));} catch (JSONException e) {}
		}
    }
    
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
	}
}
