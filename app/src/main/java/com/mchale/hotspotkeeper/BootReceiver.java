package com.mchale.hotspotkeeper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts the keeper as soon as the box powers up. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (WifiKeeper.enabled(context) && !KeeperService.running) {
            KeeperService.start(context);
        }
    }
}
