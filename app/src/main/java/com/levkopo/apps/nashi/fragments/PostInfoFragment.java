package com.levkopo.apps.nashi.fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.adapters.PostInfoAdapter;
import com.levkopo.apps.nashi.models.PostItem;
import com.levkopo.vksdk.VKSdk;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.levkopo.vksdk.VKParameters;
import android.util.Log;
import com.levkopo.apps.nashi.models.CommentModel;
import androidx.appcompat.app.AppCompatActivity;
import java.io.Serializable;
import com.levkopo.vksdk.VKError;
import android.os.Parcelable;

public class PostInfoFragment extends ToolbarFragment implements SwipeRefreshLayout.OnRefreshListener
{
	private RecyclerView list;

	private SwipeRefreshLayout refresh;
	
	public ArrayList<Object> post_info = new ArrayList<>();
	
	public PostItem post;
	
	public String posts;

	private LinearLayoutManager layoutManager;
	
	private int offset;
		
	public static PostInfoFragment newInstance(PostItem post) {
		Bundle args = new Bundle();
		args.putParcelable("post", (Parcelable) post);
		args.putString("posts", post.owner_id+"_"+post.post_id);
		PostInfoFragment f = new PostInfoFragment();
		f.setArguments(args);
		return f;
	}
	
	public static PostInfoFragment newInstance(int owner_id, int post_id) {
		Bundle args = new Bundle();
		args.putString("posts", owner_id+"_"+post_id);
		PostInfoFragment f = new PostInfoFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public View createContent(LayoutInflater inflater, Bundle savedInstanceState) {
		refresh = new SwipeRefreshLayout(getContext());
		list = new RecyclerView(getContext());
		layoutManager =  new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		list.setLayoutManager(layoutManager);
		list.getLayoutManager().setAutoMeasureEnabled(true);
		list.setAdapter(new PostInfoAdapter(this, post_info));
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
						offset += 10;
						loadComments();
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
		hideBottomLayout();
		setSwipeRefreshLayout(refresh);
		posts = getArguments().getString("posts", null);
		post = getArguments().getParcelable("post");
		if(post!=null)
			post_info.add(post);
		setTitle(R.string.post_on_wall);
		setIndeterminate(true);
		onRefresh();
	}
	
	@Override
	public void onRefresh() {
		getVKSdk().request("wall.getById",
						   VKParameters.from("extended", 1, "posts", posts),
			new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					try{
						JSONObject response_object = response.json.getJSONObject("response");
						JSONArray items = response_object.optJSONArray("items");
						post = new PostItem(items.getJSONObject(0), false).bindOwners(response_object.optJSONArray("profiles"), response_object.optJSONArray("groups"));
						if(post_info.size()!=0)
							post_info.set(0, post);
						else
							post_info.add(post);
						list.getAdapter().notifyDataSetChanged();
						offset = 0;
						loadComments();
					}catch(JSONException e){
						e.printStackTrace();
					}
				}

				@Override
				public void onError(VKError error) {
					
					//Off refreshing animations
					refresh.setRefreshing(false);
					setIndeterminate(false);
				}
			});
	}
	
	public void loadComments(){
		getVKSdk().request("wall.getComments", 
			new Object[]{"extended", "1",
				"post_id", post.post_id,
				"owner_id", post.owner_id,
				"post_id", post.post_id,
				"offset",offset,
				"thread_items_count", 3
			}, 
			new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					if(offset==0&&post_info.size()!=1){
						post_info.clear();
						post_info.add(post);
					}
					try{
						JSONObject response_object = response.json.getJSONObject("response");
						JSONArray items = response_object.optJSONArray("items");
						if(items.length()!=0)
							for(int i = 0; i < items.length(); i++){
								post_info.add(new CommentModel(items.getJSONObject(i)).bindAuthors(response_object.optJSONArray("profiles"), response_object.optJSONArray("groups")));
							}
						list.getAdapter().notifyDataSetChanged();
					}catch(JSONException e){
						e.printStackTrace();
					}
					refresh.setRefreshing(false);
					setIndeterminate(false);
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
	public void onRetry() {
		super.onRetry();
		setIndeterminate(true);
		offset = 0;
		onRefresh();
	}
}
