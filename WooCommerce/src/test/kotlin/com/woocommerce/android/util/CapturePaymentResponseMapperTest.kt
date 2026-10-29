package com.woocommerce.android.util

import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentError
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentResponsePayload

class CapturePaymentResponseMapperTest {
    private val mapper = CapturePaymentResponseMapper()

    @Test
    fun `given gateway error, when mapping capture response, then gateway error returned`() {
        val result = mapper.mapResponse(responseWithError(WCCapturePaymentErrorType.GATEWAY_ERROR))

        assertThat(result).isEqualTo(CapturePaymentResponse.Error.GatewayError("error message"))
    }

    @Test
    fun `given network error, when mapping capture response, then network error returned`() {
        val result = mapper.mapResponse(responseWithError(WCCapturePaymentErrorType.NETWORK_ERROR))

        assertThat(result).isEqualTo(CapturePaymentResponse.Error.NetworkError("error message"))
    }

    private fun responseWithError(type: WCCapturePaymentErrorType) = WCCapturePaymentResponsePayload(
        error = WCCapturePaymentError(type = type, message = "error message"),
        site = SiteModel(),
        paymentId = "pi_test",
        orderId = 1L
    )
}
