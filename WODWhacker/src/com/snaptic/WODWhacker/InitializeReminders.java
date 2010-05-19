package com.snaptic.WODWhacker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.snaptic.WODWhacker.AutoUpdate;

/*
 * Setting up broadcast reciever for auto updates. Change name to reflect this -htormey.
 * 
 * */
public class InitializeReminders extends BroadcastReceiver {
	@Override
    public void onReceive(Context context, Intent intent)
    {   
      context.startService(new Intent("com.android.intent.action.INITIALIZE_ALARMS"));
      AutoUpdate.schedule(context);
    }
}