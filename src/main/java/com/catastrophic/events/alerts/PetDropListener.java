package com.catastrophic.events.alerts;

import com.catastrophic.events.CatastrophicEventsConfig;
import com.catastrophic.events.api.AlertsApiClient;
import com.catastrophic.events.api.ApiCallback;
import com.catastrophic.events.api.ApiErrorType;
import com.google.common.base.Strings;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;

/** Shares pet drops to Discord with a screenshot. */
@Slf4j
public class PetDropListener
{
	// Covers both known pet-obtained message families: "You have a funny feeling like you're
	// being followed." (combat/boss/minigame pets) and "You feel something weird sneaking into
	// your backpack." (skilling pets). Not filtered by ChatMessageType - matched purely on these
	// distinctive phrases, since the type classification isn't verified against a live client.
	private static final Pattern PET_OBTAINED = Pattern.compile(
		"funny feeling like you're being followed|feel something weird sneaking into your backpack",
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
