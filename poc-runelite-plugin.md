# Catastrophic Events — RuneLite Plugin POC

## Goal
A RuneLite plugin that shows upcoming events in a side panel, lets a member sign up with one click, sends private reminder messages, and supports a Discord deep-link. Talks to the website API described in `poc-website.md` — read that first.

## Scope for this POC
**In scope:**
- Config panel to paste the plugin token (from `!link` in Discord)
- Poll loop hitting `GET /api/plugin/events`
- Side panel listing events with a Join button
- Private chatbox reminders at fixed milestones
- Discord deep-link button (opens announcement channel)

**Out of scope (later phases):**
- Auto check-in via world/location detection (`POST /api/plugin/checkin` — endpoint exists on the backend, but wiring detection logic is a follow-up, not POC)
- Screenshot/drop capture and Discord routing
- Join VC button (depends on voice-channel bot wiring, separate track)
- Looking for Content (LFC) ticket browsing in-panel
- Any rendering of `!cevents` chat messages — the bot posts plain text for POC, nothing for the plugin to decode

## Stack
Java, Gradle, RuneLite plugin API (Guice DI). Standard plugin project structure — if you don't already have a plugin skeleton, start from RuneLite's example plugin template.

## Plugin skeleton

```java
@PluginDescriptor(name = "Catastrophic Events")
public class CatastrophicPlugin extends Plugin {

  @Inject private ScheduledExecutorService executor;
  @Inject private OkHttpClient httpClient;
  @Inject private ChatMessageManager chatMessageManager;
  @Inject private ClientToolbar clientToolbar;
  @Inject private CatastrophicConfig config; // holds the pasted token

  private NavigationButton navButton;
  private EventsPanel panel;
  private ScheduledFuture<?> pollTask;
  private final Set<String> remindersShown = new HashSet<>(); // dedupe: "eventId:milestone"

  @Override
  protected void startUp() {
    panel = new EventsPanel(this);
    navButton = NavigationButton.builder()
        .panel(panel)
        .tooltip("Catastrophic Events")
        .icon(ICON)
        .build();
    clientToolbar.addNavigation(navButton);

    pollTask = executor.scheduleWithFixedDelay(this::pollEvents, 0, 45, TimeUnit.SECONDS);
  }

  @Override
  protected void shutDown() {
    pollTask.cancel(true);
    clientToolbar.removeNavigation(navButton);
  }

  private void pollEvents() {
    if (config.token() == null || config.token().isEmpty()) return;
    Request req = new Request.Builder()
        .url(config.apiBase() + "/api/plugin/events")
        .header("Authorization", "Bearer " + config.token())
        .build();
    httpClient.newCall(req).enqueue(new Callback() {
      @Override public void onResponse(Call call, Response response) {
        List<EventDto> events = parseEvents(response);
        SwingUtilities.invokeLater(() -> panel.update(events));
        checkReminderMilestones(events);
      }
      @Override public void onFailure(Call call, IOException e) { /* log, retry next cycle */ }
    });
  }

  private void checkReminderMilestones(List<EventDto> events) {
    for (EventDto e : events) {
      if (!"signed_up".equals(e.status)) continue;
      long minsUntil = e.minutesUntilStart();
      for (int milestone : new int[]{180, 60, 5}) { // 3hr, 1hr, 5min
        String key = e.id + ":" + milestone;
        if (minsUntil <= milestone && !remindersShown.contains(key)) {
          remindersShown.add(key);
          sendReminder(e, milestone);
        }
      }
    }
  }

  private void sendReminder(EventDto e, int milestone) {
    String text = milestone >= 60
        ? String.format("%s starts in %d hour(s)", e.title, milestone / 60)
        : String.format("%s starts in %d min", e.title, milestone);
    chatMessageManager.queue(QueuedMessage.builder()
        .type(ChatMessageType.CONSOLE)
        .sender("Catastrophic Events")
        .runeLiteFormattedMessage(text)
        .build());
  }
}
```

## Signup button (panel)
```java
joinButton.addActionListener(e -> {
  RequestBody body = RequestBody.create(
      MediaType.parse("application/json"),
      "{\"event_id\":" + event.id + "}");
  Request req = new Request.Builder()
      .url(config.apiBase() + "/api/plugin/signup")
      .header("Authorization", "Bearer " + config.token())
      .post(body)
      .build();
  httpClient.newCall(req).enqueue(standardCallback((success) -> {
    if (success) SwingUtilities.invokeLater(() -> joinButton.setText("Signed Up ✓"));
  }));
});
```

## Discord deep-link
```java
deepLinkButton.addActionListener(e -> {
  try {
    Desktop.getDesktop().browse(new URI(
        "https://discord.com/channels/" + GUILD_ID + "/" + event.discordChannelId));
  } catch (Exception ex) { /* log */ }
});
```

## Notes / decisions already made
- **Reminders are private and client-side only** — `ChatMessageManager` renders locally, nothing is sent to the game server. No rate limit concerns, fire as often as the poll loop decides.
- **Dedupe reminders per event+milestone** (see `remindersShown` above) — otherwise every poll cycle re-fires the same reminder.
- **The deep-link is an OS-level action** (`Desktop.browse`), not a game action — no automation caution applies here.
- **Token storage**: store the raw token in RuneLite's config (it's local to the user's machine, same trust model as any other plugin credential). Don't log it.

## Acceptance criteria for this POC
- [ ] Pasting a valid token into config causes the panel to populate with real events
- [ ] An invalid/revoked token shows a clear "not linked" state, not a silent failure
- [ ] Clicking Join calls the signup endpoint and updates the button state
- [ ] Reminders fire once per milestone per event, not repeatedly on every poll
- [ ] Deep-link button opens the correct Discord channel
- [ ] Plugin doesn't throw/crash if the API is unreachable — fails quietly and retries next cycle
