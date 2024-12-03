package com.woocommerce.android.ui.woopos.home.totals.payment.receipt

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.store.WCOrderStore

class WooPosTotalsPaymentReceiptRepositoryTest {
    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val orderStore: WCOrderStore = mock()
    private val orderCreateEditRepository: OrderCreateEditRepository = mock()
    private val orderMapper: OrderMapper = mock()

    private val repository = WooPosTotalsPaymentReceiptRepository(
        selectedSite,
        orderStore,
        orderCreateEditRepository,
        orderMapper,
    )

    @Test
    fun `given valid order id and email, when sendReceiptByEmail, then return success`() = runTest {
        // GIVEN
        val orderId = 1L
        val email = "test@example.com"
        val mockOrder: Order = mock {
            on { billingAddress }.thenReturn(mock())
            on { customer }.thenReturn(mock())
        }
        whenever(orderStore.getOrderByIdAndSite(orderId, siteModel)).thenReturn(mock())
        whenever(orderMapper.toAppModel(any())).thenReturn(mockOrder)
        whenever(orderCreateEditRepository.createOrUpdateOrder(any(), eq(""))).thenReturn(Result.success(mockOrder))
        val sendOrderReceiptResult = WooPayload<Unit>(Unit)
        whenever(orderStore.sendOrderReceipt(siteModel, orderId)).thenReturn(sendOrderReceiptResult)

        // WHEN
        val result = repository.sendReceiptByEmail(orderId, email)

        // THEN
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `given invalid order id, when sendReceiptByEmail, then return failure`() = runTest {
        // GIVEN
        val orderId = 999L
        val email = "test@example.com"
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(orderStore.getOrderByIdAndSite(orderId, siteModel)).thenReturn(null)

        // WHEN
        val result = repository.sendReceiptByEmail(orderId, email)

        // THEN
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given email update fails, when sendReceiptByEmail, then return failure`() = runTest {
        // GIVEN
        val email = "test@example.com"
        val orderId = 1L
        val mockOrder: Order = mock {
            on { billingAddress }.thenReturn(mock())
            on { customer }.thenReturn(mock())
        }

        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(orderStore.getOrderByIdAndSite(orderId, siteModel)).thenReturn(mock())
        whenever(orderMapper.toAppModel(any())).thenReturn(mockOrder)
        whenever(orderCreateEditRepository.createOrUpdateOrder(anyOrNull(), eq(""))).thenReturn(
            Result.failure(Exception("Update failed"))
        )

        // WHEN
        val result = repository.sendReceiptByEmail(orderId, email)

        // THEN
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given receipt sending fails, when sendReceiptByEmail, then return failure`() = runTest {
        // GIVEN
        val orderId = 1L
        val email = "test@example.com"
        val mockOrder: Order = mock {
            on { billingAddress }.thenReturn(mock())
            on { customer }.thenReturn(mock())
        }
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(orderStore.getOrderByIdAndSite(orderId, siteModel)).thenReturn(mock())
        whenever(orderMapper.toAppModel(any())).thenReturn(mockOrder)
        whenever(orderCreateEditRepository.createOrUpdateOrder(any(), eq(""))).thenReturn(
            Result.success(mockOrder)
        )
        val sendOrderReceiptResult = WooPayload<Unit>(
            WooError(
                WooErrorType.GENERIC_ERROR,
                GenericErrorType.TIMEOUT,
            )
        )
        whenever(orderStore.sendOrderReceipt(siteModel, orderId)).thenReturn(sendOrderReceiptResult)

        // WHEN
        val result = repository.sendReceiptByEmail(orderId, email)

        // THEN
        assertThat(result.isFailure).isTrue()
    }
}
