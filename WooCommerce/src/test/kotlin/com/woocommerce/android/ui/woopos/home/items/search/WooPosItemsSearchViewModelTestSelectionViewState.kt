package com.woocommerce.android.ui.woopos.home.items.search

import app.cash.turbine.test
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.PaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData.VariableProductData
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState.Product
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WooPosItemsSearchViewModelTestSelectionViewState {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val mockEmptyStateProvider: WooPosItemsSearchEmptyStateProvider = mock()
    private val mockPriceFormat: WooPosFormatPrice = mock()
    private val mockDataSource: WooPosSearchProductsMockedDataSource = mock()
    private val mockChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val mockParentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val mockNavigator: WooPosItemsNavigator = mock()

    private val defaultQuery = "test query"
    private val defaultProduct = ProductTestUtils.generateProduct(
        productId = 1,
        productName = "Test Product",
        amount = "10.0",
        productType = "simple"
    )

    @Before
    fun setup() = runTest {
        whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(emptyList())
        whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(emptyList())
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(emptyFlow())
        whenever(mockPriceFormat(BigDecimal("10.0"))).thenReturn("$10.0")
        whenever(mockPriceFormat(BigDecimal("20.0"))).thenReturn("$20.0")
    }

    @Test
    fun `given less than max popular items and recent searches, when view model created, then all items are shown`() =
        runTest {
            // GIVEN
            val popularItems = listOf(
                Product.Simple(id = 1, name = "Popular Item 1", price = "$10.0", imageUrl = null),
                Product.Simple(id = 2, name = "Popular Item 2", price = "$15.0", imageUrl = null)
            )
            val recentSearches = listOf("Recent Search 1")

            whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(popularItems)
            whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(recentSearches)

            // WHEN
            val viewModel = createViewModel()

            // THEN
            viewModel.viewState.test {
                val value = awaitItem()
                assertThat(value).isInstanceOf(WooPosItemsSearchViewState.EmptySearchQuery::class.java)

                val emptySearchQuery = value as WooPosItemsSearchViewState.EmptySearchQuery
                assertThat(emptySearchQuery.popularItems).hasSize(2)
                assertThat(emptySearchQuery.recentSearches).hasSize(1)
            }
        }

    @Test
    fun `given empty popular items and recent searches, when view model created, then empty lists are shown`() =
        runTest {
            // WHEN
            val viewModel = createViewModel()

            // THEN
            viewModel.viewState.test {
                val value = awaitItem()
                assertThat(value).isInstanceOf(WooPosItemsSearchViewState.EmptySearchQuery::class.java)

                val emptySearchQuery = value as WooPosItemsSearchViewState.EmptySearchQuery
                assertThat(emptySearchQuery.popularItems).isEmpty()
                assertThat(emptySearchQuery.recentSearches).isEmpty()
            }
        }

    @Test
    fun `given view model initialization, when view model created, then initial state is EmptySearchQuery`() = runTest {
        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosItemsSearchViewState.EmptySearchQuery::class.java)
        }
    }

    @Test
    fun `given more than max items count, when view model created, then only max items are shown`() = runTest {
        // GIVEN
        val popularItems = listOf(
            Product.Simple(id = 1, name = "Popular Item 1", price = "$10.0", imageUrl = null),
            Product.Simple(id = 2, name = "Popular Item 2", price = "$15.0", imageUrl = null),
            Product.Simple(id = 3, name = "Popular Item 3", price = "$20.0", imageUrl = null),
            Product.Simple(id = 4, name = "Popular Item 4", price = "$25.0", imageUrl = null)
        )
        val recentSearches = listOf(
            "Recent Search 1",
            "Recent Search 2",
            "Recent Search 3",
            "Recent Search 4",
        )
        whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(popularItems)
        whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(recentSearches)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosItemsSearchViewState.EmptySearchQuery::class.java)
            val emptySearchQuery = value as WooPosItemsSearchViewState.EmptySearchQuery
            assertThat(emptySearchQuery.popularItems).hasSize(3)
            assertThat(emptySearchQuery.popularItems.map { it.id }).containsExactly(1, 2, 3)
            assertThat(emptySearchQuery.recentSearches).hasSize(3)
            assertThat(emptySearchQuery.recentSearches).containsExactly(
                "Recent Search 1",
                "Recent Search 2",
                "Recent Search 3"
            )
        }
    }

    @Test
    fun `given search query and search results, when view model created, then view state updated accordingly`() =
        runTest {
            // GIVEN
            mockSuccessfulSearch(defaultQuery, listOf(defaultProduct))

            // WHEN
            val viewModel = createViewModel()

            // THEN
            viewModel.viewState.test {
                val initialState = awaitItem()
                assertThat(initialState).isInstanceOf(WooPosItemsSearchViewState.EmptySearchQuery::class.java)

                val contentState = awaitItem()
                assertThat(contentState).isInstanceOf(WooPosItemsSearchViewState.Content::class.java)

                val content = contentState as WooPosItemsSearchViewState.Content
                assertThat(content.items).hasSize(1)
                assertThat((content.items[0] as Product.Simple).name).isEqualTo(
                    "Test Product"
                )
            }

            verify(mockChildToParentEventSender).sendToParent(ChildToParentEvent.SearchEvent.Started)
            verify(mockChildToParentEventSender).sendToParent(ChildToParentEvent.SearchEvent.Finished)
        }

    @Test
    fun `given empty search query, when search query updated, then view state is empty search query`() = runTest {
        // GIVEN
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(""))
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosItemsSearchViewState.EmptySearchQuery::class.java)
        }
    }

    @Test
    fun `given valid search query, when search query changed, then view state updated accordingly`() = runTest {
        // GIVEN
        mockSuccessfulSearch(defaultQuery, listOf(defaultProduct))

        // WHEN
        val viewModel = createViewModel()
        advanceTimeBy(600)

        // THEN
        viewModel.viewState.test {
            skipItems(0)

            val content = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(content.items).hasSize(1)
            assertThat((content.items[0] as Product.Simple).name).isEqualTo("Test Product")
        }

        verify(mockChildToParentEventSender).sendToParent(ChildToParentEvent.SearchEvent.Started)
        verify(mockChildToParentEventSender).sendToParent(ChildToParentEvent.SearchEvent.Finished)
    }

    @Test
    fun `given empty search query, when search query changed to empty, then view state is empty search query`() =
        runTest {
            // GIVEN
            val emptyQuery = ""

            whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(emptyList())
            whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(emptyList())
            whenever(mockParentToChildrenEventReceiver.events).thenReturn(
                flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(emptyQuery))
            )

            // WHEN
            val viewModel = createViewModel()

            // THEN
            viewModel.viewState.test {
                val value = awaitItem()
                assertThat(value).isInstanceOf(WooPosItemsSearchViewState.EmptySearchQuery::class.java)
            }
        }

    @Test
    fun `given search with no results, when search performed, then view state is empty`() = runTest {
        // GIVEN
        mockSuccessfulSearch(defaultQuery, emptyList())

        // WHEN
        val viewModel = createViewModel()
        advanceTimeBy(600)

        // THEN
        viewModel.viewState.test {
            assertThat(awaitItem()).isEqualTo(WooPosItemsSearchViewState.Empty)
        }
    }

    @Test
    fun `given search fails, when search performed, then view state is error`() = runTest {
        // GIVEN
        mockFailedSearch(defaultQuery, Exception("Search failed"))

        // WHEN
        val viewModel = createViewModel()
        advanceTimeBy(600)

        // THEN
        viewModel.viewState.test {
            assertThat(awaitItem()).isEqualTo(WooPosItemsSearchViewState.Error(defaultQuery))
        }

        verify(mockChildToParentEventSender).sendToParent(ChildToParentEvent.SearchEvent.Started)
        verify(mockChildToParentEventSender).sendToParent(ChildToParentEvent.SearchEvent.Finished)
    }

    @Test
    fun `given variable product in search results, when search performed, then mapped correctly to view state`() =
        runTest {
            // GIVEN
            val variableProduct =
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Variable Product",
                    amount = "10.0",
                    productType = "variable",
                    isVariable = true,
                    variationIds = "[101,102,103]"
                )

            mockSuccessfulSearch(defaultQuery, listOf(variableProduct))

            // WHEN
            val viewModel = createViewModel()
            advanceTimeBy(600)

            // THEN
            viewModel.viewState.test {
                skipItems(0)

                val value = awaitItem() as WooPosItemsSearchViewState.Content
                assertThat(value.items[0]).isInstanceOf(Product.Variable::class.java)

                val variableProduct = value.items[0] as Product.Variable
                assertThat(variableProduct.name).isEqualTo("Variable Product")
                assertThat(variableProduct.numOfVariations).isEqualTo(3)
                assertThat(variableProduct.variationIds).containsExactly(101L, 102L, 103L)
            }
        }

    @Test
    fun `given content state and more pages available, when end of list reached, then load more data`() = runTest {
        // GIVEN
        val additionalProduct = ProductTestUtils.generateProduct(
            productId = 2,
            productName = "Test Product 2",
            amount = "20.0",
            productType = "simple"
        )

        mockSuccessfulSearch(defaultQuery, listOf(defaultProduct))
        mockSuccessfulPagination(defaultQuery, listOf(additionalProduct))

        // WHEN
        val viewModel = createViewModel()
        advanceTimeBy(600)

        // THEN
        viewModel.viewState.test {
            val initialState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(initialState.items).hasSize(1)

            viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnNextPageRequested)

            val loadingState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(loadingState.paginationState).isEqualTo(PaginationState.Loading)

            val finalState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(finalState.items).hasSize(1)
            assertThat(finalState.paginationState).isEqualTo(PaginationState.None)
        }
    }

    @Test
    fun `given content state when load more fails, then pagination state is error`() = runTest {
        // GIVEN
        mockSuccessfulSearch(defaultQuery, listOf(defaultProduct))
        mockFailedPagination(defaultQuery, Exception("Failed to load more"))

        // WHEN
        val viewModel = createViewModel()
        advanceTimeBy(600)

        // THEN
        viewModel.viewState.test {
            awaitItem() as WooPosItemsSearchViewState.Content

            viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnNextPageRequested)

            val loadingState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(loadingState.paginationState).isEqualTo(PaginationState.Loading)

            val errorState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(errorState.paginationState).isEqualTo(PaginationState.Error)
        }
    }

    @Test
    fun `given cached products, when search performed, then cached results shown while loading`() = runTest {
        // GIVEN
        val cachedProduct = ProductTestUtils.generateProduct(
            productId = 1,
            productName = "Cached Product",
            amount = "10.0",
            productType = "simple"
        )

        val remoteProduct = ProductTestUtils.generateProduct(
            productId = 2,
            productName = "Remote Product",
            amount = "20.0",
            productType = "simple"
        )

        mockCachedThenRemoteSearch(defaultQuery, cachedProduct, remoteProduct)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            awaitItem() as WooPosItemsSearchViewState.EmptySearchQuery

            val cachedState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(cachedState.items).hasSize(1)
            assertThat((cachedState.items[0] as Product.Simple).name).isEqualTo("Cached Product")

            val remoteState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(remoteState.items).hasSize(1)
            assertThat((remoteState.items[0] as Product.Simple).name).isEqualTo("Remote Product")
        }

        verify(mockChildToParentEventSender).sendToParent(ChildToParentEvent.SearchEvent.Started)
        verify(mockChildToParentEventSender).sendToParent(ChildToParentEvent.SearchEvent.Finished)
    }

    @Test
    fun `given multiple search queries in quick succession, when typing, then only last query is executed`() = runTest {
        // GIVEN
        val query1 = "first"
        val query2 = "second"

        whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(emptyList())
        whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(emptyList())

        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Test Product",
                amount = "10.0",
                productType = "simple"
            )
        )

        whenever(mockDataSource.searchProducts(query1)).thenReturn(
            flowOf(
                WooPosSearchProductsMockedDataSource.ProductsResult.Remote(
                    Result.success(emptyList())
                )
            )
        )

        whenever(mockDataSource.searchProducts(query2)).thenReturn(
            flowOf(
                WooPosSearchProductsMockedDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )

        whenever(mockPriceFormat(BigDecimal("10.0"))).thenReturn("$10.0")

        val searchEvents = flow {
            emit(ParentToChildrenEvent.SearchEvent.ChangedQuery(query1))
            delay(100)
            emit(ParentToChildrenEvent.SearchEvent.ChangedQuery(query2))
        }

        whenever(mockParentToChildrenEventReceiver.events).thenReturn(searchEvents)

        // WHEN
        val viewModel = createViewModel()
        advanceTimeBy(600)

        // THEN
        viewModel.viewState.test {
            skipItems(1)

            val contentState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(contentState.searchQuery).isEqualTo(query2)
            assertThat(contentState.items).hasSize(1)
        }
    }

    @Test
    fun `given empty cached results, when search performed, then show loading state`() = runTest {
        // GIVEN
        val query = "test query"
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Test Product",
                amount = "10.0",
                productType = "simple"
            )
        )

        whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(emptyList())
        whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(emptyList())

        whenever(mockDataSource.searchProducts(query)).thenReturn(
            flow {
                emit(WooPosSearchProductsMockedDataSource.ProductsResult.Cached(emptyList()))
                delay(100)
                emit(WooPosSearchProductsMockedDataSource.ProductsResult.Remote(Result.success(products)))
            }.flowOn(UnconfinedTestDispatcher(testScheduler))
        )

        whenever(mockPriceFormat(BigDecimal("10.0"))).thenReturn("$10.0")
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(query))
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            awaitItem() as WooPosItemsSearchViewState.EmptySearchQuery

            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(WooPosItemsSearchViewState.Loading::class.java)

            val contentState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(contentState.items).hasSize(1)
        }
    }

    @Test
    fun `given content state and no more pages available, when end of list reached, then don't load more data`() =
        runTest {
            // GIVEN
            val query = "test query"
            val products = listOf(
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Test Product",
                    amount = "10.0",
                    productType = "simple"
                )
            )

            whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(emptyList())
            whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(emptyList())

            whenever(mockDataSource.searchProducts(query)).thenReturn(
                flowOf(
                    WooPosSearchProductsMockedDataSource.ProductsResult.Remote(
                        Result.success(products)
                    )
                )
            )

            whenever(mockDataSource.hasMorePages).thenReturn(false)

            whenever(mockPriceFormat(BigDecimal("10.0"))).thenReturn("$10.0")
            whenever(mockParentToChildrenEventReceiver.events).thenReturn(
                flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(query))
            )

            // WHEN
            val viewModel = createViewModel()
            advanceTimeBy(600)

            // THEN
            viewModel.viewState.test {
                awaitItem() as WooPosItemsSearchViewState.Content

                viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnNextPageRequested)

                expectNoEvents()
            }
        }

    @Test
    fun `given error state with search query, when retry button clicked, then reload with same query`() = runTest {
        // GIVEN
        val query = "test query"
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Test Product",
                amount = "10.0",
                productType = "simple"
            )
        )

        whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(emptyList())
        whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(emptyList())

        var searchAttempt = 0
        whenever(mockDataSource.searchProducts(query)).thenAnswer {
            if (searchAttempt++ == 0) {
                flowOf(
                    WooPosSearchProductsMockedDataSource.ProductsResult.Remote(
                        Result.failure(Exception("Search failed"))
                    )
                )
            } else {
                flowOf(
                    WooPosSearchProductsMockedDataSource.ProductsResult.Remote(
                        Result.success(products)
                    )
                )
            }
        }

        whenever(mockPriceFormat(BigDecimal("10.0"))).thenReturn("$10.0")
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(query))
        )

        // WHEN
        val viewModel = createViewModel()
        advanceTimeBy(600)

        // THEN
        viewModel.viewState.test {
            val errorState = awaitItem()
            assertThat(errorState).isInstanceOf(WooPosItemsSearchViewState.Error::class.java)
            assertThat((errorState as WooPosItemsSearchViewState.Error).searchQuery).isEqualTo(query)

            viewModel.onUIEvent(WooPosItemsSearchUiEvent.LoadingErrorRetryButtonClicked)

            val contentState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(contentState.searchQuery).isEqualTo(query)
            assertThat(contentState.items).hasSize(1)
        }
    }

    @Test
    fun `given simple product, when item clicked, then send product click event to parent`() = runTest {
        // GIVEN
        val simpleProduct = Product.Simple(id = 1, name = "Test Product", price = "$10.0", imageUrl = null)

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsSearchUiEvent.ItemClicked(simpleProduct))

        // THEN
        verify(mockChildToParentEventSender).sendToParent(
            ChildToParentEvent.ItemClickedInProductSelector(
                ItemClickedData.Product.Simple(id = 1)
            )
        )
    }

    @Test
    fun `given variable product, when item clicked, then navigate to variations screen`() = runTest {
        // GIVEN
        val variableProduct = Product.Variable(
            id = 1,
            name = "Variable Product",
            price = "$10.0",
            imageUrl = null,
            numOfVariations = 3,
            variationIds = listOf(101L, 102L, 103L)
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsSearchUiEvent.ItemClicked(variableProduct))
        advanceUntilIdle()

        // THEN
        verify(mockNavigator).sendNavigationEvent(
            NavigateToVariationsScreen(
                VariableProductData(
                    id = 1,
                    name = "Variable Product",
                    numOfVariations = 3
                )
            )
        )
    }

    @Test
    fun `given variation, when item clicked, then throw error`() = runTest {
        // GIVEN
        val variation = WooPosItemSelectionViewState.Variation(
            id = 1,
            name = "Test Variation",
            price = "$10.0",
            productId = 1L,
            imageUrl = null,
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThrows(IllegalStateException::class.java) {
            viewModel.onUIEvent(WooPosItemsSearchUiEvent.ItemClicked(variation))
        }
    }

    private fun mockSuccessfulSearch(query: String, products: List<com.woocommerce.android.model.Product>) {
        whenever(mockDataSource.searchProducts(query)).thenReturn(
            flowOf(
                WooPosSearchProductsMockedDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(query))
        )
    }

    private fun mockFailedSearch(query: String, error: Exception) {
        whenever(mockDataSource.searchProducts(query)).thenReturn(
            flowOf(
                WooPosSearchProductsMockedDataSource.ProductsResult.Remote(
                    Result.failure(error)
                )
            )
        )
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(query))
        )
    }

    private fun mockSuccessfulPagination(
        query: String,
        products: List<com.woocommerce.android.model.Product>
    ) = runTest {
        whenever(mockDataSource.hasMorePages).thenReturn(true)
        whenever(mockDataSource.loadMore(query)).thenReturn(Result.success(products))
    }

    private fun mockFailedPagination(query: String, error: Exception) = runTest {
        whenever(mockDataSource.hasMorePages).thenReturn(true)
        whenever(mockDataSource.loadMore(query)).thenReturn(Result.failure(error))
    }

    private fun mockCachedThenRemoteSearch(
        query: String,
        cachedProduct: com.woocommerce.android.model.Product,
        remoteProduct: com.woocommerce.android.model.Product
    ) {
        whenever(mockDataSource.searchProducts(query)).thenReturn(
            flow {
                emit(WooPosSearchProductsMockedDataSource.ProductsResult.Cached(listOf(cachedProduct)))
                delay(1)
                emit(WooPosSearchProductsMockedDataSource.ProductsResult.Remote(Result.success(listOf(remoteProduct))))
            }.flowOn(coroutinesTestRule.testDispatcher)
        )
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(query))
        )
    }

    private fun createViewModel() = WooPosItemsSearchViewModel(
        emptyStateProvider = mockEmptyStateProvider,
        priceFormat = mockPriceFormat,
        dataSource = mockDataSource,
        childToParentEventSender = mockChildToParentEventSender,
        parentToChildrenEventReceiver = mockParentToChildrenEventReceiver,
        navigator = mockNavigator,
    )
}
