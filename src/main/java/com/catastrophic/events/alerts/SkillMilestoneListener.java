package com.catastrophic.events.alerts;

import com.catastrophic.events.CatastrophicEventsConfig;
import com.catastrophic.events.api.AlertsApiClient;
import com.catastrophic.events.api.ApiCallback;
import com.catastrophic.events.api.ApiErrorType;
import com.google.common.base.Strings;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.Subscribe;

/** Shares skill/XP/total-level milestones to Discord: 99s, 25m XP-past-99 milestones, 50m total XP milestones, total level milestones, and max cape. */
@Slf4j
public class SkillMilestoneListener
{
	private static final long SKILL_XP_MILESTONE_STEP = 25_000_000L;
	private static final long TOTAL_XP_MILESTONE_STEP = 50_000_000L;
	private static final int[] TOTAL_LEVEL_MILESTONES = {1750, 2000, 2200};
	private static final int MAX_TOTAL_LEVEL = 2277;

	private final Client client;
	private final CatastrophicEventsConfig config;
	private final AlertsApiClient alertsApiClient;

	// Baseline per skill is recorded, never alerted on, the first time each skill is observed this
	// session - this is what stops a veteran account's pre-existing 99s/milestones from false-firing.
	private final Map<Skill, Integer> previousLevel = new EnumMap<>(Skill.class);
	private final Map<Skill, Long> previousXp = new EnumMap<>(Skill.class);
	private Integer previousTotalLevel;
	private Long previousTotalXp;

	@Inject
	public SkillMilestoneListener(Client client, CatastrophicEventsConfig config, AlertsApiClient alertsApiClient)
	{
		this.client = client;
		this.config = config;
		this.alertsApiClient = alertsApiClient;
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
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

		Skill skill = event.getSkill();
		if (skill == Skill.OVERALL)
		{
			return;
		}

		checkSkillMilestones(token, skill, event.getLevel(), event.getXp());
		checkTotalMilestones(token);
	}

	private void checkSkillMilestones(String token, Skill skill, int level, long xp)
	{
		Integer prevLevel = previousLevel.put(skill, level);
		Long prevXp = previousXp.put(skill, xp);

		if (prevLevel == null || prevXp == null)
		{
			return;
		}

		if (prevLevel < 99 && level >= 99)
		{
			post(token, String.format("reached 99 %s", skill.getName()), String.format("99 %s", skill.getName()));
		}

		if (level >= 99)
		{
			long prevMilestone = prevXp / SKILL_XP_MILESTONE_STEP;
			long currentMilestone = xp / SKILL_XP_MILESTONE_STEP;
			if (currentMilestone > prevMilestone && currentMilestone > 0)
			{
				long milestoneXp = currentMilestone * SKILL_XP_MILESTONE_STEP;
				post(token, String.format("reached %,d XP in %s", milestoneXp, skill.getName()),
					String.format("%dm %s XP", currentMilestone * 25, skill.getName()));
			}
		}
	}

	private void checkTotalMilestones(String token)
	{
		int totalLevel = client.getTotalLevel();
		long totalXp = client.getOverallExperience();

		Integer prevTotalLevel = previousTotalLevel;
		Long prevTotalXp = previousTotalXp;
		previousTotalLevel = totalLevel;
		previousTotalXp = totalXp;

		if (prevTotalLevel == null || prevTotalXp == null)
		{
			return;
		}

		for (int threshold : TOTAL_LEVEL_MILESTONES)
		{
			if (prevTotalLevel < threshold && totalLevel >= threshold)
			{
				post(token, String.format("reached %,d Total Level", threshold), String.format("%,d Total Level", threshold));
			}
		}

		if (prevTotalLevel < MAX_TOTAL_LEVEL && totalLevel >= MAX_TOTAL_LEVEL)
		{
			post(token, "achieved max cape (all 99s)", "Max Cape");
		}

		long prevMilestone = prevTotalXp / TOTAL_XP_MILESTONE_STEP;
		long currentMilestone = totalXp / TOTAL_XP_MILESTONE_STEP;
		if (currentMilestone > prevMilestone && currentMilestone > 0)
		{
			long milestoneXp = currentMilestone * TOTAL_XP_MILESTONE_STEP;
			post(token, String.format("reached %,d Total XP", milestoneXp), String.format("%dm Total XP", currentMilestone * 50));
		}
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
