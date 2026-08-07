package com.catastrophic.events.api;

public enum ApiErrorType
{
	/** Missing / invalid / revoked token - a 401 response. */
	UNAUTHORIZED,
	/** Request never reached the server, timed out, DNS failure, etc. */
	NETWORK,
	/** Server reachable but returned an unexpected status or malformed body. */
	SERVER
}
