package com.woocommerce.android.ui.bookings

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore

class PaymentStatusResolverTest {

    private val orderStore: WCOrderStore = mock()
    private val site: SiteModel = mock()
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
    fun `given order with _payment_status paid, when resolve, then returns PAID`() = runTest {
        // GIVEN
        val order = createOrder(paymentStatus = "paid")
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.PAID)
    }

    @Test
    fun `given order with _payment_status unpaid, when resolve, then returns UNPAID`() = runTest {
        // GIVEN
        val order = createOrder(paymentStatus = "unpaid")
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.UNPAID)
    }

    @Test
    fun `given order with _payment_status refunded, when resolve, then returns REFUNDED`() = runTest {
        // GIVEN
        val order = createOrder(paymentStatus = "refunded")
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.REFUNDED)
    }

    @Test
    fun `given order with _payment_status partially_refunded, when resolve, then returns PARTIALLY_REFUNDED`() =
        runTest {
            // GIVEN
            val order = createOrder(paymentStatus = "partially_refunded")
            whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

            // WHEN
            val result = sut.resolve(1L)

            // THEN
            assertThat(result).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED)
        }

    @Test
    fun `given order with _payment_status failed, when resolve, then returns FAILED`() = runTest {
        // GIVEN
        val order = createOrder(paymentStatus = "failed")
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.FAILED)
    }

    @Test
    fun `given order with _payment_status authorized, when resolve, then returns AUTHORIZED`() = runTest {
        // GIVEN
        val order = createOrder(paymentStatus = "authorized")
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.AUTHORIZED)
    }

    @Test
    fun `given order with _payment_status authorization_voided, when resolve, then returns AUTHORIZATION_VOIDED`() =
        runTest {
            // GIVEN
            val order = createOrder(paymentStatus = "authorization_voided")
            whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

            // WHEN
            val result = sut.resolve(1L)

            // THEN
            assertThat(result).isEqualTo(PaymentStatus.AUTHORIZATION_VOIDED)
        }

    @Test
    fun `given order with unknown _payment_status value, when resolve, then returns UNPAID`() = runTest {
        // GIVEN
        val order = createOrder(paymentStatus = "some_future_value")
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.UNPAID)
    }

    @Test
    fun `given order without _payment_status metadata, when resolve, then returns UNPAID`() = runTest {
        // GIVEN
        val order = createOrder(paymentStatus = null)
        whenever(orderStore.getOrderByIdAndSite(1L, site)).thenReturn(order)

        // WHEN
        val result = sut.resolve(1L)

        // THEN
        assertThat(result).isEqualTo(PaymentStatus.UNPAID)
    }

    @Test
    fun `given unique order ids, when resolveAll, then returns status for each id`() = runTest {
        // GIVEN
        val paidOrder = createOrder(orderId = 11L, paymentStatus = "paid")
        val refundedOrder = createOrder(orderId = 22L, paymentStatus = "refunded")
        val failedOrder = createOrder(orderId = 33L, paymentStatus = "failed")
        whenever(orderStore.getOrdersByIdsAndSite(listOf(11L, 22L, 33L), site))
            .thenReturn(listOf(failedOrder, paidOrder, refundedOrder))

        // WHEN
        val result = sut.resolveAll(listOf(11L, 22L, 33L))

        // THEN
        assertThat(result).isEqualTo(
            mapOf(
                11L to PaymentStatus.PAID,
                22L to PaymentStatus.REFUNDED,
                33L to PaymentStatus.FAILED
            )
        )
    }

    @Test
    fun `given order ids with duplicates, when resolveAll, then returns status per unique id`() = runTest {
        // GIVEN
        val paidOrder = createOrder(orderId = 1L, paymentStatus = "paid")
        val failedOrder = createOrder(orderId = 2L, paymentStatus = "failed")
        whenever(orderStore.getOrdersByIdsAndSite(listOf(1L, 2L), site))
            .thenReturn(listOf(paidOrder, failedOrder))

        // WHEN
        val result = sut.resolveAll(listOf(1L, 2L, 1L))

        // THEN
        assertThat(result).isEqualTo(
            mapOf(
                1L to PaymentStatus.PAID,
                2L to PaymentStatus.FAILED
            )
        )
    }

    @Test
    fun `given missing orders, when resolveAll, then missing ids fallback to UNPAID`() = runTest {
        // GIVEN
        val refundedOrder = createOrder(orderId = 3L, paymentStatus = "refunded")
        whenever(orderStore.getOrdersByIdsAndSite(listOf(3L, 999L), site))
            .thenReturn(listOf(refundedOrder))

        // WHEN
        val result = sut.resolveAll(listOf(3L, 999L))

        // THEN
        assertThat(result).isEqualTo(
            mapOf(
                3L to PaymentStatus.REFUNDED,
                999L to PaymentStatus.UNPAID
            )
        )
    }

    private fun createOrder(
        orderId: Long = 1L,
        paymentStatus: String? = null,
    ): OrderEntity {
        val metaData = if (paymentStatus != null) {
            listOf(
                WCMetaData(
                    id = 1L,
                    key = WCMetaData.PaymentMetadataKeys.PAYMENT_STATUS,
                    value = paymentStatus
                )
            )
        } else {
            emptyList()
        }
        return OrderEntity(
            localSiteId = LocalId(1),
            orderId = orderId,
            metaData = metaData,
        )
    }
}
