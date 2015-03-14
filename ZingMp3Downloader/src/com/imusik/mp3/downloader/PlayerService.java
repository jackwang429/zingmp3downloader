package com.imusik.mp3.downloader;

import com.idroid.lib.support.MyLog;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PlayerService extends Service {
	
	// values
//	public static final String COMMAND_PLAY_NEW = "command_play_new";
//	public static final String COMMAND_RESUME = "command_resume";
//	public static final String COMMAND_PAUSE = "command_pause";
	
	public static final String ACTION_PLAYERSERVICE = "zing.mp3.PlayerService";
	
	public static final String EXTRA_NAME = "extra_name";
	public static final String EXTRA_ARTIST = "extra_artist";
	public static final String EXTRA_ID = "extra_id";
	public static final String EXTRA_DOWNLOADLINK = "extra_downloadlink";
	public static final String EXTRA_SEEK_TO = "extra_seek_to";
	public static final String EXTRA_COUNTDOWN_VALUE = "extra_countdown_value";
	public static final String EXTRA_PROCESS_PERCENT = "extra_process_percent";
	public static final String EXTRA_REPEAT_ONE = "extra_repeat_one";
	
	public static final String EXTRA_COMMAND = "extra_command";	
	public static final String EXTRA_BROADCAST = "extra_broadcast";
	
	public static final String BROADCAST_COMPLETE_SONG = "broadcast_complete_song";
	public static final String BROADCAST_UPDATE_PROGRESS_MAIN = "broadcast_update_progress_main";
	public static final String BROADCAST_UPDATE_PROGRESS_SECOND = "broadcast_update_progress_second";
	public static final String BROADCAST_NOWPLAYING_START = "broadcast_nowplaying_play";
	public static final String BROADCAST_NOWPLAYING_STOP = "broadcast_nowplaying_stop";
	public static final String BROADCAST_COUNTDOWN_TIME_LEFT = "broadcast_countdown_time_left";
	public static final String BROADCAST_COUNTDOWN_TIME_OUT = "broadcast_countdown_time_out";

	public static final String COMMAND_PLAY_NEW = "command_play_new";
	public static final String COMMAND_NOWPLAYING = "command_nowplaying";
	public static final String COMMAND_STOP_SERVICE = "command_stop_service";
	public static final String COMMAND_SEEK_TO = "command_seek_to";
	public static final String COMMAND_COUNTDOWN_TIME = "command_sleep_time";
	
	public static final int NOTIFY_ID = 0x2222;

	public static final String COMMAND_PHONE_RINGING_OR_OFFHOOK = "command_phone_ringing_or_offhook";
	public static final String COMMAND_PHONE_IDLE = "command_phone_idle";
	private static final String PLAY_STATE_PLAYING = "play_state_playing";
	private static final String PLAY_STATE_PAUSING = "play_state_pausing";
	
	String lastPlayState;
	MediaPlayer mMediaPlayer;
	Handler mHandler;
	
	String id;
	String name;
	String artist;
	
	boolean isLoading;
	
	// countdown time to sleep
	Handler timeHandler = new Handler();
	long startCountdownTime;
	int countDownValue;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();

		mHandler = new Handler();
		initMediaPlayer();

	}

	@Override
	public int onStartCommand(final Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		if (intent == null) {
			MyLog.log("onstartCommand intent = nullllllllllllllllllllllllllll");
			return super.onStartCommand(intent, flags, startId);
		}
		
		MyLog.log("PlayerService onStartCommand PlayerService, command=" + intent.getStringExtra(EXTRA_COMMAND));

		String extraCommand = intent.getStringExtra(EXTRA_COMMAND);
		if (extraCommand.equals(COMMAND_PLAY_NEW)) {
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.stop();
			}

			if (!isLoading) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						try {
							isLoading = true;

							name = intent.getStringExtra(EXTRA_NAME);
							artist = intent.getStringExtra(EXTRA_ARTIST);
							id = intent.getStringExtra(EXTRA_ID);
							
							String mp3Link;
							
							String checkSongExist = ZingTools.checkSongOrVideoFileExist(id);
							if (checkSongExist != null) {
								mp3Link = checkSongExist;
								
								// display second progress full
								Intent broadcastIntent = new Intent();
								broadcastIntent.setAction(ACTION_PLAYERSERVICE);
								broadcastIntent.putExtra(EXTRA_BROADCAST, BROADCAST_UPDATE_PROGRESS_SECOND);
								broadcastIntent.putExtra(EXTRA_PROCESS_PERCENT, 100);
								broadcastIntent.putExtra(EXTRA_NAME, name);
								broadcastIntent.putExtra(EXTRA_ARTIST, artist);
								sendBroadcast(broadcastIntent);								
							} else {
								mp3Link = ZingMp3Tools.getDownloadLink(id);
							}

							mMediaPlayer.reset();
							mMediaPlayer.setDataSource(mp3Link);
							mMediaPlayer.prepare();
							mMediaPlayer.start();

							Intent broadcastIntent = new Intent();
							broadcastIntent.setAction(ACTION_PLAYERSERVICE);
							broadcastIntent.putExtra(EXTRA_BROADCAST, BROADCAST_NOWPLAYING_START);
							sendBroadcast(broadcastIntent);
							
							// show playing notification
							showNotify();
							
							isLoading = false;
						} catch (Exception e) {
							// TODO: handle exception
							e.printStackTrace();
							isLoading = false;
						}
					}
				}).start();
			}
		} else if (extraCommand.equals(COMMAND_NOWPLAYING) && id != null) {
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.pause();

				Intent broadcastIntent = new Intent();
				broadcastIntent.setAction(ACTION_PLAYERSERVICE);
				broadcastIntent.putExtra(EXTRA_BROADCAST, BROADCAST_NOWPLAYING_STOP);
				sendBroadcast(broadcastIntent);
			} else if (!mMediaPlayer.isPlaying()) {
				String extraRepeatOne = intent.getStringExtra(EXTRA_REPEAT_ONE);
				if (extraRepeatOne != null) {
					showNotify();
				}
				
				if (ZingTools.checkSongOrVideoFileExist(id) != null) {
					Intent broadcastIntentProgress = new Intent();
					broadcastIntentProgress.setAction(ACTION_PLAYERSERVICE);
					broadcastIntentProgress.putExtra(EXTRA_BROADCAST, BROADCAST_UPDATE_PROGRESS_SECOND);
					broadcastIntentProgress.putExtra(EXTRA_PROCESS_PERCENT, 100);
					sendBroadcast(broadcastIntentProgress);					
				}				
				
				mMediaPlayer.start();
				updateProgress();

				Intent broadcastIntent = new Intent();
				broadcastIntent.setAction(ACTION_PLAYERSERVICE);
				broadcastIntent.putExtra(EXTRA_BROADCAST, BROADCAST_NOWPLAYING_START);
				sendBroadcast(broadcastIntent);
				

			}
		} else if (extraCommand.equals(COMMAND_STOP_SERVICE)) {
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.stop();
			}
			
			hideNotify();
		} else if (extraCommand.equals(COMMAND_PHONE_RINGING_OR_OFFHOOK)) {
			if (mMediaPlayer != null ) {
				if (mMediaPlayer.isPlaying()) {
					lastPlayState = PLAY_STATE_PLAYING;
					mMediaPlayer.pause();					
				} else {
					lastPlayState = PLAY_STATE_PAUSING;
				}
			}
		} else if (extraCommand.equals(COMMAND_PHONE_IDLE)) {
			if (lastPlayState != null && lastPlayState.equals(PLAY_STATE_PLAYING)) {
				if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
					mMediaPlayer.start();
				}
			}
		} else if (extraCommand.equals(COMMAND_SEEK_TO)) {
			int seekToPercent = intent.getIntExtra(EXTRA_SEEK_TO, 0);
			if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
				int seekToPosition = (int) (mMediaPlayer.getDuration() * 1.0 * seekToPercent / 100);
				mMediaPlayer.seekTo(seekToPosition);
			}
		} else if (extraCommand.equals(COMMAND_COUNTDOWN_TIME)) {
			startCountdownTime = System.currentTimeMillis();
			countDownValue = intent.getIntExtra(EXTRA_COUNTDOWN_VALUE, 0);
			
			if (countDownValue > 0) {
				startCountDown(startCountdownTime);
			}
			
		}

		return super.onStartCommand(intent, flags, startId);
	}

	public void initMediaPlayer() {
		if (mMediaPlayer == null) {
			mMediaPlayer = new MediaPlayer();
		}

		mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
				Intent broadcastIntent = new Intent();
				broadcastIntent.setAction(ACTION_PLAYERSERVICE);
				broadcastIntent.putExtra(EXTRA_BROADCAST, BROADCAST_COMPLETE_SONG);
				sendBroadcast(broadcastIntent);
				
				hideNotify();
			}
		});

		mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
				updateProgress();
			}
		});

		mMediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
			@Override
			public void onBufferingUpdate(MediaPlayer mp, int percent) {
				// TODO Auto-generated method stub
				Intent broadcastIntent = new Intent();
				broadcastIntent.setAction(ACTION_PLAYERSERVICE);
				broadcastIntent.putExtra(EXTRA_BROADCAST, BROADCAST_UPDATE_PROGRESS_SECOND);
				broadcastIntent.putExtra(EXTRA_PROCESS_PERCENT, percent);
				broadcastIntent.putExtra(EXTRA_NAME, name);
				broadcastIntent.putExtra(EXTRA_ARTIST, artist);
				sendBroadcast(broadcastIntent);
			}
		});
	}

	public void updateProgress() {
		if (mMediaPlayer.isPlaying()) {
			int percent = (int) (mMediaPlayer.getCurrentPosition() * 1.0 / mMediaPlayer.getDuration() * 100);
			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction(ACTION_PLAYERSERVICE);
			broadcastIntent.putExtra(EXTRA_BROADCAST, BROADCAST_UPDATE_PROGRESS_MAIN);
			broadcastIntent.putExtra(EXTRA_PROCESS_PERCENT, percent);
			broadcastIntent.putExtra(EXTRA_NAME, name);
			broadcastIntent.putExtra(EXTRA_ARTIST, artist);
			sendBroadcast(broadcastIntent);

			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					updateProgress();
				}
			}, 500);
		}
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		hideNotify();
		
		if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
			mMediaPlayer.stop();
		}
	}
	
	public void showNotify() {
		Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
		
		Notification notification = new NotificationCompat.Builder(getApplicationContext())
										.setContentTitle(name)
										.setContentText(artist)
										.setSmallIcon(R.drawable.ic_cover_blank_super_tiny)
										.setContentIntent(pendingIntent)
										.setTicker(name + " - " + artist)
										.build();
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFY_ID, notification);		
	}
	
	public void hideNotify() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFY_ID);
	}
	
	/** ------------ COUNTDOWN TIME TO SLEEP ------------ */
	/** ------------ COUNTDOWN TIME TO SLEEP ------------ */
	/** ------------ COUNTDOWN TIME TO SLEEP ------------ */
	
	public void startCountDown(final long startTime) {
		timeHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (startTime == startCountdownTime) { // if not change countdown value
					countDownValue -= 1;
					if (countDownValue == 0) { // if time out
						// finish main activity
						Intent broadcastIntent = new Intent();
						broadcastIntent.setAction(ACTION_PLAYERSERVICE);
						broadcastIntent.putExtra(EXTRA_BROADCAST, BROADCAST_COUNTDOWN_TIME_OUT);						
						sendBroadcast(broadcastIntent);
						
						// stop play
						stopSelf();						
					} else {
						// send broadcast to change value of time
						Intent broadcastIntent = new Intent();
						broadcastIntent.setAction(ACTION_PLAYERSERVICE);
						broadcastIntent.putExtra(EXTRA_BROADCAST, BROADCAST_COUNTDOWN_TIME_LEFT);
						broadcastIntent.putExtra(EXTRA_COUNTDOWN_VALUE, countDownValue);
						sendBroadcast(broadcastIntent);
						startCountDown(startTime);
					}
				}
			}
		}, 60 * 1000);
	}
	
	
	
	
}
