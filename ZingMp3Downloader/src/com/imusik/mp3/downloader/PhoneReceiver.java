package com.imusik.mp3.downloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;

public class PhoneReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Bundle extra = intent.getExtras();
		if (extra != null) {
			String state = extra.getString(TelephonyManager.EXTRA_STATE);
			Intent playServiceIntent = new Intent(context, PlayerService.class);
			
			if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) || state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
				playServiceIntent.putExtra(PlayerService.EXTRA_COMMAND, PlayerService.COMMAND_PHONE_RINGING_OR_OFFHOOK);
			} else {
				playServiceIntent.putExtra(PlayerService.EXTRA_COMMAND, PlayerService.COMMAND_PHONE_IDLE);
			}
			
			context.startService(playServiceIntent);
		}		 
	}

}
