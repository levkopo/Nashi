package com.levkopo.ui.widgets;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.levkopo.ui.widgets.ultilist.BaseListItem;

import java.util.ArrayList;

public class UltiList extends RecyclerView.Adapter<UltiList.ViewHolder>
{
	public OnClickListener onClickListener;
	
	public ArrayList<BaseListItem> items;
	
	public UltiList(ArrayList<BaseListItem> items) {
		this.items = items;
	}

	@Override
	public UltiList.ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
		View view = items.get(p2).createView(p1.getContext());
		RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		view.setLayoutParams(lp);
		view.setOnClickListener(new OnClickItem(items.get(p2)));
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(UltiList.ViewHolder p1, int p2) {
	 	BaseListItem item = items.get(p2);
		item.itemView = p1.itemView;
		item.onClick = this.onClickListener;
		item.bind(p1.itemView.getContext());
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public int getItemViewType(int position) {
		Log.wtf("UILib", "Pos: "+position);
		return position;
	}
	
	public static UltiList generate(RecyclerView rv, ArrayList<BaseListItem> items){
		UltiList list = new UltiList(items);
		rv.setAdapter(list);
		return list;
	}
	
	public void setOnClickListener(OnClickListener onClickListener) {
		this.onClickListener = onClickListener;
	}

	public OnClickListener getOnClickListener() {
		return onClickListener;
	}
	
	public class OnClickItem implements View.OnClickListener {
		
		public BaseListItem item;

		public OnClickItem(BaseListItem item) {
			this.item = item;
		}
		
		@Override
		public void onClick(View p1){                                                        
			if(onClickListener!=null){
				onClickListener.onClick(item);
			}
		}		
	}
	
	public class ViewHolder extends RecyclerView.ViewHolder{
		public ViewHolder(View view){
			super(view);
		}
	}
	
	public interface OnClickListener{
		void onClick(BaseListItem item);
	}
}
