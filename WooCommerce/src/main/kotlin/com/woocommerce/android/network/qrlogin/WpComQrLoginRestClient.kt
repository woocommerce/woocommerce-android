package com.woocommerce.android.network.qrlogin

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import java.io.IOException
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_PRECON_FAILED
import javax.inject.Inject
import javax.inject.Named

/**
 * Calls the wp.com QR-app-login endpoints. Three endpoints, same shape as the wc-admin variant
 * in [QrLoginRestClient] but against `public-api.wordpress.com` and authenticated with the app's
 * OAuth `client_id` / `client_secret` rather than the QR token alone:
 *
 *   - [scan] reports the QR was scanned. Server picks the real number for number-matching and
 *     returns a session id, the user's email, and the 90-second TTL.
 *   - [checkSessionStatus] polls for the user's tap in the browser. Returns the `exchange_grant`
 *     once approved.
 *   - [exchange] swaps the grant for a single-use wp.com magic-link URL the app then opens.
 *
 * On success [exchange] returns [WpComQrLoginExchangeResult] containing the magic link. Errors
 * are mapped to [WpComQrLoginExchangeException] variants. The 500 `already_consumed` case
 * surfaces as [WpComQrLoginExchangeException.AlreadyConsumed] — we don't retry the exchange
 * call, so this means a different actor consumed the grant first.
 */
class WpComQrLoginRestClient @Inject constructor(
    @Named("custom-ssl") private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val dispatchers: CoroutineDispatchers,
    private val appSecrets: AppSecrets,
) {

    suspend fun scan(token: String, encrypted: String): Result<WpComQrLoginScanResult> =
        withContext(dispatchers.io) {
            try {
                Result.success(performScan(token, encrypted))
            } catch (ce: CancellationException) {
                throw ce
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Result.failure(mapScanException(e))
            }
        }

    suspend fun checkSessionStatus(sessionId: String): Result<WpComQrLoginSessionStatus> =
        withContext(dispatchers.io) {
            try {
                Result.success(performSessionStatus(sessionId))
            } catch (ce: CancellationException) {
                throw ce
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Result.failure(mapSessionStatusException(e))
            }
        }

    suspend fun exchange(
        token: String,
        encrypted: String,
        exchangeGrant: String,
    ): Result<WpComQrLoginExchangeResult> =
        withContext(dispatchers.io) {
            try {
                Result.success(performExchange(token, encrypted, exchangeGrant))
            } catch (ce: CancellationException) {
                throw ce
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Result.failure(mapExchangeException(e))
            }
        }

    // region scan

    private fun performScan(token: String, encrypted: String): WpComQrLoginScanResult {
        val request = buildScanRequest(token, encrypted)
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw mapScanHttpStatus(response.code, response.body.string())
            return parseScanBody(response.body.string())
                ?: throw WpComQrLoginScanException.MalformedResponse
        }
    }

    private fun buildScanRequest(token: String, encrypted: String): Request {
        val url = WPCOMV2.auth.qr_code_app.scan.url.toHttpUrl()
        val payload = gson.toJson(
            ScanRequest(
                clientId = appSecrets.appId,
                clientSecret = appSecrets.appSecret,
                token = token,
                encrypted = encrypted,
                supportsNumberMatching = true,
            )
        )
        return Request.Builder()
            .url(url)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    private fun parseScanBody(body: String): WpComQrLoginScanResult? {
        val response = gson.fromJson(body, ScanResponse::class.java)
        return response?.toResult()
    }

    private fun mapScanHttpStatus(code: Int, body: String): WpComQrLoginScanException = when (code) {
        HTTP_FORBIDDEN -> WpComQrLoginScanException.RestForbidden
        HTTP_NOT_FOUND -> WpComQrLoginScanException.SessionNotFound
        HTTP_CONFLICT -> WpComQrLoginScanException.AlreadyScanned
        HTTP_BAD_REQUEST -> mapScanBadRequest(body)
        HTTP_TOO_MANY_REQUESTS -> WpComQrLoginScanException.RateLimited
        else -> {
            // Body intentionally not logged — a successful scan response carries `session_id` /
            // `user_email`, and an unexpected status here could surface those alongside diagnostics.
            WooLog.w(WooLog.T.LOGIN, "wp.com QR scan unexpected HTTP $code")
            WpComQrLoginScanException.HttpError(code)
        }
    }

    private fun mapScanBadRequest(body: String): WpComQrLoginScanException = try {
        when (gson.fromJson(body, ErrorBody::class.java)?.code) {
            "no_number_matching" -> WpComQrLoginScanException.NoNumberMatching
            else -> WpComQrLoginScanException.HttpError(HTTP_BAD_REQUEST)
        }
    } catch (e: JsonSyntaxException) {
        WooLog.w(WooLog.T.LOGIN, "wp.com QR scan 400 body was not JSON: ${e.message}")
        WpComQrLoginScanException.HttpError(HTTP_BAD_REQUEST)
    }

    private fun mapScanException(throwable: Throwable): Throwable = when (throwable) {
        is WpComQrLoginScanException -> throwable
        is IOException -> {
            WooLog.w(WooLog.T.LOGIN, "wp.com QR scan network failure")
            WpComQrLoginScanException.Network
        }
        is JsonSyntaxException -> {
            WooLog.w(WooLog.T.LOGIN, "wp.com QR scan response was not valid JSON")
            WpComQrLoginScanException.MalformedResponse
        }
        else -> {
            WooLog.e(WooLog.T.LOGIN, "wp.com QR scan unexpected failure: ${throwable.javaClass.simpleName}")
            WpComQrLoginScanException.Unknown(throwable)
        }
    }

    // endregion

    // region session status

    private fun performSessionStatus(sessionId: String): WpComQrLoginSessionStatus {
        val request = buildSessionStatusRequest(sessionId)
        okHttpClient.newCall(request).execute().use { response ->
            // The server keeps `rejected`/`consumed` records for ~2 minutes after their terminal
            // event; once the cache TTL elapses the row is gone and we get a 404. Per the wp.com
            // spec that means the session genuinely timed out — surface as `Expired` rather than
            // a hard error so the user sees the timeout copy instead of a generic failure.
            if (response.code == HTTP_NOT_FOUND) return WpComQrLoginSessionStatus.Expired
            if (!response.isSuccessful) throw mapSessionStatusHttpStatus(response.code)
            val parsed = gson.fromJson(response.body.string(), SessionStatusResponse::class.java)
            // Log only the parsed shape — the raw body carries `exchange_grant`, a single-use
            // nonce that swaps for a magic-link URL. The same risk applies to `session_id` and
            // any other secrets that may surface in the response. Never log the raw body.
            WooLog.d(
                WooLog.T.LOGIN,
                "wp.com QR session-status: state=${parsed.status}, hasGrant=${!parsed.exchangeGrant.isNullOrBlank()}"
            )
            return parsed.toStatus()
        }
    }

    private fun buildSessionStatusRequest(sessionId: String): Request {
        val url = WPCOMV2.auth.qr_code_app.session_status.url.toHttpUrl().newBuilder()
            .addQueryParameter("client_id", appSecrets.appId)
            .addQueryParameter("client_secret", appSecrets.appSecret)
            .addQueryParameter("session_id", sessionId)
            .build()
        return Request.Builder()
            .url(url)
            .get()
            // Polling responses are state-bearing; defeat any intermediate cache so we don't see
            // the same `scanned` indefinitely after the user taps in the browser.
            .cacheControl(POLL_CACHE_CONTROL)
            .build()
    }

    private fun mapSessionStatusHttpStatus(code: Int): WpComQrLoginSessionStatusException = when (code) {
        // Note: 404 is handled as `Expired` upstream and never reaches this mapper.
        HTTP_TOO_MANY_REQUESTS -> WpComQrLoginSessionStatusException.RateLimited
        else -> WpComQrLoginSessionStatusException.HttpError(code)
    }

    private fun mapSessionStatusException(throwable: Throwable): Throwable = when (throwable) {
        is WpComQrLoginSessionStatusException -> throwable
        is IOException -> WpComQrLoginSessionStatusException.Network
        is JsonSyntaxException -> WpComQrLoginSessionStatusException.MalformedResponse
        else -> WpComQrLoginSessionStatusException.Unknown(throwable)
    }

    // endregion

    // region exchange

    private fun performExchange(
        token: String,
        encrypted: String,
        exchangeGrant: String,
    ): WpComQrLoginExchangeResult {
        val request = buildExchangeRequest(token, encrypted, exchangeGrant)
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw mapExchangeHttpStatus(response.code, response.body.string())
            return parseExchangeBody(response.body.string())
                ?: throw WpComQrLoginExchangeException.MalformedResponse
        }
    }

    private fun buildExchangeRequest(token: String, encrypted: String, exchangeGrant: String): Request {
        val url = WPCOMV2.auth.qr_code_app.exchange.url.toHttpUrl()
        val payload = gson.toJson(
            ExchangeRequest(
                clientId = appSecrets.appId,
                clientSecret = appSecrets.appSecret,
                token = token,
                encrypted = encrypted,
                exchangeGrant = exchangeGrant,
            )
        )
        return Request.Builder()
            .url(url)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    private fun parseExchangeBody(body: String): WpComQrLoginExchangeResult? {
        val response = gson.fromJson(body, ExchangeResponse::class.java)
        return response?.toResult()
    }

    private fun mapExchangeHttpStatus(code: Int, body: String): WpComQrLoginExchangeException = when (code) {
        HTTP_PRECON_FAILED -> mapPreconditionFailed(body)
        HTTP_INTERNAL_ERROR -> mapInternalServerError(body, code)
        HTTP_NOT_FOUND -> WpComQrLoginExchangeException.SessionNotFound
        HTTP_TOO_MANY_REQUESTS -> WpComQrLoginExchangeException.RateLimited
        else -> WpComQrLoginExchangeException.HttpError(code)
    }

    /**
     * 412 splits two ways: `qr_login_not_approved` (server hasn't seen the user tap yet) vs
     * `invalid_exchange_grant` (the grant doesn't match what was minted at approve time).
     */
    private fun mapPreconditionFailed(body: String): WpComQrLoginExchangeException = try {
        when (gson.fromJson(body, ErrorBody::class.java)?.code) {
            "qr_login_not_approved" -> WpComQrLoginExchangeException.NotApproved
            "invalid_exchange_grant" -> WpComQrLoginExchangeException.InvalidExchangeGrant
            else -> WpComQrLoginExchangeException.HttpError(HTTP_PRECON_FAILED)
        }
    } catch (e: JsonSyntaxException) {
        WooLog.w(WooLog.T.LOGIN, "wp.com QR exchange 412 body was not JSON: ${e.message}")
        WpComQrLoginExchangeException.HttpError(HTTP_PRECON_FAILED)
    }

    /**
     * 500 with code `already_consumed` means a previous /exchange call (likely from another
     * actor — different device/tab) already swapped the grant. Distinguish from generic 500s
     * so the UI can show "completed elsewhere" copy instead of a retry-friendly server error.
     */
    private fun mapInternalServerError(body: String, code: Int): WpComQrLoginExchangeException = try {
        when (gson.fromJson(body, ErrorBody::class.java)?.code) {
            "already_consumed" -> WpComQrLoginExchangeException.AlreadyConsumed
            else -> WpComQrLoginExchangeException.HttpError(code)
        }
    } catch (e: JsonSyntaxException) {
        WooLog.w(WooLog.T.LOGIN, "wp.com QR exchange 500 body was not JSON: ${e.message}")
        WpComQrLoginExchangeException.HttpError(code)
    }

    private fun mapExchangeException(throwable: Throwable): Throwable = when (throwable) {
        is WpComQrLoginExchangeException -> throwable
        is IOException -> {
            WooLog.w(WooLog.T.LOGIN, "wp.com QR exchange network failure")
            WpComQrLoginExchangeException.Network
        }
        is JsonSyntaxException -> {
            WooLog.w(WooLog.T.LOGIN, "wp.com QR exchange response was not valid JSON")
            WpComQrLoginExchangeException.MalformedResponse
        }
        else -> {
            WooLog.e(WooLog.T.LOGIN, "wp.com QR exchange unexpected failure: ${throwable.javaClass.simpleName}")
            WpComQrLoginExchangeException.Unknown(throwable)
        }
    }

    // endregion

    // region request / response shapes

    private data class ScanRequest(
        @SerializedName("client_id") val clientId: String,
        @SerializedName("client_secret") val clientSecret: String,
        val token: String,
        val encrypted: String,
        @SerializedName("supports_number_matching") val supportsNumberMatching: Boolean,
    )

    private data class ScanResponse(
        @SerializedName("session_id") val sessionId: String?,
        @SerializedName("real_number") val realNumber: String?,
        @SerializedName("expires_in") val expiresIn: Int?,
        @SerializedName("user_email") val userEmail: String?,
    ) {
        fun toResult(): WpComQrLoginScanResult? = WpComQrLoginScanResult(
            sessionId = sessionId?.takeIf { it.isNotBlank() } ?: return null,
            realNumber = realNumber?.takeIf { it.isNotBlank() } ?: return null,
            expiresInSeconds = expiresIn ?: return null,
            userEmail = userEmail?.takeIf { it.isNotBlank() } ?: return null,
        )
    }

    private data class SessionStatusResponse(
        // Accept both `state` (matches AP/wc-admin flow + later spec revisions) and `status`
        // (original wp.com spec). The server appears to use `state`; supporting both means a
        // server-side rename either direction won't silently break number-matching.
        @SerializedName(value = "state", alternate = ["status"]) val status: String?,
        @SerializedName("exchange_grant") val exchangeGrant: String?,
    ) {
        fun toStatus(): WpComQrLoginSessionStatus = when (status) {
            "scanned" -> WpComQrLoginSessionStatus.Scanned
            "approved" -> approvedOrFailClosed()
            "rejected" -> WpComQrLoginSessionStatus.Rejected
            "consumed" -> WpComQrLoginSessionStatus.Consumed
            // Treat unknown states defensively as Expired so the UI shows a terminal screen
            // rather than spinning indefinitely on a state the app doesn't understand.
            else -> WpComQrLoginSessionStatus.Expired
        }

        private fun approvedOrFailClosed(): WpComQrLoginSessionStatus =
            exchangeGrant
                ?.takeIf { it.isNotBlank() }
                ?.let { WpComQrLoginSessionStatus.Approved(it) }
                ?: WpComQrLoginSessionStatus.Expired
    }

    private data class ExchangeRequest(
        @SerializedName("client_id") val clientId: String,
        @SerializedName("client_secret") val clientSecret: String,
        val token: String,
        val encrypted: String,
        @SerializedName("exchange_grant") val exchangeGrant: String,
    )

    private data class ExchangeResponse(
        @SerializedName("magic_link_url") val magicLinkUrl: String?,
    ) {
        fun toResult(): WpComQrLoginExchangeResult? =
            magicLinkUrl?.takeIf { it.isNotBlank() }?.let { WpComQrLoginExchangeResult(magicLinkUrl = it) }
    }

    /** Used to disambiguate 412/500 responses by their `code` field. */
    private data class ErrorBody(val code: String?)

    // endregion

    private companion object {
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_CONFLICT = 409
        val POLL_CACHE_CONTROL = CacheControl.Builder()
            .noCache()
            .noStore()
            .build()
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

/**
 * Successful response from `/scan`. `userEmail` is shown above the number-match tile so the
 * user can verify which account they're signing into before tapping. `realNumber` is the 3-digit
 * tile they must tap in the browser.
 */
data class WpComQrLoginScanResult(
    val sessionId: String,
    val realNumber: String,
    val expiresInSeconds: Int,
    val userEmail: String,
)

/** Result of a single `/session-status` poll. */
sealed interface WpComQrLoginSessionStatus {
    /** Waiting on the user to tap a tile in the browser. Keep polling. */
    data object Scanned : WpComQrLoginSessionStatus

    /** User tapped the matching tile. [grant] is the nonce required by `/exchange`. */
    data class Approved(val grant: String) : WpComQrLoginSessionStatus

    /** User tapped a wrong tile in the browser. Terminal — no retry. */
    data object Rejected : WpComQrLoginSessionStatus

    /**
     * Either the QR code TTL elapsed pre-scan, or the 90-second number-match window elapsed
     * post-scan, or the cached terminal record was evicted (~2 min after the terminal event)
     * and the lookup 404'd. Terminal — no retry.
     */
    data object Expired : WpComQrLoginSessionStatus

    /**
     * The `/exchange` call already succeeded for this session — typically because another
     * device/tab consumed the grant first. Shouldn't normally surface during polling; treat as
     * a terminal "completed elsewhere" state. The app has no magic link to open and the user
     * needs to refresh the browser page to start a fresh sign-in.
     */
    data object Consumed : WpComQrLoginSessionStatus
}

/**
 * Successful response from `/exchange`. The app opens [magicLinkUrl] in the browser, which
 * completes wp.com authentication and issues an OAuth token to the app via the existing
 * magic-link intercept.
 */
data class WpComQrLoginExchangeResult(
    val magicLinkUrl: String,
) {
    // Magic link is single-use; redact in logs / crash reports.
    override fun toString(): String = "WpComQrLoginExchangeResult(magicLinkUrl=***)"
}

sealed class WpComQrLoginScanException(message: String) : Exception(message) {
    /** 403 — invalid `client_id` / `client_secret`. */
    data object RestForbidden : WpComQrLoginScanException("Invalid OAuth credentials for wp.com QR scan")

    /** 400 with `no_number_matching` code — server requires `supports_number_matching=true`. */
    data object NoNumberMatching :
        WpComQrLoginScanException("wp.com QR scan rejected: number-matching capability missing")

    /** 404 — QR code expired before scan reached the server. */
    data object SessionNotFound : WpComQrLoginScanException("wp.com QR session not found (expired pre-scan)")

    /** 409 — another device already scanned this QR. */
    data object AlreadyScanned : WpComQrLoginScanException("wp.com QR token has already been scanned elsewhere")

    data object RateLimited : WpComQrLoginScanException("Rate limit hit on wp.com QR scan")
    data object Network : WpComQrLoginScanException("Network failure during wp.com QR scan")
    data object MalformedResponse : WpComQrLoginScanException("wp.com QR scan response was malformed")
    data class HttpError(val code: Int) : WpComQrLoginScanException("HTTP $code from wp.com QR scan")
    data class Unknown(val original: Throwable) :
        WpComQrLoginScanException("Unknown wp.com QR scan failure: ${original.javaClass.simpleName}")
}

sealed class WpComQrLoginSessionStatusException(message: String) : Exception(message) {
    data object RateLimited : WpComQrLoginSessionStatusException("Rate limit hit on wp.com QR session-status")
    data object Network : WpComQrLoginSessionStatusException("Network failure during wp.com QR session-status")
    data object MalformedResponse :
        WpComQrLoginSessionStatusException("wp.com QR session-status response was malformed")
    data class HttpError(val code: Int) :
        WpComQrLoginSessionStatusException("HTTP $code from wp.com QR session-status")
    data class Unknown(val original: Throwable) : WpComQrLoginSessionStatusException(
        "Unknown wp.com QR session-status failure: ${original.javaClass.simpleName}"
    )
}

sealed class WpComQrLoginExchangeException(message: String) : Exception(message) {
    /** 412 `qr_login_not_approved` — exchange called before approval landed. */
    data object NotApproved :
        WpComQrLoginExchangeException("wp.com QR exchange called before user approved")

    /** 412 `invalid_exchange_grant` — grant nonce did not match server-side value. */
    data object InvalidExchangeGrant :
        WpComQrLoginExchangeException("wp.com QR exchange grant nonce did not match")

    /** 500 `already_consumed` — another actor used this grant first. */
    data object AlreadyConsumed :
        WpComQrLoginExchangeException("wp.com QR exchange grant already consumed by another device")

    data object SessionNotFound : WpComQrLoginExchangeException("wp.com QR exchange: session not found")
    data object RateLimited : WpComQrLoginExchangeException("Rate limit hit on wp.com QR exchange")
    data object Network : WpComQrLoginExchangeException("Network failure during wp.com QR exchange")
    data object MalformedResponse : WpComQrLoginExchangeException("wp.com QR exchange response was malformed")
    data class HttpError(val code: Int) : WpComQrLoginExchangeException("HTTP $code from wp.com QR exchange")
    data class Unknown(val original: Throwable) :
        WpComQrLoginExchangeException("Unknown wp.com QR exchange failure: ${original.javaClass.simpleName}")
}
