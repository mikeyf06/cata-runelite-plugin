package com.catastrophic.events.alerts;

import com.catastrophic.events.CatastrophicEventsConfig;
import com.catastrophic.events.api.AlertsApiClient;
import com.catastrophic.events.api.ApiCallback;
import com.catastrophic.events.api.ApiErrorType;
import com.google.common.base.Strings;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ActorDeath;
import net.runelite.client.eventbus.Subscribe;

/** Shares a screenshot to Discord whenever the local player dies. */
@Slf4j
public class DeathAlertListener
{
	private final Client client;
	private final CatastrophicEventsConfig config;
	private final ScreenshotCapture screenshotCapture;
	private final AlertsApiClient alertsApiClient;

	@Inject
	public DeathAlertListener(Client client, CatastrophicEventsConfig config,
		ScreenshotCapture screenshotCapture, AlertsApiClient alertsApiClient)
	{
		this.client = client;
		this.config = config;
		this.screenshotCapture = screenshotCapture;
		this.alertsApiClient = alertsApiClient;
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (!config.deathScreenshotsEnabled())
		{
			return;
		}

		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}

		String token = config.token();
		if (Strings.isNullOrEmpty(token))
		{
			return;
		}

		String message = Strings.isNullOrEmpty(config.deathMessage()) ? "Died." : config.deathMessage();

		screenshotCapture.capture(png -> alertsApiClient.sendAlert(token, AlertKind.DEATH, message, png, new ApiCallback<Void>()
		{
			@Override
			public void onSuccess(Void result)
			{
			}

			@Override
			public void onError(ApiErrorType type, String message)
			{
				log.debug("Death alert failed: {}", message);
			}
		}));
	}
}
