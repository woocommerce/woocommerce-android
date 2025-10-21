package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.tools.SelectedSite
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
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import java.util.Date

class WooPosGetOrderRefundsByOrderIdTest {
    private lateinit var refundStore: WCRefundStore
    private lateinit var selectedSite: SelectedSite
    private lateinit var sut: WooPosGetOrderRefundsByOrderId
    private lateinit var site: SiteModel

    @Before
    fun setup() {
        refundStore = mock()
        selectedSite = mock()
        site = mock()

        whenever(selectedSite.get()).thenReturn(site)

        sut = WooPosGetOrderRefundsByOrderId(
            refundStore = refundStore,
            selectedSite = selectedSite
        )
    }

    @Test
    fun `given refunds exist, when invoke called, then returns mapped refunds`() = runTest {
        // GIVEN
        val orderId = 123L
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
        whenever(refundStore.getAllRefunds(site, orderId)).thenReturn(fluxCRefunds)

        // WHEN
        val result = sut.invoke(orderId)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(1L)
        assertThat(result[0].amount).isEqualTo(BigDecimal.TEN)
        assertThat(result[1].id).isEqualTo(2L)
        assertThat(result[1].amount).isEqualTo(BigDecimal.valueOf(5))
    }

    @Test
    fun `given no refunds exist, when invoke called, then returns empty list`() = runTest {
        // GIVEN
        val orderId = 123L
        whenever(refundStore.getAllRefunds(site, orderId)).thenReturn(emptyList())

        // WHEN
        val result = sut.invoke(orderId)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given orderId provided, when invoke called, then passes correct orderId to store`() = runTest {
        // GIVEN
        val orderId = 456L
        whenever(refundStore.getAllRefunds(any(), any())).thenReturn(emptyList())

        // WHEN
        sut.invoke(orderId)

        // THEN
        verify(refundStore).getAllRefunds(site, orderId)
    }
}
