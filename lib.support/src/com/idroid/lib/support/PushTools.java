package com.idroid.lib.support;

import java.io.IOException;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class PushTools {
	
	public static final String SENDER_ID = "230356017302";
	public static final String API_LOG = "http://blackmaze.net/push.notification/log.php";
//	public static final String API_LOG = "http://192.168.54.107/push.notification/log.php";
	
	public static void logToken(final Context context) {
		// get token and log
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context.getApplicationContext());
					String regId = gcm.register(SENDER_ID);
					
					String params = "token=" + regId
								  + "&package=" + context.getPackageName()
								  + "&mac=" + getMacAddress(context)
								  + "&android_id=" + getAndroidID(context);
					String response = ServiceHelper.post(API_LOG, params);
					
					if (response.contains("successful")) {
						PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).edit().putBoolean("logged_token", true).commit();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}).start();
	}
	
	public static String getMacAddress(Context context) {
	    WifiManager wimanager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	    String macAddress = wimanager.getConnectionInfo().getMacAddress();
	    if (macAddress == null) {
	        macAddress = "null";
	    }
	    return macAddress;
	}
	
	public static String getAndroidID(Context context) {
		String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		if (androidId == null) {
			androidId = "null";
		}
		return androidId;
	}
	
	public static void generateNotification(Context context, Bundle bundle) {
		String title = bundle.getString("title");
		String message = bundle.getString("message");
		String sound = bundle.getString("sound");
		String vibrate = bundle.getString("vibrate");
		String icon = bundle.getString("icon");
		String checked_install = bundle.getString("check_installed");
		
		if (checked_install != null && checked_install.equals("yes")) {
			String link = bundle.getString("link");
			String packageName = link.split("=")[1];
			if (SupportTools.checkAppInstalled(context, packageName)) {
				return;
			}
		}
		
		Intent intent = null;
		String openType = bundle.getString("open_type");
		if (openType.equals("open_link")) {
			String link = bundle.getString("link");
			intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(link));			
		} else if (openType.equals("open_app")) {
			intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
		}
		
		if (intent == null) {
			return;
		}
		
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
		.setContentTitle(title)
		.setContentText(message)
		.setTicker(message)
		.setContentIntent(pendingIntent)
		.setAutoCancel(true)
		.setLights(Color.GREEN, 3000, 3000);
		
		if (sound != null && sound.equals("yes")) {
			builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));	
		}
		
		if (vibrate != null && vibrate.equals("yes")) {
			builder.setVibrate(new long[]{200,200,200});	
		}
		
		if (icon == null) {
			builder.setSmallIcon(context.getApplicationInfo().icon);
		} else {
			builder.setSmallIcon(R.drawable.ic_playstore);
		}
		
		((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, builder.build());
	}	
}
