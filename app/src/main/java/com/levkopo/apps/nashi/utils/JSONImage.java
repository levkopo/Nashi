package com.levkopo.apps.nashi.utils;
import org.json.JSONObject;

public class JSONImage
{
	public int width;
	public int height;
	
	public String url;
	
	public JSONImage(JSONObject obj){
		width = obj.optInt("width");
		height = obj.optInt("height");
		
		if(!obj.isNull("url"))
			url = obj.optString("url");
	}
}
