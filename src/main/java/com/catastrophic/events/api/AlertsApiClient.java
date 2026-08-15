package com.catastrophic.events.api;

import com.catastrophic.events.alerts.AlertKind;
import java.io.IOException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class AlertsApiClient
{
	private static final MediaType PNG = MediaType.parse("image/png");
	private static final String API_BASE = "https://catabot-production.up.railway.app";

	private final OkHttpClient httpClient;

	@Inject
	public AlertsApiClient(OkHttpClient httpClient)
	{
		this.httpClient = httpClient;
	}

	public void sendAlert(String token, AlertKind kind, String summary, byte[] pngBytes, ApiCallback<Void> callback)
	{
		RequestBody body = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("kind", kind.wireValue())
			.addFormDataPart("summary", summary)
			.addFormDataPart("image", "alert.png", RequestBody.create(PNG, pngBytes))
			.build();

		Request request = new Request.Builder()
			.url(API_BASE + "/alerts")
			.header("Authorization", "Bearer " + token)
			.post(body)
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Alert request to {} failed", request.url(), e);
				callback.onError(ApiErrorType.NETWORK, e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response ignored = response)
				{
					if (response.code() == 401)
					{
						callback.onError(ApiErrorType.UNAUTHORIZED, "Invalid or revoked token");
						return;
					}

					if (!response.isSuccessful())
					{
						callback.onError(ApiErrorType.SERVER, "Unexpected response: " + response.code());
						return;
					}

					callback.onSuccess(null);
				}
			}
		});
	}
}
