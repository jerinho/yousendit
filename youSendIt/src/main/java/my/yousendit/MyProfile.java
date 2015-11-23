package my.yousendit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.JrgAndroid;
import my.helper.SimpleHttp;
import my.yousendit.peripherals.ApiButton;
import my.yousendit.peripherals.DrawerActivity;
import my.yousendit.peripherals.MyTextBox;
import my.yousendit.peripherals.Shared;

import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.text.Html;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;

public class MyProfile extends DrawerActivity implements ApiHandler, OnClickListener, OnMenuItemClickListener, android.view.View.OnClickListener, OnShowListener, Transformation{

	MyTextBox name, email, location, etpwdcrt, etpwdnew, etpwdver;
	Button btnpwdprompt, btnpwdupdate;
	ApiButton update;
	ImageView image;
	ImageButton changeimage;
	String imagepath, country, crtname, crtemail;
	View prompt;
	ScrollView scroller;
	AlertDialog dialog;
	int locindex, crtloc;
	boolean busy;
	List<String> countries, countriesbycode;
	Class[] xtvts = new Class[]{Account.class, Payment.class, History.class, MyReview.class};
	boolean[] enabled = new boolean[4];
	public byte[] bytes;
	public Bitmap result;
	LinearLayout links;
	ApiRequest imageupdate;
	Uri imguri;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(Shared.id==0) System.exit(0);
		if(Shared.countries!=null) countries = Arrays.asList(Shared.countries);
		if(Shared.countriesbycode!=null) countriesbycode = Arrays.asList(Shared.countriesbycode);
		image = (ImageView) findViewById(R.id.profileimg);
		name = (MyTextBox) findViewById(R.id.profilename);
		email = (MyTextBox) findViewById(R.id.profileemail);
		location = (MyTextBox) findViewById(R.id.profilelocation);
		location.getTextBox().setOnClickListener(this);
		btnpwdprompt = (Button) findViewById(R.id.profilepassword);
		prompt = getLayoutInflater().inflate(R.layout.dialogpassword, null);
		etpwdcrt = (MyTextBox) prompt.findViewById(R.id.profilepwdcurrent);
		etpwdnew = (MyTextBox) prompt.findViewById(R.id.profilepwdnew);
		etpwdver = (MyTextBox) prompt.findViewById(R.id.profilepwdconfirm);
		update = (ApiButton) findViewById(R.id.profileupdate);
		links = (LinearLayout) findViewById(R.id.profilelinks);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setPositiveButton(Html.fromHtml("<b style='color:#f44336';>UPDATE</b>"),null);
		builder.setNegativeButton(Html.fromHtml("<b style='color:#f44336';>CANCEL</b>"),null);
		builder.setTitle(Html.fromHtml("<b style='color:black';>Change Password</b>"));
		dialog = builder.create();
		dialog.setView(prompt);
		dialog.setOnShowListener(this);
		HashMap<String,Object> params = new HashMap<String, Object>();
		params.put("loginkey", Shared.loginkey);
		request = SimpleHttp.request("resume",Shared.url+"auth/profileView",params,this);
		busy = true;
	}
	
	@Override protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		//if(imguri!=null) outState.putString("imagepath",imguri.toString());
	}
	
	@Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		//String uripath = savedInstanceState.getString("imagepath");
		//imguri = uripath==null?null:Uri.parse(uripath);
	}
	
	@Override protected void onResume() {
		super.onResume();
		for(int i=0; i<links.getChildCount(); i++){
			LinearLayout link = (LinearLayout) links.getChildAt(i);
			link.setBackgroundColor(Color.TRANSPARENT);
		}
	}
		
	@Override public View setMainView() {
		return (ScrollView) inflate(R.layout.myprofile);
	}
	
	@Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if(v==location||v==location.getTextBox())
			for(String country : Shared.countries) menu.add(country).setOnMenuItemClickListener(this);
		else if(v == changeimage){
			changeimage.setEnabled(false);
			menu.add("Take image from camera").setOnMenuItemClickListener(this);
			menu.add("Use existing image").setOnMenuItemClickListener(this);
		}
	}
	
	@Override public void onContextMenuClosed(Menu menu) {
		super.onContextMenuClosed(menu);
		if(changeimage!=null) changeimage.setEnabled(true);
	}
	
	@Override public boolean onContextItemSelected(MenuItem item) {
		return super.onContextItemSelected(item);
	}
	
	@Override public boolean onMenuItemClick(MenuItem item) {
		if(changeimage==null||changeimage.isEnabled()){
			location.setText(item.getTitle().toString());
			locindex = countriesbycode.indexOf(item.getTitle())+1;
		}else try{
			char ch = item.getTitle().charAt(0);
			if(ch=='T') addImageFromCamera();
			else if(ch=='U') addImageFromGallery();
			changeimage.setEnabled(true);
		}catch(IOException ex){
			//System.out.println("Add image : "+ex.getMessage());
		}
		return false;
	}
		
	@Override public void onClick(DialogInterface dialog, int which) {
	}
	
	public void update(View view){
		if(busy) return;
		update.setBusy(true);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("loginkey", Shared.loginkey);
		if(crtname==null||!crtname.equals(name.getText().toString())) params.put("name", name.getText().toString());
		if(crtemail==null||!crtemail.equals(email.getText().toString())) params.put("email", email.getText().toString());
		if(locindex!=0&&crtloc!=locindex) params.put("location", String.valueOf(locindex));
		ArrayList<ContentBody> bodies = new ArrayList<ContentBody>();
		if(bytes!=null) bodies.add(new ByteArrayBody(bytes, "profile.jpeg"));
		imageupdate = ApiRequest.request("update", Shared.url+"auth/profileEdit", params, null, bodies.size()==0?null:bodies, this);
		busy = true;
	}
	
	@Override public void onClick(View v) {
		if(v==location||v==location.getTextBox()){
			registerForContextMenu(v);
			openContextMenu(v);
			unregisterForContextMenu(v);
		}
		else if(v==btnpwdupdate){
			String pwdcrt = etpwdcrt.getText().toString();
			String pwdnew = etpwdnew.getText().toString();
			String pwdver = etpwdver.getText().toString();
			if(pwdcrt.length()<6||pwdnew.length()<6||pwdcrt.equals(pwdnew)) Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show();
			else if(!pwdnew.equals(pwdver)) Toast.makeText(this, "Password not match", Toast.LENGTH_SHORT).show();
			else{
				HashMap<String,Object> params = new HashMap<String, Object>();
				params.put("loginkey", Shared.loginkey);
				params.put("currentPassword", pwdcrt);
				params.put("newPassword", pwdnew);
				request = SimpleHttp.request("password",Shared.url+"auth/changePassword",params,this);
				busy = true;
			}
		}
	}
	
	public void password(View view){
		dialog.show();
	}
	
	@Override public void onShow(DialogInterface dialog) {
		etpwdcrt.setText("");
		etpwdnew.setText("");
		etpwdver.setText("");
		btnpwdupdate = this.dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		btnpwdupdate.setOnClickListener(this);
	}
		
	public void image(View view){
		changeimage = (ImageButton) view;
		registerForContextMenu(view);
		openContextMenu(view);
		unregisterForContextMenu(view);
	}
	
	public void addImageFromGallery(){
		Intent intent = new Intent();
		intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_PICK);
		if(!Build.MANUFACTURER.equalsIgnoreCase("SAMSUNG")) 
			intent.putExtra(MediaStore.EXTRA_OUTPUT, getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues()));
		startActivityForResult(intent,2);
	}
	public void addImageFromCamera() throws IOException{
		//<uses-feature android:name="android.hardware.Camera"/>
		//<uses-permission android:name="android.permission.CAMERA"/>
		//Don't get the uri data from camera result because some camera activity will return null, instead pass uri to EXTRA_OUTPUT key to let the camera store the image
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if(!Build.MANUFACTURER.equalsIgnoreCase("SAMSUNG")){
			ContentValues values = new ContentValues();
			values.put(MediaStore.Images.Media.TITLE, "uploadprofileimage.jpg");
			values.put(MediaStore.Images.Media.DESCRIPTION,"Image capture by camera");
			imguri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,imguri);
		}
        startActivityForResult(intent,2);
	}
	
	@Override protected void onActivityResult(int req, int res, Intent data) {
		super.onActivityResult(req, res, data);
		if(res!=Activity.RESULT_OK||req!=2) return;
		Uri uri = data!=null&&data.getData()!=null?data.getData():(data==null&&imguri!=null?imguri:null);
		if(uri!=null) Picasso.with(this).load(uri).resize(500,500).centerInside().transform(this).into(image);
	}
	
	public void jump(View view){
		LinearLayout item = (LinearLayout) view.getParent(), listitems = (LinearLayout) item.getParent();
		item.setBackgroundColor(Color.LTGRAY);
		int pos = listitems.indexOfChild(item);
		if(pos>=xtvts.length) return;
		Intent intent = new Intent(this, xtvts[pos]);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
		
	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		busy = false;
		if(result==null){
			Toast.makeText(this, "Unexpected error. Please retry", Toast.LENGTH_SHORT).show();
			if(tag.equals("update")) update.setBusy(false);
			return;
		}
		if(tag.equals("password")) try{
			JSONObject obj = new JSONObject(result);
			if(obj.getInt("success")==1){
				Toast.makeText(this, "Your password was succesfully updated", Toast.LENGTH_SHORT).show();
				dialog.dismiss();
				btnpwdprompt.setText("CHANGED");
			}else{
				Toast.makeText(this, "Fail. Please retry", Toast.LENGTH_SHORT).show();
			}
		}catch(JSONException jsex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
		else if(tag.equals("update")) try{ //on response to job submitting or updating
			update.setBusy(false);
			JSONObject obj = new JSONObject(result);
			if(obj.getInt("success")==1){
				Toast.makeText(this, "Your profile was succesfully updated", Toast.LENGTH_SHORT).show();
				crtname = name.getText().toString();
				crtemail = email.getText().toString();
				crtloc = locindex;
				if(obj.isNull("post")) return;
				JSONObject data = obj.getJSONObject("post");
				if(data.isNull("image")) return;
				Shared.editor.putString(Shared.image = data.getString("image"),null);
				Shared.editor.apply();
			}else{
				Toast.makeText(this, "Fail. Please retry", Toast.LENGTH_SHORT).show();
			}
		}catch(JSONException jsex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}else if(tag.equals("resume")) try{
			JSONObject obj = new JSONObject(result);
			if(obj.getInt("success")==1){
				JSONObject post = obj.getJSONArray("post").getJSONObject(0);
				if(!post.isNull("email")){
					crtemail = post.getString("email");
					email.setText(crtemail);
				}
				if(!post.isNull("name")){
					crtname = post.getString("name");
					name.setText(crtname);
				}
				if(!post.isNull("location")){
					crtloc = post.getInt("location");
					location.setText(countriesbycode.get(crtloc!=0?crtloc-1:0));
				}
				if(!post.isNull("image")){
					imguri = null;
					Picasso.with(this).load(post.getString("image")).into(image);
				}
			}else{
				Toast.makeText(this, "Fail. Please retry", Toast.LENGTH_SHORT).show();
			}
		}catch(JSONException jsex){
			Toast.makeText(this, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
	}

	public String key() {
		return "myprofile";
	}

	@Override public Bitmap transform(Bitmap bmp) {
		//System.out.println("MyProfile.Transformation. Bitmap size : "+bmp.getByteCount());
		bytes = JrgAndroid.getBytes(bmp, 300000);
		return bmp;
	}
	
	/*
	@Override public Bitmap transform(Bitmap bmp) {
		try{
			if(imguri==null||Build.MANUFACTURER.equalsIgnoreCase("SAMSUNG")) result = bmp;
			else{
				ExifInterface exif = new ExifInterface(imguri.getPath());
				int or = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
				result = JrgAndroid.rotateBitmap(bmp, or);
			}
			bytes = JrgAndroid.getBytes(result,200000);
			return result;
		}catch(Exception ex){
			System.out.println("Activity result : "+ex.getMessage());
			return null;
		}
	}
	*/
}