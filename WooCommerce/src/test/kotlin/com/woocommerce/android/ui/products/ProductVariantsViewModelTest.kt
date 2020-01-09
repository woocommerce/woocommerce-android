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
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductVariantsViewModel.ViewState
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

class ProductVariantsViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val productVariantsRepository: ProductVariantsRepository = mock()
    private val currencyFormatter: CurrencyFormatter = mock()

    private val productRemoteId = 1L
    private lateinit var viewModel: ProductVariantsViewModel
    private val productVariants = ProductTestUtils.generateProductVariantList(productRemoteId)
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
                ProductVariantsViewModel(
                        savedState,
                        coroutineDispatchers,
                        productVariantsRepository,
                        networkStatus,
                        currencyFormatter
                )
        )
    }

    @Test
    fun `Displays the product variant list view correctly`() {
        doReturn(productVariants).whenever(productVariantsRepository).getProductVariantList(productRemoteId)

        createViewModel()

        val fetchedProductVariantList = ArrayList<ProductVariant>()
        viewModel.productVariantList.observeForever { it?.let { fetchedProductVariantList.addAll(it) } }

        viewModel.start(productRemoteId)
        assertThat(fetchedProductVariantList).isEqualTo(productVariants)
    }

    @Test
    fun `Do not fetch product variants from api when not connected`() = test {
        doReturn(false).whenever(networkStatus).isConnected()

        createViewModel()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start(productRemoteId)

        verify(productVariantsRepository, times(1)).getProductVariantList(productRemoteId)
        verify(productVariantsRepository, times(0)).fetchProductVariants(productRemoteId)

        assertThat(snackbar).isEqualTo(ShowSnackbar(string.offline_error))
    }

    @Test
    fun `Shows and hides product variants skeleton correctly`() = test {
        doReturn(emptyList<ProductVariant>()).whenever(productVariantsRepository).getProductVariantList(productRemoteId)

        createViewModel()

        val isSkeletonShown = ArrayList<Boolean>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { isSkeletonShown.add(it) }
        }

        viewModel.start(productRemoteId)
        assertThat(isSkeletonShown).containsExactly(true, false)
    }

    @Test
    fun `Display empty view on fetch product variants error`() = test {
        whenever(productVariantsRepository.fetchProductVariants(productRemoteId)).thenReturn(null)
        whenever(productVariantsRepository.getProductVariantList(productRemoteId)).thenReturn(null)

        createViewModel()

        val showEmptyView = ArrayList<Boolean>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showEmptyView.add(it) }
        }

        viewModel.start(productRemoteId)

        verify(productVariantsRepository, times(1)).fetchProductVariants(productRemoteId)
        assertThat(showEmptyView).containsExactly(true, false)
    }
}
