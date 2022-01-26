package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.creation.CreateOrUpdateOrderDraft.OrderDraftUpdateStatus
import com.woocommerce.android.util.InlineClassesAnswer
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.flow.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.*
import java.math.BigDecimal

class CreateOrUpdateOrderDraftTests : BaseUnitTest() {
    private val orderCreationRepository = mock<OrderCreationRepository> {
        onBlocking { createOrUpdateDraft(any()) } doAnswer InlineClassesAnswer {
            val order = it.arguments.first() as Order
            Result.success(order.copy(total = order.total + BigDecimal.TEN))
        }
    }
    private val orderDraftChanges = MutableStateFlow(Order.EMPTY)
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

        orderDraftChanges.update { draft ->
            draft.copy(items = OrderTestUtils.generateTestOrderItems())
        }

        advanceUntilIdle()

        assertThat(updateStatuses.size).isEqualTo(2)
        assertThat(updateStatuses[0]).isEqualTo(OrderDraftUpdateStatus.Ongoing)
        assertThat(updateStatuses[1]).isInstanceOf(OrderDraftUpdateStatus.Succeeded::class.java)
        with(updateStatuses[1] as OrderDraftUpdateStatus.Succeeded) {
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

        orderDraftChanges.update { draft ->
            draft.copy(items = OrderTestUtils.generateTestOrderItems())
        }

        advanceUntilIdle()

        assertThat(updateStatuses.size).isEqualTo(2)
        assertThat(updateStatuses[0]).isEqualTo(OrderDraftUpdateStatus.Ongoing)
        assertThat(updateStatuses[1]).isInstanceOf(OrderDraftUpdateStatus.Failed::class.java)

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
        advanceTimeBy(CreateOrUpdateOrderDraft.DEBOUNCE_DURATION_MS)
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

        assertThat(updateStatuses.size).isEqualTo(4)
        assertThat(updateStatuses.last()).isInstanceOf(OrderDraftUpdateStatus.Succeeded::class.java)

        job.cancel()
    }
}
