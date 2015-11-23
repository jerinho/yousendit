package my.yousendit.fragments;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import my.helper.ApiRequest;
import my.helper.JrgAndroid;
import my.helper.ApiRequest.ApiHandler;
import my.helper.SimpleHttp;
import my.yousendit.FragsSteps;
import my.yousendit.R;
import my.yousendit.UserProfile;
import my.yousendit.peripherals.Shared;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

public class MyJob extends Fragment implements ApiHandler{

	ListView list;
	ArrayList<JSONObject> myjobs;
	ArrayAdapter<JSONObject> adapter;
	boolean assender, activejobs;
	SimpleHttp request;
	String result;
	
	static String[] statusposted = new String[]{
		"Job is done",
		"Awarded to NNN. Pick up date not yet confirmed",
		"Awarded to NNN. Pick up date is on DDD",
		"NNN need you to confirm that the item was picked up",
		"Waiting for delivery",
		"Job is still opened"
	};
	static String[] statusaccepted = new String[]{
		"Job is done. Your job rates is RRR",
		"Awarded by NNN. Pick up date not yet confirmed",
		"Awarded by NNN. Pick up date is on DDD",
		"Waiting from NNN to confirm the pick up",
		"Please deliver before DDD"
	};
	
	public MyJob() {
		super();
	}
	
    public void setAsSender(boolean assender){
        this.assender = assender;
    }

    public void setIsActiveJobs(boolean activejobs){
        this.activejobs = activejobs;
    }

	//setUserVisibleHint been called first before onCreateView, but onCreateView only called once
	//API request should only be called after the view have finish created

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
    	list = new ListView(getActivity());
		myjobs = new ArrayList<JSONObject>();
		adapter = new MyJobAdapter(assender, activejobs, getActivity(), R.layout.myjobitem, myjobs);
    	list.setAdapter(adapter);
    	return list;
	}

	@Override public void setUserVisibleHint(boolean visible) {
		super.setUserVisibleHint(visible);
		if(visible&&adapter!=null&&adapter.getCount()==0) Toast.makeText(getActivity(), "You're not yet have any "+(assender?"posted":"accepted")+" jobs", Toast.LENGTH_SHORT).show();
	}
	
	@Override public void onResume() {
		super.onResume();
    	if(result==null) request();
	}
	
	@Override public void onPause() {
		super.onPause();
		if(request!=null) request.abort();
	}
	
	public void request(){
    	HashMap<String,Object> pars = new HashMap<String, Object>();
    	pars.put("loginkey",Shared.loginkey);
    	if(!activejobs) pars.put("jobHistory", 1);
    	request = SimpleHttp.request("list", Shared.url+"jobs/"+(assender?"postedjobs":"acceptedjobs"), pars, this);
	}
	
	public void putItems(String result) throws JSONException{
		if(adapter==null) return;
		Activity activity = getActivity();
		String[] subs = result.split(Pattern.quote("</div>"));
		JSONObject obj = new JSONObject(subs[subs.length-1]);
		if(obj.getInt("success")==1){
			String code = obj.getString("code");
			if(code.equals("no_records_found")||!obj.has("post")) Toast.makeText(activity, "You're not yet have any "+(assender?"posted":"accepted")+" jobs", Toast.LENGTH_SHORT).show();
			else{
				JSONArray post = obj.getJSONArray("post");
				adapter.clear();
				if(post==null||post.length()==0) Toast.makeText(activity, "You're not yet have any "+(assender?"posted":"accepted")+" jobs", Toast.LENGTH_SHORT).show();
				else for(int i=0;i<post.length();i++){
					JSONObject job = post.getJSONObject(i);
					if(activejobs||!job.isNull("timedropoff")) adapter.add(job);
				}
				adapter.notifyDataSetChanged();
			}
		}else{
			Toast.makeText(activity, "Server error. Please retry", Toast.LENGTH_SHORT).show();
		}
	}
		
	public static class MyJobAdapter extends ArrayAdapter<JSONObject> implements OnClickListener{

		LayoutInflater inflater;
		View prev;
		Context context;
		String[] messages;
		boolean assender, active;
	    Transformation transform = new Transformation() {
			
			@Override public Bitmap transform(Bitmap bmp) {
				Bitmap cropped = JrgAndroid.circleCrop(bmp,100,100);
				bmp.recycle();
				return cropped;
			}
			
			@Override public String key() {
				return "myjob";
			}
		};

		public MyJobAdapter(boolean assender, boolean active, Context context, int layout, List<JSONObject> obj) {
			super(context, layout, R.id.myjobto, obj);
			this.context = context;
			this.assender = assender;
			this.messages = assender?MyJob.statusposted:MyJob.statusaccepted;
			this.active = active;
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		@Override public View getView(int pos, View convert, ViewGroup parent) {
			View view = convert;
			if(view==null) view = inflater.inflate(R.layout.myjobitem,null);
			JSONObject obj = getItem(pos);
			TextView tvfrom = (TextView) view.findViewById(R.id.myjobfrom);
			TextView tvto = (TextView) view.findViewById(R.id.myjobto);
			TextView tvstatus = (TextView) view.findViewById(R.id.myjobstatus);
			TextView tvname = (TextView) view.findViewById(R.id.myjobname);
			RatingBar rb = (RatingBar) view.findViewById(R.id.myjobrate);
			ImageView img = (ImageView) view.findViewById(R.id.myjobimg);
			try {
				String name = null, agreed = null, image = null;
				int rate = -1, userid = 0;
				String addrfrom = obj.getString("addrfrom");
				String addrto = obj.getString("addrto");
				Date timepost = null, timedone = null;
				if(!active){
					Shared.sqltime.setTimeZone(TimeZone.getTimeZone("utc"));
					timepost = Shared.sqltime.parse(obj.getString("on"));
					timedone = Shared.sqltime.parse(obj.getString("timedropoff"));
					Shared.sqltime.setTimeZone(TimeZone.getDefault());
				}
				int stat = 0;
				if(!obj.isNull("status")) obj.getInt("status");
				if(!obj.isNull("name")) name = obj.getString("name");
				if(!obj.isNull("transporter_name")) name = obj.getString("transporter_name");
				if(!obj.isNull("agreed")) agreed = obj.getString("agreed");
				if(!obj.isNull("rate")) rate = obj.getInt("rate");
				if(!obj.isNull("image")) image = obj.getString("image");
				if(!obj.isNull(assender?"transporter_id":"by")) userid = obj.getInt(assender?"transporter_id":"by");
				String message = null;
				if(!obj.isNull("message")) message = obj.getString("message");
				else if(stat!=0){
					message = messages[stat-1];
					if(name!=null) message = message.replace("NNN", name);
					if(agreed!=null) message = message.replace("DDD", agreed);
					if(rate!=-1) message = message.replace("RRR", String.valueOf(rate));
				}
				rb.setVisibility(rate!=-1?View.VISIBLE:View.GONE);
				if(rate!=-1) rb.setRating((float) obj.getInt("rate"));
				tvname.setVisibility(name!=null?View.VISIBLE:View.GONE);
				tvname.setText(name!=null?name:"");
				tvstatus.setText(active?message:("Posted on "+Shared.usertime.format(timepost)+"\nDone on "+Shared.usertime.format(timedone)));
				tvfrom.setText(Html.fromHtml("<font color='grey'>From</font> "+addrfrom));
				tvto.setText(Html.fromHtml("<font color='grey'>to</font> "+addrto));
				if(image!=null) Picasso.with(context).load(image).fit().centerInside().transform(transform).into(img);
				int jobid = obj.getInt("id");
				view.setTag(jobid);
				tvfrom.setTag(jobid);
				tvfrom.setOnClickListener(this);
				tvto.setTag(jobid);
				tvto.setOnClickListener(this);
				if(userid==0) return view;
				img.setTag(userid);
				img.setOnClickListener(this);
			}catch(JSONException ex) {
				//System.out.println("JSON Exception on MyJob : "+ex.getMessage());
			}catch(ParseException ex){
				//System.out.println("Parse Exception on MyJob : "+ex.getMessage());
			}
			return view;
		}
		
		@Override public void onClick(View v) {
			int id = (Integer) v.getTag();
			Intent intent = new Intent(context, (v instanceof ImageView)?UserProfile.class:FragsSteps.class);
		    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("id",id);
		    context.startActivity(intent);
		}
	}

	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		Activity activity = getActivity();
		if(tag.equals("list")){
			if(result==null) Toast.makeText(activity, "Unexpected error. Please retry", Toast.LENGTH_SHORT).show();
			else{
				this.result = result;
				try{putItems(result);} catch (JSONException e) {Toast.makeText(getActivity(), "Unexpected result", Toast.LENGTH_SHORT).show();}
			}
		}
	}
}