package my.yousendit;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import my.helper.ApiRequest.ApiHandler;
import my.helper.Fragmentivity;
import my.helper.Notifier;
import my.helper.SimpleHttp;
import my.yousendit.fragments.MyJob;
import my.yousendit.peripherals.DrawerActivity;
import my.yousendit.peripherals.MyException;
import my.yousendit.peripherals.Shared;

public class FragsJobs extends Fragmentivity{
	
    LayoutInflater inflater;
    private SlidingMenu menu;
	private LinearLayout menuview;
    private LinearLayout.LayoutParams fullsize = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
    private ImageView imageview;
    private TextView tvname, tvemail;
    private Transformation transform = new DrawerActivity.DrawerTransform();
    private TableLayout table;
    SimpleHttp request;
    ApiHandler apihandler = new ApiHandler() {
		
    	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
    		if(!tag.equals("logout")) return;
    		if(result!=null) try{
    			JSONObject json = new JSONObject(result);
    			if(json.getInt("success")==1||json.getString("code").equals("loginkey_not_exist")){
    		    	Shared.editor.remove("loginkey").apply();
    		    	Shared.editor.remove("istransporter").apply();
    		    	Shared.loginkey = null;
    		    	Shared.istransporter = false;
    		    	Shared.global.edit().remove("key").apply();
    				Intent intent = new Intent(FragsJobs.this, FragsLogin.class);
    			    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    			    startActivity(intent);
    			    finish();
    			}
    		}catch(JSONException ex){
    			//System.out.println("JSON Exception : "+ex.getMessage());
    		}else{
        		unfocus(DrawerActivity.current);
        		focus(DrawerActivity.current = 0);
    			Toast.makeText(FragsJobs.this, "Fail to log you out. Please try again later", Toast.LENGTH_SHORT);
    		}
    	}
	};
	
	protected void onCreate(android.os.Bundle args) {
		super.onCreate(args);
		if(Shared.id==0) System.exit(0);
        inflater = getLayoutInflater();
        setTitle("My Jobs");
		MyJob posted = new MyJob();
        posted.setAsSender(true);
        posted.setIsActiveJobs(true);
		MyJob awarded = new MyJob();
        awarded.setAsSender(false);
        awarded.setIsActiveJobs(true);
		setSwipable(false);
		setActionBar(true);
		setTabBar(true);
		setStateBar(false);
		add("posted",posted,"Posted Jobs");
		add("awarded",awarded,"Awarded Jobs");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(android.R.color.transparent);
        menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        menu.setShadowDrawable(R.drawable.drawershadow);
        menu.setShadowWidth(2);
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        menu.setBehindWidth((int) (metric.widthPixels*0.8));
        menu.setMenu(setMenuView());
        menu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);
	}
    @Override public boolean onOptionsItemSelected(MenuItem item) {
    	if(item.getItemId()==android.R.id.home&&menu.isShown()) menu.toggle(true);
    	return true;
    }
	@Override protected void onResume() {
		super.onResume();
		Notifier.removeHandler();
		Shared.screen = this;
	}
    @Override protected void onPause() {
    	super.onPause();
    	if(request!=null) request.abort();
    }
	public View setMenuView(){
		menuview = (LinearLayout) inflater.inflate(R.layout.sidemenu,null);
		table = (TableLayout) menuview.findViewById(R.id.drawertable);
		imageview = (ImageView) menuview.findViewById(R.id.drawerimage);
		tvname = (TextView) menuview.findViewById(R.id.drawername);
		tvemail = (TextView) menuview.findViewById(R.id.draweremail);
		Picasso.with(this).load(Shared.image).fit().centerInside().transform(transform).into(imageview);
		tvemail.setText(Shared.email);
		tvname.setText(Shared.name);
		TableRow row = (TableRow) table.getChildAt(DrawerActivity.current);
		((ImageView) row.getChildAt(0)).setImageDrawable(getResources().getDrawable(DrawerActivity.focuses[DrawerActivity.current]));
		((TextView) row.getChildAt(1)).setTextColor(getResources().getColor(R.color.theme));
		return menuview;
	}
	
	public void setEnabled(boolean b){
		menu.setEnabled(b);
		menu.setSlidingEnabled(b);
		menu.setSelectorDrawable(b?R.drawable.selectordrawer:R.drawable.selectorup);
	}
	    
	public void focus(int select){
		TableRow row = (TableRow) table.getChildAt(select);
		((ImageView) row.getChildAt(0)).setImageDrawable(getResources().getDrawable(DrawerActivity.focuses[select]));
		if(DrawerActivity.primcolor==null||DrawerActivity.themecolor==null){
			DrawerActivity.primcolor = ((TextView) row.getChildAt(1)).getTextColors();
			DrawerActivity.themecolor = ColorStateList.valueOf(getResources().getColor(R.color.theme));
		}
		((TextView) row.getChildAt(1)).setTextColor(DrawerActivity.themecolor);
	}
	
	public void unfocus(int select){
		TableRow row = (TableRow) table.getChildAt(select);
		((ImageView) row.getChildAt(0)).setImageDrawable(getResources().getDrawable(DrawerActivity.unfocuses[select]));
		((TextView) row.getChildAt(1)).setTextColor(DrawerActivity.primcolor);
	}
	
    public void click(View view){
    	click(table.indexOfChild((View) view.getParent()));
    }
    
	public void click(int position){
		if(DrawerActivity.navitem[position].equals("Logout")){
		    unfocus(DrawerActivity.current);
			focus(DrawerActivity.current = position);
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("loginkey",Shared.loginkey);
			SimpleHttp.request("logout", Shared.url+"auth/logout", params, apihandler);
		}else if(position<DrawerActivity.classes.length&&position!=DrawerActivity.current){
		    unfocus(DrawerActivity.current);
			focus(DrawerActivity.current = position);
			Intent intent = new Intent(this, DrawerActivity.classes[DrawerActivity.current]);
		    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    startActivity(intent);
		    finish();
		}else menu.toggle();
	}
}