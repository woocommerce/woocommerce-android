package com.woocommerce.android.network.qrlogin

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_PRECON_FAILED
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import javax.inject.Inject
import javax.inject.Named

/**
 * Calls the QR login endpoints on the merchant's WordPress site.
 *
 * Three endpoints are exposed:
 *   - [scan] reports the merchant scanned the QR. Server picks the real number for the
 *     coincidence-verification step and returns a session id the app polls for status.
 *   - [checkSessionStatus] polls for the merchant's tap on wc-admin. Returns the
 *     `exchange_grant` nonce once approved, which gates the final exchange call.
 *   - [exchange] swaps the QR token + exchange grant for an Application Password.
 *
 * All three are unauthenticated — possession of the single-use token (and the matching
 * grant nonce on exchange) is the authorization.
 */
class QrLoginRestClient @Inject constructor(
    @Named("custom-ssl") private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val dispatchers: CoroutineDispatchers,
    private val deviceInfoProvider: QrLoginDeviceInfoProvider,
) {

    suspend fun scan(siteUrl: String, token: String): Result<QrLoginScanResult> =
        withContext(dispatchers.io) {
            try {
                Result.success(performScan(siteUrl, token))
            } catch (ce: CancellationException) {
                throw ce
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Result.failure(mapScanException(e))
            }
        }

    suspend fun checkSessionStatus(siteUrl: String, sessionId: String): Result<QrLoginSessionStatus> =
        withContext(dispatchers.io) {
            try {
                Result.success(performSessionStatus(siteUrl, sessionId))
            } catch (ce: CancellationException) {
                throw ce
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Result.failure(mapSessionStatusException(e))
            }
        }

    suspend fun exchange(
        siteUrl: String,
        token: String,
        exchangeGrant: String,
    ): Result<QrLoginCredentials> =
        withContext(dispatchers.io) {
            try {
                Result.success(performExchange(siteUrl, token, exchangeGrant))
            } catch (ce: CancellationException) {
                throw ce
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Result.failure(mapExchangeException(e))
            }
        }

    // region scan

    private fun performScan(siteUrl: String, token: String): QrLoginScanResult {
        val request = buildScanRequest(siteUrl, token)
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw mapScanHttpStatus(response.code, response.body.string())
            return parseScanBody(response.body.string())
                ?: throw QrLoginScanException.MalformedResponse
        }
    }

    private fun buildScanRequest(siteUrl: String, token: String): Request {
        val baseUrl = siteUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("siteUrl could not be parsed by HttpUrl")
        val scanUrl = baseUrl.newBuilder()
            .addPathSegments(SCAN_PATH_SEGMENTS)
            .build()
        val payload = gson.toJson(
            ScanRequest(
                token = token,
                device = deviceInfoProvider.get(),
                supportsNumberMatching = true,
            )
        )
        return Request.Builder()
            .url(scanUrl)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    private fun parseScanBody(body: String): QrLoginScanResult? {
        val response = gson.fromJson(body, ScanResponse::class.java)
        return response?.toResult()
    }

    private fun mapScanHttpStatus(code: Int, body: String): QrLoginScanException = when (code) {
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> QrLoginScanException.TokenRejected
        HTTP_NOT_FOUND -> QrLoginScanException.EndpointMissing
        HTTP_TOO_MANY_REQUESTS -> QrLoginScanException.RateLimited
        HTTP_CONFLICT -> QrLoginScanException.AlreadyScanned
        HTTP_BAD_REQUEST -> QrLoginScanException.MalformedRequest
        HTTP_UPGRADE_REQUIRED -> QrLoginScanException.UpgradeRequired
        else -> {
            WooLog.w(WooLog.T.LOGIN, "QR login scan unexpected HTTP $code: $body")
            QrLoginScanException.HttpError(code)
        }
    }

    private fun mapScanException(throwable: Throwable): Throwable = when (throwable) {
        is QrLoginScanException -> throwable
        is IOException -> {
            WooLog.w(WooLog.T.LOGIN, "QR login scan network failure")
            QrLoginScanException.Network
        }
        is JsonSyntaxException -> {
            WooLog.w(WooLog.T.LOGIN, "QR login scan response was not valid JSON")
            QrLoginScanException.MalformedResponse
        }
        else -> {
            WooLog.e(
                WooLog.T.LOGIN,
                "QR login scan unexpected failure: ${throwable.javaClass.simpleName}"
            )
            QrLoginScanException.Unknown(throwable)
        }
    }

    // endregion

    // region session status

    private fun performSessionStatus(siteUrl: String, sessionId: String): QrLoginSessionStatus {
        val request = buildSessionStatusRequest(siteUrl, sessionId)
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw mapSessionStatusHttpStatus(response.code)
            val parsed = gson.fromJson(response.body.string(), SessionStatusResponse::class.java)
            return parsed.toStatus()
        }
    }

    private fun buildSessionStatusRequest(siteUrl: String, sessionId: String): Request {
        val baseUrl = siteUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("siteUrl could not be parsed by HttpUrl")
        val statusUrl = baseUrl.newBuilder()
            .addPathSegments(SESSION_STATUS_PATH_SEGMENTS)
            .addQueryParameter("session_id", sessionId)
            .build()
        return Request.Builder()
            .url(statusUrl)
            .get()
            // Force-bust any intermediary HTTP cache (OkHttp's shared cache, edge proxy, CDN).
            // Polling responses are state-bearing, so pinning the first response means the app
            // sees `scanned` indefinitely even after the merchant approves on wc-admin.
            .cacheControl(POLL_CACHE_CONTROL)
            .build()
    }

    private fun mapSessionStatusHttpStatus(code: Int): QrLoginSessionStatusException = when (code) {
        HTTP_NOT_FOUND -> QrLoginSessionStatusException.EndpointMissing
        HTTP_TOO_MANY_REQUESTS -> QrLoginSessionStatusException.RateLimited
        else -> QrLoginSessionStatusException.HttpError(code)
    }

    private fun mapSessionStatusException(throwable: Throwable): Throwable = when (throwable) {
        is QrLoginSessionStatusException -> throwable
        is IOException -> QrLoginSessionStatusException.Network
        is JsonSyntaxException -> QrLoginSessionStatusException.MalformedResponse
        else -> QrLoginSessionStatusException.Unknown(throwable)
    }

    // endregion

    // region exchange

    private fun performExchange(siteUrl: String, token: String, exchangeGrant: String): QrLoginCredentials {
        val request = buildExchangeRequest(siteUrl, token, exchangeGrant)
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw mapExchangeHttpStatus(response.code, response.body.string())
            return parseExchangeBody(response.body.string())
                ?: throw QrLoginExchangeException.MalformedResponse
        }
    }

    private fun buildExchangeRequest(siteUrl: String, token: String, exchangeGrant: String): Request {
        val baseUrl = siteUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("siteUrl could not be parsed by HttpUrl")
        val exchangeUrl = baseUrl.newBuilder()
            .addPathSegments(EXCHANGE_PATH_SEGMENTS)
            .build()
        val payload = gson.toJson(ExchangeRequest(token = token, exchangeGrant = exchangeGrant))
        return Request.Builder()
            .url(exchangeUrl)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    private fun parseExchangeBody(body: String): QrLoginCredentials? {
        // Let JsonSyntaxException propagate so mapException converts it to MalformedResponse.
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

    private fun mapExchangeHttpStatus(code: Int, body: String): QrLoginExchangeException = when (code) {
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> QrLoginExchangeException.TokenRejected
        HTTP_NOT_FOUND -> QrLoginExchangeException.EndpointMissing
        HTTP_TOO_MANY_REQUESTS -> QrLoginExchangeException.RateLimited
        HTTP_PRECON_FAILED -> mapPreconditionFailed(body)
        else -> QrLoginExchangeException.HttpError(code)
    }

    /**
     * 412 from /qr-login-exchange splits two ways: `qr_login_not_approved` (the merchant
     * never tapped a number, or tapped the wrong one) vs `invalid_exchange_grant` (the
     * grant nonce we sent doesn't match what the server minted at approve time — race
     * condition the user can act on by starting over).
     */
    private fun mapPreconditionFailed(body: String): QrLoginExchangeException = try {
        val parsed = gson.fromJson(body, ErrorBody::class.java)
        when (parsed?.code) {
            "invalid_exchange_grant" -> QrLoginExchangeException.InvalidExchangeGrant
            "qr_login_not_approved" -> QrLoginExchangeException.NotApproved
            else -> QrLoginExchangeException.HttpError(HTTP_PRECON_FAILED)
        }
    } catch (e: JsonSyntaxException) {
        WooLog.w(WooLog.T.LOGIN, "QR login exchange 412 body was not JSON: ${e.message}")
        QrLoginExchangeException.HttpError(HTTP_PRECON_FAILED)
    }

    private fun mapExchangeException(throwable: Throwable): Throwable = when (throwable) {
        is QrLoginExchangeException -> throwable
        is IOException -> {
            WooLog.w(WooLog.T.LOGIN, "QR login exchange network failure")
            QrLoginExchangeException.Network
        }
        is JsonSyntaxException -> {
            WooLog.w(WooLog.T.LOGIN, "QR login exchange response was not valid JSON")
            QrLoginExchangeException.MalformedResponse
        }
        else -> {
            WooLog.e(
                WooLog.T.LOGIN,
                "QR login exchange unexpected failure: ${throwable.javaClass.simpleName}"
            )
            QrLoginExchangeException.Unknown(throwable)
        }
    }

    // endregion

    // region request / response shapes

    private data class ScanRequest(
        val token: String,
        val device: QrLoginDeviceInfo,
        @SerializedName("supports_number_matching") val supportsNumberMatching: Boolean,
    )

    private data class ScanResponse(
        @SerializedName("session_id") val sessionId: String?,
        @SerializedName("real_number") val realNumber: String?,
        @SerializedName("expires_in") val expiresIn: Int?,
    ) {
        fun toResult(): QrLoginScanResult? {
            val session = sessionId?.takeIf { it.isNotBlank() } ?: return null
            val number = realNumber?.takeIf { it.isNotBlank() } ?: return null
            val ttl = expiresIn ?: return null
            return QrLoginScanResult(
                sessionId = session,
                realNumber = number,
                expiresInSeconds = ttl,
            )
        }
    }

    private data class SessionStatusResponse(
        val state: String?,
        @SerializedName("exchange_grant") val exchangeGrant: String?,
    ) {
        fun toStatus(): QrLoginSessionStatus = when (state) {
            "scanned" -> QrLoginSessionStatus.Scanned
            "approved" -> approvedOrFailClosed()
            "rejected" -> QrLoginSessionStatus.Rejected
            "expired" -> QrLoginSessionStatus.Expired
            // Treat unknown states defensively as Expired so the UI shows a terminal screen
            // rather than spinning indefinitely on a state the app doesn't understand.
            else -> {
                WooLog.w(
                    WooLog.T.LOGIN,
                    "$QR_LOGIN_UNKNOWN_SESSION_STATE_LOG_ID: ${state ?: "(missing)"}"
                )
                QrLoginSessionStatus.Expired
            }
        }

        // Approved without a grant shouldn't be possible against a Task-7 server, but if
        // it ever happens we fail closed rather than continuing to poll forever.
        private fun approvedOrFailClosed(): QrLoginSessionStatus =
            exchangeGrant
                ?.takeIf { it.isNotBlank() }
                ?.let { QrLoginSessionStatus.Approved(it) }
                ?: run {
                    WooLog.w(
                        WooLog.T.LOGIN,
                        "$QR_LOGIN_APPROVED_NO_GRANT_LOG_ID: approved session-status missing exchange_grant"
                    )
                    QrLoginSessionStatus.Expired
                }
    }

    private data class ExchangeRequest(
        val token: String,
        @SerializedName("exchange_grant") val exchangeGrant: String,
    )

    private data class ExchangeResponse(
        @SerializedName("user_login") val userLogin: String?,
        @SerializedName("site_url") val siteUrl: String?,
        @SerializedName("application_password") val applicationPassword: String?
    ) {
        fun toCredentials(): QrLoginCredentials? {
            val user = userLogin?.takeIf { it.isNotBlank() } ?: return null
            if (siteUrl.isNullOrBlank()) return null
            val password = applicationPassword?.takeIf { it.isNotBlank() } ?: return null
            return QrLoginCredentials(userLogin = user, applicationPassword = password)
        }

        fun missingFields(): String = buildList {
            if (userLogin.isNullOrBlank()) add("user_login")
            if (siteUrl.isNullOrBlank()) add("site_url")
            if (applicationPassword.isNullOrBlank()) add("application_password")
        }.joinToString(",").ifEmpty { "(none)" }
    }

    /** Used to disambiguate 412s from the exchange endpoint by their `code` field. */
    private data class ErrorBody(val code: String?)

    // endregion

    private companion object {
        const val SCAN_PATH_SEGMENTS = "wp-json/wc-admin/mobile-app/qr-login-scan"
        const val SESSION_STATUS_PATH_SEGMENTS = "wp-json/wc-admin/mobile-app/qr-login-session-status"
        const val EXCHANGE_PATH_SEGMENTS = "wp-json/wc-admin/mobile-app/qr-login-exchange"
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_CONFLICT = 409
        const val HTTP_UPGRADE_REQUIRED = 426
        const val QR_LOGIN_APPROVED_NO_GRANT_LOG_ID = "QR_LOGIN_APPROVED_NO_GRANT"
        const val QR_LOGIN_UNKNOWN_SESSION_STATE_LOG_ID = "QR_LOGIN_UNKNOWN_SESSION_STATE"
        val POLL_CACHE_CONTROL = CacheControl.Builder()
            .noCache()
            .noStore()
            .build()
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

data class QrLoginCredentials(
    val userLogin: String,
    val applicationPassword: String,
) {
    override fun toString(): String =
        "QrLoginCredentials(userLogin=$userLogin, applicationPassword=***)"
}

/**
 * Successful response from `/qr-login-scan`. The [realNumber] is what the merchant must tap
 * on wc-admin to advance the flow; [sessionId] is the handle the app polls with.
 */
data class QrLoginScanResult(
    val sessionId: String,
    val realNumber: String,
    val expiresInSeconds: Int,
)

/** Result of a single `/qr-login-session-status` poll. */
sealed interface QrLoginSessionStatus {
    /** Waiting on the merchant to tap a number on wc-admin. Keep polling. */
    data object Scanned : QrLoginSessionStatus

    /** Merchant tapped the right number. [grant] is the nonce required by `/qr-login-exchange`. */
    data class Approved(val grant: String) : QrLoginSessionStatus

    /** Merchant tapped a wrong number or cancelled. Terminal. */
    data object Rejected : QrLoginSessionStatus

    /** 90-second window elapsed without an approval. Terminal. */
    data object Expired : QrLoginSessionStatus
}

sealed class QrLoginScanException(message: String) : Exception(message) {
    data object TokenRejected : QrLoginScanException("Token was rejected by the site")
    data object AlreadyScanned : QrLoginScanException("Token has already been scanned")
    data object EndpointMissing : QrLoginScanException("Scan endpoint not available on the site")
    data object RateLimited : QrLoginScanException("Rate limit hit on the scan endpoint")
    data object Network : QrLoginScanException("Network failure during scan")
    data object MalformedRequest : QrLoginScanException("Scan request body was rejected by the site")
    data object MalformedResponse : QrLoginScanException("Scan response was malformed")
    data object UpgradeRequired : QrLoginScanException("Merchant site requires a newer protocol")
    data class HttpError(val code: Int) : QrLoginScanException("HTTP $code from scan endpoint")
    data class Unknown(val original: Throwable) :
        QrLoginScanException("Unknown scan failure: ${original.javaClass.simpleName}")
}

sealed class QrLoginSessionStatusException(message: String) : Exception(message) {
    data object EndpointMissing : QrLoginSessionStatusException("Session-status endpoint not available")
    data object RateLimited : QrLoginSessionStatusException("Rate limit hit on session-status")
    data object Network : QrLoginSessionStatusException("Network failure during session-status poll")
    data object MalformedResponse : QrLoginSessionStatusException("Session-status response was malformed")
    data class HttpError(val code: Int) : QrLoginSessionStatusException("HTTP $code from session-status")
    data class Unknown(val original: Throwable) :
        QrLoginSessionStatusException("Unknown session-status failure: ${original.javaClass.simpleName}")
}

sealed class QrLoginExchangeException(message: String) : Exception(message) {
    data object TokenRejected : QrLoginExchangeException("Token was rejected by the site")
    data object EndpointMissing : QrLoginExchangeException("Exchange endpoint not available on the site")
    data object RateLimited : QrLoginExchangeException("Rate limit hit on the exchange endpoint")
    data object Network : QrLoginExchangeException("Network failure during exchange")
    data object MalformedResponse : QrLoginExchangeException("Exchange response was malformed")
    data object NotApproved :
        QrLoginExchangeException("Exchange called before merchant approved the number match")
    data object InvalidExchangeGrant :
        QrLoginExchangeException("Exchange grant nonce did not match the server-side value")
    data class HttpError(val code: Int) : QrLoginExchangeException("HTTP $code from exchange endpoint")
    data class Unknown(val original: Throwable) :
        QrLoginExchangeException("Unknown exchange failure: ${original.javaClass.simpleName}")
}
