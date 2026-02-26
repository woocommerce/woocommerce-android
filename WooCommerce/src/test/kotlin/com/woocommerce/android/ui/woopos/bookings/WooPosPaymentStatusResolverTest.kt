package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.ui.bookings.PaymentStatus
import com.woocommerce.android.ui.bookings.PaymentStatusResolver
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosPaymentStatusResolverTest {

    private val paymentStatusResolver: PaymentStatusResolver = mock()
    private val sut = WooPosPaymentStatusResolver(paymentStatusResolver)

    @Test
    fun `when resolve called, then delegates to shared PaymentStatusResolver`() = runTest {
        // GIVEN
        whenever(paymentStatusResolver.resolve(1L)).thenReturn(PaymentStatus.PAID)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.PAID)
    }
}
