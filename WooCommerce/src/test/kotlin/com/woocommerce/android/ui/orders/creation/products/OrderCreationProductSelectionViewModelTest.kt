package com.woocommerce.android.ui.orders.creation.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.ShowProductVariations
import com.woocommerce.android.ui.orders.creation.products.OrderCreationProductSelectionViewModel.AddProduct
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import com.woocommerce.android.ui.products.ProductTestUtils.generateProduct
import com.woocommerce.android.ui.products.ProductTestUtils.generateProductList
import com.woocommerce.android.ui.products.ProductTestUtils.generateProductListWithDrafts
import com.woocommerce.android.ui.products.ProductTestUtils.generateProductListWithNonPurchasable
import com.woocommerce.android.ui.products.ProductTestUtils.generateProductListWithVariations
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class OrderCreationProductSelectionViewModelTest : BaseUnitTest() {
    private lateinit var sut: OrderCreationProductSelectionViewModel
    private lateinit var productListRepository: ProductListRepository
    private lateinit var searchResult: List<Product>
    private lateinit var fullProductList: List<Product>

    @Before
    fun setUp() {
        productListRepository = mock {
            fullProductList = generateProductList()
            searchResult = listOf(generateProduct(333))
            on { getProductList() } doReturn fullProductList
            onBlocking { fetchProductList(loadMore = false) } doReturn fullProductList
            onBlocking { searchProductList(SEARCH_QUERY) } doReturn searchResult
            on { lastSearchQuery } doReturn SEARCH_QUERY
            on { canLoadMoreProducts } doReturn true
        }
    }

    @Test
    fun `when loading products, then get cached products before fetching from remote`() = testBlocking {
        startSut()
        inOrder(productListRepository).run {
            this.verify(productListRepository).getProductList()
            this.verify(productListRepository).fetchProductList()
        }
    }

    @Test
    fun `when loading products, then pass loadMore to fetch products from store`() = testBlocking {
        startSut()
        verify(productListRepository).fetchProductList(false)
        sut.onLoadMoreRequest()
        verify(productListRepository).fetchProductList(true)
    }

    @Test
    fun `when loading empty cached product list, then ignore result`() = testBlocking {
        var productListUpdateCalls = 0
        whenever(productListRepository.getProductList()).thenReturn(emptyList())
        whenever(productListRepository.fetchProductList(loadMore = true)).thenReturn(fullProductList)
        startSut()
        sut.productListData.observeForever { productListUpdateCalls++ }
        // to avoid a race condition between starting the sut and subscribing the observer
        productListUpdateCalls = 0
        sut.onLoadMoreRequest()
        assertThat(productListUpdateCalls).isEqualTo(1)
    }

    @Test
    fun `when non variable product is selected, then trigger AddProduct event`() = testBlocking {
        var lastReceivedEvent: Event? = null
        whenever(productListRepository.fetchProductList()).thenReturn(generateProductListWithVariations())
        startSut()
        sut.event.observeForever {
            lastReceivedEvent = it
        }
        sut.onProductSelected(NON_VARIABLE_PRODUCT_ID)
        assertThat(lastReceivedEvent).isNotNull
        assertThat(lastReceivedEvent).isInstanceOf(AddProduct::class.java)
    }

    @Test
    fun `when variable product is selected, then trigger ShowProductVariations event`() = testBlocking {
        var lastReceivedEvent: Event? = null
        whenever(productListRepository.fetchProductList()).thenReturn(generateProductListWithVariations())
        startSut()
        sut.event.observeForever {
            lastReceivedEvent = it
        }
        sut.onProductSelected(VARIABLE_PRODUCT_ID)
        assertThat(lastReceivedEvent).isNotNull
        assertThat(lastReceivedEvent).isInstanceOf(ShowProductVariations::class.java)
    }

    @Test
    fun `when loaded product list equals fetched products, then ignore the result`() = testBlocking {
        var productListUpdateCalls = 0
        startSut()
        sut.productListData.observeForever { productListUpdateCalls++ }
        assertThat(productListUpdateCalls).isEqualTo(1)
    }

    @Test
    fun `when loaded product list differs from fetched products, then apply the result again`() = testBlocking {
        var productListUpdateCalls = 0
        whenever(productListRepository.getProductList()).thenReturn(generateProductList())
        whenever(productListRepository.fetchProductList(loadMore = true)).thenReturn(generateProductList())
        startSut()
        sut.productListData.observeForever {
            productListUpdateCalls++
        }
        // to avoid a race condition between starting the sut and subscribing the observer
        productListUpdateCalls = 0
        sut.onLoadMoreRequest()
        assertThat(productListUpdateCalls).isEqualTo(2)
    }

    @Test
    fun `when searching for products, then apply the expected result`() = testBlocking {
        var actualProductList: List<Product>? = null
        startSut()
        sut.productListData.observeForever {
            actualProductList = it
        }
        sut.searchProductList(SEARCH_QUERY)
        verify(productListRepository).searchProductList(SEARCH_QUERY)
        assertThat(actualProductList).isEqualTo(searchResult)
    }

    @Test
    fun `when loading more products with search active, then apply the expected result`() = testBlocking {
        var actualProductList: List<Product>? = null
        val loadMoreSearchResponse = listOf(generateProduct(666))
        whenever(productListRepository.searchProductList(SEARCH_QUERY, true))
            .thenReturn(loadMoreSearchResponse)

        startSut()
        sut.productListData.observeForever {
            actualProductList = it
        }
        sut.onSearchOpened()

        sut.searchProductList(SEARCH_QUERY)
        verify(productListRepository).searchProductList(SEARCH_QUERY)
        assertThat(actualProductList).isEqualTo(searchResult)

        sut.onLoadMoreRequest()
        verify(productListRepository).searchProductList(SEARCH_QUERY, true)
        assertThat(actualProductList).isEqualTo(loadMoreSearchResponse + searchResult)
    }

    @Test
    fun `when onSearchOpened is called, then product list should be empty and search should be active`() =
        testBlocking {
            var actualProductList: List<Product>? = null
            var actualSearchState: Boolean? = null
            startSut()
            sut.productListData.observeForever {
                actualProductList = it
            }
            sut.viewStateData.observeForever { _, new ->
                actualSearchState = new.isSearchActive
            }

            sut.onSearchOpened()

            assertThat(actualSearchState).isTrue
            assertThat(actualProductList).isEmpty()
        }

    @Test
    fun `when onSearchClosed is called, then product full list should be loaded and search should be inactive`() =
        testBlocking {
            var actualProductList: List<Product>? = null
            var actualSearchState: Boolean? = null
            var actualQueryString: String? = null
            startSut()
            sut.productListData.observeForever {
                actualProductList = it
            }
            sut.viewStateData.observeForever { _, new ->
                actualSearchState = new.isSearchActive
                actualQueryString = new.query
            }

            sut.onSearchOpened()
            sut.onSearchClosed()

            assertThat(actualQueryString).isNull()
            assertThat(actualSearchState).isFalse
            assertThat(actualProductList).isEqualTo(fullProductList)
        }

    @Test
    fun `when onSearchQueryCleared is called, then product list and search query should be empty`() =
        testBlocking {
            var actualProductList: List<Product>? = null
            startSut()
            sut.productListData.observeForever {
                actualProductList = it
            }

            sut.onSearchQueryCleared()

            assertThat(sut.currentQuery).isEmpty()
            assertThat(actualProductList).isEmpty()
        }

    @Test
    fun `when loading products, then filter non-published products out`() = testBlocking {
        var actualProductList: List<Product>? = null
        val completeProductList = generateProductListWithDrafts()
        val filteredProductList = completeProductList
            .filter { it.status == PUBLISH }
        whenever(productListRepository.fetchProductList(loadMore = true)).thenReturn(completeProductList)
        startSut()
        sut.productListData.observeForever {
            actualProductList = it
        }
        sut.onLoadMoreRequest()
        assertThat(actualProductList).isEqualTo(filteredProductList)
    }

    @Test
    fun `when loading products, then filter non-purchasable products out`() = testBlocking {
        var actualProductList: List<Product>? = null
        val completeProductList = generateProductListWithNonPurchasable()
        val filteredProductList = completeProductList
            .filter { it.isPurchasable }
        whenever(productListRepository.fetchProductList(loadMore = true)).thenReturn(completeProductList)
        startSut()
        sut.productListData.observeForever {
            actualProductList = it
        }
        sut.onLoadMoreRequest()
        assertThat(actualProductList).isEqualTo(filteredProductList)
    }

    @Test
    fun `when loading more products, then ignore the request if canLoadMoreProducts is false`() = testBlocking {
        var actualProductList: List<Product>?
        whenever(productListRepository.canLoadMoreProducts).thenReturn(false)
        startSut()
        sut.productListData.observeForever {
            actualProductList = it
        }
        actualProductList = null
        sut.onLoadMoreRequest()
        assertThat(actualProductList).isNull()
        verify(productListRepository, times(0)).fetchProductList(loadMore = true)
    }

    private fun startSut() {
        sut = OrderCreationProductSelectionViewModel(
            SavedStateHandle(),
            productListRepository
        )
    }

    companion object {
        const val VARIABLE_PRODUCT_ID = 6L
        const val NON_VARIABLE_PRODUCT_ID = 1L
        const val SEARCH_QUERY = "search_query"
    }
}
