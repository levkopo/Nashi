package com.levkopo.apps.nashi.fragments;
import com.github.ybq.android.spinkit.SpinKitView;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;

public class TabFragment extends BaseFragment
{
	private TabsFragment fragment;
	
	public TabFragment(TabsFragment fragment, SpinKitView spin){
		this.progressBar = spin;
		this.fragment = fragment;
	}

	@Override
	public void open(BaseFragment fr) {
		fragment.open(fr);
	}

	@Override
	public void close() {
		fragment.close();
	}
}
