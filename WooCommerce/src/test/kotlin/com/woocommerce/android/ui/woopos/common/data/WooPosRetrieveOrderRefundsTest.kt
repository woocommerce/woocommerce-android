package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import java.util.Date

class WooPosRetrieveOrderRefundsTest {
    private lateinit var refundStore: WCRefundStore
    private lateinit var selectedSite: SelectedSite
    private lateinit var sut: WooPosRetrieveOrderRefunds
    private lateinit var site: SiteModel

    @Before
    fun setup() {
        refundStore = mock()
        selectedSite = mock()
        site = mock()

        whenever(selectedSite.get()).thenReturn(site)

        sut = WooPosRetrieveOrderRefunds(
            refundStore = refundStore,
            selectedSite = selectedSite
        )
    }

    @Test
    fun `given refunds exist locally, when invoke called, then returns mapped refunds`() = runTest {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder(orderId = 123L)

        val fluxCRefunds = listOf(
            WCRefundModel(
                id = 1L,
                dateCreated = Date(),
                amount = BigDecimal.TEN,
                reason = "Test refund",
                automaticGatewayRefund = true,
                items = emptyList(),
                shippingLineItems = emptyList(),
                feeLineItems = emptyList()
            ),
            WCRefundModel(
                id = 2L,
                dateCreated = Date(),
                amount = BigDecimal.valueOf(5),
                reason = "Another refund",
                automaticGatewayRefund = false,
                items = emptyList(),
                shippingLineItems = emptyList(),
                feeLineItems = emptyList()
            )
        )
        whenever(refundStore.getAllRefunds(site, order.id)).thenReturn(fluxCRefunds)

        // WHEN
        val result = sut.invoke(order)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(1L)
        assertThat(result[0].amount).isEqualTo(BigDecimal.TEN)
        assertThat(result[1].id).isEqualTo(2L)
        assertThat(result[1].amount).isEqualTo(BigDecimal.valueOf(5))
    }

    @Test
    fun `given no refunds exist locally, when invoke called, then fetches and returns mapped refunds`() = runTest {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder(orderId = 123L)

        whenever(refundStore.getAllRefunds(site, order.id)).thenReturn(emptyList())

        val remoteRefunds = listOf(
            WCRefundModel(
                id = 10L,
                dateCreated = Date(),
                amount = BigDecimal.ONE,
                reason = "Remote refund",
                automaticGatewayRefund = false,
                items = emptyList(),
                shippingLineItems = emptyList(),
                feeLineItems = emptyList()
            )
        )
        whenever(refundStore.fetchAllRefunds(site, order.id)).thenReturn(
            WooResult(model = remoteRefunds)
        )

        // WHEN
        val result = sut.invoke(order)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(10L)
        assertThat(result[0].amount).isEqualTo(BigDecimal.ONE)
        verify(refundStore).getAllRefunds(site, order.id)
        verify(refundStore).fetchAllRefunds(site, order.id)
    }

    @Test
    fun `given order refundTotal is zero, when invoke called, then returns empty list and does not hit store`() = runTest {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder(orderId = 999L, refundTotal = BigDecimal.ZERO)

        // WHEN
        val result = sut.invoke(order)

        // THEN
        assertThat(result).isEmpty()
        verify(refundStore, org.mockito.kotlin.never()).getAllRefunds(any(), any())
    }

    @Test
    fun `given order provided, when invoke called, then passes correct orderId to store`() = runTest {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder(orderId = 456L, refundTotal = BigDecimal.ONE)
        whenever(refundStore.getAllRefunds(any(), any())).thenReturn(emptyList())
        whenever(refundStore.fetchAllRefunds(any(), any(), any(), any())).thenReturn(
            WooResult(emptyList())
        )

        // WHEN
        sut.invoke(order)

        // THEN
        verify(refundStore).getAllRefunds(site, order.id)
    }
}
