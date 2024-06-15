package com.levkopo.apps.nashi.fragments;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;
import java.io.InputStream;
import com.levkopo.apps.nashi.activities.AppBaseActivity;
import org.json.JSONObject;
import org.json.JSONException;
import com.levkopo.vksdk.VKParameters;
import com.levkopo.vksdk.VKSdk;
import com.levkopo.vksdk.VKSdk.VKResponse;
import com.levkopo.vksdk.VKError;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;
import android.util.AndroidException;
import android.util.Log;

public class VKMiniAppFragment extends BaseFragment
{
	public String url;
	
	public WebView web;

	private View close;

	private boolean dark;
	
	public AppBaseActivity activity;
	
	public static VKMiniAppFragment newInstance(String url) {
		Bundle args = new Bundle();
		args.putString("url", url);
		VKMiniAppFragment f = new VKMiniAppFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.vkapp_window, null);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		url = getArguments().getString("url", null);
		dark = PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getBoolean("dark", false);
		setIndeterminate(true);
		
		close = view.findViewById(R.id.close);
		close.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1) {
					close();
				}
			});
		web = view.findViewById(R.id.vkapp_webview);
		web.setVisibility(View.INVISIBLE);
		web.setWebViewClient(new VKMiniAppClient());
		web.setWebChromeClient(new WebChromeClient() {
				@Override
				public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
					android.util.Log.wtf("WebView", consoleMessage.sourceId()+"|| "+consoleMessage.message());
					return true;
				}
		});
		web.getSettings().setJavaScriptEnabled(true);
		web.addJavascriptInterface(new AndroidBridge(this.getContext()), "AndroidBridge");
		web.loadUrl(url);
	}
	
	public void handleEvent(JSONObject response){
		
		try {
			JSONObject r = new JSONObject();
			r.put("detail", response);
			
			StringBuilder builder = new StringBuilder();
			builder.append("window.dispatchEvent(new CustomEvent('VKWebAppEvent', ");
			builder.append(r.toString());
			builder.append("));");
			final String js = "javascript:"+builder.toString();
			web.post(new Runnable() {
					@Override
					public void run() {
						web.evaluateJavascript(js, null);
					}
			});
				
			/*
			 new-instance v0, Lorg/json/JSONObject;

			 invoke-direct {v0}, Lorg/json/JSONObject;-><init>()V

			 const-string v1, "detail"

			 .line 3
			 invoke-virtual {v0, v1, p1}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

			 .line 4
			 new-instance p1, Ljava/lang/StringBuilder;

			 invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

			 const-string v1, "window.dispatchEvent(new CustomEvent(\'VKWebAppEvent\', "

			 invoke-virtual {p1, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

			 invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

			 const-string v0, "));"

			 invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

			 invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

			 move-result-object p1

			 .line 5
			 iget-object v0, p0, Lcom/vk/webapp/bridges/a;->b:Landroid/webkit/WebView;

			 if-eqz v0, :cond_38

			 new-instance v1, Ljava/lang/StringBuilder;

			 invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

			 const-string v2, "javascript:"

			 invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

			 invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

			 invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

			 move-result-object p1

			 invoke-static {v0, p1}, Lcom/vk/webapp/utils/d;->a(Landroid/webkit/WebView;Ljava/lang/String;)V

			 :cond_38
			*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendResponse(String type, JSONObject data){
		JSONObject response = new JSONObject();
		try {
			response.put("type", type);
			response.put("data", data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		handleEvent(response);
	}
	
	public class VKMiniAppClient extends WebViewClient {

		public String theme;
		
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			setIndeterminate(true);
			view.setVisibility(View.INVISIBLE);
		}
		
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
		}

		// Inject CSS method: read style.css from assets folder
		// Append stylesheet to document head
	}
	
	public class AndroidBridge {
		
		double version = 1.0;
		Context mContext;

		public AndroidBridge(Context c) {
			mContext = c;
		}

		@JavascriptInterface
		public void VKWebAppInit(String data) {
			try{
				web.post(new Runnable() {
						@Override
						public void run() {
							String theme = "bright_light";
							if(dark){
								theme = "client_dark";
							}
							web.loadUrl("javascript:document.body.setAttribute('scheme', '"+theme+"');");
							sendResponse("VKWebAppInitDone", new JSONObject());
							web.setVisibility(View.VISIBLE);
							setIndeterminate(false);
						}
					});
				}catch(Exception e){
				Log.wtf("Nashi", e);
			}
		}
		
		@JavascriptInterface
		public void VKWebAppGetUserInfo(String data){
			try{
				web.post(new Runnable() {
						@Override
						public void run() {
							
							getVKSdk().request("users.get", VKParameters.from("fields", "sex,bdate,city,country,photo_100,photo_200,photo_max_org,timezone"), new VKSdk.RequestListener(){

									@Override
									public void onComplete(VKSdk.VKResponse response) {
										JSONObject userdata_vk = response.json.optJSONArray("response").optJSONObject(0);
										sendResponse("VKWebAppGetUserInfoResult", userdata_vk);
									}
	
									@Override
									public void onError(VKError error) {
										sendResponse("VKWebAppGetUserInfoFailed", new JSONObject());
									}
							});
				}});
			}catch(Exception e){
				Log.wtf("Nashi", e);
			}
		}
		
		public void VKWebAppSetViewSettings(final String data){
			try {
				web.post(new Runnable() {
						@Override
						public void run() {
							try {
								JSONObject d = new JSONObject(data);
								Window w = getActivity().getWindow();
								if (d.optString("status_bar_style").equals("light")) {
									w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
								} else {
									w.getDecorView().setSystemUiVisibility(0);
								}
								if (!d.isNull("action_bar_color")) {
									findViewById(R.id.top_space).setBackgroundColor(d.optInt("action_bar_color"));
									w.setStatusBarColor(d.optInt("action_bar_color"));
								}
							} catch (Exception e) {
								sendResponse("VKWebAppSetViewSettingsFailed", new JSONObject());
							}
						}});
			} catch (Exception e) {
				Log.wtf("Nashi", e);
			}
		}
		
		@JavascriptInterface
		public void VKWebAppСlose(String data){
			try {
				JSONObject data_json = new JSONObject(data);
				JSONObject close_result = new JSONObject();
				close_result.put("payload", data_json.optJSONObject("payload"));
				sendResponse("END", close_result);
				close();
			} catch (Exception e) {}
		}
		
		@JavascriptInterface
		public void NashiShowToast(String data) {
			try {
				JSONObject d = new JSONObject(data);
				Toast.makeText(mContext, d.optString("text"), Toast.LENGTH_SHORT).show();
			} catch (JSONException e) {}
		}
	}
}
