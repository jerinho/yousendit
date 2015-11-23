package my.yousendit;

import my.helper.ApiRequest;
import my.helper.JrgAndroid;
import my.helper.ApiRequest.ApiHandler;
import my.helper.SimpleHttp;
import my.yousendit.peripherals.MyException;
import my.yousendit.peripherals.Shared;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class UserProfile extends Activity implements ApiHandler, OnClickListener, OnLayoutChangeListener, Transformation{
	
	TextView tvname, tvlocation, tvhistory, btnall;
	MyScroller scroller;
	LinearLayout latest;
	ListView list;
	ImageView profileimage;
	ArrayList<JSONObject> reviews = new ArrayList<JSONObject>();
	ReviewAdapter adapter;
	boolean allrev, fetched, busy;
	Bitmap bmp;
	int id;
	String image, userprofileimage = "userprofileimage";
	Transformation transform = new Transformation() {
		
		@Override public Bitmap transform(Bitmap bmp) {
			return bmp;
		}
		
		@Override public String key() {
			return userprofileimage;
		}
	};
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(Shared.id==0) System.exit(0);
		id = getIntent().getIntExtra("id",0);
		if(id==0) finish();
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		setContentView(R.layout.userprofile);
		setTitle("");
		profileimage = (ImageView) findViewById(R.id.publicimg);
		profileimage.addOnLayoutChangeListener(this);
		latest = (LinearLayout) findViewById(R.id.publiclatest);
		scroller = (MyScroller) findViewById(R.id.publicscroller);
		scroller.setImageView(profileimage);
		tvname = (TextView) findViewById(R.id.publicname);
		tvlocation = (TextView) findViewById(R.id.publicloc);
		tvhistory = (TextView) findViewById(R.id.publichistory);
		btnall = (TextView) findViewById(R.id.publicallrev);
		btnall.setOnClickListener(this);
		list = (ListView) findViewById(R.id.publicreviews);
		adapter = new ReviewAdapter(this,list,R.id.publicreviewmsg);
		list.setAdapter(adapter);
		list.setScrollContainer(false);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(android.R.color.transparent);
		HashMap<String,Object> pars = new HashMap<String, Object>();
		pars.put("loginkey",Shared.loginkey);
		pars.put("userId", id);
		SimpleHttp.request("details",Shared.url+"profile/userProfile",pars,this);
		busy = true;
	}
	
	@Override protected void onResume() {
		super.onResume();
		Shared.screen = this;
	}
	
	@Override public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
		scroller.setHeight(v.getHeight());
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) onBackPressed();
		return super.onOptionsItemSelected(item);
	}

	@Override public void onClick(View v) {
		if(busy) return;
		scroller.setVisibility(View.GONE);
		scroller.scrollTo(0, 0);
		scroller.color.setAlpha(255);
		getActionBar().setBackgroundDrawable(scroller.color);
		getActionBar().setTitle("Job Reviews");
		list.setVisibility(View.VISIBLE);
		if(!fetched){
			HashMap<String,Object> pars = new HashMap<String, Object>();
			pars.put("loginkey",Shared.loginkey);
			pars.put("userId", id);
			SimpleHttp.request("reviews",Shared.url+"profile/getReview",pars,this);
			busy = true;
		}
		allrev = true;
	}

	@Override public void onBackPressed() {
		if(!allrev) super.onBackPressed();
		else{
			scroller.setVisibility(View.VISIBLE);
			list.setVisibility(View.GONE);
			scroller.color.setAlpha(0);
			getActionBar().setBackgroundDrawable(scroller.color);
			getActionBar().setTitle("");
			allrev = false;
		}
	}
	
	public static class ReviewAdapter extends ArrayAdapter<JSONObject> implements Transformation{
		
		LayoutInflater inflater;
		ListView list;
		int total = 0;
		Context context;
		public static String key = "userprofilereview";
		
		public ReviewAdapter(Context context, ListView list, int textViewResourceId) {
			super(context, textViewResourceId);
			this.context = context;
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.list = list;
		}
		
		@Override public void clear() {
			super.clear();
			total = 0;
		}
		
		@Override public Bitmap transform(Bitmap bmp) {
			Bitmap cropped = JrgAndroid.circleCrop(bmp,100,100);
			bmp.recycle();
			return cropped;
		}
		
		@Override public View getView(int position, View reuse, ViewGroup parent) {
			View view = reuse!=null?reuse:inflater.inflate(R.layout.userprofileitem, null);
			try{
				JSONObject rev = getItem(position);
				Date on = null;
				if(!rev.isNull("timereview")) on = Shared.sqltime.parse(rev.getString("timereview"));
				((TextView) view.findViewById(R.id.publicreviewname)).setText(rev.getString("name")+(on!=null?"\non"+Shared.monthdate.format(on):""));
				TextView msg = (TextView) view.findViewById(R.id.publicreviewmsg);
				ImageView img = (ImageView) view.findViewById(R.id.publicreviewimg);
				msg.setVisibility(!rev.isNull("review")?View.VISIBLE:View.GONE);
				msg.setText(!rev.isNull("review")?rev.getString("review"):"");
				RatingBar ratebar = (RatingBar) view.findViewById(R.id.publicreviewrate);
				ratebar.setRating((float) rev.getInt("rate"));
				String image = rev.isNull("image")?null:rev.getString("image");
				if(image!=null) Picasso.with(getContext()).load(image).fit().centerInside().transform(this).into(img);
			}catch(JSONException ex){}catch(ParseException ex){}
			return view;
		}

		@Override public String key() {
			return key;
		}
	}
	
	public static class MyScroller extends ScrollView{
		
		ImageView imageview;
		ColorDrawable color;
		ActionBar bar;
		int height;
		
		public void setImageView(ImageView iv){
			imageview = iv;
			bar = ((Activity) getContext()).getActionBar();
			color = new ColorDrawable(getResources().getColor(R.color.theme));
		}
		
		public void setHeight(int h){
			//System.out.println("Set profile image height : "+h);
			height = h;
		}

		public MyScroller(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
		}
		
		public MyScroller(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		public MyScroller(Context context) {
			super(context);
		}
				
		@Override protected void onScrollChanged(int l, int t, int oldl, int oldt) {
			super.onScrollChanged(l, t, oldl, oldt);
			imageview.setTop(getScrollY()/2);
			imageview.getLayoutParams().height = height - getScrollY()/2;
			//color.setAlpha(255*getScrollY()/height);
			//bar.setBackgroundDrawable(color);
		}

		/*
		private float getAlphaForView(int position) {
		    int diff = 0;
		    float minAlpha = 0.4f, maxAlpha = 1.f;
		    float alpha = minAlpha; // min alpha
		    if (position > screenHeight) alpha = minAlpha;
		    else if (position + locationImageHeight < screenHeight) alpha = maxAlpha;
		    else {
		        diff = screenHeight - position;
		        alpha += ((diff * 1f) / locationImageHeight)* (maxAlpha - minAlpha);
		    }
		    return alpha;
		}
		*/
	}
	
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		busy = false;
		if(tag.equals("details")){
			if(result==null) finish();
			else try{
				JSONObject obj = new JSONObject(result);
				if(obj.getInt("success")==1){
					JSONObject data = obj.getJSONObject("post");
					tvname.setText(data.getString("name"));
					/*
					if(!data.isNull("image")) try{
						/*
						Bitmap bmp = BitmapFactory.decodeStream(new URL(data.getString("image")).openConnection().getInputStream());
					    Matrix matrix = new Matrix();
					    float scale = profileiv.getWidth()/bmp.getWidth();
					    matrix.postScale(scale, scale);
					    Bitmap scaledBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
					    profileiv.getLayoutParams().width = scaledBitmap.getWidth();
					    profileiv.getLayoutParams().height = scaledBitmap.getHeight();
						profileiv.setImageBitmap(scaledBitmap);
					}catch(Exception ex){}
					*/
					if(!data.isNull("image")){
						image = data.getString("image");
				    	Picasso.with(this).load(image).resize(500,500).centerInside().transform(transform).into(profileimage);
					}
					tvlocation.setVisibility(data.isNull("location")?View.GONE:View.VISIBLE);
					if(!data.isNull("location")){
						int loc = data.getInt("location");
						tvlocation.setText(Shared.countriesbycode[loc==0?0:loc-1]);
					}
					int count = (data.isNull("noOfJob")||data.getInt("noOfJob")==0)?0:data.getInt("noOfJob");
					if(count==0) tvhistory.setText("Has no completed job yet");
					else{
						Date since = Shared.sqltime.parse(data.getString("on"));
						tvhistory.setText("Has completed "+count+" jobs since "+Shared.monthyear.format(since));
					}
					if(!data.isNull("userReview")){
						JSONArray reviews = data.getJSONArray("userReview");
						latest.removeAllViews();
						for(int i=0; i<reviews.length(); i++){
							JSONObject rev = reviews.getJSONObject(i);
							LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.userprofileitem, null);
							Date on = null;
							if(!rev.isNull("timereview")) on = Shared.sqltime.parse(rev.getString("timereview"));
							((TextView) view.findViewById(R.id.publicreviewname)).setText(rev.getString("name")+(on!=null?"\non"+Shared.monthdate.format(on):""));
							TextView msg = (TextView) view.findViewById(R.id.publicreviewmsg);
							msg.setVisibility(!rev.isNull("review")?View.VISIBLE:View.GONE);
							msg.setText(!rev.isNull("review")?rev.getString("review"):"");
							RatingBar ratebar = (RatingBar) view.findViewById(R.id.publicreviewrate);
							ratebar.setRating((float) rev.getInt("rate"));
							ImageView profileimage = (ImageView) view.findViewById(R.id.publicreviewimg);
							String image = rev.isNull("image")?null:rev.getString("image");
							if(image!=null) Picasso.with(this).load(image).fit().centerInside().transform(this).into(profileimage);
							latest.addView(view);
						}
					}else{
						btnall.setVisibility(View.GONE);
						TextView text = new TextView(this);
						text.setText("Has no job reviews yet");
						text.setTextColor(Color.BLACK);
						latest.addView(text);
					}
				}else finish();
			}catch(JSONException jsex){
				finish();
				//System.out.println(jsex.toString());
			}catch(ParseException pex){
				finish();
				//System.out.println(pex.toString());
			}
		}else if(tag.equals("reviews")){
			if(result==null){
				Toast.makeText(this, "Unexpected error. Please retry later", Toast.LENGTH_SHORT).show();
				onBackPressed();
			}else try{
				JSONObject obj = new JSONObject(result);
				if(obj.getInt("success")==1){
					adapter.clear();
					JSONArray data = obj.getJSONArray("post");
					for(int i=0; i<data.length(); i++) adapter.add(data.getJSONObject(i));
					adapter.notifyDataSetChanged();
					fetched = true;
				}else{
					onBackPressed();
				}
			}catch(JSONException ex){
				onBackPressed();
			}
		}
	}
	
	private void draw(ImageView view, Bitmap bmp){
	    int width = bmp.getWidth();
	    int height = bmp.getHeight();
	    float scale = view.getWidth()/width;
	    Matrix matrix = new Matrix();
	    matrix.postScale(scale, scale);
	    Bitmap scaledBitmap = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
	    width = scaledBitmap.getWidth();
	    height = scaledBitmap.getHeight();
	    view.setImageDrawable(new BitmapDrawable(scaledBitmap));
	    android.view.ViewGroup.LayoutParams params = view.getLayoutParams();
	    params.width = width;
	    params.height = height;
	    view.setLayoutParams(params);
	}

	@Override public String key() {
		return ReviewAdapter.key;
	}

	@Override public Bitmap transform(Bitmap bmp) {
		Bitmap cropped = JrgAndroid.circleCrop(bmp,100,100);
		bmp.recycle();
		return cropped;
	}
}