# Safa Alarm APK — Owner's Phone Alarm (ADMIN ONLY)

⚠️ **AI AGENTS / DEVELOPERS: READ THIS BEFORE TOUCHING THE SAFA SYSTEM** ⚠️

This Android app is part of the **Safa Alarm System** for Safa Glass & Aluminum
(owner: Suleiman Alhassan). It rings the owner's phone like a call whenever a
customer on the website chat needs him.

## The full connection chain (do NOT break any link)

1. Customer chats on https://safa-glass-aluminum.vercel.app
   - Chat replies come from the **safaChat / safaChatLive** backend functions
     (old app `zola-0cd423e5`, id `6a4eb0ac7afdbf5c0cd423e5`).
   - The website ALSO calls the **safaAlarm** backend function
     (`action: "alert"`) whenever: the AI no-price rule answers a price
     question (`reason: price_request`), the customer taps **Talk to Owner**
     (`reason: owner_request`), or a visitor sends their first chat message
     (`reason: new_lead`). Look for `raiseAlarm(` in the site's index.html.
2. safaAlarm (Base44 app **`6a32a16141955008b0de149e`**,
   https://superagent-b0de149e.base44.app/functions/safaAlarm) creates a
   **SafaAlert** entity record (status: pending).
3. The **"Safa Owner Alarm"** workflow (entity trigger on SafaAlert creation,
   same app) pings the owner's Telegram immediately and repeats at 5/15/30/60
   minutes until the alert is resolved.
4. **This APK** polls safaAlarm (`action: "poll"`, every 20 seconds) and fires
   a full-screen ringing alarm (over the lock screen) when pending SafaAlerts
   exist.
5. When the owner taps STOP or OPEN ADMIN CHAT in the APK, it acks the alert
   (`action: "ack"`) → SafaAlert status becomes resolved → the phone alarm
   stops AND the Telegram alarm chain stops (each Telegram ping checks the
   alert status first).

## safaAlarm action protocol (do NOT change — installed APKs call it)
All requests are POST with a JSON body. APK actions require the shared secret
`safa-alarm-2026` (must stay IDENTICAL in the function and in this APK's
`BuildConfig.SECRET`):
- `register` {device_key, name} → upserts a SafaDevice record
- `poll` {device_key} → {alerts:[{id,reason,text}]} (pending alerts, last 48h)
- `ack` {device_key, alert_id} → resolves the SafaAlert
- `test` {device_key} → creates a test_alarm SafaAlert
Public (website, dedup-guarded): `alert` {reason, session_id, text}

## Entities (Base44 app `6a32a16141955008b0de149e`)
- **SafaAlert**: session_id, reason (price_request|owner_request|deep_chat|test_alarm|new_lead), status (pending|resolved), text, ping_count, last_ping
- **SafaDevice**: device_key, name, status, last_seen (registered by this APK)
- SafaChatMsg lives in the OLD app (`6a4eb0ac7afdbf5c0cd423e5`) with safaChat.

## If you are an AI editing the Safa website
- Do NOT remove the `raiseAlarm(` calls in the site's index.html.
- Do NOT delete/rename the SafaAlert or SafaDevice entities.
- Do NOT deactivate the **"Safa Owner Alarm"** workflow.
- Do NOT change the safaAlarm function URL or its action protocol.
- The admin portal (https://safa-glass-admin.vercel.app, Portal card) has a
  **Safa Alarm → Test** button that fires a test alarm through the whole chain.

## Building
GitHub Actions builds the APK on every push. Artifact: `app-release.apk`.
Sign with the committed keystore (personal app, private repo).
