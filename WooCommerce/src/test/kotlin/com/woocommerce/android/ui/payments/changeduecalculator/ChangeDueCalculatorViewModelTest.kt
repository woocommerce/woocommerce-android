package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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

    private val parameters: SiteParameters = mock {
        on { currencySymbol }.thenReturn("$")
    }
    private val parameterRepository: ParameterRepository = mock {
        on { getParameters() }.thenReturn(parameters)
    }

    private val savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf("orderId" to 1L))

    private val resourceProvider: ResourceProvider = mock()

    private lateinit var viewModel: ChangeDueCalculatorViewModel

    @Test
    fun `given valid order details, when order details are requested, then success state is emitted`() = runTest {
        // GIVEN
        whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)

        // WHEN
        viewModel = ChangeDueCalculatorViewModel(
            savedStateHandle = savedStateHandle,
            orderDetailRepository = orderDetailRepository,
            parameterRepository = parameterRepository,
            resourceProvider = resourceProvider
        )

        // THEN
        val uiState = viewModel.uiState.value
        assertThat(uiState.change).isEqualTo(BigDecimal.ZERO)
        assertThat(uiState.amountDue).isEqualTo(BigDecimal(ORDER_TOTAL))
        assertThat(uiState.change).isEqualTo(BigDecimal.ZERO)
        assertThat(uiState.amountReceived).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `when updateAmountReceived is called, then amountReceived and change are updated`() = runTest {
        // Given
        whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)
        viewModel = ChangeDueCalculatorViewModel(
            savedStateHandle = savedStateHandle,
            orderDetailRepository = orderDetailRepository,
            parameterRepository = parameterRepository,
            resourceProvider = resourceProvider
        )

        // WHEN
        val amountReceived = BigDecimal("150.00")
        viewModel.updateAmountReceived(amountReceived)
        advanceUntilIdle()

        // THEN
        val uiState = viewModel.uiState.value
        assertThat(uiState.amountReceived).isEqualTo(amountReceived)
        assertThat(uiState.change).isEqualTo(BigDecimal("50.00"))
    }

    @Test
    fun `given amount due is zero, when amount received is zero, then change is zero`() = runTest {
        // GIVEN
        val emptyOrder: Order = mock {
            on { total }.thenReturn(BigDecimal.ZERO)
        }
        whenever(orderDetailRepository.getOrderById(any())).thenReturn(emptyOrder)

        // WHEN
        viewModel = ChangeDueCalculatorViewModel(
            savedStateHandle = savedStateHandle,
            orderDetailRepository = orderDetailRepository,
            parameterRepository = parameterRepository,
            resourceProvider = resourceProvider
        )
        viewModel.updateAmountReceived(BigDecimal.ZERO)
        advanceUntilIdle()

        // THEN
        val uiState = viewModel.uiState.value
        assertThat(uiState.amountDue).isEqualTo(BigDecimal.ZERO)
        assertThat(uiState.amountReceived).isEqualTo(BigDecimal.ZERO)
        assertThat(uiState.change).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `when amount received is less than amount due, then change is negative`() = runTest {
        // GIVEN
        whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)
        val amountReceived = BigDecimal("50.00")

        // WHEN
        viewModel = ChangeDueCalculatorViewModel(
            savedStateHandle = savedStateHandle,
            orderDetailRepository = orderDetailRepository,
            parameterRepository = parameterRepository,
            resourceProvider = resourceProvider
        )
        viewModel.updateAmountReceived(amountReceived)
        advanceUntilIdle()

        // THEN
        val uiState = viewModel.uiState.value
        assertThat(uiState.amountDue).isEqualTo(BigDecimal(ORDER_TOTAL))
        assertThat(uiState.amountReceived).isEqualTo(amountReceived)
        assertThat(uiState.change).isEqualTo(BigDecimal("-50.00"))
    }

    @Test
    fun `when amount received is greater than or equal to amount due, then order can be completed`() = runTest {
        // GIVEN
        whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)
        viewModel = ChangeDueCalculatorViewModel(
            savedStateHandle = savedStateHandle,
            orderDetailRepository = orderDetailRepository,
            parameterRepository = parameterRepository,
            resourceProvider = resourceProvider
        )

        // WHEN
        val amountReceived = BigDecimal("100.00")
        viewModel.updateAmountReceived(amountReceived)
        advanceUntilIdle()
        val canCompleteOrder = viewModel.uiState.value.canCompleteOrder

        // THEN
        val uiState = viewModel.uiState.value
        assertThat(uiState.amountDue).isEqualTo(BigDecimal(ORDER_TOTAL))
        assertThat(uiState.amountReceived).isEqualTo(amountReceived)
        assertThat(uiState.change).isEqualTo(BigDecimal("0.00"))
        assertThat(canCompleteOrder).isTrue
    }

    @Test
    fun `when updateRecordTransactionDetailsChecked is called, then recordTransactionDetailsChecked state is updated`() =
        runTest {
            // GIVEN
            whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)
            viewModel = ChangeDueCalculatorViewModel(
                savedStateHandle = savedStateHandle,
                orderDetailRepository = orderDetailRepository,
                parameterRepository = parameterRepository,
                resourceProvider = resourceProvider
            )

            // WHEN
            viewModel.updateRecordTransactionDetailsChecked(true)
            advanceUntilIdle()

            // THEN
            val isChecked = viewModel.uiState.value.recordTransactionDetailsChecked
            assertThat(isChecked).isTrue
        }

    @Test
    fun `when getCurrencySymbol is called, then currency symbol from parameter repository is returned`() = runTest {
        // GIVEN
        val currencySymbol = "$"
        whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)
        val siteParameters: SiteParameters = mock()
        whenever(siteParameters.currencySymbol).thenReturn(currencySymbol)
        whenever(parameterRepository.getParameters()).thenReturn(siteParameters)
        viewModel = ChangeDueCalculatorViewModel(
            savedStateHandle = savedStateHandle,
            orderDetailRepository = orderDetailRepository,
            parameterRepository = parameterRepository,
            resourceProvider = resourceProvider
        )

        // WHEN
        val result = viewModel.uiState.value.currencySymbol

        // THEN
        assertThat(result).isEqualTo(currencySymbol)
    }
}
