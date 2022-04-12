package com.woocommerce.android.ui.orders.simplepayments

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.assertj.core.api.Assertions.assertThat

@ExperimentalCoroutinesApi
class TakePaymentFragmentViewModelTests : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
        private const val REMOTE_ORDER_NUMBER = "100"
        private const val ORDER_PAYMENT_URL = "https://automattic.com"
    }

    private val testOrder: Order
        get() {
            return OrderTestUtils.generateTestOrder(ORDER_ID).copy(
                number = REMOTE_ORDER_NUMBER,
                paymentUrl = ORDER_PAYMENT_URL
            )
        }

    private lateinit var viewModel: TakePaymentViewModel

    private val savedState: SavedStateHandle =
        TakePaymentFragmentArgs(order = testOrder).initSavedStateHandle()

    private fun initViewModel() {
        viewModel = TakePaymentViewModel(savedState, mock(), mock(), mock(), mock(), mock() )
    }

    @Test
    fun `when payment url exists, then share payment link is enabled`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            initViewModel()
            var state: TakePaymentViewModel.ViewState? = null
            viewModel.viewStateLiveData.observeForever { _, viewState ->
                state = viewState
            }
            assertThat(state).isNotNull
            assertThat(state!!.isSharePaymentUrlEnabled).isTrue()
        }

}
