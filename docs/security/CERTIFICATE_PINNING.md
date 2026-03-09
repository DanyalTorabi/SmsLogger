# Certificate Pinning Guide

This document explains how to configure certificate pinning for local development,
staging, and production environments.

## Overview

Certificate pinning is implemented via:
- **OkHttp `CertificatePinner`** – enforced at the HTTP client level (`SmsApiClient.kt`)
- **Android Network Security Config** – enforced at the OS level (`network_security_config.xml`)

Pinning is **disabled in debug builds** (empty `BuildConfig` fields) so local development
works without any certificate setup.

---

## Files involved

| File | Purpose |
|------|---------|
| `app/build.gradle` / `app/build.gradle.kts` | `CERT_HOSTNAME`, `CERT_PIN_PRIMARY`, `CERT_PIN_BACKUP` BuildConfig fields |
| `app/src/main/res/xml/network_security_config.xml` | OS-level pin-set |
| `app/src/main/java/.../api/SmsApiClient.kt` | OkHttp CertificatePinner wiring |

---

## Local Development (debug builds)

Pinning is **automatically disabled** when `BuildConfig.CERT_HOSTNAME` is blank.  
No action needed — the app will connect to your local server over plain HTTP.

Default local server URL: `http://10.0.2.2:8080` (Android emulator → host machine)

If you want to test pinning locally against a self-signed HTTPS server:

1. Start your local server with TLS (e.g. via `mkcert` or a self-signed cert).
2. Extract the pin from the local server:
   ```bash
   openssl s_client -connect localhost:8443 -servername localhost </dev/null 2>/dev/null \
     | openssl x509 -pubkey -noout \
     | openssl pkey -pubin -outform DER \
     | openssl dgst -sha256 -binary \
     | openssl base64
   ```
3. In `app/build.gradle`, temporarily update the **debug** block:
   ```groovy
   debug {
       buildConfigField "String", "CERT_HOSTNAME", '"localhost"'
       buildConfigField "String", "CERT_PIN_PRIMARY", '"sha256/<output-from-step-2>"'
       buildConfigField "String", "CERT_PIN_BACKUP", '""'
   }
   ```
   > ⚠️ Do **not** commit these local overrides. Revert before pushing.

---

## Staging / Production (release builds)

### Source of truth

All production pin values come from the **server pin bundle**:

```
DanyalTorabi/sms-syncer-server
└── docs/security/android-pin-bundle.json
```

Server handoff issue: [DanyalTorabi/sms-syncer-server#130](https://github.com/DanyalTorabi/sms-syncer-server/issues/130)  
Rotation runbook: `docs/security/android-certificate-pinning-runbook.md` in the server repo

### Mapping from bundle → Android files

| `android-pin-bundle.json` field | Android target |
|---------------------------------|---------------|
| `hosts[].hostname` | `CERT_HOSTNAME` in both `build.gradle` files + `<domain>` in `network_security_config.xml` |
| `hosts[].pins.current` | `CERT_PIN_PRIMARY` (include the `sha256/` prefix) |
| `hosts[].pins.backup` | `CERT_PIN_BACKUP` (include the `sha256/` prefix) |
| `hosts[].validity.notAfter` | `expiration` attribute in `<pin-set>` in `network_security_config.xml` |

### Step-by-step

1. Clone / pull the server repo and open `docs/security/android-pin-bundle.json`.
2. Copy the values for your target environment (staging or production).
3. Update **both** `app/build.gradle` and `app/build.gradle.kts` release block:
   ```groovy
   buildConfigField "String", "CERT_HOSTNAME",    '"api.yourdomain.com"'
   buildConfigField "String", "CERT_PIN_PRIMARY", '"sha256/<current-pin>"'
   buildConfigField "String", "CERT_PIN_BACKUP",  '"sha256/<backup-pin>"'
   ```
4. Update `app/src/main/res/xml/network_security_config.xml`:
   ```xml
   <domain includeSubdomains="true">api.yourdomain.com</domain>
   <pin-set expiration="YYYY-MM-DD">
       <pin digest="SHA-256"><current-pin-without-sha256-prefix></pin>
       <pin digest="SHA-256"><backup-pin-without-sha256-prefix></pin>
   </pin-set>
   ```
   > Note: `network_security_config.xml` uses the raw base64 value **without** the `sha256/` prefix,
   > while `BuildConfig` fields include it.
5. Open a PR referencing the server handoff issue.

---

## Certificate rotation

Follow the runbook in `DanyalTorabi/sms-syncer-server`:
```
docs/security/android-certificate-pinning-runbook.md
```

Key rules:
- Give **14 days notice** before rotating (notify Android maintainers).
- Always keep a **backup pin** active before deploying a new certificate.
- Never remove old pins in the same release as the key rotation.
- Update `expiration` date in `network_security_config.xml` to match the new cert validity.

---

## Verifying a live endpoint

```bash
openssl s_client -connect api.yourdomain.com:443 -servername api.yourdomain.com </dev/null 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform DER \
  | openssl dgst -sha256 -binary \
  | openssl base64
```

The output should match `hosts[].pins.current` in the server pin bundle.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| `CertificatePinningException` in logcat | Pin mismatch or cert rotated | Re-extract pin from live endpoint and compare with bundle |
| App connects fine in debug but fails in release | Release pins not updated | Follow the staging/production steps above |
| `SSLPeerUnverifiedException` | Same as pin mismatch | Same fix |
| Sync halted notification shown | `SmsSyncService` caught a pinning failure | Check logcat for `SmsApiClient` tag |

