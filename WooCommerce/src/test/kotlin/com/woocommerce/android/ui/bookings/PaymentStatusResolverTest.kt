package com.woocommerce.android.ui.bookings

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import java.math.BigDecimal

class PaymentStatusResolverTest {

    private val orderStore: WCOrderStore = mock()
    private val site: SiteModel = mock {
        on { localId() }.thenReturn(LocalId(1))
    }
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(site)
    }
    private val sut = PaymentStatusResolver(orderStore, selectedSite)

    @Test
    fun `given order not found, when resolve, then returns UNPAID`() = runTest {
        // GIVEN
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(null)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.UNPAID)
    }

    @Test
    fun `given order with datePaid, when resolve, then returns PAID`() = runTest {
        // GIVEN
        val order = createOrder(datePaid = "2025-01-01", refundTotal = BigDecimal.ZERO)
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.PAID)
    }

    @Test
    fun `given full refund, when resolve, then returns REFUNDED`() = runTest {
        // GIVEN
        val order = createOrder(
            total = "100",
            refundTotal = BigDecimal("-100"),
            datePaid = "2025-01-01"
        )
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.REFUNDED)
    }

    @Test
    fun `given partial refund, when resolve, then returns PARTIALLY_REFUNDED`() = runTest {
        // GIVEN
        val order = createOrder(
            total = "100",
            refundTotal = BigDecimal("-50"),
            datePaid = "2025-01-01"
        )
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED)
    }

    @Test
    fun `given failed order status, when resolve, then returns FAILED`() = runTest {
        // GIVEN
        val order = createOrder(status = "failed", datePaid = "", refundTotal = BigDecimal.ZERO)
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.FAILED)
    }

    @Test
    fun `given cancelled order status, when resolve, then returns FAILED`() = runTest {
        // GIVEN
        val order = createOrder(status = "cancelled", datePaid = "", refundTotal = BigDecimal.ZERO)
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.FAILED)
    }

    @Test
    fun `given unpaid order with no refunds, when resolve, then returns UNPAID`() = runTest {
        // GIVEN
        val order = createOrder(datePaid = "", refundTotal = BigDecimal.ZERO, status = "pending")
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.UNPAID)
    }

    private fun createOrder(
        status: String = "processing",
        datePaid: String = "",
        refundTotal: BigDecimal = BigDecimal.ZERO,
        total: String = "100",
    ) = OrderEntity(
        localSiteId = LocalId(1),
        orderId = 1L,
        status = status,
        datePaid = datePaid,
        refundTotal = refundTotal,
        total = total,
    )
}
