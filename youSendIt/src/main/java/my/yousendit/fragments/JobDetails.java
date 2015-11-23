package my.yousendit.fragments;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.ApiRequest.BytesHandler;
import my.helper.ImageFlow;
import my.helper.JrgAndroid;
import my.helper.Map;
import my.helper.MyFragment;
import my.helper.SimpleHttp;
import my.yousendit.Chat;
import my.yousendit.FragsSteps;
import my.yousendit.JobForm;
import my.yousendit.MapScreen;
import my.yousendit.R;
import my.yousendit.UserProfile;
import my.yousendit.peripherals.ApiButton;
import my.yousendit.peripherals.Shared;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class JobDetails extends MyFragment implements ApiHandler, Transformation{
	
	TextView tvsender, tvfrom, tvto, tvfee, tvsize, tvurgency, tvvalid, tvtime;
	ApiButton bid;
	SimpleHttp request;
	ImageButton edit, chat;
	TableLayout table;
	ImageFlow flow;
	LinearLayout owner;
	ImageView ivsender;
	View main;
	boolean loaded;
	
	@Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if(!Shared.details.issender||Shared.details.trans!=0) return;
		MenuItem item = menu.add("EDIT");
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		item.setIcon(getResources().getDrawable(R.drawable.selectoredit));
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getTitle().equals("EDIT")) edit(null);
		return super.onOptionsItemSelected(item);
	}
	
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		main = inflater.inflate(R.layout.jobdetails, null);
		tvsender = (TextView) main.findViewById(R.id.jobdisender);
		tvfrom = (TextView) main.findViewById(R.id.jobdifrom);
		tvto = (TextView) main.findViewById(R.id.jobdito);
		tvfee = (TextView) main.findViewById(R.id.jobdifee);
		tvsize = (TextView) main.findViewById(R.id.jobdisize);
		tvurgency = (TextView) main.findViewById(R.id.jobdiurgency);
		tvvalid = (TextView) main.findViewById(R.id.jobdivalid);
		tvtime = (TextView) main.findViewById(R.id.jobditime);
		flow = (ImageFlow) main.findViewById(R.id.jobdiflow);
		chat = (ImageButton) main.findViewById(R.id.jobdichat);
		bid = (ApiButton) main.findViewById(R.id.jobdibid);
		owner = (LinearLayout) main.findViewById(R.id.jobdiowner);
		ivsender = (ImageView) main.findViewById(R.id.jobdiimg);
		//table = (TableLayout) view.findViewById(R.id.jobditable);
		//edit = (ImageButton) view.findViewById(R.id.jobdiedit);
		loaded = false;
		setHasOptionsMenu(true);
		return main;
	}
	
	@Override public void onVisible() {
		main.setVisibility(View.VISIBLE);
		if(getActivity().getIntent().getBooleanExtra("reload",false)) loaded = false;
		if(loaded) return;
		try {
			JSONArray sizes = Shared.listitems.getJSONArray("size");
			JSONArray urgs = Shared.listitems.getJSONArray("urgency");
			flow.editModeOn(false);
			//edit.setVisibility(Shared.details.issender?View.VISIBLE:View.GONE);
			//chat.setVisibility(Shared.details.issender?View.GONE:View.VISIBLE);
			owner.setVisibility(Shared.details.issender?View.GONE:View.VISIBLE);
			boolean disablebid = Shared.details.timepickup!=null||Shared.details.issender||Shared.details.istrans||Shared.details.isbidder;
			bid.setVisibility(disablebid?View.GONE:View.VISIBLE);
			tvsender.setText(Shared.details.issender?"This is your job":Shared.details.sendername);
			tvtime.setText(Shared.datetime.format(Shared.details.timepost));
			tvfee.setText(new DecimalFormat("'$'0.00").format(Shared.details.fee));
			tvvalid.setText(Shared.userdate.format(Shared.details.valid));
			tvfrom.setText(Shared.details.addrfrom);
			tvto.setText(Shared.details.addrto);
			tvsize.setText(sizes.getString(Shared.details.size-1));
			tvurgency.setText(urgs.getString(Shared.details.urgency-1));
			Picasso.with(getActivity()).load(Shared.details.senderimage).fit().centerInside().transform(this).into(ivsender);
			flow.clear();
			if(Shared.details.images==null) return;
			for(int i=0; i<Shared.details.images.length; i++) if(Shared.details.images[i]!=null) flow.putImageFromUrl(Shared.details.images[i]);
		} catch (JSONException ex) {
			//System.out.println("Error from JobDetails onResume : "+ex.getMessage());
		}
	}
	
	@Override public void onInvisible() {
		super.onInvisible();
		if(request!=null) request.abort();
	}
	
	public void chat(View view){
		if(Shared.details.issender) return;
		Intent intent = new Intent(activity, Chat.class);
	    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    intent.putExtra("id",Shared.details.sender);
	    intent.putExtra("name",Shared.details.sendername);
	    intent.putExtra("image",Shared.details.senderimage);
	    intent.putExtra("chatonly", true);
	    startActivity(intent);
	}
	
	public void bid(View view){
    	HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("loginkey", Shared.loginkey);
		params.put("jobid", String.valueOf(Shared.details.id));
		request = SimpleHttp.request("bid", Shared.url+"bids/placebid", params, this);
		bid.setBusy(true);
	}
	
	public void edit(View view){
    	Intent intent = new Intent(getActivity(),JobForm.class);
    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    	intent.putExtra("id",Shared.details.id);
    	intent.putExtra("locfrom", Shared.details.locfrom);
    	intent.putExtra("locto", Shared.details.locto);
    	intent.putExtra("addrfrom", Shared.details.addrfrom);
    	intent.putExtra("addrto", Shared.details.addrto);
    	intent.putExtra("size", Shared.details.size);
    	intent.putExtra("urgency", Shared.details.urgency);
    	intent.putExtra("fee", Shared.details.fee);
    	intent.putExtra("valid", Shared.details.valid);
    	if(Shared.details.images!=null&&Shared.details.images.length!=0) intent.putExtra("images",Shared.details.images);
    	/*
    	if(flow.countImages()>0){
	    	intent.putExtra("count", flow.countImages());
	    	for(int i=0; i<flow.countImages(); i++){
	    		Bitmap bmp = flow.getBitmap(i);
	    		ByteBuffer buffer = ByteBuffer.allocate(bmp.getByteCount());
	    		bmp.copyPixelsToBuffer(buffer);
	        	intent.putExtra("bitmap"+i,buffer.array());
	    	}
    	}
		*/
    	startActivity(intent);
	}
	
	public void locate(View view){
		double ix = Shared.details.locfrom[0], iy = Shared.details.locfrom[1], fx = Shared.details.locto[0], fy = Shared.details.locto[1];
		double ox = 0.05*Math.abs(ix-fx), oy = 0.05*Math.abs(iy-fy);
		double xmin = Math.min(ix, fx), ymin = Math.min(iy, fy), xmax = Math.max(ix, fx), ymax = Math.max(iy, fy);
    	String marks = "["
        		+ "{"+Map.LATITUDE+":"+ix+","+Map.LONGITUDE+":"+iy+","+Map.ICON+":"+R.drawable.icon_pickup+","+Map.INFO+":'Pick up the item here'},"
        		+ "{"+Map.LATITUDE+":"+fx+","+Map.LONGITUDE+":"+fy+","+Map.ICON+":"+R.drawable.icon_dropoff+","+Map.INFO+":'Drop off the item here'}]";
    	String span = "["
    		+ "{"+Map.LATITUDE+":"+(xmin-0.5*ox)+","+Map.LONGITUDE+":"+(ymin-oy)+"},"
    		+ "{"+Map.LATITUDE+":"+(xmax+1.5*ox)+","+Map.LONGITUDE+":"+(ymax+oy)+"}]";
    	//System.out.println("------------------- Job locations ---------------------");
    	//System.out.println("Offset : ("+oy+","+ox+")");
    	//System.out.println("Marks : "+marks);
    	//System.out.println("Span : "+span);
    	//System.out.println("-------------------------------------------------------");
    	Intent intent = new Intent(activity, MapScreen.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    		.putExtra(Map.BGCLEAR,R.drawable.selectorclear)
    		.putExtra(Map.BGCENTER, R.drawable.icon_centrelocation)
    		.putExtra(Map.POSITIONCENTER, Gravity.CENTER)
    		.putExtra(Map.CENTER, true)
    		.putExtra(Map.TITLE, activity.getTitle())
    		.putExtra(Map.MARKS, marks)
    		.putExtra(Map.SPAN, span);
    	startActivity(intent);
	}
	
	public void notified(int code, JSONObject data){
		//Notification only received by the transporter
		if(code==3){
			((FragsSteps) activity).pickup = (Pickup) ((FragsSteps) activity).addandjump("pickup", new Pickup(),"Pickup date");
			((FragsSteps) activity).pickup.notified(code, data);
		}
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		if(tag.equals("bid")){
			bid.setBusy(false);
			if(result==null) Toast.makeText(getActivity(), "Unexpected error. Please try again", Toast.LENGTH_SHORT).show();
			else try{
				JSONObject obj = new JSONObject(result);
				if(obj.getInt("success")==1){
					bid.setVisibility(View.GONE);
					Shared.details.isbidder = true;
					String msg = "Your offer was successfully sent.\n\nWe'll notify you if you're chosen by the sender to become the transporter";
					new AlertDialog.Builder(getActivity()).setMessage(msg).setPositiveButton("OK", null).show();
				}else{
					Toast.makeText(getActivity(), "Fail to place your bid", Toast.LENGTH_SHORT).show();
				}
			}catch(JSONException ex){
				Toast.makeText(getActivity(), "Unexpected result", Toast.LENGTH_SHORT).show();
			}
		}
	}

	public void profile(View view) {
		Intent intent = new Intent(activity, UserProfile.class);
	    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    intent.putExtra("id",Shared.details.sender);
	    startActivity(intent);
	}

	@Override public String key() {
		return "jobdetails";
	}

	@Override public Bitmap transform(Bitmap bmp) {
		Bitmap cropped = JrgAndroid.circleCrop(bmp,100,100);
		bmp.recycle();
		return cropped;
	}
}