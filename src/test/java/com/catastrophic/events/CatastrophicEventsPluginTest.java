package com.catastrophic.events;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CatastrophicEventsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CatastrophicEventsPlugin.class);
		RuneLite.main(args);
	}
}
