package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class AutoSyncOrderStrategyTest : SyncStrategyTest() {
    private val sut: AutoSyncOrder = AutoSyncOrder(createUpdateOrderUseCase)

    @Test
    fun `WHEN the user commits a random change THEN the change is submitted to the API`() = testBlocking {
        val job = sut.syncOrderChanges(orderDraftChanges, retryTrigger)
            .launchIn(this)
        val change = order.copy(customerNote = "testing")
        orderDraftChanges.value = change
        advanceUntilIdle()
        verify(orderCreateEditRepository, times(1)).createOrUpdateDraft(change)
        job.cancel()
    }

    @Test
    fun `WHEN the user commits a price modifier change THEN the change is submitted to the API`() = testBlocking {
        val job = sut.syncOrderChanges(orderDraftChanges, retryTrigger)
            .launchIn(this)
        val change = order.copy(items = OrderTestUtils.generateTestOrderItems())
        orderDraftChanges.value = change
        advanceUntilIdle()
        verify(orderCreateEditRepository, times(1)).createOrUpdateDraft(change)
        job.cancel()
    }

    @Test
    fun `WHEN the order is NOT changed by the user THEN the change is NOT submitted to the API`() = testBlocking {
        val job = sut.syncOrderChanges(orderDraftChanges, retryTrigger)
            .launchIn(this)
        val change = order.copy(number = "12354")
        orderDraftChanges.value = change
        advanceUntilIdle()
        verify(orderCreateEditRepository, times(0)).createOrUpdateDraft(change)
        job.cancel()
    }

    @Test
    fun `WHEN the user commits a change in fee line tax THEN the change is submitted to the API`() = testBlocking {
        val job = sut.syncOrderChanges(orderDraftChanges, retryTrigger)
            .launchIn(this)
        val updatedFeeLines = order.feesLines.map {
            it.taxStatus = Order.FeeLine.FeeLineTaxStatus.TAXABLE
            it
        }
        val change = order.copy(feesLines = updatedFeeLines)
        orderDraftChanges.value = change
        advanceUntilIdle()
        verify(orderCreateEditRepository, times(1)).createOrUpdateDraft(change)
        job.cancel()
    }
}
