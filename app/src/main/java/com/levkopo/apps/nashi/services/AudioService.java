package com.levkopo.apps.nashi.services;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.levkopo.apps.nashi.R;
import com.levkopo.apps.nashi.activities.AudioPlayerActivity;
import com.levkopo.apps.nashi.models.AudioModel;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;

public class AudioService extends Service implements MediaPlayer.OnCompletionListener,
MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
MediaPlayerControl,
AudioManager.OnAudioFocusChangeListener {

	private final String TAG = "MusBox";

	// Binder given to clients
	private final IBinder iBinder = new LocalBinder();

	//Media Player
	public MediaPlayer mediaPlayer;
	
	public boolean prepared = false;
	
	public ArrayList<AudioModel> audios = new ArrayList<>();
	
	public int position = 0;
	
	public AudioModel audio;

	//	pause|resume
	private int resumePosition;

	public AudioManager audioManager;
	
	public int duration;

	public MediaController mediaController;

	//MediaSession
	private MediaSessionManager mediaSessionManager;
	private MediaSessionCompat mediaSession;
	private MediaControllerCompat.TransportControls transportControls;

	//AudioPlayer notification ID
	private static final int NOTIFICATION_ID = 4559;

	@Override
	public IBinder onBind(Intent intent) {
		return iBinder;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		//Invoked indicating buffering status of
		//a media resource being streamed over the network.
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		//Invoked when playback of a media source has completed.
		buildNotification(PlaybackStatus.PAUSED);
		stopMedia();
		mp.release();
		if(audios.size()-1>position+1){
			position++;
			audio = audios.get(position);
			buildNotification(PlaybackStatus.PLAYING);
			initMediaPlayer();
		}else
			removeNotification();
	}

	//Handle errors
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {

		Toast.makeText(getApplication(), "Ошибка", Toast.LENGTH_LONG).show();

		//Invoked when there has been an error during an asynchronous operation
		switch (what) {
			case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
				Log.wtf(TAG, "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
				break;
			case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
				Log.wtf(TAG, "MEDIA ERROR SERVER DIED " + extra);
				break;
			case MediaPlayer.MEDIA_ERROR_UNKNOWN:
				Log.wtf(TAG, "MEDIA ERROR UNKNOWN " + extra);
				break;
		}
		
		removeNotification();
		stopSelf();
		stopMedia();
		stopService(new Intent(this, AudioService.class));
		return false;
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		//Invoked to communicate some info.
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		
		prepared = true;
		
		//Invoked when the media source is ready for playback.
		duration = mediaPlayer.getDuration();
		mediaController.setMediaPlayer(this);
			
		playMedia();
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		//Invoked indicating the completion of a seek operation.
	}

	@Override
	public void onAudioFocusChange(int focusState) {
		//Invoked when the audio focus of the system is updated.
		switch (focusState) {
			case AudioManager.AUDIOFOCUS_GAIN:
				// resume playback
				if (mediaPlayer == null) initMediaPlayer();
				else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
				mediaPlayer.setVolume(1.0f, 1.0f);
				break;
			case AudioManager.AUDIOFOCUS_LOSS:
				// Lost focus for an unbounded amount of time: stop playback and release media player
				if (mediaPlayer.isPlaying()) mediaPlayer.stop();
				mediaPlayer.release();
				mediaPlayer = null;
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				// Lost focus for a short time, but we have to stop
				// playback. We don't release the media player because playback
				// is likely to resume
				if (mediaPlayer.isPlaying()) mediaPlayer.pause();
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				// Lost focus for a short time, but it's ok to keep playing
				// at an attenuated level
				if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
				break;
		}
	}

	//The system calls this method when an activity, requests the service be started
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.wtf("Nashi", "OSC");
		
		registerReceiver(new AudioService.AudioReceiver(), 
						new IntentFilter(AudioService.AudioReceiver.PAUSE));
		registerReceiver(new AudioService.AudioReceiver(), 
						 new IntentFilter(AudioService.AudioReceiver.RESUME));
		registerReceiver(new AudioService.AudioReceiver(), 
						 new IntentFilter(AudioService.AudioReceiver.PLAY));
		registerReceiver(new AudioService.AudioReceiver(), 
						 new IntentFilter(AudioService.AudioReceiver.NEXT));
		registerReceiver(new AudioService.AudioReceiver(), 
						 new IntentFilter(AudioService.AudioReceiver.PREVIOUS));
		registerReceiver(new AudioService.AudioReceiver(), 
						 new IntentFilter(AudioService.AudioReceiver.SKIP));
		
		
		Bundle media = intent.getBundleExtra("media");
		audios = media.getParcelableArrayList("audios_list");
		position = media.getInt("pos", 0);
		audio = audios.get(position);
		
		//Request audio focus
		if (requestAudioFocus() == false) {
			//Could not gain focus
			stopSelf();
		}
		
		if (mediaSessionManager == null) {
			try {
				initMediaSession();
				initMediaPlayer();
			} catch (RemoteException e) {
				e.printStackTrace();
				stopSelf();
			}
			buildNotification(PlaybackStatus.PLAYING);
		}

		if (audio != null || audio.url != null || audio.url != "")
			initMediaPlayer();

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mediaPlayer != null) {
			stopMedia();
			mediaPlayer.release();
		}
		removeNotification();
		removeAudioFocus();
	}
	
	private boolean requestAudioFocus() {
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			//Focus gained
			return true;
		}
		//Could not gain focus
		return false;
	}

	private boolean removeAudioFocus() 
	{
		if(audioManager!=null)
			return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
          	  audioManager.abandonAudioFocus(this);
			
		return false;
	}

	private void initMediaPlayer() {
		
		prepared = false;
		
		mediaPlayer = new MediaPlayer();
		mediaController = new MediaController(this);
		//Set up MediaPlayer event listeners
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setOnSeekCompleteListener(this);
		mediaPlayer.setOnInfoListener(this);
		//Reset so that the MediaPlayer is not pointing to another data source
		mediaPlayer.reset();

		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			// Set the data source to the mediaFile location
			mediaPlayer.setDataSource(audio.url);
		} catch (IOException e) {
			e.printStackTrace();
			stopSelf();
		}
		mediaPlayer.prepareAsync();
	}

	private void playMedia() {
		if (!mediaPlayer.isPlaying()) {
			mediaPlayer.start();
		}
	}

	private void stopMedia() {
		if (mediaPlayer == null) return;
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.stop();
		}
	}

	private void pauseMedia() {
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			resumePosition = mediaPlayer.getCurrentPosition();
		}
	}

	private void resumeMedia() {
		if (!mediaPlayer.isPlaying()) {
			mediaPlayer.seekTo(resumePosition);
			mediaPlayer.start();
		}
	}
	
	private void skipToNext() {

		if (position == audios.size() - 1) {
			//if last in playlist
			position = 0;
			audio = audios.get(position);
		} else {
			//get next in playlist
			audio = audios.get(++position);
		}
		
		stopMedia();
		//reset mediaPlayer
		mediaPlayer.reset();
		initMediaPlayer();
	}

	private void skipToPrevious() {

		if (position == 0) {
			//if first in playlist
			//set index to the last of audioList
			position = audios.size() - 1;
			audio = audios.get(position);
		} else {
			//get previous in playlist
			audio = audios.get(--position);
		}
		stopMedia();
		//reset mediaPlayer
		mediaPlayer.reset();
		initMediaPlayer();
	}
	
	@Override
	public void start() {
		mediaPlayer.start();
	}

	@Override
	public void pause() {
		mediaPlayer.pause();
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Override
	public int getCurrentPosition() {
		return mediaPlayer.getCurrentPosition();
	}

	@Override
	public void seekTo(int p1) {
		mediaPlayer.seekTo(p1);
	}

	@Override
	public boolean isPlaying() {
		return mediaPlayer.isPlaying();
	}

	@Override
	public int getBufferPercentage() {
		return 0;
	}

	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getAudioSessionId() {
		return mediaPlayer.getAudioSessionId();
	}

	public class LocalBinder extends Binder {
		public AudioService getService() {
			return AudioService.this;
		}
	}
	
	private void initMediaSession() throws RemoteException {
		if (mediaSessionManager != null) return; //mediaSessionManager exists

		mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
		// Create a new MediaSession
		mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
		//Get MediaSessions transport controls
		transportControls = mediaSession.getController().getTransportControls();
		//set MediaSession -> ready to receive media commands
		mediaSession.setActive(true);
		//indicate that the MediaSession handles transport control commands
		// through its MediaSessionCompat.Callback.
		mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

		//Set mediaSession's MetaData
		updateMetaData();

		// Attach Callback to receive MediaSession updates
		mediaSession.setCallback(new MediaSessionCompat.Callback() {
				// Implement callbacks
				@Override
				public void onPlay() {
					super.onPlay();
					resumeMedia();
					buildNotification(PlaybackStatus.PLAYING);
				}

				@Override
				public void onPause() {
					super.onPause();
					pauseMedia();
					buildNotification(PlaybackStatus.PAUSED);
				}

				@Override
				public void onSkipToNext() {
					super.onSkipToNext();
					skipToNext();
					updateMetaData();
					buildNotification(PlaybackStatus.PLAYING);
				}

				@Override
				public void onSkipToPrevious() {
					super.onSkipToPrevious();
					skipToPrevious();
					updateMetaData();
					buildNotification(PlaybackStatus.PLAYING);
				}

				@Override
				public void onStop() {
					super.onStop();
					removeNotification();
					//Stop the service
					stopSelf();
				}

				@Override
				public void onSeekTo(long position) {
					super.onSeekTo(position);
				}
			});
	}

	private void updateMetaData() {
		//Bitmap albumArt = BitmapUtils.getBitmapFromURL(audio.album.photo_300);//replace with medias albumArt
		// Update the current metadata
		String album = "Неизвестный";
		if(audio.album!=null)
			album = audio.album.title;
		mediaSession.setMetadata(new MediaMetadataCompat.Builder()
								// .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
								 .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, audio.artist)
								 .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
								 .putString(MediaMetadataCompat.METADATA_KEY_TITLE, audio.title)
								 .build());
	}
	
	private void buildNotification(PlaybackStatus playbackStatus) {

		int notificationAction = R.drawable.ic_round_pause_black_36;//needs to be initialized
		PendingIntent play_pauseAction = null;

		//Build a new notification according to the current state of the MediaPlayer
		if (playbackStatus == PlaybackStatus.PLAYING) {
			notificationAction = R.drawable.ic_round_pause_black_36;
			//create the pause action
			play_pauseAction = playbackAction(1);
		} else if (playbackStatus == PlaybackStatus.PAUSED) {
			notificationAction = R.drawable.ic_round_play_arrow_black_36;
			//create the play action
			play_pauseAction = playbackAction(0);
		}

		// Create a new Notification
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "audio_service")
            .setShowWhen(false)
            // Set the Notification style
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
					  // Attach our MediaSession token
					  .setMediaSession(mediaSession.getSessionToken())
					  .setShowCancelButton(true)
					  // Show our playback controls in the compact notification view.
					  .setShowActionsInCompactView(0, 1, 2))
            // Set the Notification color
            .setColor(getResources().getColor(R.color.light_gray))
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setShowWhen(false)
			.setContentTitle(audio.title)
			.setContentText(audio.artist)
			.setSmallIcon(notificationAction)
			.setWhen(0)
			.setAutoCancel(false)
			.setContentIntent(playbackAction(4))
			.setOngoing(true)
            // Add playback action
            .addAction(R.drawable.ic_round_skip_previous_black_36, "previous", playbackAction(3))
            .addAction(notificationAction, "pause", play_pauseAction)
            .addAction(R.drawable.ic_round_skip_next_black_36, "next", playbackAction(2))
			.addAction(R.drawable.ic_round_skip_next_black_36, "skip", playbackAction(5));

		if(audio.album!=null){
			notificationBuilder.setCategory(audio.album.title);
		}
		NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(this);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			String channelId = "nashi_chanel_media";
			NotificationChannel channel = new NotificationChannel(
				channelId,
				"Nashi",
				NotificationManager.IMPORTANCE_LOW);
			mNotificationManager.createNotificationChannel(channel);
			notificationBuilder.setChannelId(channelId);
		}
		
		if(audio.album!=null)
			new loadIcon(notificationBuilder, mNotificationManager, NOTIFICATION_ID).execute(audio.album.photo_600);
		else{
			mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
		}
	}

	private void removeNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFICATION_ID);
	}
	
	private PendingIntent playbackAction(int actionNumber) {
		Intent playbackAction;
		
		switch (actionNumber) {
			case 0:
				// Play
			    playbackAction = new Intent(AudioReceiver.RESUME);
				return PendingIntent.getBroadcast(this, actionNumber, playbackAction, 0);
			case 1:
				// Pause
			    playbackAction = new Intent(AudioReceiver.PAUSE);
				return PendingIntent.getBroadcast(this, actionNumber, playbackAction, 0);
			case 2:
				// Next track
				playbackAction = new Intent(AudioReceiver.NEXT);
				return PendingIntent.getBroadcast(this, actionNumber, playbackAction, 0);
			case 3:
				// Previous track
				playbackAction = new Intent(AudioReceiver.PREVIOUS);
				return PendingIntent.getBroadcast(this, actionNumber, playbackAction, 0);
			case 4:
				playbackAction = new Intent(this, AudioPlayerActivity.class);
				return PendingIntent.getActivity(this, actionNumber, playbackAction, 0);
			case 5:
				// Skip music
				playbackAction = new Intent(AudioReceiver.SKIP);
				return PendingIntent.getBroadcast(this, actionNumber, playbackAction, 0);
		}
		return null;
	}
	
	public enum PlaybackStatus {
		PLAYING,
		PAUSED
	}

	public class AudioReceiver extends BroadcastReceiver {

		public final static String PAUSE = "com.levkopo.apps.nashi.PAUSE";
		
		public final static String RESUME = "com.levkopo.apps.nashi.RESUME";
		
		public final static String PLAY = "com.levkopo.apps.nashi.PLAY";
		
		public final static String NEXT = "com.levkopo.apps.nashi.NEXT";
		
		public final static String PREVIOUS = "com.levkopo.apps.nashi.PREVIOUS";
		
		public final static String SKIP = "com.levkopo.apps.nashi.SKIP";
		
		@Override
		public void onReceive(Context p1, Intent p2) {
			updateMetaData();
			switch(p2.getAction()){
				case PAUSE:{
					buildNotification(PlaybackStatus.PAUSED);
					pauseMedia();
				}
				break;
				
				case RESUME:{
					buildNotification(PlaybackStatus.PLAYING);
					resumeMedia();
				}
				break;
				
				case PLAY:{
						Bundle media = p2.getBundleExtra("media");
						audios = media.getParcelableArrayList("audios_list");
						position = media.getInt("pos", 0);
						audio = audios.get(position);
						buildNotification(PlaybackStatus.PLAYING);
						
						stopMedia();
						mediaPlayer.reset();
						initMediaPlayer();
				}break;
				
				case NEXT:{
						stopMedia();
						mediaPlayer.release();
						if(position+1<audios.size()-1){
							position++;
						}else{
							position = 0;
						}
						audio = audios.get(position);
						buildNotification(PlaybackStatus.PLAYING);
						initMediaPlayer();
				}
				break;
				
				case PREVIOUS:{
						stopMedia();
						mediaPlayer.release();
						if(position>=0){
							position--;
							audio = audios.get(position);
							initMediaPlayer();
							buildNotification(PlaybackStatus.PLAYING);
						}
				}
				break;
				
				case SKIP:{
					removeNotification();
					stopMedia();
					stopSelf();
					stopService(new Intent(p1, AudioService.class));
				}
				break;
			}
		}
	}
	
	private class loadIcon extends AsyncTask<String, Void, Bitmap> {

        NotificationCompat.Builder notif;
        NotificationManagerCompat m;

		private int id;

        public loadIcon(NotificationCompat.Builder notif, NotificationManagerCompat m, int id) {
            super();
            this.notif = notif;
			this.m = m;
			this.id = id;
        }

        @Override
        protected Bitmap doInBackground(String... params) {

            InputStream in;
            try {

				URL url = new URL(params[0]);
				HttpURLConnection connection = (HttpURLConnection)url.openConnection();
				connection.setDoInput(true);
				connection.connect();
				in = connection.getInputStream();
				Bitmap myBitmap = BitmapFactory.decodeStream(in);
				return myBitmap;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {

            super.onPostExecute(result);
            try {
				if(result!=null)
             	   notif.setLargeIcon(result);
				m.notify(id, notif.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

