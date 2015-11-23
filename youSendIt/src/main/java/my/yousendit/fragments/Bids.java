package my.yousendit.fragments;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import my.helper.ApiRequest;
import my.helper.JrgAndroid;
import my.helper.ApiRequest.ApiHandler;
import my.helper.MyFragment;
import my.helper.SimpleHttp;
import my.yousendit.Chat;
import my.yousendit.FragsSteps;
import my.yousendit.R;
import my.yousendit.UserProfile;
import my.yousendit.peripherals.Shared;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

public class Bids extends MyFragment implements ApiHandler, OnClickListener{

	BidsAdapter adapter;
	ImageButton accept;
	ListView list;
	boolean enabledaccept = true, loaded;
	SimpleHttp request;
	View selected, accepted;
	AlertDialog dialogaward, dialogdone;
	static String empty = "You do not have any bids for this job";
	
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		//list = (ListView) inflater.inflate(R.layout.bids,null);
		list = new ListView(getActivity());
		adapter = new BidsAdapter(activity, R.layout.bidsitem, new ArrayList<JSONObject>());
		list.setAdapter(adapter);
		loaded = false;
		return list;
	}
	
	@Override public void onVisible() {
		if(getActivity().getIntent().getBooleanExtra("reload",false)) loaded = false;
		if(loaded){
			if(adapter.isEmpty()) Toast.makeText(activity, empty, Toast.LENGTH_SHORT).show();
			return;
		}
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("loginkey", Shared.loginkey);
		params.put("jobid", String.valueOf(Shared.details.id));
		ApiRequest.request("list", Shared.url+"bids/listbids", params, this);
	}
	
	@Override public void onInvisible() {
		super.onInvisible();
		if(request!=null) request.abort();
	}
	
	public void accept(View view){
		int pos = (Integer) view.getTag();
		try{
			JSONObject json = adapter.getItem(pos);
			if(json.getInt("bidder_id")==Shared.details.trans) return;
			accepted = view;
			selected = (View) view.getParent();
			String msg = 
				"Award the job for "+json.getString("bidder_name") +
				"?\n\nYour credit card account will be charged "+
				(new DecimalFormat("'$'0.00")).format(Math.abs(Shared.details.fee)) +
				". The fee will only be transferred to the transporter's account after the parcel is " +
				"delivered to the destination";
			dialogaward = new AlertDialog.Builder(activity).setPositiveButton("Award",this).setNegativeButton("Cancel",null).setMessage(msg).show();
		}catch(JSONException ex){}
	}
	
	public void chat(View view){
		if(!Shared.details.issender) return;
		int pos = (Integer) view.getTag();
		try{
			JSONObject json = adapter.getItem(pos);
			Intent intent = new Intent(activity, Chat.class);
		    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    intent.putExtra("id",Integer.valueOf(json.getString("bidder_id")));
		    intent.putExtra("name",json.getString("bidder_name"));
		    intent.putExtra("image", json.getString("bidder_image"));
		    intent.putExtra("chatonly", true);
		    startActivity(intent);
		}catch(JSONException ex){
			//System.out.println("JSON Exception : "+ex.getMessage());
		}
	}
	
	@Override public void onClick(DialogInterface dialog, int which) {
		//proceed to next step (swipe fragment tabhost)
		if(dialog==dialogdone){
			FragsSteps steps = (FragsSteps) activity;
			if(steps.pickup == null) activity.addandjump("pickup",steps.pickup = new Pickup(),"Pick up date");
			else activity.jump(activity.current()+1);
		}else if(dialog==dialogaward){
			if(!enabledaccept) return;
			accept = (ImageButton) accepted;
			accept.setEnabled(false);
			selected = (View) accepted.getParent();
			JSONObject json = (JSONObject) selected.getTag();
			try{
				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("loginkey", Shared.loginkey);
				params.put("jobid", String.valueOf(Shared.details.id)); //job id
				params.put("bidder", json.getString("bidder_id")); //bidder id
				request = SimpleHttp.request("award", Shared.url+"bids/acceptbid", params, this);
				enabledaccept = false;
			}catch(JSONException ex){}
		}
	}
	
	public void notified(int code, JSONObject data){
		adapter.insert(data,0);
		adapter.notifyDataSetChanged();
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		if(tag.equals("list")){
			if(result==null) Toast.makeText(activity, "Unexpected error. Please retry", Toast.LENGTH_SHORT).show();
			else try{
				JSONObject json = new JSONObject(result);
				String code = json.getString("code");
				if(json.getInt("success")==1){
					if(code.equals("no_bids_found")){
						Toast.makeText(activity,empty, Toast.LENGTH_SHORT).show();
						return;
					}
					JSONArray jsar = json.getJSONArray("post");
					adapter.clear();
					for(int i=0; i<jsar.length(); i++) adapter.insert(jsar.getJSONObject(i),0);
					adapter.notifyDataSetChanged();
				}else{
					Toast.makeText(activity, "Sorry. Server error", Toast.LENGTH_SHORT).show();
					activity.finish();
				}
			}catch(JSONException ex){
				Toast.makeText(activity, "Unexpected result", Toast.LENGTH_SHORT).show();
			}
		}else if(tag.equals("award")){
			enabledaccept = true;
			accept.setEnabled(true);
			//accept.setText("Accept");
			if(result==null) Toast.makeText(activity, "Unexpected error. Please retry", Toast.LENGTH_SHORT).show();
			else try{
				JSONObject json = new JSONObject(result);
				if(json.getInt("success")==1){
					selected.findViewById(R.id.jobbidaward).setBackground(activity.getResources().getDrawable(R.drawable.button_award_active));
					if(adapter.prev!=null){
						View prev = adapter.prev.findViewById(R.id.jobbidaward);
						prev.setVisibility(View.VISIBLE);
						if(prev instanceof TextView) ((TextView) prev).setText("Accept");
					}
					JSONObject obj = (JSONObject) selected.getTag();
					Shared.details.trans = obj.getInt("bidder_id");
					Shared.details.transname = obj.getString("bidder_name");
					String msg = "Your job has been awarded to "+Shared.details.transname+".\n\nLet's start propose a pick up date with the transporter";
					dialogdone = new AlertDialog.Builder(activity).setPositiveButton("OK",this).setNegativeButton(null,null).setMessage(msg).show();
					adapter.notifyDataSetChanged();
				}else{
					Toast.makeText(activity, "Unexpected error. Please retry", Toast.LENGTH_SHORT).show();
				}
			}catch(JSONException ex){
				Toast.makeText(activity, "Unexpected result", Toast.LENGTH_SHORT).show();
			}
		}
	}

	public static class BidsAdapter extends ArrayAdapter<JSONObject> implements android.view.View.OnClickListener{
		
		LayoutInflater inflater;
		View prev;
		Context context;
	    Transformation transform = new Transformation() {
			
			@Override public Bitmap transform(Bitmap bmp) {
				Bitmap cropped = JrgAndroid.circleCrop(bmp,100,100);
				bmp.recycle();
				return cropped;
			}
			
			@Override public String key() {
				return "bids";
			}
		};

		public BidsAdapter(Context context, int layout, List<JSONObject> obj) {
			super(context, layout, R.id.jobbidname, obj);
			this.context = context;
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		@Override public View getView(int pos, View convert, ViewGroup parent) {
			View view = convert;
			if(view==null) view = inflater.inflate(R.layout.bidsitem,null);
			JSONObject obj = getItem(pos);
			view.setTag(obj);
			try{
				int id = obj.getInt("bidder_id");
				boolean istrans = Shared.details.trans==id;
				ImageButton btnchat = (ImageButton) view.findViewById(R.id.jobbidchat);
				ImageButton btnaward = (ImageButton) view.findViewById(R.id.jobbidaward);
				//btnaward.setVisibility(Shared.details.trans==0?View.VISIBLE:View.GONE);
				btnaward.setTag(pos);
				btnchat.setTag(pos);
				String image = obj.isNull("bidder_image")?null:obj.getString("bidder_image");
				ImageView imageview = (ImageView) view.findViewById(R.id.jobbidimage);
				imageview.setTag(id);
				imageview.setOnClickListener(this);
				if(image!=null) Picasso.with(getContext()).load(image).fit().centerInside().transform(transform).into(imageview);
				if(istrans){
					prev = view;
					btnaward.setBackground(getContext().getResources().getDrawable(R.drawable.button_award_active));
				}
				LinearLayout wrap = (LinearLayout) view.findViewById(R.id.jobbidwrap);
				((TextView) view.findViewById(R.id.jobbidname)).setText(obj.getString("bidder_name"));
				RatingBar ratebar = ((RatingBar) view.findViewById(R.id.jobbidrate));
				ratebar.setVisibility(View.GONE);
				double rating = obj.getDouble("bidder_rating");
				if(rating>0){
					ratebar.setVisibility(View.VISIBLE);
					ratebar.setRating((float) rating);
				}
			}catch(JSONException e){}
			return view;
		}
		
		@Override public void onClick(View view) {
			if(!Shared.details.issender) return;
			int id = (Integer) view.getTag();
			Intent intent = new Intent(context, UserProfile.class);
		    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("id",id);
		    context.startActivity(intent);
			/*
			try{
				Intent intent = new Intent(getContext(), ChatBox.class);
			    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			    intent.putExtra("with",String.valueOf(((JSONObject)view.getTag()).getString("bidder_name")));
			    intent.putExtra("job",Shared.details.id);
			    getContext().startActivity(intent);
			}catch(JSONException ex){}
			*/
		}
	}
}