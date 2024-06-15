package com.levkopo.vksdk;
import org.json.JSONObject;

public class VKError
{
	public int error_code;
	
	public String error_msg;
	
	public JSONObject json;
	
	public static VKError bind(JSONObject json){
		VKError err = new VKError();
		
		err.error_code = json.optInt("error_code");
		err.error_msg = json.optString("error_msg");
		err.json = json;
		
		return err;
	}

	@Override
	public String toString() {
		return "VKError:{error_code:"+error_code+
		"error_msg:"+error_msg;
	}
}
