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
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
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
import java.util.Date

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
        val orderId = 123L
        val site: SiteModel = mock()
        val statusModel = WCOrderStatusModel(statusKey = Order.Status.Pending.value)
        val updateResult = UpdateOrderResult.RemoteUpdateResult(OnOrderChanged())

        whenever(selectedSite.get()).thenReturn(site)
        whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Pending.value)).thenReturn(statusModel)
        whenever(orderStore.updateOrderStatus(orderId = orderId, site = site, newStatus = statusModel))
            .thenReturn(flowOf(updateResult))

        val result = repository.promoteOrderToPending(orderId)

        assertThat(result.isSuccess).isTrue()
        verify(orderStore).updateOrderStatus(orderId = eq(orderId), site = eq(site), newStatus = eq(statusModel))
    }

    @Test
    fun `given remote update fails, when promoteOrderToPending, then result is failure`() = runTest {
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

        val result = repository.promoteOrderToPending(orderId)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("boom")
    }

    @Test
    fun `given fetch succeeds, when fetchOrderSnapshot, then mapped Order returned`() = runTest {
        val orderId = 123L
        val site: SiteModel = mock()
        val entity = OrderEntity(localSiteId = LocalId(1), orderId = orderId)
        val order = Order.getEmptyOrder(Date(), Date()).copy(id = orderId)

        whenever(selectedSite.get()).thenReturn(site)
        whenever(orderStore.fetchSingleOrderSync(site, orderId)).thenReturn(WooResult(entity))
        whenever(orderMapper.toAppModel(entity)).thenReturn(order)

        val result = repository.fetchOrderSnapshot(orderId)

        assertThat(result).isEqualTo(order)
    }

    @Test
    fun `given fetch errors, when fetchOrderSnapshot, then null returned`() = runTest {
        val orderId = 123L
        val site: SiteModel = mock()
        whenever(selectedSite.get()).thenReturn(site)
        whenever(orderStore.fetchSingleOrderSync(site, orderId)).thenReturn(
            WooResult(WooError(WooErrorType.GENERIC_ERROR, original = mock())),
        )

        val result = repository.fetchOrderSnapshot(orderId)

        assertThat(result).isNull()
    }

    @Test
    fun `given postOrderNote succeeds, when addOrderNote, then success`() = runTest {
        val orderId = 123L
        val site: SiteModel = mock()
        whenever(selectedSite.get()).thenReturn(site)
        whenever(
            orderStore.postOrderNote(site = eq(site), orderId = eq(orderId), note = any(), isCustomerNote = eq(false))
        )
            .thenReturn(
                WooResult(
                    OrderNoteEntity(
                        localSiteId = LocalId(1),
                        noteId = RemoteId(1L),
                        orderId = RemoteId(orderId),
                        isSystemNote = false,
                        isCustomerNote = false,
                    )
                )
            )

        val result = repository.addOrderNote(orderId, "Customer paid via Scan to Pay")

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
        val orderId = 123L
        val site: SiteModel = mock()
        whenever(selectedSite.get()).thenReturn(site)
        whenever(
            orderStore.postOrderNote(site = eq(site), orderId = eq(orderId), note = any(), isCustomerNote = eq(false))
        )
            .thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, original = mock(), message = "no note")))

        val result = repository.addOrderNote(orderId, "Customer paid via Scan to Pay")

        assertThat(result.isFailure).isTrue()
    }
}
