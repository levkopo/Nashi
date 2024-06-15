package com.levkopo.apps.nashi.models.keyboard;
import org.json.JSONObject;

public class TextKeyboardAction extends KeyboardAction{
	
	public String label;
	
	public TextKeyboardAction(JSONObject object){
		super(object);
		label = object.optString("label");
	}
}
