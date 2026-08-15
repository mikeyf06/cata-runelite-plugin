package com.catastrophic.events.alerts;

/** Matches the `kind` field expected by catabot's POST /alerts endpoint. */
public enum AlertKind
{
	LOOT("loot"),
	DEATH("death"),
	COFFER("coffer");

	private final String wireValue;

	AlertKind(String wireValue)
	{
		this.wireValue = wireValue;
	}

	public String wireValue()
	{
		return wireValue;
	}
}
