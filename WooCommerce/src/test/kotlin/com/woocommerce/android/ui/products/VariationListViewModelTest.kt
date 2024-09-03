package com.woocommerce.android.ui.products

import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationListFragmentArgs
import com.woocommerce.android.ui.products.variations.VariationListViewModel
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ProgressDialogState
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationConfirmation
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationsError
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationsError.LimitExceeded
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ViewState
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.ui.products.variations.domain.GenerateVariationCandidates
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
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
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class VariationListViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private lateinit var variationRepository: VariationRepository
    private val currencyFormatter: CurrencyFormatter = mock()
    private val productRepository: ProductDetailRepository = mock()
    private val generateVariationCandidates: GenerateVariationCandidates = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()

    private val productRemoteId = 1L
    private val product =
        ProductHelper.getDefaultNewProduct(ProductType.VARIABLE, false).copy(remoteId = productRemoteId)
    private lateinit var viewModel: VariationListViewModel
    private val variations = ProductTestUtils.generateProductVariationList(productRemoteId)
    private val savedState = VariationListFragmentArgs(remoteProductId = productRemoteId).toSavedStateHandle()

    @Before
    fun setup() {
        doReturn(true).whenever(networkStatus).isConnected()
        whenever(productRepository.getProductFromLocalCache(productRemoteId)).thenReturn(product)

        variationRepository = mock {
            onBlocking { fetchProductVariations(any(), any()) } doReturn emptyList()
            onBlocking { bulkCreateVariations(any(), any()) } doReturn RequestResult.SUCCESS
        }
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

        viewModel.start()
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

            viewModel.start()

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

        viewModel.start()
        assertThat(isSkeletonShown).containsExactly(true, false)
    }

    @Test
    fun `Display empty view on fetch product variations error`() = testBlocking {
        whenever(variationRepository.fetchProductVariations(productRemoteId)).thenReturn(emptyList())
        whenever(variationRepository.getProductVariationList(productRemoteId)).thenReturn(emptyList())

        createViewModel()

        val showEmptyView = ArrayList<Boolean>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showEmptyView.add(it) }
        }

        viewModel.start()

        verify(variationRepository, times(1)).fetchProductVariations(productRemoteId)
        assertThat(showEmptyView).containsExactly(true, false)
    }

    @Test
    fun `Display confirmation when generated variations are in limits`() = testBlocking {
        // given
        val variationCandidates = listOf(
            listOf(
                VariantOption(
                    1,
                    "Size",
                    "S"
                )
            )
        )
        createViewModel()
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { event -> events.add(event) }
        whenever(generateVariationCandidates(product)).thenReturn(variationCandidates)

        // when
        viewModel.start()
        viewModel.onAddAllVariationsClicked()

        // then
        events
            .last()
            .let { lastEvent ->
                assertThat(lastEvent).isEqualTo(ShowGenerateVariationConfirmation(variationCandidates))
            }
    }

    @Test
    fun `Display limit exceeded error if user requested variations generation above the limit`() = testBlocking {
        // given
        val variationCandidatesAboveLimit =
            List(GenerateVariationCandidates.VARIATION_CREATION_LIMIT + 1) {
                listOf(
                    VariantOption(
                        1,
                        "Size",
                        "S"
                    )
                )
            }
        createViewModel()
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { event -> events.add(event) }
        whenever(generateVariationCandidates(product)).thenReturn(variationCandidatesAboveLimit)

        // when
        viewModel.start()
        viewModel.onAddAllVariationsClicked()

        // then
        events
            .last()
            .let { lastEvent ->
                assertThat(lastEvent).isEqualTo(LimitExceeded(variationCandidatesAboveLimit.size))
            }
    }

    @Test
    fun `Refresh variations list and hide progress bar if variation generation is successful`() = testBlocking {
        // given
        val variationCandidates = List(5) { id ->
            listOf(VariantOption(id.toLong(), "Number", id.toString()))
        }
        createViewModel()
        val states = mutableListOf<ViewState>()
        viewModel.viewStateLiveData.observeForever { _, new -> states.add(new) }

        // when
        viewModel.start()
        viewModel.onGenerateVariationsConfirmed(variationCandidates)

        // then
        verify(variationRepository, times(2)).getProductVariationList(productRemoteId)
        states
            .last()
            .let { lastState ->
                assertThat(lastState).isEqualTo(
                    ViewState(
                        isSkeletonShown = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        canLoadMore = null,
                        isEmptyViewVisible = true,
                        isWarningVisible = null,
                        progressDialogState = ProgressDialogState.Hidden,
                        parentProduct = product,
                        isVariationsOptionsMenuEnabled = false,
                        isBulkUpdateProgressDialogShown = false
                    )
                )
            }
    }

    @Test
    fun `Show error and hide progress bar if variation generation failed`() = testBlocking {
        // given
        variationRepository.stub {
            onBlocking { bulkCreateVariations(any(), any()) } doReturn RequestResult.ERROR
        }
        val variationCandidates = List(5) { id ->
            listOf(VariantOption(id.toLong(), "Number", id.toString()))
        }
        createViewModel()
        val states = mutableListOf<ViewState>()
        viewModel.viewStateLiveData.observeForever { _, new -> states.add(new) }
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { event -> events.add(event) }

        // when
        viewModel.start()
        viewModel.onGenerateVariationsConfirmed(variationCandidates)

        // then
        events.last()
            .let { lastEvent -> assertThat(lastEvent).isEqualTo(ShowGenerateVariationsError.NetworkError) }

        states
            .last()
            .let { lastState ->
                assertThat(lastState).isEqualTo(
                    ViewState(
                        isSkeletonShown = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        canLoadMore = null,
                        isEmptyViewVisible = true,
                        isWarningVisible = null,
                        progressDialogState = ProgressDialogState.Hidden,
                        parentProduct = product,
                        isVariationsOptionsMenuEnabled = false,
                        isBulkUpdateProgressDialogShown = false
                    )
                )
            }
    }

    @Test
    fun `When user exceed variations limits then variations limit error is tracked`() = testBlocking {
        // Given a variation candidates list with a size of [limit + 1]
        val exceedVariationsLimit =
            List(GenerateVariationCandidates.VARIATION_CREATION_LIMIT + 1) { id ->
                listOf(VariantOption(id.toLong(), "Number", id.toString()))
            }
        doReturn(variations).whenever(variationRepository).getProductVariationList(productRemoteId)
        createViewModel()
        whenever(generateVariationCandidates(product)).thenReturn(exceedVariationsLimit)

        // When AddAllVariations is Clicked
        viewModel.start()
        viewModel.onAddAllVariationsClicked()

        // Then variation request is tracked
        verify(tracker).track(AnalyticsEvent.PRODUCT_VARIATION_GENERATION_REQUESTED)
        // Then variation limit error is tracked
        verify(tracker).track(
            AnalyticsEvent.PRODUCT_VARIATION_GENERATION_LIMIT_REACHED,
            mapOf(AnalyticsTracker.KEY_VARIATIONS_COUNT to exceedVariationsLimit.size)
        )
    }

    @Test
    fun `When user reach variations limits then variations limit error is NOT tracked`() = testBlocking {
        // Given a variation candidates list with a size of [limit]
        val reachVariationsLimit =
            List(GenerateVariationCandidates.VARIATION_CREATION_LIMIT) { id ->
                listOf(VariantOption(id.toLong(), "Number", id.toString()))
            }
        doReturn(variations).whenever(variationRepository).getProductVariationList(productRemoteId)
        createViewModel()
        whenever(generateVariationCandidates(product)).thenReturn(reachVariationsLimit)

        // When AddAllVariations is Clicked
        viewModel.start()
        viewModel.onAddAllVariationsClicked()

        // Then variation request is tracked
        verify(tracker).track(AnalyticsEvent.PRODUCT_VARIATION_GENERATION_REQUESTED)
        // Then variation limit error is NOT tracked
        verify(tracker, never()).track(eq(AnalyticsEvent.PRODUCT_VARIATION_GENERATION_LIMIT_REACHED), any())
    }

    @Test
    fun `When generated variations are successfully created then success event is tracked`() = testBlocking {
        // Given a valid variation candidates list
        val variationCandidates = List(5) { id ->
            listOf(VariantOption(id.toLong(), "Number", id.toString()))
        }
        createViewModel()
        viewModel.start()

        // When AddAllVariations succeed
        viewModel.onGenerateVariationsConfirmed(variationCandidates)

        // Then variation confirmed event is tracked
        verify(tracker).track(
            AnalyticsEvent.PRODUCT_VARIATION_GENERATION_CONFIRMED,
            mapOf(AnalyticsTracker.KEY_VARIATIONS_COUNT to variationCandidates.size)
        )
        // Then variation success is tracked
        verify(tracker).track(AnalyticsEvent.PRODUCT_VARIATION_GENERATION_SUCCESS)
    }

    @Test
    fun `When generated variations failed then failure event is tracked`() = testBlocking {
        // Given a variation candidates list with a size of [5]
        val variationCandidates = List(5) { id ->
            listOf(VariantOption(id.toLong(), "Number", id.toString()))
        }
        createViewModel()
        viewModel.start()
        viewModel.onAddVariationsClicked()

        // When AddAllVariations fails
        variationRepository.stub {
            onBlocking { bulkCreateVariations(any(), any()) } doReturn RequestResult.ERROR
        }
        viewModel.onGenerateVariationsConfirmed(variationCandidates)

        // Then variation confirmed event is tracked
        verify(tracker).track(
            AnalyticsEvent.PRODUCT_VARIATION_GENERATION_CONFIRMED,
            mapOf(AnalyticsTracker.KEY_VARIATIONS_COUNT to variationCandidates.size)
        )
        // Then variation limit error is tracked
        verify(tracker).track(AnalyticsEvent.PRODUCT_VARIATION_GENERATION_FAILURE)
    }

    @Test
    fun `When user requested variation generation but there are no candidates, show error`() = testBlocking {
        // given
        whenever(generateVariationCandidates(product)).thenReturn(emptyList())
        createViewModel()
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { event -> events.add(event) }
        viewModel.start()

        // when
        viewModel.onAddAllVariationsClicked()

        // then
        events
            .last()
            .let { lastEvent ->
                assertThat(lastEvent).isEqualTo(ShowGenerateVariationsError.NoCandidates)
            }
    }
}
