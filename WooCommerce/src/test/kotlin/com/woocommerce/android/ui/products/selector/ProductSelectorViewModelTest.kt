package com.woocommerce.android.ui.products.selector

import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_SELECTOR_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ProductSelectorRestriction.NoVariableProductsWithNoVariations
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ProductSelectorRestriction.OnlyPublishedProducts
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorRepository
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
internal class ProductSelectorViewModelTest : BaseUnitTest() {
    companion object {
        private val VALID_PRODUCT = ProductTestUtils.generateProduct(productId = 1L)
        private val DRAFT_PRODUCT = ProductTestUtils.generateProduct(productId = 2L, customStatus = "draft")
        private val VARIABLE_PRODUCT_WITH_NO_VARIATIONS = ProductTestUtils.generateProduct(
            productId = 3L,
            isVariable = true,
            variationIds = "[]",
        )
    }

    private val currencyFormatter: CurrencyFormatter = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val listHandler: ProductListHandler = mock {
        on { productsFlow } doReturn flowOf(listOf(VALID_PRODUCT, DRAFT_PRODUCT, VARIABLE_PRODUCT_WITH_NO_VARIATIONS))
    }
    private val variationSelectorRepository: VariationSelectorRepository = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val orderStore: WCOrderStore = mock()
    private val productsMapper: ProductsMapper = mock()
    private val analyticsTracksWrapper: AnalyticsTrackerWrapper = mock()

    @Test
    fun `given published products restriction, when view model created, should not show draft products`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = arrayOf(OnlyPublishedProducts),
        ).initSavedStateHandle()

        val sut = ProductSelectorViewModel(
            navArgs,
            currencyFormatter,
            wooCommerceStore,
            orderStore,
            selectedSite,
            listHandler,
            variationSelectorRepository,
            resourceProvider,
            productsMapper,
            analyticsTracksWrapper,
        )

        sut.viewState.observeForever { state ->
            assertThat(state.products).isNotEmpty
            assertThat(state.products.filter { it.id == DRAFT_PRODUCT.remoteId }).isEmpty()
        }
    }

    @Test
    fun `given no variable products with no variations restriction, when view model created, should not show variable products with no variations`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = arrayOf(NoVariableProductsWithNoVariations),
        ).initSavedStateHandle()

        val sut = ProductSelectorViewModel(
            navArgs,
            currencyFormatter,
            wooCommerceStore,
            orderStore,
            selectedSite,
            listHandler,
            variationSelectorRepository,
            resourceProvider,
            productsMapper,
            analyticsTracksWrapper,
        )

        sut.viewState.observeForever { state ->
            assertThat(state.products).isNotEmpty
            assertThat(
                state.products.filter { it.type == ProductType.VARIABLE && it.numVariations == 0 }
            ).isEmpty()
        }
    }

    @Test
    fun `given multiple restrictions, when view model created, should show correct products`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = arrayOf(OnlyPublishedProducts, NoVariableProductsWithNoVariations),
        ).initSavedStateHandle()

        val sut = ProductSelectorViewModel(
            navArgs,
            currencyFormatter,
            wooCommerceStore,
            orderStore,
            selectedSite,
            listHandler,
            variationSelectorRepository,
            resourceProvider,
            productsMapper,
            analyticsTracksWrapper,
        )

        sut.viewState.observeForever { state ->
            assertThat(state.products).isNotEmpty
            assertThat(
                state.products.filter { it.type == ProductType.VARIABLE && it.numVariations == 0 }
            ).isEmpty()
            assertThat(state.products.filter { it.id == DRAFT_PRODUCT.remoteId }).isEmpty()
        }
    }

    @Test
    fun `given no restrictions, when view model created, should show all products`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = emptyArray(),
        ).initSavedStateHandle()

        val sut = ProductSelectorViewModel(
            navArgs,
            currencyFormatter,
            wooCommerceStore,
            orderStore,
            selectedSite,
            listHandler,
            variationSelectorRepository,
            resourceProvider,
            productsMapper,
            analyticsTracksWrapper,
        )

        sut.viewState.observeForever { state ->
            assertThat(state.products.count()).isEqualTo(3)
            assertThat(
                state.products.map { it.id }
            ).containsAll(
                listOf(VALID_PRODUCT.remoteId, DRAFT_PRODUCT.remoteId, VARIABLE_PRODUCT_WITH_NO_VARIATIONS.remoteId)
            )
        }
    }

    // region Sort by popularity and recently sold products

    @Test
    fun `given popular products, when view model created, then verify popular products are sorted in descending order`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val popularOrdersList = generatePopularOrders()
            val ordersList = generateTestOrders()
            val totalOrders = popularOrdersList + ordersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)
            val argumentCaptor = argumentCaptor<List<Long>>()

            ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )

            verify(productsMapper, times(2)).mapProductIdsToProduct(argumentCaptor.capture())
            assertThat(argumentCaptor.firstValue).isEqualTo(
                listOf(2445L, 2448L, 2447L, 2444L, 2446L)
            )
        }
    }

    @Test
    fun `given popular products, when view model created, then only filter popular products from the orders that are already paid`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val popularOrdersList = generatePopularOrders()
            val popularOrdersThatAreNotPaidYet = mutableListOf<OrderEntity>()
            repeat(10) {
                popularOrdersThatAreNotPaidYet.add(
                    OrderTestUtils.generateOrder(
                        lineItems = generateLineItems(
                            name = "ACME Bike",
                            productId = "1111"
                        ),
                        datePaid = ""
                    ),
                )
            }
            val ordersList = generateTestOrders()
            val totalOrders = popularOrdersList + popularOrdersThatAreNotPaidYet + ordersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)
            val argumentCaptor = argumentCaptor<List<Long>>()

            ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )

            verify(productsMapper, times(2)).mapProductIdsToProduct(argumentCaptor.capture())
            assertThat(argumentCaptor.firstValue).isEqualTo(
                listOf(2445L, 2448L, 2447L, 2444L, 2446L)
            )
        }
    }

    @Test
    fun `given popular products, when searched for products, then hide the popular products section`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val popularOrdersList = generatePopularOrders()
            val ordersList = generateTestOrders()
            val totalOrders = popularOrdersList + ordersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(ProductTestUtils.generateProductList())

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onSearchQueryChanged("Test query")

            var viewState: ProductSelectorViewModel.ViewState? = null
            sut.viewState.observeForever { state ->
                viewState = state
            }

            assertThat(viewState?.popularProducts)?.isEmpty()
        }
    }

    @Test
    fun `given popular products, when search query cleared from the search view, then show the popular products section`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val popularOrdersList = generatePopularOrders()
            val ordersList = generateTestOrders()
            val totalOrders = popularOrdersList + ordersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(ProductTestUtils.generateProductList())

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onSearchQueryChanged("Test query")

            var viewState: ProductSelectorViewModel.ViewState? = null
            sut.viewState.observeForever { state ->
                viewState = state
            }

            assertThat(viewState?.popularProducts)?.isEmpty()

            sut.onSearchQueryChanged("")

            assertThat(viewState?.popularProducts)?.isNotEmpty
        }
    }

    @Test
    fun `given recent products, when searched for products, then hide the recent products section`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val ordersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(ordersList)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(ProductTestUtils.generateProductList())

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onSearchQueryChanged("Test query")

            var viewState: ProductSelectorViewModel.ViewState? = null
            sut.viewState.observeForever { state ->
                viewState = state
            }

            assertThat(viewState?.recentProducts)?.isEmpty()
        }
    }

    @Test
    fun `given recent products, when search query cleared from the search view, then show the recent products section`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val ordersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(ordersList)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(ProductTestUtils.generateProductList())

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onSearchQueryChanged("Test query")

            var viewState: ProductSelectorViewModel.ViewState? = null
            sut.viewState.observeForever { state ->
                viewState = state
            }

            assertThat(viewState?.recentProducts)?.isEmpty()

            sut.onSearchQueryChanged("")

            assertThat(viewState?.recentProducts)?.isNotEmpty
        }
    }

    @Test
    fun `given recent products, when view model created, then verify recent products are sorted in descending order`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)
            val argumentCaptor = argumentCaptor<List<Long>>()

            ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )

            verify(productsMapper, times(2)).mapProductIdsToProduct(argumentCaptor.capture())
            assertThat(argumentCaptor.firstValue).isEqualTo(
                listOf(2444L, 2446L, 2449L, 2450L, 2451L)
            )
        }
    }

    @Test
    fun `given recent products, when view model created, then only filter recent products from the orders that are already paid`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val ordersThatAreNotPaidYet = mutableListOf<OrderEntity>()
            repeat(10) {
                ordersThatAreNotPaidYet.add(
                    OrderTestUtils.generateOrder(
                        lineItems = generateLineItems(
                            name = "ACME Bike",
                            productId = "1111"
                        ),
                        datePaid = ""
                    ),
                )
            }
            val recentOrdersList = generateTestOrders()
            val totalOrders = ordersThatAreNotPaidYet + recentOrdersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)
            val argumentCaptor = argumentCaptor<List<Long>>()

            ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )

            verify(productsMapper, times(2)).mapProductIdsToProduct(argumentCaptor.capture())
            assertThat(argumentCaptor.firstValue).isEqualTo(
                listOf(2444L, 2446L, 2449L, 2450L, 2451L)
            )
        }
    }

    //endregion

    // region sort by popularity and recently sold, analytics
    @Test
    fun `given product selected from popular section, then track correct source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val ordersThatAreNotPaidYet = mutableListOf<OrderEntity>()
            val recentOrdersList = generateTestOrders()
            val totalOrders = ordersThatAreNotPaidYet + recentOrdersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onProductClick(
                item = generateProductListItem(0L),
                productSourceForTracking = ProductSourceForTracking.POPULAR
            )
            sut.onDoneButtonClick()

            verify(analyticsTracksWrapper).track(
                PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(ProductSourceForTracking.POPULAR)
                )
            )
        }
    }

    @Test
    fun `given product selected from recent section, then track correct source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val ordersThatAreNotPaidYet = mutableListOf<OrderEntity>()
            val recentOrdersList = generateTestOrders()
            val totalOrders = ordersThatAreNotPaidYet + recentOrdersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onProductClick(
                item = generateProductListItem(id = 0L),
                productSourceForTracking = ProductSourceForTracking.RECENT
            )
            sut.onProductClick(
                item = generateProductListItem(id = 1L),
                productSourceForTracking = ProductSourceForTracking.POPULAR
            )
            sut.onDoneButtonClick()

            verify(analyticsTracksWrapper).track(
                PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.RECENT,
                        ProductSourceForTracking.POPULAR
                    )
                )
            )
        }
    }

    @Test
    fun `given product selected from alphabetical section, then track correct source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val ordersThatAreNotPaidYet = mutableListOf<OrderEntity>()
            val recentOrdersList = generateTestOrders()
            val totalOrders = ordersThatAreNotPaidYet + recentOrdersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onProductClick(
                item = generateProductListItem(id = 0L),
                productSourceForTracking = ProductSourceForTracking.RECENT
            )
            sut.onProductClick(
                item = generateProductListItem(id = 2L),
                productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
            )
            sut.onProductClick(
                item = generateProductListItem(id = 1L),
                productSourceForTracking = ProductSourceForTracking.POPULAR
            )
            sut.onDoneButtonClick()

            verify(analyticsTracksWrapper).track(
                PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.RECENT,
                        ProductSourceForTracking.ALPHABETICAL,
                        ProductSourceForTracking.POPULAR
                    )
                )
            )
        }
    }

    @Test
    fun `given product selected from search section, then track correct source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val ordersThatAreNotPaidYet = mutableListOf<OrderEntity>()
            val recentOrdersList = generateTestOrders()
            val totalOrders = ordersThatAreNotPaidYet + recentOrdersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onSearchQueryChanged("Test")
            sut.onProductClick(
                item = generateProductListItem(id = 0L),
                productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
            )
            sut.onDoneButtonClick()

            verify(analyticsTracksWrapper).track(
                PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.SEARCH
                    )
                )
            )
        }
    }

    @Test
    fun `given product selected from filter section, then track correct source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val ordersThatAreNotPaidYet = mutableListOf<OrderEntity>()
            val recentOrdersList = generateTestOrders()
            val totalOrders = ordersThatAreNotPaidYet + recentOrdersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onFiltersChanged(
                stockStatus = "In stock",
                productCategory = null,
                productStatus = null,
                productType = null,
                productCategoryName = null
            )
            sut.onProductClick(
                item = generateProductListItem(id = 0L),
                productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
            )
            sut.onDoneButtonClick()

            verify(analyticsTracksWrapper).track(
                PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.FILTER
                    )
                )
            )
        }
    }

    @Test
    fun `given product variation selected from popular section, then track correct source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onSelectedVariationsUpdated(
                VariationSelectorViewModel.VariationSelectionResult(
                    productId = 0L,
                    selectedVariationIds = setOf(1L),
                    productSourceForTracking = ProductSourceForTracking.POPULAR
                )
            )
            sut.onDoneButtonClick()

            verify(analyticsTracksWrapper).track(
                PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.POPULAR
                    )
                )
            )
        }
    }

    @Test
    fun `given product variation selected from recent section, then track correct source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onSelectedVariationsUpdated(
                VariationSelectorViewModel.VariationSelectionResult(
                    productId = 0L,
                    selectedVariationIds = setOf(1L),
                    productSourceForTracking = ProductSourceForTracking.RECENT
                )
            )
            sut.onDoneButtonClick()

            verify(analyticsTracksWrapper).track(
                PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.RECENT
                    )
                )
            )
        }
    }

    @Test
    fun `given product variation selected from alphabetical section, then track correct source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onSelectedVariationsUpdated(
                VariationSelectorViewModel.VariationSelectionResult(
                    productId = 0L,
                    selectedVariationIds = setOf(1L),
                    productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
                )
            )
            sut.onDoneButtonClick()

            verify(analyticsTracksWrapper).track(
                PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.ALPHABETICAL
                    )
                )
            )
        }
    }

    @Test
    fun `given product variation selected from search section, then track correct source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onSelectedVariationsUpdated(
                VariationSelectorViewModel.VariationSelectionResult(
                    productId = 0L,
                    selectedVariationIds = setOf(1L),
                    productSourceForTracking = ProductSourceForTracking.SEARCH
                )
            )
            sut.onDoneButtonClick()

            verify(analyticsTracksWrapper).track(
                PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.SEARCH
                    )
                )
            )
        }
    }

    @Test
    fun `given product variation selected from filter section, then track correct source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = ProductSelectorViewModel(
                navArgs,
                currencyFormatter,
                wooCommerceStore,
                orderStore,
                selectedSite,
                listHandler,
                variationSelectorRepository,
                resourceProvider,
                productsMapper,
                analyticsTracksWrapper,
            )
            sut.onSelectedVariationsUpdated(
                VariationSelectorViewModel.VariationSelectionResult(
                    productId = 0L,
                    selectedVariationIds = setOf(1L),
                    productSourceForTracking = ProductSourceForTracking.FILTER
                )
            )
            sut.onDoneButtonClick()

            verify(analyticsTracksWrapper).track(
                PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.FILTER
                    )
                )
            )
        }
    }
    //endregion

    private fun generateProductListItem(
        id: Long,
    ) = ProductSelectorViewModel.ProductListItem(
        id = id,
        title = "",
        type = ProductType.SIMPLE,
        imageUrl = null,
        numVariations = 0,
        stockAndPrice = null,
        sku = null,
        selectedVariationIds = emptySet(),
        selectionState = SelectionState.SELECTED
    )

    private fun generateLineItems(
        name: String,
        productId: String,
    ): String {
        return "[{\"id\":1121,\"meta_data\":[],\"name\":\"$name\",\"price\":\"88.6\"," +
            "\"product_id\":$productId,\"quantity\":1.0,\"sku\":\"ACBI\"," +
            "\"subtotal\":\"88.60\",\"total\":\"88.60\",\"total_tax\":\"0.00\",\"variation_id\":0}]"
    }

    private fun generateTestOrders() = listOf(
        OrderTestUtils.generateOrder(
            lineItems = generateLineItems(
                name = "ACME Bike",
                productId = "2444"
            )
        ),
        OrderTestUtils.generateOrder(
            lineItems = generateLineItems(
                name = "Variation Product",
                productId = "2446"
            )
        ),
        OrderTestUtils.generateOrder(
            lineItems = generateLineItems(
                name = "Lorry",
                productId = "2449"
            )
        ),
        OrderTestUtils.generateOrder(
            lineItems = generateLineItems(
                name = "Bus",
                productId = "2450"
            )
        ),
        OrderTestUtils.generateOrder(
            lineItems = generateLineItems(
                name = "TVS",
                productId = "2451"
            )
        ),
    )

    private fun generatePopularOrders(): MutableList<OrderEntity> {
        val ordersList = mutableListOf<OrderEntity>()
        repeat(4) {
            ordersList.add(
                OrderTestUtils.generateOrder(
                    lineItems = generateLineItems(
                        name = "Bicycle",
                        productId = "2445"
                    )
                )
            )
        }
        repeat(3) {
            ordersList.add(
                OrderTestUtils.generateOrder(
                    lineItems = generateLineItems(
                        name = "Toys",
                        productId = "2448"
                    )
                )
            )
        }
        repeat(2) {
            ordersList.add(
                OrderTestUtils.generateOrder(
                    lineItems = generateLineItems(
                        name = "Car",
                        productId = "2447"
                    )
                )
            )
        }
        return ordersList
    }
}
