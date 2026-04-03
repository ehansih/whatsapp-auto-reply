<div align="center">

# 💬 WhatsApp Auto Reply

WhatsApp auto-reply app with custom rules — Android APK.

![Android](https://img.shields.io/badge/Android-green?logo=android) ![Kotlin](https://img.shields.io/badge/Kotlin-purple?logo=kotlin) ![Last Commit](https://img.shields.io/github/last-commit/ehansih/whatsapp-auto-reply) ![Stars](https://img.shields.io/github/stars/ehansih/whatsapp-auto-reply?style=social)

</div>

---

# WhatsApp Auto Reply

Android app for automated WhatsApp replies with intelligent, customisable response rules.

## Features
- Auto-reply to WhatsApp messages based on custom rules
- Keyword-based trigger matching
- Schedule-based replies (e.g. out-of-office hours)
- Contact-specific reply rules
- Reply history log
- Notification listener service (no root required)

## Tech Stack
- **Language**: Kotlin
- **Min SDK**: Android 8.0 (API 26)
- **Architecture**: MVVM + Room
- **UI**: Jetpack Compose

## Permissions Required
- `BIND_NOTIFICATION_LISTENER_SERVICE` — reads WhatsApp notifications
- No SMS permissions needed — works through notification access only

## Getting Started

1. Clone the repo
2. Open in Android Studio
3. Build and install on your device
4. Grant Notification Access in Settings
5. Configure your reply rules in the app

## Privacy
- All rules and reply data are stored locally on device
- No data is sent to any server
- No analytics or tracking

## Author
**Harsh Vardhan Singh Chauhan** — [github.com/ehansih](https://github.com/ehansih)

## License
MIT
