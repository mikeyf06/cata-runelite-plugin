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
		keyName = "apiBase",
		name = "Plugin API base URL",
		description = "Base URL of the bot's plugin API (no trailing slash) - this is the bot's HTTP server, not the website."
	)
	default String apiBase()
	{
		return "http://localhost:8080";
	}

	@ConfigItem(
		position = 3,
		keyName = "websiteBase",
		name = "Website base URL",
		description = "Base URL of the Catastrophic Events website (no trailing slash) - only used for the View Event link."
	)
	default String websiteBase()
	{
		return "http://localhost:3000";
	}

	@ConfigItem(
		position = 4,
		keyName = "guildId",
		name = "Discord server ID",
		description = "The clan's Discord server (guild) ID, used to build the deep-link button URL."
	)
	default String guildId()
	{
		return "";
	}
}
