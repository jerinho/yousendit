package my.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.support.v4.view.ViewPager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Fragmentivity extends FragmentActivity implements OnPageChangeListener, TabListener{

	public FrameLayout layout;
	MyPager pager;
    MyPagerAdapter adapter;
    ArrayList<Fragment> fragments;
    ActionBar bar;
    static android.widget.LinearLayout.LayoutParams laypar = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    
	@Override protected void onCreate(Bundle args) {
    	super.onCreate(args);
        fragments = new ArrayList<Fragment>();
        adapter  = new MyPagerAdapter(getSupportFragmentManager(),fragments);
		layout = new FrameLayout(this);
		bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    	//bar.getThemedContext().setTheme(R.style.MyActionBarTabs);
		//bar.setBackgroundDrawable(new ColorDrawable(Color.GRAY));
		LayoutParams lp = new LayoutParams();
		lp.gravity = Gravity.TOP;
		lp.height = LayoutParams.MATCH_PARENT;
		lp.width = LayoutParams.MATCH_PARENT;
		pager = new MyPager(this);
        pager.setOnPageChangeListener(this);
        pager.setAdapter(adapter);
		layout.addView(pager);
		addContentView(layout,lp);
    }
	
	public PagerTitleStrip getPagerStrip(){
		return pager.strip;
	}
	
    public Fragment add(String tag, Fragment fragment, String title){
    	Bundle bundle = fragment.getArguments();
    	if(bundle==null) fragment.setArguments(bundle = new Bundle());
    	bundle.putString("title", title);
    	bundle.putString("tag", tag);
    	adapter.add(fragment);
    	adapter.notifyDataSetChanged();
    	bar.addTab(bar.newTab().setText(title).setTabListener(this));
    	//getSupportFragmentManager().beginTransaction().add(fragment,label).commit();
    	return fragment;
    }
    
    public void jump(int n, boolean b){
    	pager.setCurrentItem(n,b);
    }
    
    public Fragment addandjump(String tag, Fragment fragment, String title){
    	add(tag, fragment, title);
    	jump(adapter.getCount()-1, true);
    	return fragment;
    }
    
    public void jump(int n){
    	pager.setCurrentItem(n,true);
    }
    
    public Fragment addandjump(String tag, Fragment fragment, String title, boolean animate){
    	add(tag, fragment, title);
    	jump(adapter.getCount()-1, animate);
    	return fragment;
    }
    
	public Fragment remove(int i){
    	Fragment fragment = adapter.getItem(i);
    	adapter.remove(i);
    	adapter.notifyDataSetChanged();
    	bar.removeTabAt(i);
    	//getSupportFragmentManager().beginTransaction().remove(frag).commit();
    	return fragment;
    }
    
    public Fragment remove(String key){
    	int selected = 0;
    	for(int i=0; i<fragments.size(); i++) if(fragments.get(i).getArguments().getString("tag").equals(key)) selected = i;
    	return remove(selected);
    }
        
    public Fragment get(int i){
    	return adapter.getItem(i);
    }
    
    public Fragment getCurrent(){
    	return get(current());
    }
        
    public int current(){
    	return pager.getCurrentItem();
    }
    
    public void update(){
    	adapter.notifyDataSetChanged();
    }
    
    public void setSwipable(boolean bool){
    	pager.setSwipable(bool);
    }
    
    public void setStateBar(boolean bool){
    	pager.strip.setVisibility(bool?View.VISIBLE:View.GONE);
    }
    
    boolean showtitle, showhome, showcustom;
    
    public void setActionBar(boolean bool){
    	bar.setDisplayShowTitleEnabled(bool);
		bar.setDisplayShowHomeEnabled(bool);
		bar.setDisplayShowCustomEnabled(bool);
    }
        
    public void setTabBar(boolean bool){
    	bar.setNavigationMode(bool?ActionBar.NAVIGATION_MODE_TABS:ActionBar.NAVIGATION_MODE_STANDARD);
    }
    
    public void clear(){
    	adapter.clear();
    	adapter.notifyDataSetChanged();
    	pager.invalidate();
    }
    
    public static class MyPager extends ViewPager{
    	
    	boolean enabled;
    	PagerTitleStrip strip;
    	
    	public MyPager(FragmentActivity activity) {
    		super(activity);
    		setId(555);
        	strip = new PagerTitleStrip(activity);
        	float scale = getResources().getDisplayMetrics().density;
        	strip.setPadding(0, (int)(scale * 4 + 0.5f), 0,(int)(scale * 4 + 0.5f));
            LayoutParams par = new LayoutParams();
            par.gravity = Gravity.TOP;
            par.width = LayoutParams.MATCH_PARENT;
            par.height = LayoutParams.WRAP_CONTENT;
            addView(strip,par);
		}
    	
    	@Override public boolean onTouchEvent(MotionEvent event) {
        	return enabled&&super.onTouchEvent(event);
        }
        @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        	return enabled&&super.onInterceptTouchEvent(event);
        }
        public void setSwipable(boolean bool){
        	enabled = bool;
        }
    }
	
    public static class MyPagerAdapter extends FragmentPagerAdapter {
        
        private List<Fragment> fragments;
        
        public MyPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            this.fragments = fragments;
        }
        
		public void add(Fragment frag){
			fragments.add(frag);
		}
		public void remove(int i){
			fragments.remove(i);
		}
		public void clear(){
			fragments.clear();
		}
		
        @Override public Fragment getItem(int position) {
            return fragments.get(position);
        }
     
        @Override public int getCount() {
            return fragments.size();
        }
        @Override public CharSequence getPageTitle(int position) {
        	return getItem(position).getArguments().getString("title");
        }
    }

	@Override public void onPageScrollStateChanged(int arg0) {
	}

	@Override public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override public void onPageSelected(int pos) {
		if(bar.getNavigationMode()!=ActionBar.NAVIGATION_MODE_TABS) return;
		bar.setSelectedNavigationItem(pos);
	}
	
	@Override public void onTabSelected(Tab tab, FragmentTransaction ft) {
		pager.setCurrentItem(tab.getPosition(), true);
		//((TextView) tab.getCustomView().findViewById(android.R.id.title)).setTextColor(Color.GRAY);
	}

	@Override public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

	@Override public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}
}