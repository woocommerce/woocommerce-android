package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.variations.VariationListViewModel
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.ui.products.variations.domain.GenerateVariationCandidates
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class VariationListViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val variationRepository: VariationRepository = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val productRepository: ProductDetailRepository = mock()
    private val generateVariationCandidates: GenerateVariationCandidates = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()

    private val productRemoteId = 1L
    private lateinit var viewModel: VariationListViewModel
    private val variations = ProductTestUtils.generateProductVariationList(productRemoteId)
    private val savedState = SavedStateHandle()

    @Before
    fun setup() {
        doReturn(true).whenever(networkStatus).isConnected()
        whenever(productRepository.getProduct(productRemoteId)).thenReturn(mock())
    }

    private fun createViewModel() {
        viewModel = spy(
            VariationListViewModel(
                savedState,
                variationRepository,
                productRepository,
                networkStatus,
                currencyFormatter,
                coroutinesTestRule.testDispatchers,
                generateVariationCandidates,
                tracker
            )
        )
    }

    @Test
    fun `Displays the product variation list view correctly`() {
        doReturn(variations).whenever(variationRepository).getProductVariationList(productRemoteId)

        createViewModel()

        val fetchedVariationList = ArrayList<ProductVariation>()
        viewModel.variationList.observeForever { it?.let { fetchedVariationList.addAll(it) } }

        viewModel.start(productRemoteId)
        assertThat(fetchedVariationList).isEqualTo(variations)
    }

    @Test
    fun `Do not fetch product variations from api when not connected`() =
        testBlocking {
            doReturn(false).whenever(networkStatus).isConnected()

            createViewModel()

            var snackbar: ShowSnackbar? = null
            viewModel.event.observeForever {
                if (it is ShowSnackbar) snackbar = it
            }

            viewModel.start(productRemoteId)

            verify(variationRepository, times(1)).getProductVariationList(productRemoteId)
            verify(variationRepository, times(0)).fetchProductVariations(productRemoteId)

            assertThat(snackbar).isEqualTo(ShowSnackbar(string.offline_error))
        }

    @Test
    fun `Shows and hides product variations skeleton correctly`() = testBlocking {
        doReturn(emptyList<ProductVariation>())
            .whenever(variationRepository).getProductVariationList(productRemoteId)

        createViewModel()

        val isSkeletonShown = ArrayList<Boolean>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { isSkeletonShown.add(it) }
        }

        viewModel.start(productRemoteId)
        assertThat(isSkeletonShown).containsExactly(true, false)
    }

    @Test
    fun `Display empty view on fetch product variations error`() = testBlocking {
        whenever(variationRepository.fetchProductVariations(productRemoteId)).thenReturn(null)
        whenever(variationRepository.getProductVariationList(productRemoteId)).thenReturn(null)

        createViewModel()

        val showEmptyView = ArrayList<Boolean>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showEmptyView.add(it) }
        }

        viewModel.start(productRemoteId)

        verify(variationRepository, times(1)).fetchProductVariations(productRemoteId)
        assertThat(showEmptyView).containsExactly(true, false)
    }

    @Test
    fun `When user generate variations then generate variations is tracked`() = testBlocking {
        createViewModel()
        viewModel.onGenerateVariationsConfirmed(emptyList())
        verify(tracker).track(AnalyticsEvent.PRODUCT_VARIATION_GENERATION_REQUESTED)
    }

    @Test
    fun `When user reach variations limits then variations limit error is tracked`() = testBlocking {
        // Given a variation candidates list with a size of [limit]
        val variationsLimit =
            List(GenerateVariationCandidates.VARIATION_CREATION_LIMIT) { id ->
                listOf(VariantOption(id.toLong(), "Number", id.toString()))
            }

        doReturn(variationsLimit).whenever(generateVariationCandidates).invoke(any())
        doReturn(variations).whenever(variationRepository).getProductVariationList(productRemoteId)
        createViewModel()
        viewModel.start(productRemoteId)

        // When AddAllVariations is Clicked
        viewModel.onAddAllVariationsClicked()

        // Then variation limit error is tracked
        verify(tracker).track(
            AnalyticsEvent.PRODUCT_VARIATION_GENERATION_LIMIT_REACHED,
            mapOf(AnalyticsTracker.KEY_VARIATIONS_COUNT to variationsLimit.size)
        )
    }

    @Test
    fun `When user almost reach variations limits then variations limit error is NOT tracked`() = testBlocking {
        // Given a variation candidates list with a size of [limit - 1]
        val almostReachVariationsLimit =
            List(GenerateVariationCandidates.VARIATION_CREATION_LIMIT - 1) { id ->
                listOf(VariantOption(id.toLong(), "Number", id.toString()))
            }
        doReturn(almostReachVariationsLimit).whenever(generateVariationCandidates).invoke(any())
        doReturn(variations).whenever(variationRepository).getProductVariationList(productRemoteId)
        createViewModel()
        viewModel.start(productRemoteId)

        // When AddAllVariations is Clicked
        viewModel.onAddAllVariationsClicked()

        // Then variation limit error is not tracked / sent
        verify(tracker, never()).track(eq(AnalyticsEvent.PRODUCT_VARIATION_GENERATION_LIMIT_REACHED), any())
    }
}
