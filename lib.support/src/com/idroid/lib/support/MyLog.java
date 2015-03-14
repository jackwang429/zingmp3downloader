package com.idroid.lib.support;

import android.util.Log;

public class MyLog {
	
	public static boolean IS_DEBUG = true;
	
	public static void log(String message) {
		if (IS_DEBUG) {
			Log.e("vkl", message);	
		}
	}
}
