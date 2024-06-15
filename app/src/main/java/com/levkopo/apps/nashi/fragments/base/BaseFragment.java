package com.levkopo.apps.nashi.fragments.base;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import androidx.fragment.app.Fragment;
import com.github.ybq.android.spinkit.SpinKitView;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.ThreeBounce;
import com.google.android.material.snackbar.Snackbar;
import com.levkopo.apps.nashi.Application;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.activities.BaseActivity;
import com.levkopo.apps.nashi.widget.AppFunc;
import com.levkopo.vksdk.VKSdk;
import org.json.JSONException;
import org.json.JSONObject;
import androidx.recyclerview.widget.RecyclerView;
import android.app.Activity;

public class BaseFragment extends Fragment 
{

	/* Базовый фрагмент с осноновными функциями
	 * для BaseActivty
	 */

	public String TAG = "BaseF";

	public Application app;

	public AppFunc funcs;

	public SpinKitView progressBar;

	private Snackbar err;

	public BaseActivity activity;
	
	public View view;
	
	public HostFragment host;
	
	public boolean is_first;

	//Получить сохраненный фрагмент
	public BaseFragment getSavedFragment(Class class_){
		BaseFragment fr = funcs.getSavedFragment(class_);
		return fr;
	}

	public boolean getToggle(String name){
		return funcs.getToggle(name);
	}
	
	public void startActivity(Class<? extends Activity> act){
		activity.startActivity(act);
	}

	//Создание константы приложения
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (Application) getActivity().getApplication();
		funcs = new AppFunc(app);
		if(getActivity() instanceof BaseActivity)
			activity = (BaseActivity) getActivity();

		try {
			if(app.savedData.contains(getClass().getName() + "_data"))
				onLoadSavedData(new JSONObject(
									app.savedData.getString(
										getClass().getName() + "_data",
										"{}"
									)
								));
		} catch (JSONException e) {}
	}

	public void saveData(JSONObject data){
		if(app!=null)
			app.savedData
				.edit()
				.putString(getClass().getName()+"_data", data.toString())
				.apply();
	}

	//Открыть панель
	public void open(BaseFragment fr){
		host.openFragment(fr);
	}

	//Закрыть панель
	public void close(){
		host.closeFragment();
	}
	
	public void hideBottomLayout(){
		activity.hideBottomLayout();
	}
	
	public void showBottomLayout(){
		activity.showBottomLayout();
	}

	//Прогресс бар и другое
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		this.view = view;

		if(view.findViewById(R.id.progress_spin)!=null)
		    progressBar = view.findViewById(R.id.progress_spin);
		if(progressBar!=null){
			Sprite doubleBounce = new ThreeBounce();
			progressBar.setIndeterminateDrawable(doubleBounce);
		}
		
		
		int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
		if(findViewById(R.id.top_space)!=null){
			findViewById(R.id.top_space).getLayoutParams()
				.height = statusBarHeight;
		}

		setIndeterminate(false);

		err = Snackbar.make(getView(), R.string.error_label, Snackbar.LENGTH_LONG)
			.setAction(R.string.retry, new OnClickListener() {
				@Override
				public void onClick(View v) {
					// Respond to the click, such as by undoing the modification that caused
					// this message to be displayed
					onRetry();
				}
			});
	}

	//Показать окно ошибки
	public void showErrorScreen(){
		err.show();
		setIndeterminate(false);
	}

	//Обновление прогресс бара
	public void setIndeterminate(boolean bool){
		if(progressBar!=null){
			if(bool){
				progressBar.setVisibility(View.VISIBLE);
			}else{
				progressBar.setVisibility(View.GONE);
			}
		}else{
			Log.wtf(TAG, "Failed set Indeterminate");
		}
	}

	public View findViewById(int res){
		if(res==0||view==null){
			throw new NullPointerException("resId == 0 or view == null");
		}

		return view.findViewById(res);
	}

	//VKSdk
	public VKSdk getVKSdk(){
		return app.sdk;
	}

	@Override
	public void onPause() {
		super.onPause();
		setIndeterminate(false);
		if(err!=null)
			err.dismiss();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		setIndeterminate(false);
		if(err!=null)
			err.dismiss();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		setIndeterminate(false);
		if(err!=null)
			err.dismiss();
	}

	public void onRetry(){}

	public void onLoadSavedData(JSONObject data){}
}
