package com.woocommerce.android.ui.woopos.home.variations

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationMapper
import com.woocommerce.android.ui.woopos.common.data.getNameForPOS
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.toWooPosVariation
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosVariationsViewState
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsUIEvents
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsViewModel
import com.woocommerce.android.ui.woopos.localcatalog.VariationsResult
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ItemsListSourceType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.ui.woopos.util.generateWooPosProduct
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
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
    private val variationsDataSource: WooPosProductsDataSource = mock()
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val priceFormat: WooPosFormatPrice = mock {
        on { invoke(BigDecimal("10.0")) }.thenReturn("$10.0")
        on { invoke(BigDecimal("20.0")) }.thenReturn("$20.0")
        on { invoke(BigDecimal("0.00")) }.thenReturn("$0.00")
        on { invoke(null) }.thenReturn("$0.00")
    }
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val mapper: WooPosVariationMapper = mock {
        on { fromProductVariation(any()) } doAnswer { invocation ->
            val productVariation = invocation.arguments[0] as com.woocommerce.android.model.ProductVariation
            WooPosVariation(
                remoteVariationId = productVariation.remoteVariationId,
                remoteProductId = productVariation.remoteProductId,
                globalUniqueId = productVariation.globalUniqueId,
                type = WooPosVariation.WooPosVariationType.VARIATION,
                price = productVariation.price,
                image = productVariation.image?.let { WooPosVariation.WooPosVariationImage(it.source) },
                attributes = productVariation.attributes.map {
                    WooPosVariation.WooPosVariationAttribute(
                        id = it.id,
                        name = it.name,
                        option = it.option
                    )
                },
                isVisible = productVariation.isVisible,
                isDownloadable = productVariation.isDownloadable
            )
        }
        on { getNameForPOS(any<WooPosVariation>(), anyOrNull(), any<ResourceProvider>()) } doAnswer { invocation ->
            val variation = invocation.arguments[0] as WooPosVariation
            val parentProduct = invocation.arguments[1] as? WooPosProductModel
            // Mock the basic behavior for tests
            if (parentProduct != null) {
                // Use parent product's variation enabled attributes
                parentProduct.variationEnabledAttributes.joinToString(", ") { attribute ->
                    val option = variation.attributes.firstOrNull { it.name == attribute.name }
                    if (option?.option != null) {
                        "${attribute.name}: ${option.option}"
                    } else {
                        "Any ${attribute.name}"
                    }
                }.ifEmpty { "Any variation" }
            } else {
                // No parent product, use all variation attributes
                variation.attributes.mapNotNull { attribute ->
                    if (attribute.name != null && attribute.option != null) {
                        "${attribute.name}: ${attribute.option}"
                    } else {
                        null
                    }
                }.joinToString(", ").takeIf { it.isNotEmpty() } ?: "Any variation"
            }
        }
    }

    @Test
    fun `given variations from data source, when view model created, then view state updated correctly`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0").toWooPosVariation(mapper),
            ProductTestUtils.generateProductVariation(2, 1, "20.0").toWooPosVariation(mapper)
        )
        whenever(variationsDataSource.fetchVariationsFirstPage(any(), any())).thenReturn(
            flowOf(VariationsResult.Remote(Result.success(variations)))
        )

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

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
            ProductTestUtils.generateProductVariation(1, 1, "0.00").toWooPosVariation(mapper),
        )
        whenever(variationsDataSource.fetchVariationsFirstPage(any(), any())).thenReturn(
            flowOf(VariationsResult.Remote(Result.success(variations)))
        )

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

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
            ProductTestUtils.generateProductVariation(1, 1, "").toWooPosVariation(mapper),
        )
        whenever(variationsDataSource.fetchVariationsFirstPage(any(), any())).thenReturn(
            flowOf(VariationsResult.Remote(Result.success(variations)))
        )

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

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
        whenever(variationsDataSource.fetchVariationsFirstPage(any(), any())).thenReturn(
            flowOf(VariationsResult.Remote(Result.success(emptyList())))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.viewState.test {
            // THEN
            assertThat(awaitItem()).isEqualTo(WooPosVariationsViewState.Empty())
        }
    }

    @Test
    fun `given error fetching variations, when view model created, then view state is error`() = runTest {
        // GIVEN
        whenever(variationsDataSource.fetchVariationsFirstPage(any(), any())).thenReturn(
            flowOf(VariationsResult.Remote(Result.failure(Exception("Fetch error"))))
        )

        // WHEN
        val viewModel = createViewModel()

        viewModel.viewState.test {
            // THEN
            assertThat(awaitItem()).isEqualTo(WooPosVariationsViewState.Error())
        }
    }

    @Test
    fun `given variations, when pull to refresh triggered, then refreshVariations() is called`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0").toWooPosVariation(mapper),
            ProductTestUtils.generateProductVariation(2, 1, "20.0").toWooPosVariation(mapper)
        )
        whenever(variationsDataSource.fetchVariationsFirstPage(any(), eq(false))).thenReturn(
            flowOf(VariationsResult.Remote(Result.success(emptyList())))
        )
        whenever(variationsDataSource.refreshVariations(any())).thenReturn(
            Result.success(
                VariationsResult.Remote(Result.success(variations))
            )
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosVariationsUIEvents.PullToRefreshTriggered(123L))

        // THEN
        verify(variationsDataSource).refreshVariations(eq(123L))
    }

    @Test
    fun `when pull to refresh triggered, then should track event`() = runTest {
        whenever(variationsDataSource.fetchVariationsFirstPage(any(), any())).thenReturn(
            flowOf(VariationsResult.Remote(Result.success(emptyList())))
        )
        whenever(variationsDataSource.refreshVariations(any())).thenReturn(
            Result.success(
                VariationsResult.Remote(Result.success(emptyList()))
            )
        )

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onUIEvent(WooPosVariationsUIEvents.PullToRefreshTriggered(123L))

        // THEN
        advanceUntilIdle()
        verify(analyticsTracker).track(
            WooPosAnalyticsEvent.Event.PullToRefreshTriggered(
                source = WooPosAnalyticsEventConstant.ItemsListSource.VARIATION,
                sourceType = ItemsListSourceType.LIST
            )
        )
    }

    @Test
    fun `given no more variations, when end of list reached, then pagination state is none`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0").toWooPosVariation(mapper),
            ProductTestUtils.generateProductVariation(2, 1, "20.0").toWooPosVariation(mapper)
        )
        whenever(variationsDataSource.fetchVariationsFirstPage(any(), any())).thenReturn(
            flowOf(VariationsResult.Remote(Result.success(variations)))
        )
        whenever(variationsDataSource.canLoadMoreVariations(any())).thenReturn(false)
        whenever(variationsDataSource.loadMoreVariations(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()
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
            ProductTestUtils.generateProductVariation(1, 1, "10.0").toWooPosVariation(mapper),
        )
        whenever(variationsDataSource.loadMoreVariations(any())).thenReturn(Result.success(variations))
        whenever(variationsDataSource.canLoadMoreVariations(any())).thenReturn(true)
        whenever(variationsDataSource.fetchVariationsFirstPage(any(), any())).thenReturn(
            flow {
                emit(
                    VariationsResult.Remote(
                        Result.success(
                            listOf(
                                ProductTestUtils.generateProductVariation(1, 1, "10.0").toWooPosVariation(mapper),
                                ProductTestUtils.generateProductVariation(2, 1, "20.0").toWooPosVariation(mapper)
                            )
                        )
                    )
                )
            }
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onUIEvent(WooPosVariationsUIEvents.EndOfItemsListReached(123L, 10))
        advanceUntilIdle()

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
        whenever(variationsDataSource.loadMoreVariations(any())).thenReturn(Result.failure(Exception()))
        whenever(variationsDataSource.canLoadMoreVariations(any())).thenReturn(true)
        whenever(variationsDataSource.fetchVariationsFirstPage(any(), any())).thenReturn(
            flow {
                emit(
                    VariationsResult.Remote(
                        Result.success(
                            listOf(
                                ProductTestUtils.generateProductVariation(1, 1, "10.0").toWooPosVariation(mapper),
                                ProductTestUtils.generateProductVariation(2, 1, "20.0").toWooPosVariation(mapper)
                            )
                        )
                    )
                )
            }
        )

        val viewModel = createViewModel()
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
    fun `given fetching variations first page and load more call is not happening, when view model created, then view state updated correctly`() =
        runTest {
            // GIVEN
            val variations = listOf(
                ProductTestUtils.generateProductVariation(1, 1, "10.0").toWooPosVariation(mapper),
                ProductTestUtils.generateProductVariation(2, 1, "20.0").toWooPosVariation(mapper)
            )
            whenever(variationsDataSource.fetchVariationsFirstPage(any(), any())).thenReturn(
                flowOf(VariationsResult.Remote(Result.success(variations)))
            )

            // WHEN
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.init(1L, sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST)
            advanceUntilIdle()

            viewModel.viewState.test {
                // THEN
                val state = awaitItem() as WooPosVariationsViewState.Content
                assertThat(state.paginationState).isEqualTo(WooPosPaginationState.None)
            }
        }

    @Test
    fun `given variation clicked and source is product list, when item clicked, then sends event with product list source`() =
        runTest {
            // GIVEN
            whenever(variationsDataSource.fetchVariationsFirstPage(any(), any())).thenReturn(
                flowOf(VariationsResult.Remote(Result.success(emptyList())))
            )

            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.init(123L, sourceType = ItemsListSourceType.LIST)
            advanceUntilIdle()
            // WHEN
            viewModel.onUIEvent(WooPosVariationsUIEvents.OnItemClicked(123L, 1L))

            // THEN
            val item = WooPosItemsViewModel.ItemClickedData.Product.Variation(123L, 1L)
            verify(fromChildToParentEventSender).sendToParent(
                ChildToParentEvent.ItemClickedInItemsList(
                    itemData = item,
                    eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                        item = item,
                        source = WooPosAnalyticsEventConstant.ItemsListSource.VARIATION,
                        sourceType = ItemsListSourceType.LIST
                    ),
                )
            )
        }

    @Test
    fun `given search results screen, when variation clicked, then sends event with search results as source`() =
        runTest {
            // GIVEN
            whenever(variationsDataSource.fetchVariationsFirstPage(anyOrNull(), anyOrNull())).thenReturn(
                flowOf(VariationsResult.Remote(Result.success(emptyList())))
            )
            val viewModel = createViewModel(123L, sourceType = ItemsListSourceType.SEARCH_RESULT)
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosVariationsUIEvents.OnItemClicked(123L, 1L))

            // THEN
            val item = WooPosItemsViewModel.ItemClickedData.Product.Variation(123L, 1L)
            verify(fromChildToParentEventSender).sendToParent(
                ChildToParentEvent.ItemClickedInItemsList(
                    itemData = item,
                    eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                        item = item,
                        source = WooPosAnalyticsEventConstant.ItemsListSource.VARIATION,
                        sourceType = ItemsListSourceType.SEARCH_RESULT
                    ),
                )
            )
        }

    @Test
    fun `given variable product, getNameForPOS returns correct name when parent product has variation enabled attributes`() =
        runTest {
            // GIVEN
            val parentProduct = generateWooPosProduct(
                productId = 1L,
                productType = WooPosProductModel.WooPosProductType.Variable,
                attributes = listOf(
                    WooPosProductModel.WooPosProductAttribute(
                        1,
                        "Color",
                        listOf("Blue", "Green", "Red"),
                        isVisible = true,
                        isVariation = true
                    )
                )
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
            val attributeName = variableProduct.toWooPosVariation(
                mapper
            ).getNameForPOS(mapper, parentProduct, resourceProvider)

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
        val wooPosVariation = variableProduct.toWooPosVariation(mapper)
        val attributeName = mapper.getNameForPOS(wooPosVariation, null, resourceProvider)

        // THEN
        assertThat(attributeName).isEqualTo("Color: Blue, Size: M")
    }

    @Test
    fun `given parent product with non-matching attributes, when getNameForPOS is called, then it returns 'Any {attribute}'`() =
        runTest {
            // GIVEN
            val parentProduct = generateWooPosProduct(
                productId = 1L,
                productType = WooPosProductModel.WooPosProductType.Variable,
                attributes = listOf(
                    WooPosProductModel.WooPosProductAttribute(
                        3,
                        "Material",
                        listOf("Cotton", "Polyester"),
                        isVisible = true,
                        isVariation = true
                    )
                )
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
            val attributeName = variableProduct.toWooPosVariation(
                mapper
            ).getNameForPOS(mapper, parentProduct, resourceProvider)

            // THEN
            assertThat(attributeName).isEqualTo("Any Material")
        }

    @Test
    fun `given matching attributes in parent product and variation, when getNameForPOS is called, then it returns the correct attribute names`() =
        runTest {
            // GIVEN
            val parentProduct = generateWooPosProduct(
                productId = 1L,
                productType = WooPosProductModel.WooPosProductType.Variable,
                attributes = listOf(
                    WooPosProductModel.WooPosProductAttribute(
                        1,
                        "Color",
                        listOf("Blue", "Green", "Red"),
                        isVisible = true,
                        isVariation = true
                    ),
                    WooPosProductModel.WooPosProductAttribute(
                        2,
                        "Size",
                        listOf("S", "M", "L"),
                        isVisible = true,
                        isVariation = true
                    )
                )
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
            val attributeName = variableProduct.toWooPosVariation(
                mapper
            ).getNameForPOS(mapper, parentProduct, resourceProvider)

            // THEN
            assertThat(attributeName).isEqualTo("Color: Blue, Size: M")
        }

    @Test
    fun `given attributes with missing options, when getNameForPOS is called, then it returns 'Any {attribute}'`() =
        runTest {
            // GIVEN
            val parentProduct = generateWooPosProduct(
                productId = 1L,
                productType = WooPosProductModel.WooPosProductType.Variable,
                attributes = listOf(
                    WooPosProductModel.WooPosProductAttribute(
                        1,
                        "Color",
                        listOf("Blue", "Green", "Red"),
                        isVisible = true,
                        isVariation = true
                    ),
                    WooPosProductModel.WooPosProductAttribute(
                        2,
                        "Size",
                        listOf("S", "M", "L"),
                        isVisible = true,
                        isVariation = true
                    )
                )
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
            val attributeName = variableProduct.toWooPosVariation(
                mapper
            ).getNameForPOS(mapper, parentProduct, resourceProvider)

            // THEN
            assertThat(attributeName).isEqualTo("Any Color, Any Size")
        }

    @Test
    fun `given variation with no attributes, when getNameForPOS is called, then it returns 'Any {attribute}'`() =
        runTest {
            // GIVEN
            val parentProduct = generateWooPosProduct(
                productId = 1L,
                productType = WooPosProductModel.WooPosProductType.Variable,
                attributes = listOf(
                    WooPosProductModel.WooPosProductAttribute(
                        1,
                        "Color",
                        listOf("Blue", "Green", "Red"),
                        isVisible = true,
                        isVariation = true
                    ),
                )
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
            val attributeName = variableProduct.toWooPosVariation(
                mapper
            ).getNameForPOS(mapper, parentProduct, resourceProvider)

            // THEN
            assertThat(attributeName).isEqualTo("Any Color")
        }

    @Test
    fun `given non-variation-enabled attributes in variation, when getNameForPOS is called, then it ignores those attributes`() =
        runTest {
            // GIVEN
            val parentProduct = generateWooPosProduct(
                productId = 1L,
                productType = WooPosProductModel.WooPosProductType.Variable,
                attributes = listOf(
                    WooPosProductModel.WooPosProductAttribute(
                        1,
                        "Color",
                        listOf("Blue", "Green", "Red"),
                        isVisible = true,
                        isVariation = true
                    ),
                    WooPosProductModel.WooPosProductAttribute(
                        2,
                        "Size",
                        listOf("S", "M", "L"),
                        isVisible = true,
                        isVariation = false
                    )
                )
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
            val attributeName = variableProduct.toWooPosVariation(
                mapper
            ).getNameForPOS(mapper, parentProduct, resourceProvider)

            // THEN
            assertThat(attributeName).isEqualTo("Color: Blue")
        }

    private fun createViewModel(
        productId: Long = 1L,
        sourceType: ItemsListSourceType = ItemsListSourceType.LIST
    ) =
        WooPosVariationsViewModel(
            fromChildToParentEventSender,
            getProductById,
            variationsDataSource,
            priceFormat,
            resourceProvider,
            analyticsTracker,
            mapper,
        ).let {
            it.init(productId, sourceType = sourceType)
            it
        }
}
