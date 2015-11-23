package my.yousendit.peripherals;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat.Builder;
import my.helper.Notifier.GcmBroadcastReceiver;
import my.yousendit.Chat;
import my.yousendit.FragsJobs;
import my.yousendit.FragsSteps;
import my.yousendit.JobSearch;
import my.yousendit.R;
import my.yousendit.Splash;

public class Broadcast extends GcmBroadcastReceiver implements OnDismissListener{
	
	SQLiteOpenHelper sql;
	
	@Override public boolean onReceive(Context context, Bundle bundle) {
		String strcode = bundle.getString("code");
		int code = strcode==null?0:Integer.parseInt(strcode);
		setTag("yousendit");
		setIcon(R.drawable.icon_sidenav_about_focus);
		setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		setTitle("You Send It");
		setId(5555+code);
		setToastOn(false);
		setNotifOn(true);
		if(code==0) setActivity(Splash.class);
		else if(code==7) setActivity(JobSearch.class);
		else if(code==8) setActivity(Chat.class);
		else if(code>=1&&code<=6) setActivity(FragsSteps.class);
		setExtras(bundle);
		if(code>=1&&code<=7){
			setMessage(bundle.getString("message"));
			return true;
		}else if(code==9) try{
			//Login on other device
			AlertDialog dialog = new AlertDialog.Builder(context).setMessage(bundle.getString("message")).setNegativeButton(null, null).setPositiveButton("OK",null).create();
			dialog.setCancelable(false);
			dialog.setCanceledOnTouchOutside(false);
			dialog.setOnDismissListener(this);
			dialog.show();
		}catch(Exception ex){
			//System.out.println("Logged out without message. Error : "+ex.getMessage());
			setMessage(bundle.getString("message"));
			System.exit(0);
		}else if(code==8){
			//Chat message incoming
			if(Shared.userpref == null) Shared.userpref = context.getSharedPreferences("userpref", Context.MODE_PRIVATE);
			if(Shared.id==0) Shared.id = Shared.userpref.getInt("id", 0);
			if(Shared.loginkey==null) Shared.loginkey = Shared.userpref.getString("loginkey", null);
			if(Shared.id==0||Shared.loginkey==null) return false;
			try{
				JSONObject data = new JSONObject(bundle.getString("post"));
				if(sql==null) sql = new SQLiteOpenHelper(context, "yousendit", null, 1) {
						
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
				int sender = data.getInt("sender");
				Cursor cursor = null;
				cursor = sql.getReadableDatabase().query("counter", null, "user = "+Shared.id+" and whom = "+sender, null, null, null, null,null);
				String query = (cursor==null||cursor.getCount()==0)?"insert into counter values ("+Shared.id+","+sender+",1);":"update counter set count = count + 1 where user = "+Shared.id+" and whom = "+sender+";";
				sql.getWritableDatabase().execSQL(query);
				int sum = 0;
				if(cursor!=null){
					cursor.close();
					cursor = sql.getReadableDatabase().query("counter", new String[]{"sum(count) as sum"}, null, null, null, null,null);
					cursor.moveToFirst();
					sum = cursor.getInt(0);
					cursor.close();
				}
				setMessage(sum>1?"You have "+sum+" unread messages":bundle.getString("message"));
			}catch(JSONException ex){}
		}
		return true;
	}

	@Override public void onBuild(Bundle bundle, Builder build) {
	}

	@Override public void onNotify(Bundle bundle, Notification notification) {
	}

	@Override public void onDismiss(DialogInterface dialog) {
		System.exit(0);
	}
	
    public boolean isForeground(Context mContext) {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(mContext.getPackageName())) return false;
        }
        return true;
    }
}