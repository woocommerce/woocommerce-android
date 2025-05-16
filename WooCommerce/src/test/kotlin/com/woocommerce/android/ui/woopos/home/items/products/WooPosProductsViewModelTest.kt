package com.woocommerce.android.ui.woopos.home.items.products

import app.cash.turbine.test
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosContentViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosProductsViewState
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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

class WooPosProductsViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val priceFormat: WooPosFormatPrice = mock {
        onBlocking { invoke(BigDecimal("10.0")) }.thenReturn("$10.0")
        onBlocking { invoke(BigDecimal("20.0")) }.thenReturn("$20.0")
    }
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val productsDataSource: WooPosProductsDataSource = mock()
    private val wooPosItemsNavigator: WooPosItemsNavigator = mock()

    @Before
    fun setup() {
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple",
                isDownloadable = false,
            ),
        )

        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
    }

    @Test
    fun `given products from data source, when view model created, then view state updated correctly`() = runTest {
        // WHEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple",
                isDownloadable = false,
            ),
            ProductTestUtils.generateProduct(
                productId = 2,
                productName = "Product 2",
                amount = "20.0",
                productType = "simple",
                isDownloadable = false,
            ).copy(firstImageUrl = "https://test.com")
        )

        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val value = awaitItem() as WooPosProductsViewState.Content

            @Suppress("UNCHECKED_CAST")
            val items = value.items as List<WooPosItemSelectionViewState.Product.Simple>
            assertThat(items).hasSize(2)
            assertThat(items[0].id).isEqualTo(1)
            assertThat(items[0].name).isEqualTo("Product 1")
            assertThat(items[0].price).isEqualTo("$10.0")
            assertThat(items[1].id).isEqualTo(2)
            assertThat(items[1].name).isEqualTo("Product 2")
            assertThat(items[1].price).isEqualTo("$20.0")
            assertThat(items[1].imageUrl).isEqualTo("https://test.com")
        }
    }

    @Test
    fun `given empty products list returned, when view model created, then view state is empty`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(emptyList())
                )
            )
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isEqualTo(
                WooPosProductsViewState.Empty()
            )
        }
    }

    @Test
    fun `given loading products fails, when view model created, then view state is error`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.failure(Exception())
                )
            )
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isEqualTo(
                WooPosProductsViewState.Error()
            )
        }
    }

    @Test
    fun `given products from data source, when pulled to refresh, then should remove products and fetch again`() =
        runTest {
            // WHEN
            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)

            // THEN
            viewModel.viewState.test {
                verify(productsDataSource).loadProducts(forceRefreshProducts = true)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `given content state, when end of products grid reached and no more pages, then do not load more`() = runTest {
        // GIVEN
        whenever(productsDataSource.hasMorePages).thenReturn(false)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.EndOfItemsListReached)

        // THEN
        viewModel.viewState.test {
            val value = awaitItem() as WooPosProductsViewState.Content
            assertThat(value.paginationState).isEqualTo(WooPosPaginationState.None)
        }
    }

    @Test
    fun `when product clicked, then send event to parent`() = runTest {
        // GIVEN
        val product = WooPosItemSelectionViewState.Product.Simple(id = 1, name = "", price = "", imageUrl = null)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.ItemClicked(product))

        // THEN
        val item = WooPosItemsViewModel.ItemClickedData.Product.Simple(
            id = product.id
        )
        viewModel.viewState.test {
            verify(fromChildToParentEventSender).sendToParent(
                eq(
                    ChildToParentEvent.ItemClickedInProductSelector(
                        itemData = item,
                        eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                            item = item,
                            source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                            sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
                        )
                    )
                )
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given load more products is called, when products source loads successfully then state is updated`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUIEvent(WooPosProductsUIEvent.EndOfItemsListReached)

            // THEN
            viewModel.viewState.test {
                val value = awaitItem() as WooPosProductsViewState.Content
                assertThat(value.paginationState).isEqualTo(WooPosPaginationState.None)
            }
        }

    @Test
    fun `when loading without pull to refresh, then should not ask to remove products`() = runTest {
        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            verify(productsDataSource).loadProducts(forceRefreshProducts = false)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `when vm created and loading products, then view state is Loading`() = runTest {
        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.viewState.value).isInstanceOf(WooPosProductsViewState.Loading::class.java)
    }

    @Test
    fun `given error from load more, when list end reached, then state is pagination error`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadMore()).thenReturn(Result.failure(Exception()))
        whenever(productsDataSource.hasMorePages).thenReturn(true)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.EndOfItemsListReached)

        // THEN
        viewModel.viewState.test {
            val value = awaitItem() as WooPosContentViewState
            assertThat(value.paginationState).isInstanceOf(WooPosPaginationState.Error::class.java)
        }
    }

    @Test
    fun `given no products, when pull to refresh, then state is Empty`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(emptyList())
                )
            )
        )

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosProductsViewState.Empty::class.java)
        }
    }

    @Test
    fun `when pull to refresh, then should track event`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)

        // THEN
        verify(analyticsTracker).track(
            eq(
                WooPosAnalyticsEvent.Event.PullToRefreshTriggered(
                    source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                    sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
                )
            )
        )
    }

    @Test
    fun `given variable product, when clicked on it, then trigger proper event`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "variable",
                isVariable = true
            )
        )
        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(
            WooPosProductsUIEvent.ItemClicked(
                WooPosItemSelectionViewState.Product.Variable(
                    id = 1L,
                    name = "Product 1",
                    numOfVariations = 10,
                    variationIds = emptyList(),
                    price = "$10.0",
                    imageUrl = null
                )
            )
        )

        // THEN
        verify(wooPosItemsNavigator).sendNavigationEvent(
            WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen(
                WooPosItemNavigationData.VariableProductData(
                    id = 1,
                    name = "Product 1",
                    numOfVariations = 10,
                    sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST,
                )
            )
        )
    }

    @Test
    fun `given variable products from data source, when view model created, then items list updated correctly`() =
        runTest {
            // GIVEN
            val products = listOf(
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Product 1",
                    amount = "10.0",
                    productType = "simple",
                    isDownloadable = false,
                ),
                ProductTestUtils.generateProduct(
                    productId = 2,
                    productName = "Product 2",
                    amount = "20.0",
                    productType = "variable",
                    isDownloadable = false,
                    isVariable = true
                ).copy(firstImageUrl = "https://test.com")
            )

            whenever(productsDataSource.loadProducts(any())).thenReturn(
                flowOf(
                    WooPosProductsDataSource.ProductsResult.Remote(
                        Result.success(products)
                    )
                )
            )

            // WHEN
            val viewModel = createViewModel()
            viewModel.viewState.test {
                // THEN
                val value = awaitItem() as WooPosProductsViewState.Content

                assertThat(
                    value.items.filterIsInstance<WooPosItemSelectionViewState.Product.Variable>().size
                ).isEqualTo(1)
            }
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `given initial load in progress, when end of list reached, then pagination state set to loading and queue load more`() =
        runTest {
            // GIVEN
            whenever(productsDataSource.hasMorePages).thenReturn(true)
            whenever(productsDataSource.loadMore()).thenReturn(
                Result.success(
                    listOf(
                        ProductTestUtils.generateProduct(
                            productId = 2,
                            productName = "Product 2",
                            amount = "20.0",
                            productType = "simple"
                        )
                    )
                )
            )

            val productsFlow = flow {
                emit(
                    WooPosProductsDataSource.ProductsResult.Remote(
                        Result.success(
                            listOf(
                                ProductTestUtils.generateProduct(
                                    productId = 1,
                                    productName = "Product 1",
                                    amount = "10.0",
                                    productType = "simple"
                                )
                            )
                        )
                    )
                )
            }

            whenever(productsDataSource.loadProducts(any())).thenReturn(productsFlow)

            val viewModel = createViewModel()

            // WHEN
            viewModel.onUIEvent(WooPosProductsUIEvent.EndOfItemsListReached)
            advanceUntilIdle()

            // THEN
            verify(productsDataSource, times(1)).loadMore()
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `given load more queued, when initial load fails, then load more is not triggered`() = runTest {
        // GIVEN
        whenever(productsDataSource.hasMorePages).thenReturn(true)

        val productsFlow = MutableSharedFlow<WooPosProductsDataSource.ProductsResult>()
        whenever(productsDataSource.loadProducts(any())).thenReturn(productsFlow)

        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.EndOfItemsListReached)

        // Then
        productsFlow.emit(
            WooPosProductsDataSource.ProductsResult.Remote(
                Result.failure(Exception("Test error"))
            )
        )
        advanceUntilIdle()

        // THEN
        verify(productsDataSource, never()).loadMore()
    }

    @Test
    fun `when variable product is clicked from product list, then navigation event uses product list source`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()
            val item = WooPosItemSelectionViewState.Product.Variable(
                id = 1,
                name = "Product",
                price = "$10",
                imageUrl = null,
                numOfVariations = 2,
                variationIds = emptyList()
            )

            // WHEN
            viewModel.onUIEvent(WooPosProductsUIEvent.ItemClicked(item))

            // THEN
            verify(wooPosItemsNavigator).sendNavigationEvent(
                WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen(
                    WooPosItemNavigationData.VariableProductData(
                        id = 1L,
                        name = "Product",
                        numOfVariations = 2,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST,
                    )
                )
            )
        }

    private fun createViewModel(): WooPosProductsViewModel {
        return WooPosProductsViewModel(
            productsDataSource = productsDataSource,
            priceFormat = priceFormat,
            analyticsTracker = analyticsTracker,
            fromChildToParentEventSender = fromChildToParentEventSender,
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            navigator = wooPosItemsNavigator,
        )
    }
}
