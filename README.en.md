# OwnTV

![OwnTV](docs/branding/owntv-wordmark.png)

**Your TV. Your rules.** An open Android TV media center with a personal home screen, games, cameras and network access.

[![GPLv3](https://img.shields.io/badge/license-GPLv3-blue)](LICENSE)
[![Android TV](https://img.shields.io/badge/platform-Android%20TV-2CEB43)](README.md#compatibilidade)
[![Local build](https://img.shields.io/badge/build-locally_verified-2CEB43)](docs/VALIDATION.md)

[Português — complete installation and customization guide](README.md) · [Current status and remaining work](docs/STATUS.pt-BR.md)

OwnTV combines an open layout with geometric EP branding, translucent surfaces, gradient focus borders and brief color transitions. It keeps the original Android settings and launcher available.

## Features

- An advertising-free launcher home screen.
- Editable favorites, categories, order, visibility, cards, labels, actions, artwork, alignment and image intensity.
- A large, remote-friendly app dock and four complete recommendation cards per page.
- Content from Android TV's shared Watch Next and preview providers, where applications support them.
- A persistent Inputs button on the home screen in Android and webOS.
- Configurable PS5 HDMI entry, picture settings, Game Master shortcuts and Fortnite setup guidance.
- A scrolling RTSP camera gallery with up to four active previews, full-resolution main-stream playback and focus restoration.
- Fifteen original photographs above 4K, a catalog of 100, offline rotation and configurable daily downloads.
- Date, time and selectable IANA time zones.
- Configurable two-line news headlines for selected topics, from Google News RSS with a local cache.
- An SSH/SFTP server with configurable credentials and port, running with the app's Android permissions.
- On-demand image decoding and short focus effects to control memory and background work.

Advertising inside streaming services remains controlled by those apps. YouTube subscriptions open in YouTube; direct subscription videos require the app to share them through Android TV. OwnTV does not use account cookies or passwords to scrape recommendations.

## Build and install

Tested on TCL Android TV 14. LG/webOS has a separate experimental implementation; this APK remains Android-only.

See the [LG Developer account, country selection, Developer Mode, SSH and hardware guide](docs/LG-WEBOS.pt-BR.md) (Portuguese). Developer SSH was verified on six LG 43LM631C0SB units on 2026-10-27 (firmware 05.40.97, SDK 4.10.0). The [webOS 0.2.1 experimental app](webos/README.md) adds topic-based news, four public YouTube suggestions per page and cameras through a private external bridge, alongside the Android-inspired home, wallpapers, preferences and HDMI shortcuts. Camera access requires an always-available computer/server; see the [bridge guide](webos/bridge/README.md). The physical Home button still opens LG Home. Favorites now support the TV’s app catalog, adding/removing apps, native colored icons and remote-controlled ordering, with paginated grids and home rows. See the [pairing and favorites guide](docs/LG-FAVORITOS.pt-BR.md). It does not have Android feature parity. Root, persistent SSH and Home replacement remain unverified. Browser and URL features are out of scope.

Use JDK 21, Android SDK 34 and Build Tools 35.0.0:

```sh
git clone https://github.com/elizandropacheco/OwnTV.git
cd OwnTV
python3 scripts/fetch-wallpapers.py --count 15
./gradlew :app:assembleDebug
adb connect TV_IP:ADB_PORT
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n io.elizandropacheco.tv/com.conreo.couchytv.MainActivity
adb shell pm grant io.elizandropacheco.tv android.permission.READ_TV_LISTINGS
```

Choose OwnTV as Home when the firmware allows it. TCL devices may require the **OwnTV Home** accessibility service and permission to start automatically. The service filters only the Home key when supported, with stock Home activity redirection as a fallback; it does not capture screen content. Disable it to return to the original launcher.

Keep the same signing key when updating. Uninstalling or clearing app data removes local configuration and camera credentials.

## Customize

Use **Personalizar apps** for favorites, apps, categories and appearance. **Cards e jogos** edits shortcuts, artwork, order and game controls. Edit the favorite order in the personalization page; entering the dock selects its first item. News settings accept one to eight comma-separated topics and control the rotation interval. Queries are sent to Google News; articles are not scraped.

Choose local/USB artwork through Android's document picker. Select the clock for time-zone settings, and **Fundos EP** for photos, rotation and download limits.

Use the D-pad and OK throughout. Down from the dock opens more content; recommendation arrows select another page. See the Portuguese guide for each menu and option.

Original wallpaper files retain their source resolution. The Android interface may render below the panel's physical resolution; decoded images follow that usable resolution to avoid wasting memory.

## Camera and SSH access

The native camera implementation uses Dahua/compatible Intelbras RTSP paths. Other Intelbras and iCSee apps remain accessible through shortcuts. Camera credentials are stored using AES-GCM and Android Keystore, outside public launcher configuration.

Open **Configurações → Acesso SSH e SFTP**, set a username, password and port, and start the service:

```sh
ssh -p 2222 YOUR_USER@TV_IP
sftp -P 2222 YOUR_USER@TV_IP
```

Passwords use salted PBKDF2-HMAC-SHA256. The server runs as the app user, without root or ADB privileges. SFTP starts in a dedicated transfer directory; port forwarding is disabled. Stop the service in the same settings page.

## Credits and license

Developed by **[Elizandro Pacheco](https://elizandropacheco.io/)**, based on **[Couchy Launcher](https://github.com/conreo/couchy-launcher)** by Conreo. Licensed under **GPLv3**. Upstream copyright, license and attribution are retained.

Photographs retain their individual licenses and attribution. Simple Icons logo paths are CC0; trademarks belong to their owners. Optional game artwork is local customization and is not included in the public source tree. See [LICENSING.md](LICENSING.md) and the bundled asset credits.

## Installation guide

The [complete Portuguese guide](docs/INSTALLATION.pt-BR.md) covers APK downloads, developer options, USB/network/wireless ADB debugging, pairing, install/update commands, Home accessibility, configuration, troubleshooting and rollback. [Releases](https://github.com/elizandropacheco/OwnTV/releases) contain the signed local build and SHA-256 checksums. YouTube recommendations are read from the Android TV provider when shared by the app; the query budget is per publisher so another app cannot exhaust it.

## Follow and support

By [Elizandro Pacheco](https://elizandropacheco.io/). Follow on [Instagram](https://www.instagram.com/elizandropacheco/), subscribe on [YouTube](https://www.youtube.com/@ElizandroPacheco) for more technology content, and [star OwnTV on GitHub](https://github.com/elizandropacheco/OwnTV). See the [branding and rendered assets](docs/BRANDING.md).

The default identity uses Elizandro Pacheco’s EP logo. **Identidade visual e logo** offers EP, the OwnTV monogram, or a local/USB PNG/JPEG/WebP for the Home, native menus and startup animation. The installed app icon retains EP. Menu Down selects the first favorite; Favorites Up selects Cameras, then PS5, then the top menu. Recommendation captions include source/channel and duration when available, with public YouTube author lookup cached in the background.

## Source and release layout

Source is directly browsable in `app/`; no source ZIP is required. See [project structure](docs/PROJECT_STRUCTURE.md), [build/signing guide](docs/BUILDING.md), [release index](releases/README.md), and [IoT/Alexa feasibility study](docs/IOT-ALEXA.md) (Portuguese). Run `./scripts/build-release.sh` to build and validate an unsigned release into `releases/<version>/`; provide your signing environment for a signed APK. GitHub Release assets host binaries, while release checksums and metadata are versioned in Git.

Editable personal logo masters are excluded from the source and APK. Only rendered PNG derivatives are published; custom branding remains configurable. IoT/Alexa is a documented proposal, not an implemented feature.
