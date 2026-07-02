package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
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
    fun `given no raw body is present, when message is built, then it returns null`() {
        val error = buildError(rawBody = null)

        val message = JetpackTunnelRawBodyErrorLogger.buildMessage("GET", "/wc/v3/orders", error)

        assertThat(message).isNull()
    }

    @Test
    fun `given raw body is present, when logging, then API warning is emitted`() {
        val error = buildError(rawBody = "<html>Fatal error</html>")

        mockStatic(AppLog::class.java).use { appLog ->
            JetpackTunnelRawBodyErrorLogger.logIfPresent("GET", "/wc/v3/orders", error)

            appLog.verify {
                AppLog.w(
                    AppLog.T.API,
                    "Jetpack Tunnel raw_body error: method=GET, path=/wc/v3/orders, " +
                        "raw_body_truncated=false, raw_body_snippet=<html>Fatal error</html>"
                )
            }
        }
    }

    private fun buildError(rawBody: String?): WPComGsonNetworkError {
        return WPComGsonNetworkError(BaseNetworkError(GenericErrorType.UNKNOWN)).apply {
            errorData = rawBody?.let { JSONObject().put("raw_body", it) }
        }
    }
}
