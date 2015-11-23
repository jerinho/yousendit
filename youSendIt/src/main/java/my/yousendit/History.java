package my.yousendit;

import android.os.Bundle;
import android.view.MenuItem;
import my.helper.Fragmentivity;
import my.yousendit.fragments.MyJob;
import my.yousendit.peripherals.Shared;

public class History extends Fragmentivity{
	
	protected void onCreate(Bundle args) {
		super.onCreate(args);
		if(Shared.id==0) System.exit(0);
		setSwipable(false);
		setActionBar(true);
		setTabBar(true);
		setStateBar(false);
        MyJob posted = new MyJob();
        posted.setAsSender(true);
        posted.setIsActiveJobs(false);
        MyJob awarded = new MyJob();
        awarded.setAsSender(true);
        awarded.setIsActiveJobs(false);
		add("posted",posted,"Posted Jobs");
		add("awarded",awarded,"Accepted Jobs");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(android.R.color.transparent);
	}
	@Override protected void onResume() {
		super.onResume();
		Shared.screen = this;
	}
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) finish();
		return super.onOptionsItemSelected(item);
	}
}