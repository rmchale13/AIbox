package com.mchale.hotspotkeeper;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** One simple screen: pick the hotspot, start/stop, see status. */
public class MainActivity extends Activity {
    private EditText ssidField;
    private EditText passField;
    private TextView statusView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            String now = WifiKeeper.currentSsid(MainActivity.this);
            statusView.setText("Status: " + KeeperService.lastStatus
                    + "\nCurrent Wi-Fi: " + (now == null ? "none" : now)
                    + "\nAuto-start at power on: " + (WifiKeeper.enabled(MainActivity.this) ? "ON" : "OFF"));
            handler.postDelayed(this, 2000);
        }
    };

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        int pad = dp(24);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Hotspot Keeper");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(title);

        TextView help = new TextView(this);
        help.setText("Join your phone's hotspot once, tap \"Use current Wi-Fi\", then \"Save & start\". "
                + "The box will reconnect to it automatically every time it powers on.");
        help.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        help.setPadding(0, dp(8), 0, dp(16));
        col.addView(help);

        ssidField = new EditText(this);
        ssidField.setHint("Hotspot name (e.g. Ryan's iPhone)");
        ssidField.setSingleLine(true);
        ssidField.setText(WifiKeeper.ssid(this));
        col.addView(ssidField);

        passField = new EditText(this);
        passField.setHint("Password (only needed if not already saved)");
        passField.setSingleLine(true);
        passField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passField.setText(WifiKeeper.password(this));
        col.addView(passField);

        col.addView(button("Use current Wi-Fi", new View.OnClickListener() {
            @Override public void onClick(View v) {
                String s = WifiKeeper.currentSsid(MainActivity.this);
                if (s == null) toast("Not connected, or location permission is off.");
                else ssidField.setText(s);
            }
        }));

        col.addView(button("Save & start", new View.OnClickListener() {
            @Override public void onClick(View v) {
                String s = ssidField.getText().toString().trim();
                if (s.length() == 0) { toast("Enter the hotspot name first."); return; }
                WifiKeeper.save(MainActivity.this, s, passField.getText().toString(), true);
                KeeperService.stop(MainActivity.this);
                KeeperService.start(MainActivity.this);
                toast("Keeping \"" + s + "\" connected.");
            }
        }));

        col.addView(button("Stop", new View.OnClickListener() {
            @Override public void onClick(View v) {
                WifiKeeper.setEnabled(MainActivity.this, false);
                KeeperService.stop(MainActivity.this);
                toast("Stopped. It won't start at power on.");
            }
        }));

        statusView = new TextView(this);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        statusView.setPadding(0, dp(24), 0, 0);
        col.addView(statusView);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);

        // Needed on Android 8.1+ to read the Wi-Fi name and see nearby networks.
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        if (WifiKeeper.enabled(this) && !KeeperService.running) KeeperService.start(this);
    }

    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(64));
        lp.topMargin = dp(12);
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
