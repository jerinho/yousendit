package my.yousendit;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import my.helper.ApiRequest.ApiHandler;
import my.helper.SimpleHttp;
import my.yousendit.peripherals.MyTextBox;
import my.yousendit.peripherals.Shared;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class Account extends Activity implements ApiHandler, OnShowListener, OnClickListener{
	
	AlertDialog dialog;
	View prompt;
	MyTextBox accamount, accname, accnumber, accbank;
	TableLayout list;
	TextView tvbalance;
	View main;
	SimpleHttp request;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		main = getLayoutInflater().inflate(R.layout.account,null);
		list = (TableLayout) main.findViewById(R.id.accounttable);
		tvbalance = (TextView) main.findViewById(R.id.accountbalance);
		setContentView(main);
		prompt = getLayoutInflater().inflate(R.layout.dialogaccount, null);
		accamount = (MyTextBox) prompt.findViewById(R.id.accountamount);
		accnumber = (MyTextBox) prompt.findViewById(R.id.accountnumber);
		accbank = (MyTextBox) prompt.findViewById(R.id.accountbank);
		accname = (MyTextBox) prompt.findViewById(R.id.accountholder);
		dialog = new AlertDialog.Builder(this)
			.setPositiveButton("Submit",null).setNegativeButton("Cancel", null)
			.setMessage("Fill up the details below. Our customer service will contact you shortly")
			.setTitle("Request payment")
			.create();
		dialog.setView(prompt);
		dialog.setOnShowListener(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(android.R.color.transparent);
		HashMap<String,Object> pars = new HashMap<String, Object>();
		pars.put("loginkey", Shared.loginkey);
		request = SimpleHttp.request("list",Shared.url+"profile/getAccountHistory",pars,this);
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) finish();
		return super.onOptionsItemSelected(item);
	}

    @Override public void onShow(DialogInterface dialog) {
    	accbank.setText("");
    	accamount.setText("");
    	accname.setText("");
    	accnumber.setText("");
		this.dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
    }
    
	@Override protected void onPause() {
		super.onPause();
    	if(request!=null) request.abort();
	}
	
	@Override protected void onResume() {
		super.onResume();
		Shared.screen = this;
	}
	
    @Override public void onClick(View v) {
    	if(accbank.getText().length()==0) return;
    	if(accamount.getText().length()==0) return;
    	if(accname.getText().length()==0) return;
    	if(accnumber.getText().length()==0) return;
		HashMap<String,Object> pars = new HashMap<String, Object>();
		pars.put("loginkey", Shared.loginkey);
		pars.put("bank", accbank.getText());
		pars.put("amount", accamount.getText());
		pars.put("name", accname.getText());
		pars.put("number", accnumber.getText());
		request = SimpleHttp.request("request",Shared.url+"profile/requestPayment",pars,this);
		Button buttonok = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		buttonok.setText("Please wait...");
		buttonok.setEnabled(false);
		dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
    }
    
    public void request(View view){
    	dialog.show();
    }

	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		if(dialog.isShowing()){
			Button buttonok = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
			buttonok.setText("Submit");
			buttonok.setEnabled(true);
			dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
		}
		if(result==null){
			Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show();
			return;
		}
		if(tag.equals("list")) try{
			JSONObject json = new JSONObject(result);
			if(json.getInt("success")==1){
				list.removeAllViews();
				if(json.isNull("post")){
					Toast.makeText(this, "No transaction history record found", Toast.LENGTH_SHORT).show();
					return;
				}else{
					JSONObject data = json.getJSONObject("post");
					double balance = data.getDouble("accountBalance");
					tvbalance.setText(String.valueOf(balance));
					JSONArray array = data.getJSONArray("accountHistory");
					Shared.shortdate.setTimeZone(TimeZone.getTimeZone("UTC"));
					for(int i=0; i<array.length(); i++){
						TableRow row = (TableRow) getLayoutInflater().inflate(R.layout.accountitem, null);
						row.setBackgroundColor(Color.LTGRAY);
						TextView tvdate = (TextView) row.findViewById(R.id.accountrowdate);
						TextView tvjob = (TextView) row.findViewById(R.id.accountrowjob);
						TextView tvcredit = (TextView) row.findViewById(R.id.accountrowcredit);
						TextView tvdebit = (TextView) row.findViewById(R.id.accountrowdebit);
						int brgt = 255-(i%2==0?20:0);
						int color = Color.rgb(brgt,brgt,brgt);
						tvdate.setBackgroundColor(color);
						tvjob.setBackgroundColor(color);
						((View) tvjob.getParent()).setBackgroundColor(color);
						tvcredit.setBackgroundColor(color);
						tvdebit.setBackgroundColor(color);
						JSONObject obj = array.getJSONObject(i);
						Date date = Shared.sqltime.parse(obj.getString("created_at"));
						tvdate.setText(Shared.shortdate.format(date));
						Double amount = 0.0;
						if(amount==0) amount = obj.getDouble("credit_amount");
						if(amount==0) amount = obj.getDouble("debit_amount")*-1;
						tvjob.setText(obj.isNull("addrfrom")?"Payment Request":obj.getString("addrfrom"));
						TextView ctordt = amount>0?tvcredit:tvdebit;
						DecimalFormat cur = new DecimalFormat("'$'0.00");
						ctordt.setText(cur.format(Math.abs(amount)));
						list.addView(row);
					}
				}
			}
		}catch(JSONException ex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}catch(ParseException ex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
		Shared.shortdate.setTimeZone(TimeZone.getDefault());
		if(tag.equals("request")) try{
			JSONObject json = new JSONObject(result);
			if(json.getInt("success")==1){
				Toast.makeText(this, "Congratulation. Your payment request was succesful", Toast.LENGTH_SHORT).show();
				dialog.dismiss();
			}else{
				String msg = json.getString("message");
				Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
			}
		}catch(JSONException ex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
	}
}