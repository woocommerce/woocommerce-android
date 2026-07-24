package com.woocommerce.android.ui.orders

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@ExperimentalCoroutinesApi
class OrdersCommunicationViewModelTest : BaseUnitTest() {
    private val savedStateHandle = SavedStateHandle()
    private val viewModel = OrdersCommunicationViewModel(savedStateHandle)

    @Test
    fun `when no order was created, then no scroll to top is pending`() {
        assertThat(viewModel.createdOrderIdPendingScrollToTop).isNull()
    }

    @Test
    fun `given an order was created, when the pending id is read repeatedly, then it stays set until handled`() {
        viewModel.onOrderCreated(orderId = 123L)

        assertThat(viewModel.createdOrderIdPendingScrollToTop).isEqualTo(123L)
        assertThat(viewModel.createdOrderIdPendingScrollToTopFlow.value).isEqualTo(123L)
        assertThat(viewModel.createdOrderIdPendingScrollToTop).isEqualTo(123L)

        viewModel.onScrollToTopAfterOrderCreationHandled()

        assertThat(viewModel.createdOrderIdPendingScrollToTop).isNull()
        assertThat(viewModel.createdOrderIdPendingScrollToTopFlow.value).isNull()
    }

    @Test
    fun `given an order was created, when the process is recreated, then the pending id survives`() {
        viewModel.onOrderCreated(orderId = 123L)

        val recreatedViewModel = OrdersCommunicationViewModel(savedStateHandle)

        assertThat(recreatedViewModel.createdOrderIdPendingScrollToTop).isEqualTo(123L)
    }
}
