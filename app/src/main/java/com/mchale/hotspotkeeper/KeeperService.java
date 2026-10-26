package com.mchale.hotspotkeeper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/** Runs in the background (with a small notification so Android won't kill it). */
public class KeeperService extends Service {
    private static final String CHANNEL = "keeper";
    private static final int NOTIF_ID = 1;
    private static final long CONNECTED_INTERVAL_MS = 30000;
    private static final long SEARCHING_INTERVAL_MS = 8000;

    static volatile String lastStatus = "Not running";
    static volatile boolean running = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable loop = new Runnable() {
        @Override public void run() {
            String status;
            try {
                status = WifiKeeper.tick(KeeperService.this);
            } catch (Exception e) {
                status = "Error: " + e.getMessage();
            }
            if (!status.equals(lastStatus)) {
                lastStatus = status;
                notifyStatus(status);
            }
            boolean connected = status.startsWith("Connected");
            handler.postDelayed(this, connected ? CONNECTED_INTERVAL_MS : SEARCHING_INTERVAL_MS);
        }
    };

    static void start(Context c) {
        Intent i = new Intent(c, KeeperService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    static void stop(Context c) {
        c.stopService(new Intent(c, KeeperService.class));
    }

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Hotspot Keeper", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        startForeground(NOTIF_ID, build("Starting…"));
        running = true;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        handler.removeCallbacks(loop);
        handler.post(loop);
        return START_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(loop);
        running = false;
        lastStatus = "Not running";
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void notifyStatus(String text) {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIF_ID, build(text));
    }

    private Notification build(String text) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        int flags = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), flags);
        return b.setContentTitle("Hotspot Keeper")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentIntent(open)
                .setOngoing(true)
                .build();
    }
}
