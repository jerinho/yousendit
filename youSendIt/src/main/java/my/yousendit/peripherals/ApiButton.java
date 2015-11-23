package my.yousendit.peripherals;

import my.yousendit.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ApiButton extends LinearLayout{
	
	String busy, free;
	TextView label;
	boolean isbusy;
	ProgressBar progress;
	
	public ApiButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}
	public ApiButton(Context context, AttributeSet attrs){
		super(context, attrs);
		init(context, attrs);
	}
	public ApiButton(Context context) {
		super(context);
		init(context, null);
	}
	
	public void init(Context context, AttributeSet attrs){
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,R.styleable.ApiButton,0, 0);
        try{
	        free = a.getString(R.styleable.ApiButton_free);
	        busy = a.getString(R.styleable.ApiButton_busy);
        }finally{
        	a.recycle();
        }
        setClickable(true);
        setBackgroundResource(R.drawable.selectorbuttons);
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setGravity(Gravity.CENTER);
        setOrientation(HORIZONTAL);
        //ViewCompat.setElevation(this, 1f);
        LinearLayout left = new LinearLayout(context);
        left.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
        left.setFocusable(false);
        left.setClickable(false);
        left.setOrientation(HORIZONTAL);
        LinearLayout right = new LinearLayout(context);
        right.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
        right.setFocusable(false);
        right.setClickable(false);
        right.setOrientation(HORIZONTAL);
        label = new Button(context);
        label.setText("Button");
    	label.setTextSize(TypedValue.COMPLEX_UNIT_SP,16f);
    	//label.setTypeface(Typeface.SANS_SERIF,Typeface.NORMAL);
        label.setFocusable(false);
        label.setClickable(false);
        label.setBackground(null);
		label.setGravity(Gravity.CENTER);
		label.setPadding(0, 0, 0, 0);
		label.setTextColor(Color.WHITE);
		label.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        progress = new ProgressBar(context);
        progress.setClickable(false);
        progress.setFocusable(false);
        addView(left);
        addView(label);
        addView(right);
        right.addView(progress);
		setBusy(false);
	}
	
	@Override protected void onFinishInflate() {
		super.onFinishInflate();
		if(free!=null) setFreeText(free);
		if(busy!=null) setBusyText(busy);
	}
	
	public void setFreeText(String text){
		free = text;
		if(isbusy) label.setText(free);
	}
	
	public void setBusyText(String text){
		busy = text;
		if(!isbusy) label.setText(free);
	}
	
	public void setBusy(boolean b) {
		isbusy = b;
		label.setText(b?busy:free);
		progress.setVisibility(b?View.VISIBLE:View.INVISIBLE);
		setEnabled(!b);
		//setCompoundDrawables(null, null, b?getResources().getDrawable(R.drawable.icon_mastercard):null, null);
	}
	
	public void setText(String text){
		label.setText(text);
	}
	
	public String getText(){
		return label.getText().toString();
	}
		
	@Override public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		setClickable(enabled);
	}
}