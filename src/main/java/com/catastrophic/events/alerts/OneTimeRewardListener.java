package com.catastrophic.events.alerts;

import com.catastrophic.events.CatastrophicEventsConfig;
import com.catastrophic.events.api.AlertsApiClient;
import com.catastrophic.events.api.ApiCallback;
import com.catastrophic.events.api.ApiErrorType;
import com.google.common.base.Strings;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;

/**
 * Shares first-time PvM rewards to Discord: fire cape, infernal cape, Dizana's quiver. Only fires on
 * the game's own Collection Log "new item" message, so "first time" is decided by the game rather
 * than guessed from completion text - an account that already owns the item can never trigger it.
 * That message only appears when the player has the in-game "Collection log - new addition
 * notification" setting enabled; with it off, no alert is posted. Deliberately not named
 * "achievement cape" - that's ambiguous with the actual Achievement Diary cape, which is excluded.
 * Filtered to ChatMessageType.GAMEMESSAGE so a player can't spoof an alert by typing matching text.
 */
@Slf4j
public class OneTimeRewardListener
{
	// Same wording RuneLite's own ScreenshotPlugin matches (COLLECTION_LOG_TEXT).
	private static final Pattern COLLECTION_LOG_ENTRY = Pattern.compile(
		"New item added to your collection log: (?<item>[^<]+)");

	enum Reward
	{
		FIRE_CAPE("^fire cape$", "Fire Cape", "achieved their first Fire cape"),
		INFERNAL_CAPE("^infernal cape$", "Infernal Cape", "achieved their first Infernal cape"),
		DIZANAS_QUIVER("^dizana.s quiver\\b", "Dizana's Quiver", "achieved their first Dizana's quiver");

		private final Pattern itemName;
		private final String title;
		private final String summary;

		Reward(String itemNameRegex, String title, String summary)
		{
			this.itemName = Pattern.compile(itemNameRegex, Pattern.CASE_INSENSITIVE);
			this.title = title;
			this.summary = summary;
		}
	}

	/** Returns the tracked reward a collection log message announces, or null if it isn't one. */
	static Reward match(String message)
	{
		if (Strings.isNullOrEmpty(message))
		{
			return null;
		}

		Matcher entry = COLLECTION_LOG_ENTRY.matcher(message);
		if (!entry.find())
		{
			return null;
		}

		String item = entry.group("item").trim();
		for (Reward reward : Reward.values())
		{
			if (reward.itemName.matcher(item).find())
			{
				return reward;
			}
		}

		return null;
	}

	private final CatastrophicEventsConfig config;
	private final AlertsApiClient alertsApiClient;

	@Inject
	public OneTimeRewardListener(CatastrophicEventsConfig config, AlertsApiClient alertsApiClient)
	{
		this.config = config;
		this.alertsApiClient = alertsApiClient;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.accomplishmentSharingEnabled() || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		Reward reward = match(event.getMessage());
		if (reward == null)
		{
			return;
		}

		String token = config.token();
		if (Strings.isNullOrEmpty(token))
		{
			return;
		}

		alertsApiClient.sendAlert(token, AlertKind.ACCOMPLISHMENT, reward.summary, reward.title, new ApiCallback<Void>()
		{
			@Override
			public void onSuccess(Void result)
			{
			}

			@Override
			public void onError(ApiErrorType type, String message)
			{
				log.debug("One-time reward alert failed: {}", message);
			}
		});
	}
}
