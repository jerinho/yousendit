package my.yousendit.peripherals;

import my.yousendit.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MyTextBox extends LinearLayout implements TextWatcher{
	
	TextView label;
	EditText edit;
	String vallabel, valtext;
	boolean showlabel;

	public MyTextBox(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}
	public MyTextBox(Context context, AttributeSet attrs){
		super(context, attrs);
		init(context, attrs);
	}
	public MyTextBox(Context context) {
		super(context);
		init(context, null);
	}
	
	public void init(Context context, AttributeSet attrs){
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,R.styleable.MyTextBox,0, 0);
		setOrientation(LinearLayout.VERTICAL);
        try{
	        vallabel = a.getString(R.styleable.MyTextBox_label);
	        valtext = a.getString(R.styleable.MyTextBox_text);
	        showlabel = a.getBoolean(R.styleable.MyTextBox_showlabel, true);
	        //type = a.getInt(R.styleable.MyTextBox_type, InputType.TYPE_NULL);
        }finally{
        	a.recycle();
        }
		//View view = LayoutInflater.from(context).inflate(R.layout.mytextbox, null);
		//addView(view);
		//label = (TextView) view.findViewById(R.id.mytextboxlabel);
        label = new TextView(context);
		label.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		label.setVisibility(View.INVISIBLE);
		label.setTextColor(Color.DKGRAY);
        addView(label);
	}
	
	@Override protected void onFinishInflate() {
		super.onFinishInflate();
    	edit = (EditText) getChildAt(1);
    	edit.setTextSize(TypedValue.COMPLEX_UNIT_SP,16f);
    	//edit.setTypeface(Typeface.SANS_SERIF,Typeface.NORMAL);
		edit.addTextChangedListener(this);
		edit.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		edit.setPadding(0, 0, 0, 0);
		edit.setBackground(getResources().getDrawable(R.drawable.selectortextview));
    	if(vallabel!=null) setLabel(vallabel);
    	if(valtext!=null) setText(valtext);
	}
		
	public void setText(String text){
		edit.setText(text);
	}
	
	public EditText getTextBox(){
		return edit;
	}
	
	public void setLabel(String label){
		this.label.setText(label);
		edit.setHint(label);
	}

	public Editable getText() {
		return edit.getText();
	}

	@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}
	@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
	}
	@Override public void afterTextChanged(Editable s) {
		label.setVisibility(s.length()!=0&&showlabel?View.VISIBLE:View.INVISIBLE);
	}
}