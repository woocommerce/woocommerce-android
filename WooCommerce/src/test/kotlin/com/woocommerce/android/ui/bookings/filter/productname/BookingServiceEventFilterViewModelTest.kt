package com.woocommerce.android.ui.bookings.filter.productname

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

@OptIn(ExperimentalCoroutinesApi::class)
class BookingServiceEventFilterViewModelTest : BaseUnitTest() {

    private lateinit var viewModel: BookingServiceEventFilterViewModel
    private val productListRepository: ProductListRepository = mock()

    private var latestFilter: BookingsFilterOption.ServiceEvents? = null

    @Before
    fun setup() {
        whenever(
            productListRepository.getProductList(
                productFilterOptions = any(),
                excludedProductIds = any(),
                sortType = anyOrNull()
            )
        ).thenReturn(emptyList())
        latestFilter = null
        viewModel = BookingServiceEventFilterViewModel(
            selectedServiceEvents = null,
            onFilterChanged = { latestFilter = it },
            productListRepository = productListRepository,
            savedStateHandle = SavedStateHandle(),
        )
    }

    @Test
    fun `given no initial filters selected, when ViewModel is initialized, then selected items is DEFAULT`() {
        val state = viewModel.uiState.value
        assertThat(state.selectedProducts).isEqualTo(BookingsFilterOption.ServiceEvents.DEFAULT)
    }

    @Test
    fun `given an initial selection when ViewModel is initialized then state reflects it`() {
        // GIVEN an initial selection
        val initial = BookingsFilterOption.ServiceEvents(
            values = setOf(BookingsFilterOption.ProductInfo(1L, "Haircut"))
        )
        // WHEN creating the ViewModel with that selection
        viewModel = BookingServiceEventFilterViewModel(
            selectedServiceEvents = initial,
            onFilterChanged = { latestFilter = it },
            savedStateHandle = SavedStateHandle(),
            productListRepository = productListRepository,
        )
        // THEN state reflects the initial selection
        val state = viewModel.uiState.value
        assertThat(state.selectedProducts).isEqualTo(initial)
    }

    @Test
    fun `given initial state when search query changes then state updates`() {
        // GIVEN
        val before = viewModel.uiState.value
        assertThat(before.searchQuery).isEqualTo("")
        // WHEN
        viewModel.uiState.value.onSearchQueryChanged("yo")
        // THEN
        val after = viewModel.uiState.value
        assertThat(after.searchQuery).isEqualTo("yo")
    }

    @Test
    fun `given a current selection when selecting Any then selection is cleared and callback receives DEFAULT`() {
        // GIVEN a pre-populated selection by re-creating VM
        val initial = BookingsFilterOption.ServiceEvents(
            values = setOf(BookingsFilterOption.ProductInfo(5L, "Pilates"))
        )
        viewModel = BookingServiceEventFilterViewModel(
            selectedServiceEvents = initial,
            onFilterChanged = { latestFilter = it },
            savedStateHandle = SavedStateHandle(),
            productListRepository = productListRepository,
        )
        // Sanity check
        val before = viewModel.uiState.value
        assertThat(before.selectedProducts).isEqualTo(initial)

        // WHEN tapping on Any
        before.onProductSelected(BookableProduct.Any)
        // THEN the selection is cleared and DEFAULT is emitted
        val after = viewModel.uiState.value
        assertThat(after.selectedProducts.values).isEmpty()
        assertThat(latestFilter).isEqualTo(BookingsFilterOption.ServiceEvents.DEFAULT)
    }

    @Test
    fun `given no selection when selecting a product twice then it toggles and invokes callback`() {
        // GIVEN
        val product = BookableProduct(id = 42L, name = "Yoga")

        // WHEN selecting a product
        viewModel.uiState.value.onProductSelected(product)
        // THEN it becomes selected and callback is invoked
        val selectedOnce = viewModel.uiState.value
        assertThat(selectedOnce.selectedProducts.values)
            .containsExactly(BookingsFilterOption.ProductInfo(42L, "Yoga"))
        assertThat(latestFilter?.values).containsExactly(BookingsFilterOption.ProductInfo(42L, "Yoga"))

        // WHEN tapping again to unselect (toggle off)
        selectedOnce.onProductSelected(product)
        // THEN selection is cleared and callback is invoked with empty selection
        val selectedTwice = viewModel.uiState.value
        assertThat(selectedTwice.selectedProducts.values).isEmpty()
        assertThat(latestFilter?.values).isEmpty()
    }
}
