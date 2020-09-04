package com.woocommerce.android.ui.products

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.variations.VariationListRepository
import com.woocommerce.android.ui.products.variations.VariationListViewModel
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ViewState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.test
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class VariationListViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val variationListRepository: VariationListRepository = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val productRepository: ProductDetailRepository = mock()

    private val productRemoteId = 1L
    private lateinit var viewModel: VariationListViewModel
    private val variations = ProductTestUtils.generateProductVariationList(productRemoteId)
    private val savedState: SavedStateWithArgs = mock()
    private val coroutineDispatchers = CoroutineDispatchers(
            Dispatchers.Unconfined, Dispatchers.Unconfined, Dispatchers.Unconfined)

    @Before
    fun setup() {
        doReturn(MutableLiveData(ViewState())).whenever(savedState).getLiveData<ViewState>(any(), any())
        doReturn(true).whenever(networkStatus).isConnected()
    }

    private fun createViewModel() {
        viewModel = spy(
            VariationListViewModel(
                savedState,
                coroutineDispatchers,
                variationListRepository,
                productRepository,
                networkStatus,
                currencyFormatter
            )
        )
    }

    @Test
    fun `Displays the product variation list view correctly`() {
        doReturn(variations).whenever(variationListRepository).getProductVariationList(productRemoteId)

        createViewModel()

        val fetchedVariationList = ArrayList<ProductVariation>()
        viewModel.variationList.observeForever { it?.let { fetchedVariationList.addAll(it) } }

        viewModel.start(productRemoteId)
        assertThat(fetchedVariationList).isEqualTo(variations)
    }

    @Test
    fun `Do not fetch product variations from api when not connected`() = test {
        doReturn(false).whenever(networkStatus).isConnected()

        createViewModel()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start(productRemoteId)

        verify(variationListRepository, times(1)).getProductVariationList(productRemoteId)
        verify(variationListRepository, times(0)).fetchProductVariations(productRemoteId)

        assertThat(snackbar).isEqualTo(ShowSnackbar(string.offline_error))
    }

    @Test
    fun `Shows and hides product variations skeleton correctly`() = test {
        doReturn(emptyList<ProductVariation>())
            .whenever(variationListRepository).getProductVariationList(productRemoteId)

        createViewModel()

        val isSkeletonShown = ArrayList<Boolean>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { isSkeletonShown.add(it) }
        }

        viewModel.start(productRemoteId)
        assertThat(isSkeletonShown).containsExactly(true, false)
    }

    @Test
    fun `Display empty view on fetch product variations error`() = test {
        whenever(variationListRepository.fetchProductVariations(productRemoteId)).thenReturn(null)
        whenever(variationListRepository.getProductVariationList(productRemoteId)).thenReturn(null)

        createViewModel()

        val showEmptyView = ArrayList<Boolean>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showEmptyView.add(it) }
        }

        viewModel.start(productRemoteId)

        verify(variationListRepository, times(1)).fetchProductVariations(productRemoteId)
        assertThat(showEmptyView).containsExactly(true, false)
    }
}
