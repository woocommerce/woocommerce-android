package org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.inperson

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class InPersonPaymentsRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private val site = SiteModel()

    private lateinit var sut: InPersonPaymentsRestClient

    @Before
    fun setUp() {
        sut = InPersonPaymentsRestClient(wooNetwork, Gson())
    }

    @Test
    fun `given success response, when preparePayment, then WCPay prepare endpoint is called`() = runTest {
        val orderId = 123L
        val paymentId = "pi_test"
        val expectedPath = WOOCOMMERCE.payments.orders.id(orderId).prepare_terminal_payment.pathV3

        whenever(
            wooNetwork.executePostGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Any::class.java),
                body = any()
            )
        ).thenReturn(WPAPIResponse.Success(Any(), emptyList()))

        val result = sut.preparePayment(site, paymentId, orderId)

        val bodyCaptor = argumentCaptor<Map<String, Any>>()
        verify(wooNetwork).executePostGsonRequest(
            site = eq(site),
            path = eq(expectedPath),
            clazz = eq(Any::class.java),
            body = bodyCaptor.capture()
        )

        assertThat(bodyCaptor.firstValue).containsExactlyEntriesOf(mapOf("payment_intent_id" to paymentId))
        assertThat(result.site).isEqualTo(site)
        assertThat(result.paymentId).isEqualTo(paymentId)
        assertThat(result.orderId).isEqualTo(orderId)
        assertThat(result.error).isNull()
    }

    @Test
    fun `given error response, when preparePayment, then error is mapped`() = runTest {
        val networkError = WPAPINetworkError(
            BaseNetworkError(GenericErrorType.NOT_FOUND, "Missing route"),
            errorCode = "rest_no_route"
        )
        whenever(
            wooNetwork.executePostGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Any::class.java),
                body = any()
            )
        ).thenReturn(WPAPIResponse.Error(networkError))

        val result = sut.preparePayment(site, paymentId = "pi_test", orderId = 123L)

        assertThat(result.error?.type).isEqualTo(WooErrorType.API_NOT_FOUND)
        assertThat(result.error?.apiErrorCode).isEqualTo("rest_no_route")
        assertThat(result.error?.message).isEqualTo("Missing route")
    }

    @Test
    fun `given gateway timeout response, when capturePayment, then gateway error is returned`() = runTest {
        givenCaptureFailsWith(statusCode = 504, type = GenericErrorType.UNKNOWN)

        val result = sut.capturePayment(WOOCOMMERCE_PAYMENTS, site, paymentId = "pi_test", orderId = 123L)

        assertThat(result.error?.type).isEqualTo(WCCapturePaymentErrorType.GATEWAY_ERROR)
    }

    @Test
    fun `given internal server error response, when capturePayment, then generic error is returned`() = runTest {
        givenCaptureFailsWith(statusCode = 500, type = GenericErrorType.SERVER_ERROR)

        val result = sut.capturePayment(WOOCOMMERCE_PAYMENTS, site, paymentId = "pi_test", orderId = 123L)

        assertThat(result.error?.type).isEqualTo(WCCapturePaymentErrorType.GENERIC_ERROR)
    }

    private suspend fun givenCaptureFailsWith(statusCode: Int, type: GenericErrorType) {
        val volleyError = VolleyError(NetworkResponse(statusCode, ByteArray(0), false, 0, emptyList()))
        whenever(
            wooNetwork.executePostGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(CapturePaymentApiResponse::class.java),
                body = any()
            )
        ).thenReturn(WPAPIResponse.Error(WPAPINetworkError(BaseNetworkError(type, volleyError))))
    }
}
