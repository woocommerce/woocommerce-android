package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class WooAiSmokeDirectJwtTokenProviderTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `given successful response, when provider supplies token, then it posts with basic auth`() = runTest {
        server.enqueue(jsonResponse("""{"token":"jwt-token"}"""))

        val token = provider().provide()

        val request = server.takeRequest()
        assertThat(token).isEqualTo("jwt-token")
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/wp-json/jetpack/v4/jetpack-ai-jwt")
        assertThat(request.getHeader("Authorization")).startsWith("Basic ")
    }

    @Test
    fun `given cached token, when provider supplies twice, then network is used once`() = runTest {
        server.enqueue(jsonResponse("""{"token":"jwt-token"}"""))
        val provider = provider()

        assertThat(provider.provide()).isEqualTo("jwt-token")
        assertThat(provider.provide()).isEqualTo("jwt-token")

        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `given invalidation, when provider supplies again, then token is minted again`() = runTest {
        server.enqueue(jsonResponse("""{"token":"jwt-token-1"}"""))
        server.enqueue(jsonResponse("""{"token":"jwt-token-2"}"""))
        val provider = provider()

        assertThat(provider.provide()).isEqualTo("jwt-token-1")
        provider.invalidate()
        assertThat(provider.provide()).isEqualTo("jwt-token-2")

        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun `given http failure, when provider supplies token, then error is redacted`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("merchant@example.com app password"))

        assertThatThrownBy {
            runTest { provider().provide() }
        }.isInstanceOf(AssistantAuthException::class.java)
            .hasMessageContaining("status=401")
            .hasMessageNotContaining("merchant@example.com")
            .hasMessageNotContaining("app password")
            .hasMessageNotContaining(server.url("/").toString())
    }

    @Test
    fun `given malformed body, when provider supplies token, then error is redacted`() {
        server.enqueue(jsonResponse("not-json"))

        assertThatThrownBy {
            runTest { provider().provide() }
        }.isInstanceOf(AssistantAuthException::class.java)
            .hasMessageContaining("Malformed Jetpack AI JWT response")
    }

    @Test
    fun `given response without token, when provider supplies token, then error is redacted`() {
        server.enqueue(jsonResponse("""{"ok":true}"""))

        assertThatThrownBy {
            runTest { provider().provide() }
        }.isInstanceOf(AssistantAuthException::class.java)
            .hasMessageContaining("Jetpack AI JWT response did not include a token")
    }

    private fun provider() = WooAiSmokeDirectJwtTokenProvider(
        httpClient = OkHttpClient.Builder()
            .callTimeout(5, TimeUnit.SECONDS)
            .build(),
        json = Json { ignoreUnknownKeys = true },
        siteUrl = server.url("/").toString(),
        username = "merchant@example.com",
        appPassword = "app password",
        redactor = WooAiSmokeRedactor(
            siteUrl = server.url("/").toString(),
            username = "merchant@example.com",
            appPassword = "app password",
        ),
    )

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
