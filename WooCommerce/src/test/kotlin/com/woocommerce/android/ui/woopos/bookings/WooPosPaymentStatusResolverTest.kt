package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.ui.bookings.PaymentStatus
import com.woocommerce.android.ui.bookings.PaymentStatusResolver
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class WooPosPaymentStatusResolverTest {

    private val paymentStatusResolver: PaymentStatusResolver = mock()
    private val sut = WooPosPaymentStatusResolver(paymentStatusResolver)

    @Test
    fun `when resolve called, then delegates to shared PaymentStatusResolver`() = runTest {
        // GIVEN
        whenever(paymentStatusResolver.resolve(1L, BigDecimal.TEN)).thenReturn(PaymentStatus.PAID)

        // WHEN
        val result = sut.resolve(1L, BigDecimal.TEN)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.PAID)
    }

    @Test
    fun `given order not found, when resolve, then returns UNPAID`() = runTest {
        // GIVEN
        whenever(paymentStatusResolver.resolve(1L, BigDecimal.TEN)).thenReturn(PaymentStatus.UNPAID)

        // WHEN
        val result = sut.resolve(1L, BigDecimal.TEN)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.UNPAID)
    }

    @Test
    fun `given full refund, when resolve, then returns REFUNDED`() = runTest {
        // GIVEN
        whenever(paymentStatusResolver.resolve(1L, BigDecimal.TEN)).thenReturn(PaymentStatus.REFUNDED)

        // WHEN
        val result = sut.resolve(1L, BigDecimal.TEN)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.REFUNDED)
    }

    @Test
    fun `given partial refund, when resolve, then returns PARTIALLY_REFUNDED`() = runTest {
        // GIVEN
        whenever(paymentStatusResolver.resolve(1L, BigDecimal.TEN)).thenReturn(PaymentStatus.PARTIALLY_REFUNDED)

        // WHEN
        val result = sut.resolve(1L, BigDecimal.TEN)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED)
    }

    @Test
    fun `given failed order, when resolve, then returns FAILED`() = runTest {
        // GIVEN
        whenever(paymentStatusResolver.resolve(1L, BigDecimal.TEN)).thenReturn(PaymentStatus.FAILED)

        // WHEN
        val result = sut.resolve(1L, BigDecimal.TEN)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.FAILED)
    }
}
