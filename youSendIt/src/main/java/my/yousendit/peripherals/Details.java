package my.yousendit.peripherals;

import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;

import com.google.android.gms.maps.model.LatLng;

import android.location.Location;
import android.text.format.Time;

public class Details{
	
	//Store all current viewed job details
	
	HashMap<String, String> map;
	
	public Details(int id) {
		this.id = id;
		map = new HashMap<String, String>();
	}
	
	@Override public String toString() {
		map.clear();
		map.put("job id", String.valueOf(id));
		map.put("sender id", String.valueOf(sender));
		map.put("transporter id", String.valueOf(trans));
		map.put("item size", String.valueOf(size));
		map.put("job urgency", String.valueOf(urgency));
		map.put("is the sender", String.valueOf(issender));
		map.put("is the transporter", String.valueOf(istrans));
		map.put("job rate", String.valueOf(rate));
		map.put("review text", review);
		map.put("proposed the last pick up date", String.valueOf(isproposer));
		map.put("sender name", sendername);
		map.put("transporter name", transname);
		map.put("pin number", pin);
		map.put("agreed pick up date", agreed.toString());
		map.put("proposed pick up date", proposed.toString());
		map.put("pick up location", locfrom.toString());
		map.put("drop off location", locto.toString());
		map.put("pick up address", addrfrom);
		map.put("drop off address", addrto);
		map.put("job fee", String.valueOf(fee));
		map.put("job valid until", valid.toString());
		map.put("actual pickup time", timepickup.toString());
		map.put("actual dropoff time", timedropoff.toString());
		map.put("job progress", progress.toString());
		if(images!=null) map.put("images", images.toString());
		return map.toString();
	}
	public int id, sender, trans, size, urgency, rate;
	public boolean issender, istrans, isbidder, isproposer, indicated, rejected;
	public double fee;
	public double[] locfrom, locto;
	public String sendername, transname, pin, addrfrom, addrto, review;
	public String[] images;
	public Date agreed, proposed, valid, timepickup, timedropoff, timepost;
	public JSONArray progress;
	public String senderimage;
}