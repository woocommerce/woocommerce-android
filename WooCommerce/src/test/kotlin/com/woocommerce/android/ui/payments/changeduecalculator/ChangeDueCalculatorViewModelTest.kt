package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal

private const val ORDER_TOTAL = "100.00"

@ExperimentalCoroutinesApi
class ChangeDueCalculatorViewModelTest : BaseUnitTest() {

    private val site: SiteModel = mock()
    private val order: Order = mock {
        on { total }.thenReturn(BigDecimal(ORDER_TOTAL))
    }

    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(site)
    }
    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(any<BigDecimal>(), any(), any()) }.thenReturn(ORDER_TOTAL)
    }
    private val wooCommerceStore: WooCommerceStore = mock {
        on { getSiteSettings(site) }.thenReturn(mock())
    }

    private val orderDetailRepository: OrderDetailRepository = mock()

    private var savedStateHandle: SavedStateHandle = mock {
        SavedStateHandle(mapOf("orderId" to 1L))
    }
    private lateinit var viewModel: ChangeDueCalculatorViewModel

    @Before
    fun setup() {
        savedStateHandle = SavedStateHandle(mapOf("orderId" to 1L))
    }

    @Test
    fun `given valid order details, when order details are requested, then success state is emitted`() = runTest {
        // GIVEN
        val expectedAmountDue = "100.00"
        val expectedChange = BigDecimal("0.0")
        whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)

        // WHEN
        viewModel = initViewModel()

        // THEN
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(ChangeDueCalculatorViewModel.UiState.Success::class.java)
        uiState as ChangeDueCalculatorViewModel.UiState.Success
        assertThat(uiState.change).isEqualTo(expectedChange)
        assertThat(uiState.amountDue).isEqualTo(expectedAmountDue)
    }

    private fun initViewModel(): ChangeDueCalculatorViewModel {
        return ChangeDueCalculatorViewModel(
            selectedSite,
            currencyFormatter,
            savedStateHandle,
            wooCommerceStore,
            orderDetailRepository
        )
    }
}
