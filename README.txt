WinGo Floating Assistant - OCR-enabled Android Studio starter project

Included:
- Movable floating panel and local 30-second clock.
- Android MediaProjection consent flow for user-authorized screen capture.
- On-device Google ML Kit text recognition (OCR).
- Latest recognized visible screen text is shown in the floating panel.

Limitations:
- OCR reads visible text only. It does not access a hidden/private game API, bypass site protections, place bets, or guarantee predictions.
- It currently displays recognized text, not verified WinGo result rows. Screen layout/overlays may reduce OCR accuracy.
- Android displays a system screen-sharing/capture indicator. Stop reading from the app to end capture.

Build:
1. Open this folder in Android Studio.
2. Allow Gradle sync and install SDK components if prompted.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Install the debug APK.
5. Grant display-over-apps permission, start the floating panel, then tap Start screen reading.
6. Approve Android's screen-capture prompt and switch to the WinGo page.

Requires internet during the first Gradle dependency download. OCR runs on-device after dependency setup.
