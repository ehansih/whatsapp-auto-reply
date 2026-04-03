<div align="center">

# 💬 WA Auto Reply

Smart WhatsApp auto-reply app with AI-powered contextual responses and custom rules.

![Android](https://img.shields.io/badge/Android-8.0%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?logo=kotlin)
![Version](https://img.shields.io/badge/version-2.0-blue)
![License](https://img.shields.io/github/license/ehansih/whatsapp-auto-reply)
![Last Commit](https://img.shields.io/github/last-commit/ehansih/whatsapp-auto-reply)

</div>

---

## What it does

Automatically replies to WhatsApp messages based on rules you define.
Supports **AI-powered smart replies** that understand the context of the message.

---

## Features

- **Custom rules** — match by contact name, number, or keyword
- **AI smart replies** — AI reads the message and writes a contextual response
- **Multiple AI providers** — Groq (free), Gemini (free), DeepSeek, OpenAI, Claude
- **Placeholders** — use `{name}` and `{message}` in any reply
- **Group chat protection** — never auto-replies to group chats
- **Duplicate guard** — won't reply twice to the same notification
- **Boot persistence** — restarts automatically after device reboot
- **Encrypted key storage** — API keys stored with AES-256 on device

---

## Quick Start

### 1. Install APK
Download the latest APK from [Releases](https://github.com/ehansih/whatsapp-auto-reply/releases).

### 2. Grant notification access
Open the app → it will prompt you to enable **Notification Access** in settings.

### 3. Disable battery optimization
Settings → Battery → WA Auto Reply → **Don't optimize**
*(Required — Android kills the service otherwise)*

### 4. Add a rule
Tap **+** → set contact, keyword, reply message → Save.

---

## Reply Modes

### Template mode (no API needed)
Use placeholders in your reply:

| Placeholder | Replaced with |
|-------------|--------------|
| `{name}` | Sender's first name |
| `{fullname}` | Sender's full name |
| `{message}` | Their exact message |

**Example:** `Hi {name}! Got your message — I'll reply soon 🙏`

---

### AI mode (smart contextual replies)

Toggle **🤖 AI Smart Reply** when creating a rule.

| Provider | Cost | Limit | Get key at |
|----------|------|-------|------------|
| **Groq — Llama 3.3** | **FREE** | 14,400/day | [groq.com](https://groq.com) |
| **Gemini 1.5 Flash** | **FREE** | 1,500/day | [aistudio.google.com](https://aistudio.google.com) |
| DeepSeek V3 | ~free credits | Unlimited after | [platform.deepseek.com](https://platform.deepseek.com) |
| OpenAI GPT-4o Mini | Paid | — | platform.openai.com |
| Claude Haiku | Paid | — | console.anthropic.com |

**AI example** — someone messages "I need urgent help":
> *"Hi Rahul! I can see you need help — Harsh will get back to you very soon. If it's urgent: iCall 9152987821 🙏"*

---

## Rule Examples

| Contact | Keyword | Reply |
|---------|---------|-------|
| `*` | `*` | `Hi {name}! I'm busy, will reply soon` |
| `Mom` | `*` | `Hi Mom! I'll call you back shortly 🙏` |
| `*` | `urgent` | AI mode ON → smart reply |
| `*` | `help` | AI mode ON → empathetic reply with crisis resources |
| `+919876543210` | `*` | `Hi! Harsh is unavailable right now` |

---

## Security

- API keys encrypted with **AES-256-GCM** via Android Keystore
- Keys stored on device only — never uploaded or logged
- No analytics, no tracking
- Internet used only for AI API calls
- ProGuard enabled in release build (code obfuscation)
- Source code fully open — audit it yourself

---

## Permissions

| Permission | Why |
|-----------|-----|
| Notification Access | Read WhatsApp notifications to trigger rules |
| Internet | AI reply API calls only |
| Receive Boot | Restart service after reboot |
| Battery Optimization | Keep service alive in background |

---

## Changelog

### v2.0
- AI smart replies: Groq (free), Gemini (free), DeepSeek, OpenAI, Claude
- `{name}`, `{fullname}`, `{message}` placeholders in all replies
- **Fixed:** WearableExtender reply action (was the main bug — replies not sending)
- **Fixed:** Group chat detection (FLAG_GROUP_SUMMARY + text format check)
- Added: BootReceiver — persists after device reboot
- Added: AES-256 encrypted API key storage
- Added: Duplicate reply guard (30s cooldown per contact)
- Delay increased 500ms → 1500ms for reliable reply delivery
- Updated all dependencies to latest versions
- ProGuard minification enabled in release

### v1.0
- Basic rule engine — contact + keyword matching
- Enable/disable individual rules
- Edit and delete rules

---

## Author

**Harsh Vardhan Singh Chauhan** — [github.com/ehansih](https://github.com/ehansih)
