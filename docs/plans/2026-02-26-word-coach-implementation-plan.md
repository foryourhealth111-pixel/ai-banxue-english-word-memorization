# Word Coach Floating Overlay App Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build an Android floating-window assistant that captures the current vocabulary-learning screen, identifies the target word, requests AI memory coaching, and shows copyable results in an overlay within 3 seconds (P50).

**Architecture:** Use a two-tier architecture: Android client (overlay, capture, OCR, UI) and a secure backend proxy (API key vault + prompt shaping + rate limiting). The Android app performs local OCR first to reduce cost and protect privacy, then sends only selected word by default. The backend calls the Gemini-compatible provider and returns a constrained structured response.

**Tech Stack:** Kotlin, Jetpack Compose, Coroutines, Retrofit/OkHttp, ML Kit OCR (Latin), Room, DataStore, Hilt, JUnit/MockK, Android Instrumentation Tests, Node.js + Fastify backend, Zod validation, Vitest, Docker.

---

## Prerequisites

- JDK 17
- Android Studio Hedgehog or newer
- Android SDK 34
- Node.js 20+
- Docker (optional for deployment)
- A new API key generated after rotating the leaked key

## Target Repo Layout

```text
word-coach/
  mobile/
    app/...
  server/
    src/...
  docs/
    plans/
```

### Task 1: Initialize Repository Skeleton

**Files:**
- Create: `README.md`
- Create: `mobile/`
- Create: `server/`
- Create: `.gitignore`
- Create: `docs/adr/0001-system-architecture.md`
- Test: `README.md` checklist section

**Step 1: Create baseline folder structure**

```powershell
New-Item -ItemType Directory -Force mobile, server, docs\adr | Out-Null
```

**Step 2: Add root README and architecture ADR**

```markdown
# Word Coach

Android overlay + secure AI proxy for vocabulary memorization.
```

**Step 3: Add `.gitignore`**

```gitignore
.idea/
*.iml
local.properties
mobile/.gradle/
mobile/build/
server/node_modules/
server/dist/
.env
```

**Step 4: Validate structure**

Run: `Get-ChildItem -Recurse`  
Expected: `mobile`, `server`, `docs\adr` exist.

**Step 5: Commit**

```bash
git add README.md .gitignore docs/adr/0001-system-architecture.md
git commit -m "chore: initialize repository skeleton and architecture adr"
```

### Task 2: Bootstrap Android App Project

**Files:**
- Create: `mobile/settings.gradle.kts`
- Create: `mobile/build.gradle.kts`
- Create: `mobile/app/build.gradle.kts`
- Create: `mobile/app/src/main/AndroidManifest.xml`
- Create: `mobile/app/src/main/java/com/wordcoach/MainActivity.kt`
- Test: `mobile/app/src/test/java/com/wordcoach/SanityTest.kt`

**Step 1: Create Gradle settings**

```kotlin
rootProject.name = "word-coach-mobile"
include(":app")
```

**Step 2: Configure Android plugin and dependencies**

```kotlin
plugins {
  id("com.android.application") version "8.5.2" apply false
  id("org.jetbrains.kotlin.android") version "2.0.20" apply false
}
```

**Step 3: Add minimal app manifest + launcher activity**

```xml
<application android:label="WordCoach">
  <activity android:name=".MainActivity" android:exported="true">
    <intent-filter>
      <action android:name="android.intent.action.MAIN" />
      <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
  </activity>
</application>
```

**Step 4: Run unit test task**

Run: `cd mobile; .\gradlew.bat testDebugUnitTest`  
Expected: BUILD SUCCESSFUL.

**Step 5: Commit**

```bash
git add mobile
git commit -m "chore(android): bootstrap kotlin compose app"
```

### Task 3: Build Secure Backend Proxy Service

**Files:**
- Create: `server/package.json`
- Create: `server/src/index.ts`
- Create: `server/src/config.ts`
- Create: `server/src/routes/coach.ts`
- Create: `server/src/lib/geminiClient.ts`
- Create: `server/.env.example`
- Test: `server/test/health.test.ts`

**Step 1: Initialize Node project**

Run: `cd server; npm init -y`

**Step 2: Add runtime dependencies**

Run: `npm i fastify zod dotenv undici pino`

**Step 3: Implement config with strict env validation**

```ts
import { z } from "zod";
export const Env = z.object({
  PORT: z.string().default("8080"),
  GEMINI_BASE_URL: z.string().url(),
  GEMINI_MODEL: z.string().min(1),
  GEMINI_API_KEY: z.string().min(20),
}).parse(process.env);
```

**Step 4: Add `/health` and `/v1/coach` routes**

```ts
fastify.get("/health", async () => ({ ok: true }));
fastify.post("/v1/coach", coachHandler);
```

**Step 5: Commit**

```bash
git add server
git commit -m "feat(server): create secure proxy with health and coach endpoints"
```

### Task 4: Add Backend Validation, Prompt Template, and Tests

**Files:**
- Modify: `server/src/routes/coach.ts`
- Create: `server/src/lib/promptBuilder.ts`
- Create: `server/test/coach.validation.test.ts`
- Create: `server/test/coach.prompt.test.ts`
- Test: `server/test/*.test.ts`

**Step 1: Define request/response schemas**

```ts
const CoachReq = z.object({
  word: z.string().regex(/^[A-Za-z-]{2,25}$/),
  locale: z.string().default("zh-CN"),
});
```

**Step 2: Build deterministic prompt template**

```ts
export function buildPrompt(word: string) {
  return `你是英语单词记忆教练...目标词:${word}...220字以内`;
}
```

**Step 3: Add safety checks**

- Reject empty words and malformed payloads.
- Remove client-supplied system prompt overrides.

**Step 4: Run tests**

Run: `cd server; npm test`  
Expected: all tests pass.

**Step 5: Commit**

```bash
git add server/src server/test
git commit -m "test(server): add request validation and prompt contract tests"
```

### Task 5: Add Backend Rate Limiting and Auth

**Files:**
- Modify: `server/src/index.ts`
- Create: `server/src/plugins/rateLimit.ts`
- Create: `server/src/plugins/clientAuth.ts`
- Test: `server/test/security.rate-limit.test.ts`

**Step 1: Add app-level rate limit**

```ts
// e.g. 30 requests / minute / client-id
```

**Step 2: Add client token verification**

- Require `X-Client-Token`.
- Verify against HMAC or allow-list for MVP.

**Step 3: Mask secrets in logs**

- Never log API key or full payload with screenshots.

**Step 4: Run security tests**

Run: `cd server; npm test -- security.rate-limit.test.ts`  
Expected: 401 on missing token, 429 when burst exceeded.

**Step 5: Commit**

```bash
git add server/src server/test
git commit -m "feat(server): add client auth and rate limiting"
```

### Task 6: Android Core Network and Data Models

**Files:**
- Create: `mobile/app/src/main/java/com/wordcoach/core/network/ApiService.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/core/network/NetworkModule.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/feature/coach/data/CoachDtos.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/feature/coach/data/CoachRepository.kt`
- Test: `mobile/app/src/test/java/com/wordcoach/feature/coach/CoachRepositoryTest.kt`

**Step 1: Define DTOs**

```kotlin
data class CoachRequest(val word: String, val locale: String = "zh-CN")
data class CoachResponse(val word: String, val explanation: String)
```

**Step 2: Add Retrofit interface**

```kotlin
@POST("/v1/coach")
suspend fun coach(@Body req: CoachRequest): CoachResponse
```

**Step 3: Implement repository with timeout + retry(1)**

- Timeout 8s.
- One retry for transient 5xx.

**Step 4: Write repository unit tests**

- success parsing
- network failure mapping

**Step 5: Commit**

```bash
git add mobile/app/src/main
git add mobile/app/src/test
git commit -m "feat(android): add coach network layer and repository"
```

### Task 7: Android Permissions and Onboarding

**Files:**
- Modify: `mobile/app/src/main/AndroidManifest.xml`
- Create: `mobile/app/src/main/java/com/wordcoach/feature/onboarding/PermissionViewModel.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/feature/onboarding/PermissionScreen.kt`
- Modify: `mobile/app/src/main/java/com/wordcoach/MainActivity.kt`
- Test: `mobile/app/src/test/java/com/wordcoach/feature/onboarding/PermissionViewModelTest.kt`

**Step 1: Add required permissions**

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**Step 2: Build onboarding state machine**

- states: `NeedOverlay`, `NeedProjection`, `NeedNotification`, `Ready`.

**Step 3: Build permission UI**

- one-tap jump to settings / projection flow.

**Step 4: Validate with unit tests**

Run: `cd mobile; .\gradlew.bat testDebugUnitTest`  
Expected: onboarding tests pass.

**Step 5: Commit**

```bash
git add mobile/app/src/main mobile/app/src/test
git commit -m "feat(android): implement permission onboarding flow"
```

### Task 8: Foreground Service and Floating Bubble

**Files:**
- Create: `mobile/app/src/main/java/com/wordcoach/feature/overlay/FloatingCoachService.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/feature/overlay/FloatingBubbleView.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/feature/overlay/OverlayNotification.kt`
- Modify: `mobile/app/src/main/AndroidManifest.xml`
- Test: `mobile/app/src/test/java/com/wordcoach/feature/overlay/OverlayStateTest.kt`

**Step 1: Register foreground service**

```xml
<service
  android:name=".feature.overlay.FloatingCoachService"
  android:foregroundServiceType="mediaProjection" />
```

**Step 2: Add persistent notification**

- channel: `word_coach_overlay`.
- text: `Word Coach 正在运行`.

**Step 3: Implement draggable bubble**

- single tap triggers capture pipeline.

**Step 4: Add service state tests**

- start/stop behavior.

**Step 5: Commit**

```bash
git add mobile/app/src/main mobile/app/src/test
git commit -m "feat(android): add foreground overlay bubble service"
```

### Task 9: Screen Capture Pipeline

**Files:**
- Create: `mobile/app/src/main/java/com/wordcoach/feature/capture/CaptureCoordinator.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/feature/capture/ProjectionSessionStore.kt`
- Modify: `mobile/app/src/main/java/com/wordcoach/feature/overlay/FloatingCoachService.kt`
- Test: `mobile/app/src/test/java/com/wordcoach/feature/capture/CaptureCoordinatorTest.kt`

**Step 1: Persist MediaProjection grant token**

- store result code + data URI handle in memory/session.

**Step 2: Capture bitmap on-demand**

- use `ImageReader` + virtual display.

**Step 3: Add timeout and fallback**

- fail after 1500ms and emit user-friendly error.

**Step 4: Run unit tests**

Run: `cd mobile; .\gradlew.bat testDebugUnitTest --tests "*CaptureCoordinatorTest"`  
Expected: pass.

**Step 5: Commit**

```bash
git add mobile/app/src/main mobile/app/src/test
git commit -m "feat(android): implement screenshot capture coordinator"
```

### Task 10: OCR and Target Word Selection

**Files:**
- Create: `mobile/app/src/main/java/com/wordcoach/feature/ocr/OcrEngine.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/feature/ocr/WordExtractor.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/feature/ocr/WordScore.kt`
- Test: `mobile/app/src/test/java/com/wordcoach/feature/ocr/WordExtractorTest.kt`
- Test: `mobile/app/src/androidTest/java/com/wordcoach/ocr/OcrInstrumentationTest.kt`

**Step 1: Integrate ML Kit Latin OCR**

- convert bitmap to `InputImage`.

**Step 2: Implement scoring formula**

```kotlin
score = 0.5f*area + 0.2f*center + 0.2f*vocab + 0.1f*confidence
```

**Step 3: Add ambiguity handling**

- if top1-top2 < threshold -> return two candidates.

**Step 4: Run tests**

Run: `cd mobile; .\gradlew.bat testDebugUnitTest connectedDebugAndroidTest`  
Expected: extractor unit tests pass; OCR instrumentation pass on device.

**Step 5: Commit**

```bash
git add mobile/app/src/main mobile/app/src/test mobile/app/src/androidTest
git commit -m "feat(android): add OCR and target word extraction strategy"
```

### Task 11: Overlay Result Card + Copy UX

**Files:**
- Create: `mobile/app/src/main/java/com/wordcoach/feature/overlay/OverlayResultCard.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/feature/overlay/OverlayViewModel.kt`
- Modify: `mobile/app/src/main/java/com/wordcoach/feature/overlay/FloatingCoachService.kt`
- Test: `mobile/app/src/test/java/com/wordcoach/feature/overlay/OverlayViewModelTest.kt`

**Step 1: Build UI state model**

```kotlin
sealed interface OverlayUiState { object Idle; object Loading; data class Success(...); data class Error(...) }
```

**Step 2: Render result card**

- sections: 单词、巧记、词源、例句、复习提示.

**Step 3: Add copy button**

- `ClipboardManager.setPrimaryClip(...)`.

**Step 4: Verify UI logic tests**

Run: `cd mobile; .\gradlew.bat testDebugUnitTest --tests "*OverlayViewModelTest"`  
Expected: pass.

**Step 5: Commit**

```bash
git add mobile/app/src/main mobile/app/src/test
git commit -m "feat(android): add overlay result card and copy action"
```

### Task 12: End-to-End Orchestration and Caching

**Files:**
- Create: `mobile/app/src/main/java/com/wordcoach/feature/coach/domain/RunCoachUseCase.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/core/storage/CoachCacheDao.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/core/storage/CoachDatabase.kt`
- Modify: `mobile/app/src/main/java/com/wordcoach/feature/overlay/OverlayViewModel.kt`
- Test: `mobile/app/src/test/java/com/wordcoach/feature/coach/RunCoachUseCaseTest.kt`

**Step 1: Define orchestration use case**

- capture -> ocr -> choose word -> coach request -> emit result.

**Step 2: Cache by normalized word**

- key: lowercase word.
- TTL: 7 days.

**Step 3: Handle candidate selection fallback**

- if ambiguous, emit candidate list to overlay.

**Step 4: Run tests**

Run: `cd mobile; .\gradlew.bat testDebugUnitTest`  
Expected: use case and cache tests pass.

**Step 5: Commit**

```bash
git add mobile/app/src/main mobile/app/src/test
git commit -m "feat(android): add end-to-end use case and explanation cache"
```

### Task 13: Privacy, Logging, and Error Taxonomy

**Files:**
- Create: `mobile/app/src/main/java/com/wordcoach/core/logging/EventLogger.kt`
- Create: `mobile/app/src/main/java/com/wordcoach/core/error/ErrorCode.kt`
- Modify: `mobile/app/src/main/java/com/wordcoach/feature/coach/domain/RunCoachUseCase.kt`
- Create: `server/src/lib/requestLogger.ts`
- Test: `server/test/security.no-secret-log.test.ts`

**Step 1: Define error taxonomy**

- `PERMISSION_DENIED`, `CAPTURE_FAILED`, `OCR_EMPTY`, `NETWORK_TIMEOUT`, `MODEL_INVALID`.

**Step 2: Add privacy-safe logs**

- log only word + error code + latency.
- never log full screenshot.

**Step 3: Add server redaction middleware**

- mask headers and payload secrets.

**Step 4: Run tests**

Run: `cd server; npm test`  
Expected: no-secret-log test passes.

**Step 5: Commit**

```bash
git add mobile/app/src/main server/src server/test
git commit -m "feat: add privacy-safe logging and error taxonomy"
```

### Task 14: CI Pipeline and Quality Gates

**Files:**
- Create: `.github/workflows/mobile-ci.yml`
- Create: `.github/workflows/server-ci.yml`
- Create: `server/eslint.config.js`
- Create: `mobile/gradle/wrapper/gradle-wrapper.properties` (if missing)
- Test: CI workflow runs

**Step 1: Add mobile CI**

- tasks: `testDebugUnitTest`, `lintDebug`.

**Step 2: Add server CI**

- tasks: `npm ci`, `npm run test`, `npm run lint`, `npm run build`.

**Step 3: Add fail-fast security checks**

- reject accidental key pattern in PR via regex scan.

**Step 4: Trigger CI**

Run: `git push`  
Expected: both workflows green.

**Step 5: Commit**

```bash
git add .github server/eslint.config.js
git commit -m "ci: add quality gates for android and server"
```

### Task 15: Deployment and Runtime Configuration

**Files:**
- Create: `server/Dockerfile`
- Create: `server/render.yaml` (or your hosting config)
- Create: `server/src/routes/version.ts`
- Create: `mobile/app/src/main/java/com/wordcoach/core/config/RuntimeConfig.kt`
- Test: deployment smoke test checklist

**Step 1: Containerize backend**

```dockerfile
FROM node:20-alpine
WORKDIR /app
COPY . .
RUN npm ci && npm run build
CMD ["node","dist/index.js"]
```

**Step 2: Add `/version` endpoint**

- return build sha and timestamp.

**Step 3: Externalize mobile base URL**

- debug/prod endpoint split via build config fields.

**Step 4: Deploy and smoke test**

Run: `curl https://<your-domain>/health`  
Expected: `{"ok":true}`.

**Step 5: Commit**

```bash
git add server mobile/app/src/main
git commit -m "ops: deploy backend proxy and add runtime config"
```

### Task 16: Release Validation and Pilot

**Files:**
- Create: `docs/test-plan/manual-e2e-checklist.md`
- Create: `docs/release/mvp-go-live.md`
- Modify: `README.md`
- Test: real-device pilot logs and metrics

**Step 1: Write E2E checklist**

- first run permissions
- bubble visible in target apps
- capture success
- OCR accuracy
- AI response time
- copy success

**Step 2: Run pilot on 5 devices**

- Android 10/12/13/14 coverage.

**Step 3: Record KPI**

- P50 latency <= 3s
- target word accuracy >= 85%
- copy success >= 99%

**Step 4: Release decision**

- go / no-go criteria in `mvp-go-live.md`.

**Step 5: Commit**

```bash
git add docs README.md
git commit -m "docs: add release checklist and pilot go-live criteria"
```

## Execution Order Summary

1. Foundation: Task 1-2  
2. Security backend: Task 3-5  
3. Android functional core: Task 6-12  
4. Hardening and operations: Task 13-15  
5. Pilot and release: Task 16

## Risk Register (Run Weekly)

- `R1` API key leakage risk -> mandatory proxy + rotated key + CI secret scan.
- `R2` FLAG_SECURE black screen -> user-visible fallback message + manual word input fallback.
- `R3` OCR ambiguity -> dual-candidate chooser + score threshold tuning.
- `R4` latency spikes -> cache hits + 8s timeout + retry(1) + backend rate controls.
- `R5` overlay permission drop -> onboarding re-check on app resume.

## Definition of Done (MVP)

- End-to-end flow works on at least 5 real devices.
- No hardcoded secrets in client package.
- CI pipelines pass for both mobile and server.
- Manual checklist completed and signed in `docs/release/mvp-go-live.md`.
- KPI targets met for 3 consecutive pilot days.

