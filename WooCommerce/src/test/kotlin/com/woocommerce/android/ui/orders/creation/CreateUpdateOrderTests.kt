package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus
import com.woocommerce.android.util.InlineClassesAnswer
import com.woocommerce.android.util.advanceTimeAndRun
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

@ExperimentalCoroutinesApi
class CreateUpdateOrderTests : BaseUnitTest() {
    private val orderCreateEditRepository = mock<OrderCreateEditRepository> {
        onBlocking { createOrUpdateOrder(any(), any()) } doAnswer InlineClassesAnswer {
            val order = it.arguments.first() as Order
            Result.success(order.copy(total = order.total + BigDecimal.TEN))
        }
    }
    private val order = Order.getEmptyOrder(Date(), Date()).copy(items = OrderTestUtils.generateTestOrderItems())
    private val orderDraftChanges = MutableStateFlow(order)
    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val sut: CreateUpdateOrder = CreateUpdateOrder(
        dispatchers = coroutinesTestRule.testDispatchers,
        orderCreateEditRepository = orderCreateEditRepository
    )

    @Test
    fun `when there are changes, then update the draft order`() = testBlocking {
        val updateStatuses = mutableListOf<OrderUpdateStatus>()
        val job = sut(orderDraftChanges, retryTrigger)
            .onEach { updateStatuses.add(it) }
            .launchIn(this)

        advanceUntilIdle()

        assertThat(updateStatuses.size).isEqualTo(3)
        assertThat(updateStatuses[0]).isEqualTo(OrderUpdateStatus.PendingDebounce)
        assertThat(updateStatuses[1]).isEqualTo(OrderUpdateStatus.Ongoing)
        assertThat(updateStatuses[2]).isInstanceOf(OrderUpdateStatus.Succeeded::class.java)
        with(updateStatuses[2] as OrderUpdateStatus.Succeeded) {
            assertThat(order)
                .isEqualTo(orderCreateEditRepository.createOrUpdateOrder(orderDraftChanges.value).getOrThrow())
        }

        job.cancel()
    }

    @Test
    fun `when the update fails, then notify the observer`() = testBlocking {
        whenever(orderCreateEditRepository.createOrUpdateOrder(any(), any())).doReturn(Result.failure(Exception()))
        val updateStatuses = mutableListOf<OrderUpdateStatus>()
        val job = sut(orderDraftChanges, retryTrigger)
            .onEach { updateStatuses.add(it) }
            .launchIn(this)

        advanceUntilIdle()

        assertThat(updateStatuses.size).isEqualTo(3)
        assertThat(updateStatuses[0]).isEqualTo(OrderUpdateStatus.PendingDebounce)
        assertThat(updateStatuses[1]).isEqualTo(OrderUpdateStatus.Ongoing)
        assertThat(updateStatuses[2]).isInstanceOf(OrderUpdateStatus.Failed::class.java)

        job.cancel()
    }

    @Test
    fun `when there are changes, then wait for the debounce delay before updating`() = testBlocking {
        val job = sut(orderDraftChanges, retryTrigger)
            .launchIn(this)

        orderDraftChanges.update { draft ->
            draft.copy(items = OrderTestUtils.generateTestOrderItems())
        }

        verify(orderCreateEditRepository, never()).createOrUpdateOrder(any(), any())
        advanceTimeAndRun(CreateUpdateOrder.DEBOUNCE_DURATION_MS)
        verify(orderCreateEditRepository, times(1)).createOrUpdateOrder(any(), any())

        job.cancel()
    }

    @Test
    fun `when retrying, then launch a new request`() = testBlocking {
        whenever(orderCreateEditRepository.createOrUpdateOrder(any(), any()))
            .thenReturn(Result.failure(Exception()))
            .thenAnswer(
                InlineClassesAnswer {
                    val order = it.arguments.first() as Order
                    Result.success(order)
                }
            )

        val updateStatuses = mutableListOf<OrderUpdateStatus>()
        val job = sut(orderDraftChanges, retryTrigger)
            .onEach { updateStatuses.add(it) }
            .launchIn(this)

        // Emit the order draft change and wait for the request
        orderDraftChanges.update { draft ->
            draft.copy(items = OrderTestUtils.generateTestOrderItems())
        }
        advanceUntilIdle()

        // Emit a retry trigger
        retryTrigger.tryEmit(Unit)
        advanceUntilIdle()

        assertThat(updateStatuses.size).isEqualTo(5)
        assertThat(updateStatuses.last()).isInstanceOf(OrderUpdateStatus.Succeeded::class.java)

        job.cancel()
    }
}
