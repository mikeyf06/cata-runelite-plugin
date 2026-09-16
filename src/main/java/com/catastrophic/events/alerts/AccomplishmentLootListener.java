package com.catastrophic.events.alerts;

import com.catastrophic.events.CatastrophicEventsConfig;
import com.catastrophic.events.api.AlertsApiClient;
import com.catastrophic.events.api.ApiCallback;
import com.catastrophic.events.api.ApiErrorType;
import com.google.common.base.Strings;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;

/** Shares a screenshot + summary to Discord for a fixed allowlist of mega-rare/high-value one-off drops, regardless of value - separate from LootAlertListener's value-threshold sharing. */
@Slf4j
public class AccomplishmentLootListener
{
	private static final Set<Integer> TRACKED_ITEM_IDS = Set.of(
		ItemID.TWISTED_BOW,
		ItemID.SCYTHE_OF_VITUR,
		ItemID.TUMEKENS_SHADOW,
		ItemID.ANCIENT_BLOOD_ORNAMENT_KIT
	);

	private final CatastrophicEventsConfig config;
	private final ItemManager itemManager;
	private final ScreenshotCapture screenshotCapture;
	private final AlertsApiClient alertsApiClient;

	@Inject
	public AccomplishmentLootListener(CatastrophicEventsConfig config, ItemManager itemManager,
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
		if (!config.accomplishmentSharingEnabled())
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
			if (!TRACKED_ITEM_IDS.contains(item.getId()))
			{
				continue;
			}

			ItemComposition composition = itemManager.getItemComposition(item.getId());
			String summary = String.format("received %s from %s", composition.getName(), source);
			sendAlert(token, composition.getName(), summary);
		}
	}

	private void sendAlert(String token, String title, String summary)
	{
		screenshotCapture.capture(png -> alertsApiClient.sendAlert(token, AlertKind.ACCOMPLISHMENT, summary, title, png,
			new ApiCallback<Void>()
			{
				@Override
				public void onSuccess(Void result)
				{
				}

				@Override
				public void onError(ApiErrorType type, String message)
				{
					log.debug("Accomplishment loot alert failed: {}", message);
				}
			}));
	}
}
