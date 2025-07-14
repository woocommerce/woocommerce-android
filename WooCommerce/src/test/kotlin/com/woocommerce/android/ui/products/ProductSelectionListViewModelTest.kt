package com.woocommerce.android.ui.products

import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.grouped.GroupedProductListType
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse

@ExperimentalCoroutinesApi
class ProductSelectionListViewModelTest : BaseUnitTest() {
    companion object {
        private const val PRODUCT_REMOTE_ID = 1L
        private const val EXCLUDED_PRODUCT_REMOTE_ID = 3L
    }

    private val productList = ProductTestUtils.generateProductList()
    private val excludedProductIdsNavArgs = listOf(EXCLUDED_PRODUCT_REMOTE_ID)
    private val excludedProductIds = excludedProductIdsNavArgs.toMutableList().apply { add(PRODUCT_REMOTE_ID) }

    private val networkStatus: NetworkStatus = mock {
        on { isConnected() } doReturn true
    }
    private val productRepository: ProductListRepository = mock()
    private val savedState = ProductSelectionListFragmentArgs(
        remoteProductId = PRODUCT_REMOTE_ID,
        groupedProductListType = GroupedProductListType.GROUPED,
        excludedProductIds = excludedProductIdsNavArgs.toLongArray()
    ).toSavedStateHandle()

    private lateinit var viewModel: ProductSelectionListViewModel

    private fun createViewModel() {
        viewModel = ProductSelectionListViewModel(
            savedState,
            networkStatus,
            productRepository
        )
    }

    @Test
    fun `Displays the product list view correctly`() = testBlocking {
        val expectedProductList = productList.toMutableList().apply {
            excludedProductIds.forEach { excludedIds ->
                this.removeIf { it.remoteId == excludedIds }
            }
        }
        doReturn(Result.success(expectedProductList)).whenever(productRepository).fetchProductList(
            excludedProductIds = excludedProductIds
        )

        createViewModel()

        val products = ArrayList<Product>()
        viewModel.productList.observeForever {
            it?.let { products.addAll(it) }
        }

        assertThat(products).isEqualTo(expectedProductList)

        val remoteProductIds = products.map { it.remoteId }
        assertFalse(remoteProductIds.contains(EXCLUDED_PRODUCT_REMOTE_ID))
    }

    @Test
    fun `Do not fetch product list from api when not connected`() = testBlocking {
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
    fun `Shows and hides product list skeleton correctly`() = testBlocking {
        doReturn(emptyList<Product>()).whenever(productRepository).getProductList(
            excludedProductIds = excludedProductIds
        )
        doReturn(Result.success(emptyList<Product>())).whenever(productRepository).fetchProductList(
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
    fun `Shows and hides product list load more progress correctly`() =
        testBlocking {
            doReturn(true).whenever(productRepository).canLoadMoreProducts
            doReturn(Result.success(emptyList<Product>())).whenever(productRepository).fetchProductList(
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

    @Test
    fun `Should exclude the current product from product selection list`() =
        testBlocking {
            val expectedProductList = productList.toMutableList().apply {
                excludedProductIds.forEach { excludedIds ->
                    this.removeIf { it.remoteId == excludedIds }
                }
            }

            doReturn(Result.success(expectedProductList)).whenever(productRepository).fetchProductList(
                excludedProductIds = excludedProductIds
            )

            createViewModel()

            val products = ArrayList<Product>()
            viewModel.productList.observeForever {
                it?.let { products.addAll(it) }
            }

            assertThat(products).isEqualTo(expectedProductList)

            val remoteProductIds = products.map { it.remoteId }
            assertFalse(remoteProductIds.contains(PRODUCT_REMOTE_ID))
        }
}
