package my.yousendit.peripherals;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import my.helper.JrgAndroid;
import my.helper.Notifier;
import my.helper.ApiRequest.ApiHandler;
import my.helper.SimpleHttp;
import my.yousendit.Chat;
import my.yousendit.FragsJobs;
import my.yousendit.FragsLogin;
import my.yousendit.Home;
import my.yousendit.MyProfile;
import my.yousendit.Notifications;
import my.yousendit.R;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public abstract class DrawerActivity extends FragmentActivity{
//public abstract class DrawerActivity extends FragmentActivity implements DrawerListener{
	
    LayoutInflater inflater;
    protected SlidingMenu menu;
    protected View main;
    protected boolean drawed, isActive;
    protected int mode;
    protected LinearLayout.LayoutParams fullsize = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
    protected TableLayout table;
    protected LinearLayout menuview;
    protected ImageView imageview;
    protected TextView tvname, tvemail;
    protected Transformation transform = new DrawerTransform();
    protected SimpleHttp request;
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
    				Intent intent = new Intent(DrawerActivity.this, FragsLogin.class);
    			    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    			    startActivity(intent);
    			    finish();
    			}
    		}catch(JSONException ex){
    			//System.out.println("JSON Exception : "+ex.getMessage());
    		}else{
        		unfocus(current);
        		focus(current = 0);
    			Toast.makeText(DrawerActivity.this, "Fail to log you out. Please try again later", Toast.LENGTH_SHORT);
    		}
    	}
	};
    //private DrawerLayout drawer;
    //public ActionBarDrawerToggle toggle;
    //private ViewGroup decor;
    //private FrameLayout container;
	ArrayAdapter<String> adapter;
    //public static String[] navitem = new String[]{"Home","My Profile","Notifications","My Job","Job History","Account History","Payment","Chat","Ratings And Reviews","Logout"};
    //public static Class[] classes = new Class[]{Home.class,MyProfile.class,Notifications.class,FragsJobs.class,FragsJobs.class,Account.class,Payment.class,Chat.class};
    public static String[] navitem = new String[]{"Home","My Job","Chat","Notifications","Profile","Logout"};
    public static Class[] classes = new Class[]{Home.class,FragsJobs.class,Chat.class,Notifications.class,MyProfile.class};
    public static int[] focuses = new int[]{
    	R.drawable.icon_sidenav_home_focus,
    	R.drawable.icon_sidenav_myjob_focus,
    	R.drawable.icon_sidenav_chat_focus,
    	R.drawable.icon_sidenav_notifications_focus,
    	R.drawable.icon_sidenav_profile_focus,
    	R.drawable.icon_sidenav_logout_focus
    };
    public static int[] unfocuses = new int[]{
    	R.drawable.icon_sidenav_home,
    	R.drawable.icon_sidenav_myjob,
    	R.drawable.icon_sidenav_chat,
    	R.drawable.icon_sidenav_notifications,
    	R.drawable.icon_sidenav_profile,
    	R.drawable.icon_sidenav_logout
    };
    public static int current;
    public static ColorStateList themecolor, primcolor;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        inflater = getLayoutInflater();
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(android.R.color.transparent);
        main = setMainView();
        setContentView(main, fullsize);
        menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        menu.setShadowDrawable(R.drawable.drawershadow);
        menu.setShadowWidth(2);
		menu.setSelectorDrawable(R.drawable.selectordrawer);
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        menu.setBehindWidth((int) (metric.widthPixels*0.7));
        menu.setMenu(setMenuView());
        menu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);
        /*
        drawer = setDrawer();
        if(drawer!=null){
	        drawer.setDrawerShadow(R.drawable.drawershadow, GravityCompat.START);
	        drawer.setDrawerListener(this);
            //decor = (ViewGroup) getWindow().getDecorView();
            //View child = decor.getChildAt(0);
            //decor.removeView(child);
            //container = new FrameLayout(this);
            //container.addView(child);
            //drawer.addView(container);
            //decor.addView(drawer);
	        addContentView(drawer, fullsize);
	        //LinearLayout wrapper = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.sidemenuprofile, null);
	        //ImageView vimg = (ImageView) wrapper.findViewById(R.id.drawerimage);
	        //TextView vtxt = (TextView) wrapper.findViewById(R.id.drawertext);
	        //DrawerList list = (DrawerList) drawer.getChildAt(1);
	        //drawer.removeView(list);
	        //wrapper.addView(list);
	        //drawer.addView(wrapper);
	        Picasso.with(this).load(Shared.image).transform(new Transformation() {
	        	
	        	public String KEY = "drawerprofile";
				
				@Override public Bitmap transform(Bitmap bmp) {
					return JrgAndroid.circleCrop(bmp,100,100);
				}
				
				@Override public String key() {
					return KEY;
				}
			}).into(vimg);
	        //vtxt.setText(Html.fromHtml("User name<br/><br/>Email address"));
	        toggle = new ActionBarDrawerToggle(this,drawer,true,R.drawable.selectordrawer,R.string.navigation_drawer_open,R.string.navigation_drawer_close);
        }
        addContentView(main, fullsize);
        if(main!=null) main.bringToFront();
        */
	}

	@Override protected void onResume() {
		super.onResume();
		Notifier.removeHandler();
        mode = getActionBar().getNavigationMode();
        Shared.screen = this;
        for(int i=0; i<classes.length; i++) if(getClass()==classes[i]) current = i;
        focus(current);
		isActive = true;
	}
	
	@Override protected void onPause() {
		super.onPause();
		isActive = false;
    	if(request!=null) request.abort();
	}
	
	public abstract View setMainView();
	
	public View setMenuView(){
		menuview = (LinearLayout) inflate(R.layout.sidemenu);
		table = (TableLayout) menuview.findViewById(R.id.drawertable);
		imageview = (ImageView) menuview.findViewById(R.id.drawerimage);
		tvname = (TextView) menuview.findViewById(R.id.drawername);
		tvemail = (TextView) menuview.findViewById(R.id.draweremail);
		if(Shared.image==null) Picasso.with(this).load(R.drawable.photo_profile_round).fit().centerInside().transform(transform).into(imageview);
		else Picasso.with(this).load(Shared.image).fit().centerInside().transform(transform).into(imageview);
		tvemail.setText(Shared.email);
		tvname.setText(Shared.name);
		//focus(current);
		return menuview;
	}

	public void focus(int select){
		TableRow row = (TableRow) table.getChildAt(select);
		((ImageView) row.getChildAt(0)).setImageDrawable(getResources().getDrawable(focuses[select]));
		if(primcolor==null||themecolor==null){
			primcolor = ((TextView) row.getChildAt(1)).getTextColors();
			themecolor = ColorStateList.valueOf(getResources().getColor(R.color.theme));
		}
		((TextView) row.getChildAt(1)).setTextColor(themecolor);
	}
	
	public void unfocus(int select){
		TableRow row = (TableRow) table.getChildAt(select);
		((ImageView) row.getChildAt(0)).setImageDrawable(getResources().getDrawable(unfocuses[select]));
		((TextView) row.getChildAt(1)).setTextColor(primcolor);
	}
	
	public View mainView(){
		return main;
	}
	
	public void setEnabled(boolean b){
		menu.setEnabled(b);
		menu.setSlidingEnabled(b);
		menu.setSelectorEnabled(b);
		//menu.setSelectorDrawable(b?R.drawable.selectordrawer:R.drawable.selectorup);
		menu.invalidate();
		//menu.setSelectorDrawable doesn't working to switch the icon, so need to use below codes
		//getActionBar().setIcon(b?android.R.color.transparent:R.drawable.selectorup);
		//getActionBar().setIcon(android.R.color.transparent);
		//getActionBar().setDisplayHomeAsUpEnabled(b);
	}
	
	public View inflate(int id){
		return inflater.inflate(id, null);
	}
	
    @Override public boolean onOptionsItemSelected(MenuItem item) {
    	if(item.getItemId()==android.R.id.home&&menu.isShown()) menu.toggle(true);
    	return true;
    }
    
    public void click(View view){
    	click(table.indexOfChild((View) view.getParent()));
    }
    
	public void click(int position){
		if(navitem[position].equals("Logout")){
		    unfocus(current);
			focus(current = position);
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("loginkey",Shared.loginkey);
			//ApiRequest.request("logout", Shared.url+"auth/logout", params, apihandler);
			SimpleHttp.request("logout", Shared.url+"auth/logout", params, apihandler);
		}else if(position<classes.length&&position!=current){
		    unfocus(current);
			focus(current = position);
			Intent intent = new Intent(this, classes[current]);
		    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    startActivity(intent);
		    finish();
		}else menu.toggle();
	}
		
	public static class DrawerTransform implements Transformation{
	
		@Override public Bitmap transform(Bitmap bmp) {
			Bitmap cropped = JrgAndroid.circleCrop(bmp,100,100);
			bmp.recycle();
			return cropped;
		}
		
		@Override public String key() {
			return "profileimage_"+Shared.id;
		}
	}
    	
	/*
	public DrawerLayout setDrawer() {
		return (DrawerLayout) inflate(R.layout.drawer);
	}
		
	public void setDrawerOn(boolean bool){
		toggle.setDrawerIndicatorEnabled(bool);
		drawer.setDrawerListener(bool?this:null);
        invalidateOptionsMenu();
	}
	
    @Override public boolean onOptionsItemSelected(MenuItem item) {
    	if(false) return true;
    	else if(drawer.isDrawerOpen(drawer)) drawer.closeDrawer(null);
    	else if(!drawer.isDrawerOpen(drawer)) drawer.openDrawer(null);
    	
    	//else if(!toggle.isDrawerIndicatorEnabled()) onBackPressed();
    	//else if(toggle.onOptionsItemSelected(item)) return true;
    	else if(item.getItemId() == R.id.action_settings) return true;
    	return true;
    }

    @Override protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toggle.onConfigurationChanged(newConfig);
    }

	@Override public void onDrawerClosed(View arg0) {
		invalidateOptionsMenu();
	}

	@Override public void onDrawerOpened(View arg0) {
		invalidateOptionsMenu();
	}
	
	@Override public void onDrawerSlide(View arg0, float dist) {
		if(dist==0f){
			main.bringToFront();
			drawed = false;
		}else if(!drawed){
			drawer.bringToFront();
			drawed = true;
		}
	}
	
	@Override public void onDrawerStateChanged(int arg0) {
	}
	*/
}