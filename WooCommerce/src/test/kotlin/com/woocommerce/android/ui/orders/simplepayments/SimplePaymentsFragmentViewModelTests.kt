package com.woocommerce.android.ui.orders.simplepayments

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class SimplePaymentsFragmentViewModelTests : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
        private const val REMOTE_ORDER_NUMBER = "100"
        private const val TAX_LINE_ID = 1L
        private const val TAX_LINE_TAX_RATE = 0.10f
    }

    private val repository: OrderDetailRepository = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val networkStatus: NetworkStatus = mock()

    private val testOrder: Order
        get() {
            val taxLines = ArrayList<Order.TaxLine>().also {
                it.add(
                    Order.TaxLine(
                        id = TAX_LINE_ID,
                        compound = false,
                        taxTotal = "10.00",
                        shippingTaxTotal = "0.00",
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
    fun `show tax rate when charge taxes enabled`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        initViewModel()
        viewModel.onChargeTaxesChanged(chargeTaxes = true)
        assertThat(viewModel.taxRatePercent).isEqualTo(TAX_LINE_TAX_RATE)
    }
}
