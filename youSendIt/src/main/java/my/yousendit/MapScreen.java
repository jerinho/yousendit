package my.yousendit;

import android.os.Bundle;
import my.helper.Map;
import my.yousendit.peripherals.Shared;

public class MapScreen extends Map{
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getActionBar().setIcon(android.R.color.transparent);
	}
	
	@Override protected void onResume() {
		super.onResume();
		Shared.screen = this;
	}
}
