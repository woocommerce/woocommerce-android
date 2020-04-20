package com.woocommerce.android.ui.products

import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductListViewModel.ViewState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.test
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ProductListViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val productRepository: ProductListRepository = mock()
    private val savedState: SavedStateWithArgs = mock()

    private val coroutineDispatchers = CoroutineDispatchers(
            Dispatchers.Unconfined, Dispatchers.Unconfined, Dispatchers.Unconfined)
    private val productList = ProductTestUtils.generateProductList()
    private lateinit var viewModel: ProductListViewModel

    @Before
    fun setup() {
        doReturn(MutableLiveData(ViewState())).whenever(savedState).getLiveData<ViewState>(any(), any())
        doReturn(true).whenever(networkStatus).isConnected()
    }

    private fun createViewModel() {
        viewModel = spy(
                ProductListViewModel(
                        savedState,
                        coroutineDispatchers,
                        productRepository,
                        networkStatus
                )
        )
    }

    @Test
    fun `Displays the product list view correctly`() = test {
        doReturn(productList).whenever(productRepository).fetchProductList(productFilterOptions = emptyMap())

        createViewModel()

        val products = ArrayList<Product>()
        viewModel.productList.observeForever {
            it?.let { products.addAll(it) }
        }

        assertThat(products).isEqualTo(productList)
    }

    @Test
    fun `Do not fetch product list from api when not connected`() = test {
        doReturn(false).whenever(networkStatus).isConnected()

        createViewModel()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        verify(productRepository, times(1)).getProductList(any())
        verify(productRepository, times(0)).fetchProductList(productFilterOptions = emptyMap())

        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
    }

    @Test
    fun `Shows and hides product list skeleton correctly`() = test {
        doReturn(emptyList<Product>()).whenever(productRepository).getProductList(any())
        doReturn(emptyList<Product>()).whenever(productRepository).fetchProductList(any(), any())

        createViewModel()

        val isSkeletonShown = ArrayList<Boolean>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { isSkeletonShown.add(it) }
        }

        viewModel.loadProducts()

        assertThat(isSkeletonShown).containsExactly(false, true, false)
    }

    @Test
    fun `Shows and hides product list load more progress correctly`() = test {
        doReturn(true).whenever(productRepository).canLoadMoreProducts
        doReturn(emptyList<Product>()).whenever(productRepository).fetchProductList(any(), any())

        createViewModel()

        val isLoadingMore = ArrayList<Boolean>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { isLoadingMore.add(it) }
        }

        viewModel.loadProducts(loadMore = true)
        assertThat(isLoadingMore).containsExactly(false, true, false)
    }
}
