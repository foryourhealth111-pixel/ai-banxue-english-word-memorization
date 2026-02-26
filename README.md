# Word Coach

Android floating-window assistant for vocabulary memorization.

## Modules

- `mobile/`: Android app (overlay + capture + OCR + result rendering)
- `server/`: secure backend proxy to AI provider
- `docs/`: design, plans, release notes

## Security Baseline

- Never put provider API keys inside the mobile app.
- Mobile calls backend proxy (`/v1/coach`) with client token.
- Backend stores provider API key in environment variables.

## Quick Start

### Server

```powershell
cd server
npm install
npm run dev
```

### Mobile

Open `mobile/` in Android Studio and run the `app` module.

## Public Repo Safety

- Do not commit `server/.env` (already ignored).
- Keep real provider keys and production client tokens in environment variables only.
- Mobile build now reads optional Gradle properties instead of hardcoding tokens:
  - `WORDCOACH_API_BASE_URL`
  - `WORDCOACH_CLIENT_TOKEN`

Example (local only, do not commit):

```properties
# mobile/gradle.properties
WORDCOACH_API_BASE_URL=http://10.0.2.2:8080
WORDCOACH_CLIENT_TOKEN=your-strong-client-token
```

You can also start from `mobile/gradle.properties.example`.

## Android Compatibility Notes

- UI strings are localized with resources (`values/` + `values-zh-rCN/`) for multi-language devices.
- Overlay and result card sizes use screen-based bounds to reduce overflow on small screens.
- Direct provider mode supports standalone device usage (without USB/local server) when Base URL + model + API key are configured in-app.

## Status

MVP skeleton implemented with:

- backend auth + rate limit + prompt contract tests
- Android overlay service skeleton + OCR selection core + copy action
