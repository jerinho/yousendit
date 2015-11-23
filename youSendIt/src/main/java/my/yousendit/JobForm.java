package my.yousendit;

import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import my.helper.ApiRequest;
import my.helper.ApiRequest.BytesHandler;
import my.helper.ImageFlow;
import my.helper.Map;
import my.helper.ApiRequest.ApiHandler;
import my.yousendit.peripherals.ApiButton;
import my.yousendit.peripherals.MySpinner;
import my.yousendit.peripherals.MyTextBox;
import my.yousendit.peripherals.Shared;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class JobForm extends Activity implements ApiHandler, OnDateSetListener, OnClickListener, android.view.View.OnClickListener, OnEditorActionListener, OnGlobalLayoutListener, BytesHandler{
	
	MyTextBox etfrom, etto, etfee, etvalid;
	MySpinner spurgency, spsize;
	ArrayAdapter<String> spaurgency, spasize;
	ArrayList<String> splurgency, splsize;
	LinearLayout details;
	JSONArray liurgency, lisize;
	ApiButton submit;
	boolean busy, isfrom, loaded;
	int oldloc, oldsize, oldtype, oldurgency, noimgs, jobid;
	String oldjson, oldaddrfrom, oldaddrto;
	double[] oldfrom, oldto, from, to;
	Date oldvalid, valid;
	double oldfee;
	DatePickerDialog datepicker;
	DatePicker picker;
	ImageView selected;
	View root;
	ImageFlow imageflow;
	ViewTreeObserver observer;
	Bundle bundle;
	ArrayList<ApiRequest> reqs = new ArrayList<ApiRequest>();
	ArrayList<String> uploadnew = new ArrayList<String>(), uploadkeep = new ArrayList<String>();
	String[] images;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(Shared.id==0) System.exit(0);
		setContentView(R.layout.jobform);
		getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(android.R.color.transparent);
		datepicker = new DatePickerDialog(this,this,0,0,0){@Override protected void onStop() {}};
		imageflow = (ImageFlow) findViewById(R.id.jobfmimageflow);
		etvalid = (MyTextBox) findViewById(R.id.jobfmvalid);
		etfrom = (MyTextBox) findViewById(R.id.jobfmfrom);
		etto = (MyTextBox) findViewById(R.id.jobfmto);
		etfee = (MyTextBox) findViewById(R.id.jobfmfee);
		spurgency = (MySpinner) findViewById(R.id.jobfmurgency);
		spsize = (MySpinner) findViewById(R.id.jobfmsize);
		submit = (ApiButton) findViewById(R.id.jobscsubmit);
		root = getWindow().getDecorView().getRootView();
		observer = root.getViewTreeObserver();
		splurgency = new ArrayList<String>();
		spaurgency = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,splurgency);
        spaurgency.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spurgency.setAdapter(spaurgency);
		splsize = new ArrayList<String>();
		spasize = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,splsize);
		spasize.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spsize.setAdapter(spasize);
		etfrom.getTextBox().setOnClickListener(this);
		etto.getTextBox().setOnClickListener(this);
		etvalid.getTextBox().setOnClickListener(this);
		etfee.getTextBox().setOnEditorActionListener(this);
		observer.addOnGlobalLayoutListener(this);
		try {
			JSONObject json = Shared.listitems;
			liurgency = new JSONArray(json.getString("urgency"));
			for(int i=0;i<liurgency.length();i++) spaurgency.addAll(liurgency.getString(i));
			lisize = new JSONArray(json.getString("size"));
			for(int i=0;i<lisize.length();i++) spasize.addAll(lisize.getString(i));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	@Override protected void onResume() {
		super.onResume();
		Shared.screen = this;
		if(getIntent().getBooleanExtra("reload",false)) loaded = false;
		if(loaded) return;
		jobid = getIntent().getIntExtra("id",0);
		if(jobid==0){
			submit.setFreeText("POST THIS JOB");
			submit.setBusyText("POSTING...");
		}else{
			submit.setFreeText("SAVE CHANGES");
			submit.setBusyText("SAVING...");
			bundle = getIntent().getExtras();
			if(bundle == null){
		    	HashMap<String,Object> params = new HashMap<String, Object>();
				params.put("loginkey", Shared.loginkey);
				params.put("jobid", String.valueOf(jobid));
				reqs.add(ApiRequest.request("details", Shared.url+"jobs/viewjob", params, this));
			}else{
				if(bundle.containsKey("locfrom")) from = oldfrom = bundle.getDoubleArray("locfrom");
				if(bundle.containsKey("locto")) to = oldto = bundle.getDoubleArray("locto");
				if(bundle.containsKey("addrfrom")) etfrom.setText(oldaddrfrom = bundle.getString("addrfrom"));
				if(bundle.containsKey("addrto")) etto.setText(oldaddrto = bundle.getString("addrto"));
				if(bundle.containsKey("valid")) etvalid.setText(Shared.longdate.format(oldvalid = (Date) bundle.getSerializable("valid")));
				if(bundle.containsKey("fee")) etfee.setText(String.valueOf(oldfee = bundle.getDouble("fee")));
				if(bundle.containsKey("size")) spsize.pushSelection((oldsize = bundle.getInt("size"))-1);
				if(bundle.containsKey("urgency")) spurgency.pushSelection((oldurgency = bundle.getInt("urgency"))-1);
				if(bundle.containsKey("images")){
					/*
					try{
						int count = bundle.getInt("images");
						String[] srcs = bundle.getStringArray("sources");
						for(int i=0;i<count;i++) imageflow.putImage(bundle.getByteArray("bitmap"+i),srcs[i]);
					}catch(IOException ex){}
					*/
					reqs.clear();
					imageflow.clear();
					images = bundle.getStringArray("images");
					for(String img : images) imageflow.putImageFromUrl(img);
					//for(String img : images) reqs.add(ApiRequest.requestFile("images",img, this));
				}
			}
		}
	}
	@Override protected void onPause() {
		super.onPause();
		if(!isRequesting()) loaded = true;
		for(ApiRequest req : reqs) req.abort();
		reqs.clear();
	}
	
	public boolean isRequesting(){
		for(ApiRequest req : reqs) if(req.isRunning()) return true;
		return false;
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) finish();
		return super.onOptionsItemSelected(item);
	}

	public boolean checkSubmit(){
		if(!busy) submit.setEnabled(false);
		boolean validfrom = etfrom.getText().length()!=0;
		boolean validto = etto.getText().length()!=0;
		boolean validdate = valid!=null;
		boolean validfee = false;
		try{validfee = etfee.getText().length()!=0&&Double.parseDouble(etfee.getText().toString())!=0;}
		catch(NumberFormatException ex){}
		boolean validpost = jobid==0&&validfrom&&validto&&validdate&&validfee;
		boolean validedit = jobid!=0&&(etfee.getTextBox().length()==0||validfee)&&(validfrom||validto||validdate||validfee);
		boolean valid = validpost||validedit;
		if(!busy) submit.setEnabled(valid);
		return valid;
	}
	
	public void submit(View view){
		if(busy) return;
		if(!checkSubmit()){
			if(jobid==0) Toast.makeText(this,
				"Please fill up all of these required information"
				+ "\n- Pick up location address\n- Destionation address"
				+ "\n- Service charge\n- Job last validation date", Toast.LENGTH_SHORT).show();
			else Toast.makeText(this,"There's no information to update",Toast.LENGTH_SHORT).show();
		}
		String addrfrom = etfrom.getText().toString();
		String addrto = etto.getText().toString();
		double fee = 0;
		try{fee = Double.parseDouble(etfee.getText().toString());}
		catch(NumberFormatException ex){return;}
		int urgency = spurgency.getSelectedItemPosition()+1;
		int size = spsize.getSelectedItemPosition()+1;
    	HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("loginkey",Shared.loginkey);
		if(jobid!=0) params.put("jobid",String.valueOf(jobid));
		if(addrfrom.length()!=0&&!addrfrom.equals(oldaddrfrom==null?"":oldaddrfrom)) params.put("addrfrom",addrfrom);
		if(addrto.length()!=0&&!addrto.equals(oldaddrto==null?"":oldaddrto)) params.put("addrto",addrto);
		if(fee!=0&&fee!=oldfee) params.put("fee",fee);
		if(urgency!=oldurgency) params.put("urgency",String.valueOf(urgency));
		if(size!=oldsize) params.put("size",String.valueOf(size));
		if(from!=null&&(oldfrom==null||from[0]!=oldfrom[0]||from[1]!=oldfrom[1])) params.put("from",from[0]+" "+from[1]);
		if(to!=null&&(oldto==null||to[0]!=oldto[0]||to[1]!=oldto[1])) params.put("to",to[0]+" "+to[1]);
		if(valid!=null&&(oldvalid==null||oldvalid.compareTo(valid)!=0)) params.put("valid",Shared.sqldate.format(valid));
		String[] srcs = imageflow.getSources();
		/*
		//images contains only the existing image URLs
		//imageflow.getSources() contains all the image URL and filepath of all images inside the imageflow
		if(jobid==0&&srcs!=null) uploadnew  = new ArrayList<String>(Arrays.asList(srcs));
		else{
			uploadnew.clear();
			uploadkeep.clear();
			if(srcs!=null) for(String src : srcs){
				boolean fromfile = true;
				if(images!=null) for(String img : images) if(img.equals(src)) fromfile = false;
				(fromfile?uploadnew:uploadkeep).add(fromfile?src:URLEncoder.encode(src));
			}
			//upload_keep is null if no images to be removed (uploadkeep.size()==images.length)
			//upload_keep is empty array if all images need to be removed
			if(images!=null&&uploadkeep.size()<images.length)
				params.put("upload_keep",uploadkeep.size()==0?"removeall":(new JSONArray(uploadkeep)).toString());
		}
		ApiRequest req = ApiRequest.request("submit", Shared.url+(jobid!=0?"jobs/editjob/":"jobs/postjob"), params, uploadnew, this);
		*/
		ArrayList<ContentBody> bodies = new ArrayList<ContentBody>();
		int i = 0;
		for(Entry<String, byte[]> entry: imageflow.getContents().entrySet()) bodies.add(new ByteArrayBody(entry.getValue(), "file_"+(i++)+".jpeg"));
		if(jobid!=0){
			uploadkeep.clear();
			if(srcs!=null) for(String src : srcs){
				boolean fromfile = true;
				if(images!=null) for(String img : images) if(img.equals(src)) fromfile = false;
				if(!fromfile) uploadkeep.add(fromfile?src:URLEncoder.encode(src));
			}
			if(images!=null&&uploadkeep.size()<images.length)
				params.put("upload_keep",uploadkeep.size()==0?"removeall":(new JSONArray(uploadkeep)).toString());
		}
		ApiRequest req = ApiRequest.request("submit", Shared.url+(jobid!=0?"jobs/editjob/":"jobs/postjob"), params, null, bodies, this);
		reqs.add(req);
		submit.setBusy(true);
		busy = true;
	}
	
	public void pickValidDate(View view){
		Calendar cal = Calendar.getInstance();
		if(valid!=null) cal.setTime(valid);
		else if(oldvalid!=null) cal.setTime(oldvalid);
		else cal = Calendar.getInstance();
		DatePicker picker = datepicker.getDatePicker();
		if(Shared.isSamsung) picker.setMinDate(System.currentTimeMillis() + 100000000);
		picker.setCalendarViewShown(true);
		picker.setSpinnersShown(false);
		datepicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
		datepicker.show();
	}
	
	@Override public void onDateSet(DatePicker view, int y, int moy, int dom) {
		Date toset = null;
		try{toset = Shared.sqldate.parse(y+"-"+(moy+1)+"-"+dom);}
		catch(ParseException ex){}
		Calendar cal = Calendar.getInstance();
		if(toset.after(cal.getTime())){
			valid = toset;
			etvalid.setText(Shared.longdate.format(toset));
			checkSubmit();
		}else{
			Toast.makeText(this, "It is in the past", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void pickfrom(View view){
		isfrom = true;
		picklocation();
	}

	public void pickto(View view){
		isfrom = false;
		picklocation();
	}

	public void picklocation(){
		//Map featuring : location, geocode, reverse geocode, return result, center marking
    	Intent intent = new Intent(this, MapScreen.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    	    .putExtra(Map.BGCENTER, R.drawable.icon_centrelocation)
    	    .putExtra(Map.BGCLEAR,R.drawable.selectorclear)
	    	.putExtra(Map.POSITIONCENTER, Gravity.CENTER)
	    	.putExtra(Map.CENTER,true)
	    	.putExtra(Map.TITLE, getTitle())
	    	.putExtra(Map.LOCATION, true)
	    	.putExtra(Map.REVERSE, true)
	    	.putExtra(Map.RESULT, true);
    	if(isfrom&&from!=null) intent.putExtra(Map.LATITUDE, from[0]).putExtra(Map.LONGITUDE, from[1]);
    	if(!isfrom&&to!=null) intent.putExtra(Map.LATITUDE, to[0]).putExtra(Map.LONGITUDE, to[1]);
    	startActivityForResult(intent,Map.requestcode);
	}
	
	@Override protected void onActivityResult(int req, int res, Intent data) {
		super.onActivityResult(req, res, data);
		if(res!=Activity.RESULT_OK) return; //I'm not OK...
		if(req==ImageFlow.requestcode) imageflow.putImage(data);
		else if(req==Map.requestcode){
			//System.out.println("---------- Google Map result -----------");
			//System.out.println("> Data String : "+data.getDataString());
			Bundle result = data.getExtras();
			String address = result.getString(Map.ADDRESS);
	    	//if(address!=null) System.out.println("> Address : "+address);
			double lat = result.getDouble(Map.LATITUDE), lon = result.getDouble(Map.LONGITUDE);
			if(lat==0||lon==0||address==null) return;
			//System.out.println("> Location : "+lat+","+lon);
			//System.out.println("---------------------------------------------------------");
			(isfrom?etfrom:etto).setText(address);
			if(isfrom) from = new double[]{lat,lon};
			else to = new double[]{lat,lon};
		}
		checkSubmit();
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> pars, ArrayList<String> files) {
		busy = false;
		if(tag.equals("submit")){ //on response to job submitting or updating
			submit.setBusy(false);
			String doing = jobid!=0?"updating":"posting", done = jobid!=0?"updated":"posted";
			String msgfail = "Job "+doing+" fail";
			if(result==null){
				Toast.makeText(this, "Unexpected error. Please retry", Toast.LENGTH_SHORT).show();
			}else try{
				JSONObject obj = new JSONObject(result);
				if(obj.getInt("success")==1){
					new AlertDialog.Builder(this).setMessage("Your job is successfully "+done).setPositiveButton("OK",this).show();
					if(jobid==0) clearForm();
				}else{
					String code = obj.getString("code");
					if(code.startsWith("unauthenticated")) new AlertDialog.Builder(this).setMessage("Access denied").setPositiveButton("OK",null).show();
					else if(code.equals("db_update_failed")) new AlertDialog.Builder(this).setMessage("Server error. Please retry").setPositiveButton("OK",null).show();
					else new AlertDialog.Builder(this).setMessage(msgfail).setPositiveButton("OK",null).show();
				}
			}catch(JSONException jsex){
				new AlertDialog.Builder(this).setMessage("Unexpected result").setPositiveButton("OK",null).show();
			}
		}else if(tag.equals("details")){
			if(result==null) {
				Toast.makeText(this, "Unexpected error. Please retry", Toast.LENGTH_SHORT).show();
			}else try {
				JSONObject obj = new JSONObject(result);
				if(obj.getInt("success")==1){
					JSONObject data = obj.getJSONObject("post");
					if(!data.isNull("pick_up_location")){
						String[] loc = data.getString("pick_up_location").split("[\\s\\,]");
						from = oldfrom = new double[]{Double.parseDouble(loc[0]), Double.parseDouble(loc[1])};
					}
					if(!data.isNull("drop_off_location")){
						String[] loc = data.getString("drop_off_location").split("[\\s\\,]");
						to = oldto = new double[]{Double.parseDouble(loc[0]), Double.parseDouble(loc[1])};
					}
					if(!data.isNull("pick_up_address")){
						oldaddrfrom = data.getString("pick_up_address");
						etfrom.setText(oldaddrfrom);
					}
					if(!data.isNull("drop_off_address")){
						oldaddrto = data.getString("drop_off_address");
						etto.setText(oldaddrto);
					}
					if(!data.isNull("job_last_valid_date")) try{
						oldvalid = Shared.sqldate.parse(data.getString("job_last_valid_date"));
						etvalid.setText(Shared.longdate.format(oldvalid));
					}catch(ParseException ex){}
					if(!data.isNull("fee")){
						oldfee = data.getDouble("fee");
						etfee.setText(String.valueOf(oldfee));
					}
					if(!data.isNull("size")){
						oldsize = data.getInt("size");
						spsize.setSelection(oldsize-1);
					}
					if(!data.isNull("urgency")){
						oldurgency = data.getInt("urgency");
						spurgency.setSelection(oldurgency-1);
					}
					if(!data.isNull("filenames")){
						JSONArray array = data.getJSONArray("filenames");
						if(array.length()>0) for(int i=0;i<array.length();i++){
							ApiRequest req = ApiRequest.requestFile(array.getString(i), this);
							reqs.add(req);
						}
					}
				}else{
					String code = obj.getString("code"), msg = null;
					if(code.equals("unauthenticated_access")) msg = "Access denied";
					else if(code.equals("cannot_edit_job")) msg = "Fail updating the job";
					Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
					finish();
				}
			} catch (JSONException e) {
				Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	@Override public void handle(String tag, byte[] result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		imageflow.putImage(result, url);
	}

	@Override public void onClick(DialogInterface dialog, int which) {
		//on job posting successful
		if(dialog==datepicker){
		}else{
	    	Intent intent = new Intent(this,FragsJobs.class);
	    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	    	intent.putExtra("reload", true);
	    	startActivity(intent);
	    	finish();
		}
	}

	@Override public void onClick(View view) {
		if(view==etvalid.getTextBox()) pickValidDate(view);
		else if(view==etfrom.getTextBox()) pickfrom(view);
		else if(view==etto.getTextBox()) pickto(view);
	}
	
	@Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
	    if(actionId == EditorInfo.IME_ACTION_DONE) checkSubmit();
	    return false;
	}
	
	@Override public void onGlobalLayout() {
        int heightDiff = root.getRootView().getHeight() - root.getHeight();
        int contentViewTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        if(heightDiff <= contentViewTop){
            onHideKeyboard();
            Intent intent = new Intent("KeyboardWillHide");
            broadcastManager.sendBroadcast(intent);
        } else {
            int keyboardHeight = heightDiff - contentViewTop;
            onShowKeyboard(keyboardHeight);
            Intent intent = new Intent("KeyboardWillShow");
            intent.putExtra("KeyboardHeight", keyboardHeight);
            broadcastManager.sendBroadcast(intent);
        }
	}
	
	public void onHideKeyboard(){
		checkSubmit();
	}
	
	public void clearForm(){
		from = null;
		to = null;
		valid = null;
		etfrom.setText("");
		etto.setText("");
		etvalid.setText("");
		etfee.setText("");
		imageflow.clear();
		checkSubmit();
	}
	
	public void onShowKeyboard(int height){}
}