package com.catastrophic.events.api.dto;

import com.google.gson.annotations.SerializedName;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SignupRequest
{
	@SerializedName("event_id")
	private final String eventId;
}
