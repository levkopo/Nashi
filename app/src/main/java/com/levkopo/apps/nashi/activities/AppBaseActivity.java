package com.levkopo.apps.nashi.activities;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.levkopo.apps.nashi.Application;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.widget.AppFunc;
import android.view.WindowManager;
import androidx.core.content.ContextCompat;
import android.graphics.Color;
import org.json.JSONObject;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;
import android.content.res.Resources;
import java.util.ArrayList;
import com.levkopo.apps.nashi.services.LongPollService;

public class AppBaseActivity extends AppCompatActivity implements Application.OnUpdateToggles
{
	private AppFunc funcs;
	
	public Application app;

	private SharedPreferences sp;

	private Toolbar toolbar;
	
	private ArrayList<OnUpdateThemeListener> listeners = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (Application) getApplication();
		sp = app.sp;
		funcs = new AppFunc(app);
		funcs.addOnUpdateTogglesListener(this);
		
		if((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
		   == Configuration.UI_MODE_NIGHT_YES){
			sp.edit().putBoolean("dark", true).apply();
		}

		if(sp.getBoolean("dark", false)){
			setTheme(R.style.AppTheme_Dark);
		}else{
			setTheme(R.style.AppTheme);
		}
		
		getWindow().setFlags(
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
		);
	}
	
	public final boolean getToggle(String name){
		return funcs.getToggle(name);
	}
	
	public final BaseFragment getSavedFragment(Class class_){
		return funcs.getSavedFragment(class_);
	}

	@Override
	public void setSupportActionBar(Toolbar toolbar) {
		super.setSupportActionBar(toolbar);
		this.toolbar = toolbar;
		int systemUi_visibility = 0;
		//Fill statusbar and Navigation bar
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			Window w = getWindow();
			if(toolbar.getBackground() instanceof ColorDrawable){
				int sb_color = ((ColorDrawable) toolbar.getBackground()).getColor();
				w.setStatusBarColor(sb_color);
				if(sb_color>=0xffc7c7c7){
					systemUi_visibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
				}
			}
			w.getDecorView().setSystemUiVisibility(systemUi_visibility);
			/*w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			 w.setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
			 w.setNavigationBarColor(ContextCompat.getColor(this, android.R.color.transparent));*/
		}
	}
	
	public void startActivity(Class<?> activity) {
		super.startActivity(new Intent(this, activity));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		
		if(menu!=null)
		for(int i = 0; i < menu.size(); i++){
            Drawable drawable = menu.getItem(i).getIcon();
            if(drawable != null) {
                drawable.mutate();
				if(((ColorDrawable) toolbar.getBackground()).getColor()>=0xffc7c7c7)
					if(!sp.getBoolean("dark", false))
						drawable.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);
					else
						drawable.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
				else
                	drawable.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
            }
        }

		return true;
	}
	
	public void switchTheme(){
		//Switch dark (light) theme
		SharedPreferences.Editor editor = sp.edit();
		if(sp.getBoolean("dark", false)){
			setTheme(R.style.AppTheme);
			editor.putBoolean("dark", false);
		}else{
			setTheme(R.style.AppTheme_Dark);
			editor.putBoolean("dark", true);
		}
		editor.apply();
		app.refresh();
		app.createFragments();
		
		for(int i = 0; i < listeners.size(); i++){
			listeners.get(i).onUpdateTheme(getTheme());
		}
	}
	
	@Override
	public void onToggleUpdate(JSONObject toggles) {}
	
	public void addOnUpdateThemeListener(OnUpdateThemeListener listener){
		listeners.add(listener);
	}
	
	public interface OnUpdateThemeListener{
		public void onUpdateTheme(Resources.Theme theme)
	}
}
