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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginRestClientTest : BaseUnitTest() {

    private var lastRequest: Request? = null
    private var responder: (Request) -> Response = { ok(it, DEFAULT_EXCHANGE_BODY) }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            Interceptor { chain ->
                lastRequest = chain.request()
                responder(chain.request())
            }
        )
        .build()

    private lateinit var client: QrLoginRestClient

    private val fakeDeviceInfoProvider = mock<QrLoginDeviceInfoProvider> {
        on { get() } doReturn QrLoginDeviceInfo(
            os = "Android",
            osVersion = "14",
            model = "Pixel 8 Pro",
            brand = "google",
            appVersion = "24.7.0",
        )
    }

    @Before
    fun setUp() {
        client = QrLoginRestClient(
            okHttpClient = okHttpClient,
            gson = Gson(),
            dispatchers = coroutinesTestRule.testDispatchers,
            deviceInfoProvider = fakeDeviceInfoProvider,
        )
    }

    // region exchange

    @Test
    fun `given 200 with valid body, when exchange, then returns credentials`() = testBlocking {
        val result = client.exchange("https://store.example", "tok", "grant-1")

        assertThat(result.getOrNull()).isEqualTo(
            QrLoginCredentials(userLogin = "admin", applicationPassword = "ap-secret")
        )
    }

    @Test
    fun `given valid credentials, when toString is called, then password is redacted`() = testBlocking {
        val credentials = requireNotNull(client.exchange("https://store.example", "tok", "g").getOrNull())

        assertThat(credentials.toString()).doesNotContain("ap-secret")
    }

    @Test
    fun `given siteUrl with trailing slash, when exchange, then request path is normalized`() = testBlocking {
        client.exchange("https://store.example/", "tok", "g")

        assertThat(lastRequest?.url.toString())
            .isEqualTo("https://store.example/wp-json/wc-admin/mobile-app/qr-login-exchange")
    }

    @Test
    fun `given exchange call, when executed, then JSON body contains token and exchange_grant and not device`() =
        testBlocking {
            client.exchange("https://store.example", "tok-42", "grant-xyz")

            val body = readBody(requireNotNull(lastRequest))
            val parsed = Gson().fromJson(body, JsonObject::class.java)
            assertThat(parsed.get("token").asString).isEqualTo("tok-42")
            assertThat(parsed.get("exchange_grant").asString).isEqualTo("grant-xyz")
            // The post-Task-7 contract sources `device` from /qr-login-scan, so it must NOT
            // appear here — sending it would be silently dropped server-side, but we want to
            // be explicit so a regression is loud.
            assertThat(parsed.has("device")).isFalse()
        }

    @Test
    fun `given 401, when exchange, then TokenRejected`() = testBlocking {
        responder = { respond(it, code = 401) }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.TokenRejected)
    }

    @Test
    fun `given 403, when exchange, then TokenRejected`() = testBlocking {
        responder = { respond(it, code = 403) }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.TokenRejected)
    }

    @Test
    fun `given 404, when exchange, then EndpointMissing`() = testBlocking {
        responder = { respond(it, code = 404) }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.EndpointMissing)
    }

    @Test
    fun `given 429, when exchange, then RateLimited`() = testBlocking {
        responder = { respond(it, code = 429) }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.RateLimited)
    }

    @Test
    fun `given 412 with qr_login_not_approved code, when exchange, then NotApproved`() = testBlocking {
        responder = { respond(it, code = 412, body = """{"code":"qr_login_not_approved"}""") }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.NotApproved)
    }

    @Test
    fun `given 412 with invalid_exchange_grant code, when exchange, then InvalidExchangeGrant`() = testBlocking {
        responder = { respond(it, code = 412, body = """{"code":"invalid_exchange_grant"}""") }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.InvalidExchangeGrant)
    }

    @Test
    fun `given 412 with unknown code, when exchange, then HttpError 412`() = testBlocking {
        responder = { respond(it, code = 412, body = """{"code":"something_else"}""") }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.HttpError(412))
    }

    @Test
    fun `given 500, when exchange, then HttpError with status code`() = testBlocking {
        responder = { respond(it, code = 500) }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.HttpError(500))
    }

    @Test
    fun `given network IOException, when exchange, then Network`() = testBlocking {
        responder = { throw IOException("connection refused") }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.Network)
    }

    @Test
    fun `given 200 with non-JSON body, when exchange, then MalformedResponse`() = testBlocking {
        responder = { ok(it, body = "not json at all") }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.MalformedResponse)
    }

    @Test
    fun `given 200 missing user_login, when exchange, then MalformedResponse`() = testBlocking {
        responder = { ok(it, body = """{"site_url":"https://x","application_password":"ap"}""") }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.MalformedResponse)
    }

    @Test
    fun `given 200 with null JSON literal body, when exchange, then MalformedResponse`() = testBlocking {
        responder = { ok(it, body = "null") }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.MalformedResponse)
    }

    @Test
    fun `given 200 with empty body, when exchange, then MalformedResponse`() = testBlocking {
        responder = { ok(it, body = "") }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.MalformedResponse)
    }

    @Test
    fun `given 200 with blank application_password, when exchange, then MalformedResponse`() = testBlocking {
        responder = {
            ok(it, body = """{"user_login":"admin","site_url":"https://x","application_password":"  "}""")
        }

        val result = client.exchange("https://store.example", "tok", "g")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.MalformedResponse)
    }

    @Test
    fun `given unexpected RuntimeException, when exchange, then wraps in Unknown preserving cause`() = testBlocking {
        val boom = RuntimeException("oops")
        responder = { throw boom }

        val result = client.exchange("https://store.example", "tok", "g")

        val failure = result.exceptionOrNull()
        assertThat(failure).isInstanceOf(QrLoginExchangeException.Unknown::class.java)
        assertThat((failure as QrLoginExchangeException.Unknown).original).isEqualTo(boom)
    }

    @Test
    fun `given CancellationException during exchange, when called, then it propagates unwrapped`() = testBlocking {
        responder = { throw CancellationException("cancelled") }

        var thrown: Throwable? = null
        try {
            client.exchange("https://store.example", "tok", "g")
        } catch (t: Throwable) {
            thrown = t
        }

        assertThat(thrown).isInstanceOf(CancellationException::class.java)
    }

    // endregion

    // region scan

    @Test
    fun `given scan call, when executed, then request is POST with token, device, and capability flag`() =
        testBlocking {
            responder = { ok(it, DEFAULT_SCAN_BODY) }

            client.scan("https://store.example", "tok-42")

            val request = requireNotNull(lastRequest)
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.url.toString())
                .isEqualTo("https://store.example/wp-json/wc-admin/mobile-app/qr-login-scan")
            val parsed = Gson().fromJson(readBody(request), JsonObject::class.java)
            assertThat(parsed.get("token").asString).isEqualTo("tok-42")
            assertThat(parsed.get("supports_number_matching").asBoolean).isTrue()
            val device = requireNotNull(parsed.getAsJsonObject("device")) {
                "Scan request must include a device payload"
            }
            assertThat(device.get("os").asString).isEqualTo("Android")
            assertThat(device.get("os_version").asString).isEqualTo("14")
            assertThat(device.get("model").asString).isEqualTo("Pixel 8 Pro")
            assertThat(device.get("brand").asString).isEqualTo("google")
            assertThat(device.get("app_version").asString).isEqualTo("24.7.0")
        }

    @Test
    fun `given 200 valid scan body, when scan, then returns session id and real number`() = testBlocking {
        responder = { ok(it, DEFAULT_SCAN_BODY) }

        val result = client.scan("https://store.example", "tok")

        assertThat(result.getOrNull()).isEqualTo(
            QrLoginScanResult(sessionId = "sess-1", realNumber = "042", expiresInSeconds = 90)
        )
    }

    @Test
    fun `given 401, when scan, then TokenRejected`() = testBlocking {
        responder = { respond(it, code = 401) }

        val result = client.scan("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginScanException.TokenRejected)
    }

    @Test
    fun `given 409, when scan, then AlreadyScanned`() = testBlocking {
        responder = { respond(it, code = 409) }

        val result = client.scan("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginScanException.AlreadyScanned)
    }

    @Test
    fun `given 426, when scan, then UpgradeRequired`() = testBlocking {
        responder = { respond(it, code = 426) }

        val result = client.scan("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginScanException.UpgradeRequired)
    }

    @Test
    fun `given 429, when scan, then RateLimited`() = testBlocking {
        responder = { respond(it, code = 429) }

        val result = client.scan("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginScanException.RateLimited)
    }

    @Test
    fun `given network IOException, when scan, then Network`() = testBlocking {
        responder = { throw IOException("connection refused") }

        val result = client.scan("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginScanException.Network)
    }

    @Test
    fun `given 200 missing real_number, when scan, then MalformedResponse`() = testBlocking {
        responder = { ok(it, body = """{"session_id":"sess-1","expires_in":90}""") }

        val result = client.scan("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginScanException.MalformedResponse)
    }

    // endregion

    // region session status

    @Test
    fun `given session-status call, when executed, then GET request includes session_id and token_hash`() =
        testBlocking {
            responder = { ok(it, """{"state":"scanned"}""") }

            client.checkSessionStatus("https://store.example", "sess-99", "tok-99")

            val request = requireNotNull(lastRequest)
            assertThat(request.method).isEqualTo("GET")
            assertThat(request.url.queryParameter("session_id")).isEqualTo("sess-99")
            // SHA-256("tok-99") — must line up byte-for-byte with PHP's hash('sha256', $token).
            assertThat(request.url.queryParameter("token_hash"))
                .isEqualTo("2cbd7d7c8841c55cab932e2e92cb3f788210c3883bfdb3cc8749623175430ca0")
            assertThat(request.url.encodedPath)
                .isEqualTo("/wp-json/wc-admin/mobile-app/qr-login-session-status")
            assertThat(request.header("Cache-Control"))
                .contains("no-cache")
                .contains("no-store")
        }

    @Test
    fun `given a different plaintext token, when session-status, then the hash differs`() = testBlocking {
        // Direct sanity check that the hash isn't accidentally a fixed string. The two values
        // below are the known SHA-256 hex digests for the two inputs.
        responder = { ok(it, """{"state":"scanned"}""") }

        client.checkSessionStatus("https://store.example", "sess-1", "tok")
        val hashForTok = requireNotNull(lastRequest).url.queryParameter("token_hash")

        client.checkSessionStatus("https://store.example", "sess-1", "plaintext-token-from-qr")
        val hashForOther = requireNotNull(lastRequest).url.queryParameter("token_hash")

        assertThat(hashForTok)
            .isEqualTo("1a7674eb4ee78df7e1ac439a93c3fa8e3c945784d4dec9fd8e3011738b2f1d62")
        assertThat(hashForOther)
            .isEqualTo("b126ded63b57b4cc3ca5742336b9e2aa522fb4d80be84b364bfe402cdde536ef")
        assertThat(hashForTok).isNotEqualTo(hashForOther)
    }

    @Test
    fun `given state=scanned, when session-status, then Scanned`() = testBlocking {
        responder = { ok(it, """{"state":"scanned"}""") }

        val result = client.checkSessionStatus("https://store.example", "sess-1", "tok")

        assertThat(result.getOrNull()).isEqualTo(QrLoginSessionStatus.Scanned)
    }

    @Test
    fun `given state=approved with grant, when session-status, then Approved with grant`() = testBlocking {
        responder = { ok(it, """{"state":"approved","exchange_grant":"grant-99"}""") }

        val result = client.checkSessionStatus("https://store.example", "sess-1", "tok")

        assertThat(result.getOrNull()).isEqualTo(QrLoginSessionStatus.Approved("grant-99"))
    }

    @Test
    fun `given state=approved without grant, when session-status, then fails closed as Expired`() = testBlocking {
        responder = { ok(it, """{"state":"approved"}""") }

        val result = client.checkSessionStatus("https://store.example", "sess-1", "tok")

        assertThat(result.getOrNull()).isEqualTo(QrLoginSessionStatus.Expired)
    }

    @Test
    fun `given state=rejected, when session-status, then Rejected`() = testBlocking {
        responder = { ok(it, """{"state":"rejected"}""") }

        val result = client.checkSessionStatus("https://store.example", "sess-1", "tok")

        assertThat(result.getOrNull()).isEqualTo(QrLoginSessionStatus.Rejected)
    }

    @Test
    fun `given state=expired, when session-status, then Expired`() = testBlocking {
        responder = { ok(it, """{"state":"expired"}""") }

        val result = client.checkSessionStatus("https://store.example", "sess-1", "tok")

        assertThat(result.getOrNull()).isEqualTo(QrLoginSessionStatus.Expired)
    }

    @Test
    fun `given unknown state, when session-status, then fails closed as Expired`() = testBlocking {
        responder = { ok(it, """{"state":"some_future_state"}""") }

        val result = client.checkSessionStatus("https://store.example", "sess-1", "tok")

        assertThat(result.getOrNull()).isEqualTo(QrLoginSessionStatus.Expired)
    }

    @Test
    fun `given 429, when session-status, then RateLimited`() = testBlocking {
        responder = { respond(it, code = 429) }

        val result = client.checkSessionStatus("https://store.example", "sess-1", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginSessionStatusException.RateLimited)
    }

    @Test
    fun `given network IOException, when session-status, then Network`() = testBlocking {
        responder = { throw IOException("disconnect") }

        val result = client.checkSessionStatus("https://store.example", "sess-1", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginSessionStatusException.Network)
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
        const val DEFAULT_EXCHANGE_BODY =
            """{"user_login":"admin","site_url":"https://store.example",""" +
                """"application_password":"ap-secret"}"""
        const val DEFAULT_SCAN_BODY =
            """{"session_id":"sess-1","real_number":"042","expires_in":90}"""
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
