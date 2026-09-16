package com.catastrophic.events.alerts;

import com.catastrophic.events.CatastrophicEventsConfig;
import com.catastrophic.events.api.AlertsApiClient;
import com.catastrophic.events.api.ApiCallback;
import com.catastrophic.events.api.ApiErrorType;
import com.google.common.base.Strings;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;

/**
 * Shares Grandmaster/special quest completions to Discord. There's no dedicated quest-completion
 * event in the RuneLite API (no QuestCompleted class), so state is checked via Quest.getState(Client).
 * A quest completion has no real-time urgency, so checkQuests() is called from
 * CatastrophicEventsPlugin's existing 45s pollEvents() loop rather than a new per-tick EventBus
 * subscription - same detection logic, no added per-tick overhead.
 */
@Slf4j
public class QuestMilestoneListener
{
	private static final Set<Quest> TRACKED_QUESTS = Set.of(
		Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE,
		Quest.DRAGON_SLAYER_II,
		Quest.MONKEY_MADNESS_II,
		Quest.SONG_OF_THE_ELVES,
		Quest.RECIPE_FOR_DISASTER,
		Quest.WHILE_GUTHIX_SLEEPS,
		Quest.THE_BLOOD_MOON_RISES
	);

	private final Client client;
	private final CatastrophicEventsConfig config;
	private final AlertsApiClient alertsApiClient;

	// Baseline per quest is recorded, never alerted on, the first time it's observed this session -
	// same veteran-safety pattern as SkillMilestoneListener, so an already-completed quest never
	// false-fires just because the plugin only just started polling it.
	private final Map<Quest, QuestState> previousState = new EnumMap<>(Quest.class);

	@Inject
	public QuestMilestoneListener(Client client, CatastrophicEventsConfig config, AlertsApiClient alertsApiClient)
	{
		this.client = client;
		this.config = config;
		this.alertsApiClient = alertsApiClient;
	}

	/** Called from CatastrophicEventsPlugin.pollEvents() every ~45s - see class javadoc for why this isn't event-driven. */
	public void checkQuests()
	{
		if (!config.accomplishmentSharingEnabled())
		{
			return;
		}

		String token = config.token();
		if (Strings.isNullOrEmpty(token))
		{
			return;
		}

		for (Quest quest : TRACKED_QUESTS)
		{
			QuestState state = quest.getState(client);
			QuestState prevState = previousState.put(quest, state);

			if (prevState == null || prevState == QuestState.FINISHED)
			{
				continue;
			}

			if (state == QuestState.FINISHED)
			{
				String questName = quest.getName();
				alertsApiClient.sendAlert(token, AlertKind.ACCOMPLISHMENT, String.format("completed %s", questName), questName,
					new ApiCallback<Void>()
					{
						@Override
						public void onSuccess(Void result)
						{
						}

						@Override
						public void onError(ApiErrorType type, String message)
						{
							log.debug("Quest accomplishment alert failed: {}", message);
						}
					});
			}
		}
	}
}
