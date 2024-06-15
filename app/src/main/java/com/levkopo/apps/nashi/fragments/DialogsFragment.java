package com.levkopo.apps.nashi.fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.models.PostItem;
import com.levkopo.vksdk.VKSdk;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import com.levkopo.apps.nashi.models.DialogItem;
import com.levkopo.apps.nashi.adapters.DialogsAdapter;
import android.util.Log;
import com.levkopo.vksdk.VKParameters;
import com.levkopo.apps.nashi.utils.OnItemClick;
import com.levkopo.vksdk.VKError;

public class DialogsFragment extends ToolbarFragment implements SwipeRefreshLayout.OnRefreshListener
{
	
	private RecyclerView list;
	
	private SwipeRefreshLayout refresh;
	
	public ArrayList<DialogItem> dialogs = new ArrayList<>();
	
	public int offset = 0;

	private LinearLayoutManager layoutManager;

	private DialogsAdapter adapter;

	private int positionIndex;
	private int topView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new DialogsAdapter(dialogs, new openDialog());
	}

	@Override
	public View createContent(LayoutInflater inflater, Bundle savedInstanceState) {
		refresh = new SwipeRefreshLayout(getContext());
		list = new RecyclerView(getContext());
		layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		list.setLayoutManager(layoutManager);
		list.getLayoutManager().setAutoMeasureEnabled(true);
		list.setAdapter(adapter);
		RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				int visibleItemCount = layoutManager.getChildCount();//смотрим сколько элементов на экране
				int totalItemCount = layoutManager.getItemCount();//сколько всего элементов
				int firstVisibleItems = layoutManager.findFirstVisibleItemPosition();//какая позиция первого элемента

				if (!refresh.isRefreshing()&&progressBar.getVisibility()==View.GONE) {//проверяем, грузим мы что-то или нет, эта переменная должна быть вне класса  OnScrollListener 
					if ( (visibleItemCount+firstVisibleItems) >= totalItemCount) {
						refresh.setRefreshing(true);//ставим флаг что мы попросили еще элемены
						offset++;
						addItems(offset);
					}
				}
			}
		};
		list.addOnScrollListener(scrollListener);
		setList(list);
		refresh.setOnRefreshListener(this);
		refresh.addView(list);
		return refresh;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if(savedInstanceState!=null){
			dialogs = savedInstanceState.getParcelableArrayList("dialogs");
			positionIndex = savedInstanceState.getInt("positionIndex", -1);
			topView = savedInstanceState.getInt("topView", 0);
			if (positionIndex!= -1) {
				layoutManager.scrollToPositionWithOffset(positionIndex, topView);
			}
		}
		
		setSwipeRefreshLayout(refresh);
		setTitle(R.string.dialogs);
		if(dialogs.size()==0)
			setIndeterminate(true);
		else
			refresh.setRefreshing(true);
			offset = 0;
			
		addItems(0);
	}

	@Override
	public void onLoadSavedData(JSONObject data) {
		super.onLoadSavedData(data);
		try{
			JSONObject response_object = data.getJSONObject("response");
			JSONArray items = response_object.optJSONArray("items");
			for(int i = 0; i < items.length(); i++){
				dialogs.add(new DialogItem(items.getJSONObject(i)).bindUsers(response_object.optJSONArray("profiles"), response_object.optJSONArray("groups")));
			}
		}catch(JSONException e){
			e.printStackTrace();
		}
	}
	
	@Override
	public void onRefresh() {
		offset = 0;
		addItems(0);
	}
	
	public void addItems(final int offset_){
		getVKSdk().request("messages.getConversations", VKParameters.from("extended", 1, "offset", offset_*20), new VKSdk.RequestListener(){

				@Override public void onComplete(VKSdk.VKResponse response) {
					//Off refreshing animations
					refresh.setRefreshing(false);
					setIndeterminate(false);

					if(offset_==0){
						dialogs.clear();
						saveData(response.json);
					}

					try{
						JSONObject response_object = response.json.getJSONObject("response");
						JSONArray items = response_object.optJSONArray("items");
						for(int i = 0; i < items.length(); i++){
							dialogs.add(new DialogItem(items.getJSONObject(i)).bindUsers(response_object.optJSONArray("profiles"), response_object.optJSONArray("groups")));
						}
						adapter.notifyDataSetChanged();
					}catch(JSONException e){
						e.printStackTrace();
						showErrorScreen();
					}}

				@Override public void onError(VKError error) {
					//Off refreshing animations
					refresh.setRefreshing(false);
					setIndeterminate(false);

					showErrorScreen();
				}});
	}

	@Override
	public void onRetry() {
		super.onRetry();
		refresh.setRefreshing(true);
		addItems(offset);
	}

	@Override
	public void onPause() {
		positionIndex = layoutManager.findFirstVisibleItemPosition();
		View startView = list.getChildAt(0);
		topView = (startView == null) ? 0 : (startView.getTop() - list.getPaddingTop());
		super.onPause();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList("dialogs", dialogs);
		outState.putInt("positionIndex", layoutManager.findFirstVisibleItemPosition());

		View startView = list.getChildAt(0);
		outState.putInt("topView", (startView == null) ? 0 : (startView.getTop() - list.getPaddingTop()));
	}

	@Override
	public void onResume() {
		super.onResume();
		if (positionIndex!= -1) {
			layoutManager.scrollToPositionWithOffset(positionIndex, topView);
		}
	}
	
	public class openDialog implements OnItemClick {

		@Override
		public void onClick(Object object) {
			//switchFragment(VKMiniAppFragment.newInstance("https://levkoposc.github.io/class-vkminiapp/index.html"));
			open(DialogFragment.newInstance(((DialogItem) object).conversation));
		}
	}
}
