# Biovarmenne 🔐

> Biometric authentication for Finnish Mobiilivarmenne — built in one evening 
> while Finnish telcos gave up 😄

Biovarmenne replaces the manual PIN code entry in Mobiilivarmenne with 
fingerprint or face authentication. What Finnish mobile operators DNA and Elisa 
promised but never delivered — available now as free open source software.

## Background

Mobiilivarmenne is a strong authentication method used widely in Finland for 
identifying yourself in online services (Suomi.fi, banking, etc.). 
It works by sending a PIN request to your phone via SIM Toolkit.

In early 2025, Finnish operators announced they would develop a biometric 
Mobiilivarmenne app. By late 2025, DNA had cancelled development completely 
and Elisa's development was severely delayed. Biovarmenne was built as a 
third-party solution to fill this gap.

## How It Works

1. Save your Mobiilivarmenne PIN code securely in Biovarmenne
2. Enable Biovarmenne in Android Accessibility Settings
3. When Mobiilivarmenne requests your PIN, Biovarmenne detects it automatically
4. Authenticate with your fingerprint or face recognition
5. PIN is filled and confirmed automatically — no typing needed!

## Features

- 🔐 Fingerprint and face authentication
- 🔒 PIN encrypted with Android Keystore (AES-256)
- 🇫🇮 Works with all Finnish operators (Telia, Elisa, DNA)
- 🌍 Available in Finnish, Swedish and English
- 📱 Works when device is locked
- ⚡ Synced with Mobiilivarmenne's 30 second timeout
- 🚫 Cancel button properly rejects spam authentication requests
- 🔋 Battery optimization guidance built in

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
5. All status indicators should be green ✓

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
| Request Ignore Battery Optimizations | Keep service running reliably |

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
