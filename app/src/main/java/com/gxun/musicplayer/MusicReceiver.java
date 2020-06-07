package com.gxun.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class MusicReceiver extends BroadcastReceiver {// receive Broadcast

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent != null) {
            Bundle bundle = intent.getExtras();
            Intent it = new Intent(context, MusicReceiverService.class);    // call service for MusicReceiverService.class
            it.putExtras(bundle);
            if (bundle != null) {
                int op = bundle.getInt("op");
                if (op == 4) {
                    context.stopService(it);        // stopService
                } else {
                    context.startService(it);        // startService
                }
            }
        }
    }
}
