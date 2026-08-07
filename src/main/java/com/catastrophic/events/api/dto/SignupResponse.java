package com.catastrophic.events.api.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class SignupResponse
{
	@SerializedName("event_id")
	private String eventId;
	private String status;
}
