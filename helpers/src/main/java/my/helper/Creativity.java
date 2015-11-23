package my.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import my.helper.Requester.Provider;
import my.helper.Requester.RequestListener;
import my.helper.Requester.Responder;
import my.helper.Requester.Result;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;
import android.widget.Toast;

public class Creativity extends Activity implements RequestListener, OnGlobalLayoutListener{
	
	public static int ERROR_TOAST = 1, ERROR_DIALOG = 2, ERROR_VIEW = 3;
	public static int CALL_ACTIVITY = 1, CALL_SERVICE = 2, CALL_BROADCAST = 3, CALL_LOCAL_BROADCAST = 4;
	public static String invalid_email_adress = "Invalid email address";
	private HashMap<String,Requester> reqs = new HashMap<String, Requester>();
	private HashMap<String,ArrayList<String>> ctxmenus = new HashMap<String, ArrayList<String>>();
	private HashMap<String,Intention> intents = new HashMap<String, Creativity.Intention>();
	private ArrayList<String> ctxmenu = new ArrayList<String>();
	private TextView errorview;
	private Requester req;
	private String activemenu;
	private ActionBar action;
	private View root;
	private ViewTreeObserver observer;
	private LocalBroadcastManager lbm;
	private Intention intent;
	private boolean busy, complete;
	private ArrayList<Requester> requests = new ArrayList<Requester>();
	
	public static class Intention{
		String tag, type, action, handle, classname;
		int code, flags, callas;
		Intent selector;
		Uri data;
		Rect bounds;
		ComponentName component;
		Class cls;
		Context context;
	}
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		busy = false;
		root = getWindow().getDecorView().getRootView();
		observer = root.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(this);
		create();
	}
	static int REFRESH_ALWAYS = 1, REFRESH_NEVER = 2;
	@Override protected void onResume() {
		//onPause cancel all the requests and set the flag whether they are all completed or not
		//onResume will check the completion and request only proceed if it is incompleted or asked to reload
		super.onResume();
		int ref = getIntent().getIntExtra("refresh",0);
		if(ref==REFRESH_ALWAYS) complete = false;
		else if(ref==REFRESH_NEVER) complete = true;
		requests.clear();
		if(!complete) refresh();
		else busy = false;
	}
	@Override protected void onPause() {
		super.onPause();
		if(!isRequesting()) complete = true;
		for(Requester req : requests) req.allAbort();
		requests.clear();
		pause();
	}
	public boolean isRequesting(){
		for(Requester req : requests) if(req.isBusy()) return true;
		return false;
	}
	@Override public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}
	@Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		for(String item : ctxmenu) menu.add(item);
	}
	@Override public boolean onContextItemSelected(MenuItem item) {
		return contextItemSelected(activemenu, (String) item.getTitle());
	}
	public void activateContext(String tag){
		if(activemenu==null||!activemenu.equals(tag)){
			activemenu = tag;
			boolean b = ctxmenus.containsKey(tag);
			ctxmenu = b?ctxmenus.get(tag):new ArrayList<String>();
			if(!b) ctxmenus.put(tag,ctxmenu);
		}
	}
	public void addContextItem(String tag, String... labels){
		activateContext(tag);
		for(String label : labels) if(ctxmenu.indexOf(label)==-1) ctxmenu.add(label);
	}
	public void openContextMenu(String tag, View view){
		activateContext(tag);
		registerForContextMenu(view);
		openContextMenu(view);
		unregisterForContextMenu(view);
	}
	public void addRequester(String tag, String url, String api){
		req = Requester.create(tag, url, api);
		req.setListener(this);
		reqs.put(tag, req);
	}
	public void addResponder(String tag, String key, View view){
		if(!req.tag.equals(tag)) req = reqs.get(tag);
		req.addResponder(key, view);
	}
	public void addProvider(String tag, String key, View view){
		if(!req.tag.equals(tag)) req = reqs.get(tag);
		req.addProvider(key, view);
	}
	public void actionBarShow(boolean bool){
		if(action==null) action = getActionBar();
		if(bool) action.show();
		else action.hide();
	}
	public void actionBarBack(boolean bool){
		(action==null?action = getActionBar():action).setDisplayHomeAsUpEnabled(bool);
	}
	public void actionBarIcon(boolean bool){
		(action==null?action = getActionBar():action).setDisplayShowHomeEnabled(bool);
	}
	public void actionBarTitle(boolean bool){
		(action==null?action = getActionBar():action).setDisplayShowTitleEnabled(bool);
	}
	public void setErrorView(TextView view){
		errorview = view;
	}
	public void displayError(int type, String msg, final boolean finish, boolean collapse){
		//Display error
		//type : toast|dialog|textview
		//collapse : errorview is collapsed if error message is null
		//finish : activity is finished after the error. not for errorview
		if(type==ERROR_TOAST) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		else if(type==ERROR_DIALOG) new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("OK",new OnClickListener() {
			@Override public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				if(finish) finish();
			}
		}).show();
		else if(errorview!=null){
			boolean nomsg = msg==null||msg.length()!=0;
			errorview.setVisibility(nomsg?(collapse?View.GONE:View.INVISIBLE):View.VISIBLE);
			errorview.setText(nomsg?"":msg);
		}
		if(type==ERROR_TOAST||errorview!=null) if(finish) new Thread(){
			@Override public void run() {
	            try{busy = true; sleep(2000);}
	            catch (InterruptedException e) {}
	            finally{finish(); busy = false;}
			}
		};
	}
	public Intention getIntent(String tag){
		if(intent==null||!intent.tag.equals(tag)) intent = intents.get(tag);
		return intent;
	}
	public Intention createIntent(String tag){
		getIntent(tag);
		if(intent==null){
			intent = new Intention();
			intent.tag = tag;
			intent.code = intents.size()+1;
			intents.put(tag, intent);
		}
		return intent;
	}
	public void jump(Class activity, boolean finish, boolean force){
		//Jump to other activity
		//activity : target activity class, finish : finish this activity. force : ignore activity business
		if(busy&&!force) return;
    	Intent intent = new Intent(this,activity);
    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    	startActivity(intent);
    	busy = true;
    	if(finish) finish();
	}
	public void call(String tag, boolean force, final Bundle extras){
		if(intent==null||!intent.tag.equals(tag)) intent = intents.get(tag);
		Intent itnt = new Intent();
		if(intent.action!=null) itnt.setAction(intent.action);
		if(intent.type!=null) itnt.setType(intent.type);
		if(intent.component!=null) itnt.setComponent(intent.component);
		if(intent.bounds!=null) itnt.setSourceBounds(intent.bounds);
		if(intent.data!=null) itnt.setData(intent.data);
		if(intent.selector!=null) itnt.setSelector(intent.selector);
		if(intent.flags!=0) itnt.setFlags(intent.flags);
		if(intent.handle!=null) itnt.setPackage(intent.handle);
		if(intent.cls!=null) itnt.setClass(this, intent.cls);
		else if(intent.classname!=null) itnt.setClassName(this, intent.classname);
		if(extras!=null) itnt.putExtras(extras);
		call(intent.callas,intent.code,itnt,force);
	}
	public void call(int type, int code, Intent intent, boolean force){
		//Call and jump to other activiy by intent not by class
		if(busy&&!force) return;
		if(type==1) startActivityForResult(intent,code);
		else if(type==2) startService(intent);
		else if(type==3) sendBroadcast(intent);
		else if(type==4) lbm.sendBroadcast(intent);
		busy = true;
	}
	@Override protected void onActivityResult(int code, int result, Intent data) {
		super.onActivityResult(code, result, data);
		if(result!=RESULT_OK) return;
	    Iterator it = intents.entrySet().iterator();
	    while (it.hasNext()) {
	    	Intention intent = (Intention)((Map.Entry)it.next()).getValue();
	        if(intent.code==code){
	        	onCallResult(intent.tag, data);
	        	return;
	        }
	        it.remove();
	    }
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
	@Override public void onResult(String tag, Result result) {
		busy = false; //Only one request for a requester
	};
	@Override public Object getValue(String tag, Provider prov) {return prov.view instanceof TextView?((TextView) prov.view).getText():null;}
	@Override public int validate(String tag, Provider prov) {return 1;}
	@Override public boolean onResponse(String tag, Responder resp) {return true;}
	@Override public void onRequest(String tag, Responder resp) {}
	@Override public void onParseError(String tag, Result result) {}
	@Override public void onResultNull(String tag, Result result) {}
	@Override public void onResultFail(String tag, Result result) {}
	@Override public void onSuccess(String tag, Result result) {}
	public boolean contextItemSelected(String menu, String item){return true;};
	public void onKeyboardHidden(){}
	public void onKeyboardShown(){}
	public void onCallResult(String tag, Intent data) {}
	public void create() {}
	public void pause() {}
	public void refresh() {}
}