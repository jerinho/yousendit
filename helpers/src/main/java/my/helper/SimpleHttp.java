package my.helper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import my.helper.ApiRequest.ApiHandler;
import my.helper.Requester.Mapper;

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

import android.os.AsyncTask;
import android.os.Build;

public class SimpleHttp extends AsyncTask<String,String,byte[]>{
	
	public HashMap<String, Object> params;
	public String url, tag, result;
	public HttpPost post;
	public boolean log = false;
	int timeout = 15000;
	
	public SimpleHttp(String tag, String url, HashMap<String, Object> params) {
		this.url = url;
		this.params = params;
		this.tag = tag;
	}
	
	public void abort(){
		cancel(true);
		if(post!=null) post.abort();
	}
	
	@Override protected byte[] doInBackground(String... uri) {
		if(log){
			System.out.println("------------ Http Preexecution -----------");
			if(tag!=null) System.out.println("Tag : "+tag);
			if(url!=null) System.out.println("Url : "+url);
			if(params!=null) System.out.println("Parameters : "+params.toString());
			System.out.println("------------------------------------------");
		}
		ArrayList<NameValuePair> list = this.params==null?null:new ArrayList<NameValuePair>(this.params.size());
		byte[] result = null;
		ArrayList<String> pars = new ArrayList<String>();
		HashMap<String,Object> iterated = this.params==null?null:(HashMap<String, Object>) this.params.clone();
		if(iterated!=null&&iterated.size()!=0) {
			Iterator iterator = iterated.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry pair = (Map.Entry)iterator.next();
				String key = String.valueOf(pair.getKey());
				String val = String.valueOf(pair.getValue());
				if(list!=null) list.add(new BasicNameValuePair(key,val));
				iterator.remove();
		        pars.add(key+":"+val);
			}
		}
		if(post!=null) post = null;
		if(isCancelled()){
			//System.out.println("Http request been cancelled. Url : "+url);
			return null;
		}
		post = new HttpPost(this.url);
		try{
			if(list!=null) post.setEntity(new UrlEncodedFormEntity(list));
		}catch(UnsupportedEncodingException ex){
			return null;
		}
		if(isCancelled()) return null;
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, this.timeout);
		HttpClient client = new DefaultHttpClient(httpParams);
		HttpResponse response = null;
		if(isCancelled()) return null;
		try{
			response = client.execute(post);
			//cause IO Exception with message "Connection to [URL] refused"
		}catch(ClientProtocolException ex){
		}catch(IOException ex) {
			//System.out.println("IOException : "+ex.getMessage());
		}
		if(response==null) return null;
		StatusLine status = response.getStatusLine();
		if(isCancelled()) return null;
		try{
			if(status.getStatusCode() == HttpStatus.SC_OK){
				if(response.getEntity()==null) return null;
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				result = out.toByteArray();
			}else response.getEntity().getContent().close();
		}catch(IOException ex){}
		if(isCancelled()) return null;
		if(log){
			System.out.println("\n-------------------- Http Response -------------------");
			if(tag!=null) System.out.println("Tag : "+tag);
			if(url!=null) System.out.println("Url : "+url);
			if(pars!=null) System.out.println("Parameters : "+pars.toString());
			System.out.println("Result : "+new String(result));
			System.out.println("--------------------------------------------------------");
		}
		return result;
	}
	
	@Override protected void onPostExecute(byte[] result) {
		super.onPostExecute(result);
		this.result = result==null?null:new String(result);
		onResult(this);
	}

	public void onResult(SimpleHttp obj) {
	}
	
	public static SimpleHttp request(String tag, String url, HashMap<String, Object> params, final ApiHandler handler) {
		SimpleHttp request = new SimpleHttp(tag, url,params){
    		@Override public void onResult(SimpleHttp obj) {
    			if(handler!=null) handler.handle(tag, result, url, params, null);
			}
		};
		request.execute();
		return request;
	}
}