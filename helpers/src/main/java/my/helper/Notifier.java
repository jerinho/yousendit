package my.helper;

import java.io.IOException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.format.Time;
import android.widget.Toast;

public abstract class Notifier{
	
	static String senderid, gcmid;
	static MessageHandler handler;
	static Context context;
	static GoogleCloudMessaging gcm;
	static SharedPreferences pref;
	static SharedPreferences.Editor editor;
	static String title;
	static String key; //key name to get the message from bundle
	
    public static void setHandler(Context context,MessageHandler handler, String senderid){
    	//set the context to become the GCM instance context and to handle the incoming downstream message
    	//in case if the GCM Id is null, expired, or invalid, register new GCM Id
    	//receiver must be set at AndroidManifest.xml with name my.helper.Notifier.GcmBroadcastReceiver
    	Notifier.context = context;
    	Notifier.handler = handler;
    	if(gcmid==null){
	    	if(pref==null){
	    		pref = context.getSharedPreferences("userpref",Context.MODE_PRIVATE);
	    		editor = pref.edit();
	    	}
	    	gcmid = pref.getString("gcmid", null);
	    	if(gcmid!=null) handler.onNotifierKey(gcmid);
    	}
    	if(gcmid==null){
        	if(senderid!=null) Notifier.senderid = senderid;
        	if(Notifier.senderid!=null){
	        	int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
	    		if(resultCode != ConnectionResult.SUCCESS && !GooglePlayServicesUtil.isUserRecoverableError(resultCode)) return;
	        	new NotifierRegistrar().execute(Notifier.senderid,null,null);
        	}
    	}
    }
    
    public static void removeHandler(){
    	handler = null;
    }
    
	public static class NotifierRegistrar extends AsyncTask<String, String, String>{
		
		boolean b;
		
		@Override protected String doInBackground(String... params) {
	    	try {
	        	if(gcm==null) gcm = GoogleCloudMessaging.getInstance(context.getApplicationContext());
	        	if(gcmid==null){
	        		gcmid = gcm.register(params[0]);
	        		editor.putString("gcmid",gcmid).apply();
	        		b = true;
	        		return gcmid;
	        	}
			} catch (IOException e) {
				b = false;
				return e.getMessage();
			}
			return null;
		}
		
		@Override protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if(b) handler.onNotifierKey(result);
			else handler.onNotifierError(result);
		}
	}
		
	public abstract static class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
		
		public Context context;
		NotificationManager manager;
		boolean toastOn = true, notifOn = true;
		int id, icon, flags;
		String tag, title, message;
		Class activity;
		Bundle extras = new Bundle();
		Notification notification;
		Builder builder;
		
		@Override public void onReceive(Context context, Intent intent) {
			this.context = context;
			if(manager==null) manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			Bundle bundle = intent.getExtras();
			boolean received = onReceive(context, bundle);
			if(!received) return;
			System.out.println("Notification handler : "+(handler==null?null:handler.getClass().getCanonicalName()));
			if(handler!=null&&!handler.onNotifierMessage(bundle,this)) return;
			if(title==null||message==null) return;
			if(toastOn) Toast.makeText(context, title+"\n\n"+message, Toast.LENGTH_LONG).show();
			if(notifOn) pushNotification(bundle);
	    }

		public void pushNotification(Bundle bundle){
			//PendingIntent pending = TaskStackBuilder.create(context)
			//	.addParentStack(activity).addNextIntent(new Intent(context, activity))
			//	.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
			builder = new Builder(context)
				.setSmallIcon(icon).setContentTitle(title).setContentText(message).setTicker(message)
				.setDefaults(Notification.DEFAULT_ALL).setAutoCancel(true);
			builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
			Intent launch = activity==null?null:new Intent(context, activity).putExtras(extras).setFlags(flags);
			PendingIntent pending = launch==null?null:PendingIntent.getActivity(context, 0, launch,flags);
			if(pending!=null) builder.setContentIntent(pending);
			onBuild(bundle,builder);
			notification = builder.build();
			notification.flags |= Notification.FLAG_SHOW_LIGHTS;
			onNotify(bundle,notification);
			manager.notify(tag, id, notification);
		}
		
		public void setTitle(String title){
			this.title = title;
		}
		public void setTag(String tag){
			this.tag = tag;
		}
		public void setMessage(String msg){
			this.message = msg;
		}
		public void setFlags(int flags){
			this.flags = flags;
		}
		public void setId(int id){
			this.id = id;
		}
		public void setIcon(int icon){
			this.icon = icon;
		}
		public void setActivity(Class activity){
			this.activity = activity;
		}
		public void setToastOn(boolean bool){
			this.toastOn = bool;
		}
		public void setNotifOn(boolean bool){
			this.notifOn = bool;
		}
		public void setExtras(Bundle extras){
			this.extras = extras;
		}
		public Bundle extras(){
			return extras;
		}
		
		public abstract boolean onReceive(Context context, Bundle bundle);
		public abstract void onBuild(Bundle bundle, Builder build);
		public abstract void onNotify(Bundle bundle, Notification notification);
	}
	
    public static interface MessageHandler{
    	
		public abstract boolean onNotifierMessage(Bundle bundle, GcmBroadcastReceiver receiver);
		public abstract void onNotifierError(String message);
		public abstract void onNotifierKey(String gcmid);
    }
}