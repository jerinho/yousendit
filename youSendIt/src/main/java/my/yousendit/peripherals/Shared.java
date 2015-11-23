package my.yousendit.peripherals;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public class Shared{
	
	//Singleton class to share data among activities
	
	public static String url = "http://api.yousendit.com.my/index.php/"; //AVD : http://10.0.2.2/, Genymotion : http://10.0.3.2/
	public static String loginkey = "";
	public static String phpsessid = "";
	public static String regexemail = "^[A-Za-z_]([A-Za-z\\d_]+\\.?)*[A-Za-z\\d_]+@([A-Za-z\\d_]+\\.?)+\\.[A-Za-z\\d_]+$";
	public static String regexname = "^[A-Za-z\\s]{6,}$";
	public static String regexpassword = "^[A-Za-z0-9]{6,}$";
	public static String email;
	public static String projectkey = "298122460897";
	public static String gcmid;
	public static String android;
	public static JSONObject listitems;
	public static SharedPreferences userpref, global;
	public static SharedPreferences.Editor editor;
	public static int id;
	public static Details details; //current job details
	public static String[] months = new String[]{"January","February","March","April","May","Jun","July","August","September","October","November","December"};
	public static boolean istransporter, haspayment;
	public static SimpleDateFormat sqldate = new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat sqltime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static SimpleDateFormat userdate = new SimpleDateFormat("EEE, dd MMM yyyy");
	public static SimpleDateFormat usertime = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm a");
	public static SimpleDateFormat progresstime = new SimpleDateFormat("EEE, dd MMM hh:mm a");
	public static SimpleDateFormat monthyear = new SimpleDateFormat("MMM yyyy");
	public static SimpleDateFormat shortdate = new SimpleDateFormat("MMM dd, yy");
	public static SimpleDateFormat longdate = new SimpleDateFormat("MMM dd, yyyy");
	public static SimpleDateFormat monthdate = new SimpleDateFormat("MMM dd");
	public static SimpleDateFormat timeonly = new SimpleDateFormat("hh:mm a");
	public static SimpleDateFormat datetime = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
	public static boolean isSamsung = Build.MANUFACTURER.equalsIgnoreCase("SAMSUNG");
	public static String[] countries, countriesbycode;
	public static HashMap<String,String> cctypes = new HashMap<String, String>();
	public static String image, name;
	public static Context screen;
	
	static{
		cctypes.put("visa","cciconvisa");
		cctypes.put("americanexpress","cciconamex");
		cctypes.put("discover","ccicondiscover");
		cctypes.put("mastercard","cciconmastercard");
		cctypes.put("jcb","cciconjcb");
	    String items = 
	    	"{" +
	    	"location : ['PJ','USJ','Damansara','Shah Alam','Subang','Ampang','Cheras','Kajang','Gombak','Klang','Puchong','Bangsar']," +
	    	"type : ['Parcel','Document','Food','Vehicle']," +
	    	"size : ['Tiny','Small','Medium','Big']," +
	    	"urgency : ['Very urgent', 'I can wait', 'Take your time']," +
	    	"progress : ['Pick up','Delivered','Delay','Re-scheduled']," +
	    	"log : ['Pick up','Pick up verified','Pick up not verified','Delayed','Delivered','Re-scheduled']" +
	    	"}";
		try{listitems = new JSONObject(items);}catch(JSONException ex){
			//System.out.println(ex.getMessage());
		}
	}
}