package my.helper;

import android.os.AsyncTask;
import android.os.Build;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import my.helper.Requester.Mapper;

public abstract class ApiRequest extends AsyncTask<String,String,byte[]>{
	
	HashMap<String, Object> params, report;
	ArrayList<String> files;
	ArrayList<ContentBody> contents;
	String url, tag, upload = "upload[]";
	int timeout = 15000;
	boolean isfile, isrunning = true;
	HttpPost post;
	static boolean log = false, uploadlog = true;
	
	public ApiRequest(String url, HashMap<String, Object> params, ArrayList<String> files, ArrayList<ContentBody> contents, String tag, String upload) {
		this.tag = tag;
		this.url = url;
		this.params = params;
		this.files = files;
		this.contents = contents;
		this.report = new HashMap<String, Object>();
		if(upload!=null) this.upload = upload;
	}

	public ApiRequest(String url, HashMap<String, Object> params, String tag) {
		this.tag = tag;
		this.url = url;
		this.params = params;
		this.report = new HashMap<String, Object>();
	}
	
	public void abort(){
		cancel(true);
		if(post!=null) post.abort();
	}
	
	public boolean isRunning(){
		return isrunning;
	}
	
	@Override protected byte[] doInBackground(String... uri) {
		boolean logthis = log&&!tag.equals("uplog");
		boolean uploadthis = logthis&&!tag.equals("uplog");
		if(logthis){
			System.out.println("------------ Http Preexecution -----------");
			if(tag!=null) System.out.println("Tag : "+tag);
			if(url!=null) System.out.println("Url : "+url);
			if(params!=null) System.out.println("Parameters : "+params.toString());
			System.out.println("------------------------------------------");
		}
    	ArrayList<NameValuePair> list = params==null?null:new ArrayList<NameValuePair>(params.size());
    	MultipartEntity entity = null;
    	byte[] result = null;
    	if(files!=null&&files.size()!=0){
        	entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
    		for(int i=0; i<files.size(); i++) entity.addPart(upload,new FileBody(new File(files.get(i))));
    	}
    	if(contents!=null&&contents.size()!=0){
        	if(entity==null) entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
    		for(int i=0; i<contents.size(); i++) entity.addPart(upload,contents.get(i));
    	}
		ArrayList<String> pars = new ArrayList<String>();
		HashMap<String,Object> iterated = params==null?null:(HashMap<String, Object>) params.clone();
    	if(iterated!=null&&iterated.size()!=0) try{
		    Iterator iterator = iterated.entrySet().iterator();
		    while (iterator.hasNext()) {
		        Map.Entry pair = (Map.Entry)iterator.next();
		        String key, val;
		        try{
		        	key = (String) pair.getKey();
		        	val = (String) pair.getValue();
		        }catch(ClassCastException ex){
		        	key = String.valueOf(pair.getKey());
		        	val = String.valueOf(pair.getValue());
		        }
		        if(entity!=null) entity.addPart(key,new StringBody(val));
		        else if(list!=null) list.add(new BasicNameValuePair(key,val));
		        iterator.remove();
		        pars.add(key+":"+val);
		    }
    	}catch(UnsupportedEncodingException ex){
    		System.out.println("Unsupported Excoding Exception : "+ex.getMessage());
    		return null;
    	}
		report.clear();
    	post = new HttpPost(url);
		try{
	    	if(entity!=null) post.setEntity(entity);
	    	else if(list!=null) post.setEntity(new UrlEncodedFormEntity(list));
    	}catch(UnsupportedEncodingException ex){
    		System.out.println("Unsupported Excoding Exception : "+ex.getMessage());
    		return null;
    	}
    	if(isCancelled()||post.isAborted()) return null;
	    /*
	    if(logthis){
	    	HttpParams postparams = post.getParams();
	    	HttpEntity postentity = post.getEntity();
	    	Header[] postheaders = post.getAllHeaders();
	    	System.out.println("\n-------------------- Http Execution ------------------------");
	    	System.out.println("URI : "+post.getURI());
	    	System.out.println("Method : "+post.getMethod());
	    	System.out.println("Params : "+postparams);
	    	System.out.println("Headers : "+postheaders);
	    	System.out.println("Entity : "+postentity);
	    	System.out.println("\n-----------------------------------------------------------------");
	    }
	    */
	    HttpParams httpParams = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
        HttpClient client = new DefaultHttpClient(httpParams);
	    HttpResponse response = null;
	    try{
	    	response = client.execute(post); //cause IO Exception with message "Connection to [URL] refused"
	    }catch(ClientProtocolException ex){
    		System.out.println("Client Protocol Exception : "+ex.getMessage());
	    }catch(IOException ex) {
    		System.out.println("IO Error : "+ex.getMessage());
		}
	    if(response==null) return null;
        StatusLine status = response.getStatusLine();
    	if(logthis){
			System.out.println("\n-------------------- Http Response -------------------");
			if(tag!=null) System.out.println("Tag : "+tag);
			if(url!=null) System.out.println("URL : "+url);
			if(params!=null) System.out.println("Parameters :: "+pars.toString());
			if(files!=null) System.out.println("Files :: "+files.toString());
			ArrayList<String> listcontents = new ArrayList<String>();
			if(contents!=null){
				for(int i=0; i<contents.size(); i++) listcontents.add(contents.get(i).getFilename()+" : "+contents.get(i).getContentLength());
				System.out.println("Contents :: "+listcontents.toString());
			}
    	}
		if(uploadthis){
			if(tag!=null) report.put("tag", tag);
			if(url!=null) report.put("url", url);
			if(params!=null) report.put("parameters",pars);
			if(files!=null) report.put("uploads", files);
		}
    	if(isCancelled()||post.isAborted()){
    		if(logthis){
				System.out.println("> Response :: request was cancelled");
				System.out.println("------------------------------------------------------");
    		}
    		if(uploadthis) report.put("response","Request was cancelled");
    		return null;
    	}
		try{
	        if(status.getStatusCode() == HttpStatus.SC_OK){
	            ArrayList<String> headkvs = new ArrayList<String>();
	            Header[] headers = response.getAllHeaders();
	            for(Header head : headers) headkvs.add(head.getName()+" : "+head.getValue());
	            report.put("headers",headkvs.toString());
	            HttpEntity responseentity = response.getEntity();
	            if(responseentity==null) return null;
	            Header responsetype = responseentity.getContentType();
	            String contype = responsetype==null?null:responsetype.getValue();
            	if(contype!=null) isfile = contype.contains("image/");
	            report.put("contenttype",contype);
	            ByteArrayOutputStream out = new ByteArrayOutputStream();
	            responseentity.writeTo(out);
            	result = out.toByteArray();
	        	String text = isfile?"Result is a file sized "+out.size()+" bytes":new String(result);
		        if(logthis){
					System.out.println("> Response :: "+text);
					System.out.println("------------------------------------------------------");
		        }
		        if(uploadthis) report.put("response",text);
	        }else response.getEntity().getContent().close();
    	}catch(IOException ex){
    		if(logthis){
				System.out.println("> IO Error : "+ex.getMessage());
				System.out.println("------------------------------------------------------");
    		}
    		if(uploadthis) report.put("error", "IO Error");
    	}
    	return result;
    }
	@Override protected void onPostExecute(byte[] result) {
		isrunning = false;
        super.onPostExecute(result);
        if(tag!=null&&tag.equals("uplog")) return;
        uplog(null,"http",report);
        try{
	        if(result==null) onResultNull(tag, url, params, files);
	        else if(isfile) onResult(result, tag, url, params, files);
	        else onResult(new String(result), tag, url, params, files);
        }catch(NullPointerException npe){
        	System.out.println("NPE get caught : "+npe.getMessage());
        }
    }
    
	public void onResultNull(String tag, String url, HashMap<String, Object> params, ArrayList<String> files) {};
	public void onResult(String result, String tag, String url, HashMap<String, Object> params, ArrayList<String> files){};
	public void onResult(byte[] result, String tag, String url, HashMap<String, Object> params, ArrayList<String> files){};
	
	public static ApiRequest request(String tag, String url, HashMap<String, Object> params, ArrayList<String> files, ArrayList<ContentBody> contents, final ApiHandler handler, String upload) {
		if(log&&!tag.equals("uplog")){
			System.out.println("------------ HTTP Request -----------");
			System.out.println("Tag : "+tag);
			System.out.println("Url : "+url);
			if(params!=null) System.out.println("Parameters : "+params.toString());
			if(files!=null) System.out.println("Files : "+files.toString());
			if(handler!=null) System.out.println("Handler : "+handler.getClass().getCanonicalName());
			ArrayList<String> listcontents = new ArrayList<String>();
			if(contents!=null){
				for(int i=0; i<contents.size(); i++) listcontents.add(contents.get(i).getFilename()+" : "+contents.get(i).getContentLength());
				System.out.println("Contents :: "+listcontents.toString());
			}
			System.out.println("-------------------------------------");
		}
		ApiRequest request = new ApiRequest(url,params,files,contents,tag,upload){
    		@Override public void onResult(String result, String tag, String url, HashMap<String, Object> params, ArrayList<String> files) {
    			if(handler!=null) handler.handle(tag, result, url, params, files);
			}
    		@Override public void onResultNull(String tag, String url, HashMap<String, Object> params, ArrayList<String> files) {
    			if(handler!=null) handler.handle(tag, null, url, params, files);
    		}
		};
		request.execute();
		return request;
	}
	public static ApiRequest request(String tag, String url, HashMap<String, Object> params, ArrayList<String> files, ArrayList<ContentBody> contents, final ApiHandler handler) {
    	return request(tag, url, params, files, contents, handler, null);
	}
	public static ApiRequest request(String tag, String url, HashMap<String, Object> params, ArrayList<String> files, final ApiHandler handler) {
    	return request(tag, url, params, files, null, handler, null);
	}
    public static ApiRequest request(String tag, String url, HashMap<String, Object> params, final ApiHandler handler){
    	return request(tag, url, params, null, handler);
    }	
    public static ApiRequest request(String tag, String url, final ApiHandler handler){
    	return request(tag, url, null, handler);
    }
	public static ApiRequest request(String url, HashMap<String, Object> params, ArrayList<String> files, final ApiHandler handler){
		return request(null, url, params, files, handler);
	}	
	public static ApiRequest request(String url, HashMap<String, Object> params, final ApiHandler handler){
		return request(url, params, null, handler);
	}
	public static ApiRequest request(String url, final ApiHandler handler){
		return request(null, url, null, handler);
	}
	public static ApiRequest requestFile(String tag, String url, HashMap<String, Object> params, ArrayList<String> files, ArrayList<ContentBody> contents, final BytesHandler handler, String upload){
		if(log&&!tag.equals("uplog")){
			System.out.println("------------ HTTP File Request -----------");
			System.out.println("Tag : "+tag);
			System.out.println("Url : "+url);
			if(params!=null) System.out.println("Parameters : "+params.toString());
			if(files!=null) System.out.println("Files : "+files.toString());
			if(handler!=null) System.out.println("Handler : "+handler.getClass().getCanonicalName());
			System.out.println("-------------------------------------");
		}
		ApiRequest request = new ApiRequest(url,params,files,contents,tag,upload){
    		@Override public void onResult(byte[] result, String tag, String url, HashMap<String, Object> params, ArrayList<String> files) {
    			if(handler!=null) handler.handle(tag, result, url, params, files);
			}
    		@Override public void onResultNull(String tag, String url, HashMap<String, Object> params, ArrayList<String> files) {
    			if(handler!=null) handler.handle(tag, null, url, params, files);
    		}
		};
		request.execute();
		return request;
	}
	public static ApiRequest requestFile(String tag, String url, HashMap<String, Object> params, ArrayList<String> files, ArrayList<ContentBody> contents, final BytesHandler handler) {
		return requestFile(tag, url, params, files, contents, handler, null);
	}
	public static ApiRequest requestFile(String tag, String url, HashMap<String, Object> params, ArrayList<String> files, final BytesHandler handler) {
		return requestFile(tag, url, params, files, null, handler, null);
	}
	
    public static ApiRequest requestFile(String tag, String url, HashMap<String, Object> params, final BytesHandler handler){
    	return requestFile(tag, url, params, null, handler);
    }
    	
    public static ApiRequest requestFile(String tag, String url, final BytesHandler handler){
    	return requestFile(tag, url, null, handler);
    }
    
	public static ApiRequest requestFile(String url, HashMap<String, Object> params, ArrayList<String> files, final BytesHandler handler){
		return requestFile(null, url, params, files, handler);
	}
	
	public static ApiRequest requestFile(String url, HashMap<String, Object> params, final BytesHandler handler){
		return requestFile(url, params, null, handler);
	}
	public static ApiRequest requestFile(String url, final BytesHandler handler){
		return requestFile(null, url, null, null, handler);
	}
	
    public static interface ApiHandler{
    	public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files);
    }
    
    public static interface BytesHandler{
		public void handle(String tag, byte[] result, String url, HashMap<String, Object> params, ArrayList<String> files);
    }
    
    public static void uplog(String url, String file, String data){
        /*
        Put this PHP code inside the server where the URL point to the file
        -------------------------------------------------------------------
    	mkdir('reports'); //or any other folder name you want
    	$file = isset($_REQUEST['file'])?$_REQUEST['file']:'general';
    	$data = isset($_REQUEST['data'])?$_REQUEST['data']:'No appended data';
    	$open = fopen('reports/'.$file,'a') or die('Cannot open file : reports/'.$file);
    	$write = "\nTime : ".date('Y-m-d H:i:s')."\nMessage : ".$data;
    	unset($_REQUEST['file'],$_REQUEST['data']);
    	$write.="\nUser information : ".json_encode($_REQUEST);
    	$write.="\n--------------------------------------------------";
    	$suc = fwrite($open, $write);
    	fclose($open);
    	echo $suc?'Report successfully logged':'Report fail to logged';
    	if(!$suc) return;
    	$gcmid = GCM_USER_ID //User's GCM id used as the target for the GCM server to send the push notification
    	$apikey = APP_API_KEY //The monitor app API access key
    	$url = GCM_SENDER_URL //The URL of the server that send request to GCM server
    	$post = http_build_query(array(gcmid => urlencode($gcmid), type => $type, key => urlencode($apikey)));
    	$opts = array('http' =>array(method=>'POST',header=>'Content-type: application/x-www-form-urlencoded',content=>$post));
    	echo file_get_contents($url, false, stream_context_create($opts));
    	--------------------------------
    	PHP file to send the push notification requst to GCM server
    	--------------------------------
    	$post = array(registration_ids=>array($_POST['gcmid']),data=>array(type=> $_POST['type']));
    	$ch = curl_init();
    	curl_setopt($ch, CURLOPT_URL, 'https://android.googleapis.com/gcm/send');
    	curl_setopt($ch, CURLOPT_POST, true);
    	curl_setopt($ch, CURLOPT_HTTPHEADER, array('Authorization: key='.$_POST['key'], 'Content-Type: application/json'));
    	curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    	curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($post));
    	$result = curl_exec($ch);
    	if(curl_errno($ch)) echo 'error : '.curl_error($ch);
    	else print_r($result);
    	curl_close($ch);
        */
    	if(url==null) url = "http://www.facilipack.com/api/index.php/testapi/report_write";
    	if(file==null) file = "general";
    	HashMap<String, Object> params = Mapper.init().put("data", data).put("file", file)
    		.put("version", Build.VERSION.RELEASE).put("sdk", String.valueOf(Build.VERSION.SDK_INT))
			.put("vendor", Build.MANUFACTURER).put("brand", Build.BRAND).put("model", Build.MODEL)
			.put("product", Build.PRODUCT).put("design", Build.DEVICE).put("hardware", Build.HARDWARE).done();
    	ApiRequest.request("uplog",url, params, null);
    }
    public static void uplog(String url, String file, HashMap<String, Object> data){
    	uplog(url, file, new JSONObject(data).toString());
    }
    public static void uplog(String file, String data){
    	uplog(null,file,data);
    }
    public static void uplog(String data){
    	uplog(null,null,data);
    }
}