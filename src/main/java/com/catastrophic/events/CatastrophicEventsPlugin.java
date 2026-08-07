package com.catastrophic.events;

import com.catastrophic.events.api.ApiCallback;
import com.catastrophic.events.api.ApiErrorType;
import com.catastrophic.events.api.EventsApiClient;
import com.catastrophic.events.api.dto.EventDto;
import com.catastrophic.events.api.dto.EventsResponse;
import com.catastrophic.events.api.dto.SignupResponse;
import com.catastrophic.events.ui.CatastrophicEventsPanel;
import com.google.common.base.Strings;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Catastrophic Events",
	description = "Browse upcoming clan events, sign up with one click, and get private reminders before they start",
	tags = {"events", "clan", "discord", "signup", "reminder"}
)
public class CatastrophicEventsPlugin extends Plugin
{
	private static final int POLL_INTERVAL_SECONDS = 45;
	private static final int[] REMINDER_MILESTONES_MINUTES = {180, 60, 5};

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private CatastrophicEventsConfig config;

	@Inject
	private EventsApiClient apiClient;

	@Inject
	private ConfigManager configManager;

	private CatastrophicEventsPanel panel;
	private NavigationButton navButton;
	private ScheduledFuture<?> pollTask;

	// dedupe key: "eventId:milestone" - a reminder fires once per event+milestone, not every poll cycle
	private final Set<String> remindersShown = ConcurrentHashMap.newKeySet();

	@Provides
	CatastrophicEventsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CatastrophicEventsConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new CatastrophicEventsPanel(this::onJoinClicked, configManager, config, this::pollEvents);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Catastrophic Events")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		remindersShown.clear();
		pollTask = executor.scheduleWithFixedDelay(this::pollEvents, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
	}

	@Override
	protected void shutDown()
	{
		if (pollTask != null)
		{
			pollTask.cancel(true);
			pollTask = null;
		}
		clientToolbar.removeNavigation(navButton);
		panel.destroy();
	}

	private void pollEvents()
	{
		String token = config.token();
		if (Strings.isNullOrEmpty(token))
		{
			SwingUtilities.invokeLater(panel::showNotLinked);
			return;
		}

		apiClient.fetchEvents(config.apiBase(), token, new ApiCallback<EventsResponse>()
		{
			@Override
			public void onSuccess(EventsResponse result)
			{
				List<EventDto> events = result.getEvents() == null ? Collections.emptyList() : result.getEvents();
				SwingUtilities.invokeLater(() -> panel.showEvents(events));
				checkReminderMilestones(events);
			}

			@Override
			public void onError(ApiErrorType type, String message)
			{
				if (type == ApiErrorType.UNAUTHORIZED)
				{
					SwingUtilities.invokeLater(panel::showNotLinked);
				}
				else
				{
					log.debug("Failed to fetch Catastrophic Events: {}", message);
					SwingUtilities.invokeLater(panel::showConnectionError);
				}
			}
		});
	}

	private void onJoinClicked(EventDto event, JButton joinButton)
	{
		String token = config.token();
		if (Strings.isNullOrEmpty(token))
		{
			return;
		}

		joinButton.setEnabled(false);
		joinButton.setText("Joining...");

		apiClient.signup(config.apiBase(), token, event.getId(), new ApiCallback<SignupResponse>()
		{
			@Override
			public void onSuccess(SignupResponse result)
			{
				SwingUtilities.invokeLater(() ->
				{
					joinButton.setText("Signed Up ✓");
					joinButton.setEnabled(false);
				});
			}

			@Override
			public void onError(ApiErrorType type, String message)
			{
				log.debug("Signup for event {} failed: {}", event.getId(), message);
				SwingUtilities.invokeLater(() ->
				{
					joinButton.setText("Join");
					joinButton.setEnabled(true);
				});
			}
		});
	}

	private void checkReminderMilestones(List<EventDto> events)
	{
		for (EventDto event : events)
		{
			if (!event.isSignedUp())
			{
				continue;
			}

			long minutesUntil;
			try
			{
				minutesUntil = event.minutesUntilStart();
			}
			catch (Exception e)
			{
				continue;
			}

			if (minutesUntil < 0)
			{
				continue;
			}

			for (int milestone : REMINDER_MILESTONES_MINUTES)
			{
				String key = event.getId() + ":" + milestone;
				if (minutesUntil <= milestone && remindersShown.add(key))
				{
					sendReminder(event, milestone);
				}
			}
		}
	}

	private void sendReminder(EventDto event, int milestoneMinutes)
	{
		String text = milestoneMinutes >= 60
			? String.format("%s starts in %d hour(s)", event.getTitle(), milestoneMinutes / 60)
			: String.format("%s starts in %d min", event.getTitle(), milestoneMinutes);

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.sender("Catastrophic Events")
			.runeLiteFormattedMessage(text)
			.build());
	}
}
