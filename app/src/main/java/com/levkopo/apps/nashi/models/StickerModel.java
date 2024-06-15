package com.levkopo.apps.nashi.models;
import org.json.JSONObject;

public class StickerModel
{
	public int product_id;
	
	public int sticker_id;
	
	public String url;
	
	public String animation_url;
	
	public StickerModel(JSONObject json){
		product_id = json.optInt("product_id");
		sticker_id = json.optInt("sticker_id");
		url = json.optJSONArray("images").optJSONObject(2).optString("url");
		if(!json.isNull("animation_url"))
			animation_url = json.optString("animation_url");
	}
}
