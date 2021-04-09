package com.woocommerce.android.ui.products

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductSelectionListViewModel.ProductSelectionListViewState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.test
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse

class ProductSelectionListViewModelTest : BaseUnitTest() {
    companion object {
        private const val PRODUCT_REMOTE_ID = 10L
    }

    private val productList = ProductTestUtils.generateProductList()
    private val excludedProductIds = listOf(PRODUCT_REMOTE_ID)

    private val networkStatus: NetworkStatus = mock()
    private val productRepository: ProductListRepository = mock()
    private val savedState: SavedStateWithArgs = spy(
        SavedStateWithArgs(
            SavedStateHandle(),
            null,
            ProductSelectionListFragmentArgs(
                remoteProductId = PRODUCT_REMOTE_ID,
                groupedProductListType = GroupedProductListType.GROUPED,
                excludedProductIds = excludedProductIds.toLongArray()
            )
        )
    )

    private val coroutineDispatchers = CoroutineDispatchers(
        Dispatchers.Unconfined, Dispatchers.Unconfined, Dispatchers.Unconfined)

    private lateinit var viewModel: ProductSelectionListViewModel

    private fun createViewModel() {
        viewModel = spy(
            ProductSelectionListViewModel(
                savedState,
                coroutineDispatchers,
                networkStatus,
                productRepository
            )
        )
    }

    @Before
    fun setup() {
        doReturn(MutableLiveData(ProductSelectionListViewState()))
            .whenever(savedState).getLiveData<ProductSelectionListViewState>(any(), any())
        doReturn(true).whenever(networkStatus).isConnected()
    }

    @Test
    fun `Displays the product list view correctly`() = test {
        doReturn(productList).whenever(productRepository).fetchProductList(
            excludedProductIds = excludedProductIds
        )

        createViewModel()

        val products = ArrayList<Product>()
        viewModel.productList.observeForever {
            it?.let { products.addAll(it) }
        }

        assertThat(products).isEqualTo(productList)

        val remoteProductIds = products.map { it.remoteId }
        assertFalse(remoteProductIds.contains(PRODUCT_REMOTE_ID))
    }

    @Test
    fun `Do not fetch product list from api when not connected`() = test {
        doReturn(false).whenever(networkStatus).isConnected()

        createViewModel()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        verify(productRepository, times(1)).getProductList(excludedProductIds = excludedProductIds)
        verify(productRepository, times(0)).fetchProductList(excludedProductIds = excludedProductIds)

        assertThat(snackbar).isEqualTo(ShowSnackbar(string.offline_error))
    }

    @Test
    fun `Shows and hides product list skeleton correctly`() = test {
        doReturn(emptyList<Product>()).whenever(productRepository).getProductList(
            excludedProductIds = excludedProductIds
        )
        doReturn(emptyList<Product>()).whenever(productRepository).fetchProductList(
            excludedProductIds = excludedProductIds
        )

        createViewModel()

        val isSkeletonShown = ArrayList<Boolean>()
        viewModel.productSelectionListViewStateLiveData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { isSkeletonShown.add(it) }
        }

        assertThat(isSkeletonShown).containsExactly(false)
    }

    @Test
    fun `Shows and hides product list load more progress correctly`() = test {
        doReturn(true).whenever(productRepository).canLoadMoreProducts
        doReturn(emptyList<Product>()).whenever(productRepository).fetchProductList(
            excludedProductIds = excludedProductIds
        )

        createViewModel()

        val isLoadingMore = ArrayList<Boolean>()
        viewModel.productSelectionListViewStateLiveData.observeForever { old, new ->
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { isLoadingMore.add(it) }
        }

        viewModel.onLoadMoreRequested()
        assertThat(isLoadingMore).containsExactly(false, true, false)
    }

    @Test
    fun `ExitWithResult event dispatched correctly when done menu button clicked`() {
        createViewModel()

        var event: Event? = null
        viewModel.event.observeForever { new -> event = new }

        val listAdded: List<Long> = listOf(2, 3, 4)
        viewModel.onDoneButtonClicked(listAdded)

        assertThat(event).isInstanceOf(ExitWithResult::class.java)
        assertThat(((event as ExitWithResult<*>).data as List<*>).size).isEqualTo(listAdded.size)
    }
}
