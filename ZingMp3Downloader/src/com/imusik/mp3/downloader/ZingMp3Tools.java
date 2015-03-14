package com.imusik.mp3.downloader;

import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ZingMp3Tools {
	/** ---------------- NETWORK TOOLS ---------------- */
	/** ---------------- NETWORK TOOLS ---------------- */
	/** ---------------- NETWORK TOOLS ---------------- */
	
	public static String getDownloadLink(String songId) {
		try {
			String randomUUID = UUID.randomUUID().toString();
			String apiUrl = "http://mp3.zing.vn/bai-hat/" + randomUUID + "/" + songId + ".html";
			Document document = Jsoup.connect(apiUrl).get();
			Element xmlLinkContainer = document.getElementsByClass("player").first();
			String xmlLinkHtml = xmlLinkContainer.html();
			String xmlLink = xmlLinkHtml.substring(xmlLinkHtml.indexOf("xmlURL=") + 7, xmlLinkHtml.indexOf("&amp;textad="));
			return getDownloadlinkFromXML(xmlLink);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
	}
	
	public static String getDownloadlinkFromXML(String xmlLink) {
		try {
			String source = Jsoup.connect(xmlLink).get().html();
			String downloadLink = source.substring(source.indexOf("http://mp3.zing.vn/xml/load-song/"));
			downloadLink = downloadLink.substring(0, downloadLink.indexOf("hq")-1).replace("\n", "").replace(" ", "");
			return downloadLink;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
	}	
	
//	public static ArrayList<ZingSongInfo> getListSongs(String url) {
//		ArrayList<ZingSongInfo> zingListSongs = new ArrayList<ZingSongInfo>();
//		try {
//			Document document = Jsoup.connect(url).get();
//			
//			// get first song
//			try {
//				Element firstSongDiv = document.getElementsByClass("first-search-song").first();
//				
//				
//				Element idAndNameContainer = firstSongDiv.getElementsByTag("a").first();
//				
//				String idContainer = idAndNameContainer.attr("href");
//				String id = idContainer.substring(idContainer.lastIndexOf('/')+1, idContainer.indexOf(".html"));
//				String name = StringEscapeUtils.unescapeHtml4(firstSongDiv.getElementsByTag("a").first().html().trim());
//				String artist = StringEscapeUtils.unescapeHtml4(firstSongDiv.getElementsByTag("p").first().getElementsByTag("a").first().html().trim());
//				String viewLong = firstSongDiv.getElementsByTag("p").get(1).html();
//				String view = viewLong.substring(viewLong.lastIndexOf('|') +1).trim().replace("Lượt nghe", "Views");
//				String downloadLinkLong = firstSongDiv.getElementsByTag("script").get(0).html();
//				String downloadLink = null;
//				
//				if (downloadLinkLong.contains("music-buy")) {
//					downloadLink = getDownloadLink(id);
//				} else {
//					downloadLink = downloadLinkLong.substring(downloadLinkLong.indexOf("http://mp3.zing.vn/download/song/"), downloadLinkLong.indexOf(" class=\"music-download _btnDownload\"")-1);
//				}
//				
//				if (downloadLink != null) {
//					ZingSongInfo zingSongInfo = new ZingSongInfo();
//					zingSongInfo.setId(id);
//					zingSongInfo.setName(name);
//					zingSongInfo.setArtist(artist);
//					zingSongInfo.setView(view);
//					zingSongInfo.setDownloadLink(downloadLink);
//					zingListSongs.add(zingSongInfo);					
//				}
//			} catch (Exception e) {
//				// TODO: handle exception
//				e.printStackTrace();
//			}		
//			
//			Element listContainer = document.getElementsByClass("content-block").get(1);
//			Elements listSongs = listContainer.getElementsByClass("content-item");
//			for (Element song : listSongs) {
//				try {
//					Elements aTags = song.getElementsByTag("a");
//					String idLong = aTags.get(0).attr("href");
//					String id = idLong.substring(idLong.lastIndexOf('/')+1, idLong.indexOf(".html"));
//					
//					String name = StringEscapeUtils.unescapeHtml4(aTags.get(0).html().trim());
//					String artist = StringEscapeUtils.unescapeHtml4(aTags.get(1).html().trim()).replace("Nhiều ca sĩ", "Various Artist");
//					
//					String bitLong = song.getElementsByTag("p").get(0).html();
//					String bit = bitLong.subSequence(bitLong.lastIndexOf('|')+1, bitLong.length()).toString().trim();
//					
//					String viewLong = song.getElementsByTag("p").get(1).html();
//					String view = viewLong.subSequence(viewLong.lastIndexOf('|') + 1, viewLong.length()).toString().trim().replace("Lượt nghe", "Views");
//					
//					String downloadLinkLong = song.getElementsByTag("script").get(0).html();
//					String downloadlink = downloadLinkLong.substring(downloadLinkLong.indexOf("http://mp3.zing.vn/download/song/"), downloadLinkLong.indexOf(" class=\"music-download _btnDownload\"")-1);
//					
//					ZingSongInfo zingSongInfo = new ZingSongInfo();
//					zingSongInfo.setName(name);
//					zingSongInfo.setArtist(artist);
////					zingSongInfo.setBit(bit);
//					zingSongInfo.setView(view);
//					zingSongInfo.setId(id);
//					zingSongInfo.setDownloadLink(downloadlink);
//					zingListSongs.add(zingSongInfo);
//				} catch (Exception e) {
//					// TODO: handle exception
//					e.printStackTrace();
//				}
//			}
//		} catch (Exception e) {
//			// TODO: handle exception
//			e.printStackTrace();
//		}		
//		
//		return zingListSongs;
//	}
	
	public static ArrayList<ZingSongInfo> getListSongsPro(String searchText, int offset, String typeSearch) {
//		http://m.mp3.zing.vn/tim-kiem/bai-hat.html?q=con+mua+ngang+qua&offset=0&t=song
		searchText = searchText.trim().replace(" ", "+");
		String url = "http://m.mp3.zing.vn/tim-kiem/bai-hat.html?q=" + searchText + "&offset=" + offset + "&t=" + typeSearch;
		
		ArrayList<ZingSongInfo> zingListSongs = new ArrayList<ZingSongInfo>();
		try {
			Document document = Jsoup.connect(url).get();
			
			// Get section specsong
			// Get section specsong
			// Get section specsong
			Element sectionSpecSong = document.getElementsByClass("section-specsong").first();
			Elements listSpecSong = sectionSpecSong.getElementsByTag("a");
			
			for (int i = 0; i < listSpecSong.size(); i++) {
				Element specSong = listSpecSong.get(i);
				String href = specSong.attr("href");
				
				String songId = href.substring(href.lastIndexOf('/')+1, href.indexOf(".html"));
				String songName = StringEscapeUtils.unescapeHtml4(specSong.getElementsByTag("h3").first().html());
				String songArtist = StringEscapeUtils.unescapeHtml4(specSong.getElementsByTag("h4").first().html().replace("<span>", "").replace("</span>", ""));
				String songViews = specSong.getElementsByTag("li").first().html();
				
				ZingSongInfo zingSongInfo = new ZingSongInfo();
				zingSongInfo.setId(songId);
				zingSongInfo.setName(songName);
				zingSongInfo.setArtist(songArtist);
				zingSongInfo.setView(songViews + " views");
				zingListSongs.add(zingSongInfo);
			}
			
			// Get section normalsong
			Element sectionNormalSongs = document.getElementsByClass("section-song").first();
			Elements listNormalSong = sectionNormalSongs.getElementsByTag("a");
			for (int i = 0; i < listNormalSong.size(); i++) {
				Element specSong = listNormalSong.get(i);
				String href = specSong.attr("href");
				
				String songId = href.substring(href.lastIndexOf('/')+1, href.indexOf(".html"));
				String songName = StringEscapeUtils.unescapeHtml4(specSong.getElementsByTag("h3").first().html());
				String songArtist = StringEscapeUtils.unescapeHtml4(specSong.getElementsByTag("h4").first().html().replace("<span>", "").replace("</span>", ""));
				String songViews = specSong.getElementsByTag("li").first().html();
				
				ZingSongInfo zingSongInfo = new ZingSongInfo();
				zingSongInfo.setId(songId);
				zingSongInfo.setName(songName);
				zingSongInfo.setArtist(songArtist);
				zingSongInfo.setView(songViews + " views");
				zingListSongs.add(zingSongInfo);
			}
			
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
		
		return zingListSongs;
	}
	
	
	
	public static JSONObject convertSongToJSON(ZingSongInfo zingSongInfo) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(ZingSongInfo.NAME, zingSongInfo.getName());
			jsonObject.put(ZingSongInfo.ARTIST, zingSongInfo.getArtist());
			jsonObject.put(ZingSongInfo.ID, zingSongInfo.getId());
			jsonObject.put(ZingSongInfo.VIEW, zingSongInfo.getView());
			return jsonObject;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
	
	public static ZingSongInfo convertJSONToSong(String jsonString) {
		try {
			ZingSongInfo zingSongInfo = new ZingSongInfo();
			JSONObject jsonObject = new JSONObject(jsonString);
			zingSongInfo.setArtist(jsonObject.getString(ZingSongInfo.ARTIST));
			zingSongInfo.setId(jsonObject.getString(ZingSongInfo.ID));
			zingSongInfo.setName(jsonObject.getString(ZingSongInfo.NAME));
			zingSongInfo.setView(jsonObject.getString(ZingSongInfo.VIEW));
			return zingSongInfo;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
	
	public static void deleteSongFromPlaylist(Context context, String playlistName, String songId) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String listSongs = sharedPreferences.getString(playlistName, "");
		listSongs = listSongs.replace(songId, "").replaceAll("\\s+", " ").trim();
		sharedPreferences.edit().putString(playlistName, listSongs).commit();
	}
	
	public static ZingSongInfo getZingSongInfoFromId(Context context, String songId) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String zingSongJSON = sharedPreferences.getString(songId, "");
		return convertJSONToSong(zingSongJSON);
	}
	
	public static void saveZingSongInfo(Context context, ZingSongInfo zingSongInfo) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		sharedPreferences.edit().putString(zingSongInfo.getId(), convertSongToJSON(zingSongInfo).toString()).commit();
	}
	
	public static boolean checkSongExistInPlaylist(Context context, String playlistName, String songId) {
		if (playlistName.equals("")) 
			return true;
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString(playlistName, "").contains(songId);
	}
	
	
}
