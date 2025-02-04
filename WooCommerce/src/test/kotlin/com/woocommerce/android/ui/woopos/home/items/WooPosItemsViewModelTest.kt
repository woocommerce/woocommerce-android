package com.woocommerce.android.ui.woopos.home.items

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class WooPosItemsViewModelTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val productsDataSource: WooPosProductsDataSource = mock()
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val posPreferencesRepository: WooPosPreferencesRepository = mock()
    private val wooPosItemsNavigator: WooPosItemsNavigator = mock()
    private val priceFormat: WooPosFormatPrice = mock {
        onBlocking { invoke(BigDecimal("10.0")) }.thenReturn("$10.0")
        onBlocking { invoke(BigDecimal("20.0")) }.thenReturn("$20.0")
    }

    @Before
    fun setup() {
        whenever(posPreferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser).thenReturn(
            flowOf(false)
        )
    }

    @Test
    fun `given products from data source, when view model created, then view state updated correctly`() = runTest {
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
                productType = "simple",
                isDownloadable = false,
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
            val value = awaitItem() as WooPosItemsViewState.Content

            @Suppress("UNCHECKED_CAST")
            val items = value.items as List<WooPosItem.SimpleProduct>
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
            assertThat(value).isEqualTo(WooPosItemsViewState.Empty())
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
            assertThat(value).isEqualTo(WooPosItemsViewState.Error())
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
            viewModel.onUIEvent(WooPosItemsUIEvent.PullToRefreshTriggered)
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
        viewModel.onUIEvent(WooPosItemsUIEvent.EndOfItemsListReached)
        viewModel.viewState.test {
            // THEN
            val value = awaitItem() as WooPosItemsViewState.Content
            assertThat(value.paginationState).isEqualTo(PaginationState.None)
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

        val product = WooPosItem.SimpleProduct(
            id = 1,
            name = "Product 1",
            price = "$10.0",
            imageUrl = "https://test.com"
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.ItemClicked(product))
        viewModel.viewState.test {
            // THEN
            verify(fromChildToParentEventSender).sendToParent(
                ChildToParentEvent.ItemClickedInProductSelector(
                    WooPosItemsViewModel.ItemClickedData.SimpleProduct(
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
            viewModel.onUIEvent(WooPosItemsUIEvent.EndOfItemsListReached)
            viewModel.viewState.test {
                // THEN
                val value = awaitItem() as WooPosItemsViewState.Content
                assertThat(value.paginationState).isEqualTo(PaginationState.None)
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
        assertThat(viewModel.viewState.value).isInstanceOf(WooPosItemsViewState.Loading::class.java)
    }

    @Test
    fun `given error from load more, when list end reached, then state is pagination error`() = runTest {
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
        viewModel.onUIEvent(WooPosItemsUIEvent.EndOfItemsListReached)
        viewModel.viewState.test {
            // THEN
            val value = awaitItem() as ContentViewState
            assertThat(value.paginationState).isInstanceOf(PaginationState.Error::class.java)
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
        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(emptyList())
                )
            )
        )

        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsUIEvent.PullToRefreshTriggered)
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
        viewModel.onUIEvent(WooPosItemsUIEvent.PullToRefreshTriggered)
        viewModel.viewState.test {
            // THEN
            verify(fromChildToParentEventSender).sendToParent(ChildToParentEvent.ProductsStatusChanged.WithCart)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `when simple products only banner is closed, then state is updated to true`() = runTest {
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
        whenever(posPreferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser).thenReturn(
            flowOf(true)
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.viewState.test {
            val contentState = awaitItem() as WooPosItemsViewState.Content
            viewModel.onUIEvent(WooPosItemsUIEvent.SimpleProductsBannerClosed)

            // THEN
            assertTrue(contentState.bannerState.isBannerHiddenByUser)
        }
    }

    @Test
    fun `when simple products only banner is closed, then data store is updated to true`() = runTest {
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
        whenever(posPreferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser).thenReturn(
            flowOf(true)
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsUIEvent.SimpleProductsBannerClosed)

        // THEN
        verify(posPreferencesRepository).setSimpleProductsOnlyBannerWasHiddenByUser(true)
    }

    @Test
    fun `given simple products only banner is shown, when view model init, then state is updated with false`() =
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
            whenever(posPreferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser).thenReturn(
                flowOf(false)
            )

            // WHEN
            val viewModel = createViewModel()
            viewModel.viewState.test {
                val contentState = awaitItem() as WooPosItemsViewState.Content

                // THEN
                assertThat(contentState.bannerState.isBannerHiddenByUser).isFalse()
            }
        }

    @Test
    fun `given simple products only banner already closed by user, when view model init, then banner state is updated to true`() =
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
            whenever(posPreferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser).thenReturn(
                flowOf(true)
            )

            // WHEN
            val viewModel = createViewModel()
            viewModel.viewState.test {
                val contentState = awaitItem() as WooPosItemsViewState.Content

                // THEN
                assertTrue(contentState.bannerState.isBannerHiddenByUser)
            }
        }

    @Test
    fun `given simple products only banner is shown, then correct title is displayed`() = runTest {
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
        whenever(posPreferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser).thenReturn(
            flowOf(false)
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.viewState.test {
            val contentState = awaitItem() as WooPosItemsViewState.Content

            // THEN
            assertThat(contentState.bannerState.title).isEqualTo(R.string.woopos_banner_simple_products_only_title)
        }
    }

    @Test
    fun `given simple products only banner is shown, then correct message is displayed`() = runTest {
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
        whenever(posPreferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser).thenReturn(
            flowOf(false)
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.viewState.test {
            val contentState = awaitItem() as WooPosItemsViewState.Content

            // THEN
            assertThat(contentState.bannerState.message).isEqualTo(R.string.woopos_banner_simple_products_only_message)
        }
    }

    @Test
    fun `given simple products only banner is shown, then correct banner icon is displayed`() = runTest {
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
        whenever(posPreferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser).thenReturn(
            flowOf(false)
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.viewState.test {
            // THEN
            val contentState = awaitItem() as WooPosItemsViewState.Content

            // THEN
            assertThat(contentState.bannerState.icon).isEqualTo(R.drawable.info)
        }
    }

    @Test
    fun `given info icon displayed, when clicked, then appropriate event is triggered`() = runTest {
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
        whenever(posPreferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser).thenReturn(
            flowOf(true)
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsUIEvent.SimpleProductsDialogInfoIconClicked)

        // THEN
        verify(fromChildToParentEventSender).sendToParent(ChildToParentEvent.ProductsDialogInfoIconClicked)
    }

    @Test
    fun `given simple products banner displayed, when learn more clicked, then appropriate event is triggered`() =
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
            whenever(posPreferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser).thenReturn(
                flowOf(false)
            )

            // WHEN
            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosItemsUIEvent.SimpleProductsBannerLearnMoreClicked)

            // THEN
            verify(fromChildToParentEventSender).sendToParent(ChildToParentEvent.ProductsDialogInfoIconClicked)
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
        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        val viewModel = createViewModel()
        viewModel.onUIEvent(
            WooPosItemsUIEvent.ItemClicked(
                WooPosItem.VariableProduct(
                    id = 1L,
                    name = "Product 1",
                    numOfVariations = 10,
                    variationIds = emptyList(),
                    price = "$10.0",
                    imageUrl = null
                )
            )
        )

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
    fun `given variations screen, when clicked back, then trigger proper event`() = runTest {
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
        whenever(productsDataSource.loadSimpleProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsUIEvent.BackButtonClicked)

        verify(wooPosItemsNavigator).sendNavigationEvent(
            WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen
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
                val value = awaitItem() as WooPosItemsViewState.Content

                assertThat(value.items.filterIsInstance<WooPosItem.VariableProduct>().size).isEqualTo(1)
            }
        }

    private fun createViewModel() =
        WooPosItemsViewModel(
            productsDataSource,
            fromChildToParentEventSender,
            priceFormat,
            posPreferencesRepository,
            wooPosItemsNavigator,
        )
}
