package com.catastrophic.events.alerts;

import com.catastrophic.events.CatastrophicEventsConfig;
import com.catastrophic.events.api.AlertsApiClient;
import com.catastrophic.events.api.ApiCallback;
import com.catastrophic.events.api.ApiErrorType;
import com.google.common.base.Strings;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;

/**
 * Shares first-time PvM/skilling rewards to Discord: fire cape, infernal cape, Dizana's quiver.
 * Deliberately not named "achievement cape" - that's ambiguous with the actual Achievement Diary
 * cape, which is excluded from this feature (see PLAN.md). Each reward is gated by a one-time
 * per-profile flag so it can never re-fire.
 */
@Slf4j
public class OneTimeRewardListener
{
	private static final String CONFIG_GROUP = "catastrophicevents";

	// Matched on distinctive keyword pairs rather than an exact sentence, since precise in-game
	// wording wasn't verified against a live client - see PLAN.md "Concerns to watch".
	private static final Pattern FIRE_CAPE_TOPIC = Pattern.compile("fight cave", Pattern.CASE_INSENSITIVE);
	private static final Pattern INFERNAL_CAPE_TOPIC = Pattern.compile("inferno", Pattern.CASE_INSENSITIVE);
	private static final Pattern DIZANAS_QUIVER_TOPIC = Pattern.compile("dizana", Pattern.CASE_INSENSITIVE);
	private static final Pattern COMPLETION_VERB = Pattern.compile("complet|obtain|congratulat", Pattern.CASE_INSENSITIVE);

	private final CatastrophicEventsConfig config;
	private final ConfigManager configManager;
	private final AlertsApiClient alertsApiClient;

	@Inject
	public OneTimeRewardListener(CatastrophicEventsConfig config, ConfigManager configManager, AlertsApiClient alertsApiClient)
	{
		this.config = config;
		this.configManager = configManager;
		this.alertsApiClient = alertsApiClient;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.accomplishmentSharingEnabled())
		{
			return;
		}

		String message = event.getMessage();
		if (Strings.isNullOrEmpty(message) || !COMPLETION_VERB.matcher(message).find())
		{
			return;
		}

		if (FIRE_CAPE_TOPIC.matcher(message).find())
		{
			fireOnce("firstFireCapeAlertSent", "Fire Cape", "achieved their first Fire cape");
		}
		else if (INFERNAL_CAPE_TOPIC.matcher(message).find())
		{
			fireOnce("firstInfernalCapeAlertSent", "Infernal Cape", "achieved their first Infernal cape");
		}
		else if (DIZANAS_QUIVER_TOPIC.matcher(message).find())
		{
			fireOnce("firstDizanasQuiverAlertSent", "Dizana's Quiver", "achieved their first Dizana's quiver");
		}
	}

	// Flag is set before the send completes (not in onSuccess) so two chat lines about the same
	// achievement arriving close together can't both slip past the check - same dedupe-first
	// tradeoff CofferAlertListener/catabot's coffer dedup already makes elsewhere in this codebase.
	private void fireOnce(String flagKey, String title, String summary)
	{
		if (!Strings.isNullOrEmpty(configManager.getConfiguration(CONFIG_GROUP, flagKey)))
		{
			return;
		}

		String token = config.token();
		if (Strings.isNullOrEmpty(token))
		{
			return;
		}

		configManager.setConfiguration(CONFIG_GROUP, flagKey, "true");

		alertsApiClient.sendAlert(token, AlertKind.ACCOMPLISHMENT, summary, title, new ApiCallback<Void>()
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
