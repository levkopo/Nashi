package com.levkopo.apps.nashi.fragments.groups;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.levkopo.apps.nashi.adapters.OwnersListAdapter;
import com.levkopo.apps.nashi.fragments.TabFragment;
import com.levkopo.apps.nashi.fragments.TabsFragment;
import com.levkopo.apps.nashi.models.GroupModel;
import com.levkopo.apps.nashi.models.OwnerModel;
import com.levkopo.vksdk.VKError;
import com.levkopo.vksdk.VKParameters;
import com.levkopo.vksdk.VKSdk;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GroupsFragment extends TabFragment implements SwipeRefreshLayout.OnRefreshListener
{
	private SwipeRefreshLayout refresh;

	private LinearLayoutManager layoutManager;

	private RecyclerView list;
	
	private int offset;
	
	private int count;
	
	public String filter;
	
	private ArrayList<OwnerModel> items = new ArrayList<>();
	
	public GroupsFragment(TabsFragment fr, String filter){
		super(fr, fr.progressBar);
		this.filter = filter;
	}
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup p2, Bundle savedInstanceState) {
		refresh = new SwipeRefreshLayout(getContext());
		list = new RecyclerView(inflater.getContext());
		layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		list.setLayoutManager(layoutManager);
		list.getLayoutManager().setAutoMeasureEnabled(true);
		list.setAdapter(new OwnersListAdapter(items, new OnClickGroupListener()));
		RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {

			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				int visibleItemCount = layoutManager.getChildCount();//смотрим сколько элементов на экране
				int totalItemCount = layoutManager.getItemCount();//сколько всего элементов
				int firstVisibleItems = layoutManager.findFirstVisibleItemPosition();//какая позиция первого элемента

				if (!refresh.isRefreshing()&&progressBar.getVisibility()==View.GONE&&count!=items.size()) {//проверяем, грузим мы что-то или нет, эта переменная должна быть вне класса  OnScrollListener 
					if ( (visibleItemCount+firstVisibleItems) >= totalItemCount) {
						refresh.setRefreshing(true);//ставим флаг что мы попросили еще элемены
						offset++;
						addItems(offset);
					}
				}
			}
		};
		list.addOnScrollListener(scrollListener);
		refresh.addView(list);
		refresh.setOnRefreshListener(this);
		return refresh;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if(items.size()==0){
			setIndeterminate(true);
		}else{
			refresh.setRefreshing(true);
		}
		
		offset = 0;
		addItems(0);
	}

	@Override
	public void onRetry() {
		super.onRetry();
		addItems(offset);
	}
	
	@Override
	public void onRefresh() {
		offset = 0;
		addItems(0);
	}
	
	private void addItems(final int offset_) {
		getVKSdk().request("groups.get", VKParameters.from("fields", "photo_100, activity", "extended", 1, "offset", offset_, "filter", filter), new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					setIndeterminate(false);
					refresh.setRefreshing(false);

					try {
						if(offset_==0)
							items.clear();

						JSONObject res = response.json.getJSONObject("response");
						count = res.optInt("count");
						JSONArray items = res.getJSONArray("items");
						for(int i = 0; i<items.length(); i++){
							GroupModel model = new GroupModel(items.getJSONObject(i));
							GroupsFragment.this.items.add(model);
						}
						list.getAdapter().notifyDataSetChanged();
					} catch (JSONException e) {}
				}

				@Override
				public void onError(VKError error) {
					setIndeterminate(false);
					refresh.setRefreshing(false);
					showErrorScreen();
				}
		});
	}
	
	public class OnClickGroupListener implements OwnersListAdapter.onClick {

		@Override
		public void onClick(OwnerModel owner) {
			Bundle args = new Bundle();
			args.putInt("group_id", ((GroupModel) owner).id);
			GroupFragment fragment = new GroupFragment();
			fragment.setArguments(args);
			
			open(fragment);
		}
	} 
}
