# Safa Alarm APK — Owner's Phone Alarm (ADMIN ONLY)

⚠️ **AI AGENTS / DEVELOPERS: READ THIS BEFORE TOUCHING THE SAFA SYSTEM** ⚠️

This Android app is part of the **Safa Alarm System** for Safa Glass & Aluminum
(owner: Suleiman Alhassan). It rings the owner's phone like a call whenever a
customer on the website chat needs him.

## The full connection chain (do NOT break any link)

1. Customer chats on https://safa-glass-aluminum.vercel.app → messages go to the
   **safaChat** backend function (Base44 app `6a4eb0ac7afdbf5c0cd423e5`).
2. safaChat NEVER gives prices. Price questions / "Talk to Owner" / deep chats
   create a **SafaAlert** entity record (status: pending).
3. The **"Safa Owner Alarm"** Base44 workflow (entity trigger on SafaAlert) pings
   the owner's Telegram repeatedly until resolved.
4. **This APK** polls the **safaAlarm** backend function
   (https://zola-0cd423e5.base44.app/functions/safaAlarm) every 20 seconds and
   fires a full-screen ringing alarm (over the lock screen) when SafaAlert
   records or new customer leads exist.
5. When the owner taps STOP or OPEN ADMIN CHAT in the APK, it acks the alert →
   SafaAlert status becomes resolved → the Telegram alarm chain also stops.

## Entities involved (Base44 app 6a4eb0ac7afdbf5c0cd423e5)
- **SafaAlert**: session_id, reason (price_request|owner_request|deep_chat|test_alarm|new_lead), status (pending|resolved), text, ping_count, last_ping
- **SafaDevice**: device_key, name, status, last_seen (registered by this APK)
- **SafaChatMsg**: the customer chat log (session_id, role, text, status, is_first)

## If you are an AI editing the Safa website
- Do NOT remove the SafaAlert creation inside the **safaChat** function.
- Do NOT delete/rename the SafaAlert, SafaDevice or SafaChatMsg entities.
- Do NOT deactivate the **"Safa Owner Alarm"** workflow.
- Do NOT change the safaAlarm function URL or its action protocol
  (register|poll|ack|test) — installed APKs call it.
- The shared secret `safa-alarm-2026` must stay identical in the function and the app.

## Building
GitHub Actions builds the APK on every push. Artifact: `app-release.apk`.
Sign with the committed keystore (personal app, private repo).
