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
 * Shares skill/XP/total-level milestones to Discord: 99s, 25m XP-past-99 milestones, 50m total XP
 * milestones, total level milestones, and max cape. Entirely chat-message driven - no polling, no
 * client-state baseline of any kind. Each of these messages only ever appears live, the moment the
 * real achievement happens, so unlike the previous Quest.getState()/StatChanged-polling approach
 * (which needed a "what did the player already have coming into this session" baseline that proved
 * impossible to time correctly around login - confirmed live, twice, for both quests and skills) there
 * is nothing to race against and nothing to get wrong.
 */
@Slf4j
public class SkillMilestoneListener
{
	private static final int MAX_TOTAL_LEVEL = 2277;

	// Confirmed live against a real client - exact Jagex wording (see task worklog for screenshots).
	private static final Pattern SKILL_LEVEL_UP = Pattern.compile(
		"Congratulations, you've just advanced your (?<skill>[A-Za-z]+) level\\. You are now level (?<level>\\d+)\\.");
	private static final Pattern TOTAL_LEVEL_MILESTONE = Pattern.compile(
		"Congratulations, you've reached a total level of (?<total>[\\d,]+)\\.");

	// Best-effort guess, not confirmed against a live client - same caveat as PetDropListener/
	// OneTimeRewardListener (concerns.json WRN-006). OSRS's native broadcast system may not actually
	// announce round XP numbers at all, unlike level-ups and total level, which are confirmed real; if
	// these never fire, that's the likely reason - tighten the wording or drop the feature once observed
	// live (or not observed after a reasonable amount of play).
	private static final Pattern SKILL_XP_MILESTONE = Pattern.compile(
		"Congratulations, you've reached (?<xp>[\\d,]+) experience in (?<skill>[A-Za-z]+)\\.");
	private static final Pattern TOTAL_XP_MILESTONE = Pattern.compile(
		"Congratulations, you've reached (?<xp>[\\d,]+) total experience\\.");

	private final CatastrophicEventsConfig config;
	private final AlertsApiClient alertsApiClient;

	@Inject
	public SkillMilestoneListener(CatastrophicEventsConfig config, AlertsApiClient alertsApiClient)
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

		String message = event.getMessage();
		if (Strings.isNullOrEmpty(message))
		{
			return;
		}

		String token = config.token();
		if (Strings.isNullOrEmpty(token))
		{
			return;
		}

		Matcher levelUp = SKILL_LEVEL_UP.matcher(message);
		if (levelUp.find())
		{
			handleSkillLevelUp(token, levelUp);
			return;
		}

		Matcher totalLevel = TOTAL_LEVEL_MILESTONE.matcher(message);
		if (totalLevel.find())
		{
			handleTotalLevelMilestone(token, totalLevel);
			return;
		}

		Matcher skillXp = SKILL_XP_MILESTONE.matcher(message);
		if (skillXp.find())
		{
			handleSkillXpMilestone(token, skillXp);
			return;
		}

		Matcher totalXp = TOTAL_XP_MILESTONE.matcher(message);
		if (totalXp.find())
		{
			handleTotalXpMilestone(token, totalXp);
		}
	}

	private void handleSkillLevelUp(String token, Matcher matcher)
	{
		if (Integer.parseInt(matcher.group("level")) != 99)
		{
			return;
		}

		String skillName = matcher.group("skill");
		post(token, String.format("reached 99 %s", skillName), String.format("99 %s", skillName));
	}

	private void handleTotalLevelMilestone(String token, Matcher matcher)
	{
		int total = Integer.parseInt(matcher.group("total").replace(",", ""));
		if (total >= MAX_TOTAL_LEVEL)
		{
			post(token, "achieved max cape (all 99s)", "Max Cape");
		}
		else
		{
			post(token, String.format("reached %,d Total Level", total), String.format("%,d Total Level", total));
		}
	}

	private void handleSkillXpMilestone(String token, Matcher matcher)
	{
		long xp = Long.parseLong(matcher.group("xp").replace(",", ""));
		String skillName = matcher.group("skill");
		post(token, String.format("reached %,d XP in %s", xp, skillName), String.format("%s XP milestone", skillName));
	}

	private void handleTotalXpMilestone(String token, Matcher matcher)
	{
		long xp = Long.parseLong(matcher.group("xp").replace(",", ""));
		post(token, String.format("reached %,d Total XP", xp), "Total XP milestone");
	}

	private void post(String token, String summary, String title)
	{
		alertsApiClient.sendAlert(token, AlertKind.ACCOMPLISHMENT, summary, title, new ApiCallback<Void>()
		{
			@Override
			public void onSuccess(Void result)
			{
			}

			@Override
			public void onError(ApiErrorType type, String message)
			{
				log.debug("Accomplishment alert failed: {}", message);
			}
		});
	}
}
