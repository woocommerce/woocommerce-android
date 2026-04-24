package com.woocommerce.android.ui.login.qrlogin

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import javax.inject.Inject
import javax.inject.Named

/**
 * Calls the QR login token-exchange endpoint on the merchant's WordPress site.
 *
 * The endpoint is unauthenticated — possession of a valid, unexpired, single-use token is the
 * authorization. See `~/Code/Automattic/qr-code-login-flow.html` for the full design.
 */
class QrLoginExchangeClient @Inject constructor(
    @Named("custom-ssl") private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val dispatchers: CoroutineDispatchers
) {

    suspend fun exchange(siteUrl: String, token: String): Result<QrLoginCredentials> =
        withContext(dispatchers.io) {
            try {
                Result.success(performExchange(siteUrl, token))
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Result.failure(mapException(t))
            }
        }

    private fun performExchange(siteUrl: String, token: String): QrLoginCredentials {
        val url = siteUrl.trimEnd('/') + EXCHANGE_PATH
        val payload = gson.toJson(ExchangeRequest(token))
        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw mapHttpStatus(response.code)
            return parseExchangeBody(response.body.string())
                ?: throw QrLoginExchangeException.MalformedResponse
        }
    }

    private fun parseExchangeBody(body: String): QrLoginCredentials? =
        runCatching { gson.fromJson(body, ExchangeResponse::class.java) }
            .getOrNull()
            ?.toCredentials()

    private fun mapHttpStatus(code: Int): QrLoginExchangeException = when (code) {
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> QrLoginExchangeException.TokenRejected
        HTTP_NOT_FOUND -> QrLoginExchangeException.EndpointMissing
        HTTP_TOO_MANY_REQUESTS -> QrLoginExchangeException.RateLimited
        else -> QrLoginExchangeException.HttpError(code)
    }

    private fun mapException(throwable: Throwable): Throwable = when (throwable) {
        is QrLoginExchangeException -> throwable
        is IOException -> {
            WooLog.w(WooLog.T.LOGIN, "QR login exchange network failure: ${throwable.message}")
            QrLoginExchangeException.Network
        }
        is JsonSyntaxException -> QrLoginExchangeException.MalformedResponse
        else -> {
            WooLog.e(WooLog.T.LOGIN, "QR login exchange unexpected failure: $throwable")
            QrLoginExchangeException.Unknown(throwable)
        }
    }

    private data class ExchangeRequest(val token: String)

    private data class ExchangeResponse(
        @SerializedName("user_login") val userLogin: String?,
        @SerializedName("site_url") val siteUrl: String?,
        @SerializedName("application_password") val applicationPassword: String?,
        @SerializedName("uuid") val uuid: String?
    ) {
        fun toCredentials(): QrLoginCredentials? {
            val user = userLogin?.takeIf { it.isNotBlank() } ?: return null
            val site = siteUrl?.takeIf { it.isNotBlank() } ?: return null
            val password = applicationPassword?.takeIf { it.isNotBlank() } ?: return null
            return QrLoginCredentials(
                userLogin = user,
                siteUrl = site,
                applicationPassword = password,
                uuid = uuid?.takeIf { it.isNotBlank() }
            )
        }
    }

    private companion object {
        const val EXCHANGE_PATH = "/wp-json/wc-admin/mobile-app/qr-login-exchange"
        const val HTTP_TOO_MANY_REQUESTS = 429
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

data class QrLoginCredentials(
    val userLogin: String,
    val siteUrl: String,
    val applicationPassword: String,
    val uuid: String?
)

sealed class QrLoginExchangeException(message: String) : Exception(message) {
    data object TokenRejected : QrLoginExchangeException("Token was rejected by the site")
    data object EndpointMissing : QrLoginExchangeException("Exchange endpoint not available on the site")
    data object RateLimited : QrLoginExchangeException("Rate limit hit on the exchange endpoint")
    data object Network : QrLoginExchangeException("Network failure during exchange")
    data object MalformedResponse : QrLoginExchangeException("Exchange response was malformed")
    data class HttpError(val code: Int) : QrLoginExchangeException("HTTP $code from exchange endpoint")
    data class Unknown(val original: Throwable) : QrLoginExchangeException("Unknown exchange failure: $original")
}
