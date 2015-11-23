package my.helper;

import java.io.File;

import com.squareup.picasso.Picasso;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class ImageViewer extends Activity{
	
	static Bitmap bmp;
	static String path;
	ImageView image;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		image = new ImageView(this);
		image.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		getActionBar().setDisplayHomeAsUpEnabled(true);
		if(path!=null){
			System.out.println("ImageViewer open path : "+path);
			if(Patterns.WEB_URL.matcher(path).matches()) Picasso.with(this).load(path).into(image);
			else if(new File(path)!=null) Picasso.with(this).load(new File(path)).into(image);
			else if(Uri.parse(path)!=null) Picasso.with(this).load(Uri.parse(path)).into(image);
		}else{
			if(bmp==null){
				String path = getIntent().getStringExtra("path");
				if(path!=null) bmp = BitmapFactory.decodeFile(path);
			}
			if(bmp==null){
				byte[] bytes = getIntent().getByteArrayExtra("bytes");
				if(bytes!=null) bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			}
			if(bmp!=null) image.setImageBitmap(bmp);
		}
		setContentView(image);
	}
	
	@Override protected void onDestroy() {
		super.onDestroy();
		image.destroyDrawingCache();
		//((BitmapDrawable) image.getDrawable()).getBitmap().recycle();
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) finish();
		return super.onOptionsItemSelected(item);
	}
}