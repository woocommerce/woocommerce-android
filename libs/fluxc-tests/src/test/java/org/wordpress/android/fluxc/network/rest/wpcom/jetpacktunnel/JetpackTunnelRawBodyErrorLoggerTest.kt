package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mockStatic
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.util.AppLog

@RunWith(RobolectricTestRunner::class)
class JetpackTunnelRawBodyErrorLoggerTest {
    @Test
    fun `given raw body is present, when message is built, then it includes sanitized snippet`() {
        val error = buildError(rawBody = "<html>\nFatal error</html>")

        val message = JetpackTunnelRawBodyErrorLogger.buildMessage("GET", "/wc/v3/orders", error)

        assertThat(message).contains("raw_body_snippet=<html> Fatal error</html>")
    }

    @Test
    fun `given raw body and error context, when message is built, then it includes context fields`() {
        val error = buildError(
            rawBody = "<html>Fatal error</html>",
            proxyStatus = 500,
            transportStatus = 502
        ).apply {
            apiError = "no_response_body"
            message = "Remote site returned non-JSON response"
        }

        val message = JetpackTunnelRawBodyErrorLogger.buildMessage("GET", "/wc/v3/orders", error)

        assertThat(message).contains("method=GET")
        assertThat(message).contains("path=/wc/v3/orders")
        assertThat(message).contains("transport_status=502")
        assertThat(message).contains("proxy_status=500")
        assertThat(message).contains("error_code=no_response_body")
        assertThat(message).contains("error_message=Remote site returned non-JSON response")
        assertThat(message).contains("raw_body_truncated=false")
        assertThat(message).contains("raw_body_snippet=<html>Fatal error</html>")
    }

    @Test
    fun `given no raw body is present, when message is built, then it returns null`() {
        val error = buildError(rawBody = null)

        val message = JetpackTunnelRawBodyErrorLogger.buildMessage("GET", "/wc/v3/orders", error)

        assertThat(message).isNull()
    }

    @Test
    fun `given raw body contains secrets, when message is built, then secret values are redacted`() {
        val error = buildError(rawBody = rawBodyWithSecrets)

        val message = JetpackTunnelRawBodyErrorLogger.buildMessage("GET", "/wc/v3/orders", error)

        assertThat(message).contains("[redacted]")
        rawBodySecretValues.forEach { secretValue ->
            assertThat(message).doesNotContain(secretValue)
        }
    }

    @Test
    fun `given context contains secrets, when message is built, then context secret values are redacted`() {
        val error = buildError(rawBody = "<html>Fatal error</html>").apply {
            apiError = "no_response_body Bearer apiErrorBearer consumer_secret=apiErrorConsumerSecret"
            message = "Cookie: wordpress_logged_in=messageCookie password=messagePassword " +
                "application_password=messageApplicationPassword"
        }
        val path = "/wc/v3/orders?consumer_key=pathConsumerKey&consumer_secret=pathConsumerSecret" +
            "&access_token=pathAccessToken&password=hunter2&application_password=app-pass"

        val message = JetpackTunnelRawBodyErrorLogger.buildMessage("POST", path, error)

        assertThat(message).contains("[redacted]")
        contextSecretValues.forEach { secretValue ->
            assertThat(message).doesNotContain(secretValue)
        }
        assertThat(message).doesNotContain(requestBodySecretValue)
    }

    @Test
    fun `given raw body and context contain secrets, when logging, then emitted API warning is redacted`() {
        val error = buildError(rawBody = rawBodyWithSecrets).apply {
            apiError = "consumer_secret=apiErrorConsumerSecret"
            message = "Authorization: Bearer messageBearer"
        }
        val path = "/wc/v3/orders?consumer_key=pathConsumerKey&password=hunter2"
        val messageCaptor = ArgumentCaptor.forClass(String::class.java)

        mockStatic(AppLog::class.java).use { appLog ->
            JetpackTunnelRawBodyErrorLogger.logIfPresent("PUT", path, error)

            appLog.verify {
                AppLog.w(eq(AppLog.T.API), messageCaptor.capture())
            }
        }
        assertThat(messageCaptor.value).contains("[redacted]")
        (rawBodySecretValues + contextSecretValues).forEach { secretValue ->
            assertThat(messageCaptor.value).doesNotContain(secretValue)
        }
        assertThat(messageCaptor.value).doesNotContain(requestBodySecretValue)
    }

    @Test
    fun `given raw body at exact limit, when message is built, then complete raw body is included`() {
        val rawBody = "a".repeat(MAX_RAW_BODY_LOG_CHARS)
        val error = buildError(rawBody = rawBody)

        val message = JetpackTunnelRawBodyErrorLogger.buildMessage("GET", "/wc/v3/orders", error)

        assertThat(message).contains("raw_body_truncated=false")
        assertThat(rawBodySnippet(message)).hasSize(MAX_RAW_BODY_LOG_CHARS)
        assertThat(rawBodySnippet(message)).isEqualTo(rawBody)
    }

    @Test
    fun `given raw body above limit, when message is built, then raw body is capped and marked truncated`() {
        val retainedPrefix = "a".repeat(MAX_RAW_BODY_LOG_CHARS)
        val truncatedTail = "tail-content"
        val error = buildError(rawBody = retainedPrefix + truncatedTail)

        val message = JetpackTunnelRawBodyErrorLogger.buildMessage("GET", "/wc/v3/orders", error)

        assertThat(message).contains("raw_body_truncated=true")
        assertThat(rawBodySnippet(message)).hasSize(MAX_RAW_BODY_LOG_CHARS)
        assertThat(rawBodySnippet(message)).isEqualTo(retainedPrefix)
        assertThat(message).doesNotContain(truncatedTail)
    }

    private fun buildError(
        rawBody: String?,
        proxyStatus: Int? = null,
        transportStatus: Int? = null
    ): WPComGsonNetworkError {
        val baseError = transportStatus?.let { status ->
            BaseNetworkError(VolleyError(NetworkResponse(status, byteArrayOf(), emptyMap(), true)))
        } ?: BaseNetworkError(GenericErrorType.UNKNOWN)
        val errorData = rawBody?.let { JSONObject().put("raw_body", it) }
        if (proxyStatus != null) {
            errorData?.put("status", proxyStatus)
        }
        return WPComGsonNetworkError(baseError).apply {
            this.errorData = errorData
        }
    }

    private fun rawBodySnippet(message: String?): String {
        return message?.substringAfter("raw_body_snippet=").orEmpty()
    }

    private companion object {
        const val MAX_RAW_BODY_LOG_CHARS = 2048
        const val requestBodySecretValue = "requestBodyShouldNotAppear"

        val rawBodyWithSecrets = """
            Authorization: Bearer abc123
            Cookie: wordpress_logged_in=wpLoginSecret
            Set-Cookie: session=sessionSecret
            consumer_key=ckSecretValue
            consumer_secret=csSecretValue
            {
              "consumer_key": "jsonConsumerKey",
              "consumer_secret": "jsonConsumerSecret",
              "access_token": "jsonAccessToken",
              "token": "jsonToken",
              "application_password": "jsonApplicationPassword",
              "password": "jsonPassword"
            }
        """.trimIndent()

        val rawBodySecretValues = listOf(
            "abc123",
            "wpLoginSecret",
            "sessionSecret",
            "ckSecretValue",
            "csSecretValue",
            "jsonConsumerKey",
            "jsonConsumerSecret",
            "jsonAccessToken",
            "jsonToken",
            "jsonApplicationPassword",
            "jsonPassword"
        )

        val contextSecretValues = listOf(
            "apiErrorBearer",
            "apiErrorConsumerSecret",
            "messageCookie",
            "messagePassword",
            "messageApplicationPassword",
            "pathConsumerKey",
            "pathConsumerSecret",
            "pathAccessToken",
            "hunter2",
            "app-pass",
            "messageBearer"
        )
    }
}
