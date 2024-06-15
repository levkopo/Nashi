package com.levkopo.apps.nashi.models.widgets;
import org.json.JSONObject;
import java.util.ArrayList;
import org.json.JSONArray;
import com.levkopo.apps.nashi.models.widgets.items.ListItem;

public class ListWidget extends BaseWidget
{
	public ArrayList<ListItem> rows = new ArrayList<>();
	
	public ListWidget(JSONObject obj){
		super(obj);
		JSONArray r = obj.optJSONArray("rows");
		for(int i = 0; i < r.length(); i++){
			rows.add(new ListItem(r.optJSONObject(i)));
		}
	}
	
	
}
