package com.imusik.mp3.downloader;

import java.io.File;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.http.AndroidHttpClient;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

public class ZingTools {
	public static final String SDCARD_FOLDER = "/Mp3Downloader/";
	public static final String FILE_POSTFIX = ".mp3";
	public static final String FILE_POSTFIX_DOWNLOAD = ".download";
	public static final String SDCARD_DIRECTORY = Environment.getExternalStorageDirectory().toString() + SDCARD_FOLDER;
	
	public static void main(String args[]) {
		
	}
	
	/** ---------------- NETWORK TOOLS ---------------- */
	/** ---------------- NETWORK TOOLS ---------------- */
	/** ---------------- NETWORK TOOLS ---------------- */
	
	public static void getLyric(final Context context, final String songId, final OnGetLyricListener onGetLyricListener) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String lyric = sharedPreferences.getString("lyric_" + songId, null);
		
		if (lyric != null) {
			onGetLyricListener.onGetLyric(lyric);
		} else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					String lyric = getLyric(songId);
					if (lyric != null) {
						SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
						sharedPreferences.edit().putString("lyric_" + songId, lyric).commit();
					}
					onGetLyricListener.onGetLyric(getLyric(songId));
				}
			}).start();
		}
	}

	public interface OnGetLyricListener {
		public void onGetLyric(String lyric);
	}
	
	public static String getLyric(String songId) {
		try {
			String randomUUID = UUID.randomUUID().toString();
			String apiUrl = "http://m.mp3.zing.vn/bai-hat/" + randomUUID + "/" + songId + ".html";
//			String source = ServiceHelper.getData(apiUrl);
//			Document document = Jsoup.parse(source);
			Document document = Jsoup.connect(apiUrl).get();
			Element lyric = document.getElementById("conLyrics");
			return lyric.html();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public static String getFinalLink(String link) {
		AndroidHttpClient client = AndroidHttpClient.newInstance("DownloadTask");
		try {
	        HttpGet httpGet = new HttpGet(link);
	        HttpResponse response = client.execute(httpGet);
	        
	        String finalLink = link;
	        if (response.getStatusLine().getStatusCode() == 302) {
	        	finalLink = response.getHeaders("Location")[0].getValue();
	        	if (finalLink.contains("?")) {
	        		finalLink = finalLink.substring(0, finalLink.indexOf('?'));
	        	}
	        }
	        
	        client.close();
	        
	        if (finalLink.equals(link)) {
	        	return finalLink;
	        } else {
	        	return getFinalLink(finalLink);	
	        }
		} catch (Exception e) {
			// TODO: handle exception
			client.close();
			e.printStackTrace();
		}
		
		return null;
	}	
	

	
	
	
	/** ---------------- SHARED PREFERENCES SONG/VIDEO+PLAYLIST---------------- */
	/** ---------------- SHARED PREFERENCES SONG/VIDEO+PLAYLIST---------------- */
	/** ---------------- SHARED PREFERENCES SONG/VIDEO+PLAYLIST---------------- */
	
	public static void addSongToPlaylist(Context context, String playlistName, String songId) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (playlistName.equals("")) {
			String listSongs = sharedPreferences.getString(NameSpace.SHARED_PREF_LIST_DOWNLOAD, "");
			
			if (listSongs.contains(songId)) { // if song already in download playlist
				return;
			}
			
			listSongs = listSongs + " " + songId;
			listSongs = listSongs.replaceAll("\\s+", " ").trim();
			sharedPreferences.edit().putString(NameSpace.SHARED_PREF_LIST_DOWNLOAD, listSongs).commit();
		} else {
			String listSongs = sharedPreferences.getString(playlistName, "");
			
			if (listSongs.contains(songId)) { // if song already in playlist
				return;
			}
			
			listSongs = listSongs + " " + songId;
			listSongs = listSongs.replaceAll("\\s+", " ").trim();
			sharedPreferences.edit().putString(playlistName, listSongs).commit();			
		}
	}
	
	public static void deleteSongFromAllPlaylist(Context context, String songId) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String listSongs;
		
		// delete mp3 file
		String fileDirectory = ZingTools.checkSongOrVideoFileExist(songId);
		if (fileDirectory != null) {
			new File(fileDirectory).delete();
		}
		
		// delete from list download
		listSongs = sharedPreferences.getString(NameSpace.SHARED_PREF_LIST_DOWNLOAD, "");
		listSongs = listSongs.replace(songId, "");
		listSongs = listSongs.replaceAll("\\s+", " ").trim();
		sharedPreferences.edit().putString(NameSpace.SHARED_PREF_LIST_DOWNLOAD, listSongs).commit();
		
		
		// delete from all other playlist
		String[] listPlaylists = getListPlaylists(context);
		for (int i = 0; i < listPlaylists.length; i++) {
			String playlist = listPlaylists[i];
			
			if (!playlist.equals("")) {
				String listSongOfPlaylist = sharedPreferences.getString(playlist, "");
				listSongOfPlaylist = listSongOfPlaylist.replace(songId, "");
				listSongOfPlaylist = listSongOfPlaylist.replaceAll("\\s+", " ").trim();
				sharedPreferences.edit().putString(playlist, listSongOfPlaylist).commit();
			}
		}
	}
	
	public static void deleteSongFromPlaylist(Context context, String playlistName, String songId) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String listSongs = sharedPreferences.getString(playlistName, "");
		listSongs = listSongs.replace(songId, "").replaceAll("\\s+", " ").trim();
		sharedPreferences.edit().putString(playlistName, listSongs).commit();
	}
	
	public static boolean checkSongOrVideoExistInPlaylist(Context context, String playlistName, String songId) {
		if (playlistName.equals("")) 
			return true;
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString(playlistName, "").contains(songId);
	}	
	
	/** ---------------- SHARED PREFERENCES PLAYLIST---------------- */
	/** ---------------- SHARED PREFERENCES PLAYLIST---------------- */
	/** ---------------- SHARED PREFERENCES PLAYLIST---------------- */
	
	public static void addPlaylist(Context context, String playlistName) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String listPlaylists = sharedPreferences.getString(NameSpace.SHARED_PREF_LIST_PLAYLIST, "");
		listPlaylists = listPlaylists + playlistName + "@";
		listPlaylists = listPlaylists.replaceAll("@+", "@");
		sharedPreferences.edit().putString(NameSpace.SHARED_PREF_LIST_PLAYLIST, listPlaylists).commit();
	}
	
	public static void deletePlaylist(Context context, String playlistName) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String listPlaylists = sharedPreferences.getString(NameSpace.SHARED_PREF_LIST_PLAYLIST, "");
		listPlaylists = listPlaylists.replace(playlistName, "");
		listPlaylists = listPlaylists.replaceAll("@+", "@");
		sharedPreferences.edit().putString(NameSpace.SHARED_PREF_LIST_PLAYLIST, listPlaylists).commit();
	}
	
	public static String[] getListPlaylists(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String[] listPlaylists = sharedPreferences.getString(NameSpace.SHARED_PREF_LIST_PLAYLIST, "").split("@+");
		return listPlaylists;
	}
	
	
	
	/** ---------------- CHECK SONG/VIDEO EXISTENCE IN PLAYLIST AND DOWNLOAD FOLDER---------------- */
	/** ---------------- CHECK SONG/VIDEO EXISTENCE IN PLAYLIST AND DOWNLOAD FOLDER---------------- */
	/** ---------------- CHECK SONG/VIDEO EXISTENCE IN PLAYLIST AND DOWNLOAD FOLDER---------------- */
	
	public static String[] getIdsFromPlaylist(Context context, String playlistName) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		String listIdsContainer;
		if (playlistName.equals("")) {
			listIdsContainer = sharedPreferences.getString(NameSpace.SHARED_PREF_LIST_DOWNLOAD, "");
		} else {
			listIdsContainer = sharedPreferences.getString(playlistName, "");
		}
		
		if (listIdsContainer.trim().equals("")) {
			return new String[]{};
		} else {
			return listIdsContainer.trim().split("\\s+");
		}
	}
	
	public static void deleteAllSongOrVideoNotHaveFileFromPlaylist(Context context) {
		String[] listIds = getIdsFromPlaylist(context, "");
		for (int i = 0; i < listIds.length; i++) {
			if (checkSongOrVideoFileExist(listIds[i]) == null) {
				deleteSongFromAllPlaylist(context, listIds[i]);
			}
		}
	}
	
	public static void deleteAllFileNotInDownloadList(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String listDownloadIds = sharedPreferences.getString(NameSpace.SHARED_PREF_LIST_DOWNLOAD, "");
		
		File file = new File(SDCARD_DIRECTORY);
		if (!file.exists()) {
			return;
		}
		
		String[] listFiles = file.list();
		for (int i = 0; i < listFiles.length; i++) {
			try {
				if (!listFiles[i].contains(FILE_POSTFIX_DOWNLOAD)) {
					String id = listFiles[i].substring(listFiles[i].indexOf('_') + 1, listFiles[i].indexOf(FILE_POSTFIX));
					if (!listDownloadIds.contains(id)) {
						new File(SDCARD_DIRECTORY, listFiles[i]).delete();
					}					
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
	}
	

	/** ---------------- OTHER TOOLS ---------------- */
	/** ---------------- OTHER TOOLS ---------------- */
	/** ---------------- OTHER TOOLS ---------------- */
	
	public static String checkSongOrVideoFileExist(String songOrVideoId) {
		File file = new File(SDCARD_DIRECTORY);
		String[] fileList = file.list();
		if (fileList != null) {
			for (int i = 0; i < fileList.length; i++) {
				if (fileList[i].contains(songOrVideoId))
					return SDCARD_DIRECTORY + fileList[i];
			}
		}
		return null;
	}
	
	public static boolean checkSongOrVideoExistInDownloadList(Context context, String songOrVideoId) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String listDownloadIds = sharedPreferences.getString(NameSpace.SHARED_PREF_LIST_DOWNLOAD, "");
		return listDownloadIds.contains(songOrVideoId);
	}
	
	public static boolean checkAppInstalled(Context context, String uri) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed = false;
        try {
               pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
               app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
               app_installed = false;
        }
        return app_installed ;
	}	
	
}
