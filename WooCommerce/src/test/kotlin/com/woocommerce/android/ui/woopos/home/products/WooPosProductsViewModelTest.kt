package com.woocommerce.android.ui.woopos.home.products

import app.cash.turbine.test
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosProductsViewModelTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val productsDataSource: WooPosProductsDataSource = mock()
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val priceFormat: WooPosFormatPrice = mock {
        onBlocking { invoke(BigDecimal("10.0")) }.thenReturn("$10.0")
        onBlocking { invoke(BigDecimal("20.0")) }.thenReturn("$20.0")
    }

    @Test
    fun `given products from data source, when view model created, then view state updated correctly`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple"
            ),
            ProductTestUtils.generateProduct(
                productId = 2,
                productName = "Product 2",
                amount = "20.0",
                productType = "simple"
            ).copy(firstImageUrl = "https://test.com")
        )

        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
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
            assertThat(value.products).hasSize(2)
            assertThat(value.products[0].id).isEqualTo(1)
            assertThat(value.products[0].name).isEqualTo("Product 1")
            assertThat(value.products[0].price).isEqualTo("$10.0")
            assertThat(value.products[1].id).isEqualTo(2)
            assertThat(value.products[1].name).isEqualTo("Product 2")
            assertThat(value.products[1].price).isEqualTo("$20.0")
            assertThat(value.products[1].imageUrl).isEqualTo("https://test.com")
        }
    }

    @Test
    fun `given empty products list returned, when view model created, then view state is empty`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(emptyList())
                )
            )
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.viewState.test {
            // THEN
            val value = awaitItem()
            assertThat(value).isEqualTo(WooPosProductsViewState.Empty())
        }
    }

    @Test
    fun `given loading products is failure, when view model created, then view state is error`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.failure(Exception())
                )
            )
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.viewState.test {
            // THEN
            val value = awaitItem()
            assertThat(value).isEqualTo(WooPosProductsViewState.Error())
        }
    }

    @Test
    fun `given products from data source, when pulled to refresh, then should remove products and fetch again`() =
        runTest {
            // GIVEN
            val products = listOf(
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Product 1",
                    amount = "10.0",
                    productType = "simple"
                ),
                ProductTestUtils.generateProduct(
                    productId = 2,
                    productName = "Product 2",
                    amount = "20.0",
                    productType = "simple"
                ).copy(firstImageUrl = "https://test.com")
            )

            whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
                flowOf(
                    WooPosProductsDataSource.ProductsResult.Remote(
                        Result.success(products)
                    )
                )
            )

            // WHEN
            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)
            viewModel.viewState.test {
                // THEN
                verify(productsDataSource).loadSimpleProducts(forceRefreshProducts = true)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `given content state, when end of products grid reached and no more pages, then do not load more`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple"
            ),
            ProductTestUtils.generateProduct(
                productId = 2,
                productName = "Product 2",
                amount = "20.0",
                productType = "simple"
            ).copy(firstImageUrl = "https://test.com")
        )
        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        whenever(productsDataSource.hasMorePages).thenReturn(false)

        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.EndOfProductListReached)
        viewModel.viewState.test {
            // THEN
            val value = awaitItem() as WooPosProductsViewState.Content
            assertThat(value.loadingMore).isFalse()
        }
    }

    @Test
    fun `when item clicked, then send event to parent`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple"
            ),
            ProductTestUtils.generateProduct(
                productId = 2,
                productName = "Product 2",
                amount = "20.0",
                productType = "simple"
            ).copy(firstImageUrl = "https://test.com")
        )
        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )

        val product = WooPosProductsListItem(
            id = 1,
            name = "Product 1",
            price = "$10.0",
            imageUrl = "https://test.com"
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.ItemClicked(product))
        viewModel.viewState.test {
            // THEN
            verify(fromChildToParentEventSender).sendToParent(
                ChildToParentEvent.ItemClickedInProductSelector(product.id)
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given load more products is called, when products source loads successfully then state is updated`() =
        runTest {
            // GIVEN
            val products = listOf(
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Product 1",
                    amount = "10.0",
                    productType = "simple"
                ),
                ProductTestUtils.generateProduct(
                    productId = 2,
                    productName = "Product 2",
                    amount = "20.0",
                    productType = "simple"
                ).copy(firstImageUrl = "https://test.com")
            )
            whenever(productsDataSource.loadSimpleProducts(eq(false))).thenReturn(
                flowOf(
                    WooPosProductsDataSource.ProductsResult.Remote(
                        Result.success(products)
                    )
                )
            )

            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosProductsUIEvent.EndOfProductListReached)
            viewModel.viewState.test {
                // THEN
                val value = awaitItem() as WooPosProductsViewState.Content
                assertThat(value.loadingMore).isFalse()
            }
        }

    @Test
    fun `when loading without pull to refresh, then should not ask to remove products`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple"
            ),
            ProductTestUtils.generateProduct(
                productId = 2,
                productName = "Product 2",
                amount = "20.0",
                productType = "simple"
            ).copy(firstImageUrl = "https://test.com")
        )

        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
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
            verify(productsDataSource).loadSimpleProducts(forceRefreshProducts = false)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `when loadProducts called, then view state is Loading`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(emptyList())
                )
            )
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.viewState.value).isInstanceOf(WooPosProductsViewState.Loading::class.java)
    }

    @Test
    fun `given error from load more, when list end reached, then state is Error`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple"
            )
        )
        whenever(productsDataSource.loadSimpleProducts(eq(false))).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        whenever(productsDataSource.loadMore()).thenReturn(Result.failure(Exception()))
        whenever(productsDataSource.hasMorePages).thenReturn(true)

        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.EndOfProductListReached)
        viewModel.viewState.test {
            // THEN
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosProductsViewState.Error::class.java)
        }
    }

    @Test
    fun `given no products, when pull to refresh, then state is Empty`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(emptyList())
                )
            )
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosProductsViewState.Empty::class.java)
        }
    }

    @Test
    fun `given empty list, when pull to refresh, then parent notified correctly`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(emptyList())
                )
            )
        )

        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)
        viewModel.viewState.test {
            // THEN
            verify(fromChildToParentEventSender).sendToParent(ChildToParentEvent.ProductsStatusChanged.FullScreen)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given products, when pull to refresh, then parent notified correctly`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple"
            )
        )
        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)
        viewModel.viewState.test {
            // THEN
            verify(fromChildToParentEventSender).sendToParent(ChildToParentEvent.ProductsStatusChanged.WithCart)
            cancelAndConsumeRemainingEvents()
        }
    }

    private fun createViewModel() =
        WooPosProductsViewModel(
            productsDataSource,
            fromChildToParentEventSender,
            priceFormat
        )
}
