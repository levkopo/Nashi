package com.levkopo.apps.nashi.models.widgets;
import org.json.JSONObject;

public class TextWidget extends BaseWidget
{
	public String text;
	
	public String descr;
	
	public TextWidget(JSONObject data){
		super(data);
		text = data.optString("text");
		descr = data.optString("descr");
	}
}
