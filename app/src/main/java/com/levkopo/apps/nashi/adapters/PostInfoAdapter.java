package com.levkopo.apps.nashi.adapters;
import android.os.Bundle;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.activities.BaseActivity;
import com.levkopo.apps.nashi.fragments.UserFragment;
import com.levkopo.apps.nashi.fragments.groups.GroupFragment;
import com.levkopo.apps.nashi.link.DefaultActionListener;
import com.levkopo.apps.nashi.link.OwnerLinkSpanFactory;
import com.levkopo.apps.nashi.models.CommentModel;
import com.levkopo.apps.nashi.models.GroupModel;
import com.levkopo.apps.nashi.models.PostItem;
import com.levkopo.apps.nashi.models.UserModel;
import com.levkopo.apps.nashi.utils.GlideUtils;
import java.util.ArrayList;
import java.util.List;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.levkopo.apps.nashi.utils.DisplayUtils;
import com.levkopo.apps.nashi.widget.Avatar;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;
import com.levkopo.apps.nashi.widget.ExpandableTextView;
import android.text.util.Linkify;
import com.levkopo.apps.nashi.widget.AttachmentsView;
import com.levkopo.apps.nashi.models.Attachments;

public class PostInfoAdapter extends RecyclerView.Adapter
{
	public ArrayList<Object> list = new ArrayList<>();

	private BaseFragment fragment;

	public PostInfoAdapter(BaseFragment fragment, ArrayList<Object> list) {
		this.list = list;
		this.fragment = fragment;
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
		if(list.get(p2) instanceof PostItem){
			return new WallAdapter.ViewHolder(LayoutInflater.from(p1.getContext()));
		}else if(list.get(p2) instanceof CommentModel){
			return new ViewHolder(LayoutInflater.from(p1.getContext()));
		}
		return null;
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder p1, int p2) {
		if(p1 instanceof WallAdapter.ViewHolder){
			WallAdapter.ViewHolder viewholder = (WallAdapter.ViewHolder) p1;
			viewholder.bind((PostItem) list.get(p2));
		}else if(p1 instanceof ViewHolder){
			ViewHolder viewholder = (PostInfoAdapter.ViewHolder) p1;
			viewholder.bind((CommentModel) list.get(p2));
		}
	}

	@Override
	public int getItemViewType(int position) {
		return position;
	}

	@Override
	public int getItemCount() {
		return list.size();
	}
	
	public class ViewHolder extends RecyclerView.ViewHolder{
		
		public LinearLayout content;
		
		public TextView name;
		
		public ExpandableTextView text;
		
		public Avatar image;
		
		public AttachmentsView attachments;
		
		public ArrayList<CommentModel> thread = new ArrayList<>();

		private RecyclerView thread_list;
		
		public ViewHolder(LayoutInflater inflater){
			super(new LinearLayout(inflater.getContext()));
			content = (LinearLayout) itemView;
			content.addView(inflater.inflate(R.layout.comment_item, null));
			content.setOrientation(LinearLayout.VERTICAL);
			image = itemView.findViewById(R.id.image);
			name = itemView.findViewById(R.id.name);
			text = itemView.findViewById(R.id.text);
			attachments = itemView.findViewById(R.id.attachments);
			thread_list = new RecyclerView(name.getContext());
			thread_list.setLayoutManager(new LinearLayoutManager(name.getContext()));
			thread_list.setAdapter(new PostInfoAdapter(fragment, (ArrayList<Object>) thread));
			content.addView(thread_list);
			thread_list.setPadding((int) DisplayUtils.convertDpToPixel(15, name.getContext()) ,0 , 0, 0);
		}
		
		public void bind(final CommentModel item){
			String title = "DELETED";
			String image_url = "";
			if(item.author instanceof GroupModel){
				GroupModel model = (GroupModel) item.author;
				title = model.name;
				image_url = model.photo_100;
			}else if(item.author instanceof UserModel){
				UserModel model = (UserModel) item.author;
				title = model.first_name + " " + model.last_name;
				image_url = model.photo_100;
			}
			if(image_url!=null)
				image.setImage(image_url);
			
			name.setText(title);
			image.setOnClickListener(new OnClickListener(){
					@Override public void onClick(View p1) {
						BaseFragment fragment_;
						if(item.from_id>0){
							fragment_ = new UserFragment();
							Bundle arg = new Bundle();
							arg.putInt("id", item.from_id);
							fragment_.setArguments(arg);
						}else{
							fragment_ = new GroupFragment();
							Bundle arg = new Bundle();
							arg.putInt("group_id", item.from_id*-1);
							fragment_.setArguments(arg);
						}
						fragment.open(fragment_);
					}});
					
			Spannable t = OwnerLinkSpanFactory.withSpans(item.text, true, true, new DefaultActionListener(fragment));
			Linkify.addLinks(t, Linkify.WEB_URLS);
			text.setText(t);
			
			if(item.attachments!=null&&item.attachments.size()!=0){
				List<Attachments.Attachment> photos = item.attachments.getAttachmentsByType("photo");
				if(!photos.isEmpty())
					attachments.addPhotoArray(photos);

				List<Attachments.Attachment> audios = item.attachments.getAttachmentsByType("audio");
				if(!audios.isEmpty())
					attachments.addAudiosArray(audios);
			}else
				content.removeView(attachments);
			
			if(item.thread.size()!=0){
				thread = item.thread;
				thread_list.getAdapter().notifyDataSetChanged();
			}
		}
	}
}
