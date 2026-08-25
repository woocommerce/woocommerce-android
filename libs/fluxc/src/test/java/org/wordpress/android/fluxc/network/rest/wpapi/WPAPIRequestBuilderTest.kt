package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.utils.HttpsUrlNormalizer
import javax.net.ssl.SSLHandshakeException

@RunWith(RobolectricTestRunner::class)
class WPAPIRequestBuilderTest {
    private val requestQueue: RequestQueue = mock()
    private val userAgent: UserAgent = mock {
        on { apiUserAgent } doReturn "test-user-agent"
    }
    private val restClient = object : BaseWPAPIRestClient(mock<Dispatcher>(), requestQueue, userAgent) {}
    private val normalizer = HttpsUrlNormalizer()

    @Test
    fun `given HTTP Gson request and TLS failure, when sending, then use HTTPS once without downgrade`() = runTest {
        lateinit var sentRequest: WPAPIGsonRequest<String>
        whenever(requestQueue.add(any<WPAPIGsonRequest<String>>())).thenAnswer { invocation ->
            sentRequest = invocation.getArgument(0)
            sentRequest.deliverError(VolleyError(SSLHandshakeException("certificate failure")))
            sentRequest
        }

        val response = WPAPIGsonRequestBuilder(normalizer).syncGetRequest(
            restClient = restClient,
            url = "http://example.com/wp-json/wc/v3/orders",
            clazz = String::class.java,
        )

        assertThat(sentRequest.url).isEqualTo("https://example.com/wp-json/wc/v3/orders")
        assertThat(response).isInstanceOf(WPAPIResponse.Error::class.java)
        verify(requestQueue, times(1)).add(any<WPAPIGsonRequest<String>>())
    }

    @Test
    fun `given HTTP encoded-body request and TLS failure, when sending, then use HTTPS once without downgrade`() =
        runTest {
            lateinit var sentRequest: WPAPIEncodedBodyRequest
            whenever(requestQueue.add(any<WPAPIEncodedBodyRequest>())).thenAnswer { invocation ->
                sentRequest = invocation.getArgument(0)
                sentRequest.deliverError(VolleyError(SSLHandshakeException("certificate failure")))
                sentRequest
            }

            val response = WPAPIEncodedBodyRequestBuilder(normalizer).syncPostRequest(
                restClient = restClient,
                url = "http://example.com/wp-login.php",
            )

            assertThat(sentRequest.url).isEqualTo("https://example.com/wp-login.php")
            assertThat(response).isInstanceOf(WPAPIResponse.Error::class.java)
            verify(requestQueue, times(1)).add(any<WPAPIEncodedBodyRequest>())
        }
}
