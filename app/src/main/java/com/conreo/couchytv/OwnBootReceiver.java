package com.conreo.couchytv;

import android.content.*;

public final class OwnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            OwnWallpapers.schedule(context);
            if (context.getSharedPreferences("own_ssh", 0).getBoolean("enabled", false))
                try {
                    OwnSshService.start(context);
                } catch (IllegalStateException ignored) {
                }
        }
    }
}
