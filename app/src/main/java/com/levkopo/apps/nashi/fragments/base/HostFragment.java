package com.levkopo.apps.nashi.fragments.base;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.levkopo.apps.nashi.R;
import java.util.ArrayList;
import com.levkopo.apps.nashi.activities.AppBaseActivity;
import android.content.res.Resources;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import com.levkopo.apps.nashi.fragments.base.HostFragment;
import com.levkopo.apps.nashi.widget.ui.NavigationViewPager;

public class HostFragment extends Fragment
{
	public final ArrayList<BaseFragment> fragments = new ArrayList<>();
	private final int pager_container_id = ViewCompat.generateViewId();;
	private NavigationViewPager pages;
	private PagesAdapter adapter;
	
	public HostFragment(BaseFragment first_fragment){
		first_fragment.host = this;
		first_fragment.is_first = true;
		if(first_fragment.getArguments()==null)
			first_fragment.setArguments(new Bundle());
		fragments.add(first_fragment);
	}
	
	public void openFragment(BaseFragment fragment){
		fragment.host = this;
		if(fragment.getArguments()==null)
			fragment.setArguments(new Bundle());
		fragments.add(fragment);
		reload();
		pages.setCurrentItem(fragments.size()-1);
	}
	
	public void closeFragment(){
		pages.setCurrentItem(fragments.size()-2);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new PagesAdapter(getFragmentManager(), fragments);
		if(getContext() instanceof AppBaseActivity){
			AppBaseActivity activity = (AppBaseActivity) getContext();
			activity.addOnUpdateThemeListener(new OnUpdateTheme());
		}		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		pages = new NavigationViewPager(container.getContext());
		pages.setAdapter(new PagesAdapter(getParentFragmentManager(), fragments));
		pages.addOnPageChangeListener(new OnPageChange());
		pages.setId(pager_container_id);
		return pages;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if(savedInstanceState!=null) {
			pages.onRestoreInstanceState(savedInstanceState.getParcelable("pagesData"));
			for (int i = 0; i < savedInstanceState.getInt("pagesCount" + this.getTag()); i++) {
				BaseFragment fragment = (BaseFragment) getParentFragmentManager().getFragment(
						savedInstanceState, "pageFragment" + i);
				if (fragment != null) {
					fragment.host = this;
					fragments.add(fragment);
				}
			}

			adapter.notifyDataSetChanged();
		}
		
		reload();
	}
	
	public void reload() {
		pages.getAdapter().notifyDataSetChanged();
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putParcelable("pagesData", pages.onSaveInstanceState());
		outState.putInt("pagesCount"+this.getTag(), fragments.size());

		int i = 0;
		for(BaseFragment fragment: fragments){
			getParentFragmentManager().putFragment(outState, "pageFragment"+i, fragment);
			i++;
		}

		super.onSaveInstanceState(outState);
	}
	
	private class OnPageChange implements ViewPager.OnPageChangeListener {

		private boolean canRemoveFragment = false;
		
		private float sumPositionAndPositionOffset = 0.0f;
		
		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			canRemoveFragment = position + positionOffset < sumPositionAndPositionOffset;
			sumPositionAndPositionOffset = position + positionOffset;
		}

		@Override
		public void onPageSelected(int p1) {

		}

		@Override
		public void onPageScrollStateChanged(int p1) {
			if (p1 == 0 && canRemoveFragment) {
				while ((fragments.size() - 1) > pages.getCurrentItem()) {
					fragments.remove(fragments.size()-1);
					reload();
				}
			}
		}
	}
	
	private class PagesAdapter extends FragmentStatePagerAdapter {

		private ArrayList<BaseFragment> fragments;
		
		public PagesAdapter(FragmentManager manager, ArrayList<BaseFragment> fragments){
			super(manager);
			this.fragments = fragments;
		}
		
		@Override
		public int getCount() {
			return fragments.size();
		}

		@Override
		public Fragment getItem(int p1) {
			return fragments.get(p1);
		}
		
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}
	}
	
	private class OnUpdateTheme implements AppBaseActivity.OnUpdateThemeListener {

		@Override
		public void onUpdateTheme(Resources.Theme theme) {
			if(HostFragment.this!=null)
				reload();
		}
	}
}
