package com.woocommerce.android.ui.woopos.home.items.search

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState.Product
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.home.items.products.SearchProductsResult
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogProductSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.ui.woopos.util.generateWooPosProduct
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WooPosItemsSearchViewModelTest {

    @Rule @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val mockEmptyStateProvider: WooPosItemsSearchEmptyStateRepository = mock()
    private val mockPriceFormat: WooPosFormatPrice = mock()
    private val mockDataSource: WooPosProductsDataSource = mock()
    private val mockChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val mockParentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val mockSearchHelper: com.woocommerce.android.ui.woopos.home.items.WooPosItemsSearchHelper = mock()
    private val mockAnalyticsTracker: WooPosItemsSearchAnalyticsTracker = mock()
    private val mockResourceProvider: ResourceProvider = mock()
    private val mockWooPosAnalyticsTracker: WooPosAnalyticsTracker = mock()

    private val defaultQuery = "test query"
    private val defaultProduct = generateWooPosProduct(
        productId = 1,
        productName = "Test Product",
        amount = "10.0",
        productType = WooPosProductModel.WooPosProductType.Simple
    )

    @Before
    fun setup() = runTest {
        whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(emptyList())
        whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(emptyList())
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(emptyFlow())
        whenever(mockPriceFormat(BigDecimal("10.0"))).thenReturn("$10.0")
        whenever(mockPriceFormat(BigDecimal("20.0"))).thenReturn("$20.0")
        whenever(mockDataSource.getCurrentSyncStrategy())
            .thenReturn(WooPosProductsDataSource.SyncStrategy.LOCAL_CATALOG_FILE)
        whenever(mockResourceProvider.getString(R.string.something_went_wrong_try_again))
            .thenReturn("Something went wrong")
        whenever(mockResourceProvider.getString(R.string.woo_pos_ptr_offline_error))
            .thenReturn("No internet connection")
    }

    @Test
    fun `given less than max popular items and recent searches, when view model created, then all items are shown`() =
        runTest {
            // GIVEN
            val popularItems = listOf(defaultProduct)
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
                assertThat(emptySearchQuery.popularItems).hasSize(1)
                assertThat(emptySearchQuery.recentSearches).hasSize(1)
            }
        }

    @Test
    fun `given empty popular items and recent searches, when view model created, then empty lists are shown`() =
        runTest {
            // GIVEN
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
        // GIVEN
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
        val popularItems = listOf(defaultProduct, defaultProduct.copy(remoteId = 2))
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
            assertThat(emptySearchQuery.popularItems).hasSize(2)
            assertThat(emptySearchQuery.popularItems.map { it.id }).containsExactly(1, 2)
            assertThat(emptySearchQuery.recentSearches).hasSize(4)
            assertThat(emptySearchQuery.recentSearches).containsExactly(
                "Recent Search 1",
                "Recent Search 2",
                "Recent Search 3",
                "Recent Search 4",
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
            advanceTimeBy(600)

            // THEN
            viewModel.viewState.test {
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
            val state = awaitItem()
            assertThat(state).isInstanceOf(WooPosItemsSearchViewState.Empty::class.java)
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
            val variableProduct = generateWooPosProduct(
                productId = 1,
                productName = "Variable Product",
                amount = "10.0",
                productType = WooPosProductModel.WooPosProductType.Variable,
                variationIds = listOf(101L, 102L, 103L)
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

                val variableProductResult = value.items[0] as Product.Variable
                assertThat(variableProductResult.name).isEqualTo("Variable Product")
                assertThat(variableProductResult.numOfVariations).isEqualTo(3)
                assertThat(variableProductResult.variationIds).containsExactly(101L, 102L, 103L)
            }
        }

    @Test
    fun `given content state and more pages available, when end of list reached, then load more data`() = runTest {
        // GIVEN
        val additionalProduct = generateWooPosProduct(
            productId = 2,
            productName = "Test Product 2",
            amount = "20.0",
            productType = WooPosProductModel.WooPosProductType.Simple
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
            assertThat(loadingState.paginationState).isEqualTo(WooPosPaginationState.Loading)

            val finalState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(finalState.items).hasSize(1)
            assertThat(finalState.paginationState).isEqualTo(WooPosPaginationState.None)
        }
    }

    @Test
    fun `given content state and more pages available, when end of list reached, then track ItemsNextPageLoaded event`() =
        runTest {
            // GIVEN
            val additionalProduct = generateWooPosProduct(
                productId = 2,
                productName = "Test Product 2",
                amount = "20.0",
                productType = WooPosProductModel.WooPosProductType.Simple
            )

            mockSuccessfulSearch(defaultQuery, listOf(defaultProduct))
            mockSuccessfulPagination(defaultQuery, listOf(additionalProduct))

            // WHEN
            val viewModel = createViewModel()
            advanceTimeBy(600)
            viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnNextPageRequested)
            advanceUntilIdle()

            // THEN
            verify(mockAnalyticsTracker).trackItemsNextPageLoaded()
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
            assertThat(loadingState.paginationState).isEqualTo(WooPosPaginationState.Loading)

            val errorState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(errorState.paginationState).isEqualTo(WooPosPaginationState.Error)
        }
    }

    @Test
    fun `given cached products, when search performed, then cached results shown while loading`() = runTest {
        // GIVEN
        val cachedProduct = generateWooPosProduct(
            productId = 1,
            productName = "Cached Product",
            amount = "10.0",
            productType = WooPosProductModel.WooPosProductType.Simple
        )

        val remoteProduct = generateWooPosProduct(
            productId = 2,
            productName = "Remote Product",
            amount = "20.0",
            productType = WooPosProductModel.WooPosProductType.Simple
        )

        mockCachedThenRemoteSearch(defaultQuery, cachedProduct, remoteProduct)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val cachedState = awaitItem() as WooPosItemsSearchViewState.Content
            assertThat(cachedState.items).hasSize(1)
            assertThat((cachedState.items[0] as Product.Simple).name).isEqualTo("Cached Product")

            advanceUntilIdle()

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
            generateWooPosProduct(
                productId = 1,
                productName = "Test Product",
                amount = "10.0",
                productType = WooPosProductModel.WooPosProductType.Simple
            )
        )

        whenever(mockDataSource.searchProducts(query1)).thenReturn(
            flowOf(
                SearchProductsResult.Local(emptyList(), searchTimeMillis = 0L, searchMethod = "fts"),
                SearchProductsResult.Remote(Result.success(emptyList()), 100L)
            )
        )
        whenever(mockDataSource.searchProducts(query2)).thenReturn(
            flowOf(
                SearchProductsResult.Local(emptyList(), searchTimeMillis = 0L, searchMethod = "fts"),
                SearchProductsResult.Remote(Result.success(products), 100L)
            )
        )
        whenever(mockDataSource.hasMoreSearchPages).thenReturn(false)

        whenever(mockPriceFormat(BigDecimal("10.0"))).thenReturn("$10.0")

        val searchEvents = flow {
            emit(ParentToChildrenEvent.SearchEvent.ChangedQuery(query1))
            delay(100)
            emit(ParentToChildrenEvent.SearchEvent.ChangedQuery(query2))
        }

        whenever(mockParentToChildrenEventReceiver.events).thenReturn(searchEvents)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            skipItems(1) // Skip initial EmptySearchQuery state

            advanceUntilIdle()

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
            generateWooPosProduct(
                productId = 1,
                productName = "Test Product",
                amount = "10.0",
                productType = WooPosProductModel.WooPosProductType.Simple
            )
        )

        whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(emptyList())
        whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(emptyList())

        whenever(mockDataSource.searchProducts(query)).thenReturn(
            flow {
                emit(SearchProductsResult.Local(emptyList(), searchTimeMillis = 0L, searchMethod = "fts"))
                delay(100)
                emit(SearchProductsResult.Remote(Result.success(products), 100L))
            }
        )

        whenever(mockPriceFormat(BigDecimal("10.0"))).thenReturn("$10.0")
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(query))
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(WooPosItemsSearchViewState.Loading::class.java)

            advanceUntilIdle()
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
                generateWooPosProduct(
                    productId = 1,
                    productName = "Test Product",
                    amount = "10.0",
                    productType = WooPosProductModel.WooPosProductType.Simple
                )
            )

            whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(emptyList())
            whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(emptyList())

            whenever(mockDataSource.searchProducts(query)).thenReturn(
                flowOf(
                    SearchProductsResult.Remote(Result.success(products), 100L)
                )
            )
            whenever(mockDataSource.hasMoreSearchPages).thenReturn(false)

            whenever(mockPriceFormat(BigDecimal("10.0"))).thenReturn("$10.0")
            whenever(mockParentToChildrenEventReceiver.events).thenReturn(
                flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(query))
            )

            // WHEN
            val viewModel = createViewModel()
            advanceUntilIdle()

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
            generateWooPosProduct(
                productId = 1,
                productName = "Test Product",
                amount = "10.0",
                productType = WooPosProductModel.WooPosProductType.Simple
            )
        )

        whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(emptyList())
        whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(emptyList())

        whenever(mockDataSource.searchProducts(query)).thenReturn(
            flowOf(
                SearchProductsResult.Remote(Result.failure(Exception("Search failed")), 100L)
            )
        ).thenReturn(
            flowOf(
                SearchProductsResult.Remote(Result.success(products), 100L)
            )
        )

        whenever(mockPriceFormat(BigDecimal("10.0"))).thenReturn("$10.0")
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(query))
        )

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        viewModel.viewState.test {
            val errorState = awaitItem()
            assertThat(errorState).isInstanceOf(WooPosItemsSearchViewState.Error::class.java)
            assertThat((errorState as WooPosItemsSearchViewState.Error).searchQuery).isEqualTo(query)

            viewModel.onUIEvent(WooPosItemsSearchUiEvent.LoadingErrorRetryButtonClicked)

            skipItems(1)

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
        viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnItemClicked(simpleProduct))

        // THEN
        val item = ItemClickedData.Product.Simple(id = 1)
        verify(mockChildToParentEventSender).sendToParent(
            eq(
                ChildToParentEvent.ItemClickedInItemsList(
                    itemData = item,
                    eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                        item = item,
                        source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT,
                    )
                )
            )
        )
    }

    @Test
    fun `given variation, when item clicked, then send variation click event to parent`() = runTest {
        // GIVEN
        val variation = Product.VariationSearchResult(
            id = 1,
            name = "Test Variation",
            price = "$10.0",
            productId = 42L,
            imageUrl = null,
            parentProductName = "Parent Product",
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnItemClicked(variation))

        // THEN
        val itemData = ItemClickedData.Product.Variation(productId = 42L, id = 1)
        verify(mockChildToParentEventSender).sendToParent(
            eq(
                ChildToParentEvent.ItemClickedInItemsList(
                    itemData = itemData,
                    eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                        item = itemData,
                        source = WooPosAnalyticsEventConstant.ItemsListSource.VARIATION,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT,
                    )
                )
            )
        )
    }

    @Test
    fun `given recent search, when recent search clicked, then send event to parent`() = runTest {
        // GIVEN
        val recentSearch = "T-shirt"

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnRecentSearchClicked(recentSearch))

        // THEN
        verify(mockChildToParentEventSender).sendToParent(
            ChildToParentEvent.SearchEvent.RecentSearchSelected(recentSearch)
        )
    }

    @Test
    fun `when recent search clicked, then track PreSearchRecentTermTapped event`() = runTest {
        // GIVEN
        val recentSearch = "T-shirt"

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnRecentSearchClicked(recentSearch))
        advanceUntilIdle()

        // THEN
        verify(mockAnalyticsTracker).trackRecentSearchSelected()
    }

    @Test
    fun `given search query in content state, when simple product clicked, then search is stored as recent`() =
        runTest {
            // GIVEN
            val query = "test query"
            val simpleProduct = Product.Simple(id = 1, name = "Test Product", price = "$10.0", imageUrl = null)

            mockSuccessfulSearch(query, listOf(defaultProduct))

            // WHEN
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnItemClicked(simpleProduct))

            // THEN
            verify(mockEmptyStateProvider).addRecentSearch(query)
        }

    @Test
    fun `given non-content state, when simple product clicked, then no recent search is stored`() = runTest {
        // GIVEN
        val simpleProduct = Product.Simple(id = 1, name = "Test Product", price = "$10.0", imageUrl = null)

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnItemClicked(simpleProduct))

        // THEN
        verify(mockEmptyStateProvider, never()).addRecentSearch(any())
    }

    @Test
    fun `given popular item, when OnPopularItemClicked event is triggered, then add popular items to cache and handle item click`() =
        runTest {
            // GIVEN
            val simpleProduct = Product.Simple(
                id = 42L,
                name = "Popular Product",
                price = "$15.0",
                imageUrl = "https://example.com/image.jpg"
            )

            // WHEN
            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnPopularItemClicked(simpleProduct))
            advanceUntilIdle()

            // THEN
            verify(mockEmptyStateProvider).addPopularItemsToCache()
            verify(mockChildToParentEventSender).sendToParent(
                ChildToParentEvent.ItemClickedInItemsList(
                    ItemClickedData.Product.Simple(id = simpleProduct.id),
                    eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                        item = ItemClickedData.Product.Simple(id = simpleProduct.id),
                        source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.POPULAR_PRODUCTS,
                    )
                )
            )
        }

    @Test
    fun `given product is in local search result, when item clicked, then send product click event to parent with local source`() =
        runTest {
            // GIVEN
            val simpleProduct = Product.Simple(id = 1, name = "Test Product", price = "$10.0", imageUrl = null)
            whenever(mockAnalyticsTracker.isProductInTheLocalSearchResult(1)).thenReturn(true)

            // WHEN
            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnItemClicked(simpleProduct))

            // THEN
            val item = ItemClickedData.Product.Simple(id = 1)
            verify(mockChildToParentEventSender).sendToParent(
                ChildToParentEvent.ItemClickedInItemsList(
                    itemData = item,
                    eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                        item = item,
                        source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT_LOCAL,
                    ),
                )
            )
        }

    @Test
    fun `given product is not in local search result, when item clicked, then send product click event to parent with remote source`() =
        runTest {
            // GIVEN
            val simpleProduct = Product.Simple(id = 1, name = "Test Product", price = "$10.0", imageUrl = null)
            whenever(mockAnalyticsTracker.isProductInTheLocalSearchResult(1)).thenReturn(false)

            // WHEN
            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnItemClicked(simpleProduct))

            // THEN
            val item = ItemClickedData.Product.Simple(id = 1)
            verify(mockChildToParentEventSender).sendToParent(
                ChildToParentEvent.ItemClickedInItemsList(
                    itemData = item,
                    eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                        item = item,
                        source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT,
                    ),
                )
            )
        }

    @Test
    fun `given local catalog sync strategy, when PTR triggered, then refresh products and rerun search`() = runTest {
        // GIVEN
        mockSuccessfulSearch(defaultQuery, listOf(defaultProduct))
        whenever(
            mockDataSource.getCurrentSyncStrategy()
        ).thenReturn(WooPosProductsDataSource.SyncStrategy.LOCAL_CATALOG_FILE)
        whenever(mockDataSource.refreshProducts()).thenReturn(
            Result.success(PosLocalCatalogProductSyncResult(PosLocalCatalogSyncResult.Success(1, 0, 100L)))
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnPullToRefreshTriggered)
        advanceUntilIdle()

        // THEN
        verify(mockDataSource).refreshProducts()
        verify(mockDataSource, times(2)).searchProducts(defaultQuery)
        viewModel.viewState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(WooPosItemsSearchViewState.Content::class.java)
            assertThat((state as WooPosItemsSearchViewState.Content).pullToRefreshState)
                .isEqualTo(WooPosPullToRefreshState.Enabled)
        }
    }

    @Test
    fun `given remote sync strategy, when content state loaded, then PTR state is Disabled`() = runTest {
        // GIVEN
        mockSuccessfulSearch(defaultQuery, listOf(defaultProduct))
        whenever(mockDataSource.getCurrentSyncStrategy()).thenReturn(WooPosProductsDataSource.SyncStrategy.REMOTE)

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        viewModel.viewState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(WooPosItemsSearchViewState.Content::class.java)
            assertThat((state as WooPosItemsSearchViewState.Content).pullToRefreshState)
                .isEqualTo(WooPosPullToRefreshState.Disabled)
        }
    }

    @Test
    fun `given PTR triggered and sync fails, when error occurs, then toast shown and PTR state reset`() = runTest {
        // GIVEN
        mockSuccessfulSearch(defaultQuery, listOf(defaultProduct))
        whenever(
            mockDataSource.getCurrentSyncStrategy()
        ).thenReturn(WooPosProductsDataSource.SyncStrategy.LOCAL_CATALOG_FILE)
        whenever(mockDataSource.refreshProducts()).thenReturn(
            Result.success(
                PosLocalCatalogProductSyncResult(
                    PosLocalCatalogSyncResult.Failure.NetworkError("Network error")
                )
            )
        )
        whenever(mockResourceProvider.getString(any())).thenReturn("Something went wrong")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnPullToRefreshTriggered)
        advanceUntilIdle()

        // THEN
        verify(mockChildToParentEventSender).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
        viewModel.viewState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(WooPosItemsSearchViewState.Content::class.java)
            assertThat((state as WooPosItemsSearchViewState.Content).pullToRefreshState)
                .isEqualTo(WooPosPullToRefreshState.Enabled)
        }
    }

    @Test
    fun `given PTR triggered, when event fired, then analytics event tracked`() = runTest {
        // GIVEN
        mockSuccessfulSearch(defaultQuery, listOf(defaultProduct))
        whenever(
            mockDataSource.getCurrentSyncStrategy()
        ).thenReturn(WooPosProductsDataSource.SyncStrategy.LOCAL_CATALOG_FILE)
        whenever(mockDataSource.refreshProducts()).thenReturn(
            Result.success(PosLocalCatalogProductSyncResult(PosLocalCatalogSyncResult.Success(1, 0, 100L)))
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnPullToRefreshTriggered)
        advanceUntilIdle()

        // THEN
        verify(mockWooPosAnalyticsTracker).track(
            WooPosAnalyticsEvent.Event.PullToRefreshTriggered(
                WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT
            )
        )
    }

    @Test
    fun `given local catalog sync strategy, when content state loaded, then PTR state is Enabled`() = runTest {
        // GIVEN
        mockSuccessfulSearch(defaultQuery, listOf(defaultProduct))
        whenever(
            mockDataSource.getCurrentSyncStrategy()
        ).thenReturn(WooPosProductsDataSource.SyncStrategy.LOCAL_CATALOG_FILE)

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        viewModel.viewState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(WooPosItemsSearchViewState.Content::class.java)
            assertThat((state as WooPosItemsSearchViewState.Content).pullToRefreshState)
                .isEqualTo(WooPosPullToRefreshState.Enabled)
        }
    }

    @Test
    fun `given empty search results with local catalog sync, when PTR triggered, then refresh products and rerun search`() =
        runTest {
            // GIVEN
            mockSuccessfulSearch(defaultQuery, emptyList())
            whenever(
                mockDataSource.getCurrentSyncStrategy()
            ).thenReturn(WooPosProductsDataSource.SyncStrategy.LOCAL_CATALOG_FILE)
            whenever(mockDataSource.refreshProducts()).thenReturn(
                Result.success(PosLocalCatalogProductSyncResult(PosLocalCatalogSyncResult.Success(1, 0, 100L)))
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            // Verify we're in Empty state
            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(WooPosItemsSearchViewState.Empty::class.java)
            }

            // WHEN
            viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnPullToRefreshTriggered)
            advanceUntilIdle()

            // THEN
            verify(mockDataSource).refreshProducts()
            verify(mockDataSource, times(2)).searchProducts(defaultQuery)
            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(WooPosItemsSearchViewState.Empty::class.java)
                assertThat((state as WooPosItemsSearchViewState.Empty).pullToRefreshState)
                    .isEqualTo(WooPosPullToRefreshState.Enabled)
            }
        }

    @Test
    fun `given empty search results, when PTR triggered and sync finishes, then PTR indicator disappears`() =
        runTest {
            // GIVEN
            mockSuccessfulSearch(defaultQuery, emptyList())
            whenever(
                mockDataSource.getCurrentSyncStrategy()
            ).thenReturn(WooPosProductsDataSource.SyncStrategy.LOCAL_CATALOG_FILE)
            whenever(mockDataSource.refreshProducts()).thenReturn(
                Result.success(PosLocalCatalogProductSyncResult(PosLocalCatalogSyncResult.Success(1, 0, 100L)))
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(WooPosItemsSearchViewState.Empty::class.java)
                assertThat((state as WooPosItemsSearchViewState.Empty).pullToRefreshState)
                    .isEqualTo(WooPosPullToRefreshState.Enabled)
            }

            // WHEN
            viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnPullToRefreshTriggered)
            advanceUntilIdle()

            // THEN
            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(WooPosItemsSearchViewState.Empty::class.java)
                assertThat((state as WooPosItemsSearchViewState.Empty).pullToRefreshState)
                    .isNotEqualTo(WooPosPullToRefreshState.Refreshing)
                assertThat(state.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
            }
        }

    @Test
    fun `given remote sync strategy, when empty search result shown, then PTR state is Disabled`() = runTest {
        // GIVEN
        mockSuccessfulSearch(defaultQuery, emptyList())
        whenever(mockDataSource.getCurrentSyncStrategy()).thenReturn(WooPosProductsDataSource.SyncStrategy.REMOTE)

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        viewModel.viewState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(WooPosItemsSearchViewState.Empty::class.java)
            assertThat((state as WooPosItemsSearchViewState.Empty).pullToRefreshState)
                .isEqualTo(WooPosPullToRefreshState.Disabled)
        }
    }

    @Test
    fun `given empty search results and PTR fails, when error occurs, then toast shown and PTR state reset`() =
        runTest {
            // GIVEN
            mockSuccessfulSearch(defaultQuery, emptyList())
            whenever(
                mockDataSource.getCurrentSyncStrategy()
            ).thenReturn(WooPosProductsDataSource.SyncStrategy.LOCAL_CATALOG_FILE)
            whenever(mockDataSource.refreshProducts()).thenReturn(
                Result.success(
                    PosLocalCatalogProductSyncResult(
                        PosLocalCatalogSyncResult.Failure.NetworkError("Network error")
                    )
                )
            )
            whenever(mockResourceProvider.getString(any())).thenReturn("Something went wrong")

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(WooPosItemsSearchViewState.Empty::class.java)
            }

            // WHEN
            viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnPullToRefreshTriggered)
            advanceUntilIdle()

            // THEN
            verify(mockChildToParentEventSender).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(WooPosItemsSearchViewState.Empty::class.java)
                assertThat((state as WooPosItemsSearchViewState.Empty).pullToRefreshState)
                    .isEqualTo(WooPosPullToRefreshState.Enabled)
            }
        }

    @Test
    fun `given network error on PTR, when pull to refresh triggered, then show offline error message`() = runTest {
        // GIVEN
        mockSuccessfulSearch(defaultQuery, listOf(defaultProduct))
        whenever(mockDataSource.refreshProducts()).thenReturn(
            Result.success(
                PosLocalCatalogProductSyncResult(
                    PosLocalCatalogSyncResult.Failure.NetworkError("Network unavailable")
                )
            )
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnPullToRefreshTriggered)

        // THEN
        verify(mockChildToParentEventSender).sendToParent(
            eq(ChildToParentEvent.ToastMessageDisplayed(message = "No internet connection"))
        )
    }

    @Test
    fun `given database error on PTR, when pull to refresh triggered, then show generic error message`() = runTest {
        // GIVEN
        mockSuccessfulSearch(defaultQuery, listOf(defaultProduct))
        whenever(mockDataSource.refreshProducts()).thenReturn(
            Result.success(
                PosLocalCatalogProductSyncResult(
                    PosLocalCatalogSyncResult.Failure.DatabaseError("Database error")
                )
            )
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnPullToRefreshTriggered)

        // THEN
        verify(mockChildToParentEventSender).sendToParent(
            eq(ChildToParentEvent.ToastMessageDisplayed(message = "Something went wrong"))
        )
    }

    private fun mockSuccessfulSearch(query: String, products: List<WooPosProductModel>) {
        whenever(mockDataSource.searchProducts(query)).thenReturn(
            flowOf(
                SearchProductsResult.Local(emptyList(), searchTimeMillis = 0L, searchMethod = "fts"),
                SearchProductsResult.Remote(Result.success(products), 100L)
            )
        )
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(query))
        )
    }

    private fun mockFailedSearch(query: String, error: Exception) {
        whenever(mockDataSource.searchProducts(query)).thenReturn(
            flowOf(
                SearchProductsResult.Local(emptyList(), searchTimeMillis = 0L, searchMethod = "fts"),
                SearchProductsResult.Remote(Result.failure(error), 100L)
            )
        )
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(query))
        )
    }

    private suspend fun mockSuccessfulPagination(query: String, products: List<WooPosProductModel>) {
        whenever(mockDataSource.hasMoreSearchPages).thenReturn(true)
        whenever(mockDataSource.loadMoreSearchResults(query)).thenReturn(
            Result.success(products)
        )
    }

    private suspend fun mockFailedPagination(query: String, error: Exception) {
        whenever(mockDataSource.hasMoreSearchPages).thenReturn(true)
        whenever(mockDataSource.loadMoreSearchResults(query)).thenReturn(
            Result.failure(error)
        )
    }

    private fun mockCachedThenRemoteSearch(
        query: String,
        cachedProduct: WooPosProductModel,
        remoteProduct: WooPosProductModel
    ) {
        whenever(mockDataSource.searchProducts(query)).thenReturn(
            flow {
                emit(SearchProductsResult.Local(listOf(cachedProduct), searchTimeMillis = 10L, searchMethod = "fts"))
                delay(100) // Small delay between emissions
                emit(SearchProductsResult.Remote(Result.success(listOf(remoteProduct)), 100L))
            }
        )
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(query))
        )
    }

    @Test
    fun `when local search returns results, then trackLocalSearchResults is called`() = runTest {
        // GIVEN
        val products = listOf(defaultProduct)
        whenever(mockDataSource.searchProducts(defaultQuery)).thenReturn(
            flowOf(
                SearchProductsResult.Local(
                    products = products,
                    searchTimeMillis = 42L,
                    searchMethod = "fts",
                ),
            )
        )
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(defaultQuery))
        )

        // WHEN
        createViewModel()
        advanceUntilIdle()

        // THEN
        verify(mockAnalyticsTracker).trackLocalSearchResults(
            resultsCount = 1,
            searchTimeMillis = 42L,
            searchMethod = "fts",
        )
    }

    @Test
    fun `when search result item is tapped, then trackSearchResultTapped is called`() = runTest {
        // GIVEN
        val product2 = generateWooPosProduct(
            productId = 2,
            productName = "Test Product 2",
            amount = "20.0",
            productType = WooPosProductModel.WooPosProductType.Simple
        )
        val products = listOf(defaultProduct, product2)
        mockSuccessfulSearch(defaultQuery, products)
        whenever(mockAnalyticsTracker.isProductInTheLocalSearchResult(any())).thenReturn(true)

        // WHEN
        val viewModel = createViewModel()
        advanceTimeBy(600)

        viewModel.viewState.test {
            val contentState = awaitItem() as WooPosItemsSearchViewState.Content
            val secondItem = contentState.items[1]
            viewModel.onUIEvent(WooPosItemsSearchUiEvent.OnItemClicked(secondItem))
            advanceUntilIdle()

            // THEN
            verify(mockAnalyticsTracker).trackSearchResultTapped(
                resultPosition = 1,
                resultType = "product",
            )
        }
    }

    private fun createViewModel() = WooPosItemsSearchViewModel(
        emptyStateRepository = mockEmptyStateProvider,
        priceFormat = mockPriceFormat,
        dataSource = mockDataSource,
        childToParentEventSender = mockChildToParentEventSender,
        parentToChildrenEventReceiver = mockParentToChildrenEventReceiver,
        searchHelper = mockSearchHelper,
        analyticsTracker = mockAnalyticsTracker,
        resourceProvider = mockResourceProvider,
        wooPosAnalyticsTracker = mockWooPosAnalyticsTracker,
    )
}
