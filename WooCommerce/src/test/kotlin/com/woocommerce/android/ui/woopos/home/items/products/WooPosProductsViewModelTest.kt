package com.woocommerce.android.ui.woopos.home.items.products

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosContentViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosProductsViewState
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogProductSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.ProductsResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosPerformLocalCatalogIncrementalSync
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.ui.woopos.util.generateWooPosProduct
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ResourceProvider
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
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosProductsViewModelTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val priceFormat: WooPosFormatPrice = mock {
        on { invoke(BigDecimal("10.0")) }.thenReturn("$10.0")
        on { invoke(BigDecimal("20.0")) }.thenReturn("$20.0")
    }
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val productsDataSource: WooPosProductsDataSource = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val catalogIncrementalSync: WooPosPerformLocalCatalogIncrementalSync = mock()

    @Before
    fun setup() {
        val products = listOf(
            generateWooPosProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = WooPosProductModel.WooPosProductType.Simple,
                isDownloadable = false,
            ),
        )

        whenever(productsDataSource.fetchFirstPage(any())).thenReturn(
            flowOf(
                ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        whenever(parentToChildrenEventReceiver.events).thenReturn(flowOf())

        whenever(productsDataSource.hasMorePages).thenReturn(false)
    }

    @Test
    fun `given products from data source, when view model created, then view state updated correctly`() = runTest {
        // WHEN
        val products = listOf(
            generateWooPosProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = WooPosProductModel.WooPosProductType.Simple,
                isDownloadable = false,
            ),
            generateWooPosProduct(
                productId = 2,
                productName = "Product 2",
                amount = "20.0",
                productType = WooPosProductModel.WooPosProductType.Simple,
                isDownloadable = false,
                images = listOf(WooPosProductModel.WooPosProductImage(0, "https://test.com", "", ""))
            )
        )

        whenever(productsDataSource.fetchFirstPage(any())).thenReturn(
            flowOf(
                ProductsResult.Remote(
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
        whenever(productsDataSource.fetchFirstPage(any())).thenReturn(
            flowOf(
                ProductsResult.Remote(
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
        whenever(productsDataSource.fetchFirstPage(any())).thenReturn(
            flowOf(
                ProductsResult.Remote(
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
    fun `given products from data source, when pulled to refresh, then should call refreshProducts`() =
        runTest {
            // GIVEN
            whenever(productsDataSource.refreshProducts()).thenReturn(
                Result.success(
                    ProductsResult.Remote(
                        Result.success(
                            listOf(
                                generateWooPosProduct(
                                    productId = 1,
                                    productName = "Product 1",
                                    amount = "10.0",
                                    productType = WooPosProductModel.WooPosProductType.Simple,
                                    isDownloadable = false,
                                )
                            )
                        )
                    )
                )
            )

            // WHEN
            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)

            // THEN
            viewModel.viewState.test {
                verify(productsDataSource).refreshProducts()
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
                    ChildToParentEvent.ItemClickedInItemsList(
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
            verify(productsDataSource, times(1)).fetchFirstPage(forceRefresh = false)
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
        whenever(productsDataSource.refreshProducts()).thenReturn(
            Result.success(
                ProductsResult.Remote(
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
        whenever(productsDataSource.refreshProducts()).thenReturn(
            Result.success(
                ProductsResult.Remote(
                    Result.success(emptyList())
                )
            )
        )
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
    fun `given variable products from data source, when view model created, then items list updated correctly`() = runTest {
        // GIVEN
        val products = listOf(
            generateWooPosProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = WooPosProductModel.WooPosProductType.Simple,
                isDownloadable = false,
            ),
            generateWooPosProduct(
                productId = 2,
                productName = "Product 2",
                amount = "20.0",
                productType = WooPosProductModel.WooPosProductType.Variable,
                isDownloadable = false,
                images = listOf(WooPosProductModel.WooPosProductImage(0, "https://test.com", "", ""))
            )
        )

        whenever(productsDataSource.fetchFirstPage(any())).thenReturn(
            flowOf(
                ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val contentState = awaitItem() as WooPosProductsViewState.Content
            assertThat(
                contentState.items.filterIsInstance<WooPosItemSelectionViewState.Product.Variable>().size
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
                        generateWooPosProduct(
                            productId = 2,
                            productName = "Product 2",
                            amount = "20.0",
                            productType = WooPosProductModel.WooPosProductType.Simple
                        )
                    )
                )
            )

            val productsFlow = flow {
                emit(
                    ProductsResult.Remote(
                        Result.success(
                            listOf(
                                generateWooPosProduct(
                                    productId = 1,
                                    productName = "Product 1",
                                    amount = "10.0",
                                    productType = WooPosProductModel.WooPosProductType.Simple
                                )
                            )
                        )
                    )
                )
            }

            whenever(productsDataSource.fetchFirstPage(any())).thenReturn(productsFlow)

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

        val productsFlow = MutableSharedFlow<ProductsResult>()
        whenever(productsDataSource.fetchFirstPage(any())).thenReturn(productsFlow)

        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.EndOfItemsListReached)

        // Then
        productsFlow.emit(
            ProductsResult.Remote(
                Result.failure(Exception("Test error"))
            )
        )
        advanceUntilIdle()

        // THEN
        verify(productsDataSource, never()).loadMore()
    }

    @Test
    fun `when variable product clicked, then send event to parent without tracking event`() = runTest {
        // GIVEN
        val variableProduct = WooPosItemSelectionViewState.Product.Variable(
            id = 123L,
            name = "Variable Product",
            price = "$20.0",
            imageUrl = null,
            numOfVariations = 5,
            variationIds = listOf(1, 2, 3)
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.ItemClicked(variableProduct))

        // THEN
        verify(fromChildToParentEventSender).sendToParent(
            eq(
                ChildToParentEvent.ItemClickedInItemsList(
                    itemData = WooPosItemsViewModel.ItemClickedData.VariableProduct(
                        id = 123L,
                        name = "Variable Product",
                        numOfVariations = 5,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST,
                    ),
                    eventForTracking = null,
                )
            )
        )
    }

    @Test
    fun `when RefreshProductList event received, then products are reloaded with force refresh`() = runTest {
        // GIVEN
        whenever(parentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.RefreshProductList)
        )

        // WHEN
        createViewModel()

        // THEN
        verify(productsDataSource).fetchFirstPage(forceRefresh = true)
    }

    @Test
    fun `when ProductsLoadingErrorRetryButtonClicked, then products are reloaded without force refresh`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.ProductsLoadingErrorRetryButtonClicked)

        // THEN
        verify(productsDataSource, times(2)).fetchFirstPage(forceRefresh = false)
    }

    @Test
    fun `when successful load more, then analytics event is tracked`() = runTest {
        // GIVEN
        whenever(productsDataSource.hasMorePages).thenReturn(true)
        whenever(productsDataSource.loadMore()).thenReturn(
            Result.success(
                listOf(
                    generateWooPosProduct(
                        productId = 2,
                        productName = "Product 2",
                        amount = "20.0",
                        productType = WooPosProductModel.WooPosProductType.Simple,
                    )
                )
            )
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosProductsUIEvent.EndOfItemsListReached)

        // THEN
        verify(analyticsTracker).track(
            WooPosAnalyticsEvent.Event.ItemsNextPageLoaded(
                source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
            )
        )
    }

    @Test
    fun `given network error on PTR, when pull to refresh fails, then show offline error message`() = runTest {
        // GIVEN
        val offlineErrorMessage = "No internet connection. Please check your network and try again."
        whenever(resourceProvider.getString(R.string.woo_pos_ptr_offline_error)).thenReturn(offlineErrorMessage)
        whenever(productsDataSource.refreshProducts()).thenReturn(
            Result.success(
                PosLocalCatalogProductSyncResult(
                    PosLocalCatalogSyncResult.Failure.NetworkError("Network unavailable")
                )
            )
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)

        // THEN
        verify(fromChildToParentEventSender).sendToParent(
            eq(ChildToParentEvent.ToastMessageDisplayed(message = offlineErrorMessage))
        )
    }

    @Test
    fun `given non-network error on PTR, when pull to refresh fails, then show generic error message`() = runTest {
        // GIVEN
        val genericErrorMessage = "Something went wrong, Please try again later."
        whenever(resourceProvider.getString(R.string.something_went_wrong_try_again)).thenReturn(genericErrorMessage)
        whenever(productsDataSource.refreshProducts()).thenReturn(
            Result.success(
                PosLocalCatalogProductSyncResult(
                    PosLocalCatalogSyncResult.Failure.DatabaseError("Database error")
                )
            )
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)

        // THEN
        verify(fromChildToParentEventSender).sendToParent(
            eq(ChildToParentEvent.ToastMessageDisplayed(message = genericErrorMessage))
        )
    }

    @Test
    fun `given remote sync network error on PTR, when pull to refresh fails, then show offline error message`() =
        runTest {
            // GIVEN
            val offlineErrorMessage = "No internet connection. Please check your network and try again."
            whenever(resourceProvider.getString(R.string.woo_pos_ptr_offline_error)).thenReturn(offlineErrorMessage)
            whenever(productsDataSource.refreshProducts()).thenReturn(
                Result.success(
                    ProductsResult.Remote(
                        Result.failure(
                            WooException(
                                WooError(
                                    type = WooErrorType.NO_CONNECTION,
                                    original = GenericErrorType.NO_CONNECTION,
                                    message = "No network"
                                )
                            )
                        )
                    )
                )
            )

            // WHEN
            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)

            // THEN
            verify(fromChildToParentEventSender).sendToParent(
                eq(ChildToParentEvent.ToastMessageDisplayed(message = offlineErrorMessage))
            )
        }

    @Test
    fun `given remote sync timeout error on PTR, when pull to refresh fails, then show offline error message`() =
        runTest {
            // GIVEN
            val offlineErrorMessage = "No internet connection. Please check your network and try again."
            whenever(resourceProvider.getString(R.string.woo_pos_ptr_offline_error)).thenReturn(offlineErrorMessage)
            whenever(productsDataSource.refreshProducts()).thenReturn(
                Result.success(
                    ProductsResult.Remote(
                        Result.failure(
                            WooException(
                                WooError(
                                    type = WooErrorType.TIMEOUT,
                                    original = GenericErrorType.TIMEOUT,
                                    message = "Request timed out"
                                )
                            )
                        )
                    )
                )
            )

            // WHEN
            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)

            // THEN
            verify(fromChildToParentEventSender).sendToParent(
                eq(ChildToParentEvent.ToastMessageDisplayed(message = offlineErrorMessage))
            )
        }

    @Test
    fun `given remote sync generic error on PTR, when pull to refresh fails, then show generic error message`() =
        runTest {
            // GIVEN
            val genericErrorMessage = "Something went wrong, Please try again later."
            whenever(
                resourceProvider.getString(R.string.something_went_wrong_try_again)
            ).thenReturn(genericErrorMessage)
            whenever(productsDataSource.refreshProducts()).thenReturn(
                Result.success(
                    ProductsResult.Remote(
                        Result.failure(
                            WooException(
                                WooError(
                                    type = WooErrorType.GENERIC_ERROR,
                                    original = GenericErrorType.UNKNOWN,
                                    message = "Something went wrong"
                                )
                            )
                        )
                    )
                )
            )

            // WHEN
            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)

            // THEN
            verify(fromChildToParentEventSender).sendToParent(
                eq(ChildToParentEvent.ToastMessageDisplayed(message = genericErrorMessage))
            )
        }

    private fun createViewModel(): WooPosProductsViewModel {
        return WooPosProductsViewModel(
            dataSource = productsDataSource,
            fromChildToParentEventSender = fromChildToParentEventSender,
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            priceFormat = priceFormat,
            analyticsTracker = analyticsTracker,
            resourceProvider = resourceProvider,
            dispatchers = CoroutineDispatchers(
                coroutinesTestRule.testDispatcher,
                coroutinesTestRule.testDispatcher,
                coroutinesTestRule.testDispatcher
            ),
            incrementalSync = catalogIncrementalSync,
        )
    }
}
