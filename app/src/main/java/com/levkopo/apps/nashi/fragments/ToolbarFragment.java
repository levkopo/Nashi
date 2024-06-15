package com.levkopo.apps.nashi.fragments;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.activities.AppBaseActivity;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;
import com.levkopo.apps.nashi.utils.DisplayUtils;

public class ToolbarFragment extends BaseFragment
{
	
	public Toolbar toolbar;
	
	public SwipeRefreshLayout srl;
	
	private View content;

	private RecyclerView list;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		content = LayoutInflater.from(this.getContext()).inflate(R.layout.appkit_toolbar_fragment, container, false);
		return content;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		if(getContext() instanceof AppBaseActivity){
			AppBaseActivity activity = (AppBaseActivity) getContext();
			activity.addOnUpdateThemeListener(new OnUpdateTheme());
		}
		
		//Получение Toolbar'а
		toolbar = view.findViewById(R.id.toolbar);
		
		//Bind content
		ViewGroup content = view.findViewById(R.id.content_full);
		content.addView(
			createContent(LayoutInflater.from(getContext()), savedInstanceState)
		);
		
		activity.setSupportActionBar(toolbar);
		
		updateUI(getActivity().getTheme());
		setHasOptionsMenu(true);
		if(srl!=null){
			srl.setRefreshing(false);
		}
	}
	
	public void updateUI(Resources.Theme theme){
		//Загрузка цветов
		TypedValue primary = new TypedValue();
		TypedValue background = new TypedValue();
		theme.resolveAttribute(R.attr.toolbarTitleColor, primary, true);
		theme.resolveAttribute(R.attr.background, background, true);
		
		//Покраска Toolbar'а
		toolbar.setBackgroundColor(background.data);
		
		
		findViewById(R.id.bd_content).setBackgroundColor(background.data);
		toolbar.setTitleTextColor(primary.data);
		
		if(!is_first){
			toolbar.setNavigationOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View p1) {
						close();
					}
				});
			Drawable icon = getResources().getDrawable(R.drawable.ic_round_arrow_back_black_24);
			icon.setTint(primary.data);
			toolbar.setNavigationIcon(icon);
		}
		
	}

	@Override
	public void showBottomLayout() {
		super.showBottomLayout();
	}

	@Override
	public void hideBottomLayout() {
		super.hideBottomLayout();
	}

	//Content creater
	public View createContent(LayoutInflater inflater, Bundle savedInstanceState){
		return new View(getContext());
	}
	
	//Toolbar methods
	public void setTitle(String string){
		toolbar.setTitle(string);
	}
	
	public void setTitle(int resId){
		toolbar.setTitle(resId);
	}
	
	public void useSpectator(boolean use){
		if(!use)
			findViewById(R.id.toolbar_spectator).setVisibility(View.INVISIBLE);
		else
			findViewById(R.id.toolbar_spectator).setVisibility(View.VISIBLE);
	}
	
	public void setList(RecyclerView list) {
		this.list = list;
		if(findViewById(R.id.toolbar_spectator).getVisibility()!=View.INVISIBLE){
			list.addOnScrollListener(spectator_scroll);
		}
		
	}

	public RecyclerView getList() {
		return list;
	}
	
	//SwipeRefreshLayout support
	public void setSwipeRefreshLayout(SwipeRefreshLayout srl) {
		this.srl = srl;
	}
	
	@Override
	public void setIndeterminate(boolean bool) {
		if(srl!=null&&bool)
			srl.setRefreshing(!bool);

		super.setIndeterminate(bool);
	}
	
	private class OnUpdateTheme implements AppBaseActivity.OnUpdateThemeListener {

		@Override
		public void onUpdateTheme(Resources.Theme theme) {
			if(ToolbarFragment.this!=null&ToolbarFragment.this.getView()!=null)
				updateUI(theme);
		}
	}
	
	RecyclerView.OnScrollListener spectator_scroll = new RecyclerView.OnScrollListener() {
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			super.onScrolled(recyclerView, dx, dy);
			if(dy<DisplayUtils.convertDpToPixel(3, getContext())){
				useSpectator(true);
			}else
				useSpectator(false);
		}
	};
}
