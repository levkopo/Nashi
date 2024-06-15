package com.levkopo.apps.nashi.fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.adapters.DialogAdapter;
import com.levkopo.apps.nashi.fragments.groups.GroupFragment;
import com.levkopo.apps.nashi.models.DialogItem;
import com.levkopo.apps.nashi.models.GroupModel;
import com.levkopo.apps.nashi.models.MessageItem;
import com.levkopo.apps.nashi.models.UserModel;
import com.levkopo.vksdk.VKError;
import com.levkopo.vksdk.VKParameters;
import com.levkopo.vksdk.VKSdk;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.levkopo.apps.nashi.widget.Avatar;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;
import com.levkopo.apps.nashi.models.OwnerModel;
import com.levkopo.apps.nashi.fragments.dialogs.ChatSheetFragment;
import com.levkopo.apps.nashi.widget.TextInput;
import android.widget.FrameLayout;
import android.view.Gravity;
import com.levkopo.vksdk.VKSdk.VKResponse;
import com.levkopo.apps.nashi.services.LongPollService;
import com.levkopo.apps.nashi.models.Conversation;
import android.util.Log;

public class DialogFragment extends ToolbarFragment implements TextInput.InputSend, LongPollService.OnNewUpdate
{
	private TextView subtitle;

	private TextView name;

	private Avatar avatar;

	private Conversation conversation;

	private RecyclerView list;
	
	public ArrayList<MessageItem> items = new ArrayList<>();

	private TextInput input;

	private DialogAdapter adapter;
	
	public static DialogFragment newInstance(Conversation conversation) {
		DialogFragment f = new DialogFragment();
		f.conversation = conversation;
		return f;
	}
	
	public static DialogFragment newInstance(OwnerModel model) {
		Conversation conversation = new Conversation(model);
		return newInstance(conversation);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app.updates.add(this);
		adapter = new DialogAdapter(this, items);
	}

	@Override
	public View createContent(LayoutInflater inflater, Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.chat_layout, null);
		
		list = view.findViewById(R.id.list);
		list.setOverScrollMode(list.OVER_SCROLL_NEVER);
		list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
		((LinearLayoutManager) list.getLayoutManager()).setStackFromEnd(true);
		list.getLayoutManager().setAutoMeasureEnabled(true);
		list.setAdapter(adapter);
		
		input = view.findViewById(R.id.input);
		input.setSendListener(this);
		
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		hideBottomLayout();
		useSpectator(false);
		setIndeterminate(true);
		toolbar.setTitle("");
		
		View header = getLayoutInflater().inflate(R.layout.dialog_header, null);
		header.setOnClickListener(new OnOpenPeerInfoFragmentListener(conversation.peer_id, this));
		name = header.findViewById(R.id.dialog_name);
		subtitle = header.findViewById(R.id.dialog_subtitle);
		avatar = header.findViewById(R.id.dialog_image);
		toolbar.addView(header);	
		
		updateConversationUI();
		loadConversation();
		loadHistory(0);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		showBottomLayout();
	}
	
	@Override
	public void send(String text, String payload){
		Log.d("DF", "Send message: "+text);
		
		getVKSdk().request("messages.send", VKParameters.from(
			"peer_id", conversation.peer_id,
			"message", text,
			"payload", payload,
			"random_id", "45" + (System.currentTimeMillis() / 1000L)
		), 
			new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					Log.wtf("Nashi", "|messages.send result: "+response.json.toString());
				}

				@Override
				public void onError(VKError error) {
					Log.wtf("Nashi", "|messages.send error");
				}
		});
	}
	
	@Override
	public void onUpdate(int type, JSONArray data) {
		if(type==LongPollService.NEW_MESSAGE){
			int peer_id = data.optInt(3);
			if(
				conversation.peer_id == peer_id ||
				(conversation.peer_id*-1) == (peer_id - 1000000000) 
			){
				getVKSdk().request("messages.getById", 
					VKParameters.from(
						"message_ids", data.optInt(1)
					),
					new VKSdk.RequestListener(){

					public void onComplete(VKSdk.VKResponse response) {
						JSONObject message = response.json.optJSONObject("response").
													optJSONArray("items")
													.optJSONObject(0);
						addMessage(new MessageItem(message));
					}

					public void onError(VKError error) {}
				});
			}
		}
	}
	
	public void addMessage(MessageItem item){
		items.add(item);
		adapter.notifyDataSetChanged();
	}
	
	public void updateConversationUI(){
		if(conversation.keyboard!=null){
			input.setKeyboard(conversation.keyboard);
		}
		
		if(conversation.peer_type.equals("user")){
			UserModel owner = (UserModel) conversation.owner;
			name.setText(owner.first_name + " " + owner.last_name);
			if(owner.photo_100!=null)
				avatar.setImage(owner.photo_100);
		}else if(conversation.peer_type.equals("group")){
			GroupModel owner = (GroupModel) conversation.owner;
			name.setText(owner.name);
			subtitle.setText(getResources().getString(R.string.group));
			if(owner.photo_100!=null)
				avatar.setImage(owner.photo_100);
		}else if(conversation.peer_type.equals("chat")){
			name.setText(conversation.chat.title);
			int str_pattern = R.string.one_member;
			if(conversation.chat.members_count<=3&&conversation.chat.members_count!=1)
				str_pattern = R.string.member_count;
			if(conversation.chat.members_count>3)
				str_pattern = R.string.members_count;
				
			subtitle.setText(String.format(getResources().getString(str_pattern),
				conversation.chat.members_count));
				
			if(conversation.chat.photo_100!=null)
				avatar.setImage(conversation.chat.photo_100);
				
			if(conversation.chat.kicked||conversation.chat.left)
				input.setVisibility(View.GONE);
		}
	}
	
	public void loadConversation(){
		getVKSdk().request("messages.getConversationsById", VKParameters.from("peer_ids", conversation.peer_id), new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					conversation = new Conversation(response.json.optJSONObject("response"));
					updateConversationUI();
				}

				@Override
				public void onError(VKError error) {
					showErrorScreen();
				}
			});
	}
	
	public void loadHistory(int offset){
		getVKSdk().request("messages.getHistory", VKParameters.from(
			"peer_id", conversation.peer_id,
			"count", 20,
			"extended", 1
			
		), new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					//Off refreshing animations
					setIndeterminate(false);
					Log.wtf("Nashi", "|D: "+response.json.toString());
					//Log.wtf("Nashi", response.json.toString());
					try {

						Collections.reverse(items);
						JSONObject response_json = response.json.getJSONObject("response");
						JSONArray items = response_json.getJSONArray("items");
						for(int i = 0; i<items.length(); i++){
							DialogFragment.this.items.add(new MessageItem(items.getJSONObject(i)));
						}
						Collections.reverse(DialogFragment.this.items);
						adapter.notifyDataSetChanged();
					} catch (JSONException e) {}
				}

				@Override
				public void onError(VKError error) {
					//Off refreshing animations
					setIndeterminate(false);
					showErrorScreen();
				}
			});
	}
	
	public class OnOpenPeerInfoFragmentListener implements OnClickListener {

		public int peer_id;
		
		public BaseFragment fr;

		public OnOpenPeerInfoFragmentListener(int peer_id, BaseFragment fr) {
			this.peer_id = peer_id;
			this.fr = fr;
		}
		
		@Override
		public void onClick(View p1) {
			if(conversation.peer_type.equals("user")){
				GroupFragment uf = new GroupFragment();
				Bundle arg = new Bundle();
				arg.putInt("group_id", peer_id*-1);
				uf.setArguments(arg);
				fr.open(uf);
			}else if(conversation.peer_type.equals("group")){
				UserFragment uf = new UserFragment();
				Bundle arg = new Bundle();
				arg.putInt("id", peer_id);
				uf.setArguments(arg);
				fr.open(uf);
			}else if(conversation.peer_type.equals("chat")){
				ChatSheetFragment chat_sheet = ChatSheetFragment.create(DialogFragment.this, conversation.chat, new Updater());
				chat_sheet.show(getFragmentManager(), "chat"+conversation.chat.id);
			}
		}
	}
	
	public class Updater implements ChatSheetFragment.UpdateChat {

		@Override
		public void updateChat() {
			loadConversation();
		}
	}
}
