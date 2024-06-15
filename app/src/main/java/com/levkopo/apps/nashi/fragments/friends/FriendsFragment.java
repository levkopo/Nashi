package com.levkopo.apps.nashi.fragments.friends;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.github.ybq.android.spinkit.SpinKitView;
import com.levkopo.apps.nashi.adapters.OwnersListAdapter;
import com.levkopo.apps.nashi.fragments.TabFragment;
import com.levkopo.apps.nashi.fragments.TabsFragment;
import com.levkopo.apps.nashi.fragments.UserFragment;
import com.levkopo.apps.nashi.models.OwnerModel;
import com.levkopo.apps.nashi.models.UserModel;
import com.levkopo.vksdk.VKError;
import com.levkopo.vksdk.VKParameters;
import com.levkopo.vksdk.VKSdk;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FriendsFragment extends TabFragment implements SwipeRefreshLayout.OnRefreshListener
{
	private SwipeRefreshLayout refresh;

	private RecyclerView list;

	private ArrayList<OwnerModel> items = new ArrayList<>();

	private int offset = 0;

	private int count;
	
	private int id;

	private LinearLayoutManager layoutManager;
	
	public FriendsFragment(TabsFragment p0, SpinKitView p1){
		super(p0, p1);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup p2, Bundle savedInstanceState) {
		refresh = new SwipeRefreshLayout(getContext());
		list = new RecyclerView(inflater.getContext());
		layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		list.setLayoutManager(layoutManager);
		list.getLayoutManager().setAutoMeasureEnabled(true);
		list.setAdapter(new OwnersListAdapter(items, new OnClick()));
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
		id = getArguments().getInt("id", app.user_id);
		setIndeterminate(true);
		addItems(0);
	}

	@Override
	public void onRefresh() {
		items.clear();
		addItems(0);
	}

	@Override
	public void onRetry() {
		super.onRetry();
		addItems(offset);
	}

	public void addItems(final int offset_){
		getVKSdk().request("friends.get", VKParameters.from("fields", "domain, photo_100, has_mobile, online, city, education", "offset", offset_,
		"user_id", id,
		"order", "name"), new VKSdk.RequestListener(){

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
							UserModel model = new UserModel(items.getJSONObject(i));
							FriendsFragment.this.items.add(model);
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

	public class OnClick implements OwnersListAdapter.onClick {

		@Override
		public void onClick(OwnerModel owner) {
			UserFragment fr = new UserFragment();
			Bundle args = new Bundle();
			args.putInt("id", ((UserModel) owner).id);
			fr.setArguments(args);
			open(fr);
		}
	}
}
