package my.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

import my.helper.ApiRequest.ApiHandler;
import my.helper.Requester.Responder;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;

public class Smartivity extends Activity implements ApiHandler, OnGlobalLayoutListener{
	
	HashMap<String,View> inputs = new HashMap<String, View>();
	HashMap<String,Request> requests = new HashMap<String,Request>();
	Request active;
	private View root;
	private ViewTreeObserver observer;
	private boolean busy, complete;
	private LocalBroadcastManager lbm;
	public JSONKeySet keys = new JSONKeySet();
	public static int CALL_ACTIVITY = 1, CALL_SERVICE = 2, CALL_BROADCAST = 3, CALL_LOCAL_BROADCAST = 4;
	public static int REFRESH_ALWAYS = 1, REFRESH_NEVER = 2;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		busy = false;
		root = getWindow().getDecorView().getRootView();
		observer = root.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(this);
		create();
	}
	
	public void addInput(String key, View view){
		inputs.put(key, view);
	}
	
	public void addRequest(String tag, String url){
		Request request = new Request();
		request.activity = this;
		request.tag = tag;
		request.url = url;
		requests.put(tag, request);
	}
	
	public void attachInput(String tag, String key, View view){
		if(!active.tag.equals(tag)) active = requests.get(tag);
		active.addInput(key, view);
	}
	
	public void attachInput(String tag, String key, String input){
		attachInput(tag, key, inputs.get(input));
	}
	
	public void removeInput(String tag, String key){
		if(!active.tag.equals(tag)) active = requests.get(tag);
		active.removeInput(key);
	}
	
	public void addParam(String tag, String key, String value){
		if(!active.tag.equals(tag)) active = requests.get(tag);
		active.addParam(key, value);
	}
	
	public void request(String tag){
		if(!active.tag.equals(tag)) active = requests.get(tag);
		active.send();
	}
	
	public String getValue(View view, String tag, String key) {
		if(view instanceof TextView) return ((TextView) view).getText().toString();
		return null;
	}
	
	public boolean isBusy(){
		for(Entry<String, Request> entry : requests.entrySet()) if(entry.getValue().request.isRunning()) return true;
		return false;
	}
	
	public void abortAll(){
		for(Entry<String, Request> entry : requests.entrySet()) entry.getValue().request.abort();
	}
	
	public boolean isValid(View view){
		if(view instanceof TextView){
			int type = ((TextView) view).getInputType();
			CharSequence value = ((TextView) view).getText();
			if(type==InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) return Patterns.EMAIL_ADDRESS.matcher(value).matches();
		}
		return true;
	}
	
	@Override public void onGlobalLayout() {
        int heightDiff = root.getRootView().getHeight() - root.getHeight();
        int contentViewTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
        lbm = LocalBroadcastManager.getInstance(this);
        if(heightDiff <= contentViewTop){
            onKeyboardHidden();
            Intent intent = new Intent("KeyboardWillHide");
            lbm.sendBroadcast(intent);
        } else {
            int keyboardHeight = heightDiff - contentViewTop;
            onKeyboardShown();
            Intent intent = new Intent("KeyboardWillShow");
            intent.putExtra("KeyboardHeight", keyboardHeight);
            lbm.sendBroadcast(intent);
        }
	}

	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files){
		Result res = new Result();
		res.tag = tag;
		res.url = url;
		res.tag = tag;
		res.params = params;
		res.files = files;
		res.busy = isBusy();
		onResult(res);
		if(result==null) onResultNull(res);
		else try{
			JSONObject obj = new JSONObject(result);
			if(obj.has(keys.json_key_code)) res.code = obj.getString(keys.json_key_code);
			if(obj.has(keys.json_key_message)) res.message = obj.getString(keys.json_key_message);
			if(obj.has(keys.json_key_data)) res.data = obj.getJSONObject(keys.json_key_data);
			if(obj.getString(keys.json_key_success).equals(keys.json_val_success)) onSuccess(res);
			else onResultFail(res);
		}catch(JSONException jsex){
			res.jsonerror = jsex.getMessage();
			onParseError(res);
		}
	}
		
	public static class Request{
		
		Smartivity activity;
		String url, tag;
		boolean active;
		HashMap<String, View> keys = new HashMap<String, View>();
		HashMap<String, Object> params = new HashMap<String, Object>();
		ArrayList<String> files = new ArrayList<String>();
		ApiRequest request;
		public static int VALID_YES = 1, VALID_NO_PROCEED =  2, VALID_NO_RETURN = 3;
		
		public void clearParams(){
			params.clear();
		}
		public void addParam(String key, Object value){
			params.put(key, value);
		}
		
		public void addInput(String key, View view){
			keys.put(key, view);
		}
		public void removeInput(String key) {
			keys.remove(key);
		}
		public void addFile(String path){
			files.add(path);
		}
		public void removeFile(String path){
			files.remove(path);
		}
		
		public void send(){
			HashMap<String, Object> params = new HashMap<String, Object>(this.params);
			for(String key : keys.keySet()){
				View view = keys.get(key);
				int val = activity.validate(view, tag, key);
	    		if(val==VALID_NO_PROCEED) continue;
	    		else if(val==VALID_NO_RETURN) return;
				String value = activity.getValue(view, tag, key);
				params.put(key, value);
			}
			request = ApiRequest.request(tag, url, params, files, activity);
			active = true;
		}
	}
	
	protected static class Result{
		public boolean busy;
		String tag, result, url, code, message, jsonerror;
		HashMap<String, Object> params;
		ArrayList<String> files;
		JSONObject data;
	}
	protected static class JSONKeySet{
		static String json_key_data_def = "post", json_key_success_def = "success";
		static String json_key_code_def = "code", json_key_message_def = "message";
		static String json_val_success_def = "1", json_val_fail_def = "0";
		String json_key_data = json_key_data_def, json_key_success = json_key_success_def;
		String json_key_code = json_key_code_def, json_key_message = json_key_message_def;
		String json_val_success = json_val_success_def, json_val_fail = json_val_fail_def;
	}
	
	public void onResult(Result result){}
	public void onSuccess(Result result) throws JSONException{}
	public void onResultFail(Result result) throws JSONException{}
	public void onResultNull(Result result){}
	public void onParseError(Result result){}
	public int validate(View view, String tag, String key){
		//validate parameter value. return. 1 : valid, 2 : invalid. continue, 3 : invalid. cancel http request
		return Request.VALID_YES;
	}
	public boolean contextItemSelected(String menu, String item){return true;};
	public void onKeyboardHidden(){}
	public void onKeyboardShown(){}
	public void onCallResult(String tag, Intent data) {}
	public void create() {}
	public void pause() {}
	public void refresh() {}
}