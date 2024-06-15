package com.levkopo.apps.nashi;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.widget.Toast;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import com.levkopo.apps.nashi.fragments.AccountsFragment;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;
import com.levkopo.apps.nashi.fragments.DialogsFragment;
import com.levkopo.apps.nashi.fragments.MenuFragment;
import com.levkopo.apps.nashi.fragments.wall.NewsFeedFragment;
import com.levkopo.apps.nashi.fragments.UserAudiosFragment;
import com.levkopo.apps.nashi.fragments.UserFriendsFragment;
import com.levkopo.apps.nashi.fragments.groups.UserGroupsFragment;
import com.levkopo.apps.nashi.models.AccentModel;
import com.levkopo.apps.nashi.models.AudioModel;
import com.levkopo.apps.nashi.services.AudioService;
import com.levkopo.vksdk.Request;
import com.levkopo.vksdk.VKError;
import com.levkopo.vksdk.VKSdk;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;
import com.levkopo.apps.nashi.fragments.NotificationsFragment;
import org.json.JSONArray;
import com.levkopo.apps.nashi.fragments.settings.MenuSettingsFragment;
import com.levkopo.apps.nashi.services.LongPollService;
import com.levkopo.apps.nashi.utils.LongPollNotifications;

public class Application extends MultiDexApplication
{
	public static String accounts = "com.levkopo.apps.nashi";

	//VK
	public VKSdk sdk;
	public Account account;
	public int user_id;
	
	//SharedPreferences
	public SharedPreferences sp;
	public SharedPreferences toggles;
	public SharedPreferences savedData;
	
	//App info
	public String version_name;
	
	//Audio service
	public AudioService player;
	public boolean audio_serviceBound = false;
	public OnChangeStatusAudioService as;
	
	//Toggles
	public ArrayList<OnUpdateToggles> tu_listeners = new ArrayList<>();

	//Saved Fragments
	public BaseFragment[] savedFragments = null;
	
	//LongPoll
	public ArrayList<LongPollService.OnNewUpdate> updates = new ArrayList<>();
	
	//Binding this Client to the AudioPlayer Service
	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			AudioService.LocalBinder binder = (AudioService.LocalBinder) service;
			player = binder.getService();
			audio_serviceBound = true;
			if(as!=null)
				as.onStart(player);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			if(as!=null){
				as.onStop();
			}
			audio_serviceBound = false;
		}
	};
	
	@Override
	protected void attachBaseContext(Context base){
		MultiDex.install(this);
		super.attachBaseContext(base);
	}
	
	public void createFragments(){
		savedFragments = new BaseFragment[]{
			new NewsFeedFragment(),
			new DialogsFragment(),
			new UserFriendsFragment(),
			new AccountsFragment(),
			new UserAudiosFragment(),
			new UserGroupsFragment(),
			new NotificationsFragment(),
			new MenuSettingsFragment(),
		};
	}

	@Override
	public void onCreate() {
		createFragments();
		super.onCreate();
		try {
			PackageInfo p_info = getPackageManager().getPackageInfo(getPackageName(), 0);
			version_name = p_info.versionName;
		} catch (PackageManager.NameNotFoundException e) {}
		
		refresh();
		updateToggles();
	}

	@Override
	public void onTerminate() {
		if (audio_serviceBound) {
			unbindService(serviceConnection);
			//service is active
			player.stopSelf();
		}
	}
	
	public void loadAudio(ArrayList<AudioModel> audios, int postition){
		if(audios==null)
			return;
		
		Bundle media = new Bundle();
		media.putParcelableArrayList("audios_list", (ArrayList<? extends Parcelable>) audios);
		media.putInt("pos", postition);
		
		if (!audio_serviceBound) {
			Intent playerIntent = new Intent(this, AudioService.class);
			
			playerIntent.putExtra("media", media);
			startService(playerIntent);
			bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		}else{
			Intent intent = new Intent(AudioService.AudioReceiver.PLAY);

			intent.putExtra("media", media);
			intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
			sendBroadcast(intent);
		}
	}
	
	public void updateToggles(){
	    toggles = getSharedPreferences("toggles", Context.MODE_PRIVATE);
		new Request(new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					//Off refreshing animations
					SharedPreferences.Editor editor = Application.this.toggles.edit();
					JSONObject toggles = response.json.optJSONObject("response");
					Iterator<String> keys = toggles.keys();
					while(keys.hasNext()){
						String key = keys.next();
						if(!toggles.isNull(key)){
							editor.putBoolean(key, toggles.optBoolean(key, false));
						}
					}
					
					editor.apply();
					
					for(OnUpdateToggles listener : tu_listeners){
						if(listener!=null){
							listener.onToggleUpdate(toggles);
						}
					}
					
				}

				@Override
				public void onError(VKError error) {}
			}).execute("https://levkopo.fvds.ru/api/nashi/method/toggles.get?app_version="+version_name);
	}
	
	public void refresh(){
		sdk = new VKSdk();
		savedData = getSharedPreferences("data", Context.MODE_PRIVATE);
	    sp = PreferenceManager.getDefaultSharedPreferences(this);
		AccountManager accountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
		Account[] accounts = accountManager.getAccountsByType(Application.accounts);
		if(accounts.length!=0){
		    account = accounts[sp.getInt("account",0)];
			user_id = Integer.decode(accountManager.getUserData(account, "id"));
			sdk.setAccessToken(accountManager.getUserData(account, "token"));
			
			updates.clear();
			updates.add(new LongPollNotifications(sdk, this));
		}
	}
	
	public interface OnUpdateToggles{
		public void onToggleUpdate(JSONObject toggles)
	}
	
	public interface OnChangeStatusAudioService{
		public void onStart(AudioService service)
		
		public void onStop()
	}
}
