package com.woocommerce.android.network.qrlogin

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WpComQrLoginRestClientTest : BaseUnitTest() {

    private var lastRequest: Request? = null
    private var responder: (Request) -> Response = { ok(it, DEFAULT_SCAN_BODY) }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            Interceptor { chain ->
                lastRequest = chain.request()
                responder(chain.request())
            }
        )
        .build()

    private val appSecrets = AppSecrets(CLIENT_ID, CLIENT_SECRET)

    private lateinit var client: WpComQrLoginRestClient

    @Before
    fun setUp() {
        client = WpComQrLoginRestClient(
            okHttpClient = okHttpClient,
            gson = Gson(),
            dispatchers = coroutinesTestRule.testDispatchers,
            appSecrets = appSecrets,
        )
    }

    // region scan

    @Test
    fun `given scan call, when executed, then POST hits public-api with creds, token, encrypted, capability`() =
        testBlocking {
            client.scan(token = "tok-42", encrypted = "enc-99")

            val request = requireNotNull(lastRequest)
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.url.toString())
                .isEqualTo("https://public-api.wordpress.com/wpcom/v2/auth/qr-code-app/scan/")
            val parsed = Gson().fromJson(readBody(request), JsonObject::class.java)
            assertThat(parsed.get("client_id").asString).isEqualTo(CLIENT_ID)
            assertThat(parsed.get("client_secret").asString).isEqualTo(CLIENT_SECRET)
            assertThat(parsed.get("token").asString).isEqualTo("tok-42")
            assertThat(parsed.get("encrypted").asString).isEqualTo("enc-99")
            assertThat(parsed.get("supports_number_matching").asBoolean).isTrue()
            // Device payload is the wc-admin flow's contract; wp.com does not consume it.
            assertThat(parsed.has("device")).isFalse()
        }

    @Test
    fun `given 200 valid scan body, when scan, then returns session, number, ttl, email`() = testBlocking {
        val result = client.scan("tok", "enc")

        assertThat(result.getOrNull()).isEqualTo(
            WpComQrLoginScanResult(
                sessionId = "sess-1",
                realNumber = "247",
                expiresInSeconds = 90,
                userEmail = "user@example.com",
            )
        )
    }

    @Test
    fun `given 200 missing user_email, when scan, then MalformedResponse`() = testBlocking {
        responder = { ok(it, """{"session_id":"sess-1","real_number":"247","expires_in":90}""") }

        val result = client.scan("tok", "enc")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginScanException.MalformedResponse)
    }

    @Test
    fun `given 403, when scan, then RestForbidden`() = testBlocking {
        responder = { respond(it, code = 403) }

        val result = client.scan("tok", "enc")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginScanException.RestForbidden)
    }

    @Test
    fun `given 400 with no_number_matching code, when scan, then NoNumberMatching`() = testBlocking {
        responder = { respond(it, code = 400, body = """{"code":"no_number_matching"}""") }

        val result = client.scan("tok", "enc")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginScanException.NoNumberMatching)
    }

    @Test
    fun `given 400 with unknown code, when scan, then HttpError 400`() = testBlocking {
        responder = { respond(it, code = 400, body = """{"code":"something_else"}""") }

        val result = client.scan("tok", "enc")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginScanException.HttpError(400))
    }

    @Test
    fun `given 404, when scan, then SessionNotFound`() = testBlocking {
        responder = { respond(it, code = 404) }

        val result = client.scan("tok", "enc")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginScanException.SessionNotFound)
    }

    @Test
    fun `given 409, when scan, then AlreadyScanned`() = testBlocking {
        responder = { respond(it, code = 409) }

        val result = client.scan("tok", "enc")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginScanException.AlreadyScanned)
    }

    @Test
    fun `given network IOException, when scan, then Network`() = testBlocking {
        responder = { throw IOException("connection refused") }

        val result = client.scan("tok", "enc")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginScanException.Network)
    }

    @Test
    fun `given CancellationException during scan, when called, then propagates unwrapped`() = testBlocking {
        responder = { throw CancellationException("cancelled") }

        var thrown: Throwable? = null
        try {
            client.scan("tok", "enc")
        } catch (t: Throwable) {
            thrown = t
        }

        assertThat(thrown).isInstanceOf(CancellationException::class.java)
    }

    // endregion

    // region session status

    @Test
    fun `given session-status, when GET, then includes session_id and creds in query and disables cache`() =
        testBlocking {
            responder = { ok(it, """{"status":"scanned"}""") }

            client.checkSessionStatus("sess-99")

            val request = requireNotNull(lastRequest)
            assertThat(request.method).isEqualTo("GET")
            assertThat(request.url.toString()).contains(
                "https://public-api.wordpress.com/wpcom/v2/auth/qr-code-app/session-status",
                "session_id=sess-99",
                "client_id=$CLIENT_ID",
                "client_secret=$CLIENT_SECRET",
            )
            assertThat(request.header("Cache-Control"))
                .contains("no-cache")
                .contains("no-store")
        }

    @Test
    fun `given status=scanned, when session-status, then Scanned`() = testBlocking {
        responder = { ok(it, """{"status":"scanned"}""") }

        val result = client.checkSessionStatus("sess-1")

        assertThat(result.getOrNull()).isEqualTo(WpComQrLoginSessionStatus.Scanned)
    }

    @Test
    fun `given status=approved with grant, when session-status, then Approved`() = testBlocking {
        responder = { ok(it, """{"status":"approved","exchange_grant":"grant-99"}""") }

        val result = client.checkSessionStatus("sess-1")

        assertThat(result.getOrNull()).isEqualTo(WpComQrLoginSessionStatus.Approved("grant-99"))
    }

    @Test
    fun `given status=approved without grant, when session-status, then fails closed as Expired`() = testBlocking {
        responder = { ok(it, """{"status":"approved"}""") }

        val result = client.checkSessionStatus("sess-1")

        assertThat(result.getOrNull()).isEqualTo(WpComQrLoginSessionStatus.Expired)
    }

    @Test
    fun `given status=rejected, when session-status, then Rejected`() = testBlocking {
        responder = { ok(it, """{"status":"rejected"}""") }

        val result = client.checkSessionStatus("sess-1")

        assertThat(result.getOrNull()).isEqualTo(WpComQrLoginSessionStatus.Rejected)
    }

    @Test
    fun `given status=expired, when session-status, then Expired`() = testBlocking {
        responder = { ok(it, """{"status":"expired"}""") }

        val result = client.checkSessionStatus("sess-1")

        assertThat(result.getOrNull()).isEqualTo(WpComQrLoginSessionStatus.Expired)
    }

    @Test
    fun `given status=consumed, when session-status, then Consumed`() = testBlocking {
        responder = { ok(it, """{"status":"consumed"}""") }

        val result = client.checkSessionStatus("sess-1")

        assertThat(result.getOrNull()).isEqualTo(WpComQrLoginSessionStatus.Consumed)
    }

    @Test
    fun `given unknown status, when session-status, then fails closed as Expired`() = testBlocking {
        responder = { ok(it, """{"status":"some_future_state"}""") }

        val result = client.checkSessionStatus("sess-1")

        assertThat(result.getOrNull()).isEqualTo(WpComQrLoginSessionStatus.Expired)
    }

    @Test
    fun `given 404, when session-status, then Expired (cache TTL elapsed)`() = testBlocking {
        // wp.com keeps terminal records for ~2 minutes; once evicted the lookup 404s. Per spec
        // that means the session timed out — we treat it as Expired rather than a hard error.
        responder = { respond(it, code = 404) }

        val result = client.checkSessionStatus("sess-1")

        assertThat(result.getOrNull()).isEqualTo(WpComQrLoginSessionStatus.Expired)
    }

    @Test
    fun `given 429, when session-status, then RateLimited`() = testBlocking {
        responder = { respond(it, code = 429) }

        val result = client.checkSessionStatus("sess-1")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginSessionStatusException.RateLimited)
    }

    @Test
    fun `given network IOException, when session-status, then Network`() = testBlocking {
        responder = { throw IOException("disconnect") }

        val result = client.checkSessionStatus("sess-1")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginSessionStatusException.Network)
    }

    // endregion

    // region exchange

    @Test
    fun `given exchange, when POST, then body has creds, token, encrypted, exchange_grant`() = testBlocking {
        responder = { ok(it, DEFAULT_EXCHANGE_BODY) }

        client.exchange(token = "tok-42", encrypted = "enc-99", exchangeGrant = "grant-xyz")

        val request = requireNotNull(lastRequest)
        assertThat(request.url.toString())
            .isEqualTo("https://public-api.wordpress.com/wpcom/v2/auth/qr-code-app/exchange/")
        val parsed = Gson().fromJson(readBody(request), JsonObject::class.java)
        assertThat(parsed.get("client_id").asString).isEqualTo(CLIENT_ID)
        assertThat(parsed.get("client_secret").asString).isEqualTo(CLIENT_SECRET)
        assertThat(parsed.get("token").asString).isEqualTo("tok-42")
        assertThat(parsed.get("encrypted").asString).isEqualTo("enc-99")
        assertThat(parsed.get("exchange_grant").asString).isEqualTo("grant-xyz")
    }

    @Test
    fun `given 200 valid exchange body, when exchange, then returns magic link`() = testBlocking {
        responder = { ok(it, DEFAULT_EXCHANGE_BODY) }

        val result = client.exchange("tok", "enc", "grant")

        assertThat(result.getOrNull())
            .isEqualTo(WpComQrLoginExchangeResult(magicLinkUrl = "https://wordpress.com/log-in/link/use/abc"))
    }

    @Test
    fun `given valid exchange result, when toString, then magic link is redacted`() = testBlocking {
        responder = { ok(it, DEFAULT_EXCHANGE_BODY) }

        val result = requireNotNull(client.exchange("tok", "enc", "grant").getOrNull())

        assertThat(result.toString()).doesNotContain("https://wordpress.com")
    }

    @Test
    fun `given 200 missing magic_link_url, when exchange, then MalformedResponse`() = testBlocking {
        responder = { ok(it, """{}""") }

        val result = client.exchange("tok", "enc", "grant")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginExchangeException.MalformedResponse)
    }

    @Test
    fun `given 412 with qr_login_not_approved, when exchange, then NotApproved`() = testBlocking {
        responder = { respond(it, code = 412, body = """{"code":"qr_login_not_approved"}""") }

        val result = client.exchange("tok", "enc", "grant")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginExchangeException.NotApproved)
    }

    @Test
    fun `given 412 with invalid_exchange_grant, when exchange, then InvalidExchangeGrant`() = testBlocking {
        responder = { respond(it, code = 412, body = """{"code":"invalid_exchange_grant"}""") }

        val result = client.exchange("tok", "enc", "grant")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginExchangeException.InvalidExchangeGrant)
    }

    @Test
    fun `given 412 with unknown code, when exchange, then HttpError 412`() = testBlocking {
        responder = { respond(it, code = 412, body = """{"code":"other"}""") }

        val result = client.exchange("tok", "enc", "grant")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginExchangeException.HttpError(412))
    }

    @Test
    fun `given 500 with already_consumed, when exchange, then AlreadyConsumed`() = testBlocking {
        responder = { respond(it, code = 500, body = """{"code":"already_consumed"}""") }

        val result = client.exchange("tok", "enc", "grant")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginExchangeException.AlreadyConsumed)
    }

    @Test
    fun `given 500 without already_consumed code, when exchange, then HttpError 500`() = testBlocking {
        responder = { respond(it, code = 500, body = """{"code":"server_error"}""") }

        val result = client.exchange("tok", "enc", "grant")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginExchangeException.HttpError(500))
    }

    @Test
    fun `given 404, when exchange, then SessionNotFound`() = testBlocking {
        responder = { respond(it, code = 404) }

        val result = client.exchange("tok", "enc", "grant")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginExchangeException.SessionNotFound)
    }

    @Test
    fun `given network IOException, when exchange, then Network`() = testBlocking {
        responder = { throw IOException("connection refused") }

        val result = client.exchange("tok", "enc", "grant")

        assertThat(result.exceptionOrNull()).isEqualTo(WpComQrLoginExchangeException.Network)
    }

    // endregion

    private fun ok(request: Request, body: String): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody(JSON_MEDIA_TYPE))
        .build()

    private fun respond(request: Request, code: Int, body: String = ""): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("status-$code")
        .body(body.toResponseBody(JSON_MEDIA_TYPE))
        .build()

    private fun readBody(request: Request): String =
        Buffer().also { requireNotNull(request.body).writeTo(it) }.readUtf8()

    private companion object {
        const val CLIENT_ID = "test-client-id"
        const val CLIENT_SECRET = "test-client-secret"
        const val DEFAULT_SCAN_BODY =
            """{"session_id":"sess-1","real_number":"247","expires_in":90,"user_email":"user@example.com"}"""
        const val DEFAULT_EXCHANGE_BODY =
            """{"magic_link_url":"https://wordpress.com/log-in/link/use/abc"}"""
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
