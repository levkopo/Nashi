package com.levkopo.apps.nashi.fragments.dialogs;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.levkopo.apps.nashi.Application;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.activities.BaseActivity;
import com.levkopo.apps.nashi.adapters.OwnersListAdapter;
import com.levkopo.apps.nashi.fragments.UserFragment;
import com.levkopo.apps.nashi.fragments.groups.GroupFragment;
import com.levkopo.apps.nashi.models.ChatModel;
import com.levkopo.apps.nashi.models.GroupModel;
import com.levkopo.apps.nashi.models.OwnerModel;
import com.levkopo.apps.nashi.models.UserModel;
import com.levkopo.apps.nashi.utils.DisplayUtils;
import com.levkopo.apps.nashi.widget.AppFunc;
import com.levkopo.apps.nashi.widget.Avatar;
import com.levkopo.vksdk.VKError;
import com.levkopo.vksdk.VKParameters;
import com.levkopo.vksdk.VKSdk;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;

public class ChatSheetFragment extends BottomSheetDialogFragment
{
	private TextView name;
	
	private Avatar image;
	
	public ChatModel chat;
	
	public int local_id = 1;

	private RecyclerView users_list;

	private BaseActivity activity;

	private AppFunc funcs;

	private Application app;
	
	private boolean adapter_set = false;

	private Button invite_btn;

	private LinearLayout content;
	
	public UpdateChat chat_updater;

	private BaseFragment root;
	
	public static ChatSheetFragment create(BaseFragment fragmen, int local_id, UpdateChat cu) {
		ChatSheetFragment fragment = new ChatSheetFragment();
		fragment.local_id = local_id;
		fragment.chat_updater = cu;
		fragment.root = fragmen;
		
		return fragment;
	}
	
	public static ChatSheetFragment create(BaseFragment fragmen, ChatModel chat, UpdateChat cu) {
		ChatSheetFragment fragment = new ChatSheetFragment();
		fragment.local_id = chat.id;
		fragment.chat = chat;
		fragment.chat_updater = cu;
		fragment.root = fragmen;

		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		int padding = (int) DisplayUtils.convertDpToPixel(10, getContext());
		
		content = new LinearLayout(getContext());
		content.setOrientation(LinearLayout.VERTICAL);

		View header = inflater.inflate(R.layout.chat_header, null);
		name = header.findViewById(R.id.name);
		image = header.findViewById(R.id.image);
		content.addView(header);
		
		invite_btn = new Button(new ContextThemeWrapper(getContext(), R.style.Widget_MaterialComponents_Button_OutlinedButton));
		content.addView(invite_btn);
		LinearLayout.LayoutParams invite_btn_params = (LinearLayout.LayoutParams) invite_btn.getLayoutParams();
		invite_btn_params.rightMargin = padding;
		invite_btn_params.leftMargin = invite_btn_params.rightMargin;
		invite_btn.setLayoutParams(invite_btn_params);
		
		users_list = new RecyclerView(getContext());
		LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		users_list.setLayoutManager(layoutManager);
		users_list.getLayoutManager().setAutoMeasureEnabled(true);
		content.addView(users_list);
		return content;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if(chat!=null)
			updateChat();
			
		refresh();
	}
	
	public void refresh(){
		getVKSdk().request("messages.getChat", VKParameters.from(
				"chat_id", local_id, 
				"fields", "photo_100"
			), new VKSdk.RequestListener(){

				@Override
				public void onComplete(VKSdk.VKResponse response) {
					chat = new ChatModel(response.json.optJSONObject("response"));
					updateChat();
				}

				@Override
				public void onError(VKError error) {}
			});
	}
	
	public void updateChat(){
		if(adapter_set)
			users_list.getAdapter().notifyDataSetChanged();
		else{
			users_list.setAdapter(new OwnersListAdapter(chat.owners));
			adapter_set = true;
		}
		name.setText(chat.title);
		image.setImage(chat.photo_200);

		if(chat.kicked)
			content.removeView(invite_btn);
		else{
			if(chat.left){
				invite_btn.setText(R.string.join_chat);
			}else{
				invite_btn.setText(R.string.leave_chat);
			}
			invite_btn.setOnClickListener(new JoinLeaveChatButtonClick());
		}
	}
	
	//VKSdk
	public VKSdk getVKSdk(){
		return app.sdk;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (Application) getActivity().getApplication();
		funcs = new AppFunc(app);
		if(getActivity() instanceof BaseActivity)
			activity = (BaseActivity) getActivity();
	}
	
	public interface UpdateChat{
		public void updateChat()
	}
	
	public class OnClick implements OwnersListAdapter.onClick {
		@Override 
		public void onClick(OwnerModel owner) {
			if(owner instanceof UserModel){
				UserFragment uf = new UserFragment();
				Bundle arg = new Bundle();
				arg.putInt("id", ((UserModel) owner).id);
				uf.setArguments(arg);
				root.open(uf);
			}else{
				GroupFragment uf = new GroupFragment();
				Bundle arg = new Bundle();
				arg.putInt("group_id", ((GroupModel) owner).id);
				uf.setArguments(arg);
				root.open(uf);
			}
			
			dismiss();
		}
	}
	
	public class JoinLeaveChatButtonClick implements OnClickListener {

		@Override
		public void onClick(View p1) {
			if(chat.left){
				getVKSdk().request("messages.addChatUser", VKParameters.from("chat_id", local_id, "user_id", app.user_id),
					new VKSdk.RequestListener(){

						@Override
						public void onComplete(VKSdk.VKResponse response) {
							refresh();
						}

						@Override
						public void onError(VKError error) {
							Toast.makeText(getContext(), error.error_msg, Toast.LENGTH_SHORT).show();
						}
					});
			}else{
				getVKSdk().request("messages.removeChatUser", VKParameters.from("chat_id", local_id, "user_id", app.user_id),
					new VKSdk.RequestListener(){

						@Override
						public void onComplete(VKSdk.VKResponse response) {
							refresh();
						}

						@Override
						public void onError(VKError error) {
							Toast.makeText(getContext(), error.error_msg, Toast.LENGTH_SHORT).show();
						}
					});
			}
		}
	}
}
