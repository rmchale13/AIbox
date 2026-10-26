# Hotspot Keeper

A small Android app for the Car Smart Box. When the box powers on, it turns Wi-Fi on and keeps reconnecting to your phone's hotspot whenever the hotspot is in range.

## Build the APK (free, no software to install)
1. On github.com, create a new **private** repository called `HotspotKeeper`.
2. Upload every file and folder in this zip, including the hidden `.github` folder. ("Add file → Upload files" accepts the folders if you drag them in from a computer.)
3. Open the **Actions** tab. The "Build APK" run starts on its own and takes about 3 minutes. If it doesn't start, click "Build APK" → "Run workflow".
4. Open the finished run and download **HotspotKeeper-apk**. It's a zip that contains `app-debug.apk`.

## Install on the box
1. Copy `app-debug.apk` to a USB stick, plug it into the box, and open it with the box's file manager.
2. If Android asks, allow installs from that source. If you get an "older version of Android" warning, tap **Install anyway**.

## Set it up
1. Turn on your iPhone's hotspot and join it once from the box's Wi-Fi settings.
2. Open **Hotspot Keeper** and allow the Location permission. Android needs it to read Wi-Fi names.
3. Tap **Use current Wi-Fi**, then **Save & start**.
4. If the box's Settings has an "Auto-start" or "Background apps" list, allow Hotspot Keeper there.

**Stop** turns it off, and it stays off after a restart.
