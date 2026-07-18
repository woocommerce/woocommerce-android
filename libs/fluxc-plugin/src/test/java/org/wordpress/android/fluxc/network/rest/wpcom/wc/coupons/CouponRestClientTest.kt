package org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.coupon.UpdateCouponRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork

class CouponRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private val client = CouponRestClient(wooNetwork)
    private val site = SiteModel()

    @Test
    fun `given null expiry, when coupon is updated, then date expires is omitted`() = runTest {
        val body = captureUpdateBody(null)

        assertThat(body).doesNotContainKey("date_expires")
    }

    @Test
    fun `given empty expiry, when coupon is updated, then empty date expires is sent`() = runTest {
        val body = captureUpdateBody("")

        assertThat(body["date_expires"]).isEqualTo("")
    }

    @Test
    fun `given expiry value, when coupon is updated, then date expires is sent`() = runTest {
        val body = captureUpdateBody("2025-11-03T00:00:00")

        assertThat(body["date_expires"]).isEqualTo("2025-11-03T00:00:00")
    }

    private suspend fun captureUpdateBody(expiryDate: String?): Map<String, Any> {
        whenever(
            wooNetwork.executePutGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(CouponDto::class.java),
                body = any(),
                params = any()
            )
        ).thenReturn(WPAPIResponse.Success(mock<CouponDto>(), emptyList()))

        client.updateCoupon(site, COUPON_ID, UpdateCouponRequest(expiryDate = expiryDate))

        val bodyCaptor = argumentCaptor<Map<String, Any>>()
        verify(wooNetwork).executePutGsonRequest(
            site = eq(site),
            path = any(),
            clazz = eq(CouponDto::class.java),
            body = bodyCaptor.capture(),
            params = any()
        )
        return bodyCaptor.firstValue
    }

    private companion object {
        const val COUPON_ID = 1L
    }
}
