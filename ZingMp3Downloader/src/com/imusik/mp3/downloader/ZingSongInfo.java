package com.imusik.mp3.downloader;

public class ZingSongInfo {
	public static final String NAME = "name";
	public static final String ARTIST = "artist";
	public static final String VIEW = "view";
	public static final String ID = "id";
//	public static final String BIT = "bit";
//	public static final String DOWNLOADLINK = "downloadlink";
	
	String name;
	String artist;
	String view;
	String id;
	String downloadLink;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getArtist() {
		return artist;
	}
	public void setArtist(String artist) {
		this.artist = artist;
	}
	public String getView() {
		return view;
	}
	public void setView(String view) {
		this.view = view;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getDownloadLink() {
		return downloadLink;
	}
	public void setDownloadLink(String downloadLink) {
		this.downloadLink = downloadLink;
	}
	
}
