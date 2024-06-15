package com.levkopo.vksdk;
import android.text.TextUtils;
import android.util.Pair;
import com.levkopo.vksdk.Request;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.json.JSONObject;
import android.util.Log;
import android.net.Uri;

public class VKSdk
{
	public String token;
	
	public String api_version = "5.103";
	
	public String vkapi_url = "https://api.vk.com/";
	
	public static final String sDefaultStringEncoding = "UTF-8";
	
	public void auth(String username, String password, VKSdk.RequestListener request, String code){
		String ad = "";
		if(code!=null){
			ad ="&code=" + code;
		}
		
		try {
			new Request(request).execute("https://oauth.vk.com/token"
										 + "?grant_type=password"
										 + "&client_id=2274003"
										 + "&client_secret=hHbZxrka2uZ6jB1inYsH"
										 + "&username=" + URLEncoder.encode(username, "utf-8")
										 + "&password=" + URLEncoder.encode(password, "utf-8")
										 + ad
										 );
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public void setAccessToken(String token){
		this.token = token;
	}
	
	public void setApiVersion(String v){
		this.api_version = v;
	}
	
	public void request(String method, Object[] params, RequestListener rl){
		String url = vkapi_url+"method/"+method+"?access_token="+token+"&v="+api_version;
		for (int i = 0; i+1 < params.length; i += 2) {
			try {
				if (params.length > i && params.length > (i + 1))
					url += "&" + URLEncoder.encode(params[i].toString(), sDefaultStringEncoding)+
						"="+URLEncoder.encode(params[i+1].toString(), sDefaultStringEncoding);
			} catch (UnsupportedEncodingException e) {}
        }
		new Request(rl).execute(url);
	}
	
	public interface RequestListener{
		
		public void onComplete(VKResponse response);
		
		public void onError(VKError error);
	}
	
	public static class VKResponse{
		public JSONObject json;
	}
}
