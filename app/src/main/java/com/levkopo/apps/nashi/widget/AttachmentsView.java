package com.levkopo.apps.nashi.widget;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.bumptech.glide.Glide;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.adapters.AudiosListAdapter;
import com.levkopo.apps.nashi.models.Attachments;
import com.levkopo.apps.nashi.models.AudioModel;
import com.levkopo.apps.nashi.models.VKPhoto;
import java.util.ArrayList;
import java.util.List;
import androidx.recyclerview.widget.GridLayoutManager;
import com.levkopo.apps.nashi.models.VKPhoto.Size;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.levkopo.apps.nashi.utils.GlideUtils;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.RequestBuilder;
import android.graphics.drawable.Drawable;
import com.levkopo.apps.nashi.models.PollModel;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;

public class AttachmentsView extends LinearLayout
{
	public ArrayList<VKPhoto> photos = new ArrayList<>();

	public ArrayList<AudioModel> audios = new ArrayList<>();
	
	private RecyclerView audios_list;

	private RecyclerView photos_list;

	private GridLayoutManager photos_lm;

	private PollView pollView;
	
	public AttachmentsView(Context ctx){
		super(ctx);
		init();
	}
	
	public AttachmentsView(Context ctx, AttributeSet attrs){
		super(ctx, attrs);
		init();
	}
	
	public AttachmentsView(Context ctx, AttributeSet attrs, int p){
		super(ctx, attrs, p);
		init();
	}
	
	public void addPhotoArray(List<Attachments.Attachment> photos){
		this.photos.clear();
		for(Attachments.Attachment att: photos){
			this.photos.add(new VKPhoto(att.object));
		}
		
		if(this.photos.size()<=5)
			photos_lm.setSpanCount(this.photos.size());
						
		photos_list.getAdapter().notifyDataSetChanged();
	}
	
	public void addPoll(PollModel poll){
		pollView.bindPoll(poll);
	}
	
	public void removePoll(){
		removeView(pollView);
	}
	
	public void addAudiosArray(List<Attachments.Attachment> audio){
		audios.clear();
		for(Attachments.Attachment att: audio){
			audios.add(new AudioModel(att.object));
		}
		audios_list.getAdapter().notifyDataSetChanged();
	}
	
	public void init(){
		setOrientation(VERTICAL);
	    photos_list = new RecyclerView(getContext());
		photos_lm = new GridLayoutManager(getContext(), 3);
		photos_list.setLayoutManager(photos_lm);
		photos_list.setAdapter(new PhotoAdapter(photos));
		addView(photos_list);
		
		audios_list = new RecyclerView(getContext());
		audios_list.setLayoutManager(new LinearLayoutManager(getContext()));
		audios_list.setAdapter(new AudiosListAdapter(audios));
		addView(audios_list);
		
		pollView = new PollView(getContext());
		addView(pollView);
	}
	
	public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

		public ArrayList<VKPhoto> photos = new ArrayList<>();

		private VKPhoto.Size size;

		private int span_width;

		public PhotoAdapter(ArrayList<VKPhoto> photos) {
			this.photos = photos;
		}
		
		@Override
		public AttachmentsView.PhotoAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
			span_width = photos_lm.getWidth()/photos_lm.getSpanCount();
			return new ViewHolder(p1.getContext());
		}

		@Override
		public void onBindViewHolder(AttachmentsView.PhotoAdapter.ViewHolder p1, int p2) {
			VKPhoto photo = photos.get(p2);
			size = photo.sizes.getSizeByType("x");	
			RequestBuilder<Drawable> load_image = Glide.with(p1.image)
				.load(size.url);
			
			if(photos.size()>1)
				load_image = load_image.apply(new RequestOptions().override(span_width, span_width));
			else
				p1.image.setLayoutParams(new RecyclerView.LayoutParams(
					RecyclerView.LayoutParams.MATCH_PARENT, 
					RecyclerView.LayoutParams.WRAP_CONTENT
				));
			
			load_image.centerCrop()
				.fitCenter()
				.transition(DrawableTransitionOptions.with(new GlideUtils.DrawableAlwaysCrossFadeFactory()))
				.placeholder(new ColorDrawable(p1.image.getContext().getResources().getColor(R.color.gray)))
				.into(p1.image);
		}

		@Override
		public int getItemCount() {
			return photos.size();
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ImageView image;
			
			public ViewHolder(Context ctx){
				super(new ImageView(ctx));
				image = (ImageView) itemView;
				image.setLayoutParams(new RecyclerView.LayoutParams(
					span_width, 
					span_width
				));
				image.setAdjustViewBounds(true);
			}
		}
	}
}
