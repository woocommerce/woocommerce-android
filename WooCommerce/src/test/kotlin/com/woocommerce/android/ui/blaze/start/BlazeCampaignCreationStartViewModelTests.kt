package com.woocommerce.android.ui.blaze.start

import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.creation.start.BlazeCampaignCreationStartFragmentArgs
import com.woocommerce.android.ui.blaze.creation.start.BlazeCampaignCreationStartViewModel
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting

@OptIn(ExperimentalCoroutinesApi::class)
class BlazeCampaignCreationStartViewModelTests : BaseUnitTest() {
    private val productListRepository: ProductListRepository = mock()
    private val blazeRepository: BlazeRepository = mock()

    private lateinit var viewModel: BlazeCampaignCreationStartViewModel

    suspend fun setup(
        productId: Long,
        setupMocks: suspend () -> Unit = {}
    ) {
        setupMocks()
        viewModel = BlazeCampaignCreationStartViewModel(
            savedStateHandle = BlazeCampaignCreationStartFragmentArgs(productId).toSavedStateHandle(),
            blazeRepository = blazeRepository,
            productListRepository = productListRepository
        )
    }

    @Test
    fun `given no campaign yet, when starting the flow, then show the intro`() = testBlocking {
        setup(productId = -1L) {
            whenever(blazeRepository.getMostRecentCampaign()).thenReturn(null)
        }

        val event = viewModel.event.value

        assertThat(event).isEqualTo(BlazeCampaignCreationStartViewModel.ShowBlazeCampaignCreationIntro(-1L))
    }

    @Test
    fun `given a existing campaign and more than 1 published product, when starting the flow, then show product selector`() =
        testBlocking {
            setup(productId = -1L) {
                whenever(blazeRepository.getMostRecentCampaign()).thenReturn(mock())
                whenever(
                    productListRepository.getProductList(
                        productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                        sortType = ProductSorting.DATE_DESC,
                    )
                ).thenReturn(List(2) { mock() })
            }

            val event = viewModel.event.value

            assertThat(event).isEqualTo(BlazeCampaignCreationStartViewModel.ShowProductSelectorScreen)
        }

    @Test
    fun `given a existing campaign and a given product id, when starting the flow, then load campaign defaults`() =
        testBlocking {
            setup(productId = 1L) {
                whenever(blazeRepository.getMostRecentCampaign()).thenReturn(mock())
                whenever(
                    productListRepository.getProductList(
                        productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                        sortType = ProductSorting.DATE_DESC,
                    )
                ).thenReturn(List(1) { mock() })
            }

            // TODO implement the test after implementing the AI logic
        }

    @Test
    fun `given a existing campaign and 1 published product, when starting the flow, then make AI call to prepare campaign defaults`() =
        testBlocking {
            setup(productId = -1L) {
                whenever(blazeRepository.getMostRecentCampaign()).thenReturn(mock())
                whenever(
                    productListRepository.getProductList(
                        productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                        sortType = ProductSorting.DATE_DESC,
                    )
                ).thenReturn(List(1) { mock() })
            }

            // TODO implement the test after implementing the AI logic
        }

    @Test
    fun `when picking a product from the product selector, then make AI call to prepare campaign default`() =
        testBlocking {
            setup(productId = -1L) {
                whenever(blazeRepository.getMostRecentCampaign()).thenReturn(mock())
                whenever(
                    productListRepository.getProductList(
                        productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                        sortType = ProductSorting.DATE_DESC,
                    )
                ).thenReturn(List(2) { mock() })
            }

            // TODO implement the test after implementing the AI logic
        }
}
