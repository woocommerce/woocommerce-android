package com.woocommerce.android.ui.woopos.markorderaspaid

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
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

    private lateinit var repository: WooPosMarkOrderAsPaidRepository

    @Before
    fun setUp() {
        repository = WooPosMarkOrderAsPaidRepository(selectedSite, orderStore, orderMapper)
    }

    @Test
    fun `given remote update succeeds, when markOrderAsPaid with blank note, then payment method 'other' and no note posted`() =
        runTest {
            // GIVEN
            val orderId = 123L
            val site: SiteModel = mock()
            val statusModel = WCOrderStatusModel(statusKey = Order.Status.Completed.value)
            val updateResult = UpdateOrderResult.RemoteUpdateResult(OnOrderChanged())

            whenever(selectedSite.get()).thenReturn(site)
            whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Completed.value)).thenReturn(statusModel)
            whenever(
                orderStore.updateOrderStatusAndPaymentDetails(
                    orderId = orderId,
                    site = site,
                    newStatus = statusModel,
                    newPaymentMethodId = "other",
                    newPaymentMethodTitle = "Other",
                    cashPaymentChangeDueAmount = null,
                )
            ).thenReturn(flowOf(updateResult))

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
                note = org.mockito.kotlin.any(),
                isCustomerNote = org.mockito.kotlin.any(),
            )
        }

    @Test
    fun `given remote update succeeds, when markOrderAsPaid with note, then note posted as non-customer note`() =
        runTest {
            // GIVEN
            val orderId = 123L
            val note = "Bank transfer"
            val site: SiteModel = mock()
            val statusModel = WCOrderStatusModel(statusKey = Order.Status.Completed.value)
            val updateResult = UpdateOrderResult.RemoteUpdateResult(OnOrderChanged())

            whenever(selectedSite.get()).thenReturn(site)
            whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Completed.value)).thenReturn(statusModel)
            whenever(
                orderStore.updateOrderStatusAndPaymentDetails(
                    orderId = orderId,
                    site = site,
                    newStatus = statusModel,
                    newPaymentMethodId = "other",
                    newPaymentMethodTitle = "Other",
                    cashPaymentChangeDueAmount = null,
                )
            ).thenReturn(flowOf(updateResult))
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
            val site: SiteModel = mock()
            val statusModel = WCOrderStatusModel(statusKey = Order.Status.Completed.value)
            val updateResult = UpdateOrderResult.RemoteUpdateResult(OnOrderChanged())

            whenever(selectedSite.get()).thenReturn(site)
            whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Completed.value)).thenReturn(statusModel)
            whenever(
                orderStore.updateOrderStatusAndPaymentDetails(
                    orderId = orderId,
                    site = site,
                    newStatus = statusModel,
                    newPaymentMethodId = "other",
                    newPaymentMethodTitle = "Other",
                    cashPaymentChangeDueAmount = null,
                )
            ).thenReturn(flowOf(updateResult))
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
        val site: SiteModel = mock()
        val statusModel = WCOrderStatusModel(statusKey = Order.Status.Completed.value)
        val errorMessage = "Server error"
        val updateResult = UpdateOrderResult.RemoteUpdateResult(
            event = OnOrderChanged(orderError = WCOrderStore.OrderError(message = errorMessage)),
        )

        whenever(selectedSite.get()).thenReturn(site)
        whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Completed.value)).thenReturn(statusModel)
        whenever(
            orderStore.updateOrderStatusAndPaymentDetails(
                orderId = orderId,
                site = site,
                newStatus = statusModel,
                newPaymentMethodId = "other",
                newPaymentMethodTitle = "Other",
                cashPaymentChangeDueAmount = null,
            )
        ).thenReturn(flowOf(updateResult))

        // WHEN
        val result = repository.markOrderAsPaid(orderId, customerNote = "Bank transfer")

        // THEN
        assertThat(result).isEqualTo(MarkOrderAsPaidOutcome.Failure)
        verify(orderStore, never()).postOrderNote(
            site = org.mockito.kotlin.any(),
            orderId = org.mockito.kotlin.any(),
            note = org.mockito.kotlin.any(),
            isCustomerNote = org.mockito.kotlin.any(),
        )
    }

    @Test
    fun `given no fluxc status for completed, when markOrderAsPaid, then falls back to a synthetic status`() =
        runTest {
            // GIVEN
            val orderId = 321L
            val site: SiteModel = mock()
            val updateResult = UpdateOrderResult.RemoteUpdateResult(OnOrderChanged())

            whenever(selectedSite.get()).thenReturn(site)
            whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Completed.value)).thenReturn(null)
            whenever(
                orderStore.updateOrderStatusAndPaymentDetails(
                    orderId = eq(orderId),
                    site = eq(site),
                    newStatus = org.mockito.kotlin.any(),
                    newPaymentMethodId = eq("other"),
                    newPaymentMethodTitle = eq("Other"),
                    cashPaymentChangeDueAmount = isNull(),
                )
            ).thenReturn(flowOf(updateResult))

            // WHEN
            val result = repository.markOrderAsPaid(orderId, customerNote = null)

            // THEN
            assertThat(result).isEqualTo(MarkOrderAsPaidOutcome.Success)
        }
}
