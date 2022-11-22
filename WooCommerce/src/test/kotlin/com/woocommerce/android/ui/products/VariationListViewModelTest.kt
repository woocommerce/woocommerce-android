package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.tools.NetworkStatus
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class VariationListViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    lateinit var variationRepository: VariationRepository
    private val currencyFormatter: CurrencyFormatter = mock()
    private val productRepository: ProductDetailRepository = mock()
    private val generateVariationCandidates: GenerateVariationCandidates = mock()

    private val productRemoteId = 1L
    private val procut =
        ProductHelper.getDefaultNewProduct(ProductType.VARIABLE, false).copy(remoteId = productRemoteId)
    private lateinit var viewModel: VariationListViewModel
    private val variations = ProductTestUtils.generateProductVariationList(productRemoteId)
    private val savedState = SavedStateHandle()

    @Before
    fun setup() {
        doReturn(true).whenever(networkStatus).isConnected()
        whenever(productRepository.getProduct(productRemoteId)).thenReturn(procut)

        variationRepository = mock {
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
    fun `Display confirmation dialog if user requested variations generation and generated variations are in limits`() {
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
        whenever(generateVariationCandidates.invoke(any())).thenReturn(variationCandidates)
        createViewModel()
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { event -> events.add(event) }

        // when
        viewModel.start(productRemoteId)
        viewModel.onAddAllVariationsClicked()

        // then
        events
            .last()
            .let { lastEvent ->
                assertThat(lastEvent).isEqualTo(ShowGenerateVariationConfirmation(variationCandidates))
            }
    }

    @Test
    fun `Display limit exceeded error if user requested variations generation above the limit`() {
        // given
        val variationCandidatesAboveLimit =
            (0..GenerateVariationCandidates.VARIATION_CREATION_LIMIT).map {
                listOf(
                    VariantOption(
                        1,
                        "Size",
                        "S"
                    )
                )
            }
        whenever(generateVariationCandidates.invoke(any())).thenReturn(variationCandidatesAboveLimit)
        createViewModel()
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { event -> events.add(event) }

        // when
        viewModel.start(productRemoteId)
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
        createViewModel()
        val states = mutableListOf<ViewState>()
        viewModel.viewStateLiveData.observeForever { _, new -> states.add(new) }

        // when
        viewModel.start(productRemoteId)
        viewModel.onGenerateVariationsConfirmed(emptyList())

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
                        parentProduct = procut,
                        isVariationsOptionsMenuEnabled = false,
                        isBulkUpdateProgressDialogShown = false
                    )
                )
            }
    }

    @Test
    fun `Show error and hide progress bar if variation generation failed`() = testBlocking {
        // given
        variationRepository = mock {
            onBlocking { bulkCreateVariations(any(), any()) } doReturn RequestResult.ERROR
        }
        createViewModel()
        val states = mutableListOf<ViewState>()
        viewModel.viewStateLiveData.observeForever { _, new -> states.add(new) }
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { event -> events.add(event) }

        // when
        viewModel.start(productRemoteId)
        viewModel.onGenerateVariationsConfirmed(emptyList())

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
                        parentProduct = procut,
                        isVariationsOptionsMenuEnabled = false,
                        isBulkUpdateProgressDialogShown = false
                    )
                )
            }
    }
}
