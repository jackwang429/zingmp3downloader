package com.idroid.lib.support;

import com.google.android.gms.drive.internal.ac;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

public class SupportTools {
	
	public static void saveString(Context context, String key, String value) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).commit();
	}
	
	public static String getString(Context context, String key) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key, null);
	}
	
	public static void saveBoolean(Context context, String key, boolean value) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).commit();
	}
	
	public static boolean getBoolean(Context context, String key) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false);
	}
	
	public static void saveInt(Context context, String key, int value) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).commit();
	}
	
	public static int getInt(Context context, String key) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, 0);
	}
	
	public static void saveAdmobId(Context context, String value) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putString("admob_id", value).commit();
	}
	
	public static String getAdmobId(Context context) {
		String admobId = PreferenceManager.getDefaultSharedPreferences(context).getString("admob_id", "");
		if (admobId.equals("")) {
			return getAdmobIdDefault();	
		} else {
			return admobId;
		}
	}
	
	public static String getAdmobIdDefault() {
		return "ca-app-pub-7884398869161784/7174637350";
	}
	
	public static void saveExtraData(Context context, String value) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putString("extra_data", value).commit();
	}
	
	public static String getExtraData(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("extra_data", "");
	}
	
	public static void checkPackageName(Activity activity, String encryptedPackageName) {
		try {
			MCrypt mCrypt = new MCrypt("Ljava/lang/Runna", "Ljava/lang/Runna");
			if (!mCrypt.decryptFromHexString(encryptedPackageName).equals(activity.getPackageName())) {
				activity.finish();
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			activity.finish();
		}
	}
	
	public static boolean checkAppInstalled(Context context, String packageName) {
		if (context.getPackageManager().getLaunchIntentForPackage(packageName) == null) {
			return false;
		} else {
			return true;
		}
	}	

}
