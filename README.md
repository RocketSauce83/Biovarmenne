# Biovarmenne 🔐

Biovarmenne replaces the manual PIN code entry in Mobiilivarmenne with 
fingerprint or face authentication. What Finnish mobile operators DNA, Elisa and Telia
promised but never delivered — available now as free open source software.

## Background

Mobiilivarmenne is a strong authentication method used widely in Finland for 
identifying yourself in online services (Suomi.fi, banking, etc.). 
It works by sending a PIN request to your phone via SIM Toolkit.

In 2019, DNA, Elisa and Telia signed an agreement to bring biometric 
authentication to Mobiilivarmenne. The feature was promised for 2020.

Nothing happened.

In early 2025, Finnish operators announced once again they would develop 
biometric support to Mobiilivarmenne, this time with a dedicated app. 
By late 2025, DNA had cancelled development completely and Elisa's 
development was severely delayed. Telia was never part of the app 
development at any stage.

Biovarmenne was built as a third-party solution to fill this gap — 
developed in one evening while Finnish telcos gave up after 6 years 
of promises. 😄

## How It Works

1. Save your Mobiilivarmenne PIN code securely in Biovarmenne
2. Enable Biovarmenne in Android Accessibility Settings
3. When Mobiilivarmenne requests your PIN, Biovarmenne detects it automatically
4. Authenticate with your fingerprint or face recognition
5. PIN is filled and confirmed automatically — no typing needed!

## Features

- 🔐 Fingerprint and face authentication
- 🔒 PIN encrypted with Android Keystore (AES-256)
- 🇫🇮 Works with all major Finnish operators (Telia, Elisa, DNA)
- 🌍 Available in Finnish, Swedish and English
- 📱 Works when device is locked
- ⚡ Synced with Mobiilivarmenne's 30 second timeout
- 🚫 Cancel button properly rejects spam authentication requests
- 🔋 Battery optimization guidance built in
- 🔔 Notifications if wrong PIN or service stops unexpectedly
- ⚙️ Toggle to quickly enable/disable without touching system settings
- 🔄 Automatic check after device restart
- 📲 Xiaomi/MIUI support with dedicated setup guidance

## Requirements

- Android 11 (API 30) or newer
- Active Finnish mobile subscription with Mobiilivarmenne enabled
- Fingerprint or face recognition set up on your device

## Installation

### Option 1 — Google Play Store (Recommended)
[Download from Google Play](#) ← link coming soon

### Option 2 — Build from source
```bash
git clone https://github.com/rocketsauce83/Biovarmenne.git
```
Open in Android Studio, build and install.

## Setup

1. Open Biovarmenne
2. Enter and save your Mobiilivarmenne PIN code
3. Tap "Open Accessibility Settings" and enable Biovarmenne
4. Tap "Disable Battery Optimization" and allow unrestricted battery usage
5. Allow notifications when prompted
6. **Xiaomi/MIUI users:** Also enable "Display pop-up windows while 
   running in background" and "Autostart" via the buttons in the app
7. All status indicators should be checked ✓

## Security

Your PIN code never leaves your device. It is stored using Android Keystore 
backed AES-256-GCM encryption. The Accessibility Service only monitors the 
Mobiilivarmenne SIM Toolkit window (`com.android.stk`) and nothing else.

Biovarmenne is open source — you can verify every line of code yourself.

## Permissions

| Permission | Reason |
|---|---|
| Accessibility Service | Detect Mobiilivarmenne PIN requests and auto-fill |
| Biometric | Fingerprint/face authentication |
| Request Ignore Battery Optimizations | Keep service running reliably in background |
| Post Notifications | Wrong PIN alerts and service status notifications |
| Receive Boot Completed | Check service status after device restart |
| System Alert Window | Required on Xiaomi/MIUI devices for biometric prompt |

## Privacy Policy

[View Privacy Policy](PRIVACY_POLICY.md)

No data is collected or transmitted. Everything stays on your device.

## Contributing

Pull requests are welcome! If you find a bug or have a feature request, 
please open an issue.

## Disclaimer

This app is not affiliated with or endorsed by Telia, Elisa, DNA, 
or any Finnish mobile operator. Mobiilivarmenne is a trademark of 
the respective operators.

## License

MIT License — see [LICENSE](LICENSE) for details.

---

*Built with ❤️ in Finland 🇫🇮*
