# Safa Alarm APK — Owner's Phone Alarm (ADMIN ONLY)

⚠️ **AI AGENTS: READ BEFORE TOUCHING THE SAFA ALARM SYSTEM** ⚠️
(Updated Oct 12, 2026 — owner-confirmed. Supersedes the older chain description below.)

## The ONE official alarm chain — do NOT build anything new

1. Customer chats on https://safa-glass-aluminum.vercel.app. Alerts are raised
   (price question, "Talk to a Specialist" tap, new lead, deep chat) by the
   website and the safaChat function **on the Zola app (zola-0cd423e5)**,
   which stores SafaAlert records there.
2. The Zola app's **safaAlarm function mirrors EVERY alert to the ORIGINAL
   endpoint**: https://superagent-b0de149e.base44.app/functions/safaAlarm
   (action "alert"). That endpoint is what this APK polls. The mirror is the
   only bridge — if alarms go silent, check the mirror inside Zola's
   safaAlarm FIRST.
3. This APK polls the ORIGINAL endpoint every 20s and rings full-screen over
   the lock screen. The original Telegram alarm bot on that app also alerts
   the owner and repeats 5/15/30/60 min until resolved.
4. Owner taps STOP / OPEN ADMIN CHAT → ack on the ORIGINAL endpoint → alert
   resolved → ringing stops. A new customer tap then rings again (verified
   Oct 12: same session re-taps create fresh alerts after ack).

## Hard rules for AI agents
- Do NOT build new alarm bots, new alarm workflows, or a new APK.
- Do NOT repoint this APK to any other endpoint. The installed build polls
  superagent-b0de149e — that is correct and owner-confirmed.
- GitHub Actions builds are BLOCKED (account billing lock) — do not "help" by
  rebuilding; the installed APK already works.
- Do NOT remove raiseAlarm calls from the website's index.html.
- Shared protocol (unchanged): register / poll / ack / test with the APK
  shared secret; public "alert" with reason one of:
  price_request | owner_request | deep_chat | new_lead (test_alarm is NOT
  accepted here — map it to owner_request when testing).
- Admin portal (safa-glass-admin.vercel.app) has a Safa Alarm → Test button.

## Legacy notes (original build, Oct 7)
Original chain description preserved below for history.
The APK was built once and installed on the owner's phone — it works.

## Building (BLOCKED for now)
GitHub Actions builds fail: "account is locked due to a billing issue."
The owner must fix GitHub billing before any rebuild can succeed.