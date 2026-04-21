package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.ui.bookings.PaymentStatus
import com.woocommerce.android.ui.bookings.PaymentStatusResolver
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class WooPosPaymentStatusResolverTest {

    private val paymentStatusResolver: PaymentStatusResolver = mock {
        on { resolve(any()) } doReturn PaymentStatus.PAID
    }
    private val sut = WooPosPaymentStatusResolver(paymentStatusResolver)

    @Test
    fun `when resolve called, then delegates to shared PaymentStatusResolver`() = runTest {
        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.PAID)
    }
}
