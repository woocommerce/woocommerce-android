package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.creation.CreateOrUpdateOrderDraft.OrderDraftUpdateStatus
import com.woocommerce.android.util.InlineClassesAnswer
import com.woocommerce.android.util.advanceTimeAndRun
import com.woocommerce.android.viewmodel.BaseUnitTest
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

class CreateOrUpdateOrderDraftTests : BaseUnitTest() {
    private val orderCreationRepository = mock<OrderCreationRepository> {
        onBlocking { createOrUpdateDraft(any()) } doAnswer InlineClassesAnswer {
            val order = it.arguments.first() as Order
            Result.success(order.copy(total = order.total + BigDecimal.TEN))
        }
    }
    private val order = Order.EMPTY.copy(items = OrderTestUtils.generateTestOrderItems())
    private val orderDraftChanges = MutableStateFlow(order)
    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val sut: CreateOrUpdateOrderDraft = CreateOrUpdateOrderDraft(
        dispatchers = coroutinesTestRule.testDispatchers,
        orderCreationRepository = orderCreationRepository
    )

    @Test
    fun `when there are changes, then update the draft order`() = testBlocking {
        val updateStatuses = mutableListOf<OrderDraftUpdateStatus>()
        val job = sut(orderDraftChanges, retryTrigger)
            .onEach { updateStatuses.add(it) }
            .launchIn(this)

        advanceUntilIdle()

        assertThat(updateStatuses.size).isEqualTo(3)
        assertThat(updateStatuses[0]).isEqualTo(OrderDraftUpdateStatus.PendingDebounce)
        assertThat(updateStatuses[1]).isEqualTo(OrderDraftUpdateStatus.Ongoing)
        assertThat(updateStatuses[2]).isInstanceOf(OrderDraftUpdateStatus.Succeeded::class.java)
        with(updateStatuses[2] as OrderDraftUpdateStatus.Succeeded) {
            assertThat(order)
                .isEqualTo(orderCreationRepository.createOrUpdateDraft(orderDraftChanges.value).getOrThrow())
        }

        job.cancel()
    }

    @Test
    fun `when the update fails, then notify the observer`() = testBlocking {
        whenever(orderCreationRepository.createOrUpdateDraft(any())).doReturn(Result.failure(Exception()))
        val updateStatuses = mutableListOf<OrderDraftUpdateStatus>()
        val job = sut(orderDraftChanges, retryTrigger)
            .onEach { updateStatuses.add(it) }
            .launchIn(this)

        advanceUntilIdle()

        assertThat(updateStatuses.size).isEqualTo(3)
        assertThat(updateStatuses[0]).isEqualTo(OrderDraftUpdateStatus.PendingDebounce)
        assertThat(updateStatuses[1]).isEqualTo(OrderDraftUpdateStatus.Ongoing)
        assertThat(updateStatuses[2]).isInstanceOf(OrderDraftUpdateStatus.Failed::class.java)

        job.cancel()
    }

    @Test
    fun `when there are changes, then wait for the debounce delay before updating`() = testBlocking {
        val job = sut(orderDraftChanges, retryTrigger)
            .launchIn(this)

        orderDraftChanges.update { draft ->
            draft.copy(items = OrderTestUtils.generateTestOrderItems())
        }

        verify(orderCreationRepository, never()).createOrUpdateDraft(any())
        advanceTimeAndRun(CreateOrUpdateOrderDraft.DEBOUNCE_DURATION_MS)
        verify(orderCreationRepository, times(1)).createOrUpdateDraft(any())

        job.cancel()
    }

    @Test
    fun `when retrying, then launch a new request`() = testBlocking {
        whenever(orderCreationRepository.createOrUpdateDraft(any()))
            .thenReturn(Result.failure(Exception()))
            .thenAnswer(
                InlineClassesAnswer {
                    val order = it.arguments.first() as Order
                    Result.success(order)
                }
            )

        val updateStatuses = mutableListOf<OrderDraftUpdateStatus>()
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
        assertThat(updateStatuses.last()).isInstanceOf(OrderDraftUpdateStatus.Succeeded::class.java)

        job.cancel()
    }
}
