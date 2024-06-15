package com.levkopo.apps.nashi.models.keyboard;
import org.json.JSONObject;

public class KeyboardAction{
	
	public String type;
	public String payload;
	
	public KeyboardAction(JSONObject object){
		type = object.optString("type");
		payload = object.optString("payload");
	}
}
