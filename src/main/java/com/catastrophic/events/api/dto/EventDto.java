package com.catastrophic.events.api.dto;

import com.google.gson.annotations.SerializedName;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import lombok.Getter;

@Getter
public class EventDto
{
	private String id;
	private String title;

	@SerializedName("start_at")
	private String startAt;

	/** "signed_up" or "not_signed_up" */
	private String status;

	@SerializedName("discord_channel_id")
	private String discordChannelId;

	private Attendance attendance;

	public boolean isSignedUp()
	{
		return "signed_up".equals(status);
	}

	/**
	 * Minutes from now until the event starts. Negative once the event has passed.
	 * Throws if start_at can't be parsed as ISO-8601 - callers should treat that event as unschedulable.
	 */
	public long minutesUntilStart()
	{
		Instant start = OffsetDateTime.parse(startAt).toInstant();
		return Duration.between(Instant.now(), start).toMinutes();
	}

	@Getter
	public static class Attendance
	{
		@SerializedName("signed_up")
		private int signedUp;
		private int present;
	}
}
