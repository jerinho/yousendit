package my.helper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Map extends FragmentActivity implements LocationListener, OnMapReadyCallback, OnMarkerDragListener, OnMarkerClickListener, OnItemClickListener, OnCameraChangeListener, OnClickListener, OnMyLocationChangeListener, OnMyLocationButtonClickListener{
	
	//listener : class name where contains STATIC field name of type MapListener
	//	used to handle the map interaction. if the class is also the map caller activity, just use getClass().getCanonicalName()
	//marks : list of marks
	//location : boolean indicate use location service
	//latitude, longitude : default my location coordinate
	//zoom : starting zoom
	//zoomtarget : zoom level on camera jump if current zoom is too wide
	//delay : delay from map drag finish to listener.onMapDrag()
	//marker : marker color (BitmapDescriptorFactory.COLOR) unset to hide the marker from map
	//center : always center the marker on map drag. default : FALSE
	//point : overlay the center mark on the map. default : FALSE
	//address : display the address bar. default : FALSE
	//reverse : reverse geocode on camera change (required address bar enabled). default : FALSE
	//confirm : the button to return result to the caller activity. default : FALSE
	//span : zoom to optimize level includes all location into the map
	
	public static String GEOCODE = "address", REVERSE = "reverse", LISTENER = "listener", MARKS = "marks", LOCATION = "location";
	public static String DELAY = "delay", MARKER = "marker", FOLLOW = "center", CENTER = "point", RESULT = "confirm";
	public static String ZOOM = "zoombound", BOUND = "zoomtarget", LATITUDE = "latitude", LONGITUDE = "longitude", ADDRESS = "address";
	public static String NULL = "null", MAPLISTENER = "maplistener", SPAN = "bounded";
	public static String GM_URL_REV = "http://maps.google.com/maps/api/geocode/json?sensor=false&address=";
	public static String GM_URL_GEO = "https://maps.google.com/maps/api/geocode/json?latlng=";
	public static String GM_GEOMETRY = "geometry", GM_LOCATION = "location", GM_STATUS = "status", GM_OK = "OK";
	public static String GM_RESULTS = "results", GM_ADDRESS = "formatted_address", GM_LATITUDE = "lat", GM_LONGITUDE = "lng";
	public static String COLOR = "color", ICON = "icon", INFO = "info", TAG = "tag", DRAGABLE = "drag", TITLE = "title", BGCLEAR = "clear";
	public static String BGCENTER = "bgcenter", POSITIONCENTER = "centerposition", SIZECENTER = "centersize";
	public static int requestcode = 444;
	LocationManager locman;
	int locstat, gpsstat, zoombound = 10, zoomtarget = 15, delay = 2;
	MyMapFragment mapfrag;
	Location myloc;
	Date locupd;
	Thread dragger, geocoder;
	String provider;
	Bundle bundle;
	AutoCompleteTextView search;
	protected MapListener listener;
	ArrayAdapter<JSONObject> adaptergeo;
	ArrayList<Mark> marks;
	ImageView center;
	ImageButton btnclear;
	boolean touch, follow, reverse, dropdownoff, geocodeoff;
	public double lat, lon;
	public float zoom;
	public Marker marker;
	public String address;
	public GoogleMap map;
	public CameraPosition camera;
	public ArrayList<Marker> markers;
	public AlertDialog dialog;
	public ContextAdapter adaptermenu;
	public static String ITEM = "mappopupitemtext";
	ListView listmenu;
	
	public class ContextAdapter extends ArrayAdapter<JSONObject>{
		
		public ContextAdapter() {
			super(Map.this, android.R.layout.simple_list_item_1);
		}
		
		@Override public View getView(int position, View reuse, ViewGroup parent) {
    		TextView tv = (TextView) reuse;
    		if(tv==null){
    			tv = new TextView(getContext());
    			tv.setPadding(5, 5, 5, 5);
    			tv.setTextSize(20);
    			tv.setSingleLine(false);
    		}
    		try{tv.setText(getItem(position).getString(ITEM));}
    		catch(JSONException ex){}
			tv.setTag(position);
			return tv;
		}
	}

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.searchmap);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        bundle = getIntent().getExtras();
        marks = new ArrayList<Mark>();
        markers = new ArrayList<Marker>();
        mapfrag = (MyMapFragment) getSupportFragmentManager().findFragmentById(R.id.searchmapfragment);
        //if(mapfrag==null) mapfrag = (MyMapFragment) getSupportFragmentManager().getFragments().get(0).getChildFragmentManager().findFragmentById(mid);
        mapfrag.getMapAsync(this);
        search = (AutoCompleteTextView) findViewById(R.id.searchmapedit);
    	center = (ImageView) findViewById(R.id.mapcenter);
        adaptergeo = new MapAdapter(this,android.R.layout.simple_list_item_1);
		adaptermenu = new ContextAdapter();
		listmenu = new ListView(this);
		listmenu.setAdapter(adaptermenu);
		listmenu.setOnItemClickListener(this);
		btnclear = (ImageButton) findViewById(R.id.mapclear);
		dialog = new AlertDialog.Builder(this).setMessage(null).setTitle(null).setPositiveButton(null, null).setNegativeButton(null, null).setView(listmenu).create();
        if(search!=null){
	        search.setAdapter(adaptergeo);
	        search.setOnClickListener(this);
	        search.setOnItemClickListener(this);
        }
        if(bundle==null) return;
        if(bundle.containsKey(ZOOM)) zoombound = bundle.getInt(ZOOM);
        if(bundle.containsKey(BOUND)) zoomtarget = bundle.getInt(BOUND);
        if(bundle.containsKey(DELAY)) delay = bundle.getInt(DELAY);
        if(bundle.containsKey(TITLE)) setTitle(bundle.getString(TITLE));
        if(bundle.containsKey(BGCLEAR)) btnclear.setBackgroundResource(bundle.getInt(BGCLEAR));
        if(bundle.containsKey(BGCENTER)) center.setBackgroundResource(bundle.getInt(BGCENTER));
        int centersize = bundle.containsKey(SIZECENTER)?bundle.getInt(SIZECENTER):50;
        android.widget.FrameLayout.LayoutParams centerparams = new FrameLayout.LayoutParams(centersize, centersize);
        centerparams.gravity = Gravity.CENTER;
    	if(bundle.containsKey(POSITIONCENTER)){
    		int pos = bundle.getInt(POSITIONCENTER);
    		if(pos==Gravity.TOP) centerparams.setMargins(0, centersize/2, 0, 0);
    		else if(pos==Gravity.BOTTOM) centerparams.setMargins(0, -centersize/2, 0, 0);
    		else if(pos==Gravity.CENTER) centerparams.setMargins(0, 0, 0, 0);
    	}
    	center.setLayoutParams(centerparams);
    	follow = bundle.getBoolean(FOLLOW,false);
        reverse = bundle.getBoolean(REVERSE,false);
        if(!bundle.getBoolean(GEOCODE,false)&&!reverse){
        	search.setVisibility(View.GONE);
        	btnclear.setVisibility(View.GONE);
        }
        if(bundle.getBoolean(CENTER,false)) findViewById(R.id.mapcenter).setVisibility(View.VISIBLE);
        if(bundle.containsKey(LISTENER)) try {
        	String fieldname, classname;
    		for(Field field : Class.forName(bundle.getString(LISTENER)).getDeclaredFields()){
    			Object obj = null;
    			try{obj = field.get(null);}
    			catch(NullPointerException ex){}
    			if(obj==null||!(obj instanceof MapListener)) continue;
    			listener = (MapListener) obj;
    			listener.map = this;
    			fieldname = field.getName();
    			classname = obj.getClass().getSimpleName();
    			break;
    		}
		}catch(ClassNotFoundException ex){}catch(IllegalAccessException ex){}
    }
    
    @Override protected void onPause() {
    	super.onPause();
    	if(locman!=null) locman.removeUpdates(this);
    }
    
    @Override public void onMapReady(GoogleMap gm) {
    	try{
	        map = mapfrag.getMap();
	    	map.getUiSettings().setRotateGesturesEnabled(false);
	    	map.setOnMarkerClickListener(this);
	    	map.setOnMarkerDragListener(this);
	    	map.setOnCameraChangeListener(this);
	    	map.setMyLocationEnabled(true);
	    	map.setOnMyLocationChangeListener(this);
	    	map.setOnMyLocationButtonClickListener(this);
	    	Location myloc = map.getMyLocation();
	    	if(myloc!=null) map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(myloc.getLatitude(), myloc.getLongitude()), zoombound));
	    	if(bundle==null) return;
	    	if(listener!=null) listener.onMapReady();
			if(bundle.containsKey(MARKS)) putMarks(bundle.getString(MARKS));
	    	if(bundle.containsKey(LATITUDE)&&bundle.containsKey(LONGITUDE)){
	    		lat = bundle.getDouble(LATITUDE);
	    		lon = bundle.getDouble(LONGITUDE);
	    		if(bundle.containsKey(ZOOM)) zoom = bundle.getFloat(ZOOM);
	    		if(bundle.containsKey(MARKER)) createMarker(lat,lon,bundle.getFloat(MARKER,0));
				moveCamera(lat, lon, zoom);
	    		return;
	    	}
	    	else if(bundle.getString(SPAN)!=null) moveCamera(bundle.getString(SPAN));
	    	if(bundle.getBoolean(LOCATION,false)) locate(null);
    	}catch(Exception ex){
    		Toast.makeText(this, "Map not yet ready. Please try again", Toast.LENGTH_SHORT);
    		finish();
    	}
    }
    
	@Override public void onProviderEnabled(String provider) {
		//Toast.makeText(this, "Please wait while we finding your location...", Toast.LENGTH_SHORT).show();
	}

	@Override public void onProviderDisabled(String provider) {
		//Toast.makeText(this, "Please turn on your location service", Toast.LENGTH_SHORT).show();
		myloc = null;
	}
	
	public void locate(View view){
		//if(locupd!=null&&Calendar.getInstance().getTime().getTime() - locupd.getTime() < 3600000) return;
		if(locman==null) locman = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		provider = null;
        if(locman.isProviderEnabled(LocationManager.GPS_PROVIDER)) provider = LocationManager.GPS_PROVIDER;
        else if(locman.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) provider = LocationManager.NETWORK_PROVIDER;
        else Toast.makeText(this, "Please turn on your location service", Toast.LENGTH_SHORT).show();
        if(provider!=null){
        	Toast.makeText(this, "Please wait while we finding your location...", Toast.LENGTH_SHORT).show();
        	locman.requestLocationUpdates(provider, 999999999, 0, this); //result listened by onLocationChanged
        }
	}
	
	public void popup(ArrayList<JSONObject> list){
		dialog.hide();
		
		dialog.show();
	}

	@Override public void onLocationChanged(Location location) {
		System.out.println("Location changed. "+location.getLatitude()+","+location.getLongitude());
		Toast.makeText(this, "Location changed. "+location.getLatitude()+","+location.getLongitude(), Toast.LENGTH_SHORT).show();
	}

	@Override public void onStatusChanged(String provider, int status, Bundle extras) {
		System.out.println("Status changed. Provider : "+provider+", Status : "+status+", Data : "+extras);
		Toast.makeText(this, "Status changed. Provider : "+provider+", Status : "+status+", Data : "+extras, Toast.LENGTH_SHORT).show();
	}
	
    @Override public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	if(bundle.getBoolean(RESULT,false)) menu.add("Confirm").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    	return true;
    }
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) finish();
		else if(item.getTitle().charAt(0)=='C'){ //Confirm
    		if(search.getText().length()==0) return false;
    		Intent intent = getIntent();
    		LatLng loc = map.getCameraPosition().target;
    		intent.putExtra(LATITUDE, loc.latitude);
    		intent.putExtra(LONGITUDE, loc.longitude);
    		intent.putExtra(ADDRESS, (String) search.getTag());
    		setResult(Activity.RESULT_OK, intent);
    		finish();
    	}
    	return true;
    }
        
	@Override public boolean onMarkerClick(Marker mark) {
		if(mark.isInfoWindowShown()) mark.hideInfoWindow();
		if(listener==null||listener.onMarkerClick(markers.indexOf(mark))) mark.showInfoWindow();
        return true; //disable default behavior (camera animated to center the marker and info window appear)
	}
    
	@Override public void onMarkerDrag(Marker mark) {
	}
	
	@Override public void onMarkerDragEnd(Marker mark) {
	}
	
	@Override public void onMarkerDragStart(Marker marker) {
		int index = markers.indexOf(marker);
		if(index ==-1){
			marker.hideInfoWindow();
			return;
		}
		MarkerOptions opt = marks.get(index).opts;
	    marker.remove();
	    Mark mark = marks.remove(index);
	    mark.mark = map.addMarker(opt);
	    marks.add(index,mark);
	    if(listener!=null) listener.onMarkerLongClick(index);
	}
		
	@Override public void onCameraChange(CameraPosition pos) {
		//Interrupt thread first to cancel running to prevent from listener.onMapDrag
		dropdownoff = true;
		if(marker!=null&&follow) markhere(null);
		if(reverse) reverseGeocode(pos.target.latitude, pos.target.longitude);
		if(dragger!=null) dragger.interrupt();
		if(touch) return;
		dragger = new Sleepy(this);
	    dragger.start();
	}
		
	@Override public void onMyLocationChange(Location arg0) {
		if(myloc!=null) return;
		gohome(null);
	}
	
	@Override public boolean onMyLocationButtonClick() {
		geocodeoff = true;
		System.out.println("geocode turned off");
		return false;
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		//send http request only after 2 seconds from touch up. to prevent http request from been sent too responsively
		dismissKeyboard();
		int action = event.getAction();
		if(action == MotionEvent.ACTION_DOWN) touch = true;
		else if(action == MotionEvent.ACTION_UP) touch = false;
		geocodeoff = false;
		if(listener==null) return true;
		return true;
	}
	
	public Object getTag(int index){
		return marks.get(index).tag;
	}
		
	public void putMarks(String result) throws JSONException{
		putMarks(new JSONArray(result));
	}
	
	public void putMarks(JSONArray locs) throws JSONException{
		double lat = 0, lon = 0;
		float color = 0;
		String info = null;
		boolean drag = false;
		Object tag = null;
		int icon = 0;
		for(int i=0;i<locs.length();i++){
			JSONObject obj = locs.getJSONObject(i);
			if(obj.has(LATITUDE)&&obj.has(LONGITUDE)){
				lat = obj.getDouble(LATITUDE);
				lon = obj.getDouble(LONGITUDE);
			}
			if(obj.has(INFO)) info = obj.getString(INFO);
			if(obj.has(DRAGABLE)) drag = obj.getBoolean(DRAGABLE);
			if(obj.has(TAG)) tag = obj.get(TAG);
			if(obj.has(ICON)) icon = obj.getInt(ICON);
			if(obj.has(COLOR)) color = (float) obj.getDouble(COLOR);
			putMark(drag, lat, lon, info, tag, color, icon);
		}
	}
	
	public void moveCamera(String locs) throws JSONException{
		moveCamera(new JSONArray(locs));
	}
	
	public void moveCamera(JSONArray locs) throws JSONException{
		JSONObject x = locs.getJSONObject(0), y = locs.getJSONObject(1);
		double[] dx = new double[]{x.getDouble(LATITUDE),x.getDouble(LONGITUDE)};
		double[] dy = new double[]{y.getDouble(LATITUDE),y.getDouble(LONGITUDE)};
    	LatLng sw = new LatLng(Math.min(dx[0],dy[0]),Math.min(dx[1],dy[1]));
    	LatLng ne = new LatLng(Math.max(dx[0],dy[0]),Math.max(dx[1],dy[1]));
		animateCameraBounds(new LatLngBounds(sw, ne));
	}
	
	public int putMark(boolean drag, double lat, double lon, String info, Object tag, float color, int icon){
		System.out.println("-------------- Marker on the map --------------");
		System.out.println("> Latitude : "+lat);
		System.out.println("> Longitude : "+lon);
		System.out.println("> Info : "+info);
		System.out.println("> Draggable : "+drag);
		System.out.println("> Color : "+color);
		System.out.println("> Icon : "+icon);
		if(tag!=null) System.out.println("> Attached : "+tag.toString());
		System.out.println("-----------------------------------------------");
		BitmapDescriptor desc;
		if(icon>0) desc = BitmapDescriptorFactory.fromResource(icon);
		else if(color>0) desc = BitmapDescriptorFactory.defaultMarker(color);
		else desc = BitmapDescriptorFactory.defaultMarker(0);
		MarkerOptions opt = new MarkerOptions().icon(desc).draggable(drag).position(new LatLng(lat,lon));
		String title = null;
		if(listener!=null){
			String res = listener.infoWindowContent(marks.size(),info);
			if(res!=null) title = res;
		}
		if(title!=null) opt.title(title);
    	Marker marker = map.addMarker(opt);
    	int pos = marks.size();
    	Mark mark = new Mark();
    	mark.mark = marker;
    	mark.info = info;
    	mark.tag = tag;
    	marks.add(mark);
    	markers.add(marker);
    	return pos;
	}
	
	public void clearMarks(){
		while(marks.size()>0) removeMark(0);
	}
	
	public void removeMark(int pos){
		markers.remove(pos).remove();
		marks.remove(pos);
	}
	
	public void editMark(int pos, double lat, double lon, float color, String info){
		Marker mark = marks.get(pos).mark;
		if(lat!=0&&lon!=0) mark.setPosition(new LatLng(lat, lon));
		if(info!=null) mark.setTitle(info);
		if(color!=-1) mark.setIcon(BitmapDescriptorFactory.defaultMarker(color));
	}
		
	public void reverseGeocode(double lat, double lon){
		if(geocodeoff) return;
		final double flat = lat, flon = lon;
		if(geocoder!=null) geocoder.interrupt();
		geocoder = new Thread() {
			
			HttpGet get;
			HttpClient client;
			HttpResponse response;
			
			@Override public void interrupt() {
				super.interrupt();
				if(get!=null) new Thread(){public void run() {get.abort();};};
			}
			
			@Override public void run() {
				String url = GM_URL_GEO + flat + "," + flon;
			    get = new HttpGet(url);
			    client = new DefaultHttpClient();
			    StringBuilder stringBuilder = new StringBuilder();
			    try {
			    	if(isInterrupted()) return;
			        response = client.execute(get);
			        HttpEntity entity = response.getEntity();
			        InputStream stream = entity.getContent();
			        int b;
			        while ((b = stream.read()) != -1) stringBuilder.append((char) b);
			    } catch (IOException ex) {System.out.println(ex.getMessage());}
			    String httpresult = stringBuilder.toString();
			    try{
			    	JSONObject json = new JSONObject(httpresult);
			    	String status = json.getString(GM_STATUS);
			    	if(!status.equals(GM_OK)) return;
		    		final String address = json.getJSONArray(GM_RESULTS).getJSONObject(0).getString(GM_ADDRESS);
			    	if(!isInterrupted()) runOnUiThread((new Runnable() {
						
						@Override public void run() {
							search.setTag(address);
							search.setText(address);
						}
					}));
			    }catch(JSONException ex){System.out.println(ex.getMessage());}
			}
		};
		geocoder.start();
	}
		
	@Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if(parent.getAdapter()==adaptergeo){
			search.dismissDropDown();
			dismissKeyboard();
			try{
				JSONObject obj = adaptergeo.getItem(position);
				address = obj.getString(ADDRESS);
				moveCamera(obj.getDouble(LATITUDE),obj.getDouble(LONGITUDE));
			}catch(JSONException ex){}
			search.setTag(address);
			search.setText(address);
		}else if(parent==listmenu){
			if(listener!=null) listener.onMenuItemClick(position);
		}
	}
	
	@Override public void onClick(View v) {
		dropdownoff = false;
	}
	
	public void dismissKeyboard(){
		InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		im.hideSoftInputFromWindow(search.getWindowToken(), 0);
	}
	
	public void gohome(View view){
    	myloc = map.getMyLocation();
    	if(myloc==null) return;
        moveCamera(myloc.getLatitude(),myloc.getLongitude());
	}
	
	public void moveCamera(double lat, double lon, float zoom){
		if(zoom==0){
			zoom = map.getCameraPosition().zoom;
			if(zoom<zoombound) zoom = (float) zoomtarget;
		}
		animateCameraTo(lat,lon,zoom);
		geocodeoff = true;
	}
	
	public void moveCamera(double lat, double lon){
		moveCamera(lat, lon, 0);
	}
	
	public void animateCameraBounds(LatLngBounds bounds){
    	map.getUiSettings().setScrollGesturesEnabled(false);
    	map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0), new CancelableCallback(){

            @Override public void onFinish(){
            	map.getUiSettings().setScrollGesturesEnabled(true);
            }

            @Override public void onCancel(){
            	map.getUiSettings().setAllGesturesEnabled(true);
            }
        });
	}
	
	public void animateCameraTo(final double lat, final double lng, final float zoom){
	    CameraPosition camPosition = map.getCameraPosition();
	    if ((Math.floor(camPosition.target.latitude * 100) / 100) == (Math.floor(lat * 100) / 100) && (Math.floor(camPosition.target.longitude * 100) / 100) == (Math.floor(lng * 100) / 100)) return;
    	map.getUiSettings().setScrollGesturesEnabled(false);
    	map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng),zoom), new CancelableCallback(){

            @Override public void onFinish(){
            	map.getUiSettings().setScrollGesturesEnabled(true);
            }

            @Override public void onCancel(){
            	map.getUiSettings().setAllGesturesEnabled(true);
            }
        });
	}
	
    public void createMarker(double lat, double lon, float col){
    	BitmapDescriptor color = BitmapDescriptorFactory.defaultMarker(col);
    	marker = map.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).icon(color).title("Drag me to set location !").draggable(true));
    }
	
	public void markhere(View view){
		LatLng loc = map.getCameraPosition().target;
		if(marker!=null) marker.setPosition(loc);
	}
	
	public void gotomarker(View view){
		if(marker==null) return;
		LatLng latlon = marker.getPosition();
		moveCamera(latlon.latitude, latlon.longitude);
	}
	
	static public float getBoundsZoom(Activity activity, LatLngBounds bounds){
		Point size = new Point();
		activity.getWindowManager().getDefaultDisplay().getSize(size);
	    double lngDiff = bounds.northeast.longitude - bounds.southwest.longitude;
	    float latZoom = zoom(size.y, 256, (latRad(bounds.northeast.latitude) - latRad(bounds.southwest.latitude)) / Math.PI);
	    float lngZoom = zoom(size.x, 256, ((lngDiff < 0) ? (lngDiff + 360) : lngDiff) / 360);
	    return Math.min(Math.min(latZoom,lngZoom),21);
	}

	static public float latRad(double lat) {
	    double sin = Math.sin(lat * Math.PI / 180);
	    return (float) (Math.max(Math.min((Math.log((1 + sin) / (1 - sin)) / 2), Math.PI), - Math.PI) / 2);
	}
	static public float zoom(double mapPx, double worldPx, double fraction) {
	    return (float) Math.floor(Math.log(mapPx / worldPx / fraction) / 0.6931471805599453);
	}
	
	public void zoomFromSpan(LatLng px, LatLng py){
		LatLngBounds bounds = new LatLngBounds.Builder().include(px).include(py).build();
		int width = getWindowManager().getDefaultDisplay().getWidth();
		map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, 250, 30));
	}
	
	public void clear(View view){
		search.setText("");
	}
	
	private static class Sleepy extends Thread{
		
		Map activity;
		
		public Sleepy(Map activity) {
			this.activity = activity;
		}
        public void run(){
        	try{
				Thread.sleep(activity.delay*1000);
				if(isInterrupted()) return;
				activity.runOnUiThread(new Thread(){
					public void run(){
						activity.camera = activity.map.getCameraPosition();
						if(activity.listener!=null) activity.listener.onMapDrag();
					}
				});
        	}catch(InterruptedException ex){} //No need to test for interruption if the error got caught on thread sleep
		}
	}
		
	public static class AddressFilter extends Filter {
		
		ArrayAdapter<JSONObject> adapter;
		Map activity;
		 
	    public AddressFilter(Map activity, ArrayAdapter<JSONObject> sma) {
	    	this.adapter = sma;
	    	this.activity = activity;
		}
	    
		@Override protected FilterResults performFiltering(CharSequence text) {
			if(activity.dropdownoff) return null;
	        FilterResults result = new FilterResults();
    	    String url = GM_URL_REV + URLEncoder.encode((String) text);
    	    HttpGet httpGet = new HttpGet(url);
    	    HttpClient client = new DefaultHttpClient();
    	    HttpResponse response;
    	    StringBuilder stringBuilder = new StringBuilder();
    	    try {
    	        response = client.execute(httpGet);
    	        HttpEntity entity = response.getEntity();
    	        InputStream stream = entity.getContent();
    	        int b;
    	        while ((b = stream.read()) != -1) stringBuilder.append((char) b);
    	    } catch (IOException ex) {System.out.println(ex.getMessage());}
    	    String httpresult = stringBuilder.toString();
    	    try{
		    	JSONObject json = new JSONObject(httpresult);
		    	String status = json.getString(GM_STATUS);
		    	if(!status.equals(GM_OK)) return null;
	    		ArrayList<JSONObject> addresses = new ArrayList<JSONObject>();
	    		JSONArray res = json.getJSONArray(GM_RESULTS);
	    		for(int i=0;i<res.length();i++){
	    			JSONObject item = res.getJSONObject(i);
	    			JSONObject loc = item.getJSONObject(GM_GEOMETRY).getJSONObject(GM_LOCATION);
	    	        addresses.add(new JSONObject()
	    	        	.put(ADDRESS, item.getString(GM_ADDRESS))
	    	        	.put(LATITUDE, loc.getDouble(GM_LATITUDE))
	    	        	.put(LONGITUDE, loc.getDouble(GM_LONGITUDE))
	    	        );
	    		}
	    		result.values = addresses;
	    		result.count = addresses.size();
    	    }catch(JSONException ex){System.out.println(ex.getMessage());}
	        return result;
	    }

	    @Override protected void publishResults(CharSequence constraint, FilterResults results) {
	        if(results==null) return;
	        adapter.clear();
	        if(results.count>0) for(JSONObject obj : (ArrayList<JSONObject>) results.values) adapter.add(obj);
	    }
	    
	}

	public static class MyMapFragment extends SupportMapFragment{
		
		  public View mOriginalContentView;
		  public TouchableWrapper mTouchView;

		  @Override public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
			  try{
				mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);    
				mTouchView = new TouchableWrapper((Map) getActivity());
				mTouchView.addView(mOriginalContentView);
				RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) ((View) mOriginalContentView.findViewById(1).getParent()).findViewById(2).getLayoutParams();
				rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
				rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
				rlp.setMargins(0, 0, 30, 30);
				return mTouchView;
			  }catch(NullPointerException ex){
				  return null;
			  }
		  }

		  @Override public View getView() {
		    return mOriginalContentView;
		  }
	}
	
	public static class MapAdapter extends ArrayAdapter<JSONObject>{
		
		AddressFilter filter;
    	
    	public MapAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
		}
    	@Override public Filter getFilter() {
    		return new AddressFilter((Map) getContext(), this);
    	}
		@Override public View getView(int position, View reuse, ViewGroup parent) {
    		LinearLayout layout = (LinearLayout) reuse;
    		TextView tv = null;
    		if(layout!=null) tv = (TextView) layout.getChildAt(0);
    		else{
    			layout = new LinearLayout(getContext());
    			tv = new TextView(getContext());
    			layout.addView(tv);
    			tv.setPadding(5, 5, 5, 5);
    			tv.setTextSize(20);
    			tv.setSingleLine(false);
    			tv.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    		}
    		try{
        		tv.setText(getItem(position).getString(ADDRESS));
    		}catch(JSONException ex){
        		tv.setText(NULL);
    		}
    		return layout;
    	}
    }
	
	public static class TouchableWrapper extends FrameLayout {
		
		Map mapact;

		  public TouchableWrapper(Map ctx) {
		    super(ctx);
		    mapact = ctx;
		  }

		  @Override public boolean dispatchTouchEvent(MotionEvent event) {
			  mapact.onTouch(null, event);
			  return super.dispatchTouchEvent(event);
		  }
	}
		
	public static class MapListener{
		
		public Map map;
    			
		//boolean return is false to prevent default
		public boolean onMarkerClick(int index){
			return true;
		}
		public void onMenuItemClick(int position) {
		}
		public boolean onMarkerLongClick(int index){
			return true;
		}
		public boolean onMapReady(){
			return true;
		}
		public boolean onMapDrag(){
			return true;
		}
		public String infoWindowContent(int pos, String result){
			return result;
		}
	}
	
	public static class Mark{
		
		public Object tag;
		public String info;
		public Marker mark;
		public MarkerOptions opts;
	}
}