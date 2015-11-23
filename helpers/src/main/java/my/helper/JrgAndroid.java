package my.helper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.ListView;

public class JrgAndroid{
	
	public static Bitmap getBitmapFromFileAndRescale(File file, int maxw, int maxh){
		try{
	        Options sopt = new Options();
	        sopt.inJustDecodeBounds = true;
	        FileInputStream sfis = new FileInputStream(file);
	        BitmapFactory.decodeStream(sfis,null,sopt);
	        sfis.close();
	        Options opt = new Options();
	        opt.inSampleSize = (int) Math.ceil(Math.max((float)sopt.outWidth/(float)maxw, (float)sopt.outHeight/(float)maxh));
	        if(opt.inSampleSize<=0) return null;
	        FileInputStream fis = new FileInputStream(file);
	        Bitmap bmp = BitmapFactory.decodeStream(fis, null, opt);
	        fis.close();
	        return bmp;
		}catch(IOException e){
			return null;
		}
	}
	public static Bitmap getBitmapFromBytesAndRescale(byte[] bytes, int maxw, int maxh){
        Options sopt = new Options();
        sopt.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, sopt);
        Options opt = new Options();
        opt.inSampleSize = (int) Math.ceil(Math.max((float)sopt.outWidth/(float)maxw, (float)sopt.outHeight/(float)maxh));
        if(opt.inSampleSize<=0) return null;
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);
        return bmp;
	}
    public static long getBitmapSize(Bitmap bitmap) {
        return bitmap==null?0:bitmap.getRowBytes() * bitmap.getHeight();
    }
	public static boolean isNetworked(Context ctx){
		//perlukan kebenaran ACCESS_NETWORK_STATE pada manifes dengan tag ditempatkan sebelum kebenaran INTERNET (bug)
		return ((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo()!=null;
	}
	public static <T> ArrayList<T> filter(ArrayList<T> items, SparseBooleanArray sba){
		ArrayList<T> ret = new ArrayList<T>();
		for(int i=0; i<items.size(); i++) if(sba.get(i)) ret.add(items.get(i));
		return ret;
	}
	public static ArrayList<Integer> getIndexes(int count, SparseBooleanArray sba){
		ArrayList<Integer> checked = new ArrayList<Integer>();
		for(int i=0; i<count; i++) if(sba.get(i)) checked.add(i);
		return checked;
	}
	public static ArrayList<Integer> getIndexes(ListView listview){
		return getIndexes(listview.getAdapter().getCount(),listview.getCheckedItemPositions());
	}
	public static SparseBooleanArray toSparseBools(ArrayList<Integer> list) {
		SparseBooleanArray sba = new SparseBooleanArray();
		for(int row : list) sba.put(row, true);
		return sba;
	}
	public static ArrayList<Integer> getPosInt(ArrayList<Integer> list, SparseBooleanArray sba){
		ArrayList<Integer> ret = new ArrayList<Integer>();
		for(int i=0; i<list.size(); i++) if(sba.get(list.get(i))) ret.add(i);
		return ret;
	}
	public static String getPathOfFile(String filename){
		return Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+filename;
	}
	public static void setImage(final ImageView image, final byte[] bytes){
		image.post(new Runnable() {@Override public void run() {
			image.setImageBitmap(JrgAndroid.getBitmapFromBytesAndRescale(bytes, image.getWidth(), image.getHeight()));
			image.invalidate();
		}});
	}
	public static void setImage(final ImageView image, final Bitmap bmp){
		/*
		image.post(new Runnable() {@Override public void run() {
			image.setImageBitmap(Bitmap.createScaledBitmap(bmp, image.getWidth(), image.getHeight(), true));
			image.invalidate();
		}});
		*/
		image.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			
			@Override public boolean onPreDraw() {
				image.setImageBitmap(Bitmap.createScaledBitmap(bmp, image.getWidth(), image.getHeight(), true));
				image.invalidate();
				return true;
			}
		});
	}
	public static void setImage(Activity activity, final ImageView image, final byte[] bytes){
		activity.runOnUiThread(new Runnable() {@Override public void run() {
			image.setImageBitmap(JrgAndroid.getBitmapFromBytesAndRescale(bytes, image.getWidth(), image.getHeight()));
			image.invalidate();
		}});
	}
	public static void setImage(Activity activity, final Object adapter, final ImageView image, final byte[] bytes){
		activity.runOnUiThread(new Runnable() {@Override public void run() {
			image.setImageBitmap(JrgAndroid.getBitmapFromBytesAndRescale(bytes, image.getWidth(), image.getHeight()));
			image.invalidate();
			if(adapter instanceof PagerAdapter) ((PagerAdapter) adapter).notifyDataSetChanged();
			else if(adapter instanceof BaseAdapter) ((BaseAdapter) adapter).notifyDataSetChanged();
			else if(adapter instanceof BaseExpandableListAdapter) ((BaseExpandableListAdapter) adapter).notifyDataSetChanged();
		}});
	}
    public static String getRealPathFromUri(Context context, Uri uri){
    	if(uri.getScheme().equalsIgnoreCase("file")) return uri.getPath();
    	//Should give problem for API 19 and above
    	try{
        	int sdk = android.os.Build.VERSION.SDK_INT;
        	String[] proj = {MediaStore.Images.Media.DATA};
        	String path = null;
    		Cursor cursor = null;
    		ContentResolver resolver = context.getContentResolver();
            //if(sdk>=19) cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,proj,"_id=?",DocumentsContract.getDocumentId(uri).split(":")[1],null);
            cursor = sdk<11?resolver.query(uri, proj, null, null, null):new CursorLoader(context,uri, proj, null, null, null).loadInBackground();
    		if(cursor!=null){
		    	if(cursor.moveToFirst()) path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
		    	cursor.close();
    		}
	    	return path;
	    }catch(Exception ex){
	    	return null;
	    }
    }
    public static Bitmap circleCrop(Bitmap bmp, int targetWidth, int targetHeight) {
    	if(bmp.isRecycled()) return null;
    	System.out.println("Circle crop bitmap. Size : "+bmp.getByteCount());
    	Bitmap targetBitmap = Bitmap.createBitmap(targetWidth,targetHeight,Bitmap.Config.ARGB_8888);
    	Canvas canvas = new Canvas(targetBitmap);
    	Path path = new Path();
    	float croppedradius = Math.min(((float) targetHeight)/2,((float) targetWidth)/2);
		path.addCircle(((float)targetWidth)/2,((float)targetHeight)/2,croppedradius,Path.Direction.CCW);
		canvas.clipPath(path);
		int halfwidth = bmp.getWidth()/2, halfheight = bmp.getHeight()/2;
		int radius = Math.min(halfwidth,halfheight);
		Rect rectsrc = new Rect(halfwidth - radius, halfheight - radius, halfwidth + radius, halfheight + radius);
		Rect rectdst = new Rect(0, 0, targetWidth, targetHeight);
		canvas.drawBitmap(bmp,rectsrc,rectdst,null);
		return targetBitmap;
    }
    public static Bitmap squareCrop(Bitmap bmp, int targetWidth, int targetHeight) {
    	if(bmp.isRecycled()) return null;
    	Bitmap targetBitmap = Bitmap.createBitmap(targetWidth,targetHeight,Bitmap.Config.ARGB_8888);
    	Canvas canvas = new Canvas(targetBitmap);
    	Path path = new Path();
    	float croppedradius = Math.min(((float) targetHeight)/2,((float) targetWidth)/2);
    	float left = ((float)targetWidth)/2 - croppedradius;
    	float right = ((float)targetWidth)/2 + croppedradius;
    	float top = ((float)targetHeight)/2 - croppedradius;
    	float bottom = ((float)targetHeight)/2 + croppedradius;
    	path.addRect(left, top, right, bottom, Direction.CCW);
    	canvas.clipPath(path);
    	int halfwidth = bmp.getWidth()/2, halfheight = bmp.getHeight()/2;
    	int radius = Math.min(halfwidth,halfheight);
		Rect rectsrc = new Rect(halfwidth - radius, halfheight - radius, halfwidth + radius, halfheight + radius);
		Rect rectdst = new Rect(0, 0, targetWidth, targetHeight);
    	canvas.drawBitmap(bmp,rectsrc,rectdst,null);
    	return targetBitmap;
    }
    
    public static Bitmap rotateBitmap(Bitmap bmp, int rotation){
    	if(bmp.isRecycled()) return null;
	    Matrix matrix = new Matrix();
	    int cx = bmp.getWidth()/2, cy = bmp.getHeight()/2;
	    switch (rotation) {
	    	case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.setScale(-1, 1); break;
	    	case ExifInterface.ORIENTATION_ROTATE_180: matrix.postRotate(180,cx,cy); break;
	    	case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.postRotate(180,cx,cy); matrix.postScale(-1, 1); break;
	    	case ExifInterface.ORIENTATION_TRANSPOSE: matrix.postRotate(90,cx,cy); matrix.postScale(-1, 1); break;
	    	case ExifInterface.ORIENTATION_ROTATE_90: matrix.postRotate(90,cx,cy); break;
	    	case ExifInterface.ORIENTATION_TRANSVERSE: matrix.postRotate(-90,cx,cy); matrix.postScale(-1, 1); break;
	    	case ExifInterface.ORIENTATION_ROTATE_270: matrix.postRotate(-90,cx,cy); break;
	    	default:
	    }
        Bitmap returnbmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        return returnbmp;
    }
    
    public static Bitmap rescaleBitmap(Bitmap bmp, int width, int height){
    	Matrix matrix = new Matrix();
    	matrix.setRectToRect(new RectF(0, 0, bmp.getWidth(), bmp.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
    	Bitmap returnbmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
    	return returnbmp;
    }
    
    public static Bitmap rescaleBitmap(Bitmap bmp, float scalewidth, float scaleheight){
    	Bitmap returnbmp = rescaleBitmap(bmp, (int) (bmp.getWidth()*scalewidth), (int) (bmp.getHeight()*scaleheight));
    	return returnbmp;
    }
    
    public static Bitmap rescaleBitmap(Bitmap bmp, float scale){
    	Bitmap returnbmp = rescaleBitmap(bmp, scale, scale);
    	return returnbmp;
    }
    
    public static Bitmap screenBitmap(Activity activity, Bitmap bmp, int x, int y){
    	if(bmp.isRecycled()) return null;
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int height = metrics.heightPixels;
		int width = metrics.widthPixels;
    	Bitmap targetBitmap = Bitmap.createBitmap(x,y,Bitmap.Config.ARGB_8888);
    	Canvas canvas = new Canvas(targetBitmap);
    	Rect rectsrc = new Rect(x - width/2, y - height/2, x + width/2, y + height/2);
    	Rect rectdst = new Rect(0, 0, width, height);
    	canvas.drawBitmap(bmp,rectsrc,rectdst,null);
    	return targetBitmap;
    }
    
    public static Bitmap croppedBitmap(Bitmap bmp, int x, int y, int w, int h){
    	if(bmp.isRecycled()) return null;
    	Bitmap targetBitmap = Bitmap.createBitmap((int)w,(int)h,Bitmap.Config.ARGB_8888);
    	Canvas canvas = new Canvas(targetBitmap);
    	Path path = new Path();
    	float left = x - w/2, right = x + w/2, top = y - h/2, bottom = y + h/2;
    	path.addRect(left, top, right, bottom, Direction.CCW);
    	canvas.clipPath(path);
    	Rect rectsrc = new Rect(0,0,w,h);
    	Rect rectdst = new Rect(0, 0, w, h);
    	canvas.drawBitmap(bmp,rectsrc,rectdst,null);
    	return targetBitmap;
    }
    
    public static int getExifOrientation(String path){
		try{return new ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);}
		catch(IOException ex){return 0;}
    }
    
    public static byte[] getBytes(Bitmap bmp){
    	if(bmp.isRecycled()) return null;
    	ByteArrayOutputStream stream = new ByteArrayOutputStream();
    	bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
    	return stream.toByteArray();
    }
    
    public static byte[] getBytes(Bitmap bmp, int max){
    	if(bmp.isRecycled()) return null;
        byte[] bytes = getBytes(bmp);
        float rate = bmp.getByteCount()/max;
        if(rate<=1) return bytes;
        /*
        Options sopt = new Options();
        sopt.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, sopt);
        int width = sopt.outWidth, height = sopt.outHeight;
        */
        Options opt = new Options();
        opt.inSampleSize = (int) Math.ceil(Math.sqrt(rate));
        System.out.println("------------------ Get new bytes from original bitmap -------------");
        System.out.println("Max size : "+max);
        System.out.println("Byte[].length : "+bytes.length);
        System.out.println("Bitmap.getByteCount() : "+bmp.getByteCount());
        System.out.println("Options.inSampleSize : "+opt.inSampleSize);
        Bitmap decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);
        byte[] decodedbytes = getBytes(decoded);
        System.out.println("Decoded bmp.getByteCount() : "+decoded.getByteCount());
        System.out.println("Decoded byte[].length : "+decodedbytes.length);
        System.out.println("-------------------------------------------------------------------");
        return decodedbytes;
    }
    
    public static ContentBody getContentBody(Bitmap bmp, String filename){
    	if(bmp.isRecycled()) return null;
        return new ByteArrayBody(getBytes(bmp),filename);
    }
}