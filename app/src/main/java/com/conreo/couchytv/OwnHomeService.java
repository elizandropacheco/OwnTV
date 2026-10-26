package com.conreo.couchytv;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;

/** Redirect only the stock HOME activity; do not inspect or record screen content. */
public class OwnHomeService extends AccessibilityService {
    private long lastLaunch;
    private boolean homePressed;

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            homePressed = true;
            if (event.getRepeatCount() == 0) {
                getSharedPreferences("home", 0).edit().putLong("allowOriginalUntil", 0).apply();
                openHome();
            }
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_UP && homePressed) {
            homePressed = false;
            return true;
        }
        return false;
    }

    private void openHome() {
        lastLaunch = SystemClock.elapsedRealtime();
        startActivity(
                new Intent(this, MainActivity.class)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        String pkg = String.valueOf(event.getPackageName());
        String activity = String.valueOf(event.getClassName());
        if (!pkg.equals("com.google.android.apps.tv.launcherx")
                || !activity.endsWith("home.HomeActivity")) return;
        if (SystemClock.elapsedRealtime() - lastLaunch < 1500) return;
        if (getSharedPreferences("home", 0).getLong("allowOriginalUntil", 0)
                > System.currentTimeMillis()) return;
        openHome();
    }

    @Override
    public void onInterrupt() {}
}
