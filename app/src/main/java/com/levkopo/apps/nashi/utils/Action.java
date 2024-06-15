package com.levkopo.apps.nashi.utils;
import android.content.Intent;
import org.json.JSONObject;

public class Action
{
	public String type;
	
	public JSONObject data;
	
	public Action(JSONObject data){
		this.data = data;
		this.type = data.optString("type");
	}
}
