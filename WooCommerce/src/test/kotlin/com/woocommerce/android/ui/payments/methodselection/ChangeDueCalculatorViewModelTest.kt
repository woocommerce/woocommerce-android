package com.woocommerce.android.ui.payments.methodselection

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import kotlin.test.assertEquals

private const val PAYMENT_URL = "paymentUrl"
private const val ORDER_TOTAL = "100$"

@ExperimentalCoroutinesApi
class ChangeDueCalculatorViewModelTest : BaseUnitTest() {

    private val site: SiteModel = mock {
        on { name }.thenReturn("siteName")
    }
    private val order: Order = mock {
        on { paymentUrl }.thenReturn(PAYMENT_URL)
        on { total }.thenReturn(BigDecimal(1L))
        on { id }.thenReturn(1L)
        on { currency }.thenReturn("USD")
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

    private val savedStateHandle: SavedStateHandle = mock()

    private lateinit var viewModel: ChangeDueCalculatorViewModel

    @Before
    fun setup() {
        viewModel = initViewModel()
    }

    @Test
    fun `given valid order details, when order details are requested, then success state is emitted`() = runTest {
        // GIVEN
        val expectedAmountDue = "100.00"
        val expectedChange = BigDecimal("20.00")
        whenever(orderDetailRepository.getOrderById(1)).thenReturn(order)

        // WHEN
        // viewModel.fetchOrderDetails()
        viewModel = initViewModel()

        // THEN
        val uiState = viewModel.uiState.value
        assert(uiState is ChangeDueCalculatorViewModel.UiState.Success)
        uiState as ChangeDueCalculatorViewModel.UiState.Success
        assertEquals(expectedAmountDue, uiState.amountDue)
        assertEquals(expectedChange, uiState.change)
    }

    @Test
    fun `given order details retrieval failure, when order details are loaded, then error state is emitted`() = runTest {
        // GIVEN
        // whenever(repository.getOrderDetails()).thenThrow(RuntimeException("Error fetching order details"))

        // WHEN
        // viewModel.fetchOrderDetails()

        // THEN
        // val uiState = viewModel.uiState.value
        // assert(uiState is ChangeDueCalculatorViewModel.UiState.Error)
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
