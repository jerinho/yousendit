package my.yousendit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.SimpleHttp;
import my.yousendit.peripherals.MyException;
import my.yousendit.peripherals.MySpinner;
import my.yousendit.peripherals.MyTextBox;
import my.yousendit.peripherals.Shared;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Payment extends Activity implements ApiHandler, OnShowListener, OnClickListener{
	
	PaymentAdapter adapter;
	AlertDialog dialog;
	View prompt;
	MyTextBox ccnumber, ccsecurity;
	//NumberPicker ccmonth, ccyear;
	SimpleHttp request;
	MySpinner ccmonth, ccyear;
	ListView list;
	Menu menu;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(Shared.id==0) System.exit(0);
        list = new ListView(this);
		setContentView(list);
		adapter = new PaymentAdapter(this, R.id.paymentmask);
		list.setAdapter(adapter);
		dialog = new AlertDialog.Builder(this).setPositiveButton("Add",null).setNegativeButton("Cancel", null).setTitle("Add credit card").create();
		prompt = getLayoutInflater().inflate(R.layout.dialogpayment, null);
		ccnumber = (MyTextBox) prompt.findViewById(R.id.paymentccnumber);
		ccsecurity = (MyTextBox) prompt.findViewById(R.id.paymentccsecurity);
		Calendar cal = Calendar.getInstance();
		ccyear = (MySpinner) prompt.findViewById(R.id.paymentccyear);
		ccmonth = (MySpinner) prompt.findViewById(R.id.paymentccmonth);
		//ccyear.setMinValue(cal.get(Calendar.YEAR));
		//ccyear.setMaxValue(cal.get(Calendar.YEAR)+50);
		//ccyear.setValue(ccyear.getMinValue());
		//ccmonth.setDisplayedValues(Shared.months);
		//ccmonth.setMinValue(1);
		//ccmonth.setMaxValue(Shared.months.length);
		ccmonth.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Shared.months));
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		ccyear.setAdapter(adapter);
		int year = cal.get(Calendar.YEAR);
		for(int i=0; i<20; i++) adapter.addAll(String.valueOf(year+i));
		dialog.setView(prompt);
		dialog.setOnShowListener(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(android.R.color.transparent);
		if(!Shared.haspayment) dialog.show();
		else{
			HashMap<String,Object> pars = new HashMap<String, Object>();
			pars.put("loginkey", Shared.loginkey);
			request = SimpleHttp.request("list",Shared.url+"payment/getCreditCard",pars,this);
		}
	}
	
	@Override protected void onResume() {
		super.onResume();
		Shared.screen = this;
	}
	
	@Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add("Remove credit card");
	}
	
	@Override public boolean onContextItemSelected(MenuItem item) {
		HashMap<String,Object> pars = new HashMap<String, Object>();
		pars.put("loginkey", Shared.loginkey);
		pars.put("index",null);
		request = SimpleHttp.request("remove",Shared.url+"payment/deleteCreditCard", pars,this);
		return false;
	}
	
	@Override public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		this.menu = menu;
		if(Shared.haspayment) return true;
		MenuItem item = menu.add("Add credit card");
		TextView tv = new TextView(this);
		tv.setText("+");
		tv.setTextColor(Color.WHITE);
		tv.setTextSize(30);
		tv.setPadding(tv.getPaddingLeft(), tv.getPaddingTop(), 30, tv.getPaddingBottom());
		tv.setOnClickListener(this);
		tv.setTag("CC");
		item.setActionView(tv);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}
	
    @Override public boolean onOptionsItemSelected(MenuItem item) {
    	super.onOptionsItemSelected(item);
    	if(item.getItemId() == android.R.id.home) finish();
    	return super.onOptionsItemSelected(item);
    }
    @Override public void onShow(DialogInterface dialog) {
    	ccnumber.setText("");
    	ccsecurity.setText("");
    	ccmonth.setSelection(0);
    	ccyear.setSelection(0);
		this.dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
		this.dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(this);
    }
    
	@Override protected void onPause() {
		super.onPause();
    	if(request!=null) request.abort();
	}
    
    @Override public void onClick(View v) {
    	if(((TextView) v).getText().toString().equals("Add")){
        	if(ccnumber.getText().length()==0) return;
        	if(ccsecurity.getText().length()==0) return;
			HashMap<String,Object> pars = new HashMap<String, Object>();
			pars.put("loginkey", Shared.loginkey);
			pars.put("number", ccnumber.getText());
			pars.put("cvv", ccsecurity.getText());
			pars.put("expireYear", ccyear.getHead().getText().toString());
			pars.put("expireMonth", ccmonth.getSelectedItemPosition()+1);
			request = SimpleHttp.request("add",Shared.url+"payment/addCreditCard",pars,this);
			Button buttonok = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
			buttonok.setText("Please wait...");
			buttonok.setEnabled(false);
			dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
    	}
    	else if(v.getTag()!=null&&v.getTag().equals("CC")) dialog.show();
    	else dialog.dismiss();
    }
	
	public static class PaymentAdapter extends ArrayAdapter<JSONObject> implements OnLongClickListener{
		
		Activity activity;
		LayoutInflater inflater;

		public PaymentAdapter(Activity activity, int rid) {
			super(activity, rid);
			this.activity = activity;
			inflater = activity.getLayoutInflater();
		}
		
		@Override public View getView(int position, View reuse, ViewGroup parent) {
			View view = reuse==null?inflater.inflate(R.layout.paymentitem, null):reuse;
			TextView text = (TextView) view.findViewById(R.id.paymentmask);
			JSONObject data = getItem(position);
			try{
				String mask = data.getString("maskedNumber");
				String card = data.getString("creditCardType");
				view.setOnLongClickListener(this);
				view.setTag(position);
				text.setText(mask);
				ImageButton image = (ImageButton) view.findViewById(R.id.paymenticon);
				String cctype = Shared.cctypes.get(card.trim().replace("-","").replace(" ","").toLowerCase());
				String path = "ccicon/"+(cctype!=null?cctype:"cciconunknown")+".png";
				image.setBackground(Drawable.createFromStream(activity.getResources().getAssets().open(path), null));
			}catch(IOException ex){
			}catch(JSONException ex){
			}
			return view;
		}

		@Override public boolean onLongClick(View v) {
			activity.registerForContextMenu(v);
			activity.openContextMenu(v);
			return false;
		}
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		if(dialog.isShowing()){
			Button buttonok = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
			buttonok.setText("Add");
			buttonok.setEnabled(true);
			dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
		}
		if(result==null){
			Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show();
			return;
		}
		if(tag.equals("list")) try{
			if(!Shared.haspayment) dialog.show();
			JSONObject json = new JSONObject(result);
			if(json.getInt("success")==1){
				if(json.isNull("post")){
					Toast.makeText(this, "Please insert your first payment account", Toast.LENGTH_SHORT).show();
					dialog.show();
					menu.add("+").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}else{
					JSONObject data = json.getJSONObject("post");
					adapter.clear();
					adapter.add(data);
					menu.removeItem(0);
				}
			}
		}catch(JSONException ex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
		if(tag.equals("add")) try{
			JSONObject json = new JSONObject(result);
			if(json.getInt("success")==1){
				Toast.makeText(this, "Thank you. Your credit card information was succesfully submitted", Toast.LENGTH_SHORT).show();
				Shared.editor.putBoolean("payment", Shared.haspayment = true).apply();
				JSONObject data = json.getJSONObject("post");
				adapter.add(data);
				adapter.notifyDataSetChanged();
				dialog.dismiss();
				menu.removeItem(0);
			}else{
				String msg = json.getString("message");
				Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
			}
		}catch(JSONException ex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
		if(tag.equals("remove")) try{
			JSONObject json = new JSONObject(result);
			if(json.getInt("success")==1){
				Shared.editor.putBoolean("payment", Shared.haspayment = false).apply();
				adapter.remove(adapter.getItem(0));
				adapter.notifyDataSetChanged();
				menu.add("+").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}else{
				String msg = json.getString("message");
				Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
			}
		}catch(JSONException ex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
	}
}