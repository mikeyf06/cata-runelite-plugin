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
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;

/**
 * Shares loot to Discord in two independent ways: any single non-stackable drop worth 1.5m gp or
 * more (LOOT), and a fixed allowlist of mega-rare/high-value one-off drops regardless of value
 * (ACCOMPLISHMENT) - an item can trigger both. Listens on all three of RuneLite's loot sources:
 * NpcLootReceived/PlayerLootReceived (direct NPC/PK kills) and LootReceived filtered to
 * EVENT/PICKPOCKET (chest/reward-interface/minigame content - e.g. Doom of Mokhaiotl - that never
 * fires the first two; NPC/PLAYER-typed LootReceived is skipped since those would duplicate the
 * other two sources).
 */
@Slf4j
public class LootAlertListener
{
	private static final long VALUE_THRESHOLD_GP = 1_500_000L;

	private static final Set<Integer> TRACKED_ACCOMPLISHMENT_ITEM_IDS = Set.of(
		ItemID.TWISTED_BOW,
		ItemID.SCYTHE_OF_VITUR,
		ItemID.TUMEKENS_SHADOW,
		ItemID.ANCIENT_BLOOD_ORNAMENT_KIT,
		ItemID.PURIFYING_SIGIL
	);

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

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		if (event.getType() != LootRecordType.EVENT && event.getType() != LootRecordType.PICKPOCKET)
		{
			return;
		}

		handleLoot(event.getItems(), event.getName());
	}

	private void handleLoot(Iterable<ItemStack> items, String source)
	{
		String token = config.token();
		if (Strings.isNullOrEmpty(token))
		{
			return;
		}

		for (ItemStack item : items)
		{
			ItemComposition composition = itemManager.getItemComposition(item.getId());

			if (config.lootSharingEnabled() && !composition.isStackable())
			{
				long totalValue = (long) itemManager.getItemPrice(item.getId()) * item.getQuantity();
				if (totalValue >= VALUE_THRESHOLD_GP)
				{
					String summary = String.format("Received %s (x%d) worth %,d gp from %s",
						composition.getName(), item.getQuantity(), totalValue, source);
					sendLootAlert(token, summary);
				}
			}

			if (config.accomplishmentSharingEnabled() && TRACKED_ACCOMPLISHMENT_ITEM_IDS.contains(item.getId()))
			{
				String summary = String.format("received %s from %s", composition.getName(), source);
				sendAccomplishmentAlert(token, composition.getName(), summary);
			}
		}
	}

	private void sendLootAlert(String token, String summary)
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

	private void sendAccomplishmentAlert(String token, String title, String summary)
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
