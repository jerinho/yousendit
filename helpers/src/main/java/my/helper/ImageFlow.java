package my.helper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONException;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.webkit.MimeTypeMap;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnClickListener;

public class ImageFlow extends HorizontalScrollView implements OnCreateContextMenuListener, OnMenuItemClickListener, OnLongClickListener, OnClickListener{
	
	ArrayList<String> srcs = new ArrayList<String>();
	LinearLayout layout;
	View selected;
	boolean userinsert = true, userremove = true;
	ImageButton insert;
	Activity context;
	public static int requestcode = 333;
	ImageFlowTransform transform = new ImageFlowTransform();
	HashMap<String, byte[]> imagesmap = new HashMap<String, byte[]>();
	String path;
	Uri imguri;
	ImageView imgview;
	int imgsrc; //1 : uri, 2 : file path, 3 : url, 4 : bitmap
	
	public ImageFlow(Context context) {
		this(context,null);
	}

	public ImageFlow(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = (Activity) context;
		setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1));
		setVerticalScrollBarEnabled(false);
		setHorizontalScrollBarEnabled(false);
		//setMinimumHeight(300);
		layout = new LinearLayout(context);
		//layout.setMinimumHeight(300);
		LinearLayout.LayoutParams pars = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		layout.setLayoutParams(pars);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		addView(layout);
		insert = new ImageButton(context);
		//insert.setImageDrawable(getResources().getDrawable(my.helper.R.drawable.button_addpic));
		insert.setBackground(getResources().getDrawable(my.helper.R.drawable.button_addpic));
		insert.setOnClickListener(this);
		pars = new LinearLayout.LayoutParams(400,400);
		pars.gravity = Gravity.CENTER;
		insert.setLayoutParams(pars);
		//LinearLayout wrap = new LinearLayout(context);
		//wrap.setLayoutParams(new LinearLayout.LayoutParams(400,400));
		//wrap.addView(insert);
		//wrap.setGravity(Gravity.CENTER);
		layout.addView(insert);
		setOnCreateContextMenuListener(this);
	}
	public void clear(){
		while(layout.getChildCount()!=0&&layout.getChildAt(0)!=insert) layout.removeViewAt(0);
		srcs.clear();
	}
	public void editModeOn(boolean bool){
		userinsert = userremove = bool;
		if(!bool) layout.removeView(insert);
		else layout.addView(insert);
	}
	public void setImagesFromPath(String[] path){
		clear();
	}
	public int countImages(){
		return srcs.size();
	}
	public String[] getSources(){
		if(srcs.size()==0) return null;
		String[] strs = new String[srcs.size()];
		return srcs.toArray(strs);
	}
	public HashMap<String, byte[]> getContents(){
		return imagesmap;
	}
	public ImageView getImage(int i){
		if(i==layout.getChildCount()-1&&userinsert) return null;
		return (ImageView) layout.getChildAt(i);
	}
	public Bitmap getBitmap(int i){
		return ((BitmapDrawable)((ImageView)layout.getChildAt(i)).getDrawable()).getBitmap();
	}
	@Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if(selected==insert){
			if(!userinsert) return;
			menu.add("Take new image using camera").setOnMenuItemClickListener(this);
			menu.add("Insert existing image").setOnMenuItemClickListener(this);
		}else{
			if(!userremove) return;
			menu.add("Remove").setOnMenuItemClickListener(this);
		}
	}
	@Override public boolean onMenuItemClick(MenuItem item) {
		char ch = item.getTitle().charAt(0);
		if(selected==insert){
			if(!userinsert) return false;
			if(ch=='T') addImageFromCamera();
			else if(ch=='I') addImageFromGallery();
		}else{
			if(ch=='R'){
				if(!userremove) return false;
				int pos = layout.indexOfChild(selected);
				imagesmap.remove(srcs.remove(pos));
				layout.removeViewAt(pos);
			}
		}
		return false;
	}
	@Override public boolean onLongClick(View clicked) {
		if(!userremove) return false;
		selected = clicked;
		selected.showContextMenu();
		return false;
	}
	@Override public void onClick(View clicked) {
		selected = clicked;
		if(selected==insert) selected.showContextMenu();
		else{
	    	Intent intent = new Intent(context,ImageViewer.class);
	    	ImageViewer.path = (String) selected.getTag();
	    	//ImageViewer.bmp = ((BitmapDrawable)((ImageView) selected).getDrawable()).getBitmap();
	    	//intent.putExtra("bitmap", bmp); //Pass bitmap data as extra may cause !!! FAILED BINDER TRANSACTION !!! error
	    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	    	context.startActivity(intent);
		}
	}
	public void addImageFromGallery(){
		if(!userinsert) return;
		Intent intent = new Intent();
		intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_PICK);
		if(!Build.MANUFACTURER.equalsIgnoreCase("SAMSUNG")) 
			intent.putExtra(MediaStore.EXTRA_OUTPUT, context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues()));
		context.startActivityForResult(intent,requestcode);
	}
	public void addImageFromCamera(){
		if(!userinsert) return;
		//<uses-feature android:name="android.hardware.Camera"/>
		//<uses-permission android:name="android.permission.CAMERA"/>
		//Don't get the uri data from camera result because some camera activity will return null, instead pass uri to EXTRA_OUTPUT key to let the camera store the image
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if(!Build.MANUFACTURER.equalsIgnoreCase("SAMSUNG")){
			ContentValues values = new ContentValues();
			values.put(MediaStore.Images.Media.TITLE, "itemimage.jpg");
			values.put(MediaStore.Images.Media.DESCRIPTION,"Image capture by camera");
			imguri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,imguri);
		}
        context.startActivityForResult(intent,requestcode);
	}
	public ImageView addImageView(){
		ImageView img = new ImageView(context){
			
			protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
				fullScroll(View.FOCUS_RIGHT);
			};
		};
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		img.setMinimumWidth(300);
		img.setMinimumHeight(300);
		img.setLayoutParams(lp);
		img.setPadding(5,0,5,0);
		img.setAdjustViewBounds(true);
		img.setLongClickable(true);
		img.setOnLongClickListener(this);
		img.setOnClickListener(this);
    	layout.addView(img,layout.getChildCount()-1);
    	return img;
	}
	
	public void putImageFromUri(final Uri uri){
		ImageView iv = addImageView();
		String path = uri.toString();
		iv.setTag(path);
		imgsrc = 1;
		this.path = path;
    	srcs.add(path);
    	Picasso.with(context).load(uri).resize(500,500).centerInside().transform(transform).into(iv);
    	imgview = iv;
    	System.out.println("Load image from : "+path);
	}
	public void putImageFromFile(final String path){
		ImageView iv = addImageView();
		iv.setTag(path);
		imgsrc = 2;
		this.path = path;
    	srcs.add(path);
    	Picasso.with(context).load(new File(path)).resize(500,500).centerInside().transform(transform).into(iv);
    	imgview = iv;
    	System.out.println("Load image from : "+path);
	}
	public void putImageFromUrl(final String path){
		ImageView iv = addImageView();
		iv.setTag(path);
		imgsrc = 3;
		this.path = path;
    	srcs.add(path);
    	Picasso.with(context).load(path).resize(500,500).centerInside().transform(transform).into(iv);
    	imgview = iv;
    	System.out.println("Load image from : "+path);
	}
	public void putImage(Bitmap bmp, String path){
	    //Bitmap.createScaledBitmap after ImageView initializing cause this warning : 
	    //Avoid object allocations during draw/layout operations (preallocate and reuse instead)
		Bitmap scaled = Bitmap.createScaledBitmap(bmp, layout.getHeight()*bmp.getWidth()/bmp.getHeight(), layout.getHeight(), true);
		imgview = addImageView();
		int ort = JrgAndroid.getExifOrientation(path);
		Bitmap rotated = JrgAndroid.rotateBitmap(scaled, ort);
		imgview.setImageBitmap(rotated);
		imgsrc = 4;
		this.path = path;
    	srcs.add(path);
	}
	public void putImage(byte[] data, String path){
		putImage(BitmapFactory.decodeByteArray(data, 0, data.length), path);
	}
	public void putImageRaw(String path){
		putImage(BitmapFactory.decodeFile(path), path);
	}
	public void putImage(Intent data){
		if(data==null){
			if(imguri!=null) putImageFromUri(imguri);
			else System.out.println("Activity result : Data is null");
			return;
		}
		Uri uri = data.getData();
		if(uri==null){
			System.out.println("Activity result : Data uri is null");
			Bundle bundle = data.getExtras();
			if(bundle==null||!bundle.containsKey("data")||bundle.get("data")==null){
				System.out.println("Activity result : No data source returned from the app");
		    	Toast.makeText(context, "No data source returned from the app", Toast.LENGTH_SHORT).show();
			}else{
				Bitmap bmp = (Bitmap) bundle.get("data");
				putImage(bmp,null);
			}
			return;
		}
		String path = JrgAndroid.getRealPathFromUri(context,uri);
		if(path==null){
			if(uri!=null&&srcs.indexOf(uri.toString())!=-1){
		    	System.out.println("Activity result : Image was already added. Path = "+uri.toString());
		    	Toast.makeText(context, "Image was already added", Toast.LENGTH_SHORT).show();
			}else{
				putImageFromUri(uri);
			}
		}else if(srcs.indexOf(path)!=-1){
	    	System.out.println("Activity result : Image was already added. Path = "+path);
	    	Toast.makeText(context, "Image was already added", Toast.LENGTH_SHORT).show();
	    }else{
		    String ext = MimeTypeMap.getFileExtensionFromUrl(path.replace(" ","_"));
		    String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
		    if(type==null||!type.startsWith("image/")){
				System.out.println("Activity result : File is not an image file");
				Toast.makeText(context, "File is not an image file", Toast.LENGTH_SHORT).show();
		    }
		    putImageFromFile(path);
	    }
	}
	
	public class ImageFlowTransform implements Transformation{
		
		public static final String KEY = "imageflowsmall";
		
		//1 : uri, 2 : file path, 3 : url, 4 : bitmap
		
		@Override public String key() {
			return KEY;
		}
		
		@Override public Bitmap transform(Bitmap bmp) {
			System.out.println("ImageFlow.Transformation. Bitmap size : "+bmp.getByteCount());
			if(bmp.isRecycled()) return null;
			if(imgsrc!=3) imagesmap.put(path,JrgAndroid.getBytes(bmp,300000));
			Bitmap cropped = JrgAndroid.squareCrop(bmp, 400, 400);
			bmp.recycle();
			return cropped;
		}

		/*
		@Override public Bitmap transform(Bitmap bmp) {
			if(bmp.isRecycled()) return null;
			try{
				if(imgsrc==3){
					System.out.println("Unmapped image loaded");
					Bitmap cropped = JrgAndroid.squareCrop(bmp, 400, 400);
					return cropped;
				}else{
					System.out.println("Mapped image loaded");
					imagesmap.put(path,JrgAndroid.getBytes(bmp,300000));
					Bitmap display = JrgAndroid.squareCrop(bmp, 400, 400), result;
					if(imguri==null||Build.MANUFACTURER.equalsIgnoreCase("SAMSUNG")) result = display;
					else{
						//Only if image captured using camera
						ExifInterface exif = new ExifInterface(imguri.getPath());
						int or = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
						result = JrgAndroid.rotateBitmap(display, or);
						if(!display.isRecycled()) display.recycle();
					}
					if(!bmp.isRecycled()) bmp.recycle();
					return result;
				}
			}catch(Exception ex){
				System.out.println("Activity result : "+ex.getMessage());
				Toast.makeText(context, "Out of memory. Please load smaller image", Toast.LENGTH_SHORT).show();
				((LinearLayout) imgview.getParent()).removeView(imgview);
				return null;
			}
		}
		*/
	}
}