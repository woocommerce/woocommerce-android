package com.woocommerce.android.ui.blaze.creation.intro

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlazeCampaignCreationIntroViewModelTests : BaseUnitTest() {
    private val productListRepository: ProductListRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: BlazeCampaignCreationIntroViewModel

    suspend fun setup(productId: Long, setupMocks: suspend () -> Unit = {}) {
        setupMocks()
        viewModel = BlazeCampaignCreationIntroViewModel(
            savedStateHandle = BlazeCampaignCreationIntroFragmentArgs(
                productId = productId,
                source = BlazeFlowSource.MY_STORE_SECTION
            ).toSavedStateHandle(),
            productListRepository = productListRepository,
            coroutineDispatchers = coroutinesTestRule.testDispatchers,
            analyticsTracker = analyticsTracker,
        )
    }

    @Test
    fun `given a product id, when tapping continue, then show campaign creation form`() = testBlocking {
        setup(productId = 1L)

        viewModel.onContinueClick()

        val event = viewModel.event.value
        assertThat(event).isEqualTo(
            BlazeCampaignCreationIntroViewModel.ShowCampaignCreationForm(
                productId = 1L,
                source = BlazeFlowSource.INTRO_VIEW
            )
        )
    }

    @Test
    fun `given no product id and a single published product, when tapping continue, then show campaign creation form`() =
        testBlocking {
            setup(productId = -1L) {
                whenever(
                    productListRepository.getProductList(
                        productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                        sortType = ProductSorting.DATE_DESC,
                    )
                ).thenReturn(List(1) { ProductTestUtils.generateProduct(productId = 1L) })
            }

            viewModel.onContinueClick()

            val event = viewModel.event.value
            assertThat(event).isEqualTo(
                BlazeCampaignCreationIntroViewModel.ShowCampaignCreationForm(
                    productId = 1L,
                    source = BlazeFlowSource.INTRO_VIEW
                )
            )
        }

    @Test
    fun `given no product id and multiple published products, when tapping continue, then show product selector`() =
        testBlocking {
            setup(productId = -1L) {
                whenever(
                    productListRepository.getProductList(
                        productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                        sortType = ProductSorting.DATE_DESC,
                    )
                ).thenReturn(List(2) { ProductTestUtils.generateProduct(productId = it.toLong()) })
            }

            viewModel.onContinueClick()

            val event = viewModel.event.value
            assertThat(event).isEqualTo(BlazeCampaignCreationIntroViewModel.ShowProductSelector)
        }

    @Test
    fun `when product selector result is received, then show campaign creation form`() = testBlocking {
        setup(productId = -1L)

        viewModel.onProductSelected(1L)

        val event = viewModel.event.value
        assertThat(event).isEqualTo(
            BlazeCampaignCreationIntroViewModel.ShowCampaignCreationForm(
                productId = 1L,
                source = BlazeFlowSource.INTRO_VIEW
            )
        )
    }

    @Test
    fun `when dismissed, then exit`() = testBlocking {
        setup(productId = 0L)

        viewModel.onDismissClick()

        val event = viewModel.event.value
        assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
    }
}
