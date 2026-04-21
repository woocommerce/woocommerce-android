package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import java.util.Date

@ExperimentalCoroutinesApi
class WooPosRetrieveOrderRefundsTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

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
    fun `given refunds exist locally, when invoke called without forceRefresh, then returns cached refunds`() = runTest {
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
        val result = sut.invoke(order, forceRefresh = false)

        // THEN
        assertThat(result.isSuccess).isTrue()
        val refunds = result.getOrThrow()
        assertThat(refunds).hasSize(2)
        assertThat(refunds[0].id).isEqualTo(1L)
        assertThat(refunds[0].amount).isEqualTo(BigDecimal.TEN)
        assertThat(refunds[1].id).isEqualTo(2L)
        assertThat(refunds[1].amount).isEqualTo(BigDecimal.valueOf(5))
        verify(refundStore).getAllRefunds(site, order.id)
    }

    @Test
    fun `given forceRefresh is true, when invoke called, then fetches fresh refunds from network`() = runTest {
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
            )
        )
        whenever(refundStore.fetchAllRefunds(site, order.id)).thenReturn(
            WooResult(model = fluxCRefunds)
        )

        // WHEN
        val result = sut.invoke(order, forceRefresh = true)

        // THEN
        assertThat(result.isSuccess).isTrue()
        val refunds = result.getOrThrow()
        assertThat(refunds).hasSize(1)
        assertThat(refunds[0].id).isEqualTo(1L)
        assertThat(refunds[0].amount).isEqualTo(BigDecimal.TEN)
        verify(refundStore).fetchAllRefunds(site, order.id)
    }

    @Test
    fun `given empty refunds in cache, when invoke called, then falls back to network fetch`() = runTest {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder(orderId = 123L)

        whenever(refundStore.getAllRefunds(site, order.id)).thenReturn(emptyList())
        whenever(refundStore.fetchAllRefunds(site, order.id)).thenReturn(
            WooResult(emptyList())
        )

        // WHEN
        val result = sut.invoke(order)

        // THEN
        assertThat(result.isSuccess).isTrue()
        val refunds = result.getOrThrow()
        assertThat(refunds).isEmpty()
        verify(refundStore).getAllRefunds(site, order.id)
        verify(refundStore).fetchAllRefunds(site, order.id)
    }

    @Test
    fun `given empty cache and network fetch fails, when invoke called, then returns failure`() = runTest {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder(orderId = 123L)

        whenever(refundStore.getAllRefunds(site, order.id)).thenReturn(emptyList())
        whenever(refundStore.fetchAllRefunds(site, order.id)).thenReturn(
            WooResult(WooError(GENERIC_ERROR, UNKNOWN))
        )

        // WHEN
        val result = sut.invoke(order)

        // THEN
        assertThat(result.isFailure).isTrue()
        verify(refundStore).getAllRefunds(site, order.id)
        verify(refundStore).fetchAllRefunds(site, order.id)
    }

    @Test
    fun `given order refundTotal is zero, when invoke called, then still fetches refunds from store`() = runTest {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder(orderId = 999L, refundTotal = BigDecimal.ZERO)
        whenever(refundStore.getAllRefunds(site, order.id)).thenReturn(emptyList())
        whenever(refundStore.fetchAllRefunds(site, order.id)).thenReturn(
            WooResult(emptyList())
        )

        // WHEN
        val result = sut.invoke(order)

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEmpty()
        verify(refundStore).getAllRefunds(site, order.id)
        verify(refundStore).fetchAllRefunds(site, order.id)
    }

    @Test
    fun `given order provided, when invoke called with forceRefresh false and cache has data, then only uses cache`() = runTest {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder(orderId = 456L, refundTotal = BigDecimal.ONE)
        val cachedRefund = WCRefundModel(
            id = 1L,
            dateCreated = Date(),
            amount = BigDecimal.TEN,
            reason = "Cached",
            automaticGatewayRefund = false,
            items = emptyList(),
            shippingLineItems = emptyList(),
            feeLineItems = emptyList()
        )
        whenever(refundStore.getAllRefunds(site, order.id)).thenReturn(listOf(cachedRefund))

        // WHEN
        val result = sut.invoke(order, forceRefresh = false)

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(refundStore).getAllRefunds(site, order.id)
        verify(
            refundStore,
            org.mockito.kotlin.never()
        ).fetchAllRefunds(
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any()
        )
    }

    @Test
    fun `given order provided, when invoke called with forceRefresh true, then passes correct orderId to network`() = runTest {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder(orderId = 456L, refundTotal = BigDecimal.ONE)
        whenever(refundStore.fetchAllRefunds(site, order.id)).thenReturn(
            WooResult(emptyList())
        )

        // WHEN
        sut.invoke(order, forceRefresh = true)

        // THEN
        verify(refundStore).fetchAllRefunds(site, order.id)
    }

    @Test
    fun `given fetch refunds fails, when invoke called with forceRefresh true, then returns failure result`() = runTest {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder(orderId = 789L, refundTotal = BigDecimal.ONE)
        whenever(refundStore.fetchAllRefunds(site, order.id)).thenReturn(
            WooResult(WooError(GENERIC_ERROR, UNKNOWN))
        )

        // WHEN
        val result = sut.invoke(order, forceRefresh = true)

        // THEN
        assertThat(result.isFailure).isTrue()
    }
}
