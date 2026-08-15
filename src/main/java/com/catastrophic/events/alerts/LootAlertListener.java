package com.catastrophic.events.alerts;

import com.catastrophic.events.CatastrophicEventsConfig;
import com.catastrophic.events.api.AlertsApiClient;
import com.catastrophic.events.api.ApiCallback;
import com.catastrophic.events.api.ApiErrorType;
import com.google.common.base.Strings;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;

/** Shares a screenshot + summary to Discord for any single non-stackable drop worth 1.5m gp or more. */
@Slf4j
public class LootAlertListener
{
	private static final long VALUE_THRESHOLD_GP = 1_500_000L;

	private final CatastrophicEventsConfig config;
	private final ItemManager itemManager;
	private final ScreenshotCapture screenshotCapture;
	private final AlertsApiClient alertsApiClient;

	@Inject
	public LootAlertListener(CatastrophicEventsConfig config, ItemManager itemManager,
		ScreenshotCapture screenshotCapture, AlertsApiClient alertsApiClient)
	{
		this.config = config;
		this.itemManager = itemManager;
		this.screenshotCapture = screenshotCapture;
		this.alertsApiClient = alertsApiClient;
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		NPC npc = event.getNpc();
		handleLoot(event.getItems(), npc.getName() == null ? "an NPC" : npc.getName());
	}

	@Subscribe
	public void onPlayerLootReceived(PlayerLootReceived event)
	{
		Player player = event.getPlayer();
		handleLoot(event.getItems(), player.getName() == null ? "a player" : player.getName());
	}

	private void handleLoot(Iterable<ItemStack> items, String source)
	{
		if (!config.lootSharingEnabled())
		{
			return;
		}

		String token = config.token();
		if (Strings.isNullOrEmpty(token))
		{
			return;
		}

		for (ItemStack item : items)
		{
			ItemComposition composition = itemManager.getItemComposition(item.getId());
			if (composition.isStackable())
			{
				continue;
			}

			long totalValue = (long) itemManager.getItemPrice(item.getId()) * item.getQuantity();
			if (totalValue < VALUE_THRESHOLD_GP)
			{
				continue;
			}

			String summary = String.format("Received %s (x%d) worth %,d gp from %s",
				composition.getName(), item.getQuantity(), totalValue, source);
			sendAlert(token, summary);
		}
	}

	private void sendAlert(String token, String summary)
	{
		screenshotCapture.capture(png -> alertsApiClient.sendAlert(token, AlertKind.LOOT, summary, png, new ApiCallback<Void>()
		{
			@Override
			public void onSuccess(Void result)
			{
			}

			@Override
			public void onError(ApiErrorType type, String message)
			{
				log.debug("Loot alert failed: {}", message);
			}
		}));
	}
}
