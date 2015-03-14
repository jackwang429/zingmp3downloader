package com.idroid.lib.support;

import android.app.Activity;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class SupportAdview {
	Activity mActivity;
	String mAdmobId;
	AdView mAdView;
	ImageView mImgClose;
	
	float density;
	int screenWidth;
	int screenHeight;
	int bannerHeight = 50;
	
	int gravity = Gravity.CENTER | Gravity.TOP;
	int widthLayoutParams = FrameLayout.LayoutParams.WRAP_CONTENT;
	
	View bannerStartApp;
	
	public SupportAdview(Activity activity, String admobId) {
		// TODO Auto-generated constructor stub
		mActivity = activity;
		mAdmobId = admobId;
		
		initAdview();
		attachAdview();
		attachCloseButton();
	}
	
	public void initAdview() {
		mAdView = new AdView(mActivity);
		mAdView.setAdSize(AdSize.BANNER);
		mAdView.setAdUnitId(mAdmobId);
		
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(widthLayoutParams, FrameLayout.LayoutParams.WRAP_CONTENT);
		lp.gravity = gravity;
		mAdView.setLayoutParams(lp);
		AdRequest adRequest = new AdRequest.Builder().build();
		mAdView.loadAd(adRequest);
	}
	
	public void setBannerStatAppView(View bannerStartApp) {
		this.bannerStartApp = bannerStartApp;
	}
	
	public void attachAdview() {
		((FrameLayout) mActivity.findViewById(android.R.id.content)).addView(mAdView);
	}
	
	public void hideAdview() {
		mAdView.setVisibility(View.GONE);
	}
	
	public void showAdview() {
		mAdView.setVisibility(View.VISIBLE);
	}
	
	public void hideCloseButton() {
		mImgClose.setVisibility(View.GONE);
	}
	
	public void showCloseButton() {
		mImgClose.setVisibility(View.VISIBLE);
	}
	
	public void attachCloseButton() {
		mImgClose = new ImageView(mActivity);
		mImgClose.setImageResource(R.drawable.ic_close);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		lp.topMargin = (int) (50 * mActivity.getResources().getDisplayMetrics().density);
		lp.gravity = Gravity.RIGHT;
		mImgClose.setLayoutParams(lp);
		mImgClose.setVisibility(View.GONE);
		
		mImgClose.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mImgClose.setVisibility(View.GONE);
				mAdView.setVisibility(View.GONE);
				
				if (bannerStartApp != null) {
					bannerStartApp.setVisibility(View.GONE);
				}
				
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						mImgClose.setVisibility(View.VISIBLE);
						mAdView.setVisibility(View.VISIBLE);
						
						if (bannerStartApp != null) {
							bannerStartApp.setVisibility(View.VISIBLE);
						}	
					}
				}, 5 * 60 * 1000);
			}
		});
		
		((FrameLayout) mActivity.findViewById(android.R.id.content)).addView(mImgClose);
		
	}
	
	
}
