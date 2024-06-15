package com.levkopo.requestlib;
import org.json.JSONObject;

public interface RequestListener
{
	public void onResponse(JSONObject object);
	public void onError(String error_msg);
}
