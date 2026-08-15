# Catastrophic Events — RuneLite Plugin

RuneLite side-panel plugin for the Catastrophic Events clan tooling. Talks to the
**bot's** plugin-facing HTTP API (`catabot/src/http/server.ts`, listening on
`PLUGIN_API_PORT`, default `8080`) — the website (`catabot-web`) previously hosted
this API but that's being removed. The plugin doesn't link out to the website at
all. See [`poc-runelite-plugin.md`](poc-runelite-plugin.md) for the original
scope/design doc (written when the website still hosted the API — the endpoint
paths there are stale, see below).

## What's implemented

- Config panel: plugin token (secret field), loot-sharing and
  death-screenshot toggles (both opt-in), a custom death message field
- Poll loop (45s) hitting `GET {apiBase}/events`
- Side panel: not-linked state, connection-error state, All Events / My Events
  tabs, a featured "hero" card for the soonest joined event, compact rows for
  everything else
- Join button → `POST {apiBase}/signup`, updates in place on success/failure
- Discord deep-link button per event, and a general "open Discord server"
  footer button, both using a hardcoded guild ID (`DISCORD_GUILD_ID` in
  `CatastrophicEventsPlugin.java`) - not user-configurable
- Private chatbox reminders at T-3h / T-1h / T-5min, deduped per event+milestone
- Inline "Event Setup" view (gear icon) for editing the plugin token without
  leaving the panel — writes straight to `ConfigManager`
- Discord alerts: loot sharing, death screenshots, and clan coffer activity —
  see "Discord alerts" below

Out of scope for this POC (see `poc-runelite-plugin.md`): auto check-in,
join-VC button, LFC browsing, `!cevents` rendering. These are represented
in the "My Events" hero card as visible-but-disabled rows ("Coming soon"), not
hidden, since the mocked-up design calls for them.

## Discord alerts

The plugin posts a screenshot (and, for loot/coffer, a text summary) to
Discord for three kinds of moments, via a new `POST {apiBase}/alerts`
endpoint on the bot (see `alerts/` and `api/AlertsApiClient.java`). The
plugin never holds Discord channel IDs or webhook URLs — the bot maps
`kind` (`loot`/`death`/`coffer`) to a channel on its own side.

- **Loot sharing** — fires on any single non-stackable item worth 1.5m gp or
  more (`ItemComposition.isStackable() == false`, GE value via
  `ItemManager.getItemPrice`), from either NPC or player (PK) loot. Toggle:
  "Share big loot drops" in config, default **off** (opt-in).
- **Death screenshots** — fires when the local player dies. Toggle: "Share
  death screenshots" in config, default **off** (opt-in). The text posted
  alongside the screenshot defaults to "Died." but can be customized via the
  "Death message" config field (e.g. "died being silly").
- **Clan coffer activity** — fires on clan-chat messages that look like a
  coffer deposit/withdraw. Always on, no config toggle (clan business, not
  personal activity — see the task's decision log).

Delivery goes through `catabot`'s `POST /alerts` endpoint, which owns the
`kind`-to-channel-ID mapping server-side. Live-verified end-to-end against
production for all three alert kinds.

## Endpoint paths (bot, not website)

The bot's routes have no `/api/plugin` prefix, unlike the website's original
routes documented in `poc-website.md`:

- `GET {apiBase}/events`
- `POST {apiBase}/signup`
- `POST {apiBase}/checkin` (not called by the plugin yet - out of scope per above)

Auth is unchanged: `Authorization: Bearer <token>`, 401 on missing/invalid/revoked.

## Project layout

Standard RuneLite external-plugin layout (matches
[runelite/example-plugin](https://github.com/runelite/example-plugin)):

```
src/main/java/com/catastrophic/events/
  CatastrophicEventsPlugin.java   - plugin entrypoint, poll loop, reminders
  CatastrophicEventsConfig.java   - config panel fields
  api/EventsApiClient.java        - OkHttp calls to the bot's plugin API
  api/AlertsApiClient.java        - multipart POST {apiBase}/alerts (screenshot + summary)
  api/dto/                        - Gson response/request shapes (ids are String - Mongo ObjectIds)
  alerts/AlertKind.java           - loot|death|coffer, matches the bot's `kind` field
  alerts/ScreenshotCapture.java   - captures the current client frame as PNG bytes
  alerts/LootAlertListener.java   - @Subscribe on NpcLootReceived/PlayerLootReceived
  alerts/DeathAlertListener.java  - @Subscribe on ActorDeath (local player only)
  alerts/CofferAlertListener.java - @Subscribe on ChatMessage (clan coffer text)
  ui/CatastrophicEventsPanel.java - top-level panel: header, tabs, footer, state routing
  ui/EventHeroCard.java           - featured/expanded card (My Events tab)
  ui/EventCompactRow.java         - compact list row (All Events tab, secondary joined events)
  ui/SettingsView.java            - inline config form opened from the gear icon
  ui/StyledButton.java, CircleIconButton.java, PillBadge.java, RoundedPanel.java,
    WrappingLabel.java, AvatarIcon.java, CatastrophicTheme.java - custom-painted
    Swing UI kit (gold-on-black theme, no default L&F chrome)
src/test/java/.../CatastrophicEventsPluginTest.java - dev launcher (see below)
```

## Running it locally

Verified end-to-end this session: `./gradlew run` boots a real RuneLite client
(JDK 11, Gradle 8.10.2 via the committed wrapper) with the plugin loaded, logged
in via a saved Jagex session, successfully polling a live backend and rendering
real event data, including the Join flow.

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
   ("Event Setup"), and paste a token obtained via the bot's `!link <rsn>`
   command.

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
