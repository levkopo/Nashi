package com.levkopo.apps.nashi.fragments.wall;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.adapters.WallAdapter;
import com.levkopo.apps.nashi.fragments.PostInfoFragment;
import com.levkopo.apps.nashi.fragments.TabFragment;
import com.levkopo.apps.nashi.fragments.TabsFragment;
import com.levkopo.apps.nashi.models.PostItem;
import com.levkopo.apps.nashi.utils.OnItemClick;
import com.levkopo.vksdk.VKError;
import com.levkopo.vksdk.VKParameters;
import com.levkopo.vksdk.VKSdk;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FeedFragment extends TabFragment implements SwipeRefreshLayout.OnRefreshListener
{
	public ArrayList<PostItem> posts = new ArrayList<>();
	
	private SwipeRefreshLayout refresh;

	private RecyclerView rv;

	public String filters;
	
	public String source_ids;
	
	private String next_from = null;

	private LinearLayoutManager layoutManager;

	private Object[] params;

	private int positionIndex;

	private int topView;

	public boolean autoUpdate;

	private WallAdapter adapter;
	
	public static FeedFragment create(TabsFragment tabs, String filters, String source_ids){
		FeedFragment fragment = new FeedFragment(tabs);
		fragment.filters = filters;
		fragment.source_ids = source_ids;
		fragment.autoUpdate = true;
		return fragment;
	}
	
	public static FeedFragment create(TabsFragment tabs, ArrayList<PostItem> items){
		FeedFragment fragment = new FeedFragment(tabs);
		fragment.posts = items;
		fragment.autoUpdate = false;
		return fragment;
	}
	
	public FeedFragment(TabsFragment fragment){
		super(fragment, fragment.progressBar);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new WallAdapter(posts, new openPost());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		refresh = new SwipeRefreshLayout(getContext());
		rv = new RecyclerView(inflater.getContext());
		layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		rv.setLayoutManager(layoutManager);
		rv.getLayoutManager().setAutoMeasureEnabled(true);
		rv.setAdapter(adapter);
		/*RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				int visibleItemCount = layoutManager.getChildCount();//смотрим сколько элементов на экране
				int totalItemCount = layoutManager.getItemCount();//сколько всего элементов
				int firstVisibleItems = layoutManager.findFirstVisibleItemPosition();//какая позиция первого элемента

				if (!refresh.isRefreshing()&&progressBar.getVisibility()==View.GONE) {//проверяем, грузим мы что-то или нет, эта переменная должна быть вне класса  OnScrollListener 
					if ( (visibleItemCount+firstVisibleItems) >= totalItemCount) {
						refresh.setRefreshing(true);//ставим флаг что мы попросили еще элемены
						loadFeed();
					}
				}
			}
		};
		rv.addOnScrollListener(scrollListener);*/
		if(autoUpdate){
			refresh.setOnRefreshListener(this);
			refresh.addView(rv);
			return refresh;
		}
		
		return rv;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if(savedInstanceState!=null){
			posts = savedInstanceState.getParcelableArrayList("posts");
			positionIndex = savedInstanceState.getInt("positionIndex", -1);
			topView = savedInstanceState.getInt("topView", 0);
			if (positionIndex!= -1) {
				layoutManager.scrollToPositionWithOffset(positionIndex, topView);
			}
		}
		
		if(autoUpdate){
			setIndeterminate(true);
			loadFeed();
		}
	}

	@Override
	public void onRefresh() {
		next_from = null;
		loadFeed();
	}
	
	private void loadFeed(){
		//Object[] params = VKParameters.from("filters", filters, "start_from", next_from);
		params = VKParameters.from("filters", filters, "source_ids", source_ids);
	
		getVKSdk().request("newsfeed.get", params, new VKSdk.RequestListener(){
				
			@Override
			public void onComplete(VKSdk.VKResponse response) {
					
				//Off refreshing animations
				refresh.setRefreshing(false);
				setIndeterminate(false);
				
				try{
					JSONObject response_object = response.json.getJSONObject("response");
					next_from = response_object.optString("next_from");
					JSONArray items = response_object.optJSONArray("items");
					if(items.length()!=0)
						for(int i = 0; i < items.length(); i++){
							if(!items.getJSONObject(i).getString("type").equals("ads"))
								posts.add(new PostItem(items.getJSONObject(i), true).bindOwners(response_object.optJSONArray("profiles"), response_object.optJSONArray("groups")));
						}

					adapter.notifyDataSetChanged();
				}catch(JSONException e){
					e.printStackTrace();
					showErrorScreen();
				}
			}

			@Override
			public void onError(VKError error) {

				//Off refreshing animations
				refresh.setRefreshing(false);
				setIndeterminate(false);

				showErrorScreen();
			}
		});
	}

	@Override
	public void onPause() {
		positionIndex = layoutManager.findFirstVisibleItemPosition();
		View startView = rv.getChildAt(0);
		topView = (startView == null) ? 0 : (startView.getTop() - rv.getPaddingTop());
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList("posts", posts);
		outState.putInt("positionIndex", layoutManager.findFirstVisibleItemPosition());
		
		View startView = rv.getChildAt(0);
		outState.putInt("topView", (startView == null) ? 0 : (startView.getTop() - rv.getPaddingTop()));
	}

	@Override
	public void onResume() {
		super.onResume();
		if (positionIndex!= -1) {
			layoutManager.scrollToPositionWithOffset(positionIndex, topView);
		}
	}
	
	public class openPost implements OnItemClick {

		@Override
		public void onClick(Object object) {
			open(PostInfoFragment.newInstance((PostItem) object));
		}
	}
}
