package com.imusik.mp3.downloader;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.PopupWindow;

public class CustomPopupWindow extends PopupWindow{
	PopupWindow mPopup;
	
//	public CustomPopupWindow(Context context, AttributeSet attrs, int defStyleAttr) {
//	    mPopup = new PopupWindow(context, attrs, defStyleAttr);
//	    mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
//	    fixPopupWindow(mPopup);
//	}
//	
	public CustomPopupWindow(View contentView, int width, int height) {
		// TODO Auto-generated constructor stub
		super(contentView, width, height);
		fixPopupWindow(this);
	}

//	@SuppressLint("NewApi")
//	public CustomPopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
//	        Context wrapped = new ContextThemeWrapper(context, defStyleRes);
//	        mPopup = new PopupWindow(wrapped, attrs, defStyleAttr);
//	    } else {
//	        mPopup = new PopupWindow(context, attrs, defStyleAttr, defStyleRes);
//	    }
//	    mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
//	    fixPopupWindow(mPopup);
//	}

	private void fixPopupWindow(final PopupWindow window) {
	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	        try {
	            final Field fAnchor = PopupWindow.class.getDeclaredField("mAnchor");
	            fAnchor.setAccessible(true);
	            Field listener = PopupWindow.class.getDeclaredField("mOnScrollChangedListener");
	            listener.setAccessible(true);
	            final ViewTreeObserver.OnScrollChangedListener originalListener = (ViewTreeObserver.OnScrollChangedListener) listener.get(window);
	            ViewTreeObserver.OnScrollChangedListener newListener = new ViewTreeObserver.OnScrollChangedListener() {
	                @Override
	                public void onScrollChanged() {
	                    try {
	                        WeakReference<View> mAnchor = (WeakReference<View>) fAnchor.get(window);
	                        if (mAnchor == null || mAnchor.get() == null) {
	                            return;
	                        } else {
	                            originalListener.onScrollChanged();
	                        }
	                    } catch (IllegalAccessException e) {
	                        e.printStackTrace();
	                    }
	                }
	            };
	            listener.set(window, newListener);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}

}
