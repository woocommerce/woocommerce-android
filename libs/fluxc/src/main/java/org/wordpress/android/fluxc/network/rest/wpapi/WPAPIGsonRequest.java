package org.wordpress.android.fluxc.network.rest.wpapi;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.network.UnexpectedStoreResponse;
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseKind;
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseLogger;
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseParseError;
import org.wordpress.android.fluxc.network.rest.GsonRequest;
import org.wordpress.android.fluxc.network.rest.ResponseWithHeaders;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.util.AppLog;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import kotlin.jvm.functions.Function1;

public class WPAPIGsonRequest<T> extends GsonRequest<T> {
    @Nullable private Function1<T, Boolean> mReadableBodyCheck;

    public WPAPIGsonRequest(int method, String url, Map<String, String> params, Map<String, Object> body,
                            Class<T> clazz, Listener<T> listener, OnWPAPIErrorListener errorListener) {
        super(method, params, body, url, clazz, null, listener, new WPAPIErrorListenerWrapper(errorListener));
        addQueryParameters(params);
    }

    public WPAPIGsonRequest(int method, String url, Map<String, String> params, Map<String, Object> body,
                            Class<T> clazz, ResponseListener<T> listener, OnWPAPIErrorListener errorListener) {
        super(method, params, body, url, clazz, null, listener, new WPAPIErrorListenerWrapper(errorListener));
        addQueryParameters(params);
    }

    public WPAPIGsonRequest(int method, String url, Map<String, String> params, Map<String, Object> body,
                            Type type, ResponseListener<T> listener, OnWPAPIErrorListener errorListener) {
        super(method, params, body, url, null, type, listener, new WPAPIErrorListenerWrapper(errorListener));
        addQueryParameters(params);
    }

    /**
     * With a check, a 2xx body that can't be read into the expected type, or that fails the check, is unexpected
     * content, even when it's empty or valid JSON.
     */
    public void setReadableBodyCheck(@Nullable Function1<T, Boolean> readableBodyCheck) {
        mReadableBodyCheck = readableBodyCheck;
    }

    @Override
    @NonNull
    protected Response<ResponseWithHeaders<T>> parseNetworkResponse(@NonNull NetworkResponse response) {
        Response<ResponseWithHeaders<T>> result = super.parseNetworkResponse(response);
        UnexpectedStoreResponse unexpectedStoreResponse = null;
        if (result.error instanceof ParseError) {
            unexpectedStoreResponse = UnexpectedStoreResponse.from(response, getMethod(), getUrl());
        }
        if (unexpectedStoreResponse == null && isUnreadable(result)) {
            unexpectedStoreResponse = UnexpectedStoreResponse.Companion.of(
                    UnexpectedStoreResponseKind.UNEXPECTED_CONTENT, response, getMethod(), getUrl());
        }
        if (unexpectedStoreResponse != null) {
            Throwable cause = result.error != null ? result.error.getCause() : null;
            return Response.error(new UnexpectedStoreResponseParseError(cause, unexpectedStoreResponse));
        }
        return result;
    }

    private boolean isUnreadable(@NonNull Response<ResponseWithHeaders<T>> result) {
        if (mReadableBodyCheck == null) {
            return false;
        }
        if (!result.isSuccess()) {
            return result.error instanceof ParseError;
        }
        T data = result.result.getData();
        return data == null || !mReadableBodyCheck.invoke(data);
    }

    @Override
    public BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error) {
        String errorCode = null;
        JSONObject errorData = null;
        if (error.volleyError instanceof UnexpectedStoreResponseParseError) {
            error.unexpectedStoreResponse =
                    ((UnexpectedStoreResponseParseError) error.volleyError).getUnexpectedStoreResponse();
        }
        if (error.hasVolleyError() && error.volleyError.networkResponse != null) {
            error.unexpectedStoreResponse =
                    UnexpectedStoreResponse.from(error.volleyError.networkResponse, getMethod(), getUrl());
            String jsonString;
            try {
                jsonString = new String(error.volleyError.networkResponse.data,
                        HttpHeaderParser.parseCharset(error.volleyError.networkResponse.headers));
                JSONObject jsonObject = new JSONObject(jsonString);

                String errorMessage = jsonObject.optString("message", "");
                errorCode = jsonObject.optString("code", "");

                if (jsonObject.has("data")) {
                    errorData = jsonObject.optJSONObject("data");
                }
                if (!errorMessage.isEmpty()) {
                    error.message = errorMessage;
                }
            } catch (UnsupportedEncodingException | JSONException e) {
                AppLog.w(AppLog.T.API, e.toString());
            }

            if (errorCode == null) {
                errorCode = "";
            }

            AuthenticationError authenticationError = null;
            if (error.volleyError.networkResponse.statusCode == 401) {
                authenticationError =
                        new AuthenticationError(AuthenticationErrorType.AUTHORIZATION_REQUIRED, errorCode);
            } else if (error.volleyError.networkResponse.statusCode == 403) {
                authenticationError = new AuthenticationError(AuthenticationErrorType.NOT_AUTHENTICATED, errorCode);
            }

            if (mOnAuthFailedListener != null && authenticationError != null) {
                mOnAuthFailedListener.onAuthFailed(new AuthenticateErrorPayload(authenticationError));
            }
        }

        if (error.unexpectedStoreResponse != null) {
            UnexpectedStoreResponseLogger.log(error.unexpectedStoreResponse);
        }

        return new WPAPINetworkError(error, errorCode, errorData);
    }
}
