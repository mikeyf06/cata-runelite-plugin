package com.catastrophic.events.alerts;

import com.catastrophic.events.CatastrophicEventsConfig;
import com.catastrophic.events.api.AlertsApiClient;
import com.catastrophic.events.api.ApiCallback;
import com.catastrophic.events.api.ApiErrorType;
import com.google.common.base.Strings;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

/**
 * Shares Grandmaster/special quest completions to Discord. There's no dedicated quest-completion
 * event in the RuneLite API (no QuestCompleted class), and polling Quest.getState() on a timer proved
 * unreliable in practice - it must run on the client thread (calling it from a background poll threw
 * AssertionError every time), and quest state wasn't reliably settled by the exact moment
 * GameStateChanged reports LOGGED_IN either (baselining there still false-fired for every tracked
 * quest at once - confirmed live, twice). Detecting the login-completion chat message instead sidesteps
 * both problems: it only ever fires for a real completion happening live in this session, so there is
 * no "already-done" state to race against or baseline in the first place.
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

	// Best-effort recalled wording, not confirmed against a live client - same caveat as
	// PetDropListener/OneTimeRewardListener (see concerns.json WRN-006). Tighten if it turns out wrong.
	private static final Pattern QUEST_COMPLETE_MESSAGE = Pattern.compile(
		"Congratulations, you have completed a quest", Pattern.CASE_INSENSITIVE);

	private final Client client;
	private final CatastrophicEventsConfig config;
	private final AlertsApiClient alertsApiClient;

	// Which tracked quests are already known finished, so the generic completion chat message (it
	// doesn't name the quest) only ever reports whichever one(s) are newly FINISHED and not already in
	// this set - never anything the player already had done coming into this session. Seeded once,
	// a few ticks after login rather than immediately on it, to give quest state (which lagged behind
	// the rest of login by a tick or more - also confirmed live) time to fully settle; only ever used to
	// suppress already-known completions, never as the trigger to alert, so being a little late seeding
	// it is harmless - nothing can complete a Grandmaster quest in the first few ticks after logging in.
	private final Set<Quest> knownFinished = new HashSet<>();
	private boolean baselineSeedPending;

	@Inject
	public QuestMilestoneListener(Client client, CatastrophicEventsConfig config, AlertsApiClient alertsApiClient)
	{
		this.client = client;
		this.config = config;
		this.alertsApiClient = alertsApiClient;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			baselineSeedPending = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!baselineSeedPending)
		{
			return;
		}

		baselineSeedPending = false;
		for (Quest quest : TRACKED_QUESTS)
		{
			if (quest.getState(client) == QuestState.FINISHED)
			{
				knownFinished.add(quest);
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.accomplishmentSharingEnabled())
		{
			return;
		}

		String message = event.getMessage();
		if (Strings.isNullOrEmpty(message) || !QUEST_COMPLETE_MESSAGE.matcher(message).find())
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
			if (knownFinished.contains(quest) || quest.getState(client) != QuestState.FINISHED)
			{
				continue;
			}

			knownFinished.add(quest);
			String questName = quest.getName();
			alertsApiClient.sendAlert(token, AlertKind.ACCOMPLISHMENT, String.format("completed %s", questName), questName,
				new ApiCallback<Void>()
				{
					@Override
					public void onSuccess(Void result)
					{
					}

					@Override
					public void onError(ApiErrorType type, String errorMessage)
					{
						log.debug("Quest accomplishment alert failed: {}", errorMessage);
					}
				});
		}
	}
}
