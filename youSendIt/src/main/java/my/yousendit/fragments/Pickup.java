package my.yousendit.fragments;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.MyFragment;
import my.helper.SimpleHttp;
import my.yousendit.FragsSteps;
import my.yousendit.R;
import my.yousendit.peripherals.ApiButton;
import my.yousendit.peripherals.Details;
import my.yousendit.peripherals.Shared;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class Pickup extends MyFragment implements ApiHandler, OnDateSetListener, OnTimeSetListener, OnClickListener{
	
	String propose, myname, yourname;
	Date tosuggest, didsuggest, beensuggest, agreed, indicate, indicated, pickedup;
	TextView dateagreed, datedidsuggest, datebeensuggest, datetosuggest;
	TextView msgdidsuggest, msgdidindicated, msgwaitindicated, msgbeensuggest, msgtoindicate, msgbeenindicated;
	LinearLayout llbeensuggest, llbeenindicated, lltosuggest, lltoindicate, llagreed, llpickedup, lldidsuggest, lldidindicated, llwaitindicate;
	SimpleHttp request;
	AlertDialog datetimepicker;
	DatePickerDialog datedialog;
	TimePickerDialog timedialog;
	DatePicker datepicker;
	TimePicker timepicker;
	boolean isagreed, loaded, busy;
	ApiButton disabled;
	String msgindicate1 = "Is the item picked up already";
	String msgindicate2 = "NNN did not acknowledge that you have picked up the item. Please discuss with NNN to reconfirm and tap button below";
	
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.pickup, null);
		datedialog = new DatePickerDialog(activity, this,0,0,0){@Override protected void onStop() {}};
		timedialog = new TimePickerDialog(activity, this,0,0,false){@Override protected void onStop() {}};
		timepicker = new TimePicker(activity);
		datepicker = new DatePicker(activity);
		LayoutParams pickerlaypar = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		tosuggest = Shared.details.proposed;
		LinearLayout datetimelayout = new LinearLayout(activity);
		datetimelayout.setOrientation(LinearLayout.VERTICAL);
		//datetimepicker.addContentView(datedialog.getDatePicker(), pickerlaypar);
		//datetimepicker.addContentView(timepicker, pickerlaypar);
		datetimelayout.addView(datepicker,pickerlaypar);
		datetimelayout.addView(timepicker,pickerlaypar);
		datetimepicker = new AlertDialog.Builder(activity).setPositiveButton("Set", this).setNegativeButton("Cancel", null).create();
		datetimepicker.setView(datetimelayout);
		llbeensuggest = (LinearLayout) view.findViewById(R.id.jobpullbeensuggest);
		llbeenindicated = (LinearLayout) view.findViewById(R.id.jobpullbeenindicated);
		lltosuggest = (LinearLayout) view.findViewById(R.id.jobpulltosuggest);
		lltoindicate = (LinearLayout) view.findViewById(R.id.jobpulltoindicate);
		llagreed = (LinearLayout) view.findViewById(R.id.jobpuagreed);
		lldidsuggest = (LinearLayout) view.findViewById(R.id.jobpudidsuggest);
		lldidindicated = (LinearLayout) view.findViewById(R.id.jobpudidindicated);
		llwaitindicate = (LinearLayout) view.findViewById(R.id.jobpuwaitindicated);
		llpickedup = (LinearLayout) view.findViewById(R.id.jobpupickedup);
		dateagreed = (TextView) view.findViewById(R.id.jobpudateagreed);
		datebeensuggest = (TextView) view.findViewById(R.id.jobpudatebeensuggest);
		datedidsuggest = (TextView) view.findViewById(R.id.jobpudatedidsuggest);
		datetosuggest = (TextView) view.findViewById(R.id.jobpudatetosuggest);
		msgdidsuggest = (TextView) view.findViewById(R.id.jobpunamedidsuggest);
		msgdidindicated = (TextView) view.findViewById(R.id.jobpunamedidindicated);
		msgwaitindicated = (TextView) view.findViewById(R.id.jobpunamewaitindicated);
		msgbeensuggest = (TextView) view.findViewById(R.id.jobpubeensuggest);
		msgtoindicate = (TextView) view.findViewById(R.id.jobputoindicate);
		msgbeenindicated = (TextView) view.findViewById(R.id.jobpubeenindicated);
		return view;
	}
	
	@Override public void onVisible() {
		myname = Shared.details.issender?Shared.details.sendername:Shared.details.transname;
		yourname = Shared.details.issender?Shared.details.transname:Shared.details.sendername;
		///////////////////////////////////
		if(Shared.details.timepickup!=null){
			llpickedup.setVisibility(View.VISIBLE);
			llbeenindicated.setVisibility(View.GONE);
			lldidindicated.setVisibility(View.GONE);
			return;
		}
		llpickedup.setVisibility(View.GONE);
		if(Shared.details.agreed!=null){
			dateagreed.setText(Shared.usertime.format(Shared.details.agreed));
			llagreed.setVisibility(View.VISIBLE);
			llbeensuggest.setVisibility(View.GONE);
			lldidsuggest.setVisibility(View.GONE);
			lltosuggest.setVisibility(View.GONE);
			if(Shared.details.indicated){
				llwaitindicate.setVisibility(View.GONE);
				lltoindicate.setVisibility(View.GONE);
				if(Shared.details.issender){
					msgbeenindicated.setText(msgbeenindicated.getText().toString().replace("NNN",yourname));
					llbeenindicated.setVisibility(View.VISIBLE);
					lldidindicated.setVisibility(View.GONE);
				}else{
					msgdidindicated.setText(msgdidindicated.getText().toString().replace("NNN",yourname));
					llbeenindicated.setVisibility(View.GONE);
					lldidindicated.setVisibility(View.VISIBLE);
				}
			}else{
				llbeenindicated.setVisibility(View.GONE);
				lldidindicated.setVisibility(View.GONE);
				if(Shared.details.issender){
					msgwaitindicated.setText(msgwaitindicated.getText().toString().replace("NNN",yourname));
					llwaitindicate.setVisibility(View.VISIBLE);
					lltoindicate.setVisibility(View.GONE);
				}else{
					msgtoindicate.setText((!Shared.details.rejected?msgindicate1:msgindicate2).replace("NNN",yourname));
					llwaitindicate.setVisibility(View.GONE);
					lltoindicate.setVisibility(View.VISIBLE);
				}
			}
		}else{
			llagreed.setVisibility(View.GONE);
			llbeenindicated.setVisibility(View.GONE);
			lldidindicated.setVisibility(View.GONE);
			lltoindicate.setVisibility(View.GONE);
			if(Shared.details.proposed!=null){
				String proposed = Shared.usertime.format(Shared.details.proposed);
				if(Shared.details.isproposer){
					msgdidsuggest.setText(msgdidsuggest.getText().toString().replace("NNN",yourname));
					datedidsuggest.setText(proposed);
					llbeensuggest.setVisibility(View.GONE);
					lltosuggest.setVisibility(View.GONE);
					lldidsuggest.setVisibility(View.VISIBLE);
				}else{
					msgbeensuggest.setText(msgbeensuggest.getText().toString().replace("NNN",yourname));
					datebeensuggest.setText(proposed);
					datetosuggest.setText(proposed);
					llbeensuggest.setVisibility(View.VISIBLE);
					lltosuggest.setVisibility(View.VISIBLE);
					lldidsuggest.setVisibility(View.GONE);
				}
			}else{
				lltosuggest.setVisibility(Shared.details.issender?View.VISIBLE:View.GONE);
			}
		}
	}
	
	@Override public void onInvisible() {
		super.onInvisible();
		if(request!=null) request.abort();
	}
	
	@Override public void onClick(DialogInterface dialog, int which) {
		if(dialog!=datetimepicker||which!=DialogInterface.BUTTON_POSITIVE) return;
		Date toset = null;
		try{
			int y = datepicker.getYear(), m = datepicker.getMonth()+1, d = datepicker.getDayOfMonth();
			int h = timepicker.getCurrentHour(), mm = timepicker.getCurrentMinute();
			toset = Shared.sqltime.parse(y+"-"+m+"-"+d+" "+h+":"+mm+":00");
		}catch(ParseException ex){}
		Calendar cal = Calendar.getInstance();
		if(toset.before(cal.getTime())) Toast.makeText(activity, "It is in the past", Toast.LENGTH_SHORT).show();
		else{
			tosuggest = toset;
			datetosuggest.setText(Shared.usertime.format(tosuggest));
		}
	}
		
	public void calendar(View view){
		Calendar cal = Calendar.getInstance();
		if(tosuggest!=null) cal.setTime(tosuggest);
		else cal = Calendar.getInstance();
		if(Shared.isSamsung) datepicker.setMinDate(System.currentTimeMillis() + 100000000);
		datepicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
		datepicker.setCalendarViewShown(true);
		datepicker.setSpinnersShown(false);
		timepicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
		timepicker.setCurrentMinute(cal.get(Calendar.MINUTE));
		datetimepicker.show();
		/*
		DatePicker picker = datedialog.getDatePicker();
		if(Shared.isSamsung) picker.setMinDate(System.currentTimeMillis() + 100000000);
		picker.setCalendarViewShown(true);
		picker.setSpinnersShown(false);
		datedialog.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
		datedialog.show();
		*/
	}
	
	public void time(View view){
		Calendar cal = Calendar.getInstance();
		timedialog.updateTime(cal.get(Calendar.HOUR_OF_DAY),Calendar.MINUTE);
		timedialog.show();
	}
	
	public void agree(View view){
		if(busy) return;
    	HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("loginkey", Shared.loginkey);
		params.put("jobid", String.valueOf(Shared.details.id));
		request = SimpleHttp.request("agree", Shared.url+"jobs/pickupagree", params, this);
		disabled = (ApiButton) view;
		disabled.setBusy(true);
		busy = true;
	}
	
	public void propose(View view){
		if(busy) return;
		if(tosuggest==null||tosuggest.equals(Shared.details.proposed)) return;
    	HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("loginkey", Shared.loginkey);
		params.put("jobid", String.valueOf(Shared.details.id));
		Shared.sqltime.setTimeZone(TimeZone.getTimeZone("UTC"));
		params.put("date", Shared.sqltime.format(tosuggest));
		Shared.sqltime.setTimeZone(TimeZone.getDefault());
		request = SimpleHttp.request("propose", Shared.url+"jobs/pickuppropose", params, this);
		disabled = (ApiButton) view;
		disabled.setBusy(true);
		busy = true;
	}
	
	public void indicate(View view){
		if(busy) return;
    	HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("loginkey", Shared.loginkey);
		params.put("jobid", String.valueOf(Shared.details.id));
		request = SimpleHttp.request("indicate", Shared.url+"jobs/pickupindicate", params, this);
		disabled = (ApiButton) view;
		disabled.setBusy(true);
		busy = true;
	}
	
	public void confirm(View view){
		if(busy) return;
		boolean b = ((ApiButton) view).getText().equalsIgnoreCase("YES");
    	HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("loginkey", Shared.loginkey);
		params.put("jobid", String.valueOf(Shared.details.id));
		params.put("status", b?"1":"2");
		request = SimpleHttp.request(b?"accept":"reject", Shared.url+"jobs/pickupconfirm", params, this);
		disabled = (ApiButton) view;
		disabled.setBusy(true);
		busy = true;
	}
	
	@Override public void onDateSet(DatePicker view, int y, int moy, int dom) {
		Date toset = null;
		try{toset = Shared.sqldate.parse(y+"-"+(moy+1)+"-"+dom);}
		catch(ParseException ex){}
		Calendar cal = Calendar.getInstance();
		if(toset.after(cal.getTime())){
			int hh = 0, mm = 0;
			if(tosuggest!=null){
				hh = tosuggest.getHours();
				mm = tosuggest.getMinutes();
			}
			tosuggest = toset;
			tosuggest.setHours(hh);
			tosuggest.setMinutes(mm);
			datetosuggest.setText(Shared.usertime.format(tosuggest));
		}
		else Toast.makeText(activity, "It is in the past", Toast.LENGTH_SHORT).show();
	}
	
	@Override public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		if(tosuggest==null) return;
		tosuggest.setHours(hourOfDay);
		tosuggest.setMinutes(minute);
		datetosuggest.setText(Shared.usertime.format(tosuggest));
	}
	
	public void notified(int code, JSONObject data){
		Shared.sqltime.setTimeZone(TimeZone.getTimeZone("UTC"));
		try{
			if(!data.isNull("proposed_date")) Shared.details.proposed = Shared.sqltime.parse(data.getString("proposed_date"));
			if(!data.isNull("isLastPickupProposer")) Shared.details.isproposer = data.getBoolean("isLastPickupProposer");
			if(!data.isNull("agreed_date")) Shared.details.agreed = Shared.sqltime.parse(data.getString("agreed_date"));
			Shared.details.rejected = !data.isNull("actual_pick_up_time");
			Shared.details.indicated = !data.isNull("actual_pick_up_time")&&(data.isNull("verifytimepickup")||(data.getInt("verifytimepickup")!=2&&data.getInt("verifytimepickup")!=1));
			if(!data.isNull("actual_pick_up_time")&&!data.isNull("verifytimepickup")&&data.getInt("verifytimepickup")==1) Shared.details.timepickup = Shared.sqltime.parse(data.getString("actual_pick_up_time"));
			if(!data.isNull("actual_drop_off_time")) Shared.details.timedropoff = Shared.sqltime.parse(data.getString("actual_drop_off_time"));
			if(!data.isNull("isJobTransporter")) Shared.details.istrans = data.getBoolean("isJobTransporter");
			onVisible();
		}catch(JSONException ex){
		}catch(ParseException ex){
		}
		Shared.sqltime.setTimeZone(TimeZone.getDefault());
		if(Shared.details.timepickup!=null){
	        ((FragsSteps) activity).progress = (Progress) ((FragsSteps) activity).addandjump("progress", new Progress(),"Job progress");
	        if(Shared.details.issender) ((FragsSteps) activity).receiver = (Receiver) activity.add("receiver", new Receiver(),"Receiver PIN");
		}
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		busy = false;
		disabled.setBusy(false);
		if(result==null){
			Toast.makeText(activity, "Unexpected error", Toast.LENGTH_SHORT).show();
			return;
		}
		if(tag.equals("agree")){
			Shared.details.agreed = Shared.details.proposed;
			onVisible();
			//llbeensuggest.setVisibility(View.GONE);
			//lltosuggest.setVisibility(View.GONE);
			//dateagreed.setText(Shared.usertime.format(Shared.details.agreed));
			//llagreed.setVisibility(View.VISIBLE);
			//TextView msg = Shared.details.issender?msgwaitindicated:msgtoindicate;
			//msg.setText(msg.getText().toString().replace("NNN",yourname));
			//(Shared.details.issender?llwaitindicate:lltoindicate).setVisibility(View.VISIBLE);
		}else if(tag.equals("propose")){
			Shared.details.proposed = tosuggest;
			Shared.details.isproposer = true;
			onVisible();
			//llbeensuggest.setVisibility(View.GONE);
			//lltosuggest.setVisibility(View.GONE);
			//msgdidsuggest.setText(msgdidsuggest.getText().toString().replace("NNN",yourname));
			//datedidsuggest.setText(Shared.usertime.format(tosuggest));
			//lldidsuggest.setVisibility(View.VISIBLE);
		}else if(tag.equals("indicate")){
			Shared.details.indicated = true;
			onVisible();
			//lltoindicate.setVisibility(View.GONE);
			//msgdidindicated.setText(msgdidindicated.getText().toString().replace("NNN",yourname));
			//lldidindicated.setVisibility(View.VISIBLE);
		}else if(tag.equals("accept")){
			Shared.details.timepickup = Shared.details.agreed; //Just to indicate that the pickup time is set
			onVisible();
			//llbeenindicated.setVisibility(View.GONE);
			//llpickedup.setVisibility(View.VISIBLE);
			//llagreed.setVisibility(View.GONE);
	        //activity.jump(0);
			//activity.remove(1);
			//activity.remove(1);
	        ((FragsSteps) activity).progress = (Progress) ((FragsSteps) activity).addandjump("progress", new Progress(),"Job progress");
	        if(Shared.details.issender) ((FragsSteps) activity).receiver = (Receiver) activity.add("receiver", new Receiver(),"Receiver PIN");
	        //activity.update();
		}else if(tag.equals("reject")){
			Shared.details.indicated = false;
			onVisible();
			//llbeenindicated.setVisibility(View.GONE);
			//msgwaitindicated.setText(msgwaitindicated.getText().toString().replace("NNN",yourname));
			//llwaitindicate.setVisibility(View.VISIBLE);
		}
	}
}
