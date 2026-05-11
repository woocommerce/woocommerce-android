package com.woocommerce.android.network.qrlogin

import com.google.gson.Gson
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
    private var responder: (Request) -> Response = { ok(it, DEFAULT_SUCCESS_BODY) }

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

    @Test
    fun `given 200 with valid body, when exchange, then returns credentials`() = testBlocking {
        val result = client.exchange("https://store.example", "tok")

        assertThat(result.getOrNull()).isEqualTo(
            QrLoginCredentials(userLogin = "admin", applicationPassword = "ap-secret")
        )
    }

    @Test
    fun `given valid credentials, when toString is called, then password is redacted`() = testBlocking {
        val credentials = requireNotNull(client.exchange("https://store.example", "tok").getOrNull())

        assertThat(credentials.toString()).doesNotContain("ap-secret")
    }

    @Test
    fun `given siteUrl with trailing slash, when exchange, then request path is normalized`() = testBlocking {
        client.exchange("https://store.example/", "tok")

        assertThat(lastRequest?.url.toString())
            .isEqualTo("https://store.example/wp-json/wc-admin/mobile-app/qr-login-exchange")
    }

    @Test
    fun `given exchange call, when executed, then request is POST with JSON body containing the token`() = testBlocking {
        client.exchange("https://store.example", "tok-42")

        val request = requireNotNull(lastRequest)
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.body?.contentType()?.toString()).startsWith("application/json")
        val buffer = Buffer().also { requireNotNull(request.body).writeTo(it) }
        val body = buffer.readUtf8()
        // The body now also carries a `device` object alongside the token; assert
        // on the token field shape rather than the full string so future device
        // payload tweaks don't break this test.
        assertThat(body).contains(""""token":"tok-42"""")
    }

    @Test
    fun `given exchange call, when executed, then JSON body includes device metadata from the provider`() =
        testBlocking {
            client.exchange("https://store.example", "tok-42")

            val request = requireNotNull(lastRequest)
            val body = Buffer().also { requireNotNull(request.body).writeTo(it) }.readUtf8()
            val parsed = Gson().fromJson(body, com.google.gson.JsonObject::class.java)
            val device = requireNotNull(parsed.getAsJsonObject("device")) {
                "Exchange request must include a `device` object alongside the token."
            }

            assertThat(device.get("os").asString).isEqualTo("Android")
            assertThat(device.get("os_version").asString).isEqualTo("14")
            assertThat(device.get("model").asString).isEqualTo("Pixel 8 Pro")
            assertThat(device.get("brand").asString).isEqualTo("google")
            assertThat(device.get("app_version").asString).isEqualTo("24.7.0")
        }

    @Test
    fun `given 401, when exchange, then TokenRejected`() = testBlocking {
        responder = { respond(it, code = 401) }

        val result = client.exchange("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.TokenRejected)
    }

    @Test
    fun `given 403, when exchange, then TokenRejected`() = testBlocking {
        responder = { respond(it, code = 403) }

        val result = client.exchange("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.TokenRejected)
    }

    @Test
    fun `given 404, when exchange, then EndpointMissing`() = testBlocking {
        responder = { respond(it, code = 404) }

        val result = client.exchange("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.EndpointMissing)
    }

    @Test
    fun `given 429, when exchange, then RateLimited`() = testBlocking {
        responder = { respond(it, code = 429) }

        val result = client.exchange("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.RateLimited)
    }

    @Test
    fun `given 500, when exchange, then HttpError with status code`() = testBlocking {
        responder = { respond(it, code = 500) }

        val result = client.exchange("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.HttpError(500))
    }

    @Test
    fun `given network IOException, when exchange, then Network`() = testBlocking {
        responder = { throw IOException("connection refused") }

        val result = client.exchange("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.Network)
    }

    @Test
    fun `given 200 with non-JSON body, when exchange, then MalformedResponse`() = testBlocking {
        responder = { ok(it, body = "not json at all") }

        val result = client.exchange("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.MalformedResponse)
    }

    @Test
    fun `given 200 missing user_login, when exchange, then MalformedResponse`() = testBlocking {
        responder = { ok(it, body = """{"site_url":"https://x","application_password":"ap"}""") }

        val result = client.exchange("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.MalformedResponse)
    }

    @Test
    fun `given 200 with null JSON literal body, when exchange, then MalformedResponse`() = testBlocking {
        responder = { ok(it, body = "null") }

        val result = client.exchange("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.MalformedResponse)
    }

    @Test
    fun `given 200 with empty body, when exchange, then MalformedResponse`() = testBlocking {
        responder = { ok(it, body = "") }

        val result = client.exchange("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.MalformedResponse)
    }

    @Test
    fun `given 200 with blank application_password, when exchange, then MalformedResponse`() = testBlocking {
        responder = {
            ok(it, body = """{"user_login":"admin","site_url":"https://x","application_password":"  "}""")
        }

        val result = client.exchange("https://store.example", "tok")

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.MalformedResponse)
    }

    @Test
    fun `given unexpected RuntimeException, when exchange, then wraps in Unknown preserving cause`() = testBlocking {
        val boom = RuntimeException("oops")
        responder = { throw boom }

        val result = client.exchange("https://store.example", "tok")

        val failure = result.exceptionOrNull()
        assertThat(failure).isInstanceOf(QrLoginExchangeException.Unknown::class.java)
        assertThat((failure as QrLoginExchangeException.Unknown).original).isEqualTo(boom)
    }

    @Test
    fun `given CancellationException during call, when exchange, then it propagates unwrapped`() = testBlocking {
        responder = { throw CancellationException("cancelled") }

        var thrown: Throwable? = null
        try {
            client.exchange("https://store.example", "tok")
        } catch (t: Throwable) {
            thrown = t
        }

        assertThat(thrown).isInstanceOf(CancellationException::class.java)
    }

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

    private companion object {
        const val DEFAULT_SUCCESS_BODY =
            """{"user_login":"admin","site_url":"https://store.example",""" +
                """"application_password":"ap-secret"}"""
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
