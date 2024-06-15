package com.levkopo.apps.nashi.fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.adapters.WallAdapter;
import com.levkopo.apps.nashi.models.PostItem;
import com.levkopo.apps.nashi.models.UserModel;
import com.levkopo.apps.nashi.utils.OnItemClick;
import com.levkopo.apps.nashi.widget.Avatar;
import com.levkopo.apps.nashi.widget.ultilist.BaseItem;
import com.levkopo.ui.widgets.UltiList;
import com.levkopo.ui.widgets.ultilist.BaseListItem;
import com.levkopo.vksdk.VKError;
import com.levkopo.vksdk.VKParameters;
import com.levkopo.vksdk.VKSdk;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.levkopo.apps.nashi.widget.CardView;
import android.widget.Button;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.ButtonBarLayout;
import android.view.View.OnClickListener;
import android.graphics.Color;
import com.levkopo.apps.nashi.adapters.DataAdapter;
import com.levkopo.apps.nashi.models.ui.DataListItem;
import com.levkopo.apps.nashi.fragments.ui.DataListFragment;

public class UserFragment extends ToolbarFragment implements SwipeRefreshLayout.OnRefreshListener
{
	private SwipeRefreshLayout refresh;

	private TextView name;

	private TextView status;

	private Avatar avatar;
	
	private UserModel user;

	private int id;
	
	private ArrayList<DataListItem> info = new ArrayList<>();
	private ArrayList<DataListItem> more_info = new ArrayList<>();
	
	private DataAdapter ultilist;

	private RecyclerView wall_list;

	private LinearLayoutManager layoutManager;
	
	private ArrayList<PostItem> wall_items = new ArrayList<>();

	public int offset;

	private Button show_dialog_btn;

	private LinearLayout button_container;

	private NestedScrollView scrollview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		id = getArguments().getInt("id", app.user_id);
	}
	
	@Override
	public View createContent(LayoutInflater inflater, Bundle savedInstanceState) {
		refresh = new SwipeRefreshLayout(getContext());
		refresh.setOnRefreshListener(this);
		scrollview = new NestedScrollView(getContext());
		LinearLayout content = new LinearLayout(getContext());
		content.setOrientation(LinearLayout.VERTICAL);
		
		View header = inflater.inflate(R.layout.user_header, null);
		
		name = header.findViewById(R.id.name);
		status = header.findViewById(R.id.status);
		avatar = header.findViewById(R.id.image);
				
		content.addView(header);
		
		button_container = new LinearLayout(getContext());
		button_container.setOrientation(LinearLayout.HORIZONTAL);
		show_dialog_btn = new Button(new ContextThemeWrapper(getContext(), R.style.Widget_MaterialComponents_Button_TextButton));
		show_dialog_btn.setOnClickListener(new OnButtonClick());
		show_dialog_btn.setText(R.string.write);
		button_container.addView(show_dialog_btn);
		
		content.addView(button_container);
		
		CardView info_card = new CardView(getContext());
		info_card.setUseCompatPadding(true);
		RecyclerView info_list = new RecyclerView(getContext());
		info_list.setLayoutManager(new LinearLayoutManager(getContext()));
		info_list.setOverScrollMode(View.OVER_SCROLL_NEVER);
		ultilist = new DataAdapter(info, new onInfoClick());
		info_list.setAdapter(ultilist);
		info_card.addView(info_list);
		content.addView(info_card);
	
		wall_list = new RecyclerView(getContext());
		layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		wall_list.setLayoutManager(layoutManager);
		wall_list.getLayoutManager().setAutoMeasureEnabled(true);
		wall_list.setAdapter(new WallAdapter(wall_items, new openPost()));
		content.addView(wall_list);
		
		scrollview.addView(content);
		scrollview.setVisibility(View.INVISIBLE);
		refresh.addView(scrollview);
		
		return refresh;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setTitle("");
		useSpectator(false);
		setSwipeRefreshLayout(refresh);
		setIndeterminate(true);
		refreshData();
	}
	
	@Override
	public void onRefresh() {
		offset = 0;
		refreshData();
	}

	@Override
	public void onRetry() {
		super.onRetry();
		setIndeterminate(true);
		refreshData();
	}
	
	public void refreshData(){
		getVKSdk().request("users.get", VKParameters.from(
			"user_ids", id,
				"fields", "online, domain, has_mobile, status, last_seen, is_friend, sex, photo_200, counters, site"   
		), new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					//Off refreshing animations
					scrollview.setVisibility(View.VISIBLE);
					refresh.setRefreshing(false);
					setIndeterminate(false);
					info.clear();
					
					try{
						JSONArray r = response.json.getJSONArray("response");
						user = new UserModel(r.getJSONObject(0));
						avatar.setImage(user.photo_200);
						avatar.setOnline(user.online);
						name.setText(user.first_name + " " + user.last_name);
						if(!user.status.isEmpty()){
							status.setText(user.status);
						}
						
						
						if(user.is_closed){
							if(!user.is_friend){
								show_dialog_btn.setEnabled(false);
								DataListItem closedMessage = new DataListItem(getContext(), "closedMessage")
								.setTitle(getResources().getString(R.string.closed_account))
								.setIcon(getResources().getDrawable(R.drawable.ic_round_info_black_48));
								info.add(closedMessage);
							}
						}
						
						if(!user.counters.isNull("followers")){
							DataListItem followers_count = new DataListItem(getContext(), "followers_count")
							.setTitle(String.format(getResources().getString(R.string.followers_count), user.counters.optInt("followers")))
							.setIcon(getResources().getDrawable(R.drawable.ic_round_rss_feed_black_48));
							info.add(followers_count);
						}
						
						if(!user.counters.isNull("friends")){
							DataListItem friends = new DataListItem(getContext(), "friends")
								.setTitle(String.format(getResources().getString(R.string.friends_count), user.counters.optInt("friends")))
								.setIcon(getResources().getDrawable(R.drawable.ic_round_rss_feed_black_48));
							more_info.add(friends);
						}
						
						if(user.site!=null){
							DataListItem site = new DataListItem(getContext(), "site")
								.setTitle(user.site)
								.setIcon(getResources().getDrawable(R.drawable.ic_round_rss_feed_black_48));
							more_info.add(site);
						}
						
						if(more_info.size()>0){
							DataListItem followers_count = new DataListItem(getContext(), "more")
								.setTitle(getResources().getString(R.string.more))
								.setIcon(getResources().getDrawable(R.drawable.ic_round_rss_feed_black_48));
							info.add(followers_count);
						}
						
						ultilist.notifyDataSetChanged();
					}catch(JSONException e){}
				}

				@Override
				public void onError(VKError error) {
					//Off refreshing animations
					refresh.setRefreshing(false);
					setIndeterminate(false);
					
					showErrorScreen();
				}
		});
		
		getVKSdk().request("wall.get", VKParameters.from(
				"filters", "post",
				"owner_id", id,
				"extended", 1,
				"offset", offset
			), new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					if(offset==0)
						wall_items.clear();
					try{
						JSONObject response_object = response.json.getJSONObject("response");
						JSONArray items = response_object.optJSONArray("items");
						if(items.length()!=0)
							for(int i = 0; i < items.length(); i++){
								if(items.getJSONObject(i).getString("type")!="ads")
									wall_items.add(new PostItem(items.getJSONObject(i), false).bindOwners(response_object.optJSONArray("profiles"), response_object.optJSONArray("groups")));
							}
						LayoutAnimationController controller =
							AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fall_down);

   					    wall_list.setLayoutAnimation(controller);
    			    	wall_list.getAdapter().notifyDataSetChanged();
   					 	wall_list.scheduleLayoutAnimation();
					}catch(JSONException e){
						e.printStackTrace();
						showErrorScreen();
					}
				}

				@Override
				public void onError(VKError error) {
					showErrorScreen();
				}
			});
	}
	
	public class OnButtonClick implements OnClickListener {

		@Override
		public void onClick(View p1) {
			if(p1.equals(show_dialog_btn)){
				open(DialogFragment.newInstance(user));
			}
		}
		
	}
	
	public class onInfoClick implements DataAdapter.OnDataItemClickListener {

		@Override
		public void onClick(DataListItem item) {
		
				switch(item.id){
					case "friends":{
						UserFriendsFragment friends = new UserFriendsFragment();
						Bundle args = new Bundle();
						args.putInt("id", id);
						friends.setArguments(args);
						open(friends);
					}
					break;
					
					case "more":{
						DataListFragment bottom = DataListFragment.get(more_info, this);
						bottom.show(getFragmentManager(), "UserMoreSheet");
					}
					break;
				}
		}
	}
	
	public class openPost implements OnItemClick {

		@Override
		public void onClick(Object object) {
			//switchFragment(VKMiniAppFragment.newInstance("https://levkoposc.github.io/class-vkminiapp/index.html"));
			open(PostInfoFragment.newInstance((PostItem) object));
		}
	}
}
