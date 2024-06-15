package com.levkopo.apps.nashi.fragments.groups;
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
import com.levkopo.apps.nashi.fragments.PostInfoFragment;
import com.levkopo.apps.nashi.fragments.ToolbarFragment;
import com.levkopo.apps.nashi.fragments.UserFriendsFragment;
import com.levkopo.apps.nashi.models.GroupModel;
import com.levkopo.apps.nashi.models.PostItem;
import com.levkopo.apps.nashi.utils.OnItemClick;
import com.levkopo.apps.nashi.widget.Avatar;
import com.levkopo.apps.nashi.widget.CardView;
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
import com.levkopo.vksdk.VKSdk.VKResponse;
import com.levkopo.apps.nashi.models.widgets.BaseWidget;
import com.levkopo.apps.nashi.models.widgets.TextWidget;
import com.levkopo.apps.nashi.widget.GroupWidgetView;
import com.levkopo.apps.nashi.models.widgets.ListWidget;
import android.util.Log;
import com.levkopo.apps.nashi.models.widgets.DonationWidget;
import com.levkopo.apps.nashi.models.widgets.CoverWidget;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class GroupFragment extends ToolbarFragment implements SwipeRefreshLayout.OnRefreshListener 
{
	private SwipeRefreshLayout refresh;

	private TextView name;
	
	private TextView status;
	
	public Avatar image;
	
	private GroupModel info;
	
	private BaseWidget widget;
	
	public int group_id = 1;

	private LinearLayoutManager layoutManager;

	private RecyclerView wall_list;

	private UltiList ultilist;

	private NestedScrollView scrollview;
	
	private ArrayList<BaseListItem> info_ = new ArrayList<>();
	
	private ArrayList<PostItem> wall_items = new ArrayList<>();

	public int offset;
	
	public GroupWidgetView group_widget;
	
	@Override
	public View createContent(LayoutInflater inflater, Bundle savedInstanceState) {
		refresh = new SwipeRefreshLayout(getContext());
		refresh.setOnRefreshListener(this);
		scrollview = new NestedScrollView(getContext());
		LinearLayout content = new LinearLayout(getContext());
		content.setOrientation(LinearLayout.VERTICAL);

		View header = inflater.inflate(R.layout.group_header, null);

		name = header.findViewById(R.id.name);
		status = header.findViewById(R.id.status);
		image = header.findViewById(R.id.image);

		content.addView(header);
		
		CardView info_card = new CardView(getContext());
		info_card.setUseCompatPadding(true);
		RecyclerView info_list = new RecyclerView(getContext());
		info_list.setLayoutManager(new LinearLayoutManager(getContext()));
		info_list.setOverScrollMode(View.OVER_SCROLL_NEVER);
		ultilist = UltiList.generate(info_list, info_);
		ultilist.setOnClickListener(new onInfoClick());
		info_card.addView(info_list);
		content.addView(info_card);

		group_widget = new GroupWidgetView(getContext());
		content.addView(group_widget);
		
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
		if(getArguments()!=null){
			group_id = getArguments().getInt("group_id", 1);
		}
		refreshData();
		
		if(info!=null){
			setTitle(info.name);
			name.setText(info.name);
			image.setImage(info.photo_200);
			if(info.status!=null)
				status.setText(info.status);
		}
	}

	@Override
	public void onRetry() {
		super.onRetry();
	}
	
	@Override
	public void onRefresh() {
		refreshData();
	}
	
	public void refreshData(){
		getVKSdk().request("groups.getById", VKParameters.from(
			"group_id", group_id, 
				"fields", "status,trending,verified,site,members_count,can_message,can_post,counters,description"
		), new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					scrollview.setVisibility(View.VISIBLE);
					JSONObject info_json = response.json.optJSONArray("response").optJSONObject(0);
					info = new GroupModel(info_json);
					setTitle(info.name);
					name.setText(info.name);
					image.setImage(info.photo_200);
					status.setText(info.status);
					
				}

				@Override
				public void onError(VKError error) {
					setIndeterminate(false);
					refresh.setRefreshing(false);
					showErrorScreen();
				}
		});
		
		getVKSdk().request("wall.get", VKParameters.from(
				"filters", "post",
				"owner_id", group_id*-1,
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
			
		getVKSdk().request("appWidgets.get", VKParameters.from(
			"group_id", group_id
		), new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					Log.wtf("Nashi", response.json.toString());
					JSONObject res = response.json.optJSONObject("response");
					switch(res.optInt("type")){
						case 1:
							widget = new TextWidget(res.optJSONObject("data"));
							break;
							
						case 5:
						case 2:
							widget = new ListWidget(res.optJSONObject("data"));
							break;
							
						case 9:
							widget = new DonationWidget(res.optJSONObject("data"));
							break;
							
						case 6:
							widget = new CoverWidget(res.optJSONObject("data"));
							break;
							
						default:
							widget = new BaseWidget(res.optJSONObject("data"));
					}
					
					group_widget.loadWidget(widget);
				}

				@Override
				public void onError(VKError error) {}
			});
		//wall.onRefresh();
	}
	
	public class onInfoClick implements UltiList.OnClickListener {

		@Override
		public void onClick(BaseListItem item) {
			if(item instanceof BaseItem){
				switch(((BaseItem) item).id){
					case "friends":{
							UserFriendsFragment friends = new UserFriendsFragment();
							Bundle args = new Bundle();
							 //args.putInt("id", id);
							friends.setArguments(args);
							open(friends);
						}
						break;
				}
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
