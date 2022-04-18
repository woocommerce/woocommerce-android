package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ShowProductFilterScreen
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ShowProductSortingBottomSheet
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.internal.verification.AtLeast
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting

@ExperimentalCoroutinesApi
class ProductListViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val productRepository: ProductListRepository = mock()
    private val mediaFileUploadHandler: MediaFileUploadHandler = mock()

    private val savedStateHandle: SavedStateHandle = SavedStateHandle()

    private val productList = ProductTestUtils.generateProductList()
    private lateinit var viewModel: ProductListViewModel

    @Before
    fun setup() {
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(ProductSorting.DATE_ASC).whenever(productRepository).productSortingChoice
    }

    private fun createViewModel() {
        viewModel = spy(
            ProductListViewModel(
                savedStateHandle,
                productRepository,
                networkStatus,
                mediaFileUploadHandler
            )
        )
    }

    @Test
    fun `Displays the product list view correctly`() = testBlocking {
        doReturn(productList).whenever(productRepository).fetchProductList(productFilterOptions = emptyMap())

        createViewModel()

        val products = ArrayList<Product>()
        viewModel.productList.observeForever {
            it?.let { products.addAll(it) }
        }

        assertThat(products).isEqualTo(productList)
    }

    @Test
    fun `Do not fetch product list from api when not connected`() = testBlocking {
        doReturn(false).whenever(networkStatus).isConnected()

        createViewModel()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        verify(productRepository, times(1)).getProductList(productFilterOptions = emptyMap())
        verify(productRepository, times(0)).fetchProductList(productFilterOptions = emptyMap())

        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
    }

    @Test
    fun `Shows and hides product list skeleton correctly`() = testBlocking {
        doReturn(emptyList<Product>()).whenever(productRepository).getProductList()
        doReturn(emptyList<Product>()).whenever(productRepository).fetchProductList()

        createViewModel()

        val isSkeletonShown = ArrayList<Boolean>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { isSkeletonShown.add(it) }
        }

        viewModel.loadProducts()

        assertThat(isSkeletonShown).containsExactly(false, true, false)
    }

    @Test
    fun `Shows and hides product list load more progress correctly`() =
        testBlocking {
            doReturn(true).whenever(productRepository).canLoadMoreProducts
            doReturn(emptyList<Product>()).whenever(productRepository).fetchProductList()

            createViewModel()

            val isLoadingMore = ArrayList<Boolean>()
            viewModel.viewStateLiveData.observeForever { old, new ->
                new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { isLoadingMore.add(it) }
            }

            viewModel.loadProducts(loadMore = true)
            assertThat(isLoadingMore).containsExactly(false, true, false)
        }

    @Test
    fun `Shows and hides add product button correctly when loading list of products`() =
        testBlocking {
            // when
            doReturn(productList).whenever(productRepository).fetchProductList()

            createViewModel()

            val isAddProductButtonVisible = ArrayList<Boolean>()
            viewModel.viewStateLiveData.observeForever { old, new ->
                new.isAddProductButtonVisible?.takeIfNotEqualTo(old?.isAddProductButtonVisible) {
                    isAddProductButtonVisible.add(it)
                }
            }

            viewModel.loadProducts()

            // then
            assertThat(isAddProductButtonVisible).containsExactly(true, false, true)
        }

    @Test
    fun `Hides add product button when list of products is empty`() =
        testBlocking {
            // when
            doReturn(emptyList<Product>()).whenever(productRepository).fetchProductList()

            createViewModel()

            val isAddProductButtonVisible = ArrayList<Boolean>()
            viewModel.viewStateLiveData.observeForever { old, new ->
                new.isAddProductButtonVisible?.takeIfNotEqualTo(old?.isAddProductButtonVisible) {
                    isAddProductButtonVisible.add(it)
                }
            }

            viewModel.loadProducts()

            // then
            assertThat(isAddProductButtonVisible).containsExactly(false)
        }

    @Test
    fun `Hides add product button when searching`() =
        testBlocking {
            // when
            doReturn(emptyList<Product>()).whenever(productRepository).fetchProductList()

            createViewModel()

            val isAddProductButtonVisible = ArrayList<Boolean>()
            viewModel.viewStateLiveData.observeForever { old, new ->
                new.isAddProductButtonVisible?.takeIfNotEqualTo(old?.isAddProductButtonVisible) {
                    isAddProductButtonVisible.add(it)
                }
            }

            viewModel.loadProducts()
            viewModel.onSearchOpened()

            // then
            assertThat(isAddProductButtonVisible).containsExactly(false)
        }

    @Test
    /* We show the Add Product FAB after searching is completed. */
    fun `Shows add product button after opening and closing search`() =
        testBlocking {
            // when
            doReturn(emptyList<Product>()).whenever(productRepository).fetchProductList()

            createViewModel()

            val isAddProductButtonVisible = ArrayList<Boolean>()
            viewModel.viewStateLiveData.observeForever { old, new ->
                new.isAddProductButtonVisible?.takeIfNotEqualTo(old?.isAddProductButtonVisible) {
                    isAddProductButtonVisible.add(it)
                }
            }

            viewModel.loadProducts()
            viewModel.onSearchOpened()
            viewModel.onSearchClosed()

            // then
            assertThat(isAddProductButtonVisible).containsExactly(false, true, false)
        }

    @Test
    /* We hide the filters when searching. */
    fun `Hides filters buttons when searching`() =
        testBlocking {
            // when
            doReturn(emptyList<Product>()).whenever(productRepository).fetchProductList()

            createViewModel()

            val displaySortAndFilterCard = ArrayList<Boolean>()
            viewModel.viewStateLiveData.observeForever { old, new ->
                new.displaySortAndFilterCard?.takeIfNotEqualTo(old?.displaySortAndFilterCard) {
                    displaySortAndFilterCard.add(it)
                }
            }

            viewModel.loadProducts()
            viewModel.onSearchOpened()

            // then
            assertThat(displaySortAndFilterCard).containsExactly(false)
        }

    @Test
    fun `Shows filters buttons after opening and closing search`() =
        testBlocking {
            // when
            doReturn(emptyList<Product>()).whenever(productRepository).fetchProductList()

            createViewModel()

            val displaySortAndFilterCard = ArrayList<Boolean>()
            viewModel.viewStateLiveData.observeForever { old, new ->
                new.displaySortAndFilterCard?.takeIfNotEqualTo(old?.displaySortAndFilterCard) {
                    displaySortAndFilterCard.add(it)
                }
            }

            viewModel.loadProducts()
            viewModel.onSearchOpened()
            viewModel.onSearchClosed()

            // then
            assertThat(displaySortAndFilterCard).containsExactly(false, true, false)
        }

    @Test
    fun `Shows offline message when trashing a product without a connection`() {
        doReturn(false).whenever(networkStatus).isConnected()

        createViewModel()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.trashProduct(any())
        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
    }

    @Test
    fun `Shows error message when trashing a product fails`() {
        runBlocking {
            doReturn(false).whenever(productRepository).trashProduct(any())
        }

        createViewModel()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.trashProduct(any())
        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.product_trash_error))
    }

    @Test
    fun `Test Filters button tap`() {
        createViewModel()

        val events = mutableListOf<Event>()
        viewModel.event.observeForever {
            events.add(it)
        }

        viewModel.onFiltersButtonTapped()

        assertThat(events.count { it is ShowProductFilterScreen }).isEqualTo(1)
    }

    @Test
    fun `Test Filters button tap when filters already enabled`() {
        createViewModel()

        val stockStatus = "instock"
        val status = "simple"
        val type = "draft"
        val category = "hoodie"
        val categoryName = "Hoodie"
        viewModel.onFiltersChanged(stockStatus, status, type, category, categoryName)

        val events = mutableListOf<Event>()
        viewModel.event.observeForever {
            events.add(it)
        }

        viewModel.onFiltersButtonTapped()

        val event = events.first() as ShowProductFilterScreen
        assertThat(event.productStatusFilter).isEqualTo(status)
        assertThat(event.productTypeFilter).isEqualTo(type)
        assertThat(event.stockStatusFilter).isEqualTo(stockStatus)
        assertThat(event.productCategoryFilter).isEqualTo(category)
        assertThat(event.selectedCategoryName).isEqualTo(categoryName)
    }

    @Test
    fun `Test Sorting button tap`() {
        createViewModel()

        val events = mutableListOf<Event>()
        viewModel.event.observeForever {
            events.add(it)
        }

        viewModel.onSortButtonTapped()

        assertThat(events.count { it is ShowProductSortingBottomSheet }).isEqualTo(1)
    }

    @Test
    fun `when image upload finishes for a product, then reload products`() {
        whenever(mediaFileUploadHandler.observeProductImageChanges()).thenReturn(flowOf(1L))
        createViewModel()

        verify(productRepository, AtLeast(2)).getProductList()
    }
}
