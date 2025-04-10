package com.woocommerce.android.ui.woopos.home.variations

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosVariationsViewState
import com.woocommerce.android.ui.woopos.home.items.variations.FetchResult
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsDataSource
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsUIEvents
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsViewModel
import com.woocommerce.android.ui.woopos.home.items.variations.getNameForPOS
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.VariationsPullToRefreshTriggered
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
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
class WooPosVariationsViewModelTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val getProductById: WooPosGetProductById = mock()
    private val variationsDataSource: WooPosVariationsDataSource = mock()
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val priceFormat: WooPosFormatPrice = mock {
        onBlocking { invoke(BigDecimal("10.0")) }.thenReturn("$10.0")
        onBlocking { invoke(BigDecimal("20.0")) }.thenReturn("$20.0")
        onBlocking { invoke(BigDecimal("0.00")) }.thenReturn("$0.00")
        onBlocking { invoke(null) }.thenReturn("$0.00")
    }
    private val analyticsTracker: WooPosAnalyticsTracker = mock()

    @Test
    fun `given variations from data source, when view model created, then view state updated correctly`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0"),
            ProductTestUtils.generateProductVariation(2, 1, "20.0")
        )
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flowOf(FetchResult.Remote(Result.success(variations)))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.init(1L)

        viewModel.viewState.test {
            // THEN
            val state = awaitItem() as WooPosVariationsViewState.Content
            assertThat(state.items).hasSize(2)
            assertThat(state.items[0].id).isEqualTo(1)
            assertThat(state.items[0].price).isEqualTo("$10.0")
        }
    }

    @Test
    fun `given variation has zero price, when view model created, then view state updated correctly`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "0.00"),
        )
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flowOf(FetchResult.Remote(Result.success(variations)))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.init(1L)

        viewModel.viewState.test {
            // THEN
            val state = awaitItem() as WooPosVariationsViewState.Content
            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].id).isEqualTo(1)
            assertThat(state.items[0].price).isEqualTo("$0.00")
        }
    }

    @Test
    fun `given variation has no price set, when view model created, then view state updated correctly`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, ""),
        )
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flowOf(FetchResult.Remote(Result.success(variations)))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.init(1L)

        viewModel.viewState.test {
            // THEN
            val state = awaitItem() as WooPosVariationsViewState.Content
            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].id).isEqualTo(1)
            assertThat(state.items[0].price).isEqualTo("$0.00")
        }
    }

    @Test
    fun `given empty variations list returned, when view model created, then view state is empty`() = runTest {
        // GIVEN
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flowOf(FetchResult.Remote(Result.success(emptyList())))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.init(1L)
        viewModel.viewState.test {
            // THEN
            assertThat(awaitItem()).isEqualTo(WooPosVariationsViewState.Empty())
        }
    }

    @Test
    fun `given error fetching variations, when view model created, then view state is error`() = runTest {
        // GIVEN
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flowOf(FetchResult.Remote(Result.failure(Exception("Fetch error"))))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.init(1L)

        viewModel.viewState.test {
            // THEN
            assertThat(awaitItem()).isEqualTo(WooPosVariationsViewState.Error())
        }
    }

    @Test
    fun `given variations, when pull to refresh triggered, then fetchFirstPage is called`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0"),
            ProductTestUtils.generateProductVariation(2, 1, "20.0")
        )
        whenever(variationsDataSource.fetchFirstPage(any(), eq(true))).thenReturn(
            flowOf(FetchResult.Remote(Result.success(variations)))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosVariationsUIEvents.PullToRefreshTriggered(123L))

        // THEN
        verify(variationsDataSource).fetchFirstPage(eq(123L), eq(true))
    }

    @Test
    fun `when pull to refresh triggered, then should track event`() = runTest {
        whenever(variationsDataSource.fetchFirstPage(any(), eq(true))).thenReturn(
            flowOf(FetchResult.Remote(Result.success(emptyList())))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosVariationsUIEvents.PullToRefreshTriggered(123L))

        // THEN
        verify(analyticsTracker).track(VariationsPullToRefreshTriggered)
    }

    @Test
    fun `given no more variations, when end of list reached, then pagination state is none`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0"),
            ProductTestUtils.generateProductVariation(2, 1, "20.0")
        )
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flowOf(FetchResult.Remote(Result.success(variations)))
        )
        whenever(variationsDataSource.canLoadMore(any())).thenReturn(false)
        whenever(variationsDataSource.loadMore(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosVariationsUIEvents.EndOfItemsListReached(123L, 10))
        advanceUntilIdle()
        // THEN
        viewModel.viewState.test {
            val state = awaitItem() as WooPosVariationsViewState.Content
            assertThat(state.paginationState).isEqualTo(WooPosPaginationState.None)
        }
    }

    @Test
    fun `given variations, when load more succeeds, then pagination state is updated`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0"),
        )
        whenever(variationsDataSource.loadMore(any())).thenReturn(Result.success(variations))
        whenever(variationsDataSource.canLoadMore(any())).thenReturn(true)
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flow {
                emit(
                    FetchResult.Remote(
                        Result.success(
                            listOf(
                                ProductTestUtils.generateProductVariation(1, 1, "10.0"),
                                ProductTestUtils.generateProductVariation(2, 1, "20.0")
                            )
                        )
                    )
                )
            }
        )

        val viewModel = createViewModel()
        viewModel.init(1L)
        viewModel.onUIEvent(WooPosVariationsUIEvents.EndOfItemsListReached(123L, 10))

        // THEN
        viewModel.viewState.test {
            val state = awaitItem() as WooPosVariationsViewState.Content
            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].id).isEqualTo(1)
        }
    }

    @Test
    fun `given load more fails, when end of list reached, then pagination state is error`() = runTest {
        // GIVEN
        whenever(variationsDataSource.loadMore(any())).thenReturn(Result.failure(Exception()))
        whenever(variationsDataSource.canLoadMore(any())).thenReturn(true)
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flow {
                emit(
                    FetchResult.Remote(
                        Result.success(
                            listOf(
                                ProductTestUtils.generateProductVariation(1, 1, "10.0"),
                                ProductTestUtils.generateProductVariation(2, 1, "20.0")
                            )
                        )
                    )
                )
            }
        )

        val viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()
        viewModel.onUIEvent(WooPosVariationsUIEvents.EndOfItemsListReached(123L, 10))
        advanceUntilIdle()

        // THEN
        viewModel.viewState.test {
            val state = awaitItem() as WooPosVariationsViewState.Content
            assertThat(state.paginationState).isEqualTo(WooPosPaginationState.Error)
        }
    }

    @Test
    fun `given fetching variations first page and load more call is also happening, when view model created, then view state updated correctly`() =
        runTest {
            // GIVEN
            val variations = listOf(
                ProductTestUtils.generateProductVariation(1, 1, "10.0"),
                ProductTestUtils.generateProductVariation(2, 1, "20.0")
            )
            whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
                flowOf(FetchResult.Remote(Result.success(variations)))
            )

            // WHEN
            val viewModel = createViewModel()
            val activeJob = Job()
            viewModel.loadMoreJob = activeJob
            viewModel.init(1L)
            viewModel.onUIEvent(WooPosVariationsUIEvents.EndOfItemsListReached(1L, 10))

            viewModel.viewState.test {
                // THEN
                val state = awaitItem() as WooPosVariationsViewState.Content
                assertThat(state.paginationState).isEqualTo(WooPosPaginationState.Loading)
            }
        }

    @Test
    fun `given fetching variations first page and load more call is not happening, when view model created, then view state updated correctly`() =
        runTest {
            // GIVEN
            val variations = listOf(
                ProductTestUtils.generateProductVariation(1, 1, "10.0"),
                ProductTestUtils.generateProductVariation(2, 1, "20.0")
            )
            whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
                flowOf(FetchResult.Remote(Result.success(variations)))
            )

            // WHEN
            val viewModel = createViewModel()
            viewModel.init(1L)

            viewModel.viewState.test {
                // THEN
                val state = awaitItem() as WooPosVariationsViewState.Content
                assertThat(state.paginationState).isEqualTo(WooPosPaginationState.None)
            }
        }

    @Test
    fun `given variation clicked, when item clicked, then send event to parent`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosVariationsUIEvents.OnItemClicked(123L, 1L))

        // THEN
        verify(fromChildToParentEventSender).sendToParent(
            ChildToParentEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Variation(123L, 1L)
            )
        )
    }

    @Test
    fun `given variable product, getNameForPOS returns correct name when parent product has variation enabled attributes`() =
        runTest {
            // GIVEN
            val parentProduct = ProductTestUtils.generateProduct(
                1,
                isVariable = true,
                productAttributes = """[
                                {
                                    "id": 1,
                                    "name":"Color",
                                    "position":0",
                                    "visible":"true",
                                    "variation":"true",
                                    "options": ["Blue","Green","Red"]
                                }
                            ]"""
            )
            val variableProduct = ProductTestUtils.generateProductVariation(
                1,
                1,
                "10.0",
                productAttributes = """[
                                {
                                    "id": 1,
                                    "name":"Color",
                                    "option": "Blue"
                                }
                            ]"""
            )

            // WHEN
            val attributeName = variableProduct.getNameForPOS(parentProduct, resourceProvider)

            // Then
            assertThat(attributeName).isEqualTo("Color: Blue")
        }

    @Test
    fun `given no parent product, when getNameForPOS is called, then it returns variation attributes`() = runTest {
        // GIVEN
        val variableProduct = ProductTestUtils.generateProductVariation(
            1,
            1,
            "10.0",
            productAttributes = """[
            {
                "id": 1,
                "name": "Color",
                "option": "Blue"
            },
            {
                "id": 2,
                "name": "Size",
                "option": "M"
            }
        ]"""
        )

        // WHEN
        val attributeName = variableProduct.getNameForPOS(null, resourceProvider)

        // THEN
        assertThat(attributeName).isEqualTo("Color: Blue, Size: M")
    }

    @Test
    fun `given parent product with non-matching attributes, when getNameForPOS is called, then it returns 'Any {attribute}'`() =
        runTest {
            // GIVEN
            val parentProduct = ProductTestUtils.generateProduct(
                1,
                productAttributes = """[
            {
                "id": 3,
                "name": "Material",
                "variation": true,
                "options": ["Cotton", "Polyester"]
            }
        ]"""
            )
            val variableProduct = ProductTestUtils.generateProductVariation(
                1,
                1,
                "10.0",
                productAttributes = """[
            {
                "id": 1,
                "name": "Color",
                "option": "Blue"
            },
            {
                "id": 2,
                "name": "Size",
                "option": "M"
            }
        ]"""
            )
            whenever(
                resourceProvider.getString(R.string.woopos_variations_any_variation, "Material")
            ).thenReturn("Any Material")

            // WHEN
            val attributeName = variableProduct.getNameForPOS(parentProduct, resourceProvider)

            // THEN
            assertThat(attributeName).isEqualTo("Any Material")
        }

    @Test
    fun `given matching attributes in parent product and variation, when getNameForPOS is called, then it returns the correct attribute names`() =
        runTest {
            // GIVEN
            val parentProduct = ProductTestUtils.generateProduct(
                1,
                productAttributes = """[
            {
                "id": 1,
                "name": "Color",
                "variation": true,
                "options": ["Blue", "Green", "Red"]
            },
            {
                "id": 2,
                "name": "Size",
                "variation": true,
                "options": ["S", "M", "L"]
            }
        ]"""
            )
            val variableProduct = ProductTestUtils.generateProductVariation(
                1,
                1,
                "10.0",
                productAttributes = """[
            {
                "id": 1,
                "name": "Color",
                "option": "Blue"
            },
            {
                "id": 2,
                "name": "Size",
                "option": "M"
            }
        ]"""
            )

            // WHEN
            val attributeName = variableProduct.getNameForPOS(parentProduct, resourceProvider)

            // THEN
            assertThat(attributeName).isEqualTo("Color: Blue, Size: M")
        }

    @Test
    fun `given attributes with missing options, when getNameForPOS is called, then it returns 'Any {attribute}'`() =
        runTest {
            // GIVEN
            val parentProduct = ProductTestUtils.generateProduct(
                1,
                productAttributes = """[
            {
                "id": 1,
                "name": "Color",
                "variation": true,
                "options": ["Blue", "Green", "Red"]
            },
            {
                "id": 2,
                "name": "Size",
                "variation": true,
                "options": ["S", "M", "L"]
            }
        ]"""
            )
            val variableProduct = ProductTestUtils.generateProductVariation(
                1,
                1,
                "10.0",
                productAttributes = """[
            {
                "id": 1,
                "name": "Color",
                "option": null
            },
            {
                "id": 2,
                "name": "Size",
                "option": null
            }
        ]"""
            )
            whenever(
                resourceProvider.getString(R.string.woopos_variations_any_variation, "Color")
            ).thenReturn("Any Color")
            whenever(
                resourceProvider.getString(R.string.woopos_variations_any_variation, "Size")
            ).thenReturn("Any Size")

            // WHEN
            val attributeName = variableProduct.getNameForPOS(parentProduct, resourceProvider)

            // THEN
            assertThat(attributeName).isEqualTo("Any Color, Any Size")
        }

    @Test
    fun `given variation with no attributes, when getNameForPOS is called, then it returns 'Any {attribute}'`() =
        runTest {
            // GIVEN
            val parentProduct = ProductTestUtils.generateProduct(
                1,
                productAttributes = """[
            {
                "id": 1,
                "name": "Color",
                "variation": true,
                "options": ["Blue", "Green", "Red"]
            }
        ]"""
            )
            val variableProduct = ProductTestUtils.generateProductVariation(
                1,
                1,
                "10.0",
                productAttributes = "[]"
            )
            whenever(
                resourceProvider.getString(R.string.woopos_variations_any_variation, "Color")
            ).thenReturn("Any Color")

            // WHEN
            val attributeName = variableProduct.getNameForPOS(parentProduct, resourceProvider)

            // THEN
            assertThat(attributeName).isEqualTo("Any Color")
        }

    @Test
    fun `given non-variation-enabled attributes in variation, when getNameForPOS is called, then it ignores those attributes`() =
        runTest {
            // GIVEN
            val parentProduct = ProductTestUtils.generateProduct(
                1,
                productAttributes = """[
            {
                "id": 1,
                "name": "Color",
                "variation": true,
                "options": ["Blue", "Green", "Red"]
            },
            {
                "id": 2,
                "name": "Material",
                "variation": false,
                "options": ["Cotton", "Polyester"]
            }
        ]"""
            )
            val variableProduct = ProductTestUtils.generateProductVariation(
                1,
                1,
                "10.0",
                productAttributes = """[
            {
                "id": 1,
                "name": "Color",
                "option": "Blue"
            },
            {
                "id": 2,
                "name": "Material",
                "option": "Cotton"
            }
        ]"""
            )

            // WHEN
            val attributeName = variableProduct.getNameForPOS(parentProduct, resourceProvider)

            // THEN
            assertThat(attributeName).isEqualTo("Color: Blue")
        }

    private fun createViewModel() =
        WooPosVariationsViewModel(
            fromChildToParentEventSender,
            getProductById,
            variationsDataSource,
            priceFormat,
            resourceProvider,
            analyticsTracker,
        )
}
