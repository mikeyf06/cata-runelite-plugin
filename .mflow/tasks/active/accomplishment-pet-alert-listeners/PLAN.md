# Plan — accomplishment-pet-alert-listeners

**Project:** cata-runelite-plugin
**Task:** accomplishment-pet-alert-listeners
**Branch:** feature/accomplishment-pet-alert-listeners
**Planned:** 2026-09-16T19:14:48Z

---

## Approach

Add five new single-purpose listener classes under `src/main/java/com/catastrophic/events/alerts/`, following the exact pattern already established by `LootAlertListener`/`DeathAlertListener`/`CofferAlertListener` (constructor-injected, `@Subscribe` on RuneLite's EventBus, early-return on disabled toggle/missing token, delegate to `AlertsApiClient`). Extend `AlertKind`, `CatastrophicEventsConfig`, and `AlertsApiClient` to support the two new kinds and the optional `title` field without touching any existing call sites' signatures, then wire the five listeners into `CatastrophicEventsPlugin`'s `startUp()`/`shutDown()`.

## Layers affected

- Alerts (`src/main/java/com/catastrophic/events/alerts/`)
- Config (`CatastrophicEventsConfig.java`)
- API client (`src/main/java/com/catastrophic/events/api/`)
- Plugin entrypoint/orchestrator (`CatastrophicEventsPlugin.java`)

## Conventions to follow

- Allman brace style with tab indentation, consistent across every file in `src/`
- `ApiCallback<Void>` anonymous-class pattern for async results, `log.debug(...)` on error (never surfaced to the user)
- Opt-in config toggles default `false` — matches the documented architectural decision that loot/death sharing is opt-in (`architecture.json` key_decisions)
- `Pattern` constants for chat-message matching, e.g. `CofferAlertListener.COFFER_TRANSACTION`
- Discord channel routing stays entirely server-side in catabot — the plugin sends only a `kind` string, never a channel ID (existing architectural decision)

## Concerns to watch

- `WRN-005` — `AlertsApiClient` already duplicates `EventsApiClient`'s error-classification logic inline. This task extends `AlertsApiClient` further; not fixing the duplication, just aware it's there.
- Two open verification items, to resolve during execution rather than assumed here:
  1. Whether "Blood Moon Rises" has a `Quest` enum constant that `QuestCompleted` actually fires for — it's a newer miniquest and may not be tracked the same way as full quests. If it isn't in the `Quest` enum RuneLite ships, log a deviation and either find an alternate detection mechanism or drop it from this pass.
  2. Exact chat-message wording for the pet "funny feeling" message and the fire cape/infernal cape/Dizana's quiver completion messages — pull from RuneLite's own source or community reference during execution rather than guessing.

---

## Steps

### Step 1 — Add new AlertKind values
In `src/main/java/com/catastrophic/events/alerts/AlertKind.java`, add `ACCOMPLISHMENT("accomplishment")` and `PET("pet")` to the enum, matching catabot's `ALERT_KINDS`.

### Step 2 — Add the two opt-in config toggles
In `src/main/java/com/catastrophic/events/CatastrophicEventsConfig.java`, add `accomplishmentSharingEnabled` (position 6, name "Share account accomplishments") and `petDropSharingEnabled` (position 7, name "Share pet drops"), both `default false`, following the exact `@ConfigItem` shape of `lootSharingEnabled`/`deathScreenshotsEnabled`.

### Step 3 — Extend AlertsApiClient with an optional title field
In `src/main/java/com/catastrophic/events/api/AlertsApiClient.java`, add two new `sendAlert(...)` overloads that accept a `String title` parameter (text-only, and with-screenshot variants), adding a `"title"` form-data part only when non-null. Refactor the two existing overloads (`sendAlert(token, kind, summary, callback)` and `sendAlert(token, kind, summary, pngBytes, callback)`) to delegate into the new title-aware overloads with `title = null`, so `LootAlertListener`, `DeathAlertListener`, and `CofferAlertListener` require no changes.

### Step 4 — SkillMilestoneListener
New file `src/main/java/com/catastrophic/events/alerts/SkillMilestoneListener.java`. `@Subscribe` to `net.runelite.api.events.StatChanged`. Maintain in-memory `Map<Skill, Integer>` of last-observed level and last-observed XP per skill, plus last-observed total level and total XP, populated as events arrive (never firing on a skill's first observation — only on a transition from a recorded previous value across a threshold, so a veteran account's pre-existing 99s never false-positive). Detect and post (gated by `config.accomplishmentSharingEnabled()`):
- Reaching level 99 in a skill
- Crossing each 25m XP boundary past 99 in a skill (25m, 50m, 75m, ...)
- Crossing each 50m boundary in total XP (50m, 100m, 150m, 200m)
- Crossing total level 1750, 2000, or 2200
- Reaching max cape (2277 total level / all skills 99)

Each alert calls `alertsApiClient.sendAlert(token, AlertKind.ACCOMPLISHMENT, summary, title, callback)` with a short `title` (e.g. `"99 Fishing"`, `"2200 Total Level"`, `"Max Cape"`).

### Step 5 — QuestMilestoneListener
New file `src/main/java/com/catastrophic/events/alerts/QuestMilestoneListener.java`. **Revised during execution/verification** (see worklog WL-007, WL-009): there is no `QuestCompleted` event in this RuneLite version at all, so quest state is read via `Quest.getState(Client)`, a verified real method. Rather than polling per `GameTick` (raised in verify-task as an architectural mismatch, not a real performance issue - see WL-009), `checkQuests()` is a plain method called from `CatastrophicEventsPlugin.pollEvents()`'s existing 45s loop. Tracks a fixed allowlist of real `Quest` enum constants (`DESERT_TREASURE_II__THE_FALLEN_EMPIRE`, `DRAGON_SLAYER_II`, `MONKEY_MADNESS_II`, `SONG_OF_THE_ELVES`, `RECIPE_FOR_DISASTER`, `WHILE_GUTHIX_SLEEPS`, `THE_BLOOD_MOON_RISES` - all confirmed to exist via `javap` against the pinned API jar). Gated by `accomplishmentSharingEnabled`, posts with `title` = quest name.

### Step 6 — AccomplishmentLootListener
New file `src/main/java/com/catastrophic/events/alerts/AccomplishmentLootListener.java`. `@Subscribe` to `NpcLootReceived`/`PlayerLootReceived` (same events as `LootAlertListener`), matching a fixed item-ID allowlist (Twisted bow, Scythe of vitur, Tumeken's shadow, Ancient blood ornament kit) regardless of value — deliberately separate from `LootAlertListener`/`lootSharingEnabled` per the task's explicit requirement, since this is a fixed-allowlist feature posting to the `accomplishment` channel, not the value-threshold `loot` feature. Captures a screenshot via the existing `ScreenshotCapture` class, same as `LootAlertListener`. Gated by `accomplishmentSharingEnabled`, posts with `title` = item name.

### Step 7 — PetDropListener
New file `src/main/java/com/catastrophic/events/alerts/PetDropListener.java`. `@Subscribe` to `ChatMessage`, regex match against the pet-obtained game message (exact wording confirmed during execution). Gated by `petDropSharingEnabled`, posts `AlertKind.PET`.

### Step 8 — OneTimeRewardListener
New file `src/main/java/com/catastrophic/events/alerts/OneTimeRewardListener.java` (renamed from the originally-planned `AchievementCapeListener` per developer feedback - that name was ambiguous with the real Achievement Diary cape, which this feature excludes). `@Subscribe` to `ChatMessage`, one `Pattern` per reward (fire cape, infernal cape, Dizana's quiver). Each match is gated by a one-time `ConfigManager` flag unique to that reward, following the exact pattern of `DEATH_MESSAGE_NAME_TOKEN_MIGRATED_KEY` in `CatastrophicEventsPlugin.java`, so it can never re-fire for the same profile. Gated by `accomplishmentSharingEnabled`, posts `AlertKind.ACCOMPLISHMENT` with `title` = reward name.

### Step 9 — Wire listeners into the plugin
In `src/main/java/com/catastrophic/events/CatastrophicEventsPlugin.java`, `@Inject` all five new listeners. Four are true EventBus listeners with matching `eventBus.register(...)`/`eventBus.unregister(...)` calls in `startUp()`/`shutDown()`, following the exact pattern already used for `lootAlertListener`/`deathAlertListener`/`cofferAlertListener`. `questMilestoneListener` is the exception (see Step 5) - it's not registered on the EventBus at all; instead `pollEvents()` calls `questMilestoneListener.checkQuests()` directly at the top of the method.

### Step 10 — Manual verification
Run `./gradlew run` (this repo's only existing verification mechanism — no automated test suite exists, `TD-001`). Verify what can reasonably be exercised locally (chat-message-driven listeners can be tested by injecting/observing real chat text; loot-driven listener can be tested against a fresh, cheap NPC drop by temporarily pointing the allowlist at a common item ID, then reverting). Document by code review what can't be feasibly triggered live (an actual 99 or GM quest completion), tracing the logic path manually instead.

---

## Definition of done

- [ ] All steps completed
- [ ] Follows existing conventions
- [ ] Unit tests written and passing
- [ ] No new concerns introduced
- [ ] Ready for `mflow verify-task`

---
_Generated by mflow plan-task — 2026-09-16T19:14:48Z_
