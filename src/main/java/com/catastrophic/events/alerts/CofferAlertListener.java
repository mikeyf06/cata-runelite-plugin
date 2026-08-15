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

/** Shares a screenshot of clan coffer deposit/withdraw chat messages to Discord. Always on - no config toggle. */
@Slf4j
public class CofferAlertListener
{
	// e.g. "Zezima has deposited 500,000 coins into the coffer." / "...withdrawn... from the coffer."
	private static final Pattern COFFER_TRANSACTION = Pattern.compile(
		"coffer", Pattern.CASE_INSENSITIVE);
	private static final Pattern COFFER_VERB = Pattern.compile(
		"deposit|withdr", Pattern.CASE_INSENSITIVE);

	private final CatastrophicEventsConfig config;
	private final ScreenshotCapture screenshotCapture;
	private final AlertsApiClient alertsApiClient;

	@Inject
	public CofferAlertListener(CatastrophicEventsConfig config, ScreenshotCapture screenshotCapture,
		AlertsApiClient alertsApiClient)
	{
		this.config = config;
		this.screenshotCapture = screenshotCapture;
		this.alertsApiClient = alertsApiClient;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.CLAN_MESSAGE)
		{
			return;
		}

		String message = event.getMessage();
		if (Strings.isNullOrEmpty(message)
			|| !COFFER_TRANSACTION.matcher(message).find()
			|| !COFFER_VERB.matcher(message).find())
		{
			return;
		}

		String token = config.token();
		if (Strings.isNullOrEmpty(token))
		{
			return;
		}

		screenshotCapture.capture(png -> alertsApiClient.sendAlert(token, AlertKind.COFFER, message, png, new ApiCallback<Void>()
		{
			@Override
			public void onSuccess(Void result)
			{
			}

			@Override
			public void onError(ApiErrorType type, String errorMessage)
			{
				log.debug("Coffer alert failed: {}", errorMessage);
			}
		}));
	}
}
