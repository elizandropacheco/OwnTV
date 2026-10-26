package com.conreo.couchytv;

import android.content.*;
import android.app.job.*;
import org.json.*;
import java.io.*;
import java.util.*;

public final class OwnWallpapers {
    public static JSONArray catalog(Context c) {
        try (InputStream i = c.getAssets().open("ep/wallpapers/catalog.json")) {
            return new JSONArray(
                    new String(OwnIO.read(i), java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static List<String> available(Context c) {
        ArrayList<String> result = new ArrayList<>();
        try {
            for (String f : c.getAssets().list("ep/wallpapers"))
                if (f.endsWith(".jpg")) result.add("asset:" + f);
        } catch (Exception ignored) {
        }
        File[] files = new File(c.getFilesDir(), "wallpapers").listFiles();
        if (files != null)
            for (File f : files) if (f.getName().endsWith(".jpg")) result.add(f.getAbsolutePath());
        Collections.sort(result);
        return result;
    }

    public static void schedule(Context c) {
        JobScheduler scheduler = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (!c.getSharedPreferences("own_appearance", 0).getBoolean("dailyDownloads", true)) {
            scheduler.cancel(8102);
            return;
        }
        if (scheduler.getPendingJob(8102) == null)
            scheduler.schedule(
                    new JobInfo.Builder(8102, new ComponentName(c, OwnWallpaperJob.class))
                            .setPeriodic(24 * 60 * 60 * 1000L)
                            .setPersisted(true)
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                            .setRequiresStorageNotLow(true)
                            .build());
    }
}
