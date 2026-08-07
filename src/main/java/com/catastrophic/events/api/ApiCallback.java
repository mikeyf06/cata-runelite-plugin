package com.catastrophic.events.api;

public interface ApiCallback<T>
{
	void onSuccess(T result);

	void onError(ApiErrorType type, String message);
}
