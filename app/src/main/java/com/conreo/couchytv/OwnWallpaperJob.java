package com.conreo.couchytv;

import android.app.job.*;
import android.graphics.BitmapFactory;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** A daily finite catalogue sync. Originals and attribution remain available offline. */
public final class OwnWallpaperJob extends JobService {
    private volatile boolean stopped;
    private ExecutorService executor;

    @Override
    public boolean onStartJob(JobParameters params) {
        stopped = false;
        executor = Executors.newSingleThreadExecutor();
        executor.execute(
                () -> {
                    try {
                        android.content.SharedPreferences prefs =
                                getSharedPreferences("own_appearance", 0);
                        if (!prefs.getBoolean("dailyDownloads", true)) return;
                        long now = System.currentTimeMillis();
                        if (now - prefs.getLong("lastDownloadAt", 0) < 20 * 60 * 60 * 1000L) return;
                        int target = prefs.getInt("downloadTarget", 100),
                                batch = prefs.getInt("downloadsPerDay", 3),
                                done = 0;
                        JSONArray catalog = OwnWallpapers.catalog(this);
                        List<String> available = OwnWallpapers.available(this);
                        File directory = new File(getFilesDir(), "wallpapers");
                        directory.mkdirs();
                        for (int n = 0;
                                n < catalog.length()
                                        && !stopped
                                        && done < batch
                                        && available.size() + done < target;
                                n++) {
                            JSONObject photo = catalog.getJSONObject(n);
                            String id = photo.getString("id"), name = id + ".jpg";
                            if (available.contains("asset:" + name)
                                    || new File(directory, name).exists()) continue;
                            URL url = new URL(photo.getString("url"));
                            if (!url.getProtocol().equals("https")
                                    || !url.getHost().equals("upload.wikimedia.org")) continue;
                            File temp = new File(directory, id + ".part");
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setConnectTimeout(15000);
                            connection.setReadTimeout(20000);
                            connection.setRequestProperty(
                                    "User-Agent", "OwnTV/0.2 personal wallpaper collection");
                            try {
                                if (connection.getResponseCode() != 200) continue;
                                try (InputStream input = connection.getInputStream();
                                        OutputStream out = new FileOutputStream(temp)) {
                                    byte[] buffer = new byte[65536];
                                    int length;
                                    long total = 0;
                                    while (!stopped && (length = input.read(buffer)) != -1) {
                                        total += length;
                                        if (total > 35 * 1024 * 1024) throw new IOException("size");
                                        out.write(buffer, 0, length);
                                    }
                                }
                                if (stopped) break;
                                BitmapFactory.Options bounds = new BitmapFactory.Options();
                                bounds.inJustDecodeBounds = true;
                                BitmapFactory.decodeFile(temp.getPath(), bounds);
                                if (bounds.outWidth > 3840
                                        && bounds.outHeight >= 2160
                                        && temp.renameTo(new File(directory, name))) done++;
                            } catch (IOException ignored) {
                            } finally {
                                connection.disconnect();
                                temp.delete();
                            }
                        }
                        if (done > 0)
                            prefs.edit()
                                    .putLong("lastDownloadAt", now)
                                    .putString(
                                            "downloadStatus",
                                            done
                                                    + " novas fotos · "
                                                    + OwnWallpapers.available(this).size()
                                                    + " locais")
                                    .apply();
                        else
                            prefs.edit()
                                    .putString(
                                            "downloadStatus",
                                            available.size() >= target
                                                    ? "Coleção completa"
                                                    : "Aguardando nova tentativa de download")
                                    .apply();
                    } catch (Exception ignored) {
                    } finally {
                        if (!stopped) jobFinished(params, false);
                        if (executor != null) executor.shutdown();
                    }
                });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        stopped = true;
        if (executor != null) executor.shutdownNow();
        return true;
    }
}
