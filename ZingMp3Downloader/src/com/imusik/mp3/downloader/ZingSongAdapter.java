package com.imusik.mp3.downloader;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ZingSongAdapter extends BaseAdapter{
	
	Context mContext;
	String[] mSongIds;
	HashMap<String, ZingSongInfo> mMapIdToInfo;
	LayoutInflater mInflater;
	OnClickListener mOnOpenPopupMenu;
	String mCurrentPlaylist;
	String mCurrentPlayingSongId = "";
	
	public ZingSongAdapter(Context context) {
		// TODO Auto-generated constructor stub
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mSongIds = ZingTools.getIdsFromPlaylist(mContext, "");
		mMapIdToInfo = new HashMap<String, ZingSongInfo>();
		mCurrentPlaylist = "";
	}
	
	public void setOnOpenPopupMenu(OnClickListener onOpenPopupMenu) {
		mOnOpenPopupMenu = onOpenPopupMenu;
	}
	
	public void reset() {
		mSongIds = ZingTools.getIdsFromPlaylist(mContext, mCurrentPlaylist);
		notifyDataSetChanged();
	}
	
	public void setPlaylist(String playlistName) {
		mCurrentPlaylist = playlistName;
		mSongIds = ZingTools.getIdsFromPlaylist(mContext, playlistName);
		notifyDataSetChanged();
	}
	
	public void setCurrentPlayingSongId(String songId) {
		mCurrentPlayingSongId = songId;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mSongIds.length;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.row_song, null);
		}
		
		convertView.findViewById(R.id.img_btn_download).setVisibility(View.GONE);
		convertView.findViewById(R.id.img_btn_popupmenu).setVisibility(View.VISIBLE);
		
		String songId = mSongIds[position];
		if (mMapIdToInfo.get(songId) == null) {
			ZingSongInfo zingSongInfo = ZingMp3Tools.getZingSongInfoFromId(mContext, songId);
			mMapIdToInfo.put(songId, zingSongInfo);
		}
		ZingSongInfo info = mMapIdToInfo.get(songId);
			
		((TextView)convertView.findViewById(R.id.tv_name)).setText(info.getName());
		((TextView)convertView.findViewById(R.id.tv_artist)).setText(info.getArtist());
		((TextView)convertView.findViewById(R.id.tv_view)).setText(info.getView());
		
		convertView.findViewById(R.id.img_btn_popupmenu).setTag(R.id.tag_zingsonginfo, info);
		convertView.findViewById(R.id.img_btn_popupmenu).setOnClickListener(mOnOpenPopupMenu);
		convertView.setTag(R.id.tag_zingsonginfo, info);
		
		if (songId.equals(mCurrentPlayingSongId)) {
			convertView.setBackgroundColor(Color.parseColor("#222222"));
		} else {
			convertView.setBackgroundColor(Color.BLACK);
		}
		
		return convertView;
	}
	
	

}
