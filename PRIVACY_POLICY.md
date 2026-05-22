# Privacy Policy for Biovarmenne

**Last updated: May 2026**

## Introduction
Biovarmenne is an open source Android application that provides biometric authentication for Mobiilivarmenne. This privacy policy explains how the app handles your data.

## Data Collection
Biovarmenne does not collect, store, or transmit any personal data to external servers. All data stays on your device.

## PIN Code Storage
Your Mobiilivarmenne PIN code is stored exclusively on your device using Android Keystore encryption (AES-256). The PIN is never transmitted over the network, never shared with third parties, and never leaves your device.

## Accessibility Service
Biovarmenne uses Android's Accessibility Service to detect when Mobiilivarmenne requests PIN authentication. The service only monitors the Mobiilivarmenne (`com.android.stk`) application window. It does not read, record, or transmit content from any other application.

## Biometric Data
Biovarmenne uses Android's built-in BiometricPrompt API for fingerprint and face authentication. Biometric data is processed entirely by the Android operating system and is never accessible to Biovarmenne.

## Permissions
- **Accessibility Service:** Used solely to detect Mobiilivarmenne PIN requests
- **Biometric:** Used for fingerprint/face authentication
- **Request Ignore Battery Optimizations:** Ensures the accessibility service runs reliably in the background

## Open Source
Biovarmenne is open source software. The complete source code is available at:
https://github.com/rocketsauce83/Biovarmenne

## Contact
If you have questions about this privacy policy, please open an issue on GitHub.

## Changes
Any changes to this privacy policy will be reflected in the GitHub repository and this document will be updated accordingly.
