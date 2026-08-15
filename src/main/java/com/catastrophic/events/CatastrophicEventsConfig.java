package com.catastrophic.events;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("catastrophicevents")
public interface CatastrophicEventsConfig extends Config
{
	@ConfigItem(
		position = 1,
		keyName = "token",
		name = "Plugin token",
		description = "Paste the token DM'd to you by the !link command in Discord.",
		secret = true
	)
	default String token()
	{
		return "";
	}

	@ConfigItem(
		position = 2,
		keyName = "lootSharingEnabled",
		name = "Share big loot drops",
		description = "Post a screenshot and summary to Discord when you receive a non-stacked drop worth 1.5m gp or more."
	)
	default boolean lootSharingEnabled()
	{
		return false;
	}

	@ConfigItem(
		position = 3,
		keyName = "deathScreenshotsEnabled",
		name = "Share death screenshots",
		description = "Post a screenshot to Discord whenever you die."
	)
	default boolean deathScreenshotsEnabled()
	{
		return false;
	}

	@ConfigItem(
		position = 4,
		keyName = "deathMessage",
		name = "Death message",
		description = "Text posted alongside death alerts, e.g. \"died being silly\". Leave blank for the default \"Died.\""
	)
	default String deathMessage()
	{
		return "";
	}
}
