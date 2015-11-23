package my.yousendit.fragments;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.MyFragment;
import my.helper.SimpleHttp;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import my.yousendit.FragsSteps;
import my.yousendit.R;
import my.yousendit.peripherals.ApiButton;
import my.yousendit.peripherals.MySpinner;
import my.yousendit.peripherals.MyTextBox;
import my.yousendit.peripherals.Shared;
import my.yousendit.peripherals.MySpinner.ItemSelectedListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Progress extends MyFragment implements ApiHandler, ItemSelectedListener{
	
	ListView list;
	LinearLayout panel, layoutpin, layoutremark;
	MySpinner spinner;
	ArrayList<String> typelist;
	ArrayList<JSONObject> proglist = new ArrayList<JSONObject>();
	ArrayAdapter<String> adapter;
	ProgressAdapter progress;
	JSONArray updatetype;
	MyTextBox pin, remark;
	ApiButton btn;
	SimpleHttp request;
	boolean busy, loaded;
	
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if(Shared.details.timepickup==null){
			view = inflater.inflate(R.layout.message,null);
			((TextView) view.findViewById(R.id.screenmessage)).setText("Job progress will only be displayed after the pick up date is set");
			return view;
		}
		view = inflater.inflate(R.layout.progress,null);
		try{
			progress = new ProgressAdapter(activity, R.layout.progressitem, proglist);
			list = (ListView) view.findViewById(R.id.jobprlist);
			list.setAdapter(progress);
			list.setDivider(null);
			list.setDividerHeight(0);
		}catch(JSONException ex){}
		if(Shared.details.istrans&&Shared.details.timedropoff==null){
			btn = (ApiButton) view.findViewById(R.id.jobprbtn);
			btn.setVisibility(View.VISIBLE);
			view.findViewById(R.id.jobprgap).setVisibility(View.VISIBLE);
			panel = (LinearLayout) view.findViewById(R.id.jobprcp);
			panel.setVisibility(View.VISIBLE);
			layoutpin = (LinearLayout) panel.findViewById(R.id.jobprpin);
			layoutremark = (LinearLayout) panel.findViewById(R.id.jobprremark);
			pin = (MyTextBox) panel.findViewById(R.id.jobprpinnumber);
			remark = (MyTextBox) panel.findViewById(R.id.jobprremarktext);
			spinner = (MySpinner) panel.findViewById(R.id.jobprstatusdropdown);
			typelist = new ArrayList<String>();
			adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item,typelist);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);
			spinner.setOnItemSelectedListener(this);
			try{
				updatetype = new JSONArray(Shared.listitems.getString("progress"));
				for(int i=1;i<updatetype.length();i++) adapter.addAll(updatetype.getString(i));
			}catch(JSONException ex){}
		}
		return view;
	}
	
    @Override public void onVisible() {
    	super.onVisible();
    	HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("loginkey", Shared.loginkey);
		params.put("jobid", String.valueOf(Shared.details.id));
		ApiRequest.request("list", Shared.url+"jobs/getprogress", params, this);
		busy = true;
    }
    
    @Override public void onInvisible() {
		super.onInvisible();
		if(request!=null) request.abort();
    }
    
    public void update(View view){
    	if(busy) return;
    	int selected = spinner.getSelectedItemPosition();
    	if(selected==0&&pin.getText().length()!=5){
    		Toast.makeText(activity, "Invalid pin number", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("loginkey", Shared.loginkey);
		params.put("jobid", String.valueOf(Shared.details.id));
		if(selected==0) params.put("pin", pin.getText());
		else if(selected==1) params.put("type", String.valueOf(4));
		else if(selected==2) params.put("type", String.valueOf(6));
		if(selected!=0&&remark.getText().length()!=0) params.put("remarks",remark.getText());
		request = SimpleHttp.request("update", Shared.url+(selected==0?"jobs/setdelivered":"jobs/otherprogress"), params, this);
		btn.setBusy(true);
		busy = true;
    }
    
    @Override public void itemSelected(int position) {
    	if(layoutpin==null||layoutremark==null) return;
		layoutpin.setVisibility(position==0?View.VISIBLE:View.GONE);
		layoutremark.setVisibility(position==0?View.GONE:View.VISIBLE);
    }
    
	public void notified(int code, JSONObject data){
		//Only sent to the sender
		if(Shared.details.istrans||progress==null) return;
		progress.insert(data,0);
		progress.notifyDataSetChanged();
		try{
			if(data.getInt("type_of_progress")!=5) return;
			Shared.details.timedropoff = Calendar.getInstance().getTime();
			((FragsSteps) activity).review = (Review) ((FragsSteps) activity).addandjump("review", new Review(),"Job review");
			((FragsSteps) activity).jump(activity.current()+1);
		}catch(JSONException ex){}
	}
    
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		busy = false;
		if(result==null) Toast.makeText(activity, "Unexpected error. Please retry", Toast.LENGTH_SHORT).show();
		else if(tag.equals("list")) try{
			JSONObject obj = new JSONObject(result);
			if(obj.getInt("success")==1){
				JSONArray jsar = obj.getJSONArray("post");
				progress.clear();
				for(int i=0;i<jsar.length();i++) progress.insert(jsar.getJSONObject(i),0);
				progress.notifyDataSetChanged();
			}
		}catch(JSONException ex){}
		else if(tag.equals("update")) try{
			btn.setBusy(false);
			JSONObject obj = new JSONObject(result);
			if(obj.getInt("success")==1){
				int type = spinner.getSelectedItemPosition();
				if(type==0) type = 5;
				else if(type==1) type = 4;
				else if(type==2) type = 6;
				Shared.sqltime.setTimeZone(TimeZone.getTimeZone("UTC"));
				JSONObject json = new JSONObject()
					.put("progress_datetime", Shared.sqltime.format(Calendar.getInstance().getTime()))
					.put("type_of_progress", type);
				if(remark.getText().length()!=0) json.put("remarks", remark.getText().toString());
				progress.insert(json,0);
				Shared.sqltime.setTimeZone(TimeZone.getDefault());
				progress.notifyDataSetChanged();
				if(spinner.getSelectedItemPosition()==0){
					Shared.details.timedropoff = Calendar.getInstance().getTime();
					panel.setVisibility(View.GONE);
					btn.setVisibility(View.GONE);
					((FragsSteps) activity).review = (Review) ((FragsSteps) activity).addandjump("review", new Review(),"Job review");
					((FragsSteps) activity).jump(activity.current()+1);
				}
			}else{
				Toast.makeText(activity, obj.getString("message"), Toast.LENGTH_SHORT).show();
			}
		}catch(JSONException ex){}
	}
	
	public static class ProgressAdapter extends ArrayAdapter<JSONObject>{
		
		List<JSONObject> progs;
		Context context;
		LayoutInflater inflater;
		JSONArray types;

		public ProgressAdapter(Context context, int layout, ArrayList<JSONObject> progs) throws JSONException {
			super(context, layout, android.R.layout.simple_list_item_1, progs);
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.context = context;
			this.progs = progs;
			types = Shared.listitems.getJSONArray("log");
		}
		
		@Override public View getView(int pos, View convert, ViewGroup parent) {
			View view = convert;
			if(view==null) view = inflater.inflate(R.layout.progressitem,null);
			JSONObject obj = getItem(pos);
			try {
				Shared.sqltime.setTimeZone(TimeZone.getTimeZone("UTC"));
				Date datetime = Shared.sqltime.parse(obj.getString("progress_datetime"));
				String date = Shared.monthdate.format(datetime);
				String time = Shared.timeonly.format(datetime);
				Shared.sqltime.setTimeZone(TimeZone.getDefault());
				boolean hasmsg = obj.isNull("remarks")||obj.getString("remarks").length()==0;
				String text = types.getString(obj.getInt("type_of_progress")-1)+(!hasmsg?" : "+obj.getString("remarks"):"");
				((TextView) view.findViewById(R.id.jobprdate)).setText(date);
				((TextView) view.findViewById(R.id.jobprtime)).setText(time);
				((TextView) view.findViewById(R.id.jobprtype)).setText(text);
			}catch(JSONException ex) {
			}catch(ParseException ex){}
			return view;
		}
	}
}