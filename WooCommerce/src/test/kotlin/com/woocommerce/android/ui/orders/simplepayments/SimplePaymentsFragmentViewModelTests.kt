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

@ExperimentalCoroutinesApi
class SimplePaymentsFragmentViewModelTests : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
        private const val REMOTE_ORDER_NUMBER = "100"
        private const val TAX_LINE_ID = 1L
        private const val TAX_LINE_TAX_RATE = 0.15f
    }

    private val testOrder: Order
        get() {
            val taxLines = ArrayList<Order.TaxLine>().also {
                it.add(
                    Order.TaxLine(
                        id = TAX_LINE_ID,
                        compound = false,
                        taxTotal = "10.00",
                        ratePercent = TAX_LINE_TAX_RATE
                    )
                )
            }
            return OrderTestUtils.generateTestOrder(ORDER_ID).copy(number = REMOTE_ORDER_NUMBER, taxLines = taxLines)
        }

    private lateinit var viewModel: SimplePaymentsFragmentViewModel

    private val savedState: SavedStateHandle =
        SimplePaymentsFragmentArgs(order = testOrder).initSavedStateHandle()

    private fun initViewModel() {
        viewModel = SimplePaymentsFragmentViewModel(savedState)
    }

    @Test
    fun `when charging taxes is enabled, then tax rate is taken from first tax line`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            initViewModel()
            viewModel.onChargeTaxesChanged(chargeTaxes = true)
            assertThat(viewModel.taxRatePercent).isEqualTo(TAX_LINE_TAX_RATE.toString())
        }

    @Test
    fun `when charging taxes is NOT enabled, then tax rate is zero`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            initViewModel()
            viewModel.onChargeTaxesChanged(chargeTaxes = false)
            assertThat(viewModel.taxRatePercent).isEqualTo(SimplePaymentsFragmentViewModel.EMPTY_TAX_RATE)
        }
}
