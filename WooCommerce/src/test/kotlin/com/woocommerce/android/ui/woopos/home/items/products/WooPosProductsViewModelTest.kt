package com.woocommerce.android.ui.woopos.home.items.products

import app.cash.turbine.test
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.items.WooPosContentViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState.Product
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ProductsPullToRefreshTriggered
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class WooPosProductsViewModelTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

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
            val value = awaitItem() as WooPosItemsViewState.Content

            @Suppress("UNCHECKED_CAST")
            val items = value.items as List<Product.Simple>
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
                WooPosItemsViewState.Empty(tabs)
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
                WooPosItemsViewState.Error(
                    tabs = tabs
                )
            )
        }
    }

    @Test
    fun `given products from data source, when pulled to refresh, then should remove products and fetch again`() =
        runTest {
            // WHEN
            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosItemsUIEvent.PullToRefreshTriggered)

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
        viewModel.onUIEvent(WooPosItemsUIEvent.EndOfItemsListReached)

        // THEN
        viewModel.viewState.test {
            val value = awaitItem() as WooPosItemsViewState.Content
            assertThat(value.paginationState).isEqualTo(WooPosPaginationState.None)
        }
    }

    @Test
    fun `when product clicked, then send event to parent`() = runTest {
        // GIVEN
        val product = Product.Simple(id = 1, name = "", price = "", imageUrl = null)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.ItemClicked(product))

        // THEN
        viewModel.viewState.test {
            verify(fromChildToParentEventSender).sendToParent(
                ChildToParentEvent.ItemClickedInProductSelector(
                    WooPosItemsViewModel.ItemClickedData.Product.Simple(
                        id = product.id
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
            viewModel.onUIEvent(WooPosItemsUIEvent.EndOfItemsListReached)

            // THEN
            viewModel.viewState.test {
                val value = awaitItem() as WooPosItemsViewState.Content
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
        assertThat(viewModel.viewState.value).isInstanceOf(WooPosItemsViewState.Loading::class.java)
    }

    @Test
    fun `given error from load more, when list end reached, then state is pagination error`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadMore()).thenReturn(Result.failure(Exception()))
        whenever(productsDataSource.hasMorePages).thenReturn(true)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.EndOfItemsListReached)

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
        viewModel.onUIEvent(WooPosItemsUIEvent.PullToRefreshTriggered)

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosItemsViewState.Empty::class.java)
        }
    }

    @Test
    fun `given empty list, when pull to refresh, then parent notified correctly`() = runTest {
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
        viewModel.onUIEvent(WooPosItemsUIEvent.PullToRefreshTriggered)

        // THEN
        viewModel.viewState.test {
            verify(fromChildToParentEventSender).sendToParent(ChildToParentEvent.ProductsStatusChanged.FullScreen)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given products, when pull to refresh, then parent notified correctly`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.PullToRefreshTriggered)

        // THEN
        viewModel.viewState.test {
            verify(fromChildToParentEventSender).sendToParent(ChildToParentEvent.ProductsStatusChanged.WithCart)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `when pull to refresh, then should track event`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.PullToRefreshTriggered)

        // THEN
        verify(analyticsTracker).track(ProductsPullToRefreshTriggered)
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
            WooPosItemsUIEvent.ItemClicked(
                Product.Variable(
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
                val value = awaitItem() as WooPosItemsViewState.Content

                assertThat(value.items.filterIsInstance<Product.Variable>().size).isEqualTo(1)
            }
        }
}
