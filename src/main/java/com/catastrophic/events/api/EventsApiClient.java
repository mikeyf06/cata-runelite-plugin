package com.catastrophic.events.api;

import com.catastrophic.events.api.dto.EventsResponse;
import com.catastrophic.events.api.dto.SignupRequest;
import com.catastrophic.events.api.dto.SignupResponse;
import com.google.gson.Gson;
import java.io.IOException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
public class EventsApiClient
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final String API_BASE = "https://catabot-production.up.railway.app";

	private final OkHttpClient httpClient;
	private final Gson gson;

	@Inject
	public EventsApiClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.gson = gson;
	}

	public void fetchEvents(String token, ApiCallback<EventsResponse> callback)
	{
		Request request = new Request.Builder()
			.url(API_BASE + "/events")
			.header("Authorization", "Bearer " + token)
			.build();

		enqueue(request, EventsResponse.class, callback);
	}

	public void signup(String token, String eventId, ApiCallback<SignupResponse> callback)
	{
		RequestBody body = RequestBody.create(JSON, gson.toJson(new SignupRequest(eventId)));
		Request request = new Request.Builder()
			.url(API_BASE + "/signup")
			.header("Authorization", "Bearer " + token)
			.post(body)
			.build();

		enqueue(request, SignupResponse.class, callback);
	}

	private <T> void enqueue(Request request, Class<T> responseType, ApiCallback<T> callback)
	{
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Catastrophic Events request to {} failed", request.url(), e);
				callback.onError(ApiErrorType.NETWORK, e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				handleResponse(response, responseType, callback);
			}
		});
	}

	private <T> void handleResponse(Response response, Class<T> responseType, ApiCallback<T> callback)
	{
		try (ResponseBody body = response.body())
		{
			if (response.code() == 401)
			{
				callback.onError(ApiErrorType.UNAUTHORIZED, "Invalid or revoked token");
				return;
			}

			if (!response.isSuccessful() || body == null)
			{
				callback.onError(ApiErrorType.SERVER, "Unexpected response: " + response.code());
				return;
			}

			T parsed = gson.fromJson(body.charStream(), responseType);
			if (parsed == null)
			{
				callback.onError(ApiErrorType.SERVER, "Empty response body");
				return;
			}

			callback.onSuccess(parsed);
		}
		catch (Exception e)
		{
			log.debug("Failed to parse Catastrophic Events response", e);
			callback.onError(ApiErrorType.SERVER, e.getMessage());
		}
	}
}
