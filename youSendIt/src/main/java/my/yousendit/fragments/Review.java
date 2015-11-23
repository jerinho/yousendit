package my.yousendit.fragments;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import my.helper.ApiRequest;
import my.helper.ApiRequest.ApiHandler;
import my.helper.MyFragment;
import my.helper.SimpleHttp;
import my.yousendit.R;
import my.yousendit.peripherals.ApiButton;
import my.yousendit.peripherals.MyTextBox;
import my.yousendit.peripherals.Shared;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class Review extends MyFragment implements ApiHandler, OnRatingBarChangeListener{
	
	RatingBar rate;
	MyTextBox text;
	ApiButton btn;
	TextView msg;
	SimpleHttp request;
	boolean loaded;
	
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View view = super.onCreateView(inflater, container, savedInstanceState);
		if(Shared.details.timedropoff==null){
			view = inflater.inflate(R.layout.message,null);
			((TextView) view.findViewById(R.id.screenmessage)).setText("You can only post the job review after the item is delivered");
			return view;
		}
		view = inflater.inflate(R.layout.review,null);
		msg = (TextView) view.findViewById(R.id.jobrvmsg);
		rate = (RatingBar) view.findViewById(R.id.jobrvrate);
		text = (MyTextBox) view.findViewById(R.id.jobrvtext);
		boolean editable = Shared.details.issender&&Shared.details.rate==0;
		if(Shared.details.rate==0&&Shared.details.istrans){
			msg.setText("Congratulation. Your job is done !\n\nPlease ask the sender to rate your job.\n\nGood luck !");
			return view;
		}
		String msgtrans = "Congratulation. Your job is done. This is your job review by the sender";
		String msgsender1 = "Congratulation. Your job is done. Please review your transporter";
		String msgsender2 = "This is your review for the transporter";
		msg.setText(Shared.details.istrans?msgtrans:(editable?msgsender1:msgsender2));
		btn = (ApiButton) view.findViewById(R.id.jobrvbtn);
		btn.setVisibility(editable?View.VISIBLE:View.GONE);
		rate.setVisibility(View.VISIBLE);
		rate.setIsIndicator(!editable);
		rate.setRating(Shared.details.rate);
		if(editable){
			rate.setOnRatingBarChangeListener(this);
			if(Shared.details.rate == 0) rate.setRating(1);
		}
		text.setVisibility(View.VISIBLE);
		text.getTextBox().setEnabled(editable);
		text.getTextBox().setTextColor(Color.DKGRAY);
		if(Shared.details.review!=null) text.setText(Shared.details.review);
		return view;
    }
    
    @Override public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
    	if(rating < 1) ratingBar.setRating(1f);
    	else if(rating > 5) ratingBar.setRating(5f);
    }
    
    @Override public void onInvisible() {
    	super.onInvisible();
    	if(request!=null) request.abort();
    }
    
    public void submit(View view){
    	HashMap<String,Object> params = new HashMap<String, Object>();
    	String review = String.valueOf(text.getText().toString());
    	params.put("loginkey", Shared.loginkey);
    	params.put("jobid", String.valueOf(Shared.details.id));
		params.put("rate", String.valueOf(String.valueOf(rate.getRating())));
		if(review.length()!=0) params.put("review", review);
		request = SimpleHttp.request("review", Shared.url+"jobs/postreview", params, this);
		btn.setBusy(true);
    }
    
	public void notified(int code, JSONObject data){
		try{
			Shared.details.rate = data.getInt("job_rate");
			if(!data.isNull("review")) Shared.details.review = data.getString("review");
			msg.setText("Congratulation. Your job is done. This is your job review by the sender");
			rate.setVisibility(View.VISIBLE);
			rate.setIsIndicator(true);
			rate.setRating(Shared.details.rate);
			text.setVisibility(View.VISIBLE);
			text.getTextBox().setEnabled(false);
			text.getTextBox().setTextColor(Color.DKGRAY);
			if(Shared.details.review!=null) text.setText(Shared.details.review);
		}catch(JSONException ex){}
	}

	@Override public void handle(String tag, String result, String url, HashMap<String, Object> params, ArrayList<String> files) {
		btn.setBusy(false);
		if(result==null){
			Toast.makeText(activity, "Unexpected error", Toast.LENGTH_SHORT).show();
			return;
		}
		try{
			JSONObject json = new JSONObject(result);
			if(json.getInt("success")==1){
				Toast.makeText(activity, "Your job review successfully sent", Toast.LENGTH_SHORT).show();
				activity.finish();
			}
			else Toast.makeText(activity, "Unexpected error. Please retry", Toast.LENGTH_SHORT).show();
		}catch(JSONException ex){
			Toast.makeText(activity, "Unexpected result", Toast.LENGTH_SHORT).show();
		}
	}
}