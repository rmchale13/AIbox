package com.mchale.hotspotkeeper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.List;

/** All the Wi-Fi logic lives here. tick() is called every few seconds by KeeperService. */
public final class WifiKeeper {
    private static final String PREFS = "keeper";
    private static long lastAttemptMs = 0;

    private WifiKeeper() {}

    // ---------- settings ----------
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    static String ssid(Context c) { return prefs(c).getString("ssid", ""); }
    static String password(Context c) { return prefs(c).getString("pass", ""); }
    static boolean enabled(Context c) { return prefs(c).getBoolean("enabled", false); }

    static void save(Context c, String ssid, String pass, boolean enabled) {
        prefs(c).edit().putString("ssid", ssid).putString("pass", pass).putBoolean("enabled", enabled).apply();
    }

    static void setEnabled(Context c, boolean enabled) {
        prefs(c).edit().putBoolean("enabled", enabled).apply();
    }

    // ---------- helpers ----------
    static WifiManager wifi(Context c) {
        return (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    /** Compare names loosely: iPhones often use a curly apostrophe (Ryan’s iPhone). */
    static boolean same(String a, String b) {
        if (a == null || b == null) return false;
        return norm(a).equals(norm(b));
    }

    private static String norm(String s) {
        return strip(s).replace('\u2019', '\'').replace('\u2018', '\'').trim();
    }

    static String strip(String s) {
        if (s == null) return null;
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length() - 1);
        return s;
    }

    /** SSID the box is connected to right now, or null. */
    static String currentSsid(Context c) {
        WifiInfo info = wifi(c).getConnectionInfo();
        if (info == null || info.getNetworkId() == -1) return null;
        String s = strip(info.getSSID());
        if (s == null || s.length() == 0 || "<unknown ssid>".equals(s)) return null;
        return s;
    }

    private static int findSavedNetwork(WifiManager wm, String ssid) {
        try {
            List<WifiConfiguration> list = wm.getConfiguredNetworks();
            if (list != null) {
                for (WifiConfiguration cfg : list) {
                    if (same(ssid, cfg.SSID)) return cfg.networkId;
                }
            }
        } catch (SecurityException ignored) { }
        return -1;
    }

    private static int addNetwork(WifiManager wm, String ssid, String pass) {
        WifiConfiguration cfg = new WifiConfiguration();
        cfg.SSID = "\"" + ssid + "\"";
        if (pass == null || pass.length() == 0) {
            cfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else {
            cfg.preSharedKey = "\"" + pass + "\"";
            cfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        }
        int id = wm.addNetwork(cfg);
        if (id != -1) wm.saveConfiguration();
        return id;
    }

    /** null = can't tell (no scan permission or no results yet). */
    private static Boolean hotspotVisible(WifiManager wm, String ssid) {
        try {
            List<ScanResult> results = wm.getScanResults();
            if (results == null || results.isEmpty()) return null;
            for (ScanResult r : results) {
                if (same(ssid, r.SSID)) return Boolean.TRUE;
            }
            return Boolean.FALSE;
        } catch (SecurityException e) {
            return null;
        }
    }

    // ---------- the main loop body ----------
    static String tick(Context c) {
        String ssid = ssid(c);
        if (ssid.length() == 0) return "No hotspot set. Open the app and pick one.";

        WifiManager wm = wifi(c);
        if (!wm.isWifiEnabled()) {
            wm.setWifiEnabled(true);
            return "Wi-Fi was off. Turning it on…";
        }

        if (same(ssid, currentSsid(c))) return "Connected to " + ssid;

        int id = findSavedNetwork(wm, ssid);
        if (id == -1) id = addNetwork(wm, ssid, password(c));
        if (id == -1) {
            return "\"" + ssid + "\" isn't saved on this box. Join it once in Wi-Fi settings, or enter its password in the app.";
        }

        // Don't trust the scan list: Android throttles background scans, so it's often stale.
        // Always ask Android to connect; it does its own fresh search when it does.
        Boolean visible = hotspotVisible(wm, ssid);
        try { wm.startScan(); } catch (Exception ignored) { }
        String seen = Boolean.FALSE.equals(visible) ? " (not seen yet)" : "";

        long now = System.currentTimeMillis();
        if (now - lastAttemptMs < 20000) return "Connecting to " + ssid + "…" + seen;
        lastAttemptMs = now;

        wm.enableNetwork(id, true);
        wm.reconnect();
        return "Connecting to " + ssid + "…" + seen;
    }
}
