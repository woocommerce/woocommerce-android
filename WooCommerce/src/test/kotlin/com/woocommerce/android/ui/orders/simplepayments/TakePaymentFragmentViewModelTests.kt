package com.woocommerce.android.ui.orders.simplepayments

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class TakePaymentFragmentViewModelTests : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
        private const val REMOTE_ORDER_NUMBER = "100"
        private const val ORDER_PAYMENT_URL = "https://automattic.com"
    }

    private val testOrderWithPaymentUrl: Order
        get() {
            return OrderTestUtils.generateTestOrder(ORDER_ID).copy(
                number = REMOTE_ORDER_NUMBER,
                paymentUrl = ORDER_PAYMENT_URL
            )
        }

    private val testOrderWithoutPaymentUrl: Order
        get() {
            return OrderTestUtils.generateTestOrder(ORDER_ID).copy(
                number = REMOTE_ORDER_NUMBER
            )
        }

    private lateinit var viewModel: TakePaymentViewModel

    private fun initViewModel(order: Order) {
        val savedState: SavedStateHandle = TakePaymentFragmentArgs(order = order).initSavedStateHandle()
        viewModel = TakePaymentViewModel(savedState, mock(), mock(), mock(), mock(), mock() )
    }

    @Test
    fun `when payment url exists, then share payment link is enabled`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            initViewModel(testOrderWithPaymentUrl)
            var state: TakePaymentViewModel.ViewState? = null
            viewModel.viewStateLiveData.observeForever { _, viewState ->
                state = viewState
            }
            assertThat(state).isNotNull
            assertThat(state!!.isSharePaymentUrlEnabled).isTrue()
        }

    @Test
    fun `when payment url is empty, then share payment link is not enabled`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            initViewModel(testOrderWithoutPaymentUrl)
            var state: TakePaymentViewModel.ViewState? = null
            viewModel.viewStateLiveData.observeForever { _, viewState ->
                state = viewState
            }
            assertThat(state).isNotNull
            assertThat(state!!.isSharePaymentUrlEnabled).isFalse()
        }
}
