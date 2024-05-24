package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

private const val ORDER_TOTAL = "100.00"

@ExperimentalCoroutinesApi
class ChangeDueCalculatorViewModelTest : BaseUnitTest() {

    private val order: Order = mock {
        on { total }.thenReturn(BigDecimal(ORDER_TOTAL))
    }

    private val orderDetailRepository: OrderDetailRepository = mock()

    private val savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf("orderId" to 1L))

    private lateinit var viewModel: ChangeDueCalculatorViewModel

    @Test
    fun `given valid order details, when order details are requested, then success state is emitted`() = runTest {
        // GIVEN
        whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)

        // WHEN
        viewModel = ChangeDueCalculatorViewModel(
            savedStateHandle = savedStateHandle,
            orderDetailRepository = orderDetailRepository
        )

        // THEN
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(ChangeDueCalculatorViewModel.UiState.Success::class.java)
        uiState as ChangeDueCalculatorViewModel.UiState.Success
        assertThat(uiState.change).isEqualTo(BigDecimal.ZERO)
        assertThat(uiState.amountDue).isEqualTo(BigDecimal(ORDER_TOTAL))
    }
}
