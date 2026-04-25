package com.woocommerce.android.network.qrlogin

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.woocommerce.android.ui.login.qrlogin.Secret
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import javax.inject.Inject
import javax.inject.Named

/**
 * Calls the QR login token-exchange endpoint on the merchant's WordPress site.
 *
 * The endpoint is unauthenticated — possession of a valid, unexpired, single-use token is the
 * authorization.
 */
class QrLoginRestClient @Inject constructor(
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
            } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
                Result.failure(t)
            }
        }

    private fun performExchange(siteUrl: String, token: String): QrLoginCredentials {
        val request = buildExchangeRequest(siteUrl, token)
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw mapHttpStatus(response.code)
            return parseExchangeBody(response.body.string())
                ?: throw QrLoginExchangeException.MalformedResponse
        }
    }

    private fun buildExchangeRequest(siteUrl: String, token: String): Request {
        // The parser only hands us URLs that survived HttpUrl validation, so this should never
        // be null in practice — fall through to the generic catch if it ever is.
        val baseUrl = siteUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("siteUrl could not be parsed by HttpUrl")
        val exchangeUrl = baseUrl.newBuilder()
            .addPathSegments(EXCHANGE_PATH_SEGMENTS)
            .build()
        val payload = gson.toJson(ExchangeRequest(token))
        return Request.Builder()
            .url(exchangeUrl)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    private fun parseExchangeBody(body: String): QrLoginCredentials? {
        val response = gson.fromJson(body, ExchangeResponse::class.java)
        val credentials = response?.toCredentials()
        if (credentials == null) {
            WooLog.w(
                WooLog.T.LOGIN,
                "QR login exchange response missing required fields: ${response.missingFields()}"
            )
        }
        return credentials
    }

    private fun mapHttpStatus(code: Int): QrLoginExchangeException = when (code) {
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> QrLoginExchangeException.TokenRejected
        HTTP_NOT_FOUND -> QrLoginExchangeException.EndpointMissing
        HTTP_TOO_MANY_REQUESTS -> QrLoginExchangeException.RateLimited
        else -> QrLoginExchangeException.HttpError(code)
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
            if (siteUrl.isNullOrBlank()) return null
            val password = applicationPassword?.takeIf { it.isNotBlank() } ?: return null
            return QrLoginCredentials(
                userLogin = user,
                applicationPassword = Secret(password),
                uuid = uuid?.takeIf { it.isNotBlank() }
            )
        }

        fun missingFields(): String = buildList {
            if (userLogin.isNullOrBlank()) add("user_login")
            if (siteUrl.isNullOrBlank()) add("site_url")
            if (applicationPassword.isNullOrBlank()) add("application_password")
        }.joinToString(",").ifEmpty { "(none)" }
    }

    private companion object {
        const val EXCHANGE_PATH_SEGMENTS = "wp-json/wc-admin/mobile-app/qr-login-exchange"
        const val HTTP_TOO_MANY_REQUESTS = 429
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

data class QrLoginCredentials(
    val userLogin: String,
    val applicationPassword: Secret,
    val uuid: String?
)

sealed class QrLoginExchangeException(message: String) : Exception(message) {
    data object TokenRejected : QrLoginExchangeException("Token was rejected by the site")
    data object EndpointMissing : QrLoginExchangeException("Exchange endpoint not available on the site")
    data object RateLimited : QrLoginExchangeException("Rate limit hit on the exchange endpoint")
    data object Network : QrLoginExchangeException("Network failure during exchange")
    data object MalformedResponse : QrLoginExchangeException("Exchange response was malformed")
    data class HttpError(val code: Int) : QrLoginExchangeException("HTTP $code from exchange endpoint")
    data class Unknown(val original: Throwable) :
        QrLoginExchangeException("Unknown exchange failure: ${original.javaClass.simpleName}")
}
