# LastWave Native: Runtime Network Traffic & Endpoint Audit

**Audit Date:** 2026-11-12  
**Target Device:** Motorola Moto G45 5G  
**PID Monitored:** `17742` (`com.lastwave.app`)  
**Diagnostic Method:** `/proc/{pid}/net/tcp6`, OkHttp logging traces, DNS endpoint resolution

---

## 1. Observed Destination Hosts & Traffic Profiles

During real runtime operation on the Motorola Moto G45 5G, the application established connections exclusively to legitimate media and content endpoints over TLS 1.3 (port 443):

| Observed Remote Host / CDN | Protocol | Purpose & Payload | Observed Behavior |
| :--- | :--- | :--- | :--- |
| `music.youtube.com` | HTTPS (TLS 1.3) | InnerTube API (`/youtubei/v1/search`, `/browse`, `/player`) | Low payload size (<50 KB JSON); compressed with gzip/brotli |
| `*.googlevideo.com` (e.g. `rr7---sn-ci5gup-pmxd.googlevideo.com`) | HTTPS (TLS 1.3) | High-speed audio chunk streaming (Opus itag 251) | Sequential byte-range GET requests (~1MB chunks); sustained throughput |
| `suggestqueries.google.com` | HTTPS (TLS 1.3) | Real-time search suggestions | Fires debounced requests (300ms) while user types in search bar |
| `lrclib.net` | HTTPS (TLS 1.3) | Synchronized `.lrc` and plain lyrics retrieval | Single lightweight JSON request per track (<15 KB) |
| `lh3.googleusercontent.com`, `i.ytimg.com` | HTTPS (TLS 1.3) | Album artwork and artist profile imagery | Cached in OkHttp disk cache; subsequent views load instantly from disk |

---

## 2. Zero-Telemetry & Zero-Ad Verification

- **Ad Networks:** 0 connections to Google AdMob, AppLovin, UnityAds, Vungle, ironSource, or Facebook Audience Network.
- **Analytics / Tracking:** 0 connections to Firebase Analytics, Google Analytics, Adjust, AppsFlyer, Sentry, or Crashlytics.
- **Cleartext Traffic:** Strictly blocked by Android Network Security Config (`usesCleartextTraffic="false"`). All outbound network connections enforce HTTPS with modern TLS cipher suites.
