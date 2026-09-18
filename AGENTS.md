# mflow — Catastrophic Events

This project uses the **My Flow** (mflow) workflow system.

## Working directory

All task and project context lives in `.mflow/`. Do not edit these files directly — use the mflow skills.

| File | Purpose |
|------|---------|
| `.mflow/project.json` | Project metadata, environments |
| `.mflow/PROJECT.md` | Human-readable project summary |
| `.mflow/stack.json` | Tech stack details |
| `.mflow/architecture.json` | Layer structure and patterns |
| `.mflow/conventions.json` | Naming and code style |
| `.mflow/concerns.json` | Tech debt and known issues |
| `.mflow/tasks/active/{id}/` | In-flight task records |
| `.mflow/tasks/waiting/{id}/` | Parked task records with HANDOFF.md |
| `.mflow/tasks/archive/` | Completed task records |

## Skills — quick reference

| Track | Skill | When to use |
|-------|-------|-------------|
| Setup (once) | `mflow map-project` | Analyze existing codebase |
| Setup (once) | `mflow init-project` | Bootstrap project context |
| Every task | `mflow init-task` | Start a new task |
| Quote track | `mflow generate-quote` | Scope and estimate for client |
| Every task | `mflow plan-task` | Break task into executable steps |
| Every task | `mflow execute-task` | Execute the plan |
| Every task | `mflow verify-task` | Verify work before shipping |
| Every task | `mflow ship-task` | PR, deploy, close task |
| New session | `mflow resume-task` | Resume a task in a fresh session |
| Any time | `mflow log-decision` | Log a decision or deviation |
| On demand | `mflow compile-docs` | Generate internal docs |
| On demand | `mflow list-tasks` | List all active tasks |

## Resuming a task

To resume a task in a new session, run `mflow resume-task` and provide the task ID.
It will load `task.json` + `worklog.json` and give you a concise briefing on what to do next.

## Branch conventions

| Type | Format |
|------|--------|
| Feature | `feature/{task_id}` |
| Bug fix | `bugfix/{task_id}` |
| Hotfix | `hotfix/{task_id}` |
| Chore | `chore/{task_id}` |

Flow: `feature → staging → main`
Commit format: `feat:`, `fix:`, `chore:`, `hotfix:`, `refactor:`, `test:`, `docs:`

## Running it locally

Verified end-to-end: `./gradlew run` boots a real RuneLite client (JDK 11,
Gradle 8.10.2 via the committed wrapper) with the plugin loaded, logged in via
a saved Jagex session, successfully polling a live backend and rendering real
event data, including the Join flow.

1. Install a JDK 11 (e.g. [Eclipse Temurin 11](https://adoptium.net/temurin/releases/?version=11)).
2. Open this folder in IntelliJ IDEA as a Gradle project (it'll use the
   committed wrapper automatically), or run `./gradlew run` from the CLI.
3. Run `CatastrophicEventsPluginTest.main()` (via IDE or the `run` Gradle
   task). This boots a full RuneLite client in developer mode with the plugin
   preloaded — no need to install it through the Plugin Hub. Developer mode
   only works launching this way, not through the Jagex Launcher; see
   `Using Jagex Accounts` on the RuneLite wiki for the `--insecure-write-credentials`
   bridge if you need to test logged in as a real Jagex account.
4. `EventsApiClient`/`AlertsApiClient` hardcode the production `catabot` URL
   (`https://catabot-production.up.railway.app`) - there's no config field to
   point the plugin at a local instance. To test against a local `catabot`
   (`PLUGIN_API_PORT`, default `8080`), temporarily edit the `API_BASE`
   constant in both files and revert before committing.
5. In the running client: open the plugin's panel, click the gear icon
   ("Event Setup"), and paste a token obtained via the bot's `/link-plugin`
   slash command (takes your RSN as a required option; gated on WOM clan and
   Discord membership).

## What's implemented

- Config panel: plugin token (secret field), loot-sharing and
  death-screenshot toggles (both opt-in), a custom death message field, an
  event chat reminders toggle (opt-in), and accomplishment-sharing /
  pet-drop-sharing toggles (both opt-in)
- Poll loop (45s) hitting `GET {apiBase}/events`
- Side panel: not-linked state, connection-error state, All Events / My Events
  tabs, a featured "hero" card for the soonest joined event, compact rows for
  everything else
- Join button → `POST {apiBase}/signup`, updates in place on success/failure
- Discord deep-link button per event, and a general "open Discord server"
  footer button, both using a hardcoded guild ID (`DISCORD_GUILD_ID` in
  `CatastrophicEventsPlugin.java`) - not user-configurable
- Private chatbox reminders at T-3h / T-1h / T-5min, deduped per event+milestone,
  plus a once-per-session "here's what you're signed up for" summary - both
  gated behind the "Event chat reminders" config toggle, default **off**
  (opt-in)
- Inline "Event Setup" view (gear icon) for editing the plugin token without
  leaving the panel — writes straight to `ConfigManager`
- Discord alerts: loot sharing, death screenshots, clan coffer activity,
  account accomplishments, and pet drops — see "Discord alerts" below

Out of scope for this POC (see `poc-runelite-plugin.md`): auto check-in,
join-VC button, LFC browsing, `!cevents` rendering. These are represented
in the "My Events" hero card as visible-but-disabled rows ("Coming soon"), not
hidden, since the mocked-up design calls for them.

## Discord alerts

The plugin posts to Discord for five kinds of moments, via a
`POST {apiBase}/alerts` endpoint on the bot (see `alerts/` and
`api/AlertsApiClient.java`). The plugin never holds Discord channel IDs or
webhook URLs — the bot maps `kind` (`loot`/`death`/`coffer`/`accomplishment`/`pet`)
to a channel on its own side. Accomplishment and pet alerts also send an
optional `title` field (e.g. `"99 Fishing"`, `"Twisted Bow"`) for a future
Discord-embeds pass to key off of — `loot`/`death`/`coffer` don't send one and
are unaffected.

- **Loot sharing** — fires on any single non-stackable item worth 1.5m gp or
  more (`ItemComposition.isStackable() == false`, GE value via
  `ItemManager.getItemPrice`), from either NPC or player (PK) loot. Posts a
  screenshot plus a text summary. Toggle: "Share big loot drops" in config,
  default **off** (opt-in).
- **Death screenshots** — fires when the local player dies. Posts a
  screenshot plus text. Toggle: "Share death screenshots" in config, default
  **off** (opt-in). The text defaults to "Died." but can be customized via
  the "Death message" config field (e.g. "died being silly").
- **Clan coffer activity** — fires on clan-chat messages that look like a
  coffer deposit/withdraw. **Text only, no screenshot** — the raw chat
  message already names the player who deposited/withdrew, so
  `AlertsApiClient.sendAlert(...)` is called via the text-only overload
  (`ScreenshotCapture` isn't used here). Always on, no config toggle (clan
  business, not personal activity — see the task's decision log).
- **Account accomplishments** — skill/XP milestones (99s, every 25m skill
  XP past 99, every 50m total XP, total level 1750/2000/2200, max cape),
  Grandmaster/special quest completions (checked via `Quest.getState()` on
  the existing 45s poll loop, not per-tick — there's no `QuestCompleted`
  event in this RuneLite version), a fixed mega-rare drop allowlist (Tbow,
  Scythe, Shadow, Ancient blood ornament kit — separate from the generic
  loot-value-threshold sharing above), and first-time fire cape / infernal
  cape / Dizana's quiver (each one-time only, gated by its own persisted
  flag so it can never re-fire). Toggle: "Share account accomplishments" in
  config, default **off** (opt-in). Deliberately excludes the achievement
  diary cape, quest point cape, music cape, and full diary completion —
  those are all *state checks* rather than fresh events, so a veteran
  player who already had them before installing the plugin would falsely
  trigger.
- **Pet drops** — fires on the pet-obtained chat message. Toggle: "Share
  pet drops" in config, default **off** (opt-in).

Delivery goes through `catabot`'s `POST /alerts` endpoint, which owns the
`kind`-to-channel-ID mapping server-side. Loot and death were live-verified
end-to-end against production; the coffer text-only change depends on a
companion catabot change (tracked separately in that repo's own `.mflow`)
to make the `image` field optional for `kind=coffer` and to stop prefixing
the coffer message with the reporting client's own RSN (which names the
observer, not the actual depositor/withdrawer).

## Endpoint paths (bot, not website)

The bot's routes have no `/api/plugin` prefix, unlike the website's original
routes documented in `poc-website.md`:

- `GET {apiBase}/events`
- `POST {apiBase}/signup`
- `POST {apiBase}/checkin` (not called by the plugin yet - out of scope per above)

Auth is unchanged: `Authorization: Bearer <token>`, 401 on missing/invalid/revoked.

## Project layout

Standard RuneLite external-plugin layout (matches
[runelite/example-plugin](https://github.com/runelite/example-plugin)). See
[`.mflow/module-map.md`](.mflow/module-map.md) for the file-by-file breakdown.

## Known gaps / things to confirm against the live API

- No endpoint returns a Discord guild ID, so the Discord deep-link URL
  (`discord.com/channels/{guild}/{channel}`) uses a hardcoded constant
  (`DISCORD_GUILD_ID`) rather than a config item - fine for this single clan's
  plugin, but not something another server could reuse without a source change.
- No `host` field in the events response yet (bot's `Event` model has
  `hostDiscordId`, but `GET /events` doesn't return it) — both card styles
  drop the "Host" row rather than show nothing/fake data.
- No "expected world" field on events yet, so the hero card's "World" row and
  any future auto-check-in logic have nothing to compare `client.getWorld()`
  against.
- `catabot-web`'s `/api/plugin/*` routes still exist as of this writing but are
  being removed — don't rely on them going forward.
- `PetDropListener` and `OneTimeRewardListener`'s chat-message regex patterns
  are best-effort, not confirmed against a live client actually producing
  those messages (a real pet drop or Inferno completion can't be faked in
  dev). If the wording turns out to be off, the affected alert just silently
  never fires — no crash, no error. Confirm against real chat text the first
  time each one fires for real, and tighten the pattern if needed (see
  WRN-006 in `.mflow/concerns.json`).

## Do not

- Edit `.mflow/` files directly — always use the mflow skills
- Commit secrets or credentials to `.mflow/` (it IS committed to the repo)
- Deploy `.mflow/` — it should be excluded from build output

---
_Generated by mflow init-project — 2026-08-07T19:55:39Z_
