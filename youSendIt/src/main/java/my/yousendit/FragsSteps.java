package my.yousendit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import my.helper.Notifier;
import my.helper.ApiRequest.ApiHandler;
import my.helper.Fragmentivity;
import my.helper.Notifier.GcmBroadcastReceiver;
import my.helper.Notifier.MessageHandler;
import my.helper.SimpleHttp;
import my.yousendit.fragments.*;
import my.yousendit.peripherals.*;

public class FragsSteps extends Fragmentivity implements ApiHandler, MessageHandler{
	
	Bundle bundle;
	public SimpleHttp request;
	public JobDetails details;
	public Bids bids;
	public Pickup pickup;
	public Progress progress;
	public Receiver receiver;
	public Review review;
	int code;
	
	protected void onCreate(android.os.Bundle args) {
		super.onCreate(args);
		if(Shared.id==0) System.exit(0);
		setSwipable(true);
		setActionBar(true);
		setTabBar(false);
		setStateBar(true);
		boolean refresh = getIntent().getBooleanExtra("refresh",false);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setIcon(android.R.color.transparent);
    	getPagerStrip().setBackgroundColor(getResources().getColor(R.color.theme));
    	getPagerStrip().setTextColor(Color.WHITE);
		bundle = getIntent().getExtras();
		if(bundle==null||!bundle.containsKey("id")) finish();
		if(bundle.containsKey("code")) code = bundle.getInt("code");
        if(Shared.details!=null&&!refresh) populateTabs();
        else{
    		HashMap<String, Object> params = new HashMap<String, Object>();
    		params.put("loginkey", Shared.loginkey);
    		params.put("jobid", String.valueOf(bundle.getInt("id")));
    		request = SimpleHttp.request("details", Shared.url+"jobs/viewjob", params, this);
        }
	}
	@Override protected void onResume() {
		super.onResume();
		Notifier.setHandler(this, this, Shared.projectkey);
		Shared.screen = this;
	}
    @Override protected void onPause() {
    	super.onPause();
    	Notifier.removeHandler();
    	if(request!=null) request.abort();
    }
	
	public void populateTabs(){
		if(Shared.details==null) return;
		clear();
		add("details", details = new JobDetails(),"Job Details");
		if(Shared.details.issender&&Shared.details.timepickup==null)
			bids = (Bids) add("bid", new Bids(),"Award the job");
		if(Shared.details.timepickup==null&&(Shared.details.issender&&Shared.details.trans!=0||Shared.details.istrans&&Shared.details.proposed!=null))
			pickup = (Pickup) add("pickup", new Pickup(),"Pickup date");
		if((Shared.details.issender||Shared.details.istrans)&&Shared.details.timepickup!=null)
	        progress = (Progress) add("progress", new Progress(),"Job progress");
		if(Shared.details.issender&&Shared.details.timepickup!=null&&Shared.details.timedropoff==null)
	        receiver = (Receiver) add("receiver", new Receiver(),"Receiver PIN");
		if((Shared.details.issender||Shared.details.istrans)&&Shared.details.timedropoff!=null)
	        review = (Review) add("review", new Review(),"Job review");
		if(code==1) return; //Sender awarded job to other bidder
		else if(code==2) jump(1); //New bid on your job
		else if(code==3) jump(Shared.details.issender?2:1); //New update on pickup screen
		else if(code==4) jump(2); //New update on progress screen
		else if(code==5) jump(pickup!=null?1:0); //Job awarded to you
		else if(code==6) jump(3);
	}
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) finish();
		return super.onOptionsItemSelected(item);
	}
	@Override protected void onDestroy() {
		super.onDestroy();
		Shared.details = null;
	}
	
	@Override public boolean onNotifierMessage(Bundle bundle, GcmBroadcastReceiver receiver) {
		if(!bundle.containsKey("jobId")||bundle.getString("jobId")==null||bundle.getString("jobId").length()==0) return true;
		if(Shared.details==null||Shared.details.id!=Integer.valueOf(bundle.getString("jobId"))) return true;
		int type = Integer.valueOf(bundle.getString("code"));
		try{
			String post = bundle.getString("post");
			JSONObject data = post==null?new JSONObject():new JSONObject(post);
			data.put("notification", bundle.getString("message"));
			if(type==0);
			else if(type==1&&details!=null&&details.isVisible()) details.notified(type,data);
			else if(type==2&&bids!=null&&bids.isVisible()) bids.notified(type,data);
			else if(type==3){
				if(pickup==null&&details!=null&&details.isVisible()) details.notified(type, data);
				else if(pickup!=null&&pickup.isVisible()) pickup.notified(type,data);
			}
			else if(type==4&&progress!=null&&progress.isVisible()) progress.notified(type,data);
			else if(type==5&&details!=null&&details.isVisible()) details.notified(type,data);
			else if(type==6&&review!=null&&review.isVisible()) review.notified(type,data);
		}catch(JSONException ex){}
		return true;
	}
	
	@Override public void onNotifierError(String message) {
		Toast.makeText(this, "Fail to get the required information from your device. The app may become unresponsive", Toast.LENGTH_LONG).show();
	}
	
	@Override public void onNotifierKey(String gcmid) {
		if(gcmid!=null) Shared.gcmid = gcmid;
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		Shared.sqltime.setTimeZone(TimeZone.getTimeZone("UTC"));
		if(result==null){
			Toast.makeText(this, "Connection problem. Please retry", Toast.LENGTH_SHORT).show();
			finish();
		}else try {
			String[] subs = result.split(Pattern.quote("</div>"));
			JSONObject obj = new JSONObject(subs[subs.length-1]);
			if(obj.getInt("success")==1){
				JSONObject data = obj.getJSONObject("post");
				Shared.details = new Details(data.getInt("job_id"));
				if(!data.isNull("isJobSender")) Shared.details.issender = data.getBoolean("isJobSender");
				if(!data.isNull("isJobTransporter")) Shared.details.istrans = data.getBoolean("isJobTransporter");
				if(!data.isNull("isJobBidder")) Shared.details.isbidder = data.getBoolean("isJobBidder");
				if(!data.isNull("isLastPickupProposer")) Shared.details.isproposer = data.getBoolean("isLastPickupProposer");
				if(!data.isNull("receiver_pin_code")) Shared.details.pin = data.getString("receiver_pin_code");
				if(!data.isNull("sender_name")) Shared.details.sendername = data.getString("sender_name");
				if(!data.isNull("sender_image")) Shared.details.senderimage = data.getString("sender_image");
				if(!data.isNull("transporter_name")) Shared.details.transname = data.getString("transporter_name");
				if(!data.isNull("pick_up_address")) Shared.details.addrfrom = data.getString("pick_up_address");
				if(!data.isNull("drop_off_address")) Shared.details.addrto = data.getString("drop_off_address");
				if(!data.isNull("transporter_user_id")) Shared.details.trans = data.getInt("transporter_user_id");
				if(!data.isNull("sender_user_id")) Shared.details.sender = data.getInt("sender_user_id");
				if(!data.isNull("job_last_valid_date")) Shared.details.valid = Shared.sqldate.parse(data.getString("job_last_valid_date"));
				if(!data.isNull("proposed_date")) Shared.details.proposed = Shared.sqltime.parse(data.getString("proposed_date"));
				if(!data.isNull("agreed_date")) Shared.details.agreed = Shared.sqltime.parse(data.getString("agreed_date"));
				if(!data.isNull("actual_pick_up_time")){
					if(data.isNull("verifytimepickup")||(data.getInt("verifytimepickup")!=2&&data.getInt("verifytimepickup")!=1)) Shared.details.indicated = true;
					else if(data.getInt("verifytimepickup")==1) Shared.details.timepickup = Shared.sqltime.parse(data.getString("actual_pick_up_time"));
				}
				Shared.details.rejected = !data.isNull("actual_pick_up_time");
				if(!data.isNull("actual_drop_off_time")) Shared.details.timedropoff = Shared.sqltime.parse(data.getString("actual_drop_off_time"));
				if(!data.isNull("pick_up_location")){
					String[] loc = data.getString("pick_up_location").split("[\\s\\,]");
					Shared.details.locfrom = new double[]{Double.parseDouble(loc[0]), Double.parseDouble(loc[1])};
				}
				if(!data.isNull("drop_off_location")){
					String[] loc = data.getString("drop_off_location").split("[\\s\\,]");
					Shared.details.locto = new double[]{Double.parseDouble(loc[0]), Double.parseDouble(loc[1])};
				}
				if(!data.isNull("fee")) Shared.details.fee = Double.parseDouble(data.getString("fee"));
				if(!data.isNull("size")) Shared.details.size  = data.getInt("size");
				if(!data.isNull("job_rate")) Shared.details.rate = data.getInt("job_rate");
				if(!data.isNull("urgency")) Shared.details.urgency = data.getInt("urgency");
				if(!data.isNull("review")) Shared.details.review = data.getString("review");
				if(!data.isNull("filenames")){
					JSONArray array = data.getJSONArray("filenames");
					if(array.length()>0){
						Shared.details.images = new String[array.length()];
						for(int i=0;i<Shared.details.images.length;i++) Shared.details.images[i] = array.getString(i);
					}
				}
				if(!data.isNull("job_post_datetime")) Shared.details.timepost = Shared.sqltime.parse(data.getString("job_post_datetime"));
				populateTabs();
			}else{
				String code = obj.getString("code");
				String msg = null;
				if(code.equals("unauthenticated_access")) msg = "Access denied";
				else if(code.equals("job_not_exist")) msg = "No such job exist";
				Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
				finish();
			}
		}catch(JSONException ex){
			//System.out.println(ex.getMessage());
			Toast.makeText(this,"Server returns unexpected result", Toast.LENGTH_SHORT).show();
		}catch (java.text.ParseException ex) {
			//System.out.println(ex.getMessage());
			Toast.makeText(this,"Parse error : "+ex.getMessage(), Toast.LENGTH_SHORT).show();
		}
		Shared.sqltime.setTimeZone(TimeZone.getDefault());
	}
	
	public void chat(View view){
		Fragment frag = getCurrent();
		if(frag instanceof Bids) bids.chat(view);
		else if(frag instanceof JobDetails) details.chat(view);
	}
	public void bid(View view){
		Fragment frag = getCurrent();
		if(frag instanceof JobDetails) details.bid(view);
	}
	public void edit(View view){
		Fragment frag = getCurrent();
		if(frag instanceof JobDetails) details.edit(view);
	}
	public void locate(View view){
		Fragment frag = getCurrent();
		if(frag instanceof JobDetails) details.locate(view);
	}
	public void accept(View view){
		Fragment frag = getCurrent();
		if(frag instanceof JobDetails) details.chat(view);
		else if(frag instanceof Bids) bids.accept(view);
	}
	public void propose(View view){
		Fragment frag = getCurrent();
		if(frag instanceof Pickup) pickup.propose(view);
	}
	public void calendar(View view){
		Fragment frag = getCurrent();
		if(frag instanceof Pickup) pickup.calendar(view);
	}
	public void indicate(View view){
		Fragment frag = getCurrent();
		if(frag instanceof Pickup) pickup.indicate(view);
	}
	public void confirm(View view){
		Fragment frag = getCurrent();
		if(frag instanceof Pickup) pickup.confirm(view);
	}
	public void agree(View view){
		Fragment frag = getCurrent();
		if(frag instanceof Pickup) pickup.agree(view);
	}
	public void time(View view){
		Fragment frag = getCurrent();
		if(frag instanceof Pickup) pickup.time(view);
	}
	public void update(View view){
		Fragment frag = getCurrent();
		if(frag instanceof Progress) progress.update(view);
	}
	public void submit(View view){
		Fragment frag = getCurrent();
		if(frag instanceof Review) review.submit(view);
	}
	public void share(View view){
		Fragment frag = getCurrent();
		if(frag instanceof Receiver) receiver.share(view);
	}
	public void profile(View view){
		Fragment frag = getCurrent();
		if(frag instanceof JobDetails) details.profile(view);
	}
}