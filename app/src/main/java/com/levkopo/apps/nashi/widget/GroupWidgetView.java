package com.levkopo.apps.nashi.widget;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.text.HtmlCompat;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.models.widgets.BaseWidget;
import com.levkopo.apps.nashi.models.widgets.TextWidget;
import com.levkopo.apps.nashi.utils.DisplayUtils;
import android.view.View;
import com.levkopo.apps.nashi.models.widgets.ListWidget;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import android.view.ViewGroup;
import com.levkopo.apps.nashi.models.widgets.items.ListItem;
import java.util.ArrayList;
import androidx.appcompat.widget.AppCompatButton;
import com.levkopo.apps.nashi.utils.OnActionClickListener;
import com.levkopo.apps.nashi.models.widgets.DonationWidget;
import android.widget.Button;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.levkopo.apps.nashi.models.widgets.items.CoverItem;
import com.levkopo.apps.nashi.models.widgets.CoverWidget;

public class GroupWidgetView extends CardView {

	private LinearLayout layout;

	private BaseWidget widget;

	private TextView title;

	private TextView more;
	public GroupWidgetView(Context ctx){
		super(ctx);
		init();
	}

	public GroupWidgetView(Context ctx, AttributeSet attrs){
		super(ctx, attrs);
		init();
	}

	public GroupWidgetView(Context ctx, AttributeSet s, int i){
		super(ctx, s, i);
		init();
	}

	private void init() {
		layout = new LinearLayout(getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		setUseCompatPadding(true);
		addView(layout);
	}
	
	public void loadWidget(BaseWidget widget){
		layout.removeAllViews();
		int padding = (int) DisplayUtils.convertDpToPixel(10, getContext());
		
		this.widget = widget;
		FrameLayout top = new FrameLayout(getContext());
		top.setPadding(padding, padding, padding, padding);
		
		if(widget.title_action!=null)
			title = new TextView(new ContextThemeWrapper(getContext(), R.style.Text_Primary));
		else
			title = new TextView(new ContextThemeWrapper(getContext(), R.style.Text_Secondary));
		
		title.setText(widget.title);
		title.setSingleLine(true);
		if(widget.title_counter!=0){
			title.setText(HtmlCompat.fromHtml(
				widget.title + " <small>" + widget.title_counter + "</small>",
				HtmlCompat.FROM_HTML_MODE_LEGACY
				)
			);
		}
		
		if(widget.title_action!=null)
			title.setOnClickListener(new OnActionClickListener(getContext(), widget.title_action));
		
		top.addView(title);
		
		if(widget.more!=null){
			more = new TextView(new ContextThemeWrapper(getContext(), R.style.Text_Primary));
			more.setText(widget.more);
			more.setOnClickListener(new OnActionClickListener(getContext(), widget.more_action));
			top.addView(more);
			more.setGravity(Gravity.RIGHT);
		}
		
		layout.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		if(widget instanceof TextWidget){
			TextWidget wid = (TextWidget) widget;
			LinearLayout textContainer = new LinearLayout(getContext());
			textContainer.setPadding(padding, padding, padding, padding);
			textContainer.setOrientation(LinearLayout.VERTICAL);
			textContainer.setGravity(Gravity.CENTER_HORIZONTAL);
			
			TextView text = new TextView(new ContextThemeWrapper(getContext(), R.style.Text_Secondary_Large));
			text.setText(wid.text);
			
			TextView descr = new TextView(new ContextThemeWrapper(getContext(), R.style.Text_Secondary_Small));
			
			if(wid.descr!=null)
				descr.setText(wid.descr);
			
			textContainer.addView(text);
			if(wid.descr!=null)
				textContainer.addView(descr);
				
			layout.addView(textContainer);
		}else if(widget instanceof ListWidget){
			RecyclerView rows_list = new RecyclerView(getContext());
			rows_list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
			rows_list.setAdapter(new ListWidgetAdapter(((ListWidget) widget).rows));
			layout.addView(rows_list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}else if(widget instanceof CoverWidget){
			RecyclerView rows_list = new RecyclerView(getContext());
			rows_list.setPadding(padding, padding, padding, padding);
			rows_list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
			rows_list.setAdapter(new CoverAdapter(((CoverWidget) widget).rows));
			layout.addView(rows_list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}else if(widget instanceof DonationWidget){
			DonationWidget donat = (DonationWidget) widget;
			
			View view = inflate(getContext(), R.layout.group_widget_donation, null);
			TextView text = view.findViewById(R.id.text);
			TextView goal = view.findViewById(R.id.goal);
			Button donate_btn = view.findViewById(R.id.donate_btn);
			LinearLayout.LayoutParams donate_btn_params = (LinearLayout.LayoutParams) donate_btn.getLayoutParams();
			donate_btn_params.rightMargin = padding;
			donate_btn_params.leftMargin = donate_btn_params.rightMargin;
			
			CircularProgressBar donate_progress = view.findViewById(R.id.process);
			
			text.setText(donat.text);
			if(donat.text_action!=null)
				text.setOnClickListener(new OnActionClickListener(getContext(), donat.text_action));
				
			goal.setText(donat.funded+"/"+donat.goal+donat.currency.getSymbol());
			donate_btn.setOnClickListener(new OnActionClickListener(getContext(), donat.button_action));
			
			donate_progress.setMax(donat.goal);
			donate_progress.setProgress(donat.funded);
			
			layout.addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
	}
		
	public class ListWidgetAdapter extends RecyclerView.Adapter<ListWidgetAdapter.ViewHolder> {

		public ArrayList<ListItem> items = new ArrayList<>();

		public ListWidgetAdapter(ArrayList<ListItem> items) {
			this.items = items;
		}
		
		@Override
		public GroupWidgetView.ListWidgetAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
			return new ViewHolder(p1.getContext());
		}

		@Override
		public void onBindViewHolder(GroupWidgetView.ListWidgetAdapter.ViewHolder p1, int p2) {
			p1.bind(items.get(p2));
		}

		@Override
		public int getItemCount() {
		 	return items.size();
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {

			private LinearLayout content;
			
			private LinearLayout right;

			private TextView title;

			private TextView text;

			private Button button;

			private TextView address;

			private TextView time;

			private ImageView icon;
			
			public ViewHolder(Context ctx){
				super(new LinearLayout(ctx));
				RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				itemView.setLayoutParams(lp);
				
				int padding = (int) DisplayUtils.convertDpToPixel(10, getContext());
				int icon_size = (int) DisplayUtils.convertDpToPixel(50, getContext());
				
				content = (LinearLayout) itemView;
				content.setOrientation(LinearLayout.HORIZONTAL);
				content.setPadding(padding, padding, padding, padding);
				
				icon = new ImageView(getContext());
				content.addView(icon, icon_size, icon_size);
				LinearLayout.LayoutParams icon_params = (LinearLayout.LayoutParams) icon.getLayoutParams();
				icon_params.rightMargin = (int) DisplayUtils.convertDpToPixel(10, getContext());
				
				right = new LinearLayout(ctx);
				right.setOrientation(LinearLayout.VERTICAL);
				
				this.title = new TextView(new ContextThemeWrapper(getContext(), R.style.Text_Secondary));
				address = new TextView(new ContextThemeWrapper(getContext(), R.style.Text_Secondary_Small));
				time = new TextView(new ContextThemeWrapper(getContext(), R.style.Text_Secondary_Small));
				
				text = new TextView(new ContextThemeWrapper(getContext(), R.style.Text_Secondary_Small));
				button = new Button(new ContextThemeWrapper(getContext(), R.style.Widget_MaterialComponents_Button_OutlinedButton));
				
				right.addView(this.title);
				right.addView(address);
				right.addView(time);
				right.addView(text);
				right.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				
				content.addView(right, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			}

			public void bind(ListItem get) {
				this.title.setText(get.title);
				if(get.title_action!=null)
					this.title.setOnClickListener(new OnActionClickListener(getContext(), get.title_action));
				
				if(get.address!=null){
					address.setText(get.address);
				}else{
					right.removeView(address);
				}
				
				if(get.time!=null){
					time.setText(get.time);
				}else{
					right.removeView(time);
				}
					
				if(get.text!=null){
					text.setText(get.text);
				}else{
					right.removeView(text);
				}
				
				RequestOptions ro = new RequestOptions();
				ro = ro.transform(new CenterCrop(), new RoundedCorners((int) getResources().getDimension(R.dimen.widget_image_round)));
				
				if(get.icons!=null)
					Glide.with(getContext())
						.load(get.icons[0].url)
						.apply(ro)
						.into(icon);
				
				if(get.button!=null){
					button.setText(get.button);
					button.setOnClickListener(new OnActionClickListener(getContext(), get.button_action));
				}else{
					right.removeView(button);
				}
			}
		}
	}
	
	public class CoverAdapter extends RecyclerView.Adapter<CoverAdapter.ViewHolder> {

		private ArrayList<CoverItem> rows = new ArrayList<>();

		public CoverAdapter(ArrayList<CoverItem> rows) {
			this.rows = rows;
		}
		
		@Override
		public GroupWidgetView.CoverAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
			return new ViewHolder(p1.getContext());
		}

		@Override
		public void onBindViewHolder(GroupWidgetView.CoverAdapter.ViewHolder p1, int p2) {
			p1.bind(rows.get(p2));
		}

		@Override
		public int getItemCount() {
			return rows.size();
		}
		
		
		public class ViewHolder extends RecyclerView.ViewHolder {

			private LinearLayout container;

			private ImageView cover;
			
			private TextView title;
			
			private TextView descr;
			
			private LinearLayout text_container;

			private LinearLayout bottom_layout;

			private Button button;
			
			public ViewHolder(Context ctx){
				super(new LinearLayout(ctx));
				RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				itemView.setLayoutParams(lp);

				container = (LinearLayout) itemView;
				container.setOrientation(LinearLayout.VERTICAL);
				cover = new ImageView(getContext());
				container.addView(cover, ViewGroup.LayoutParams.MATCH_PARENT, (int) getResources().getDimension(R.dimen.widget_cover_height));
				
				bottom_layout = new LinearLayout(getContext());
				bottom_layout.setOrientation(LinearLayout.HORIZONTAL);
				
				text_container = new LinearLayout(getContext());
				text_container.setOrientation(LinearLayout.VERTICAL);
				
				title = new TextView(new ContextThemeWrapper(getContext(), R.style.Text_Secondary));
				descr = new TextView(new ContextThemeWrapper(getContext(), R.style.Text_Secondary_Small));
				title.setSingleLine(true);
				descr.setSingleLine(true);
				
				text_container.addView(title);
				text_container.addView(descr);
				
				bottom_layout.addView(text_container, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				text_container.setWeightSum(1f);
				
				button = new Button(new ContextThemeWrapper(getContext(), R.style.Widget_MaterialComponents_Button_OutlinedButton));
				bottom_layout.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				
				container.addView(bottom_layout);
			}

			public void bind(CoverItem get) {
				RequestOptions ro = new RequestOptions();
				ro = ro.transform(new CenterCrop(), new RoundedCorners((int) getResources().getDimension(R.dimen.widget_image_round)));
				
				Glide.with(getContext())
					.load(get.cover[0].url)
					.apply(ro)
					.into(cover);
					
				if(get.title!=null){
					title.setText(get.title);
					if(get.descr!=null)
						descr.setText(get.descr);
				}else{
					container.removeView(text_container);
					if(get.button==null){
						container.removeView(bottom_layout);
					}
				}
				
				if(get.action!=null){
					itemView.setOnClickListener(new OnActionClickListener(getContext(), get.action));
				}
				
				if(get.button!=null){
					button.setText(get.button);
					button.setOnClickListener(new OnActionClickListener(getContext(), get.button_action));
				}
			}
		}
	}
}
