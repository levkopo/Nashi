package com.levkopo.apps.nashi.models.widgets;
import org.json.JSONObject;
import com.levkopo.apps.nashi.models.widgets.items.CoverItem;
import java.util.ArrayList;
import org.json.JSONArray;

public class CoverWidget extends BaseWidget
{
	public ArrayList<CoverItem> rows = new ArrayList<>();
	
	public CoverWidget(JSONObject obj){
		super(obj);
		JSONArray r = obj.optJSONArray("rows");
		for(int i = 0; i < r.length(); i++){
			rows.add(new CoverItem(r.optJSONObject(i)));
		}
	}
}
