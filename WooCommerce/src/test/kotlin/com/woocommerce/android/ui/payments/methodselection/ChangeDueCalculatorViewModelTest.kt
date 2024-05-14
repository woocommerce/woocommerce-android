package com.woocommerce.android.ui.payments.methodselection

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.methodselection.ChangeDueCalculatorViewModel
import com.woocommerce.android.ui.payments.methodselection.ChangeDueCalculatorViewModel.UiState
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ChangeDueCalculatorViewModelTest : BaseUnitTest() {

    private lateinit var orderDetailRepository: OrderDetailRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: ChangeDueCalculatorViewModel

    @Before
    fun setup() {
        orderDetailRepository = mock()
        savedStateHandle = mock()

        whenever(savedStateHandle.get<Long>("orderId")).thenReturn(1L)
        viewModel = ChangeDueCalculatorViewModel(savedStateHandle, orderDetailRepository)
    }

    @Test
    fun `order details load successfully emits success state`() = testBlocking {
        // TODO
    }

    @Test
    fun `order details load failure emits error state`() = testBlocking {
        // TODO
        viewModel.loadOrderDetails()

        assert(viewModel.uiState.value is UiState.Error)
    }
}
