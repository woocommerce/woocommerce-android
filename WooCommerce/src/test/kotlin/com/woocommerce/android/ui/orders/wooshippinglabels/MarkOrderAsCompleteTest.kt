package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class MarkOrderAsCompleteTest : BaseUnitTest() {
    private lateinit var markOrderAsComplete: MarkOrderAsComplete
    private val orderDetailRepository: OrderDetailRepository = mock()

    private val orderId = 123L

    @Before
    fun setup() {
        markOrderAsComplete = MarkOrderAsComplete(orderDetailRepository)
    }

    @Test
    fun `When update order status succeeds, then return success result`() = testBlocking {
        // Given
        val successEvent = OnOrderChanged()
        val updateResult = UpdateOrderResult.RemoteUpdateResult(successEvent)
        whenever(orderDetailRepository.updateOrderStatus(orderId, CoreOrderStatus.COMPLETED.value))
            .thenReturn(flowOf(updateResult))

        // When
        val result = markOrderAsComplete(orderId)

        // Then
        verify(orderDetailRepository).updateOrderStatus(orderId, CoreOrderStatus.COMPLETED.value)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `When update order status fails, then return failure result with error`() = testBlocking {
        // Given
        val error = OrderError(message = "Error updating order status")
        val failureEvent = OnOrderChanged(orderError = error)
        val updateResult = UpdateOrderResult.RemoteUpdateResult(failureEvent)
        whenever(orderDetailRepository.updateOrderStatus(orderId, CoreOrderStatus.COMPLETED.value))
            .thenReturn(flowOf(updateResult))

        // When
        val result = markOrderAsComplete(orderId)

        // Then
        verify(orderDetailRepository).updateOrderStatus(orderId, CoreOrderStatus.COMPLETED.value)
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is OnChangedException)
        assertEquals(error, exception.error)
    }
}
