package my.helper;

import java.util.ArrayList;
import java.util.HashMap;
import my.helper.ApiRequest.ApiHandler;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

public class Requester implements ApiHandler{
	
	//Requester used as automation HTTP request sender which can be
	//set the property and behavior to handle the request and the response result without much self implementation by the user
	//- Take tag name, URL, API name, parameters values, key names, and file paths
	//- Android views can be assigned as the input providers to automatically supply the data as the request parameters
	//- Android views can be assigned as the responder to be enabled and disabled as the response to HTTP request and result
	//- Set the listener to response on Requester event
	//- Handle response result based on standard JSON format
	
	public static int VALID_YES = 1, VALID_NO_PROCEED =  2, VALID_NO_RETURN = 3;
	public JSONKeySet keys = new JSONKeySet();
	public MessageSet msgs = new MessageSet();
	String tag, url, api, uploadkey;
	HashMap<String, Object> params = new HashMap<String, Object>();
	ArrayList<String> uploads = new ArrayList<String>();
	ArrayList<Responder> responders = new ArrayList<Responder>();
	ArrayList<Provider> providers = new ArrayList<Provider>();
	ArrayList<ApiRequest> reqs = new ArrayList<ApiRequest>();
	RequestListener listener;
	
	public static Requester create(String tag, String url, String api){
		return new Requester(tag, url, api);
	}
	public Requester(String tag, String url, String api) {
		this.tag = tag;
		this.url = url;
		this.api = api;
	}
	public void setListener(RequestListener listener){
		this.listener = listener;
	}
	public Responder addResponder(String key, View view){
		Responder resp = new Responder();
		resp.tag = key;
		resp.view = view;
		responders.add(resp);
		return resp;
	}
	public Requester removeResponder(String tag){
		Responder selected = null;
		for(Responder resp : responders) if(resp.tag.equals(tag)) selected = resp;
		responders.remove(selected);
		return this;
	}
	public Requester clearResponders(){
		responders.clear();
		return this;
	}
	public Provider addProvider(String key, View view){
		Provider prov = new Provider();
		prov.key = key;
		prov.view = view;
		providers.add(prov);
		return prov;
	}
	public Requester removeProvider(String key){
		Provider selected = null;
		for(Provider prov : providers) if(prov.key.equals(key)) selected = prov;
		providers.remove(selected);
		return this;
	}
	public Requester clearProviders(){
		providers.clear();
		return this;
	}
	public Requester setParams(HashMap<String,Object> keyval){
		params.putAll(keyval);
		return this;
	}
	public Requester setParam(String key, Object val){
		if(val==null) params.remove(key);
		else params.put(key, val);
		return this;
	}
	public Requester removeParams(String... keys){
		for(String key : keys) params.remove(key);
		return this;
	}
	public Requester clearParams(){
		params.clear();
		return this;
	}
	public Requester addFiles(String... paths){
		for(String path : paths) if(uploads.indexOf(path)==-1) uploads.add(path);
		return this;
	}
	public Requester removeFiles(String... paths){
		for(String path : paths) uploads.remove(path);
		return this;
	}
	public Requester clearFiles(){
		uploads.clear();
		return this;
	}
	public boolean isBusy(){
		for(ApiRequest req : reqs) if(req.isRunning()) return true;
		return false;
	}
	public void allAbort(){
		for(ApiRequest req : reqs) req.abort();
		reqs.clear();
	}
	public void send(String tag, String url, String api, HashMap<String,Object> params, String uploadkey, ArrayList<String> uploads, boolean abort){
		if(abort) allAbort();
		params = new HashMap<String, Object>(params);
	    for(Provider prov : providers){
	    	if(prov==null) continue;
	    	if(!isValid(prov.view)){
	    		int val = validate(prov);
	    		if(val==VALID_NO_PROCEED) continue;
	    		else if(val==VALID_NO_RETURN) return;
	    	}
	    	params.put(prov.key, getValue(prov));
	    }
		for(Responder resp : responders){
			if(resp==null) continue;
			onRequest(resp);
			resp.view.setEnabled(false);
			if(resp.view instanceof TextView){
				resp.label = (String) ((TextView) resp.view).getText();
				((TextView) resp.view).setText(msgs.busy_message);
			}
		}
		reqs.add(ApiRequest.request(tag, url+(url.endsWith("/")||api.startsWith("/")?"":"/")+api, params, uploads, null, this, uploadkey));
	}
	public void send(boolean abort){
		send(tag,url,api,params,uploadkey,uploads,abort);
	}
	@Override public void handle(String tag, String result, String url, final HashMap<String, Object> params, final ArrayList<String> files) {
		Result res = new Result();
		res.tag = tag;
		res.url = url;
		res.tag = tag;
		res.params = params;
		res.files = files;
		res.busy = isBusy();
		onResult(tag, res);
		for(Responder resp : responders){
			if(resp==null||!onResponse(resp, res)) continue;
			//onResponse returns true means the responder view will not be enabled
			resp.view.setEnabled(true);
			if(resp.label!=null) ((TextView) resp.view).setText(resp.label);
		}
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
	public boolean isValid(View view){
		if(view instanceof TextView){
			int type = ((TextView) view).getInputType();
			CharSequence value = ((TextView) view).getText();
			if(type==InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) return Patterns.EMAIL_ADDRESS.matcher(value).matches();
		}
		return true;
	}
	protected static class JSONKeySet{
		String json_key_data = json_key_data_def, json_key_success = json_key_success_def;
		String json_key_code = json_key_code_def, json_key_message = json_key_message_def;
		String json_val_success = json_val_success_def, json_val_fail = json_val_fail_def;
		static String json_key_data_def = "post", json_key_success_def = "success";
		static String json_key_code_def = "code", json_key_message_def = "message";
		static String json_val_success_def = "1", json_val_fail_def = "0";
	}
	protected static class MessageSet{
		String busy_message = busy_message_def;
		String invalid_email_address = invalid_email_address_def;
		static String busy_message_def = "Please wait...";
		static String invalid_email_address_def = "Invalid email address";
	}
	protected static class Result{
		public boolean busy;
		String tag, result, url, code, message, jsonerror;
		HashMap<String, Object> params;
		ArrayList<String> files;
		JSONObject data;
	}
	protected static class Responder{
		View view;
		String label, tag;
	}
	protected static class Provider{
		View view;
		String key;
	}
	public static class Mapper<K,V>{
				
		HashMap<K,V> map;
		
		public static <K,V> Mapper<K, V> init(){
			return new Mapper<K, V>();
		}
		public Mapper() {
			if(map==null) map = new HashMap<K, V>();
		}
		public Mapper<K, V> put(K key, V val){
			map.put(key, val);
			return this;
		}
		public <K,V> HashMap<K,V> done(){
			return (HashMap<K, V>) map;
		}
	}
	public void onResult(String tag, Result result){
		if(listener!=null) listener.onResult(tag, result);
	}
	public void onSuccess(Result result) throws JSONException{
		//http request return json with successful status
		if(listener!=null) listener.onSuccess(tag, result);
	}
	public void onResultFail(Result result) throws JSONException{
		//http request return json with fail status
		if(listener!=null) listener.onResultFail(tag, result);
	}
	public void onResultNull(Result result){
		//http request return null result
		if(listener!=null) listener.onResultNull(tag, result);
	}
	public void onParseError(Result result){
		//fail to parse http json result
		if(listener!=null) listener.onParseError(tag, result);
	}
	public void onRequest(Responder resp){
		//on after http request sent
		if(listener!=null) listener.onRequest(tag, resp);
	}
	public boolean onResponse(Responder resp, Result result){
		//on after http response received
		return listener!=null&&listener.onResponse(tag, resp);
	}
	public int validate(Provider prov){
		//validate parameter value. return. 1 : valid, 2 : invalid. continue, 3 : invalid. cancel http request
		return listener!=null?listener.validate(tag, prov):3;
	}
	public Object getValue(Provider prov){
		//get the value from input view on activity to append on http request
		return listener!=null?listener.getValue(tag, prov):null;
	}
	interface RequestListener{
		Object getValue(String tag, Provider prov);
		int validate(String tag, Provider prov);
		boolean onResponse(String tag, Responder resp);
		void onRequest(String tag, Responder resp);
		void onResult(String tag, Result result);
		void onParseError(String tag, Result result);
		void onResultNull(String tag, Result result);
		void onResultFail(String tag, Result result);
		void onSuccess(String tag, Result result);
	}
}