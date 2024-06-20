package com.woocommerce.android.ui.products.details

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.media.MediaFilesRepository
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import com.woocommerce.android.ui.products.variations.VariationListViewModel
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.ui.products.variations.domain.GenerateVariationCandidates
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.IsWindowClassLargeThanCompact
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class ProductDetailViewModelGenerateVariationFlowTest : BaseUnitTest() {
    companion object {
        private const val PRODUCT_REMOTE_ID = 1L
    }

    private val wooCommerceStore: WooCommerceStore = mock()
    private val networkStatus: NetworkStatus = mock()
    private lateinit var productRepository: ProductDetailRepository
    private val productCategoriesRepository: ProductCategoriesRepository = mock()
    private val productTagsRepository: ProductTagsRepository = mock()
    private val mediaFilesRepository: MediaFilesRepository = mock()
    private lateinit var variationRepository: VariationRepository
    private val resources: ResourceProvider = mock()
    private val productImagesServiceWrapper: ProductImagesServiceWrapper = mock()
    private val currencyFormatter: CurrencyFormatter = mock()

    private val mediaFileUploadHandler: MediaFileUploadHandler = mock {
        on { it.observeCurrentUploadErrors(any()) } doReturn emptyFlow()
        on { it.observeCurrentUploads(any()) } doReturn flowOf(emptyList())
        on { it.observeSuccessfulUploads(any()) } doReturn emptyFlow()
    }

    private var savedState: SavedStateHandle =
        ProductDetailFragmentArgs(mode = ProductDetailFragment.Mode.ShowProduct(PRODUCT_REMOTE_ID)).toSavedStateHandle()

    private val parameterRepository: ParameterRepository = mock()
    private val generateVariationCandidates: GenerateVariationCandidates = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()

    private val prefs: AppPrefsWrapper = mock()
    private val addonRepository: AddonRepository = mock()

    private val product = ProductTestUtils.generateProduct(PRODUCT_REMOTE_ID)
    private val isWindowClassLargeThanCompact: IsWindowClassLargeThanCompact = mock()

    private var selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel().apply { setIsPrivate(false) }
    }

    private lateinit var viewModel: ProductDetailViewModel

    @Before
    fun setup() {
        doReturn(true).whenever(networkStatus).isConnected()

        productRepository = mock {
            onBlocking { fetchProductOrLoadFromCache(PRODUCT_REMOTE_ID) } doReturn product
        }

        variationRepository = mock {
            onBlocking { bulkCreateVariations(any(), any()) } doReturn RequestResult.SUCCESS
        }

        viewModel = spy(
            ProductDetailViewModel(
                savedState = savedState,
                dispatchers = coroutinesTestRule.testDispatchers,
                parameterRepository = parameterRepository,
                productRepository = productRepository,
                networkStatus = networkStatus,
                currencyFormatter = currencyFormatter,
                resources = resources,
                productCategoriesRepository = productCategoriesRepository,
                productTagsRepository = productTagsRepository,
                mediaFilesRepository = mediaFilesRepository,
                variationRepository = variationRepository,
                mediaFileUploadHandler = mediaFileUploadHandler,
                appPrefsWrapper = prefs,
                addonRepository = addonRepository,
                generateVariationCandidates = generateVariationCandidates,
                duplicateProduct = mock(),
                tracker = tracker,
                selectedSite = selectedSite,
                getBundledProductsCount = mock(),
                getComponentProducts = mock(),
                productListRepository = mock(),
                isBlazeEnabled = mock(),
                isProductCurrentlyPromoted = mock(),
                isWindowClassLargeThanCompact = isWindowClassLargeThanCompact,
            )
        )

        clearInvocations(
            viewModel,
            productRepository,
            networkStatus,
            currencyFormatter,
            wooCommerceStore,
            productImagesServiceWrapper,
            resources,
            productCategoriesRepository,
            productTagsRepository
        )
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
                Assertions.assertThat(lastEvent)
                    .isEqualTo(VariationListViewModel.ShowGenerateVariationConfirmation(variationCandidates))
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
                Assertions.assertThat(lastEvent).isEqualTo(
                    VariationListViewModel.ShowGenerateVariationsError.LimitExceeded(variationCandidatesAboveLimit.size)
                )
            }
    }

    @Test
    fun `Hide progress bar and exit attributes screen if variation generation is successful`() = testBlocking {
        // given
        val variationCandidates = List(5) { id ->
            listOf(VariantOption(id.toLong(), "Number", id.toString()))
        }
        val states = mutableListOf<ProductDetailViewModel.AttributeListViewState>()
        viewModel.attributeListViewStateData.observeForever { _, new -> states.add(new) }
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { event -> events.add(event) }

        // when
        viewModel.start()
        viewModel.onGenerateVariationsConfirmed(variationCandidates)

        // then
        events.last()
            .let { lastEvent ->
                Assertions.assertThat(lastEvent)
                    .isEqualTo(ProductDetailViewModel.ProductExitEvent.ExitAttributesAdded)
            }

        states
            .last()
            .let { lastState ->
                Assertions.assertThat(lastState).isEqualTo(
                    ProductDetailViewModel.AttributeListViewState(
                        isFetchingVariations = false,
                        progressDialogState = VariationListViewModel.ProgressDialogState.Hidden
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
        val states = mutableListOf<ProductDetailViewModel.AttributeListViewState>()
        viewModel.attributeListViewStateData.observeForever { _, new -> states.add(new) }
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { event -> events.add(event) }

        // when
        viewModel.start()
        viewModel.onGenerateVariationsConfirmed(variationCandidates)
        // then
        events.last()
            .let { lastEvent ->
                Assertions.assertThat(lastEvent)
                    .isEqualTo(VariationListViewModel.ShowGenerateVariationsError.NetworkError)
            }

        states
            .last()
            .let { lastState ->
                Assertions.assertThat(lastState).isEqualTo(
                    ProductDetailViewModel.AttributeListViewState(
                        isFetchingVariations = false,
                        progressDialogState = VariationListViewModel.ProgressDialogState.Hidden
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
        viewModel.start()
        viewModel.onGenerateVariationClicked()

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
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { event -> events.add(event) }
        viewModel.start()

        // when
        viewModel.onAddAllVariationsClicked()

        // then
        events
            .last()
            .let { lastEvent ->
                Assertions.assertThat(lastEvent)
                    .isEqualTo(VariationListViewModel.ShowGenerateVariationsError.NoCandidates)
            }
    }
}
