package my.helper;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ListView;

public class OverScrollListView extends ListView implements OnTouchListener{
	
	int drag;
	OverScrollListener listener;
	
	public OverScrollListView(Context context) {
		super(context);
		setOnTouchListener(this);
	}
	
	public void setListener(OverScrollListener listener) {
		this.listener = listener;
	}

	public OverScrollListView(Context context, AttributeSet attrs) {
		super(context,attrs);
		setOnTouchListener(this);
	}
	
	public OverScrollListView(Context context, AttributeSet attrs, int defstyle) {
		super(context,attrs,defstyle);
		setOnTouchListener(this);
	}
	
	@Override protected boolean overScrollBy(int dx, int dy, int sx, int sy, int srx, int sry, int mosx, int mosy, boolean touch){
		if(dy<0) drag -= dy;
		else if(dy>0) drag += dy;
		return true;
	}
	
	@Override protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
		super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
	}
	
	@Override public boolean onTouch(View v, MotionEvent event) {
		if(event.getAction() == android.view.MotionEvent.ACTION_UP){
			if(drag>100){
				if(getFirstVisiblePosition()==0) listener.onOverScroll(true);
				else if(getLastVisiblePosition()==(getCount()-1)) listener.onOverScroll(false);
			}
			drag = 0;
		}
		return false;
	}
	
	public interface OverScrollListener{

		void onOverScroll(boolean istop);
		
	}
}