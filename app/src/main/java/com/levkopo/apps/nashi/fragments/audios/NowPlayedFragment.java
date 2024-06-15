package com.levkopo.apps.nashi.fragments.audios;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.activities.AudioPlayerActivity;
import com.levkopo.apps.nashi.fragments.base.BaseFragment;
import com.levkopo.apps.nashi.models.AudioModel;
import com.levkopo.apps.nashi.services.AudioService;
import com.levkopo.apps.nashi.utils.OnTouchListener;
import java.util.concurrent.TimeUnit;

public class NowPlayedFragment extends BaseFragment
{
	
	private TextView title;

	private TextView artist;

	private ImageView image;

	private ImageView middle_btn;

	private ImageView previous_btn;

	private ImageView next_btn;

	private SeekBar audio_seek;
	
	private Handler mHandler = new Handler();

	private TextView currentPosition;

	private TextView duration;

	private AudioService service;

	private AudioModel audio;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.audio_player_layout, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		service = app.player;
		audio = service.audio;
		
		title = (TextView) findViewById(R.id.title);
		artist = (TextView) findViewById(R.id.artist);
		image = (ImageView) findViewById(R.id.image);
		currentPosition = (TextView) findViewById(R.id.currentPosition);
		duration = (TextView) findViewById(R.id.duration);
		audio_seek = (SeekBar) findViewById(R.id.audio_seek);
		middle_btn = (ImageView) findViewById(R.id.middle_button);
		previous_btn = (ImageView) findViewById(R.id.previous);
		next_btn = (ImageView) findViewById(R.id.next);

		findViewById(R.id.skip).setOnClickListener(new OnClickListener(){
				@Override public void onClick(View p1) {
					getActivity().finish();
				}});
		middle_btn.setOnTouchListener(new OnTouchListener(middle_btn));
		middle_btn.setOnClickListener(new OnContolButtonClickListener());
		next_btn.setOnTouchListener(new OnTouchListener(next_btn));
		next_btn.setOnClickListener(new OnContolButtonClickListener());
		previous_btn.setOnTouchListener(new OnTouchListener(previous_btn));
		previous_btn.setOnClickListener(new OnContolButtonClickListener());


		audio_seek.setOnSeekBarChangeListener(new OnAudioSeekChange());
		updateAudio(audio);

		//Make sure you update Seekbar on UI thread
		getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if(service.audio!=null&&service.audio!=NowPlayedFragment.this.audio){
						updateAudio(service.audio);
					}
					
					if(service.prepared){
						audio_seek.setMax(service.duration/1000);
						duration.setText(convertDurationMillis(service.duration));

						int mCurrentPosition = service.mediaPlayer.getCurrentPosition() / 1000;
						audio_seek.setProgress(mCurrentPosition);

						if(service.mediaPlayer.isPlaying()){
							middle_btn.setImageResource(R.drawable.ic_round_pause_black_48);
						}else{
							middle_btn.setImageResource(R.drawable.ic_round_play_arrow_black_48);
						}
					}

					if(service.prepared){
						audio_seek.setIndeterminate(false);
					}else{
						audio_seek.setIndeterminate(true);
					}
					if(mHandler!=null)
						mHandler.postDelayed(this, 50);
				}});
	}
	public void updateAudio(AudioModel newAudio){
		this.audio = newAudio;
		title.setText(newAudio.title);
		artist.setText(newAudio.artist);

		RequestOptions ro = new RequestOptions();
		ro = ro.transform(new CenterCrop(), new RoundedCorners((int) getResources().getDimension(R.dimen.audio_album_image_round)));
		if(audio.album!=null)
			Glide.with(this)
				.load(newAudio.album.photo_600)
				.apply(ro)
				.into(image);
	}
	
	public String convertDurationMillis(Integer getDurationInMillis){
		int getDurationMillis = getDurationInMillis;

		String convertHours = String.format("%02d", TimeUnit.MILLISECONDS.toHours(getDurationMillis)); 
		String convertMinutes = String.format("%02d", TimeUnit.MILLISECONDS.toMinutes(getDurationMillis) -
											  TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(getDurationMillis))); //I needed to add this part.
		String convertSeconds = String.format("%02d", TimeUnit.MILLISECONDS.toSeconds(getDurationMillis) -
											  TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getDurationMillis)));


		String getDuration = convertHours + ":" + convertMinutes + ":" + convertSeconds;

		return getDuration;

	}

	@Override
	public void onDetach() {
		super.onDetach();
		mHandler = null;
	}

	public class OnAudioSeekChange implements SeekBar.OnSeekBarChangeListener{

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {                
			currentPosition.setText(convertDurationMillis(progress * 1000));
			if(service.mediaPlayer != null && fromUser){
				service.mediaPlayer.seekTo(progress * 1000);
			}
		}
	}

	public class OnContolButtonClickListener implements OnClickListener {

		@Override
		public void onClick(View p1) {
			switch(p1.getId()){
				case R.id.middle_button:{
						Intent intent;
						if(service.mediaPlayer.isPlaying()){
							intent = new Intent(AudioService.AudioReceiver.PAUSE);
						}else{
							intent = new Intent(AudioService.AudioReceiver.RESUME);
						}

						intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
						getContext().sendBroadcast(intent);
					}break;

				case R.id.next:{
						Intent intent = new Intent(AudioService.AudioReceiver.NEXT);

						intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
						getContext().sendBroadcast(intent);
					}break;

				case R.id.previous:{
						Intent intent = new Intent(AudioService.AudioReceiver.PREVIOUS);

						intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
						getContext().sendBroadcast(intent);
					}break;
			}
		}

	}
}
