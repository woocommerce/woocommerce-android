package com.woocommerce.android.ui.products.selector

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_SELECTOR_FILTER_STATUS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_SELECTOR_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.products.ProductNavigationTarget
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ProductListItem
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
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
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
        private val VARIABLE_SUBSCRIPTION_PRODUCT = ProductTestUtils.generateProduct(
            productId = 4L,
            isVariable = true,
            productType = "variable-subscription",
            variationIds = "[1,2]",
        )
        private val VARIABLE_PRODUCT = ProductTestUtils.generateProduct(
            productId = 5L,
            isVariable = true,
            variationIds = "[1,2]",
        )
    }

    private val currencyFormatter: CurrencyFormatter = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val listHandler: ProductListHandler = mock {
        on { productsFlow } doReturn flowOf(
            listOf(
                VALID_PRODUCT,
                DRAFT_PRODUCT,
                VARIABLE_PRODUCT_WITH_NO_VARIATIONS,
                VARIABLE_SUBSCRIPTION_PRODUCT,
                VARIABLE_PRODUCT
            )
        )
    }
    private val variationSelectorRepository: VariationSelectorRepository = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()
    private val productSelectorTracker: ProductSelectorTracker = ProductSelectorTracker(tracker)
    private val orderStore: WCOrderStore = mock()
    private val productsMapper: ProductsMapper = mock()
    private val siteSettings: WCSettingsModel = mock()

    @Before
    fun setup() {
        val site: SiteModel = mock()
        whenever(selectedSite.get()).thenReturn(site)
        whenever(siteSettings.currencyCode).thenReturn("USD")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)
    }

    @Test
    fun `given published products restriction, when view model created, should not show draft products`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = arrayOf(OnlyPublishedProducts),
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.Undefined,
        ).initSavedStateHandle()

        val sut = createViewModel(navArgs)

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
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.Undefined,
        ).initSavedStateHandle()

        val sut = createViewModel(navArgs)

        sut.viewState.observeForever { state ->
            assertThat(state.products).isNotEmpty
            assertThat(
                state.products.filter {
                    (it.type == ProductType.VARIABLE || it.type == ProductType.VARIABLE_SUBSCRIPTION) &&
                        it.numVariations == 0
                }
            ).isEmpty()
        }
    }

    @Test
    fun `given multiple restrictions, when view model created, should show correct products`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = arrayOf(OnlyPublishedProducts, NoVariableProductsWithNoVariations),
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.Undefined,
        ).initSavedStateHandle()

        val sut = createViewModel(navArgs)

        sut.viewState.observeForever { state ->
            assertThat(state.products).isNotEmpty
            assertThat(
                state.products.filter {
                    (it.type == ProductType.VARIABLE || it.type == ProductType.VARIABLE_SUBSCRIPTION) &&
                        it.numVariations == 0
                }
            ).isEmpty()
            assertThat(state.products.filter { it.id == DRAFT_PRODUCT.remoteId }).isEmpty()
        }
    }

    @Test
    fun `given variable product, when view model created, should generate correct item subtitle`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = arrayOf(NoVariableProductsWithNoVariations),
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.Undefined,
        ).initSavedStateHandle()

        whenever(currencyFormatter.formatCurrency(VARIABLE_PRODUCT.price!!, "USD"))
            .thenReturn("$${VARIABLE_PRODUCT.price}")

        val sut = createViewModel(navArgs)

        sut.viewState.observeForever { state ->
            assertThat(state.products).isNotEmpty
            state.products.firstOrNull { it.id == VARIABLE_PRODUCT.remoteId }.apply {
                assertThat(this).isNotNull
                this!!
                assertThat(stockAndPrice).isEqualTo("In stock • %d variations • $${VARIABLE_PRODUCT.price}")
            }
        }
    }

    @Test
    fun `given variable subscription product, when view model created, should generate correct item subtitle`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = arrayOf(NoVariableProductsWithNoVariations),
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.Undefined,
        ).initSavedStateHandle()

        whenever(currencyFormatter.formatCurrency(VARIABLE_SUBSCRIPTION_PRODUCT.price!!, "USD"))
            .thenReturn("$${VARIABLE_SUBSCRIPTION_PRODUCT.price}")

        val sut = createViewModel(navArgs)

        sut.viewState.observeForever { state ->
            assertThat(state.products).isNotEmpty
            state.products.firstOrNull { it.id == VARIABLE_SUBSCRIPTION_PRODUCT.remoteId }.apply {
                assertThat(this).isNotNull
                this!!
                assertThat(stockAndPrice).isEqualTo("In stock • $${VARIABLE_SUBSCRIPTION_PRODUCT.price}")
            }
        }
    }

    @Test
    fun `given non-variable product, when view model created, should generate correct item subtitle`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = arrayOf(NoVariableProductsWithNoVariations),
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.Undefined,
        ).initSavedStateHandle()

        whenever(currencyFormatter.formatCurrency(VALID_PRODUCT.price!!, "USD")).thenReturn("$${VALID_PRODUCT.price}")

        val sut = createViewModel(navArgs)

        sut.viewState.observeForever { state ->
            assertThat(state.products).isNotEmpty
            state.products.firstOrNull { it.id == VALID_PRODUCT.remoteId }.apply {
                assertThat(this).isNotNull
                this!!
                assertThat(stockAndPrice).isEqualTo("In stock • $${VALID_PRODUCT.price}")
            }
        }
    }

    @Test
    fun `given no restrictions, when view model created, should show all products`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = emptyArray(),
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.Undefined,
        ).initSavedStateHandle()

        val sut = createViewModel(navArgs)

        sut.viewState.observeForever { state ->
            assertThat(state.products.count()).isEqualTo(3)
            assertThat(
                state.products.map { it.id }
            ).containsAll(
                listOf(VALID_PRODUCT.remoteId, DRAFT_PRODUCT.remoteId, VARIABLE_PRODUCT_WITH_NO_VARIATIONS.remoteId)
            )
        }
    }

    @Test
    fun `given order creation flow, when item is selected, should track analytic event`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = emptyArray(),
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
        ).initSavedStateHandle()

        val sut = createViewModel(navArgs)

        sut.onProductClick(
            item = ProductListItem(1, "", ProductType.SIMPLE, numVariations = 0),
            productSourceForTracking = ProductSourceForTracking.ALPHABETICAL,
        )
        verify(tracker).track(AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_ITEM_SELECTED)
    }

    @Test
    fun `given order creation flow, when item is unselected, should track analytic event`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = emptyArray(),
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
        ).initSavedStateHandle()

        val sut = createViewModel(navArgs)
        val listItem = ProductListItem(1, "", ProductType.SIMPLE, numVariations = 0)
        sut.onProductClick(listItem, ProductSourceForTracking.ALPHABETICAL) // select
        sut.onProductClick(listItem, ProductSourceForTracking.ALPHABETICAL) // unselect

        verify(tracker).track(AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_ITEM_UNSELECTED)
    }

    @Test
    fun `given order creation flow and no items selected, when done button is tapped, should track analytics event`() =
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = emptyArray(),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()

            val sut = createViewModel(navArgs)
            sut.onDoneButtonClick()

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    "product_count" to 0,
                    KEY_PRODUCT_SELECTOR_SOURCE to emptyList<ProductSourceForTracking>(),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to false,
                )
            )
        }

    @Test
    fun `given order creation flow and multiple items selected, when done button is tapped, should track analytics event`() =
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = emptyArray(),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()

            val sut = createViewModel(navArgs)
            sut.onProductClick(
                item = ProductListItem(1, "", ProductType.SIMPLE, numVariations = 0),
                productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
            )
            sut.onProductClick(
                item = ProductListItem(2, "", ProductType.SIMPLE, numVariations = 0),
                productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
            )
            sut.onDoneButtonClick()

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    "product_count" to 2,
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.ALPHABETICAL.name,
                        ProductSourceForTracking.ALPHABETICAL.name
                    ),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to false,
                )
            )
        }

    @Test
    fun `given order creation flow, when clear button is tapped, should track analytics event`() = testBlocking {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = emptyArray(),
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
        ).initSavedStateHandle()

        val sut = createViewModel(navArgs)
        sut.onClearButtonClick()

        verify(tracker).track(
            AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_CLEAR_SELECTION_BUTTON_TAPPED,
            mapOf("source" to "product_selector")
        )
    }

    @Test
    fun `when variable product is tapped, should redirect to variation picker`() = testBlocking {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = emptyArray(),
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
        ).initSavedStateHandle()
        val popularOrdersList = generatePopularOrders()
        val ordersList = generateTestOrders()
        val totalOrders = ordersList + popularOrdersList
        whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)

        val sut = createViewModel(navArgs)
        sut.onProductClick(
            item = ProductListItem(1, "", ProductType.VARIABLE, numVariations = 2),
            productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
        )

        assertThat(sut.event.value).isEqualTo(
            ProductNavigationTarget.NavigateToVariationSelector(
                productId = 1,
                selectedVariationIds = emptySet(),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
                productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
            )
        )
    }

    @Test
    fun `when variable subscription is tapped, should redirect to variation picker`() = testBlocking {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = emptyArray(),
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
        ).initSavedStateHandle()
        val popularOrdersList = generatePopularOrders()
        val ordersList = generateTestOrders()
        val totalOrders = ordersList + popularOrdersList
        whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)

        val sut = createViewModel(navArgs)
        sut.onProductClick(
            item = ProductListItem(23, "", ProductType.VARIABLE_SUBSCRIPTION, numVariations = 2),
            productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
        )

        assertThat(sut.event.value).isEqualTo(
            ProductNavigationTarget.NavigateToVariationSelector(
                productId = 23,
                selectedVariationIds = emptySet(),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
                productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
            )
        )
    }

    // region Sort by popularity and recently sold products

    @Test
    fun `given published products restriction, when view model created, should not show draft products in the popular section`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.Undefined,
            ).initSavedStateHandle()
            val popularOrdersList = generatePopularOrders()
            val ordersList = generateTestOrders()
            val totalOrders = ordersList + popularOrdersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(
                listOf(
                    DRAFT_PRODUCT,
                    DRAFT_PRODUCT,
                    VALID_PRODUCT
                )
            )

            val sut = createViewModel(navArgs)

            var viewState: ProductSelectorViewModel.ViewState? = null
            sut.viewState.observeForever { state ->
                viewState = state
            }
            assertThat(viewState?.popularProducts).isNotEmpty
            assertThat(viewState?.popularProducts?.filter { it.id == DRAFT_PRODUCT.remoteId }).isEmpty()
        }
    }

    @Test
    fun `given published products restriction, when view model created, should not show draft products in the last sold section`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.Undefined,
            ).initSavedStateHandle()
            val ordersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(ordersList)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(
                listOf(
                    DRAFT_PRODUCT,
                    DRAFT_PRODUCT,
                    VALID_PRODUCT
                )
            )

            val sut = createViewModel(navArgs)

            var viewState: ProductSelectorViewModel.ViewState? = null
            sut.viewState.observeForever { state ->
                viewState = state
            }
            assertThat(viewState?.recentProducts).isNotEmpty
            assertThat(viewState?.recentProducts?.filter { it.id == DRAFT_PRODUCT.remoteId }).isEmpty()
        }
    }

    @Test
    fun `given popular products, when view model created, then verify popular products are sorted in descending order`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val popularOrdersList = generatePopularOrders()
            val ordersList = generateTestOrders()
            val totalOrders = ordersList + popularOrdersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)
            val argumentCaptor = argumentCaptor<List<Long>>()

            createViewModel(navArgs)

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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
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
            val totalOrders = ordersList + popularOrdersList + popularOrdersThatAreNotPaidYet
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)
            val argumentCaptor = argumentCaptor<List<Long>>()

            createViewModel(navArgs)

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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val popularOrdersList = generatePopularOrders()
            val ordersList = generateTestOrders()
            val totalOrders = popularOrdersList + ordersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(ProductTestUtils.generateProductList())

            val sut = createViewModel(navArgs)
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val popularOrdersList = generatePopularOrders()
            val ordersList = generateTestOrders()
            val totalOrders = popularOrdersList + ordersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(ProductTestUtils.generateProductList())

            val sut = createViewModel(navArgs)
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val ordersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(ordersList)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(ProductTestUtils.generateProductList())

            val sut = createViewModel(navArgs)
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val ordersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(ordersList)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(ProductTestUtils.generateProductList())

            val sut = createViewModel(navArgs)
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)
            val argumentCaptor = argumentCaptor<List<Long>>()

            createViewModel(navArgs)

            verify(productsMapper, times(2)).mapProductIdsToProduct(argumentCaptor.capture())
            assertThat(argumentCaptor.secondValue).isEqualTo(
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
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

            createViewModel(navArgs)

            verify(productsMapper, times(2)).mapProductIdsToProduct(argumentCaptor.capture())
            assertThat(argumentCaptor.secondValue).isEqualTo(
                listOf(2444L, 2446L, 2449L, 2450L, 2451L)
            )
        }
    }

    @Test
    fun `given order creation, when multiple same products purchased, then only display 1 of them in the recent products section `() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val recentOrdersList = mutableListOf<OrderEntity>()
            repeat(10) {
                recentOrdersList.add(
                    OrderTestUtils.generateOrder(
                        lineItems = generateLineItems(
                            name = "ACME Bike",
                            productId = "1111"
                        ),
                    ),
                )
            }
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)
            val argumentCaptor = argumentCaptor<List<Long>>()

            createViewModel(navArgs)

            verify(productsMapper, times(2)).mapProductIdsToProduct(argumentCaptor.capture())
            assertThat(argumentCaptor.secondValue).isEqualTo(
                listOf(1111L)
            )
        }
    }

    @Test
    fun `given popular products, when filter is applied, then hide the popular products section`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val ordersList = generateTestOrders() + generatePopularOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(ordersList)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(ProductTestUtils.generateProductList())

            val sut = createViewModel(navArgs)
            sut.onFiltersChanged(
                stockStatus = "In stock",
                productCategory = null,
                productStatus = null,
                productType = null,
                productCategoryName = null
            )

            var viewState: ProductSelectorViewModel.ViewState? = null
            sut.viewState.observeForever { state ->
                viewState = state
            }

            assertThat(viewState?.popularProducts)?.isEmpty()
        }
    }

    @Test
    fun `given popular products, when filter is cleared, then show the popular products section`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val ordersList = generateTestOrders() + generatePopularOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(ordersList)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(ProductTestUtils.generateProductList())

            val sut = createViewModel(navArgs)
            sut.onFiltersChanged(
                stockStatus = "In stock",
                productCategory = null,
                productStatus = null,
                productType = null,
                productCategoryName = null
            )

            var viewState: ProductSelectorViewModel.ViewState? = null
            sut.viewState.observeForever { state ->
                viewState = state
            }

            assertThat(viewState?.popularProducts)?.isEmpty()
            sut.onFiltersChanged(
                stockStatus = null,
                productCategory = null,
                productStatus = null,
                productType = null,
                productCategoryName = null
            )
            assertThat(viewState?.popularProducts)?.isNotEmpty
        }
    }

    @Test
    fun `given last sold products, when filter is applied, then hide the last sold products section`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val ordersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(ordersList)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(ProductTestUtils.generateProductList())

            val sut = createViewModel(navArgs)
            sut.onFiltersChanged(
                stockStatus = "In stock",
                productCategory = null,
                productStatus = null,
                productType = null,
                productCategoryName = null
            )

            var viewState: ProductSelectorViewModel.ViewState? = null
            sut.viewState.observeForever { state ->
                viewState = state
            }

            assertThat(viewState?.recentProducts)?.isEmpty()
        }
    }

    @Test
    fun `given last sold products, when filter is cleared, then show the last sold products section`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val ordersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(ordersList)
            whenever(productsMapper.mapProductIdsToProduct(any())).thenReturn(ProductTestUtils.generateProductList())

            val sut = createViewModel(navArgs)
            sut.onFiltersChanged(
                stockStatus = "In stock",
                productCategory = null,
                productStatus = null,
                productType = null,
                productCategoryName = null
            )

            var viewState: ProductSelectorViewModel.ViewState? = null
            sut.viewState.observeForever { state ->
                viewState = state
            }

            assertThat(viewState?.recentProducts)?.isEmpty()
            sut.onFiltersChanged(
                stockStatus = null,
                productCategory = null,
                productStatus = null,
                productType = null,
                productCategoryName = null
            )
            assertThat(viewState?.recentProducts)?.isNotEmpty
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val ordersThatAreNotPaidYet = mutableListOf<OrderEntity>()
            val recentOrdersList = generateTestOrders()
            val totalOrders = ordersThatAreNotPaidYet + recentOrdersList
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(totalOrders)

            val sut = createViewModel(navArgs)
            sut.onProductClick(
                item = generateProductListItem(0L),
                productSourceForTracking = ProductSourceForTracking.POPULAR
            )
            sut.onDoneButtonClick()

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_COUNT to 1,
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(ProductSourceForTracking.POPULAR.name),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to false,
                )
            )
        }
    }

    @Test
    fun `given product selected from recent and popular section, when done button clicked, then track correct source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = createViewModel(navArgs)
            sut.onProductClick(
                item = generateProductListItem(id = 0L),
                productSourceForTracking = ProductSourceForTracking.LAST_SOLD
            )
            sut.onProductClick(
                item = generateProductListItem(id = 1L),
                productSourceForTracking = ProductSourceForTracking.POPULAR
            )
            sut.onDoneButtonClick()

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_COUNT to 2,
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.LAST_SOLD.name,
                        ProductSourceForTracking.POPULAR.name
                    ),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to false,
                )
            )
        }
    }

    @Test
    fun `given products selected from recent, alphabetical and popular, when done button clicked, then track correct source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = createViewModel(navArgs)
            sut.onProductClick(
                item = generateProductListItem(id = 0L),
                productSourceForTracking = ProductSourceForTracking.LAST_SOLD
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

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_COUNT to 3,
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.LAST_SOLD.name,
                        ProductSourceForTracking.ALPHABETICAL.name,
                        ProductSourceForTracking.POPULAR.name
                    ),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to false,
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = createViewModel(navArgs)
            sut.onSearchQueryChanged("Test")
            sut.onProductClick(
                item = generateProductListItem(id = 0L),
                productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
            )
            sut.onDoneButtonClick()

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_COUNT to 1,
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.SEARCH.name
                    ),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to false,
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = createViewModel(navArgs)
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

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_COUNT to 1,
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.ALPHABETICAL.name
                    ),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to true,
                )
            )
        }
    }

    @Test
    fun `given product variation detail screen entered but not selected, then do not track source`() {
        testBlocking {
            val navArgs = ProductSelectorFragmentArgs(
                selectedItems = emptyArray(),
                restrictions = arrayOf(OnlyPublishedProducts),
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = createViewModel(navArgs)
            sut.onSelectedVariationsUpdated(
                VariationSelectorViewModel.VariationSelectionResult(
                    productId = 0L,
                    selectedVariationIds = emptySet(),
                    productSourceForTracking = ProductSourceForTracking.POPULAR
                )
            )
            sut.onDoneButtonClick()

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_COUNT to 0,
                    KEY_PRODUCT_SELECTOR_SOURCE to emptyList<String>(),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to false,
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = createViewModel(navArgs)
            sut.onSelectedVariationsUpdated(
                VariationSelectorViewModel.VariationSelectionResult(
                    productId = 0L,
                    selectedVariationIds = setOf(1L),
                    productSourceForTracking = ProductSourceForTracking.POPULAR
                )
            )
            sut.onDoneButtonClick()

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_COUNT to 1,
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.POPULAR.name
                    ),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to false,
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = createViewModel(navArgs)
            sut.onSelectedVariationsUpdated(
                VariationSelectorViewModel.VariationSelectionResult(
                    productId = 0L,
                    selectedVariationIds = setOf(1L),
                    productSourceForTracking = ProductSourceForTracking.LAST_SOLD
                )
            )
            sut.onDoneButtonClick()

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_COUNT to 1,
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.LAST_SOLD.name
                    ),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to false,
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = createViewModel(navArgs)
            sut.onSelectedVariationsUpdated(
                VariationSelectorViewModel.VariationSelectionResult(
                    productId = 0L,
                    selectedVariationIds = setOf(1L),
                    productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
                )
            )
            sut.onDoneButtonClick()

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_COUNT to 1,
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.ALPHABETICAL.name
                    ),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to false,
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = createViewModel(navArgs)
            sut.onSelectedVariationsUpdated(
                VariationSelectorViewModel.VariationSelectionResult(
                    productId = 0L,
                    selectedVariationIds = setOf(1L),
                    productSourceForTracking = ProductSourceForTracking.SEARCH
                )
            )
            sut.onDoneButtonClick()

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_COUNT to 1,
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.SEARCH.name
                    ),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to false,
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
                productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ).initSavedStateHandle()
            val recentOrdersList = generateTestOrders()
            whenever(orderStore.getPaidOrdersForSiteDesc(selectedSite.get())).thenReturn(recentOrdersList)

            val sut = createViewModel(navArgs)
            sut.onFiltersChanged(
                stockStatus = "In stock",
                productStatus = null,
                productType = null,
                productCategory = null,
                productCategoryName = null
            )
            sut.onSelectedVariationsUpdated(
                VariationSelectorViewModel.VariationSelectionResult(
                    productId = 0L,
                    selectedVariationIds = setOf(1L),
                    productSourceForTracking = ProductSourceForTracking.ALPHABETICAL
                )
            )
            sut.onDoneButtonClick()

            verify(tracker).track(
                ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                mapOf(
                    KEY_PRODUCT_COUNT to 1,
                    KEY_PRODUCT_SELECTOR_SOURCE to listOf(
                        ProductSourceForTracking.ALPHABETICAL.name
                    ),
                    KEY_PRODUCT_SELECTOR_FILTER_STATUS to true,
                )
            )
        }
    }
    //endregion

    private fun generateProductListItem(
        id: Long,
    ) = ProductListItem(
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

    private fun createViewModel(navArgs: SavedStateHandle) =
        ProductSelectorViewModel(
            navArgs,
            currencyFormatter,
            wooCommerceStore,
            orderStore,
            selectedSite,
            listHandler,
            variationSelectorRepository,
            resourceProvider,
            productSelectorTracker,
            productsMapper,
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
