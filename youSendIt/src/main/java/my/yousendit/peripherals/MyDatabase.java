package my.yousendit.peripherals;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabase extends SQLiteOpenHelper{
	
	public MyDatabase(Context context) {
		super(context, "yousendit", null, 2, null);
	}

	@Override public void onCreate(SQLiteDatabase db) {
	}
	
	@Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}