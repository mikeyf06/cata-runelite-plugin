# Plan — discord-loot-death-coffer-alerts

**Project:** cata-runelite-plugin
**Task:** discord-loot-death-coffer-alerts
**Branch:** feature/discord-loot-death-coffer-alerts
**Planned:** 2026-08-13T18:50:00Z

---

## Approach

Add a game-event listener layer that detects loot/death/coffer moments client-side, captures a screenshot via RuneLite's frame-capture API, and posts (image + text summary) through a new `AlertsApiClient` to a `POST /alerts` endpoint on catabot. Catabot owns the kind-to-channel-ID mapping server-side, so the plugin never holds raw channel IDs or webhook URLs — it just sends `kind` (loot/death/coffer) + summary + image. The companion catabot-side endpoint is out of scope for this task and is tracked separately in that repo's own `.mflow` project.

## Layers affected

- Config (`CatastrophicEventsConfig.java`)
- New `alerts/` package (game-event listeners + screenshot capture)
- API client layer (new `AlertsApiClient`)
- Plugin entrypoint (`CatastrophicEventsPlugin.java`) — registers listeners with RuneLite's `EventBus`

## Conventions to follow

- Allman brace style, tab indentation
- Layer-grouping file structure (not feature-grouping) under `src/main/java/com/catastrophic/events/`
- `ApiClient` suffix for the API-access class; `ApiCallback<T>` / `ApiErrorType` for async results
- Lombok `@Slf4j` for logging
- No `var` usage — explicit types throughout
- DTO/contract isolation kept separate from listener/domain logic, mirroring the existing `api/dto` pattern

## Concerns to watch

- **WRN-001** — a new endpoint (`/alerts`) extends the same unversioned, untested HTTP contract between this plugin and catabot; a shape mismatch would fail silently or noisily depending on how `AlertsApiClient` is written.
- **WRN-004** — `apiBase` defaults to plain HTTP with no TLS enforcement; screenshots (potentially showing chat, location, inventory) are a more sensitive payload than the existing JSON traffic.
- **TD-001** — no automated test infrastructure exists in this repo; verification for this task is manual via `./gradlew run`, consistent with everything else here.
- **External dependency** — true end-to-end Discord delivery is blocked on a companion catabot task (`POST /alerts`, multipart parsing, channel-ID config, Discord client wiring) that does not exist yet. Plugin-side work can be built and manually stubbed/logged locally in the meantime.

---

## Steps

### Step 1 — Config checkboxes
`CatastrophicEventsConfig.java`: add two `@ConfigItem` booleans, `lootSharingEnabled` (default `true`) and `deathScreenshotsEnabled` (default `true`), positioned after `token`. These render as native checkboxes in RuneLite's config panel — no custom Swing needed. Coffer alerts get no config item (always on, per worklog decision WL-1).

### Step 2 — Alert kind enum
New `alerts/AlertKind.java` enum with `LOOT`, `DEATH`, `COFFER` values.

### Step 3 — Screenshot capture
New `alerts/ScreenshotCapture.java` wrapping RuneLite's `DrawManager.requestNextFrameListener(...)` to capture the current client frame as a PNG byte array via an async callback.

### Step 4 — Alerts API client
New `api/AlertsApiClient.java`: `sendAlert(String token, AlertKind kind, String summary, byte[] pngBytes, ApiCallback<Void>)` posting `multipart/form-data` (fields: `kind`, `summary`, `image`) to `{API_BASE}/alerts` with the existing Bearer auth header pattern from `EventsApiClient`. This is the first multipart usage in this codebase (existing client is pure-JSON) — build with OkHttp's `MultipartBody.Builder`. Log a deviation note if this ends up diverging meaningfully from `EventsApiClient`'s conventions during execution.

### Step 5 — Loot alert listener
New `alerts/LootAlertListener.java`: `@Subscribe` on RuneLite's loot-received event, iterate received items, filter to `!ItemComposition.isStackable()` items whose single-unit GE value (`ItemManager.getItemPrice`) is `>= 1,500,000` gp, gated on `config.lootSharingEnabled()`, build a text summary (player, item, quantity, value, source), capture a screenshot via `ScreenshotCapture`, send via `AlertsApiClient` with `kind=LOOT`.

### Step 6 — Death alert listener
New `alerts/DeathAlertListener.java`: `@Subscribe` on `ActorDeath` filtered to the local player, gated on `config.deathScreenshotsEnabled()`, capture a screenshot, send via `AlertsApiClient` with `kind=DEATH`.

### Step 7 — Clan coffer alert listener
New `alerts/CofferAlertListener.java`: `@Subscribe` on `ChatMessage`, regex-match clan coffer deposit/withdraw text, always active (no config gate, per WL-1), capture a screenshot of the chat, send via `AlertsApiClient` with `kind=COFFER`.

### Step 8 — Wire listeners into the plugin
`CatastrophicEventsPlugin.java`: inject `EventBus`, construct the three listeners plus `ScreenshotCapture` and `AlertsApiClient`, register listeners with `eventBus.register(...)` in `startUp()` and `eventBus.unregister(...)` in `shutDown()`.

### Step 9 — Docs
Update `README.md` with the new config options (`lootSharingEnabled`, `deathScreenshotsEnabled`) and a description of all three alert features, following the existing docs convention.

### Step 10 — Manual QA
Manual QA pass via `./gradlew run` per Definition of Done — no CI exists in this repo. Verify each listener fires correctly, the 1.5m/non-stackable filter behaves as expected, and both toggles gate their respective features. End-to-end Discord delivery is blocked on the companion catabot task and can only be smoke-tested once that endpoint exists (or stubbed locally against a dev catabot instance).

---

## Definition of done

- [ ] All steps completed
- [ ] Follows existing conventions
- [ ] Unit tests written and passing
- [ ] No new concerns introduced
- [ ] Ready for `mflow verify-task`

---
_Generated by mflow plan-task — 2026-08-13T18:50:00Z_
