package com.levkopo.apps.nashi.widget;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spannable;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.bumptech.glide.Glide;
import com.levkopo.apps.nashi.Application;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.activities.BaseActivity;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;
import com.levkopo.apps.nashi.link.DefaultActionListener;
import com.levkopo.apps.nashi.link.OwnerLinkSpanFactory;
import com.levkopo.apps.nashi.models.Attachments;
import com.levkopo.apps.nashi.models.PollModel;
import com.levkopo.apps.nashi.models.StickerModel;
import com.levkopo.apps.nashi.utils.DisplayUtils;
import java.util.List;

public class MessageView extends FrameLayout
{
	
	public boolean out = false;

	private TextView text;

	private Application app;

	private AttachmentsView attachments;

	private LottieAnimationView sticker_animation;

	private LinearLayout content;

	private ImageView sticker_img;
	
	public MessageView(Context ctx){
		super(ctx);
		init();
	}
	
	public MessageView(Context ctx, AttributeSet as){
		super(ctx, as);
		init(as);
	}
	
	public MessageView(Context ctx, AttributeSet as, int q){
		super(ctx, as, q);
		init(as);
	}

	public void setOut(boolean out) {
		this.out = out;
		init();
	}

	public boolean isOut() {
		return out;
	}
	
	public void init(AttributeSet as){
		init();
	}
	
	public void init(){
		
		app = (Application) getContext().getApplicationContext();
		
		removeAllViewsInLayout();
		
		int padding = (int) DisplayUtils.convertDpToPixel(8, getContext());
		int margin = (int) DisplayUtils.convertDpToPixel(4, getContext());
		
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		if(out){
			params.gravity = Gravity.RIGHT;
		}else
			params.gravity = Gravity.LEFT;
		setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			
		params.setMargins(margin,margin,margin,margin);
		
		TypedValue outMessBG = new TypedValue();
		TypedValue outMessTC = new TypedValue();
		TypedValue inMessBG = new TypedValue();
		TypedValue inMessTC = new TypedValue();
		
		
		getContext().getTheme().resolveAttribute(R.attr.out_message_bg_textColor, outMessTC, true);
		getContext().getTheme().resolveAttribute(R.attr.out_message_bg, outMessBG, true);
		getContext().getTheme().resolveAttribute(R.attr.in_message_bg, inMessBG, true);
		getContext().getTheme().resolveAttribute(R.attr.in_message_bg_textColor, inMessTC, true);
		
		
		Drawable bg = getResources().getDrawable(R.drawable.message_shape);
		if(out){
			bg.setTint(outMessBG.data);
		}else{
			bg.setTint(inMessBG.data);
		}
		
		content = new LinearLayout(getContext());
		content.setBackground(bg);
		content.setOrientation(LinearLayout.VERTICAL);
		content.setPadding(padding, padding, padding, padding);
		addView(content, params);
		
		text = new TextView(getContext());
		if(out){
			text.setTextColor(outMessTC.data);
		}else{
			text.setTextColor(inMessTC.data);
		}
		attachments = new AttachmentsView(getContext());
		
		content.addView(attachments);		
		content.addView(text);
	}
	
	public void setAttachments(Attachments att){
		if(att!=null&&att.size()!=0){
			List<Attachments.Attachment> audios = att.getAttachmentsByType("audio");
			if(audios.size()>0)
				attachments.addAudiosArray(audios);

			List<Attachments.Attachment> photos = att.getAttachmentsByType("photo");
			if(photos.size()>0)
				attachments.addPhotoArray(photos);
				
			if(att.getAttachmentsByType("sticker").size()==1){
				content.removeView(text);
				content.setBackground(null);
				StickerModel sticker = new StickerModel(att.getAttachmentsByType("sticker").get(0).object);
				int sticker_size = (int) DisplayUtils.convertDpToPixel(150, getContext());
				if(sticker.animation_url!=null){
					sticker_animation = new LottieAnimationView(getContext());
					sticker_animation.setAnimationFromUrl(sticker.animation_url);
					content.addView(sticker_animation, sticker_size, sticker_size);
					sticker_animation.playAnimation();
					sticker_animation.setRepeatCount(LottieDrawable.INFINITE);
				}else{
					sticker_img = new ImageView(getContext());
					Glide.with(this).load(sticker.url).into(sticker_img);
					content.addView(sticker_img, sticker_size, sticker_size);
				}
			}
			
			List<Attachments.Attachment> polls = att.getAttachmentsByType("poll");
			if(!polls.isEmpty()){
				PollModel poll = new PollModel(polls.get(0).object);
				attachments.addPoll(poll);
			}else{
				attachments.removePoll();
			}
		}else
			attachments.removePoll();
	}
	
	public void setHTMLText(String html){
		if(text==null)
			return;
		
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N){
			text.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
		}else{
			text.setText(Html.fromHtml(html));
		}
		
		Linkify.addLinks(text, Linkify.WEB_URLS);
	}
	
	public void setText(BaseFragment fragment, CharSequence chars){
		if(text==null)
			return;
		
		Spannable t = OwnerLinkSpanFactory.withSpans(chars.toString(), true, true, new DefaultActionListener(fragment));
		text.setText(t);
		Linkify.addLinks(text, Linkify.WEB_URLS);
	}
}	
