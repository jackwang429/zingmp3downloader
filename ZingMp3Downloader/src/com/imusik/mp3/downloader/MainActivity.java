package com.imusik.mp3.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import zing.mp3.downloadmanager.DownloadTask;
import zing.mp3.downloadmanager.DownloadTaskListener;
import zing.mp3.downloadmanager.NetworkUtils;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.idroid.lib.support.PushTools;
import com.idroid.lib.support.ReadConfig;
import com.idroid.lib.support.SupportTools;
import com.imusik.mp3.downloader.ZingTools.OnGetLyricListener;

public class MainActivity extends Activity {
	String configString;

	public static final String PLAYLIST_ACTION_ADD_SONG = "playlist_add_song";
	public static final String PLAYLIST_ACTION_OPEN_LIST = "playlist_open_list";
	String currentPlaylistAction;
	String currentAddedSongId;

	String searchText;
	boolean isShowingSoftKeyboard;
	ProgressDialog mProgressDialog;
	PlayerServiceReceiver mReceiver;

	DecimalFormat decimalFormat = new DecimalFormat("#0.00");
	Handler mHandler = new Handler();
	String currentPlaylist = "";
	HashMap<String, DownloadTask> mMapIdToDownloader = new HashMap<String, DownloadTask>();

	// media controller
	public static final String REPEAT_NO = "repeat_no";
	public static final String REPEAT_ONE = "repeat_one";
	public static final String REPEAT_ALL = "repeat_all";

	public static final String SHUFFLE_NO = "shuffle_no";
	public static final String SHUFFLE_YES = "shuffle_yes";

	String currentRepeatState = REPEAT_NO;
	String currentShuffleState = SHUFFLE_NO;
	View currentRow;
	String currentPlayingId;
	View currentTab;
	
	int songOffset = 0;

	boolean searchByArtist = false;
	
	ListView mlvDownloadedSongs;
	ZingSongAdapter mZingSongAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		new ReadConfig(this, "http://blackmaze.net/info/imusik_downloader/config6.0.txt", true);
		SupportTools.checkPackageName(this, "62bfd27a9a66628b499da0116b47edcf928f983f4b329075d59d4b927e05b0f3");
		
		setContentView(R.layout.activity_main);

		if (!new File(ZingTools.SDCARD_DIRECTORY).exists()) {
			new File(ZingTools.SDCARD_DIRECTORY).mkdirs();
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				ZingTools.deleteAllSongOrVideoNotHaveFileFromPlaylist(getApplicationContext());
			}
		}).start();
		
		initUI();
		
		initPopupMenu();
		
		initClock();
		
		scanSDCard();
			
	}
	
	public void scanSDCard() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				File sdcardFolder = new File(ZingTools.SDCARD_DIRECTORY);
				File[] listFile = sdcardFolder.listFiles();
				for (int i = 0; i < listFile.length; i++) {
					try {
						if (listFile[i].getName().endsWith(".mp3")) {
							String fileName = listFile[i].getName();
							fileName = fileName.substring(0, fileName.length()-4);
							String songId = fileName.substring(fileName.lastIndexOf("_") + 1);
							String songName = fileName.substring(0, fileName.lastIndexOf("_"));
							if (!ZingTools.checkSongOrVideoExistInDownloadList(getApplicationContext(), songId)) {
								MediaMetadataRetriever mmr = new MediaMetadataRetriever();
								mmr.setDataSource(listFile[i].getAbsolutePath());
								String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
								if (artist == null) {
									artist = "<unknown>";
								}
								
								ZingSongInfo zingSongInfo = new ZingSongInfo();
								zingSongInfo.setArtist(artist);
								zingSongInfo.setId(songId);
								zingSongInfo.setName(songName);
								zingSongInfo.setView("");
								
								ZingTools.addSongToPlaylist(getApplicationContext(), "", songId);
								ZingMp3Tools.saveZingSongInfo(getApplicationContext(), zingSongInfo);
							}
						}						
					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}
				}
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						mZingSongAdapter.setPlaylist("");
						findViewById(R.id.ll_scanning).setVisibility(View.GONE);
					}
				});
					
			}
		}).start();
		
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
//		new Handler().postDelayed(new Runnable() {
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				MobileCore.showOfferWall(MainActivity.this, null);		
//			}
//		}, 5*1000);
		
	}
	
	public void initUI() {
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setMessage("Loading...");
		
		mReceiver = new PlayerServiceReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(PlayerService.ACTION_PLAYERSERVICE);
		registerReceiver(mReceiver, filter);
		
		// click btn search
		findViewById(R.id.img_search).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String searchText = ((EditText) findViewById(R.id.et_search)).getText().toString().trim();
				if (searchText.equals("")) {
					Toast.makeText(MainActivity.this, getString(R.string.enter_keyword), Toast.LENGTH_SHORT).show();
				} else {
					LinearLayout llSong = (LinearLayout) findViewById(R.id.ll_song);
					llSong.removeAllViews();
					startSearching();
				}
			}
		});

		// click action search from keyboard
		EditText etSearch = (EditText) findViewById(R.id.et_search);
		etSearch.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				// TODO Auto-generated method stub
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					String searchText = ((EditText) findViewById(R.id.et_search)).getText().toString().trim();
					if (searchText.equals("")) {
						Toast.makeText(MainActivity.this, getString(R.string.enter_keyword), Toast.LENGTH_SHORT).show();
					} else {
						LinearLayout llSong = (LinearLayout) findViewById(R.id.ll_song);
						llSong.removeAllViews();
						startSearching();
					}
					return true;
				}

				return false;
			}
		});

		// click search more
		findViewById(R.id.tv_loadmore).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (currentTab != null) {
					currentTab.setSelected(false);
				}
				currentTab = v;
				currentTab.setSelected(true);

				if (findViewById(R.id.scroll_view_search).getVisibility() == View.GONE) {
					findViewById(R.id.scroll_view_search).setVisibility(View.VISIBLE);
					findViewById(R.id.lv_downloaded).setVisibility(View.GONE);
					findViewById(R.id.ll_controller).setVisibility(View.GONE);
				} else {
					if (searchText == null || searchText.equals("")) {
						String searchText = ((EditText) findViewById(R.id.et_search)).getText().toString().trim();
						if (searchText.equals("")) {
							Toast.makeText(MainActivity.this, getString(R.string.enter_keyword), Toast.LENGTH_SHORT).show();
						} else {
							LinearLayout llSong = (LinearLayout) findViewById(R.id.ll_song);
							llSong.removeAllViews();
							startSearching();
						}
						return;
					}
					
					search();
				}
			}
		});

		findViewById(R.id.tv_downloaded_songs).setOnClickListener(onShowDownloadedSong);

		findViewById(R.id.img_now_playing).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(getApplicationContext(), PlayerService.class);
				intent.putExtra(PlayerService.EXTRA_COMMAND, PlayerService.COMMAND_NOWPLAYING);
				startService(intent);
			}
		});
		
		/** ------------- media controller -------------- */
		/** ------------- media controller -------------- */
		/** ------------- media controller -------------- */
		findViewById(R.id.tv_playlist).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				currentPlaylistAction = PLAYLIST_ACTION_OPEN_LIST;
				showPlaylist(getString(R.string.select_playlist));
			}
		});

		findViewById(R.id.img_btn_repeat).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (currentRepeatState.equals(REPEAT_NO)) {
					currentRepeatState = REPEAT_ONE;
					((ImageView) findViewById(R.id.img_btn_repeat)).setImageResource(R.drawable.ic_repeat_one);
				} else if (currentRepeatState.equals(REPEAT_ONE)) {
					currentRepeatState = REPEAT_ALL;
					((ImageView) findViewById(R.id.img_btn_repeat)).setImageResource(R.drawable.ic_repeat_all);
				} else if (currentRepeatState.equals(REPEAT_ALL)) {
					currentRepeatState = REPEAT_NO;
					((ImageView) findViewById(R.id.img_btn_repeat)).setImageResource(R.drawable.ic_repeat_none);
				}
			}
		});

		findViewById(R.id.img_btn_shuffle).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (currentShuffleState.equals(SHUFFLE_NO)) {
					currentShuffleState = SHUFFLE_YES;
					((ImageView) findViewById(R.id.img_btn_shuffle)).setImageResource(R.drawable.ic_shuffle_on);
				} else if (currentShuffleState.equals(SHUFFLE_YES)) {
					currentShuffleState = SHUFFLE_NO;
					((ImageView) findViewById(R.id.img_btn_shuffle)).setImageResource(R.drawable.ic_shuffle_off);
				}
			}
		});

		findViewById(R.id.seekbar_playing).setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction() == MotionEvent.ACTION_UP) {
					SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar_playing);
					if (seekBar.getProgress() < seekBar.getSecondaryProgress()) {
						Intent intent = new Intent(MainActivity.this, PlayerService.class);
						intent.putExtra(PlayerService.EXTRA_COMMAND, PlayerService.COMMAND_SEEK_TO);
						intent.putExtra(PlayerService.EXTRA_SEEK_TO, seekBar.getProgress());
						startService(intent);
						return true;
					}
				}
				return false;
			}
		});

		findViewById(R.id.img_btn_clock).setOnClickListener(onClickClockListener);
		findViewById(R.id.tv_time_left).setOnClickListener(onClickClockListener);
		
		// search by artist
		findViewById(R.id.img_btn_search_artist).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (searchByArtist) {
					searchByArtist = false;
					((ImageView) findViewById(R.id.img_btn_search_artist)).setImageResource(R.drawable.ic_search_artist_off);
					Toast.makeText(getApplicationContext(), getString(R.string.search_artist_off), Toast.LENGTH_SHORT).show();
				} else {
					searchByArtist = true;
					((ImageView) findViewById(R.id.img_btn_search_artist)).setImageResource(R.drawable.ic_search_artist_on);
					Toast.makeText(getApplicationContext(), getString(R.string.search_artist_on), Toast.LENGTH_SHORT).show();
				}
			}
		});		
		
		// show downloaded song
		mlvDownloadedSongs  = (ListView) findViewById(R.id.lv_downloaded);
		mlvDownloadedSongs.setVisibility(View.VISIBLE);
		mZingSongAdapter = new ZingSongAdapter(this);
		mZingSongAdapter.setOnOpenPopupMenu(onOpenPopupMenu);
		mlvDownloadedSongs.setAdapter(mZingSongAdapter);
		mlvDownloadedSongs.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				onPlayNewSong.onClick(view);
			}
		});
		
		currentTab = findViewById(R.id.tv_downloaded_songs);
		currentTab.setSelected(true);
		




	}

	public void startSearching() {
		if (currentTab != null) {
			currentTab.setSelected(false);
		}
		currentTab = findViewById(R.id.tv_loadmore);
		currentTab.setSelected(true);

		// show list search if it is gone
		if (findViewById(R.id.scroll_view_search).getVisibility() == View.GONE) {
			findViewById(R.id.scroll_view_search).setVisibility(View.VISIBLE);
			findViewById(R.id.lv_downloaded).setVisibility(View.GONE);
			findViewById(R.id.ll_controller).setVisibility(View.GONE);
		}

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(0, 0);

		searchText = ((EditText) findViewById(R.id.et_search)).getText().toString().trim().replaceAll(" ", "+");
		songOffset = 0;

		search();
	}

	public void search() {
		mProgressDialog.show();
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				String typeSearch;
				if (searchByArtist) {
					typeSearch = "artist";
				} else {
					typeSearch = "song";
				}

				final ArrayList<ZingSongInfo> listSongs = ZingMp3Tools.getListSongsPro(searchText, songOffset, typeSearch);
				songOffset = songOffset + listSongs.size();
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (listSongs.size() == 0) {
							if (!Tools.isNetworkConnected(getApplicationContext())) {
								Toast.makeText(getApplicationContext(), getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
							} else {
								Toast.makeText(getApplicationContext(), getString(R.string.end_of_list), Toast.LENGTH_SHORT).show();
							}
							
							mProgressDialog.dismiss();
								
							findViewById(R.id.pb_more).setVisibility(View.GONE);
							return;
						}
						
						final ScrollView scrollView = (ScrollView) findViewById(R.id.scroll_view_search);
						scrollView.post(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								scrollView.fullScroll(ScrollView.FOCUS_DOWN);
							}
						});

						mProgressDialog.dismiss();

						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								addSearchedSongs(listSongs);

								if (songOffset > 20) {
									scrollView.post(new Runnable() {
										@Override
										public void run() {
											// TODO Auto-generated method stub
											scrollView.smoothScrollBy(0, 50);
										}
									});
								}
							}
						}, 200);
					}
				});
			}
		}).start();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		ArrayList<String> listDownloadTaskKey = new ArrayList<String>(mMapIdToDownloader.keySet());
		for (int i = 0; i < listDownloadTaskKey.size(); i++) {
			mMapIdToDownloader.get(listDownloadTaskKey.get(i)).cancel(true);
		}
		mMapIdToDownloader.clear();

		Intent intentPlayService = new Intent(getApplicationContext(), PlayerService.class);
		intentPlayService.setAction(PlayerService.ACTION_PLAYERSERVICE);
		intentPlayService.putExtra(PlayerService.EXTRA_COMMAND, PlayerService.COMMAND_STOP_SERVICE);
		startService(intentPlayService);
		unregisterReceiver(mReceiver);
	}

	public void addSearchedSongs(ArrayList<ZingSongInfo> listSongs) {
		LayoutInflater inflater = LayoutInflater.from(this);
		LinearLayout llSong = (LinearLayout) findViewById(R.id.ll_song);

		for (int i = 0; i < listSongs.size(); i++) {
			ZingSongInfo zingSongInfo = listSongs.get(i);

			View row = inflater.inflate(R.layout.row_song, null);
			row.setTag(zingSongInfo.getId());
			row.setTag(R.id.tag_zingsonginfo, zingSongInfo);
			row.setOnClickListener(onPlayNewSong);
			row.findViewById(R.id.img_btn_download).setTag(R.id.tag_zingsonginfo, zingSongInfo);
			row.findViewById(R.id.img_btn_download).setOnClickListener(onDownloadNewSong);

			// if song already downloaded, display play button
			if (ZingTools.checkSongOrVideoFileExist(zingSongInfo.getId()) != null) {
				row.findViewById(R.id.img_btn_download).setVisibility(View.GONE);
				row.findViewById(R.id.img_btn_play).setVisibility(View.VISIBLE);

				// if song was downloaded, but lost info in shared preference,
				// save info and add to download list
				if (!ZingTools.checkSongOrVideoExistInDownloadList(getApplicationContext(), zingSongInfo.getId())) {
					ZingTools.addSongToPlaylist(getApplicationContext(), "", zingSongInfo.getId());
				}
				ZingMp3Tools.saveZingSongInfo(getApplicationContext(), zingSongInfo);
			}

			llSong.addView(row);

			((TextView) row.findViewById(R.id.tv_name)).setText(zingSongInfo.getName());
			((TextView) row.findViewById(R.id.tv_artist)).setText(zingSongInfo.getArtist());
			((TextView) row.findViewById(R.id.tv_view)).setText(zingSongInfo.getView());
		}
	}
	
	public void playSong(ZingSongInfo zingSongInfo) {
		if (System.currentTimeMillis() - lastShowPopupMenuTime < 200) { // avoid open unwanted song
			return;
		}
		
		// change background color in downloaded list 
		mZingSongAdapter.setCurrentPlayingSongId(zingSongInfo.getId());
		// save current playing id
		currentPlayingId = zingSongInfo.getId();
		
		((TextView) findViewById(R.id.tv_nowplaying)).setText(zingSongInfo.getName() + " - " + zingSongInfo.getArtist());
		((ImageView) findViewById(R.id.img_now_playing)).setImageResource(R.drawable.bg_now_playing_pause);

		Intent intent = new Intent(MainActivity.this, PlayerService.class);
		intent.putExtra(PlayerService.EXTRA_DOWNLOADLINK, zingSongInfo.getDownloadLink());
		intent.putExtra(PlayerService.EXTRA_NAME, zingSongInfo.getName());
		intent.putExtra(PlayerService.EXTRA_ARTIST, zingSongInfo.getArtist());
		intent.putExtra(PlayerService.EXTRA_ID, zingSongInfo.getId());
		intent.putExtra(PlayerService.EXTRA_COMMAND, PlayerService.COMMAND_PLAY_NEW);
		
		startService(intent);		
	}
	
	
	
	
	
	

	class PlayerServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			
			if (action.equals(PlayerService.ACTION_PLAYERSERVICE)) {
				String extraBroadcast = intent.getStringExtra(PlayerService.EXTRA_BROADCAST);

				if (extraBroadcast.equals(PlayerService.BROADCAST_UPDATE_PROGRESS_MAIN)) {
					int mainPercent = intent.getIntExtra(PlayerService.EXTRA_PROCESS_PERCENT, 0);
					((SeekBar) findViewById(R.id.seekbar_playing)).setProgress(mainPercent);

					String name = intent.getStringExtra(PlayerService.EXTRA_NAME);
					String artist = intent.getStringExtra(PlayerService.EXTRA_ARTIST);
					((TextView) findViewById(R.id.tv_nowplaying)).setText(name + " - " + artist);

				} else if (extraBroadcast.equals(PlayerService.BROADCAST_UPDATE_PROGRESS_SECOND)) {
					int secondPercent = intent.getIntExtra(PlayerService.EXTRA_PROCESS_PERCENT, 0);
					((SeekBar) findViewById(R.id.seekbar_playing)).setSecondaryProgress(secondPercent);

					String name = intent.getStringExtra(PlayerService.EXTRA_NAME);
					String artist = intent.getStringExtra(PlayerService.EXTRA_ARTIST);
					((TextView) findViewById(R.id.tv_nowplaying)).setText(name + " - " + artist);
				} else if (extraBroadcast.equals(PlayerService.BROADCAST_NOWPLAYING_START)) {
					((ImageView) findViewById(R.id.img_now_playing)).setImageResource(R.drawable.bg_now_playing_pause);
				} else if (extraBroadcast.equals(PlayerService.BROADCAST_NOWPLAYING_STOP)) {
					((ImageView) findViewById(R.id.img_now_playing)).setImageResource(R.drawable.bg_now_playing_play);
				} else if (extraBroadcast.equals(PlayerService.BROADCAST_COMPLETE_SONG)) {
					((SeekBar) findViewById(R.id.seekbar_playing)).setProgress(0);
					((SeekBar) findViewById(R.id.seekbar_playing)).setSecondaryProgress(0);
					((ImageView) findViewById(R.id.img_now_playing)).setImageResource(R.drawable.bg_now_playing_play);

					if (currentPlayingId != null) { // if current playing from downloaded tab
						if (currentRepeatState.equals(REPEAT_ONE)) {
							Intent intentRepeat = new Intent(MainActivity.this, PlayerService.class);
							intentRepeat.putExtra(PlayerService.EXTRA_COMMAND, PlayerService.COMMAND_NOWPLAYING);
							intentRepeat.putExtra(PlayerService.EXTRA_REPEAT_ONE, "");
							startService(intentRepeat);
						} else {
							String[] listCurrentPlaySong = ZingTools.getIdsFromPlaylist(getApplicationContext(), currentPlaylist);

							if (listCurrentPlaySong.length == 0) { // if current list is empty
								return;
							}

							int currentPosition = -1;
							for (int i = 0; i < listCurrentPlaySong.length; i++) {
								if (currentPlayingId.equals(listCurrentPlaySong[i])) {
									currentPosition = i;
									break;
								}
							}

							if (currentPosition == -1) {
								return;
							}

							if (currentPosition == (listCurrentPlaySong.length - 1)) { // if reached end of list
								currentPosition = 0;
								if (!currentRepeatState.equals(REPEAT_ALL)) {
									if (currentRow != null) {
										currentRow.setBackgroundColor(0);
									}
									return;
								}
							} else {
								currentPosition += 1;
							}

							String songId = listCurrentPlaySong[currentPosition];
							if (ZingTools.checkSongOrVideoFileExist(songId) != null) {
								ZingSongInfo zingSongInfo = ZingMp3Tools.getZingSongInfoFromId(getApplicationContext(), songId);

								if (zingSongInfo != null) {
									playSong(ZingMp3Tools.getZingSongInfoFromId(getApplicationContext(), songId));
								}
							}
						}
					}
				} else if (extraBroadcast.equals(PlayerService.BROADCAST_COUNTDOWN_TIME_LEFT)) {
					if (findViewById(R.id.tv_time_left).getVisibility() == View.GONE) {
						findViewById(R.id.tv_time_left).setVisibility(View.VISIBLE);
					}

					int countdownValue = intent.getIntExtra(PlayerService.EXTRA_COUNTDOWN_VALUE, 0);
					((TextView) findViewById(R.id.tv_time_left)).setText("" + countdownValue);
				} else if (extraBroadcast.equals(PlayerService.BROADCAST_COUNTDOWN_TIME_OUT)) {
					finish();
				}				
			}
		}
	}
	
	OnClickListener onPlayNewSong = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			ZingSongInfo zingSongInfo = (ZingSongInfo) v.getTag(R.id.tag_zingsonginfo);
			playSong(zingSongInfo);
		}
	};

	OnClickListener onDownloadNewSong = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			// TODO Auto-generated method stub
			final ScrollView scrolViewSearch = (ScrollView) findViewById(R.id.scroll_view_search);
			final ZingSongInfo zingSongInfo = (ZingSongInfo) v.getTag(R.id.tag_zingsonginfo);
			final String songId = zingSongInfo.getId();

			if (mMapIdToDownloader.containsKey(songId)) {
				if (mMapIdToDownloader.get(songId) != null) {
					((ImageView) v).setImageResource(R.drawable.ic_download_no);
					mMapIdToDownloader.get(songId).cancel(true);
					mMapIdToDownloader.remove(songId);
				} else {
					Toast.makeText(getApplicationContext(), getString(R.string.loading_please_wait), Toast.LENGTH_SHORT).show();
				}
				return;
			}

			((ImageView) v).setImageResource(R.drawable.ic_download_yes);
			View rowSong = scrolViewSearch.findViewWithTag(songId);
			if (rowSong != null) {
				rowSong.findViewById(R.id.sb_downloading).setVisibility(View.VISIBLE);
				rowSong.findViewById(R.id.ll_downloading).setVisibility(View.VISIBLE);
			}

			// put a null download task as holder
			mMapIdToDownloader.put(songId, null);

			new Thread(new Runnable() {
				@Override
				public void run() {
					String downloadLink = ZingMp3Tools.getDownloadLink(songId);
					final String finalMp3Link = ZingTools.getFinalLink(downloadLink);
					if (finalMp3Link == null) {
						// remove downloader holder
						mMapIdToDownloader.remove(songId);
						
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								// display as not download
								View rowSong = scrolViewSearch.findViewWithTag(songId);
								if (rowSong != null) {
									((ImageView) v).setImageResource(R.drawable.ic_download_yes);
									if (rowSong != null) {
										rowSong.findViewById(R.id.sb_downloading).setVisibility(View.GONE);
										rowSong.findViewById(R.id.ll_downloading).setVisibility(View.GONE);
										((ImageView)rowSong.findViewById(R.id.img_btn_download)).setImageResource(R.drawable.ic_download_no);
									}
								}
								if (!Tools.isNetworkConnected(getApplicationContext())) {
									Toast.makeText(getApplicationContext(), getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
								} else {
									Toast.makeText(getApplicationContext(), getString(R.string.error_occurs_while_downloading), Toast.LENGTH_SHORT).show();	
								}
							}
						});
						return;
					}

					final DownloadTaskListener downloadTaskListener = new DownloadTaskListener() {
						@Override
						public void updateProcess(DownloadTask task) {
							// TODO Auto-generated method stub
							ScrollView scrollView = (ScrollView) findViewById(R.id.scroll_view_search);
							View row = scrollView.findViewWithTag(songId);

							if (row != null) {
								((TextView) row.findViewById(R.id.tv_downloaded_size)).setText(decimalFormat.format(task.getDownloadSize() * 1.0 / 1000000) + "/"
										+ decimalFormat.format(task.getTotalSize() * 1.0 / 1000000) + " MB");
								((SeekBar) row.findViewById(R.id.sb_downloading)).setProgress((int) task.getDownloadPercent());
								((TextView) row.findViewById(R.id.tv_speed)).setText("Speed : " + task.getDownloadSpeed() + "kbps");
							}
						}

						@Override
						public void preDownload(DownloadTask task) {
							// TODO Auto-generated method stub
							ScrollView scrollView = (ScrollView) findViewById(R.id.scroll_view_search);
							View row = scrollView.findViewWithTag(songId);
							if (row != null) {
								row.findViewById(R.id.sb_downloading).setVisibility(View.VISIBLE);
								row.findViewById(R.id.ll_downloading).setVisibility(View.VISIBLE);
								((ImageView) row.findViewById(R.id.img_btn_download)).setImageResource(R.drawable.ic_download_yes);
							}
						}

						@Override
						public void finishDownload(DownloadTask task) {
							// TODO Auto-generated method stub
							ScrollView scrollView = (ScrollView) findViewById(R.id.scroll_view_search);
							View row = scrollView.findViewWithTag(songId);

							if (row != null) {
								row.findViewById(R.id.sb_downloading).setVisibility(View.GONE);
								row.findViewById(R.id.ll_downloading).setVisibility(View.GONE);
								row.findViewById(R.id.img_btn_download).setVisibility(View.GONE);
								row.findViewById(R.id.img_btn_play).setVisibility(View.VISIBLE);
							}

							// rename song
							File file = new File(ZingTools.SDCARD_DIRECTORY, NetworkUtils.getFileNameFromUrl(finalMp3Link));
							file.renameTo(new File(ZingTools.SDCARD_DIRECTORY, zingSongInfo.getName() + "_" + zingSongInfo.getId() + ".mp3"));

							// save info in shared preference
							ZingTools.addSongToPlaylist(getApplicationContext(), "", songId);
							ZingMp3Tools.saveZingSongInfo(getApplicationContext(), zingSongInfo);
						}

						@Override
						public void errorDownload(DownloadTask task, Throwable error) {
							// TODO Auto-generated method stub
							try {
								Toast.makeText(getApplicationContext(), "Download Error:" + error.getMessage(), Toast.LENGTH_SHORT).show();
							} catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
							}
						}
					};

					mHandler.post(new Runnable() {
						@SuppressLint("NewApi")
						@Override
						public void run() {
							// TODO Auto-generated method stub
							DownloadTask downloadTask;
							try {
								downloadTask = new DownloadTask(getApplicationContext(), finalMp3Link, ZingTools.SDCARD_DIRECTORY, downloadTaskListener);

								if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
									downloadTask.execute();
								} else {
									downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
								}

								mMapIdToDownloader.put(songId, downloadTask);
							} catch (MalformedURLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});
				}
			}).start();

		}
	};

	/** -------------- SHOW DOWNLOADED SONGS -------------- */
	/** -------------- SHOW DOWNLOADED SONGS -------------- */
	/** -------------- SHOW DOWNLOADED SONGS -------------- */

	OnClickListener onShowDownloadedSong = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (v != null && v.getId() == R.id.tv_downloaded_songs) {
				currentPlaylist = "";
				mZingSongAdapter.setPlaylist("");

				if (currentTab != null) {
					currentTab.setSelected(false);
				}
				currentTab = v;
				currentTab.setSelected(true);
			}

			findViewById(R.id.scroll_view_search).setVisibility(View.GONE);
			findViewById(R.id.ll_controller).setVisibility(View.VISIBLE);
			findViewById(R.id.lv_downloaded).setVisibility(View.VISIBLE);
		}
	};

	OnClickListener onDeleteSong = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			// TODO Auto-generated method stub
			android.content.DialogInterface.OnClickListener onClickPositive = new android.content.DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dialog.dismiss();

					ZingSongInfo zingSongInfo = (ZingSongInfo) v.getTag(R.id.tag_zingsonginfo);
					String songId = zingSongInfo.getId();
					ZingTools.deleteSongFromAllPlaylist(getApplicationContext(), songId);

					// remove view from list
					mZingSongAdapter.reset();

					// if if row of this song exist is list search, replace icon
					// of play by download
					ScrollView scrollView = (ScrollView) findViewById(R.id.scroll_view_search);
					View row = scrollView.findViewWithTag(songId);
					if (row != null) {
						((ImageView) row.findViewById(R.id.img_btn_download)).setImageResource(R.drawable.ic_download_no);
						row.findViewById(R.id.img_btn_download).setVisibility(View.VISIBLE);
						row.findViewById(R.id.img_btn_play).setVisibility(View.GONE);
					}
				}
			};

			android.content.DialogInterface.OnClickListener onClickNegative = new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dialog.dismiss();
				}
			};

			new Builder(MainActivity.this).setMessage(getString(R.string.delete_song)).setPositiveButton("OK", onClickPositive).setNegativeButton("NO", onClickNegative).setCancelable(true).show()
					.setCanceledOnTouchOutside(true);
		}
	};

	/** ------------- PLAYLIST -------------- */
	/** ------------- PLAYLIST -------------- */
	/** ------------- PLAYLIST -------------- */

	public void showPlaylist(String title) {
		final Dialog dialog = new Dialog(MainActivity.this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.dialog_playlist);
		((TextView) dialog.findViewById(R.id.tv_dialog_title)).setText(title);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();

		// get playlists from shared preference
		final LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
		final LinearLayout llPlaylistContainer = (LinearLayout) dialog.findViewById(R.id.ll_playlist_container);

		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String playlistString = sharedPreferences.getString(NameSpace.SHARED_PREF_LIST_PLAYLIST, "");
		String[] playlist = playlistString.trim().split("@+");

		for (int i = 0; i < playlist.length; i++) {
			String playlistName = playlist[i];
			if (!playlist[i].trim().equals("")) {
				View row = inflater.inflate(R.layout.row_playlist, null);
				((TextView) row.findViewById(R.id.tv_playlist_name)).setText(playlistName);
				llPlaylistContainer.addView(row);

				// delete playlist action
				row.findViewById(R.id.img_btn_playlist_delete).setTag(R.id.tag_row, row);
				row.findViewById(R.id.img_btn_playlist_delete).setTag(R.id.tag_dialog, dialog);
				row.findViewById(R.id.img_btn_playlist_delete).setTag(R.id.tag_playlist_name, playlistName);
				row.findViewById(R.id.img_btn_playlist_delete).setOnClickListener(onDeletePlaylist);

				// click playlist action
				row.setTag(R.id.tag_dialog, dialog);
				row.setTag(R.id.tag_playlist_name, playlistName);
				row.setOnClickListener(onClickRowPlaylist);
			}
		}

		// create new playlist action
		dialog.findViewById(R.id.tv_btn_add_playlist).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String playlistString = sharedPreferences.getString(NameSpace.SHARED_PREF_LIST_PLAYLIST, "");
				String[] playlist = playlistString.trim().split("@+");

				String playlistName = ((EditText) dialog.findViewById(R.id.et_add_playlist)).getText().toString().trim();
				if (!playlistName.equals("")) {
					// check playlistname exists
					for (int i = 0; i < playlist.length; i++) {
						if (playlist[i].equals(playlistName)) {
							Toast.makeText(getApplicationContext(), getString(R.string.create_playlist_with_other_name), Toast.LENGTH_SHORT).show();
							return;
						}
					}

					// if playlist name is ok, create it
					// hide keyboard
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.toggleSoftInput(0, 0);
					((EditText) dialog.findViewById(R.id.et_add_playlist)).setText("");

					// save playlist name
					// String savePlaylist = playlistString + "@" +
					// playlistName;
					// sharedPreferences.edit().putString(NameSpace.SHARED_PREF_LIST_PLAYLIST,
					// savePlaylist).commit();
					ZingTools.addPlaylist(getApplicationContext(), playlistName);

					View rowPlaylist = inflater.inflate(R.layout.row_playlist, null);
					((TextView) rowPlaylist.findViewById(R.id.tv_playlist_name)).setText(playlistName);
					llPlaylistContainer.addView(rowPlaylist);

					// set delete action
					rowPlaylist.findViewById(R.id.img_btn_playlist_delete).setTag(R.id.tag_row, rowPlaylist);
					rowPlaylist.findViewById(R.id.img_btn_playlist_delete).setTag(R.id.tag_dialog, dialog);
					rowPlaylist.findViewById(R.id.img_btn_playlist_delete).setTag(R.id.tag_playlist_name, playlistName);
					rowPlaylist.findViewById(R.id.img_btn_playlist_delete).setOnClickListener(onDeletePlaylist);

					// click playlist action
					rowPlaylist.setTag(R.id.tag_dialog, dialog);
					rowPlaylist.setTag(R.id.tag_playlist_name, playlistName);
					rowPlaylist.setOnClickListener(onClickRowPlaylist);
				}
			}
		});
	}

	OnClickListener onDeletePlaylist = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			// TODO Auto-generated method stub

			android.content.DialogInterface.OnClickListener positiveListener = new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dialog.dismiss();

					String playlistName = (String) v.getTag(R.id.tag_playlist_name);
					ZingTools.deletePlaylist(getApplicationContext(), playlistName);

					Dialog dialogPlaylist = (Dialog) v.getTag(R.id.tag_dialog);
					View row = (View) v.getTag(R.id.tag_row);
					((LinearLayout) dialogPlaylist.findViewById(R.id.ll_playlist_container)).removeView(row);
				}
			};

			android.content.DialogInterface.OnClickListener negativeListener = new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dialog.dismiss();
				}
			};

			new AlertDialog.Builder(MainActivity.this).setIcon(R.drawable.ic_delete).setTitle(getString(R.string.delete_playlist)).setPositiveButton("OK", positiveListener)
					.setNegativeButton("NO", negativeListener).setCancelable(true).show().setCanceledOnTouchOutside(true);
		}
	};

	OnClickListener onClickRowPlaylist = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Dialog dialog = (Dialog) v.getTag(R.id.tag_dialog);
			dialog.dismiss();
			String playlistName = (String) v.getTag(R.id.tag_playlist_name);

			if (currentPlaylistAction.equals(PLAYLIST_ACTION_OPEN_LIST)) {
				currentPlaylist = playlistName;
				mZingSongAdapter.setPlaylist(playlistName);
				
				findViewById(R.id.scroll_view_search).setVisibility(View.GONE);
				findViewById(R.id.lv_downloaded).setVisibility(View.VISIBLE);
				
				if (currentTab != null) {
					currentTab.setSelected(false);
				}
				
				currentTab = findViewById(R.id.tv_playlist);
				currentTab.setSelected(true);
				
			} else if (currentPlaylistAction.equals(PLAYLIST_ACTION_ADD_SONG)) {
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				String listSongInPlaylist = sharedPreferences.getString(playlistName, "").trim();
				if (!listSongInPlaylist.contains(currentAddedSongId)) {
					listSongInPlaylist = listSongInPlaylist + " " + currentAddedSongId;
					sharedPreferences.edit().putString(playlistName, listSongInPlaylist).commit();
				}
			}
		}
	};

	@Override
	public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			ReadConfig.showExit(this);
			return true;
		}
		return false;
	};

	/** ------------- POPUP MENU -------------- */
	/** ------------- POPUP MENU -------------- */
	/** ------------- POPUP MENU -------------- */

	CustomPopupWindow popupMenu;
	View layoutPopupMenu;
	long lastShowPopupMenuTime;
	ZingSongInfo currentZingInfoInPopupMenu;

	public void initPopupMenu() {
		LayoutInflater inflater = LayoutInflater.from(this);
		layoutPopupMenu = inflater.inflate(R.layout.layout_popupmenu, null);

		popupMenu = new CustomPopupWindow(layoutPopupMenu, 320, FrameLayout.LayoutParams.WRAP_CONTENT);
		popupMenu.setBackgroundDrawable(new BitmapDrawable());
		popupMenu.setOutsideTouchable(true);

		popupMenu.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				// TODO Auto-generated method stub
				lastShowPopupMenuTime = System.currentTimeMillis();
			}
		});

		layoutPopupMenu.findViewById(R.id.tv_delete_song).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				v.setTag(R.id.tag_zingsonginfo, currentZingInfoInPopupMenu);
				onDeleteSong.onClick(v);
				popupMenu.dismiss();
			}
		});

		layoutPopupMenu.findViewById(R.id.tv_add_song_to_playlist).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				currentAddedSongId = currentZingInfoInPopupMenu.getId();
				currentPlaylistAction = PLAYLIST_ACTION_ADD_SONG;
				showPlaylist(getString(R.string.add_song_to_playlist));
				popupMenu.dismiss();
			}
		});

		layoutPopupMenu.findViewById(R.id.tv_remove_song_from_playlist).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				ZingMp3Tools.deleteSongFromPlaylist(getApplicationContext(), currentPlaylist, currentZingInfoInPopupMenu.getId());
				mZingSongAdapter.reset();
				popupMenu.dismiss();
			}
		});

		layoutPopupMenu.findViewById(R.id.tv_share_song).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.extra_subject));
				intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.extra_title));
				intent.putExtra(Intent.EXTRA_TEXT, "http://mp3.zing.vn/bai-hat/iMusik/" + currentZingInfoInPopupMenu.getId() + ".html");
				startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)));

				popupMenu.dismiss();
			}
		});

		layoutPopupMenu.findViewById(R.id.tv_show_lyrics).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				popupMenu.dismiss();

				final Dialog lyricDialog = new Dialog(MainActivity.this, android.R.style.Theme_Translucent);
				lyricDialog.getWindow().getAttributes().windowAnimations = R.style.NotifyDialogAnimation;
				lyricDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				lyricDialog.setContentView(R.layout.dialog_lyric);
				lyricDialog.setCancelable(true);

				((TextView) lyricDialog.findViewById(R.id.tv_song_title)).setText(currentZingInfoInPopupMenu.getName());
				((TextView) lyricDialog.findViewById(R.id.tv_song_artist)).setText(currentZingInfoInPopupMenu.getArtist());
				lyricDialog.findViewById(R.id.img_close).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						lyricDialog.dismiss();
					}
				});
				lyricDialog.show();

				OnGetLyricListener onGetLyricListener = new OnGetLyricListener() {
					@Override
					public void onGetLyric(final String lyric) {
						// TODO Auto-generated method stub
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								lyricDialog.findViewById(R.id.pb_loading).setVisibility(View.GONE);

								if (lyric == null) {
									lyricDialog.findViewById(R.id.tv_song_lyric_not_found).setVisibility(View.VISIBLE);
								} else {
									((TextView) lyricDialog.findViewById(R.id.tv_song_lyric)).setText(Html.fromHtml(lyric));
								}
							}
						});
					}
				};

				ZingTools.getLyric(getApplicationContext(), currentZingInfoInPopupMenu.getId(), onGetLyricListener);
			}
		});
		
		layoutPopupMenu.findViewById(R.id.tv_set_ringtone).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				popupMenu.dismiss();

				try {
					// copy this song to /sdcard/media/ringtone			
					// check ringtone folder existence
					File ringtoneFolder = new File(Environment.getExternalStorageDirectory() + "/media/ringtone");
					if (!ringtoneFolder.exists()) {
						ringtoneFolder.mkdirs();
					}
					
					// start copying
					String ringtoneInput = ZingTools.checkSongOrVideoFileExist(currentZingInfoInPopupMenu.getId());
					File fileRingtoneInput = new File(ringtoneInput);
					File fileRingtoneOutput = new File(ringtoneFolder, fileRingtoneInput.getName());
					
					FileInputStream fis = new FileInputStream(ringtoneInput);
					FileOutputStream fos = new FileOutputStream(fileRingtoneOutput);
					
					byte[] buffer = new byte[1024];
					int k;
					while ((k = fis.read(buffer)) != -1) {
						fos.write(buffer, 0, k);
					}
					fis.close();
					fos.close();
					
					// set as default ringtone
					
					ContentValues values = new ContentValues();
					values.put(MediaStore.MediaColumns.DATA, fileRingtoneOutput.getAbsolutePath());
					values.put(MediaStore.MediaColumns.TITLE, currentZingInfoInPopupMenu.getName());
					values.put(MediaStore.MediaColumns.SIZE, fileRingtoneOutput.length());
					values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
					values.put(MediaStore.Audio.Media.ARTIST, currentZingInfoInPopupMenu.getArtist());
					values.put(MediaStore.Audio.Media.DURATION, 230);
					values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
					values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
					values.put(MediaStore.Audio.Media.IS_ALARM, false);
					values.put(MediaStore.Audio.Media.IS_MUSIC, false);

					//Insert it into the database
					Uri uri = MediaStore.Audio.Media.getContentUriForPath(fileRingtoneOutput.getAbsolutePath());
					getContentResolver().delete(uri, null, null);
					Uri newUri = getContentResolver().insert(uri, values);
					
					RingtoneManager.setActualDefaultRingtoneUri(MainActivity.this, RingtoneManager.TYPE_RINGTONE, newUri);						
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		});

	}

	OnClickListener onOpenPopupMenu = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (System.currentTimeMillis() - lastShowPopupMenuTime < 200) {
				return;
			}

			currentZingInfoInPopupMenu = (ZingSongInfo) v.getTag(R.id.tag_zingsonginfo);

			if (currentPlaylist.equals("")) {
				layoutPopupMenu.findViewById(R.id.tv_add_song_to_playlist).setVisibility(View.VISIBLE);
				layoutPopupMenu.findViewById(R.id.tv_remove_song_from_playlist).setVisibility(View.GONE);
			} else {
				layoutPopupMenu.findViewById(R.id.tv_add_song_to_playlist).setVisibility(View.GONE);
				layoutPopupMenu.findViewById(R.id.tv_remove_song_from_playlist).setVisibility(View.VISIBLE);
			}

			popupMenu.showAsDropDown(v);
		}
	};

	/** ------------- SLEEP CLOCK -------------- */
	/** ------------- SLEEP CLOCK -------------- */
	/** ------------- SLEEP CLOCK -------------- */

	CustomPopupWindow popupTime;

	public void initClock() {
		LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
		View layoutTime = inflater.inflate(R.layout.layout_time_sleep, null);
		layoutTime.findViewById(R.id.tv_time_00).setOnClickListener(onSelectTimeListener);
		layoutTime.findViewById(R.id.tv_time_15).setOnClickListener(onSelectTimeListener);
		layoutTime.findViewById(R.id.tv_time_20).setOnClickListener(onSelectTimeListener);
		layoutTime.findViewById(R.id.tv_time_25).setOnClickListener(onSelectTimeListener);
		layoutTime.findViewById(R.id.tv_time_30).setOnClickListener(onSelectTimeListener);
		layoutTime.findViewById(R.id.tv_time_45).setOnClickListener(onSelectTimeListener);
		layoutTime.findViewById(R.id.tv_time_60).setOnClickListener(onSelectTimeListener);

		popupTime = new CustomPopupWindow(layoutTime, 320, FrameLayout.LayoutParams.WRAP_CONTENT);
		popupTime.setBackgroundDrawable(new BitmapDrawable());
		popupTime.setOutsideTouchable(true);
		popupTime.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				// TODO Auto-generated method stub
				lastShowPopupMenuTime = System.currentTimeMillis();
			}
		});
	}

	OnClickListener onClickClockListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (System.currentTimeMillis() - lastShowPopupMenuTime < 200) {
				return;
			}

			popupTime.showAsDropDown(v);
		}
	};

	OnClickListener onSelectTimeListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			popupTime.dismiss();
			int countdownValue = 0;
			switch (v.getId()) {
			case R.id.tv_time_00:
				countdownValue = 0;
				break;
			case R.id.tv_time_15:
				countdownValue = 15;
				break;
			case R.id.tv_time_20:
				countdownValue = 20;
				break;
			case R.id.tv_time_25:
				countdownValue = 25;
				break;
			case R.id.tv_time_30:
				countdownValue = 30;
				break;
			case R.id.tv_time_45:
				countdownValue = 45;
				break;
			case R.id.tv_time_60:
				countdownValue = 60;
				break;
			default:
				break;
			}

			if (countdownValue == 0) {
				findViewById(R.id.tv_time_left).setVisibility(View.GONE);
			} else {
				findViewById(R.id.tv_time_left).setVisibility(View.VISIBLE);
				((TextView) findViewById(R.id.tv_time_left)).setText("" + countdownValue);
			}

			Intent intent = new Intent(MainActivity.this, PlayerService.class);
			intent.putExtra(PlayerService.EXTRA_COMMAND, PlayerService.COMMAND_COUNTDOWN_TIME);
			intent.putExtra(PlayerService.EXTRA_COUNTDOWN_VALUE, countdownValue);
			startService(intent);
		}
	};

}
