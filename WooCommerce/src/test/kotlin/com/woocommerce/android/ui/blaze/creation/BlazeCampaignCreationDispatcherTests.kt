package com.woocommerce.android.ui.blaze.creation

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher.BlazeCampaignCreationDispatcherEvent
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting

@OptIn(ExperimentalCoroutinesApi::class)
class BlazeCampaignCreationDispatcherTests : BaseUnitTest() {
    private val productListRepository: ProductListRepository = mock()
    private val blazeRepository: BlazeRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private lateinit var dispatcher: BlazeCampaignCreationDispatcher

    suspend fun setup(
        setupMocks: suspend () -> Unit = {}
    ) {
        setupMocks()
        dispatcher = BlazeCampaignCreationDispatcher(
            blazeRepository = blazeRepository,
            productListRepository = productListRepository,
            coroutineDispatchers = coroutinesTestRule.testDispatchers,
            analyticsTracker = analyticsTracker
        )
    }

    @Test
    fun `given no campaign yet, when starting the flow, then show the intro`() = testBlocking {
        setup {
            whenever(blazeRepository.getMostRecentCampaign()).thenReturn(null)
        }

        var event: BlazeCampaignCreationDispatcherEvent? = null
        dispatcher.startCampaignCreation(source = BlazeFlowSource.MY_STORE_SECTION) { event = it }

        assertThat(event).isEqualTo(
            BlazeCampaignCreationDispatcherEvent.ShowBlazeCampaignCreationIntro(
                productId = null,
                blazeSource = BlazeFlowSource.MY_STORE_SECTION
            )
        )
    }

    @Test
    fun `given a existing campaign and more than 1 published product, when starting the flow, then show product selector`() =
        testBlocking {
            setup {
                whenever(blazeRepository.getMostRecentCampaign()).thenReturn(mock())
                whenever(
                    productListRepository.getProductList(
                        productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                        sortType = ProductSorting.DATE_DESC,
                    )
                ).thenReturn(List(2) { ProductTestUtils.generateProduct(productId = it.toLong()) })
            }

            var event: BlazeCampaignCreationDispatcherEvent? = null
            dispatcher.startCampaignCreation(source = BlazeFlowSource.MY_STORE_SECTION) { event = it }

            assertThat(event).isEqualTo(BlazeCampaignCreationDispatcherEvent.ShowProductSelectorScreen)
        }

    @Test
    fun `given a existing campaign and a given product id, when starting the flow, then show ad creation form`() =
        testBlocking {
            setup {
                whenever(blazeRepository.getMostRecentCampaign()).thenReturn(mock())
                whenever(
                    productListRepository.getProductList(
                        productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                        sortType = ProductSorting.DATE_DESC,
                    )
                ).thenReturn(List(1) { ProductTestUtils.generateProduct(productId = it.toLong()) })
            }

            var event: BlazeCampaignCreationDispatcherEvent? = null
            dispatcher.startCampaignCreation(
                source = BlazeFlowSource.MY_STORE_SECTION,
                productId = 1L
            ) { event = it }

            assertThat(event).isEqualTo(
                BlazeCampaignCreationDispatcherEvent.ShowBlazeCampaignCreationForm(
                    productId = 1L,
                    blazeSource = BlazeFlowSource.MY_STORE_SECTION
                )
            )
        }

    @Test
    fun `given a existing campaign and 1 published product, when starting the flow, then show ad creation form`() =
        testBlocking {
            setup {
                whenever(blazeRepository.getMostRecentCampaign()).thenReturn(mock())
                whenever(
                    productListRepository.getProductList(
                        productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                        sortType = ProductSorting.DATE_DESC,
                    )
                ).thenReturn(List(1) { ProductTestUtils.generateProduct(productId = 1L) })
            }

            var event: BlazeCampaignCreationDispatcherEvent? = null
            dispatcher.startCampaignCreation(source = BlazeFlowSource.MY_STORE_SECTION) { event = it }

            assertThat(event).isEqualTo(
                BlazeCampaignCreationDispatcherEvent.ShowBlazeCampaignCreationForm(
                    productId = 1L,
                    blazeSource = BlazeFlowSource.MY_STORE_SECTION,
                )
            )
        }
}
