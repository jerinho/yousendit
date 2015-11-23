package my.yousendit;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import my.helper.ApiRequest;
import my.helper.JrgAndroid;
import my.helper.Notifier;
import my.helper.ApiRequest.ApiHandler;
import my.helper.Notifier.GcmBroadcastReceiver;
import my.helper.Notifier.MessageHandler;
import my.helper.SimpleHttp;
import my.yousendit.peripherals.DrawerActivity;
import my.yousendit.peripherals.Shared;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Chat extends DrawerActivity implements ApiHandler, OnClickListener, MessageHandler, OnScrollListener, OnGlobalLayoutListener{
	
	ArrayAdapter<JSONObject> adaptermessages, adapterusers;
	ListView listusers, listmessages;
	LinearLayout panel;
	LayoutInflater inflater;
	int current, state, selected;
	EditText message;
	ImageButton button;
	String name, image;
	JSONObject active;
	SQLiteOpenHelper sql;
	boolean chatonly, globallayouton = true;
	HashMap<Integer, Integer> counts = new HashMap<Integer, Integer>();
    Transformation transform = new Transformation() {
		
		@Override public Bitmap transform(Bitmap bmp) {
			Bitmap cropped = JrgAndroid.circleCrop(bmp,100,100);
			bmp.recycle();
			return cropped;
		}
		
		@Override public String key() {
			return "chatuser";
		}
	};
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int to = getIntent().getIntExtra("id", 0);
		chatonly = getIntent().getBooleanExtra("chatonly", false);
		image = getIntent().getStringExtra("image");
		inflater = getLayoutInflater();
		initSqlDb();
		/*
		root = getWindow().getDecorView().getRootView();
		observer = root.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(this);
		*/
		if(to!=0) openChat(to, getIntent().getStringExtra("name"));
		if(chatonly) return;
		Cursor cursor = sql.getReadableDatabase().query("counter", new String[]{"whom","count"}, "user = "+Shared.id, null, null, null, null);
		if(cursor!=null){
			while(cursor.moveToNext()) counts.put(cursor.getInt(0), cursor.getInt(1));
			cursor.close();
			//System.out.println("---------------- User new notifications counts ----------------");
			//System.out.println("No. of columns : "+cursor.getColumnCount());
			//System.out.println("Count : "+cursor.getCount());
			//System.out.println("---------------------------------------------------------------");
		}
		HashMap<String,Object> pars = new HashMap<String, Object>();
		pars.put("loginkey", Shared.loginkey);
		request = SimpleHttp.request("list",Shared.url+"chat/listChatRoom",pars,this);
	}
	
	@Override protected void onResume() {
		super.onResume();
		Notifier.setHandler(this, this, Shared.projectkey);
	}
	
	@Override protected void onPause() {
		super.onPause();
		Notifier.removeHandler();
	}
	
	public void initSqlDb(){
		if(sql==null) sql = new SQLiteOpenHelper(this, "yousendit", null, 1, null) {
			
			@Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				db.execSQL("drop table messages;");
				db.execSQL("drop table counter;");
				onCreate(db);
			}
			
			@Override public void onCreate(SQLiteDatabase db) {
				db.execSQL("create table messages (user integer(8) not null, whom integer(8) not null, direction integer(1) not null, time datetime not null, message text not null);");
				db.execSQL("create table counter (user integer(8) not null, whom integer(8) not null, count integer(3) not null);");
			}
		};
	}
	
	@Override public View setMainView() {
		View view = inflate(R.layout.chat);
        listusers = (ListView) view.findViewById(R.id.chatlistusers);
        listmessages = (ListView) view.findViewById(R.id.chatlistmessages);
        listmessages.setDivider(null);
        listmessages.setDividerHeight(0);
        panel = (LinearLayout) view.findViewById(R.id.chatpanel);
        message = (EditText) view.findViewById(R.id.chatmessagetosend);
		adapterusers = new ArrayAdapter<JSONObject>(this, android.R.layout.simple_list_item_1){
			
			@Override public View getView(int position, View reuse, ViewGroup parent) {
				View view = reuse==null?inflater.inflate(R.layout.chatuser, null):reuse;
				ImageView image = (ImageView) view.findViewById(R.id.chatuserimage);
				TextView tvname = (TextView) view.findViewById(R.id.chatusername);
				TextView tvmessage = (TextView) view.findViewById(R.id.chatusermessage);
				TextView tvtime = (TextView) view.findViewById(R.id.chatusertime);
				TextView tvcount = (TextView) view.findViewById(R.id.chatusercount);
				try{
					JSONObject data = getItem(position);
					tvname.setText(data.getString("name"));
					String imgurl = data.isNull("image")?null:data.getString("image");
					if(imgurl!=null) Picasso.with(Chat.this).load(imgurl).fit().centerInside().transform(transform).into(image);
					Date created = data.isNull("lastDateTime")?Calendar.getInstance().getTime():Shared.sqltime.parse(data.getString("lastDateTime"));
					Calendar cal1 = Calendar.getInstance(), cal2 = Calendar.getInstance();
					cal2.setTime(created);
					boolean sameday = cal1.get(Calendar.YEAR)==cal2.get(Calendar.YEAR)&&cal1.get(Calendar.DAY_OF_YEAR)==cal2.get(Calendar.DAY_OF_YEAR);
					String time = (sameday?Shared.timeonly:Shared.monthdate).format(created);
					tvtime.setText(time);
					tvmessage.setText(data.getString("lastMessage"));
					Integer count = counts.get(data.getInt("userId"));
					tvcount.setVisibility((count==null||count==0)?View.GONE:View.VISIBLE);
					if(count!=null) tvcount.setText(String.valueOf(count));
				}catch(JSONException ex){
					//System.out.println("JSON Error : "+ex.getMessage());
				}catch(ParseException ex){
					//System.out.println("ParseException : "+ex.getMessage());
				}
				view.setTag(position);
				view.setOnClickListener(Chat.this);
				return view;
			}
		};
		adaptermessages = new ArrayAdapter<JSONObject>(this, android.R.layout.simple_list_item_1){

			@Override public View getView(int position, View reuse, ViewGroup parent) {
				LinearLayout view = (LinearLayout) (reuse==null?inflater.inflate(R.layout.chatmessage, null):reuse);
				LinearLayout bubble = (LinearLayout) view.findViewById(R.id.chatmessagebubble);
				TextView text = (TextView) view.findViewById(R.id.chatmessagetext);
				//ImageView ivuser = (ImageView) view.findViewById(R.id.chatmessageuser);
				//ImageView ivother = (ImageView) view.findViewById(R.id.chatmessageother);
				ImageView pointuser = (ImageView) view.findViewById(R.id.chatmessagepointme);
				ImageView pointother = (ImageView) view.findViewById(R.id.chatmessagepointyou);
				try{
					JSONObject data = getItem(position), prev = position==0?null:getItem(position - 1);
					String msg = data.getString("message");
					Date created = data.isNull("created_at")?Calendar.getInstance().getTime():Shared.sqltime.parse(data.getString("created_at"));
					Calendar cal1 = Calendar.getInstance(), cal2 = Calendar.getInstance();
					cal2.setTime(created);
					boolean sameday = cal1.get(Calendar.YEAR)==cal2.get(Calendar.YEAR)&&cal1.get(Calendar.DAY_OF_YEAR)==cal2.get(Calendar.DAY_OF_YEAR);
					String time = (sameday?Shared.timeonly:Shared.monthdate).format(created), prevtime = null;
					data.put("time", time);
					boolean byme = data.getBoolean("byme"), prevbyme = false, same = false, sametime = true;
					if(prev!=null){
						prevbyme = prev.getBoolean("byme");
						prevtime = prev.isNull("time")?null:prev.getString("time");
						same = byme&&prevbyme||!byme&&!prevbyme;
						sametime = prevtime==null||same&&time.equals(prevtime);
					}
					//ivuser.setVisibility(byme?View.VISIBLE:View.INVISIBLE);
					//ivother.setVisibility(byme?View.INVISIBLE:View.VISIBLE);
					//if(image==null&&active!=null&&!active.isNull("image")) image = active.getString("image");
					//if(byme) Picasso.with(Chat.this).load(Shared.image).transform(transform).into(ivuser);
					//else if(image!=null) Picasso.with(Chat.this).load(image).transform(transform).into(ivother);
					pointuser.setVisibility(byme?View.VISIBLE:View.INVISIBLE);
					pointother.setVisibility(byme?View.INVISIBLE:View.VISIBLE);
					if(same){
						pointuser.setVisibility(View.INVISIBLE);
						pointother.setVisibility(View.INVISIBLE);
					}
					view.setPadding(5, same?5:10, 5, 0);
					view.setGravity(byme?Gravity.RIGHT:Gravity.LEFT);
					bubble.setBackground(getResources().getDrawable(byme?R.drawable.chatbubble_self:R.drawable.chatbubble_other));
				    Spannable span = new SpannableString(msg+"\n"+time);
				    ForegroundColorSpan spanmsg = new ForegroundColorSpan(byme?Color.BLACK:Color.WHITE);
				    ForegroundColorSpan spantime = new ForegroundColorSpan(byme?Color.BLACK:Color.WHITE);
				    RelativeSizeSpan spansmall = new RelativeSizeSpan(0.7f);
				    span.setSpan(spanmsg, 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				    span.setSpan(spantime, msg.length(), span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				    span.setSpan(spansmall, msg.length(), span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					text.setText(span);
				}catch(JSONException ex){
					//System.out.println("JSONException : "+ex.getMessage());
				}catch(ParseException ex){
					//System.out.println("ParseException : "+ex.getMessage());
				}
				return view;
			}
		};
		adapterusers.setNotifyOnChange(false);
		adaptermessages.setNotifyOnChange(false);
		listusers.setAdapter(adapterusers);
		listmessages.setAdapter(adaptermessages);
		listmessages.setOnScrollListener(this);
		listmessages.getViewTreeObserver().addOnGlobalLayoutListener(this);
		return view;
	}
	
	public void openChat(int user, String name){
		//System.out.println("Chat to : "+name);
		setEnabled(false);
		if(listusers!=null) listusers.setVisibility(View.GONE);
		if(listmessages!=null)listmessages.setVisibility(View.VISIBLE);
		if(panel!=null) panel.setVisibility(View.VISIBLE);
		setTitle(name);
		if(adaptermessages!=null) adaptermessages.clear();
		current=user;
		counts.put(current, 0);
		sql.getWritableDatabase().execSQL("update counter set count = 0 where user = "+Shared.id+" and whom = "+current);
		if(adapterusers!=null) adapterusers.notifyDataSetChanged();
		onScroll(null, 0, 0, 0);
		onScroll(null, 0, 0, 0);
		HashMap<String,Object> pars = new HashMap<String, Object>();
		pars.put("loginkey", Shared.loginkey);
		pars.put("userId", String.valueOf(current));
		request = SimpleHttp.request("messages",Shared.url+"chat/getUnreadMessage",pars,this);
	}
	
	public void send(View view){
		button = (ImageButton) view;
		button.setEnabled(false);
		message.setEnabled(false);
		HashMap<String,Object> pars = new HashMap<String, Object>();
		pars.put("loginkey", Shared.loginkey);
		pars.put("message", message.getText().toString());
		pars.put("userId", String.valueOf(current));
		request = SimpleHttp.request("send",Shared.url+"chat/sendMessage",pars,this);
	}
	
	public void storeMessage(JSONObject obj) throws JSONException{
		//int user, int other, String time, String msg, boolean byme
		ContentValues cv = new ContentValues();
		cv.put("user",Shared.id);
		cv.put("whom",obj.getInt("whom"));
		cv.put("direction",obj.getBoolean("byme")?1:0);
		cv.put("time",obj.getString("created_at"));
		cv.put("message",obj.getString("message"));
		long row = sql.getWritableDatabase().insert("messages", null, cv);
		//System.out.println("------------- Store new message ---------------");
		//System.out.println("User ID : "+Shared.id);
		//System.out.println("Other user ID : "+current);
		//System.out.println("Time : "+obj.getString("created_at"));
		//System.out.println("Outgoing : "+obj.getBoolean("byme"));
		//System.out.println("Message : "+obj.getString("message"));
		//System.out.println("Row ID : "+row);
		//System.out.println("-------------------------------------------------------");
	}
	
	public void scrollToBottom(){
		(new Thread(){
			public void run() {
				listmessages.smoothScrollToPosition(adaptermessages.getCount());
			};
		}).start();
	}
	
	
	@Override public void onClick(View v) {
		selected = (Integer) v.getTag();
		active = adapterusers.getItem(selected);
		try{openChat(active.getInt("userId"),active.getString("name"));}
		catch(JSONException ex){}
	}
	
	@Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override public boolean onContextItemSelected(MenuItem item) {
		return false;
	}
	
    @Override public boolean onOptionsItemSelected(MenuItem item) {
    	if(chatonly) finish();
		else if(!listmessages.isShown()) super.onOptionsItemSelected(item);
		else{
			listmessages.setVisibility(View.GONE);
			panel.setVisibility(View.GONE);
			listusers.setVisibility(View.VISIBLE);
			setTitle("Chat History");
			setEnabled(true);
		}
    	return true;
    }
	
	@Override public void onBackPressed() {
		if(listmessages.isShown()) onOptionsItemSelected(null);
		else super.onBackPressed();
	}

	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		if(result==null){
			Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show();
			return;
		}
		if(tag.equals("list")) try{
			JSONObject json = new JSONObject(result);
			if(json.getInt("success")==1) try{
				JSONArray data = json.getJSONArray("post");
				adapterusers.clear();
				if(data!=null) for(int i=0; i<data.length();i++){
					JSONObject obj = data.getJSONObject(i);
					Shared.sqltime.setTimeZone(TimeZone.getTimeZone("UTC"));
					Date servertime = Shared.sqltime.parse(obj.getString("lastDateTime"));
					Shared.sqltime.setTimeZone(TimeZone.getDefault());
					String time = Shared.sqltime.format(servertime);
					obj.put("lastDateTime", time);
					adapterusers.add(obj);
				}
				adapterusers.notifyDataSetChanged();
			}catch(JSONException ex){
				//System.out.println("JSON Exception : "+ex.getMessage());
			}catch(ParseException ex){
				//System.out.println("Parse Exception : "+ex.getMessage());
			}
		}catch(JSONException ex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
		if(tag.equals("send")) try{
			message.setText("");
			message.setEnabled(true);
			button.setEnabled(true);
			JSONObject json = new JSONObject(result);
			if(json.getInt("success")==1) try{
				//System.out.println(params);
				JSONObject data = json.has("post")?json.getJSONObject("post"):null;
				Shared.sqltime.setTimeZone(TimeZone.getTimeZone("UTC"));
				Date servertime = Shared.sqltime.parse(data.getString("time"));
				Shared.sqltime.setTimeZone(TimeZone.getDefault());
				String time = Shared.sqltime.format(servertime);
				String message = (String) params.get("message");
				JSONObject obj = new JSONObject().put("message", message).put("created_at", time).put("byme", true).put("whom", current);
				storeMessage(obj);
				adaptermessages.add(obj);
				adaptermessages.notifyDataSetChanged();
				scrollToBottom();
			}
			catch(JSONException ex){
				//System.out.println("JSON Exception : "+ex.getMessage());
			}
			catch(ParseException ex){
				//System.out.println("Parse Exception : "+ex.getMessage());
			}
		}catch(JSONException ex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
		if(tag.equals("messages")) try{
			JSONObject json = new JSONObject(result);
			if(json.getInt("success")==1) try{
				int sender = Integer.valueOf((String) params.get("userId"));
				JSONArray data = json.has("post")?json.getJSONArray("post"):null;
				boolean iscurrent = listmessages.isShown()&&sender==current;
				/*
				int countold = counts.get(sender);
				int countnew = iscurrent?0:(countold+(data==null?0:data.length()));
				counts.put(sender, countnew);
				String query = "update counter set count = "+countnew+" where user = "+Shared.id+" and whom = "+sender;
				if(countold!=countnew) sql.getWritableDatabase().execSQL(query);
				*/
				adapterusers.notifyDataSetChanged();
				for(int i=0; i<data.length();i++){
					JSONObject obj = data.getJSONObject(i).put("byme", false);
					Shared.sqltime.setTimeZone(TimeZone.getTimeZone("UTC"));
					Date servertime = Shared.sqltime.parse(obj.getString("created_at"));
					Shared.sqltime.setTimeZone(TimeZone.getDefault());
					String time = Shared.sqltime.format(servertime);
					obj.put("created_at", time);
					obj.put("whom", sender);
					storeMessage(obj);
					if(iscurrent) adaptermessages.add(obj);
				}
				if(iscurrent) adaptermessages.notifyDataSetChanged();
				scrollToBottom();
			}
			catch(JSONException ex){
				//System.out.println("JSON Exception : "+ex.getMessage());
			}
			catch(ParseException ex){
				//System.out.println("Parse Exception : "+ex.getMessage());
			}
		}catch(JSONException ex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override public boolean onNotifierMessage(Bundle bundle, GcmBroadcastReceiver receiver) {
		String code = bundle.getString("code"), message = bundle.getString("message");
		receiver.setNotifOn(code==null||Integer.parseInt(code)!=8);
		try{
			JSONObject json = new JSONObject(bundle.getString("post"));
			int toupdate = -1, sender = Integer.parseInt(json.getString("sender"));
			if(adapterusers==null) return true;
			for(int i=0; i<adapterusers.getCount(); i++)
				if(adapterusers.getItem(i)!=null&&sender==adapterusers.getItem(i).getInt("userId")) toupdate = i;
			Shared.sqltime.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date time = Shared.sqltime.parse(json.getString("created_at"));
			Shared.sqltime.setTimeZone(TimeZone.getDefault());
			if(toupdate!=-1) adapterusers.getItem(toupdate)
				.put("lastMessage",json.getString("message")).put("lastDateTime",Shared.sqltime.format(time));
			else adapterusers.add(json
				.put("userId",sender).put("name",json.getString("senderName"))
				.put("lastMessage",json.getString("message")).put("lastDateTime",Shared.sqltime.format(time))
			);
			boolean b = !listmessages.isShown()||sender!=current;
			if(b) counts.put(sender,counts.get(sender)+1);
			adapterusers.notifyDataSetChanged();
			if(b) return true;
			HashMap<String,Object> pars = new HashMap<String, Object>();
			pars.put("loginkey", Shared.loginkey);
			pars.put("userId", String.valueOf(sender));
			request = SimpleHttp.request("messages",Shared.url+"chat/getUnreadMessage",pars,this);
		}
		catch(JSONException ex){
			//System.out.println("JSON Exception : "+ex.getMessage());
		}catch(ParseException ex){
			//System.out.println("Parse Exception : "+ex.getMessage());
		}
		return true;
	}
	
	@Override public void onNotifierError(String message) {
	}

	@Override public void onNotifierKey(String gcmid) {
	}
	
	@Override public void onScrollStateChanged(AbsListView view, int scrollState) {
		state = scrollState;
	}
	
	@Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		//System.out.println("First visible item : "+firstVisibleItem+", Visible item count : "+visibleItemCount+", Total item count : "+totalItemCount);
		initSqlDb();
		if(firstVisibleItem!=0||current==0) return;
		try{
			String last = adaptermessages.getCount()!=0?adaptermessages.getItem(0).getString("created_at"):null;
			Cursor cursor = sql.getReadableDatabase().query("messages", new String[]{"time","direction","message"}, "user = "+Shared.id+" and whom = "+current+(last!=null?" and time < datetime('"+last+"')":""), null, null, null, "time desc", "10");
			if(cursor==null||cursor.getCount()==0) return;
			//System.out.println("---------------- Finish fetched messages from database ----------------");
			//System.out.println("Before : "+last+". "+Columns : "+cursor.getColumnCount()+". "+Rows : "+cursor.getCount());
			//System.out.println("---------------------------------------------------------------");
			while(cursor.moveToNext()){
				String time = cursor.getString(0);
				boolean dir = cursor.getInt(1)==1;
				String message = cursor.getString(2);
				//System.out.println("---------------- Message fetched from database ----------------");
				//System.out.println("Time : "+time+". "+By me : "+dir+". "+Message : "+message);
				//System.out.println("---------------------------------------------------------------");
				adaptermessages.insert(new JSONObject().put("message", message).put("created_at", time).put("byme",dir).put("whom",current),0);
			}
			adaptermessages.notifyDataSetChanged();
			globallayouton = false;
			listmessages.setSelection(cursor.getCount()+1);
			cursor.close();
		}catch(JSONException ex){
			//System.out.println("JSON Exception : "+ex.getMessage());
		}
	}
	
	@Override public void onGlobalLayout() {
		if((listmessages.getRootView().getHeight() - listmessages.getHeight()) > listmessages.getRootView().getHeight()/3){
			//System.out.println("Keyboard shown");
			if(globallayouton) scrollToBottom();
			else globallayouton = true;
		}else{
			//System.out.println("Keyboard hidden");
		}
	}
}