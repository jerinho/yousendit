package my.helper;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

public class MyFragment extends Fragment{
	
	protected Fragmentivity activity;
	boolean afterresume, visible;
	
	public MyFragment() {
		super();
	}
	
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.activity = (Fragmentivity) getActivity();
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override public void setUserVisibleHint(boolean visible) {
		super.setUserVisibleHint(visible);
		this.visible = visible;
		afterresume = false;
		if(getView()==null) afterresume = true;
		else if(!visible) onInvisible();
		else onVisible();
	}
	
	@Override public void onResume() {
		super.onResume();
		if(visible&&afterresume) onVisible();
	}
	
	public void onVisible(){}
	public void onInvisible(){}
}