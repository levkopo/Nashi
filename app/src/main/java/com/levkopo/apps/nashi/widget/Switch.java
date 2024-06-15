package com.levkopo.apps.nashi.widget;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.SwitchCompat;
import com.levkopo.apps.nashi.R;

public class Switch extends SwitchCompat 
{
	public Switch(Context ctx){
		super(ctx);
		init();
	}
	
	public Switch(Context ctx, AttributeSet as){
		super(ctx, as);
		init();
	}
	
	public Switch(Context ctx, AttributeSet as, int i){
		super(ctx, as, i);
		init();
	}
	
	public void init(){
		setThumbResource(R.drawable.switch_thumb_selector);
		setTrackResource(R.drawable.switch_track_selector);
	}
}
