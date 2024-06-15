package com.levkopo.ui.widgets.ultilist;
import android.view.View;
import android.content.Context;
import com.levkopo.ui.widgets.UltiList;
import android.view.View.OnClickListener;

public abstract class BaseListItem
{
	public View itemView;
	public UltiList.OnClickListener onClick;
	
	public abstract View createView(Context ctx);
	public abstract void bind(Context ctx);
}
