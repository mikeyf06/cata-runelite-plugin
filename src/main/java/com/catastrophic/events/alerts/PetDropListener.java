package com.catastrophic.events.alerts;

import com.catastrophic.events.CatastrophicEventsConfig;
import com.catastrophic.events.api.AlertsApiClient;
import com.catastrophic.events.api.ApiCallback;
import com.catastrophic.events.api.ApiErrorType;
import com.google.common.base.Strings;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;

/** Shares pet drops to Discord with a screenshot. */
@Slf4j
public class PetDropListener
{
	// Covers the known pet-obtained message families: "You have a funny feeling like you're being
	// followed." (combat/boss/minigame pets, first time), "You have a funny feeling like you would
	// have been followed." (duplicate pet roll), and "You feel something weird sneaking into your
	// backpack." (skilling pets). Filtered to ChatMessageType.GAMEMESSAGE below so a player can't
	// spoof an alert by typing this text themselves - confirmed against a real Discord incident and
	// verified against the "Discord Notifications" Hub plugin's equivalent check.
	private static final Pattern PET_OBTAINED = Pattern.compile(
		"funny feeling like you're being followed"
			+ "|funny feeling like you would have been followed"
			+ "|feel something weird sneaking into your backpack",
		Pattern.CASE_INSENSITIVE);

	private final CatastrophicEventsConfig config;
	private final ScreenshotCapture screenshotCapture;
	private final AlertsApiClient alertsApiClient;

	@Inject
	public PetDropListener(CatastrophicEventsConfig config, ScreenshotCapture screenshotCapture,
		AlertsApiClient alertsApiClient)
	{
		this.config = config;
		this.screenshotCapture = screenshotCapture;
		this.alertsApiClient = alertsApiClient;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.petDropSharingEnabled())
		{
			return;
		}

		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = event.getMessage();
		if (Strings.isNullOrEmpty(message) || !PET_OBTAINED.matcher(message).find())
		{
			return;
		}

		String token = config.token();
		if (Strings.isNullOrEmpty(token))
		{
			return;
		}

		screenshotCapture.capture(png -> alertsApiClient.sendAlert(token, AlertKind.PET, "received a pet", null, png,
			new ApiCallback<Void>()
			{
				@Override
				public void onSuccess(Void result)
				{
				}

				@Override
				public void onError(ApiErrorType type, String errorMessage)
				{
					log.debug("Pet drop alert failed: {}", errorMessage);
				}
			}));
	}
}
