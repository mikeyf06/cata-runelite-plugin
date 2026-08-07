package com.catastrophic.events.api.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class EventsResponse
{
	private List<EventDto> events;
}
