package my.yousendit.fragments;

import my.helper.MyFragment;
import my.yousendit.R;
import my.yousendit.peripherals.Shared;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Receiver extends MyFragment{
	
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	super.onCreateView(inflater, container, savedInstanceState);
    	View view = inflater.inflate(R.layout.receiver, null);
		((TextView) view.findViewById(R.id.receiverpin)).setText(Shared.details.pin);
    	return view;
    }
    
    public void share(View view){
    	Intent sendIntent = new Intent();
    	sendIntent.setAction(Intent.ACTION_SEND);
    	String txt = "";
    	txt += "You have an incoming delivery from this address.\n\n"+Shared.details.addrfrom+"\n\n";
    	txt += "Once you receive the item, please pass this PIN code to the transporter.\n\n"+Shared.details.pin;
    	sendIntent.putExtra(Intent.EXTRA_TEXT, txt);
    	sendIntent.setType("text/plain");
    	startActivity(sendIntent);
    }
}