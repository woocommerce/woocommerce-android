package org.wordpress.android.fluxc.network

import com.android.volley.NetworkResponse
import com.android.volley.NoConnectionError
import com.android.volley.Request.Method.GET
import com.android.volley.Response
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_SSL_CERTIFICATE
import javax.net.ssl.SSLHandshakeException
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class BaseRequestTest {
    @Test
    fun `given TLS failure wrapped as no connection, when delivering error, then classify invalid certificate`() {
        lateinit var deliveredError: BaseNetworkError
        val request = TestRequest { deliveredError = it }

        request.deliverError(NoConnectionError(SSLHandshakeException("certificate rejected")))

        assertEquals(INVALID_SSL_CERTIFICATE, deliveredError.type)
    }

    private class TestRequest(
        errorListener: BaseRequest.BaseErrorListener
    ) : BaseRequest<Unit>(GET, "https://example.com", errorListener) {
        override fun deliverBaseNetworkError(error: BaseNetworkError): BaseNetworkError = error

        override fun parseNetworkResponse(response: NetworkResponse?): Response<Unit> =
            Response.success(Unit, null)

        override fun deliverResponse(response: Unit) = Unit
    }
}
