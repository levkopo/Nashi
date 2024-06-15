package com.levkopo.apps.nashi.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.adapters.AudiosListAdapter;
import com.levkopo.apps.nashi.models.AudioModel;
import com.levkopo.vksdk.Request;
import com.levkopo.vksdk.VKError;
import com.levkopo.vksdk.VKSdk;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import android.view.ViewGroup;
import com.github.ybq.android.spinkit.SpinKitView;
import org.json.JSONObject;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;

public class AudioFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener
{
	private SwipeRefreshLayout refresh;

	private RecyclerView list;

	private ArrayList<AudioModel> items = new ArrayList<>();

	private int offset = 0;

	private LinearLayoutManager layoutManager;
	
	public AudioFragment(SpinKitView spin){
		this.progressBar = spin;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup p2, Bundle savedInstanceState) {
		refresh = new SwipeRefreshLayout(getContext());
		list = new RecyclerView(inflater.getContext());
		layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		list.setLayoutManager(layoutManager);
		list.getLayoutManager().setAutoMeasureEnabled(true);
		list.setAdapter(new AudiosListAdapter(items));
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
		refresh.addView(list);
		refresh.setOnRefreshListener(this);
		return refresh;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setIndeterminate(true);
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

	@Override
	public void onLoadSavedData(JSONObject data) {
		super.onLoadSavedData(data);
		try{
			items.clear();
			JSONArray audios = data.getJSONArray("response");
			for(int i = 0; i < audios.length(); i++){
				items.add(new AudioModel(audios.getJSONObject(i)));
			}
		}catch(JSONException e){}
	}

	private void addItems(final int p0) {
		new Request(new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					
					//Off refreshing animations
					refresh.setRefreshing(false);
					setIndeterminate(false);
					
					try {
						if(p0==0){
							items.clear();
							saveData(response.json);
						}

						JSONArray audios = response.json.getJSONArray("response");
						for(int i = 0; i < audios.length(); i++){
							items.add(new AudioModel(audios.getJSONObject(i)));
						}
						list.getAdapter().notifyDataSetChanged();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onError(VKError error) {
					refresh.setRefreshing(false);
					setIndeterminate(false);
					showErrorScreen();
				}
			}).execute("https://levkopo.fvds.ru/api/nashi/method/audio.get?access_token="+getVKSdk().token
					   +"&offset="+p0);
	}
}
