package com.woocommerce.android.ui.woopos.scantopay

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.persistence.entity.OrderNoteEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult

class WooPosScanToPayRepositoryTest {

    private val selectedSite: SelectedSite = mock()
    private val orderStore: WCOrderStore = mock()
    private val orderMapper: OrderMapper = mock()

    private lateinit var repository: WooPosScanToPayRepository

    @Before
    fun setUp() {
        repository = WooPosScanToPayRepository(selectedSite, orderStore, orderMapper)
    }

    @Test
    fun `given remote update succeeds, when promoteOrderToPending, then status is set to pending`() = runTest {
        // GIVEN
        val orderId = 123L
        val site: SiteModel = mock()
        val statusModel = WCOrderStatusModel(statusKey = Order.Status.Pending.value)
        val updateResult = UpdateOrderResult.RemoteUpdateResult(mock { on { isError }.thenReturn(false) })

        whenever(selectedSite.get()).thenReturn(site)
        whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Pending.value)).thenReturn(statusModel)
        whenever(orderStore.updateOrderStatus(orderId = orderId, site = site, newStatus = statusModel))
            .thenReturn(flowOf(updateResult))

        // WHEN
        val result = repository.promoteOrderToPending(orderId)

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(orderStore).updateOrderStatus(orderId = eq(orderId), site = eq(site), newStatus = eq(statusModel))
    }

    @Test
    fun `given remote update fails, when promoteOrderToPending, then result is failure`() = runTest {
        // GIVEN
        val orderId = 123L
        val site: SiteModel = mock()
        val statusModel = WCOrderStatusModel(statusKey = Order.Status.Pending.value)
        val updateResult = UpdateOrderResult.RemoteUpdateResult(
            event = OnOrderChanged(orderError = WCOrderStore.OrderError(message = "boom")),
        )

        whenever(selectedSite.get()).thenReturn(site)
        whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Pending.value)).thenReturn(statusModel)
        whenever(orderStore.updateOrderStatus(orderId = orderId, site = site, newStatus = statusModel))
            .thenReturn(flowOf(updateResult))

        // WHEN
        val result = repository.promoteOrderToPending(orderId)

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("boom")
    }

    @Test
    fun `given fetch succeeds, when fetchOrderSnapshot, then mapped Order returned`() = runTest {
        // GIVEN
        val orderId = 123L
        val site: SiteModel = mock()
        val entity: OrderEntity = mock()
        val order: Order = mock()

        whenever(selectedSite.get()).thenReturn(site)
        whenever(orderStore.fetchSingleOrderSync(site, orderId)).thenReturn(WooResult(entity))
        whenever(orderMapper.toAppModel(entity)).thenReturn(order)

        // WHEN
        val result = repository.fetchOrderSnapshot(orderId)

        // THEN
        assertThat(result).isEqualTo(order)
    }

    @Test
    fun `given fetch errors, when fetchOrderSnapshot, then null returned`() = runTest {
        // GIVEN
        val orderId = 123L
        val site: SiteModel = mock()
        whenever(selectedSite.get()).thenReturn(site)
        whenever(orderStore.fetchSingleOrderSync(site, orderId)).thenReturn(
            WooResult(WooError(WooErrorType.GENERIC_ERROR, original = mock())),
        )

        // WHEN
        val result = repository.fetchOrderSnapshot(orderId)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given postOrderNote succeeds, when addOrderNote, then success`() = runTest {
        // GIVEN
        val orderId = 123L
        val site: SiteModel = mock()
        whenever(selectedSite.get()).thenReturn(site)
        whenever(
            orderStore.postOrderNote(site = eq(site), orderId = eq(orderId), note = any(), isCustomerNote = eq(false))
        )
            .thenReturn(WooResult(mock<OrderNoteEntity>()))

        // WHEN
        val result = repository.addOrderNote(orderId, "Customer paid via Scan to Pay")

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(orderStore).postOrderNote(
            site = eq(site),
            orderId = eq(orderId),
            note = eq("Customer paid via Scan to Pay"),
            isCustomerNote = eq(false),
        )
    }

    @Test
    fun `given postOrderNote fails, when addOrderNote, then failure with message`() = runTest {
        // GIVEN
        val orderId = 123L
        val site: SiteModel = mock()
        whenever(selectedSite.get()).thenReturn(site)
        whenever(
            orderStore.postOrderNote(site = eq(site), orderId = eq(orderId), note = any(), isCustomerNote = eq(false))
        )
            .thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, original = mock(), message = "no note")))

        // WHEN
        val result = repository.addOrderNote(orderId, "Customer paid via Scan to Pay")

        // THEN
        assertThat(result.isFailure).isTrue()
    }
}
