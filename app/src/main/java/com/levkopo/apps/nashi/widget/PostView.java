package com.levkopo.apps.nashi.widget;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.Spannable;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.levkopo.apps.nashi.Application;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.activities.BaseActivity;
import com.levkopo.apps.nashi.link.DefaultActionListener;
import com.levkopo.apps.nashi.link.OwnerLinkSpanFactory;
import com.levkopo.apps.nashi.models.GroupModel;
import com.levkopo.apps.nashi.models.PostItem;
import com.levkopo.apps.nashi.models.UserModel;
import com.levkopo.apps.nashi.utils.DisplayUtils;
import com.levkopo.apps.nashi.utils.GlideUtils;
import com.levkopo.vksdk.VKError;
import com.levkopo.vksdk.VKParameters;
import com.levkopo.vksdk.VKSdk;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.widget.FrameLayout;
import java.util.List;
import com.levkopo.apps.nashi.models.Attachments;
import com.levkopo.apps.nashi.utils.OwnerModelUtils;
import com.levkopo.apps.nashi.fragments.base.HostFragment;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;
import com.levkopo.apps.nashi.models.PollModel;

public class PostView extends LinearLayout
{
	
	public Avatar image;

	public TextView name;

	public TextView date;

	public ExpandableTextView text;

	public View like_btn;

	public ImageView like_icon;

	public TextView like_counter;

	public View repost_btn;

	public ImageView repost_icon;

	public TextView repost_counter;

	public AttachmentsView attachments;

	private Application app;

	private FrameLayout repost_container;
	
	private boolean repost_view_attached = false;

	private View header;

	private Avatar repost_avatar;

	private TextView repost_name;

	private TextView repost_text;
	
	private BaseFragment fragment;
	
	public PostView(Context ctx){
		super(ctx);
		init();
	}

	public PostView(Context ctx, AttributeSet attrs){
		super(ctx, attrs);
		init();
	}

	public PostView(Context ctx, AttributeSet attrs, int p){
		super(ctx, attrs, p);
		init();
	}

	public void setFragment(BaseFragment fragment) {
		this.fragment = fragment;
	}
	
	private void init() {
		app = (Application) getContext().getApplicationContext();
		setOrientation(VERTICAL);
		
		Resources.Theme theme = getContext().getTheme();
		TypedValue textColor = new TypedValue();
		theme.resolveAttribute(R.attr.textColor, textColor, true);
				
		//Header
	    header = inflate(getContext(), R.layout.post_item_header, null);
		image = header.findViewById(R.id.image);
		name = header.findViewById(R.id.name);
		date = header.findViewById(R.id.date);
		addView(header);

		//Attachments
		attachments = new AttachmentsView(getContext());
		addView(attachments);

		//Text
		int text_padding = (int) DisplayUtils.convertDpToPixel(15, getContext());
		int top_text_padding = (int) DisplayUtils.convertDpToPixel(5, getContext());
		text = new ExpandableTextView(getContext());
		text.setTextColor(textColor.data);
		text.setPadding(
			text_padding, 
			top_text_padding, 0, 
			text_padding
		);
		addView(text);
		
		//Repost
		int repost_padding = (int) DisplayUtils.convertDpToPixel(15, getContext());
		repost_container = new FrameLayout(getContext());
		repost_container.setPadding(
			repost_padding, 
			0, 0, 
			repost_padding
		);
		addView(repost_container);

		//Bottom panel
		View bottom_panel = inflate(getContext(), R.layout.post_item_bottom_panel, null);
		addView(bottom_panel);

		//Like button
		like_btn = bottom_panel.findViewById(R.id.like_btn);
		like_icon = bottom_panel.findViewById(R.id.like_icon);
		like_counter = bottom_panel.findViewById(R.id.like_counter);
		like_icon.getDrawable().setTint(Color.parseColor("#909090"));
		like_counter.setTextColor(Color.parseColor("#909090"));
		//like_btn.setOnTouchListener(new OnTouchListener(like_btn));

		repost_btn = bottom_panel.findViewById(R.id.repost_btn);
		repost_icon = bottom_panel.findViewById(R.id.repost_icon);
		repost_counter = bottom_panel.findViewById(R.id.repost_counter);
		repost_icon.getDrawable().setTint(Color.parseColor("#909090"));
		repost_counter.setTextColor(Color.parseColor("#909090"));
		//repost_btn.setOnTouchListener(new OnTouchListener(repost_btn));
	}
	
	public void bind(PostItem item){
		
		//repost
		if(item.repost!=null){
			if(!repost_view_attached){
				View repost = inflate(getContext(), R.layout.post_repost_item, null);
				repost_avatar = repost.findViewById(R.id.avatar);
				repost_name = repost.findViewById(R.id.name);
				repost_text = repost.findViewById(R.id.text);
				repost_container.addView(repost);
				repost_view_attached = true;
			}
			
			//Get image
			String image_url = "";
			if(item.repost.author instanceof GroupModel){
				GroupModel model = (GroupModel) item.repost.author;
				image_url = model.photo_100;
			}else if(item.repost.author instanceof UserModel){
				UserModel model = (UserModel) item.repost.author;
				image_url = model.photo_100;
			}
			
			repost_name.setText(OwnerModelUtils.getFullTitleByOwner(item.repost.author));
			repost_avatar.setImage(image_url);
			repost_text.setText(item.repost.text);
		}else{
			removeView(repost_container);
		}
		
		//Get title
		String image_url = "";
		if(item.author instanceof GroupModel){
			GroupModel model = (GroupModel) item.author;
			image_url = model.photo_100;
		}else if(item.author instanceof UserModel){
			UserModel model = (UserModel) item.author;
			image_url = model.photo_100;
		}

		//Title
		name.setText(OwnerModelUtils.getFullTitleByOwner(item.author));

		//Image
		if(!image_url.isEmpty())
			image.setImage(image_url);

		//Date
		CharSequence ago = 
			DateUtils.getRelativeTimeSpanString(item.date*1000L, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
		date.setText(ago);

		//Attachments
		if(item.attachments!=null&&item.attachments.size()!=0){
			List<Attachments.Attachment> photos = item.attachments.getAttachmentsByType("photo");
			if(!photos.isEmpty())
				attachments.addPhotoArray(photos);
			
			List<Attachments.Attachment> audios = item.attachments.getAttachmentsByType("audio");
			if(!audios.isEmpty())
				attachments.addAudiosArray(audios);
				
			List<Attachments.Attachment> polls = item.attachments.getAttachmentsByType("poll");
			if(!polls.isEmpty()){
				PollModel poll = new PollModel(polls.get(0).object);
				attachments.addPoll(poll);
			}else{
				attachments.removePoll();
			}
		}else
			attachments.removePoll();

		if(!item.text.isEmpty()){
			//Text content
			if(fragment!=null){
				Spannable t = OwnerLinkSpanFactory.withSpans(item.text, true, true, new DefaultActionListener(fragment));
				Linkify.addLinks(t, Linkify.WEB_URLS);
				text.setText(t);
			}else
				text.setText(item.text);
			text.setVisibility(VISIBLE);
		}else
			text.setVisibility(GONE);
			
		//Likes
		like_counter.setText(""+item.likes_count);
		OnPostButtonClickListener onC = new OnPostButtonClickListener(getContext(), item);
		onC.setLikeCount(item.likes_count);
		onC.setLike(item.user_likes);
		like_btn.setOnClickListener(onC);
	}
	
	public class OnHeaderClick implements OnClickListener {

		@Override
		public void onClick(View p1) {
			if(getContext() instanceof BaseActivity){
				BaseActivity activity = (BaseActivity) getContext();
			}
		}
	}
	
	public class OnPostButtonClickListener implements OnClickListener {

		//colors
		public int like_active;
		private int gray;

		//data
		public boolean like;
		public int likes_count = 0;

		private Application app;

		private PostItem post;

		public OnPostButtonClickListener(Context ctx, PostItem item){
			this.post = item;
			app = (Application) ctx.getApplicationContext();
			gray = Color.parseColor("#909090");
			like_active = ctx.getResources().getColor(R.color.like_active);
		}

		public void setLike(boolean like) {
			this.like = like;
			if(like){
				like_counter.setTextColor(like_active);
				like_icon.getDrawable().setTint(like_active);
			}else{
				like_counter.setTextColor(gray);
				like_icon.getDrawable().setTint(gray);
			}
		}

		public void setLikeCount(int p0){
			likes_count = p0;
			like_counter.setText(""+p0);
		}

		public boolean isLike() {
			return like;
		}

		@Override
		public void onClick(View p1) {
			switch(p1.getId()){
				case R.id.like_btn:{
						if(!isLike()){
							setLike(true);
							app.sdk.request("likes.add", VKParameters.from(
									"type", "post",
									"owner_id", post.owner_id,
									"item_id", post.post_id
								), new VKSdk.RequestListener(){

									@Override
									public void onComplete(VKSdk.VKResponse response) {
										setLikeCount(response.json.optJSONObject("response").optInt("likes"));
									}

									@Override
									public void onError(VKError error) {
										setLike(false);
									}
								});
						}else{
							setLike(false);
							app.sdk.request("likes.delete", VKParameters.from(
									"type", "post",
									"owner_id", post.owner_id,
									"item_id", post.post_id
								), new VKSdk.RequestListener(){

									@Override
									public void onComplete(VKSdk.VKResponse response) {
										setLikeCount(response.json.optJSONObject("response").optInt("likes"));
									}

									@Override
									public void onError(VKError error) {
										setLike(true);
									}
								});
						}
					}
					break;
			}
		}
	}
}
