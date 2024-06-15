package com.levkopo.vksdk;
import java.util.Map;
import java.net.URL;
import android.net.Uri;
import java.util.LinkedHashMap;

public class UrlHelper
{
	public static String httpUrl(String url, Object[] args){
		Uri.Builder builder = new Uri.Builder();
		builder.authority(url);
        for (int i = 0; i+1 < args.length; i += 2) {
			if(args.length>i&&args.length>(i+1))
            	builder.appendQueryParameter((String) args[i], args[i + 1].toString());
        }
        
		
		return builder.build().toString();
	}
}
