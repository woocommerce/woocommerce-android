package com.woocommerce.android.ui.woopos.markorderaspaid

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.entity.OrderNoteEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult

class WooPosMarkOrderAsPaidRepositoryTest {

    private val selectedSite: SelectedSite = mock()
    private val orderStore: WCOrderStore = mock()
    private val orderMapper: OrderMapper = mock()
    private val site: SiteModel = mock()
    private val statusModel = WCOrderStatusModel(statusKey = Order.Status.Completed.value)

    private lateinit var repository: WooPosMarkOrderAsPaidRepository

    @Before
    fun setUp() {
        runBlocking {
            repository = WooPosMarkOrderAsPaidRepository(selectedSite, orderStore, orderMapper)
            whenever(selectedSite.get()).thenReturn(site)
            whenever(
                orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Completed.value)
            ).thenReturn(statusModel)
            whenever(
                orderStore.updateOrderStatusAndPaymentDetails(
                    orderId = any(),
                    site = eq(site),
                    newStatus = any(),
                    newPaymentMethodId = eq("other"),
                    newPaymentMethodTitle = eq("Other"),
                    cashPaymentChangeDueAmount = isNull(),
                )
            ).thenReturn(flowOf(UpdateOrderResult.RemoteUpdateResult(OnOrderChanged())))
        }
    }

    @Test
    fun `given remote update succeeds, when markOrderAsPaid with blank note, then payment method 'other' and no note posted`() =
        runTest {
            // GIVEN
            val orderId = 123L

            // WHEN
            val result = repository.markOrderAsPaid(orderId, customerNote = null)

            // THEN
            assertThat(result).isEqualTo(MarkOrderAsPaidOutcome.Success)
            verify(orderStore).updateOrderStatusAndPaymentDetails(
                orderId = eq(orderId),
                site = eq(site),
                newStatus = eq(statusModel),
                newPaymentMethodId = eq("other"),
                newPaymentMethodTitle = eq("Other"),
                cashPaymentChangeDueAmount = isNull(),
            )
            verify(orderStore, never()).postOrderNote(
                site = eq(site),
                orderId = eq(orderId),
                note = any(),
                isCustomerNote = any(),
            )
        }

    @Test
    fun `given remote update succeeds, when markOrderAsPaid with note, then note posted as non-customer note`() =
        runTest {
            // GIVEN
            val orderId = 123L
            val note = "Bank transfer"
            whenever(
                orderStore.postOrderNote(site = site, orderId = orderId, note = note, isCustomerNote = false)
            ).thenReturn(
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

            // WHEN
            val result = repository.markOrderAsPaid(orderId, customerNote = note)

            // THEN
            assertThat(result).isEqualTo(MarkOrderAsPaidOutcome.Success)
            verify(orderStore).postOrderNote(
                site = eq(site),
                orderId = eq(orderId),
                note = eq(note),
                isCustomerNote = eq(false),
            )
        }

    @Test
    fun `given remote update succeeds and note post fails, when markOrderAsPaid, then outcome flags failed note`() =
        runTest {
            // GIVEN
            val orderId = 123L
            val note = "Bank transfer"
            whenever(
                orderStore.postOrderNote(site = site, orderId = orderId, note = note, isCustomerNote = false)
            ).thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, original = mock())))

            // WHEN
            val result = repository.markOrderAsPaid(orderId, customerNote = note)

            // THEN
            assertThat(result).isEqualTo(MarkOrderAsPaidOutcome.SuccessWithFailedNote)
        }

    @Test
    fun `given remote update fails, when markOrderAsPaid, then result is failure and no note posted`() = runTest {
        // GIVEN
        val orderId = 123L
        whenever(
            orderStore.updateOrderStatusAndPaymentDetails(
                orderId = eq(orderId),
                site = eq(site),
                newStatus = eq(statusModel),
                newPaymentMethodId = eq("other"),
                newPaymentMethodTitle = eq("Other"),
                cashPaymentChangeDueAmount = isNull(),
            )
        ).thenReturn(
            flowOf(
                UpdateOrderResult.RemoteUpdateResult(
                    event = OnOrderChanged(orderError = WCOrderStore.OrderError(message = "Server error")),
                )
            )
        )

        // WHEN
        val result = repository.markOrderAsPaid(orderId, customerNote = "Bank transfer")

        // THEN
        assertThat(result).isEqualTo(MarkOrderAsPaidOutcome.Failure)
        verify(orderStore, never()).postOrderNote(
            site = any(),
            orderId = any(),
            note = any(),
            isCustomerNote = any(),
        )
    }

    @Test
    fun `given no fluxc status for completed, when markOrderAsPaid, then falls back to a synthetic status`() =
        runTest {
            // GIVEN
            val orderId = 321L
            whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Completed.value)).thenReturn(null)

            // WHEN
            val result = repository.markOrderAsPaid(orderId, customerNote = null)

            // THEN
            assertThat(result).isEqualTo(MarkOrderAsPaidOutcome.Success)
        }
}
