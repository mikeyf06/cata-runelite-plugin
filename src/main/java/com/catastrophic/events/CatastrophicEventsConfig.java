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
}
