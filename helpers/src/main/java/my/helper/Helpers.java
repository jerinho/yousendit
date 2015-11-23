package my.helper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TabHost;
import android.view.animation.Transformation;

public class Helpers{
	
	public static ViewPager createViewPager(FragmentActivity activity, TabHost tabhost){
		LinearLayout layout = null;
		ViewPager pager = new ViewPager(activity);
		int count = tabhost.getTabWidget().getChildCount();
		ViewPagerAdapter adapter = new ViewPagerAdapter(activity.getSupportFragmentManager(),tabhost);
		pager.setAdapter(adapter);
		for(int i=0; i<count; i++) layout = (LinearLayout) tabhost.getTabWidget().getChildAt(0);
		return pager;
	}
	
	public static void expand(final View v) {
	    v.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	    final int targetHeight = v.getMeasuredHeight();
	    v.getLayoutParams().height = 0;
	    v.setVisibility(View.VISIBLE);
	    Animation a = new Animation(){
	        @Override protected void applyTransformation(float interpolatedTime, Transformation t) {
	            v.getLayoutParams().height = (int) (interpolatedTime == 1?LayoutParams.WRAP_CONTENT:(targetHeight * interpolatedTime));
	            v.requestLayout();
	        }
	        @Override public boolean willChangeBounds() {
	            return true;
	        }
	    };
	    a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));
	    v.startAnimation(a);
	}

	public static void collapse(final View v) {
	    final int initialHeight = v.getMeasuredHeight();
	    Animation a = new Animation(){
	        @Override protected void applyTransformation(float interpolatedTime, Transformation t) {
	            if(interpolatedTime == 1){
	                v.setVisibility(View.GONE);
	            }else{
	                v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
	                v.requestLayout();
	            }
	        }

	        @Override public boolean willChangeBounds() {
	            return true;
	        }
	    };
	    a.setDuration((int)(initialHeight / v.getContext().getResources().getDisplayMetrics().density));
	    v.startAnimation(a);
	}
	
	public static Bitmap getRoundedShape(Bitmap scaleBitmapImage, int diam) {
		  Bitmap targetBitmap = Bitmap.createBitmap(diam, diam,Bitmap.Config.ARGB_8888);
		  Canvas canvas = new Canvas(targetBitmap);
		  Path path = new Path();
		  path.addCircle(((float) diam - 1) / 2,
		  ((float) diam - 1) / 2,
		  (Math.min(((float) diam), ((float) diam)) / 2), Path.Direction.CCW);
		  canvas.clipPath(path);
		  Bitmap bmp = scaleBitmapImage;
		  canvas.drawBitmap(bmp, new Rect(0, 0, bmp.getWidth(), bmp.getHeight()), new Rect(0, 0, diam, diam), null);
		  return targetBitmap;
	}
	
	public static class ViewPagerAdapter extends FragmentPagerAdapter{
		
		TabHost tabhost;
		
		public ViewPagerAdapter(android.support.v4.app.FragmentManager fragman, TabHost tabhost) {
			super(fragman);
			this.tabhost = tabhost;
		}

		@Override public int getCount() {
			return 0;
		}

		@Override public Fragment getItem(int pos) {
			return null;
		}
	}
}

/*
fth = (TabHost) findViewById(android.R.id.tabhost);
fth.setup(this, getSupportFragmentManager(), android.R.id.tabcontent); //FragmentTabHost.setup(Context,FragmentManager,Container Id (FrameLayout))
FragmentManager fm = getSupportFragmentManager();
FragmentTransaction ft = fm.beginTransaction();
Login fraglogin = new Login();
Register fragregis = new Register();
ft.add(R.id.framelogin, fraglogin, "fraglogin");
ft.add(R.id.frameregis, fragregis, "fragregis");
ft.addToBackStack(null);
ft.commit();
*/

//fth = (FragmentTabHost) findViewById(android.R.id.tabhost);
//fth.setup(this, getSupportFragmentManager(), android.R.id.tabcontent); //FragmentTabHost.setup(Context,FragmentManager,Container Id (FrameLayout))
//fth.addTab(fth.newTabSpec("taglogin").setIndicator("Login"));
//fth.addTab(fth.newTabSpec("tagregister").setIndicator("Not yet a member?"));
//List<Fragment> frags = getSupportFragmentManager().getFragments();
//System.out.println("No of fragmnets : "+frags.size());

//fth.addTab(fth.newTabSpec("tagreset").setIndicator("Forget Password?", null),Reset.class, null);
//fth.addTab(fth.newTabSpec("tagactivation").setIndicator("Activation", null),Activation.class, null);

//Reset fragreset = new Reset();
//Activation fragactiv = new Activation();
//getSupportFragmentManager().beginTransaction().add(R.id.frmlogin, fraglogin, "fraglogin").commit();
//getSupportFragmentManager().beginTransaction().add(R.id.frmregis, fragreg, "fragregis").commit();

/*
ActionBar.Tab tablogin, tabregis, tabreset, tabactiv;
getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragreset, "tabsfragment").commit();
getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragactiv, "tabsfragment").commit();

ActionBar bar = getActionBar();
bar.setDisplayShowHomeEnabled(false);
bar.setDisplayShowTitleEnabled(false);
bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
tablogin = bar.newTab().setText("Login");
tabregis = bar.newTab().setText("Register");
tablogin.setTabListener(this);
bar.addTab(tablogin);
bar.addTab(tabregis);
*/