package com.smerkous.greengrass.boot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.smerkous.greengrass.services.GrassService;

public class GrassBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent grassService = new Intent(context, GrassService.class);
            grassService.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            grassService.putExtra("boot", true);
            context.startService(grassService);
        }
    }
}
