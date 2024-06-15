package com.levkopo.apps.nashi.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.levkopo.apps.nashi.Application;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.fragments.DialogsFragment;
import com.levkopo.apps.nashi.fragments.MenuFragment;
import com.levkopo.apps.nashi.fragments.NotificationsFragment;
import com.levkopo.apps.nashi.fragments.VKMiniAppFragment;
import com.levkopo.apps.nashi.fragments.base.HostFragment;
import com.levkopo.apps.nashi.fragments.wall.NewsFeedFragment;
import com.levkopo.apps.nashi.services.AudioService;
import com.levkopo.apps.nashi.services.LongPollService;
import com.levkopo.apps.nashi.widget.MiniAudioController;
import com.levkopo.vksdk.VKError;
import com.levkopo.vksdk.VKParameters;
import com.levkopo.vksdk.VKSdk;
import org.json.JSONObject;
import android.util.Log;

public class BaseActivity extends AppBaseActivity
{
	//Widgets
	public BottomNavigationView bnav;
	public LinearLayout bottom_layout;
	
	//Counters
	public JSONObject counters;
	
	//Hosts
	private HostFragment[] hosts;
	private int selected_host_id = 0;
	public HostFragment   selected_host;
	private MiniAudioController mac;
		
	private AccountManager accountManager;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.v("BA", "-----Starting BaseActivity");
		startService(new Intent(this, LongPollService.class));
		
		app = (Application) getApplication();
		app.as = new OnChangeStatusAudioService();
		
		//Bind view
		setContentView(R.layout.activity_main);
		
		//Cheack Accounts
		accountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
        Account[] accounts = accountManager.getAccountsByType(Application.accounts);
		if(accounts.length==0){
			Log.v("BA","Accounts not founded. Starting LoginActivity");
			startActivity(LoginActivity.class);
			finish();
		}
		
		hostsCreate();
			
		//Bottom Navigation
	    bnav = findViewById(R.id.bottom_navigation);
		bnav.setElevation(0f);
		bnav.setOnNavigationItemSelectedListener(new Nav());
		bnav.setSelectedItemId(R.id.newsfeed);
		bnav.setOnNavigationItemReselectedListener(new DebugNavReselectedListener());
		Resources resources = getResources();
		int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
		if (resourceId > 0) {
			findViewById(R.id.bottom_space).getLayoutParams().height = resources.getDimensionPixelSize(resourceId);
		}
		
		if(savedInstanceState!=null){
			bnav.setSelectedItemId(savedInstanceState.getInt("sbni"));
			selected_host_id = savedInstanceState.getInt("shi");
		}
		
		bottom_layout = findViewById(R.id.bottom_layout);
		
		mac = findViewById(R.id.mac);
		mac.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1) {
					startActivity(AudioPlayerActivity.class);
				}
		});
		
		addOnUpdateThemeListener(new OnUpdateTheme());
			
		updateCounters();
	}

	private void hostsCreate() {
		hosts = new HostFragment[]{
			new HostFragment(getSavedFragment(NewsFeedFragment.class)),
			new HostFragment(getSavedFragment(DialogsFragment.class)),
			new HostFragment(getSavedFragment(NotificationsFragment.class)),
			new HostFragment(new MenuFragment()),
		};
	}

	//Fragment switcher
	public boolean selectHost(int fragment){
		selected_host = hosts[fragment];
		FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
		//fTrans.setTransition( FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		fTrans.replace(R.id.fragment_container, selected_host);
		fTrans.commit();
		updateCounters();
		return true;
	}
		
	//Back
	@Override
	public void onBackPressed() {
		if(selected_host.fragments.size()>1){
			selected_host.closeFragment();
		}else
			super.onBackPressed();
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateCounters();
	}

	@Override
	public void onToggleUpdate(JSONObject toggles) {
		super.onToggleUpdate(toggles);
		if(!getToggle("app_supported")){
			startActivity(UpdateAppActivity.class);
			finish();
		}
	}
	
	public void hideBottomLayout(){
		bottom_layout.animate()
			.translationY(bottom_layout.getLayoutParams().height)
			.alpha(0.0f)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					FrameLayout fragment_container = findViewById(R.id.fragment_container);
					CoordinatorLayout.LayoutParams fragment_container_params = (CoordinatorLayout.LayoutParams) fragment_container.getLayoutParams();
					fragment_container_params.setMargins(0,0,0,0);
					
					bottom_layout.setVisibility(View.GONE);
				}
			});
	}
	
	public void showBottomLayout(){
		bottom_layout.animate()
			.translationY(0)
			.alpha(1.0f)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					FrameLayout fragment_container = findViewById(R.id.fragment_container);
					CoordinatorLayout.LayoutParams fragment_container_params = (CoordinatorLayout.LayoutParams) fragment_container.getLayoutParams();
					fragment_container_params.setMargins(0,0,0, (int) getResources().getDimension(R.dimen.bottom_nav_height));
					
					bottom_layout.setVisibility(View.VISIBLE);
				}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
		super.onSaveInstanceState(outState, outPersistentState);
		outState.putInt("shi", selected_host_id);
		outState.putInt("sbni", bnav.getSelectedItemId());
	}
	
	public void updateCounters(){
		app.sdk.request("account.getCounters", VKParameters.from(), new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					counters = response.json.optJSONObject("response");
					if(!counters.isNull("messages")){
						BadgeDrawable badge = bnav.getOrCreateBadge(R.id.dialogs);
						badge.setNumber(counters.optInt("messages"));
					}else{
						bnav.removeBadge(R.id.dialogs);
					}
					
					if(!counters.isNull("notifications")){
						BadgeDrawable badge = bnav.getOrCreateBadge(R.id.notifications);
						badge.setNumber(counters.optInt("notifications"));
					} else {
						bnav.removeBadge(R.id.notification_main_column_container);
					}
				}

				@Override public void onError(VKError error) {
					if(error!=null&&error.error_code==5){
						accountManager.removeAccount(app.account, null, null);
						startActivity(LoginActivity.class);
						finish();
					}
				}
			});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopService(new Intent(this, LongPollService.class));
	}
	
	public class OnChangeStatusAudioService implements Application.OnChangeStatusAudioService {

		@Override
		public void onStart(AudioService service) {
			if(BaseActivity.this!=null){
				mac.setVisibility(View.VISIBLE);
				mac.startWorking(BaseActivity.this, service);
			}
		}

		@Override
		public void onStop() {
			if(BaseActivity.this!=null){
				mac.setVisibility(View.INVISIBLE);
			}
		}
	}
	
	public class Nav implements BottomNavigationView.OnNavigationItemSelectedListener {

		private boolean success;	
		
		@Override
		public boolean onNavigationItemSelected(MenuItem p1)
		{
			switch(p1.getItemId()){

				case R.id.newsfeed:
					selected_host_id = 0;
				break;
				
				case R.id.dialogs:
					selected_host_id = 1;
				break;
				
				case R.id.menu:
					selected_host_id = 3;
				break;
				
				case R.id.notifications:
					selected_host_id = 2;
				break;
				
			}
			
			selectHost(selected_host_id);
			return true;
		}
	}
	
	public class DebugNavReselectedListener implements BottomNavigationView.OnNavigationItemReselectedListener {

		public int message_clicks_count = 0;
		public int menu_clicks_count ;
		
		public int debug_clicks_count = 5;

		private int newsfeed_clicks_count;
		
		@Override
		public void onNavigationItemReselected(MenuItem p1) {
			switch(p1.getItemId()){
				case R.id.dialogs:{
					message_clicks_count++;
				}
				break;
				
				case R.id.menu:{
					menu_clicks_count++;
				}
				break;
				
				case R.id.newsfeed:{
					newsfeed_clicks_count++;
				}
				break;
			}
			
			/*  Код для перезагрузки фрагментов
			 *	Новости = 8   Связано с датой рождения
			 *	Диалоги = 1   15.08.2005
			 *	Меню    = 5
			 */
			if(message_clicks_count== 8  &&
			   newsfeed_clicks_count==1 &&
			   menu_clicks_count==    5){
				   Toast.makeText(BaseActivity.this, "OK!", Toast.LENGTH_LONG).show();
				   hostsCreate();
				   
				   message_clicks_count  = 0;
				   newsfeed_clicks_count = 0;
				   menu_clicks_count     = 0;
			   }
			   
			if(message_clicks_count==5){
				VKMiniAppFragment ui = VKMiniAppFragment.newInstance("https://kuhel.github.io/vkapps-connect-promise/?vk_access_token_settings=&vk_app_id=6909581&vk_are_notifications_enabled=0&vk_is_app_user=1&vk_is_favorite=1&vk_language=ru&vk_platform=mobile_android&vk_ref=other&vk_user_id=432176401&sign=rISofyDaQOqzePRoZG27vekc20GZtT-Xo0iUDoqvdzE");
				selected_host.openFragment(ui);
			}
		}
	}
	
	private class OnUpdateTheme implements AppBaseActivity.OnUpdateThemeListener {

		@Override
		public void onUpdateTheme(Resources.Theme theme) {
			hostsCreate();
			TypedValue bg = new TypedValue();
			theme.resolveAttribute(R.attr.background, bg, true);
			
			findViewById(R.id.bottom_space).setBackgroundColor(bg.data);
			bnav.setBackgroundColor(bg.data);
			
			bnav.setItemIconTintList(getResources().getColorStateList(R.color.bottom_nav_colors, theme));
		}
	}
}
