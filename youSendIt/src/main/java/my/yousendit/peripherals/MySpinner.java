package my.yousendit.peripherals;

import my.yousendit.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class MySpinner extends LinearLayout implements OnItemSelectedListener, OnClickListener{
	
	TextView label;
	Spinner droplist;
	String vallabel, valtext;
	Context context;
	EditText text;
	ImageView icon;
	ArrayAdapter<String> list;
	RelativeLayout drophead;
	boolean selected, showlabel;

	public MySpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}
	public MySpinner(Context context, AttributeSet attrs){
		super(context, attrs);
		init(context, attrs);
	}
	public MySpinner(Context context) {
		super(context);
		init(context, null);
	}
	
	public void init(Context context, AttributeSet attrs){
		this.context = context;
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,R.styleable.MyTextBox,0, 0);
		setOrientation(LinearLayout.VERTICAL);
        try{
	        vallabel = a.getString(R.styleable.MyTextBox_label);
	        valtext = a.getString(R.styleable.MyTextBox_text);
	        showlabel = a.getBoolean(R.styleable.MyTextBox_showlabel, true);
        }finally{
        	a.recycle();
        }
        label = new TextView(context);
		label.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		label.setVisibility(View.INVISIBLE);
		label.setTextColor(Color.DKGRAY);
    	if(vallabel!=null) label.setText(vallabel);
        text = new EditText(context);
        android.widget.RelativeLayout.LayoutParams textparams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		text.setLayoutParams(textparams);
    	text.setTextSize(TypedValue.COMPLEX_UNIT_SP,16f);
    	//text.setTypeface(Typeface.SANS_SERIF,Typeface.NORMAL);
		text.setBackground(getResources().getDrawable(R.drawable.selectortextview));
		text.setPadding(0, 0, 0, 0);
		text.setFocusable(false);
		text.setClickable(false);
		text.setOnClickListener(this);
        icon = new ImageView(context);
        icon.setImageDrawable(getResources().getDrawable(R.drawable.icon_dropdown));
        android.widget.RelativeLayout.LayoutParams iconparams = new RelativeLayout.LayoutParams(35,35);
        iconparams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        iconparams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        iconparams.setMargins(0, 5, 0, 0);
        icon.setLayoutParams(iconparams);
		if(vallabel!=null) text.setHint(vallabel);
    	droplist = new Spinner(context){
    		
    		@Override public void setSelection(int position) {
    			super.setSelection(position);
    			if(!selected) return;
    			label.setVisibility(showlabel?View.VISIBLE:View.INVISIBLE);
    			if(text!=null) text.setText(getSelectedItem().toString());
    			if(listener!=null) listener.itemSelected(position);
    		}
    	};
    	droplist.setVisibility(View.GONE);
    	if(vallabel!=null) droplist.setPrompt(vallabel);
		drophead = new RelativeLayout(context);
		drophead.addView(text);
		drophead.addView(icon);
		addView(label);
		addView(drophead);
		addView(droplist);
	}
	
	@Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		//System.out.println("Droplist offset : "+label.getHeight()+"+"+drophead.getHeight());
		droplist.setDropDownVerticalOffset(label.getHeight()+drophead.getHeight());
		text.setClickable(true);
	}
	
	public int getSelectedItemPosition(){
		return droplist.getSelectedItemPosition();
	}
	
	public EditText getHead(){
		return text;
	}
	
	public void setAdapter(ArrayAdapter<String> adapter){
		list = adapter;
		droplist.setAdapter(adapter);
	}

	public void setSelection(int selection){
		droplist.setSelection(selection);
	}
	
	@Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
	}
	@Override public void onNothingSelected(AdapterView<?> parent) {
	}
	@Override public void onClick(View v) {
		selected = true;
		droplist.performClick();
	}
	public void pushSelection(int i) {
		selected = true;
		droplist.setSelection(i);
	}
	public void setOnItemSelectedListener(ItemSelectedListener listener){
		this.listener = listener;
	}
	
	public ItemSelectedListener listener;
	
	public static interface ItemSelectedListener{
		
		public void itemSelected(int position);
	}
}