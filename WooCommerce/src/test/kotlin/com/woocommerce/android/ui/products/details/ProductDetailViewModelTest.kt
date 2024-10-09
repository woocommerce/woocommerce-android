package com.woocommerce.android.ui.products.details

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.media.MediaFilesRepository
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.ProductAggregate
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.customfields.CustomFieldsRepository
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.settings.ProductVisibility
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.ui.products.variations.domain.GenerateVariationCandidates
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.IsWindowClassLargeThanCompact
import com.woocommerce.android.util.ProductUtils
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class ProductDetailViewModelTest : BaseUnitTest() {
    companion object {
        private const val PRODUCT_REMOTE_ID = 1L
        private const val OFFLINE_PRODUCT_REMOTE_ID = 2L
        private val SALE_END_DATE = Date.from(
            LocalDateTime.of(2020, 4, 1, 8, 0)
                .toInstant(ZoneOffset.UTC)
        )
    }

    private val wooCommerceStore: WooCommerceStore = mock()
    private val networkStatus: NetworkStatus = mock()
    private val productRepository: ProductDetailRepository = mock()
    private val productCategoriesRepository: ProductCategoriesRepository = mock()
    private val productTagsRepository: ProductTagsRepository = mock()
    private val mediaFilesRepository: MediaFilesRepository = mock()
    private val variationRepository: VariationRepository = mock()

    private val resources: ResourceProvider = mock {
        on(it.getString(any())).thenAnswer { i -> i.arguments[0].toString() }
        on(it.getString(any(), any())).thenAnswer { i -> i.arguments[0].toString() }
    }
    private val productImagesServiceWrapper: ProductImagesServiceWrapper = mock()
    private val currencyFormatter: CurrencyFormatter = mock {
        on(it.formatCurrency(any<BigDecimal>(), any(), any())).thenAnswer { i -> "${i.arguments[1]}${i.arguments[0]}" }
    }
    private var mediaFileUploadHandler: MediaFileUploadHandler = mock {
        on { it.observeCurrentUploadErrors(any()) } doReturn emptyFlow()
        on { it.observeCurrentUploads(any()) } doReturn flowOf(emptyList())
        on { it.observeSuccessfulUploads(any()) } doReturn emptyFlow()
    }
    private val addonRepository: AddonRepository = mock {
        onBlocking { hasAnyProductSpecificAddons(any()) } doReturn false
    }

    private val isBlazeEnabled: IsBlazeEnabled = mock {
        onBlocking { invoke() } doReturn false
    }

    private var savedState: SavedStateHandle =
        ProductDetailFragmentArgs(ProductDetailFragment.Mode.ShowProduct(PRODUCT_REMOTE_ID)).toSavedStateHandle()

    private val siteParams = SiteParameters(
        currencyCode = "USD",
        currencySymbol = "$",
        currencyFormattingParameters = null,
        weightUnit = "kg",
        dimensionUnit = "cm",
        gmtOffset = 0f
    )
    private val parameterRepository: ParameterRepository = mock {
        on(it.getParameters(any(), any<SavedStateHandle>())).thenReturn(siteParams)
    }
    private val generateVariationCandidates: GenerateVariationCandidates = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()

    private val prefsWrapper: AppPrefsWrapper = mock()
    private val productUtils = ProductUtils()

    private val productAggregate = ProductAggregate(ProductTestUtils.generateProduct(PRODUCT_REMOTE_ID))
    private val productWithTagsAndCategories = ProductTestUtils.generateProductWithTagsAndCategories(PRODUCT_REMOTE_ID)
    private val offlineProduct = ProductTestUtils.generateProduct(OFFLINE_PRODUCT_REMOTE_ID)
    private val productCategories = ProductTestUtils.generateProductCategories()
    private val isWindowClassLargeThanCompact: IsWindowClassLargeThanCompact = mock()
    private val determineProductPasswordApi: DetermineProductPasswordApi = mock()
    private val customFieldsRepository: CustomFieldsRepository = mock {
        onBlocking { hasDisplayableCustomFields(any()) } doReturn false
    }

    private lateinit var viewModel: ProductDetailViewModel

    private var selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel().apply { setIsPrivate(false) }
    }

    private val productWithParameters = ProductDetailViewModel.ProductDetailViewState(
        productAggregateDraft = productAggregate,
        auxiliaryState = ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.None,
        uploadingImageUris = emptyList(),
        showBottomSheetButton = true,
        areImagesAvailable = true,
    )

    private val expectedCards = listOf(
        ProductPropertyCard(
            type = ProductPropertyCard.Type.PRIMARY,
            properties = listOf(
                ProductProperty.Editable(R.string.product_detail_title_hint, productAggregate.product.name),
                ProductProperty.ComplexProperty(R.string.product_description, productAggregate.product.description)
            )
        ),
        ProductPropertyCard(
            type = ProductPropertyCard.Type.SECONDARY,
            properties = listOf(
                ProductProperty.PropertyGroup(
                    R.string.product_price,
                    mapOf(
                        Pair(
                            resources.getString(R.string.product_regular_price),
                            currencyFormatter.formatCurrency(
                                productWithParameters.productDraft?.regularPrice ?: BigDecimal.ZERO,
                                siteParams.currencyCode ?: ""
                            )
                        ),
                        Pair(
                            resources.getString(R.string.product_sale_price),
                            currencyFormatter.formatCurrency(
                                productWithParameters.productDraft?.salePrice ?: BigDecimal.ZERO,
                                siteParams.currencyCode ?: ""
                            )
                        )
                    ),
                    R.drawable.ic_gridicons_money
                ),
                ProductProperty.RatingBar(
                    R.string.product_reviews,
                    resources.getString(R.string.product_ratings_count, productAggregate.product.ratingCount),
                    productAggregate.product.averageRating,
                    R.drawable.ic_reviews
                ),
                ProductProperty.PropertyGroup(
                    R.string.product_inventory,
                    mapOf(
                        Pair(
                            resources.getString(R.string.product_stock_status),
                            resources.getString(R.string.product_stock_status_instock)
                        )
                    ),
                    R.drawable.ic_gridicons_list_checkmark,
                    true
                ),
                ProductProperty.PropertyGroup(
                    R.string.product_shipping,
                    mapOf(
                        Pair(
                            resources.getString(R.string.product_weight),
                            productWithParameters.productDraft?.getWeightWithUnits(siteParams.weightUnit) ?: ""
                        ),
                        Pair(
                            resources.getString(R.string.product_dimensions),
                            productWithParameters.productDraft?.getSizeWithUnits(siteParams.dimensionUnit) ?: ""
                        ),
                        Pair(resources.getString(R.string.product_shipping_class), ""),
                        Pair(resources.getString(R.string.subscription_one_time_shipping), "")
                    ),
                    R.drawable.ic_gridicons_shipping,
                    true
                ),
                ProductProperty.ComplexProperty(
                    R.string.product_categories,
                    productWithTagsAndCategories.categories.joinToString(transform = { it.name }),
                    R.drawable.ic_gridicons_folder,
                    maxLines = 5
                ),
                ProductProperty.ComplexProperty(
                    R.string.product_tags,
                    productWithTagsAndCategories.tags.joinToString(transform = { it.name }),
                    R.drawable.ic_gridicons_tag,
                    maxLines = 5
                ),
                ProductProperty.ComplexProperty(
                    R.string.product_short_description,
                    productAggregate.product.shortDescription,
                    R.drawable.ic_gridicons_align_left
                ),
                ProductProperty.ComplexProperty(
                    R.string.product_downloadable_files,
                    resources.getString(R.string.product_downloadable_files_value_single),
                    R.drawable.ic_gridicons_cloud
                ),
                ProductProperty.ComplexProperty(
                    R.string.product_type,
                    resources.getString(R.string.product_detail_product_type_hint),
                    R.drawable.ic_gridicons_product,
                    true
                )
            )
        )
    )

    @Before
    fun setup() {
        doReturn(true).whenever(networkStatus).isConnected()

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
                appPrefsWrapper = prefsWrapper,
                addonRepository = addonRepository,
                generateVariationCandidates = generateVariationCandidates,
                duplicateProduct = mock(),
                tracker = tracker,
                selectedSite = selectedSite,
                getBundledProductsCount = mock(),
                getComponentProducts = mock(),
                productListRepository = mock(),
                isBlazeEnabled = isBlazeEnabled,
                isProductCurrentlyPromoted = mock(),
                isWindowClassLargeThanCompact = isWindowClassLargeThanCompact,
                determineProductPasswordApi = determineProductPasswordApi,
                customFieldsRepository = customFieldsRepository
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
    fun `Displays the product detail properties correctly`() = testBlocking {
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(ProductAggregate(productWithTagsAndCategories)).whenever(productRepository).getProductAggregate(any())

        viewModel.productDetailViewStateData.observeForever { _, _ -> }

        var cards: List<ProductPropertyCard>? = null
        viewModel.productDetailCards.observeForever {
            cards = it.map { card -> productUtils.stripCallbacks(card) }
        }

        viewModel.start()

        Assertions.assertThat(cards).isEqualTo(expectedCards)
    }

    @Test
    fun `Displays the product detail view correctly`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        doReturn(productAggregate).whenever(productRepository).fetchAndGetProductAggregate(any())

        var productData: ProductDetailViewModel.ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start()

        Assertions.assertThat(productData).isEqualTo(productWithParameters)
    }

    @Test
    fun `given nothing returned from repo, when view model started, the error status emitted`() = testBlocking {
        whenever(productRepository.fetchAndGetProductAggregate(PRODUCT_REMOTE_ID)).thenReturn(null)
        whenever(productRepository.getProductAggregate(PRODUCT_REMOTE_ID)).thenReturn(null)

        viewModel.start()

        verify(productRepository, times(1)).fetchAndGetProductAggregate(PRODUCT_REMOTE_ID)

        Assertions.assertThat(viewModel.getProduct().productDraft).isNull()
        Assertions.assertThat(viewModel.getProduct().auxiliaryState).isEqualTo(
            ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.Error(
                R.string.product_detail_fetch_product_error
            )
        )
    }

    @Test
    fun `given nothing returned from repo with INVALID_PRODUCT_ID error, when view model started, the error status emitted with invalid id text`() =
        testBlocking {
            whenever(productRepository.fetchAndGetProductAggregate(PRODUCT_REMOTE_ID)).thenReturn(null)
            whenever(productRepository.getProductAggregate(PRODUCT_REMOTE_ID)).thenReturn(null)
            whenever(productRepository.lastFetchProductErrorType).thenReturn(
                WCProductStore.ProductErrorType.INVALID_PRODUCT_ID
            )

            viewModel.start()

            verify(productRepository, times(1)).fetchAndGetProductAggregate(PRODUCT_REMOTE_ID)

            Assertions.assertThat(viewModel.getProduct().productDraft).isNull()
            Assertions.assertThat(viewModel.getProduct().auxiliaryState).isEqualTo(
                ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.Error(
                    R.string.product_detail_fetch_product_invalid_id_error
                )
            )
        }

    @Test
    fun `Do not fetch product from api when not connected`() = testBlocking {
        doReturn(ProductAggregate(offlineProduct)).whenever(productRepository).getProductAggregate(any())
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: MultiLiveEvent.Event.ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is MultiLiveEvent.Event.ShowSnackbar) snackbar = it
        }

        viewModel.start()

        verify(productRepository, times(1)).getProductAggregate(PRODUCT_REMOTE_ID)
        verify(productRepository, times(0)).fetchAndGetProductAggregate(any())

        Assertions.assertThat(snackbar).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
    }

    @Test
    fun `Shows and hides product detail skeleton correctly`() = testBlocking {
        doReturn(null).whenever(productRepository).getProductAggregate(any())
        doReturn(productAggregate).whenever(productRepository).fetchAndGetProductAggregate(any())

        val auxiliaryStates = ArrayList<ProductDetailViewModel.ProductDetailViewState.AuxiliaryState>()
        viewModel.productDetailViewStateData.observeForever { old, new ->
            new.auxiliaryState.takeIfNotEqualTo(old?.auxiliaryState) { auxiliaryStates.add(it) }
        }

        viewModel.start()

        Assertions.assertThat(auxiliaryStates).containsExactly(
            ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.Error(
                R.string.product_detail_fetch_product_error
            ),
            ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.Loading,
            ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.None,
        )
    }

    @Test
    fun `Displays the updated product detail view correctly`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        doReturn(productAggregate).whenever(productRepository).fetchAndGetProductAggregate(any())

        var productData: ProductDetailViewModel.ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start()

        Assertions.assertThat(productData).isEqualTo(productWithParameters)

        val updatedDescription = "Updated product description"
        viewModel.updateProductDraft(updatedDescription)

        Assertions.assertThat(productData?.productDraft?.description).isEqualTo(updatedDescription)
    }

    @Test
    fun `When update product price is null, product detail view displayed correctly`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        doReturn(productAggregate).whenever(productRepository).fetchAndGetProductAggregate(any())

        var productData: ProductDetailViewModel.ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start()

        Assertions.assertThat(productData).isEqualTo(productWithParameters)

        val updatedRegularPrice = null
        val updatedSalePrice = null
        viewModel.updateProductDraft(
            regularPrice = updatedRegularPrice,
            salePrice = updatedSalePrice
        )

        assertNull(productData?.productDraft?.regularPrice)
        assertNull(productData?.productDraft?.salePrice)
    }

    @Test
    fun `When update product price is zero, product detail view displayed correctly`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        doReturn(productAggregate).whenever(productRepository).fetchAndGetProductAggregate(any())

        var productData: ProductDetailViewModel.ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start()

        Assertions.assertThat(productData).isEqualTo(productWithParameters)

        val updatedRegularPrice = BigDecimal.ZERO
        val updatedSalePrice = BigDecimal.ZERO
        viewModel.updateProductDraft(
            regularPrice = updatedRegularPrice,
            salePrice = updatedSalePrice
        )

        Assertions.assertThat(productData?.productDraft?.regularPrice).isEqualTo(updatedRegularPrice)
        Assertions.assertThat(productData?.productDraft?.salePrice).isEqualTo(updatedSalePrice)
    }

    @Test
    fun `Displays update menu action if product is edited`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        viewModel.productDetailViewStateData.observeForever { _, _ -> }

        var hasChanges: Boolean? = null
        viewModel.hasChanges.observeForever { hasChanges = it }

        viewModel.start()

        Assertions.assertThat(hasChanges).isFalse()

        val updatedDescription = "Updated product description"
        viewModel.updateProductDraft(updatedDescription)

        Assertions.assertThat(hasChanges).isTrue()
    }

    @Test
    fun `Displays progress dialog when product is edited`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        doReturn(Pair(false, null)).whenever(productRepository).updateProduct(any<ProductAggregate>())

        val isProgressDialogShown = ArrayList<Boolean>()
        viewModel.productDetailViewStateData.observeForever { old, new ->
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                isProgressDialogShown.add(it)
            }
        }

        viewModel.start()

        viewModel.onSaveButtonClicked()

        Assertions.assertThat(isProgressDialogShown).containsExactly(true, false)
    }

    @Test
    fun `Do not update product when not connected`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: MultiLiveEvent.Event.ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is MultiLiveEvent.Event.ShowSnackbar) snackbar = it
        }

        var productData: ProductDetailViewModel.ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start()

        viewModel.onSaveButtonClicked()

        verify(productRepository, times(0)).updateProduct(any<ProductAggregate>())
        Assertions.assertThat(snackbar).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        Assertions.assertThat(productData?.isProgressDialogShown).isFalse()
    }

    @Test
    fun `Display error message on generic update product error`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        doReturn(Pair(false, WCProductStore.ProductError())).whenever(productRepository)
            .updateProduct(any<ProductAggregate>())

        var snackbar: MultiLiveEvent.Event.ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is MultiLiveEvent.Event.ShowSnackbar) snackbar = it
        }

        var productData: ProductDetailViewModel.ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start()

        viewModel.onSaveButtonClicked()

        verify(productRepository, times(1)).updateProduct(any<ProductAggregate>())
        Assertions.assertThat(snackbar)
            .isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.product_detail_update_product_error))
        Assertions.assertThat(productData?.isProgressDialogShown).isFalse()
    }

    @Test
    fun `Display error message on min-max quantities update product error`() = testBlocking {
        val displayErrorMessage = "This is an error message"
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        doReturn(
            Pair(
                false,
                WCProductStore.ProductError(
                    type = WCProductStore.ProductErrorType.INVALID_MIN_MAX_QUANTITY,
                    message = displayErrorMessage
                )
            )
        )
            .whenever(productRepository).updateProduct(any<ProductAggregate>())

        var showUpdateProductError: ProductDetailViewModel.ShowUpdateProductError? = null
        viewModel.event.observeForever {
            if (it is ProductDetailViewModel.ShowUpdateProductError) showUpdateProductError = it
        }

        var productData: ProductDetailViewModel.ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start()

        viewModel.onSaveButtonClicked()

        verify(productRepository, times(1)).updateProduct(any<ProductAggregate>())
        Assertions.assertThat(showUpdateProductError)
            .isEqualTo(ProductDetailViewModel.ShowUpdateProductError(displayErrorMessage))
        Assertions.assertThat(productData?.isProgressDialogShown).isFalse()
    }

    @Test
    fun `Display success message on update product success`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        doReturn(Pair(true, null)).whenever(productRepository).updateProduct(any<ProductAggregate>())

        var successSnackbarShown = false
        viewModel.event.observeForever {
            if (it is MultiLiveEvent.Event.ShowSnackbar && it.message == R.string.product_detail_save_product_success) {
                successSnackbarShown = true
            }
        }

        var productData: ProductDetailViewModel.ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        var hasChanges: Boolean? = null
        viewModel.hasChanges.observeForever { hasChanges = it }

        viewModel.start()

        viewModel.onSaveButtonClicked()

        verify(productRepository, times(1)).updateProduct(any<ProductAggregate>())
        verify(productRepository, times(2)).getProductAggregate(PRODUCT_REMOTE_ID)

        Assertions.assertThat(successSnackbarShown).isTrue()
        Assertions.assertThat(productData?.isProgressDialogShown).isFalse
        Assertions.assertThat(hasChanges).isFalse()
        Assertions.assertThat(productData?.productAggregateDraft).isEqualTo(productAggregate)
    }

    @Test
    fun `Correctly sorts the Product Categories By their Parent Ids and by name`() {
        testBlocking {
            val sortedByNameAndParent = viewModel.sortAndStyleProductCategories(
                productAggregate.product,
                productCategories
            ).toList()
            Assertions.assertThat(sortedByNameAndParent[0].category).isEqualTo(productCategories[0])
            Assertions.assertThat(sortedByNameAndParent[1].category).isEqualTo(productCategories[7])
            Assertions.assertThat(sortedByNameAndParent[2].category).isEqualTo(productCategories[10])
            Assertions.assertThat(sortedByNameAndParent[3].category).isEqualTo(productCategories[1])
            Assertions.assertThat(sortedByNameAndParent[4].category).isEqualTo(productCategories[6])
            Assertions.assertThat(sortedByNameAndParent[5].category).isEqualTo(productCategories[8])
            Assertions.assertThat(sortedByNameAndParent[6].category).isEqualTo(productCategories[9])
            Assertions.assertThat(sortedByNameAndParent[7].category).isEqualTo(productCategories[2])
            Assertions.assertThat(sortedByNameAndParent[8].category).isEqualTo(productCategories[3])
            Assertions.assertThat(sortedByNameAndParent[9].category).isEqualTo(productCategories[5])
            Assertions.assertThat(sortedByNameAndParent[10].category).isEqualTo(productCategories[4])
        }
    }

    @Test
    fun `Displays the trash confirmation dialog correctly`() {
        viewModel.start()
        viewModel.onTrashButtonClicked()

        var trashDialogShown = false
        viewModel.event.observeForever {
            if (it is MultiLiveEvent.Event.ShowDialog && it.messageId == R.string.product_confirm_trash) {
                trashDialogShown = true
            }
        }

        Assertions.assertThat(trashDialogShown).isTrue()
    }

    @Test
    fun `Do not enable trashing a product when in add product flow`() {
        viewModel.start()
        doReturn(true).whenever(viewModel).isProductUnderCreation
        Assertions.assertThat(viewModel.isTrashEnabled).isFalse()
    }

    @Test
    fun `Display offline message and don't show trash confirmation dialog when not connected`() {
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: MultiLiveEvent.Event.ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is MultiLiveEvent.Event.ShowSnackbar) snackbar = it
        }

        var isTrashDialogShown = false
        viewModel.productDetailViewStateData.observeForever { _, new ->
            new.isConfirmingTrash.takeIfNotEqualTo(false) {
                isTrashDialogShown = true
            }
        }

        viewModel.start()
        viewModel.onTrashButtonClicked()

        Assertions.assertThat(snackbar).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        Assertions.assertThat(isTrashDialogShown).isFalse()
    }

    @Test
    fun `Should update view state with not null sale end date when sale is scheduled`() = testBlocking {
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())

        viewModel.start()
        viewModel.updateProductDraft(saleEndDate = SALE_END_DATE, isSaleScheduled = true)

        Assertions.assertThat(productsDraft?.saleEndDateGmt).isEqualTo(SALE_END_DATE)
    }

    @Test
    fun `Should update with stored product sale end date when sale is not scheduled`() = testBlocking {
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())

        viewModel.start()
        viewModel.updateProductDraft(saleEndDate = SALE_END_DATE, isSaleScheduled = false)

        Assertions.assertThat(productsDraft?.saleEndDateGmt).isEqualTo(productAggregate.product.saleEndDateGmt)
    }

    @Test
    fun `Should update sale end date when sale schedule is unknown but stored product sale is scheduled`() =
        testBlocking {
            viewModel.productDetailViewStateData.observeForever { _, _ -> }
            val storedProductAggregate = productAggregate.copy(
                product = productAggregate.product.copy(isSaleScheduled = true)
            )
            doReturn(storedProductAggregate).whenever(productRepository).getProductAggregate(any())

            viewModel.start()
            viewModel.updateProductDraft(saleEndDate = SALE_END_DATE, isSaleScheduled = null)

            Assertions.assertThat(productsDraft?.saleEndDateGmt).isEqualTo(SALE_END_DATE)
        }

    @Test
    fun `Should update with null sale end date and stored product has scheduled sale`() = testBlocking {
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        val storedProductAggregate = productAggregate.copy(
            product = productAggregate.product.copy(
                saleEndDateGmt = SALE_END_DATE,
                isSaleScheduled = true
            )
        )
        doReturn(storedProductAggregate).whenever(productRepository).getProductAggregate(any())

        viewModel.start()
        viewModel.updateProductDraft(saleEndDate = null)

        Assertions.assertThat(productsDraft?.saleEndDateGmt).isNull()
    }

    @Test
    fun `Re-ordering attribute terms is saved correctly`() = testBlocking {
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        val storedProductAggregate = productAggregate.copy(
            product = productAggregate.product.copy(
                attributes = ProductTestUtils.generateProductAttributeList()
            )
        )
        doReturn(storedProductAggregate).whenever(productRepository).getProductAggregate(any())

        val attribute = storedProductAggregate.product.attributes[0]
        val firstTerm = attribute.terms[0]
        val secondTerm = attribute.terms[1]

        viewModel.start()
        viewModel.swapProductDraftAttributeTerms(
            attribute.id,
            attribute.name,
            firstTerm,
            secondTerm
        )

        val draftAttribute = viewModel.productDraftAttributes[0]
        val draftTerms = draftAttribute.terms
        Assertions.assertThat(draftTerms[0]).isEqualTo(secondTerm)
        Assertions.assertThat(draftTerms[1]).isEqualTo(firstTerm)
    }

    @Test
    fun `Re-name attribute terms is saved correctly`() = testBlocking {
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        val attributeName = "name"
        val newName = attributeName.replaceFirstChar { it.uppercase() }

        val attributes = ArrayList<ProductAttribute>()
        attributes.add(
            ProductAttribute(
                id = 1,
                name = attributeName,
                isVariation = true,
                isVisible = true,
                terms = ArrayList<String>().also {
                    it.add("one")
                }
            )
        )

        val storedProductAggregate = productAggregate.copy(
            product = productAggregate.product.copy(attributes = attributes)
        )
        doReturn(storedProductAggregate).whenever(productRepository).getProductAggregate(any())

        viewModel.start()
        viewModel.renameAttributeInDraft(1, attributeName, newName)

        val draftAttribute = viewModel.productDraftAttributes[0]
        Assertions.assertThat(draftAttribute.name).isEqualTo(newName)
    }

    /**
     * Protection for a race condition bug in Variations.
     *
     * We're requiring [ProductDetailRepository.fetchAndGetProductAggregate] to be called right after
     * [VariationRepository.createEmptyVariation] to fix a race condition problem in the Product Details page. The
     * bug can be reproduced inconsistently by following these steps:
     *
     * 1. Create a new variable product.
     * 2. Follow the flow of adding a variation until you've generated a variation.
     * 3. You'll be navigated back to the Product Details page. And you'll see “Variations” with 1 count.
     * 4. Navigate to the variations list by tapping on “Variations”.
     * 5. Navigate back. The Variations count will sometimes be zero.
     *
     * The reason for this is that [VariationListViewModel.onExit] dictates what the number of variations should be
     * displayed in the Product Detail page. So if the [VariationListViewModel] does not have the recent product
     * information, it will incorrectly update the variations count.
     *
     * This can be inconsistent because, sometimes, there's a random _updating_ of the underlying Product
     * like in [ProductDetailViewModel.saveAttributeChanges], which eventually fixes the variations count.
     *
     * Fetching right after generating a variation ensures that we have the latest Product information
     * (and variations count) from the API.
     */
    @Test
    fun `When generating a variation, the latest Product should be fetched from the site`() =
        testBlocking {
            // Given
            doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())

            var productData: ProductDetailViewModel.ProductDetailViewState? = null
            viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

            viewModel.start()

            clearInvocations(productRepository)

            // Precondition
            Assertions.assertThat(productData?.productDraft?.numVariations).isZero()

            doReturn(mock<ProductVariation>()).whenever(variationRepository).createEmptyVariation(any())
            doReturn(productAggregate.copy(product = productAggregate.product.copy(numVariations = 1_914)))
                .whenever(productRepository).fetchAndGetProductAggregate(eq(productAggregate.product.remoteId))

            // When
            viewModel.onGenerateVariationClicked()

            // Then
            verify(variationRepository, times(1)).createEmptyVariation(eq(productAggregate.product))
            // Prove that we fetched from the API.
            verify(productRepository, times(1)).fetchAndGetProductAggregate(eq(productAggregate.remoteId))

            // The VM state should have been updated with the _fetched_ product's numVariations
            Assertions.assertThat(productData?.productDraft?.numVariations).isEqualTo(1_914)
        }

    @Test
    fun `when there image upload errors, then show a snackbar`() = testBlocking {
        val errorEvents = MutableSharedFlow<List<MediaFileUploadHandler.ProductImageUploadData>>()
        doReturn(errorEvents).whenever(mediaFileUploadHandler).observeCurrentUploadErrors(PRODUCT_REMOTE_ID)
        doReturn(productAggregate).whenever(productRepository).fetchAndGetProductAggregate(any())
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        val errorMessage = "message"
        doReturn(errorMessage).whenever(resources).getString(any(), anyVararg())

        viewModel.start()
        val errors = listOf(
            MediaFileUploadHandler.ProductImageUploadData(
                PRODUCT_REMOTE_ID,
                "uri",
                MediaFileUploadHandler.UploadStatus.Failed(
                    mediaErrorType = MediaStore.MediaErrorType.GENERIC_ERROR,
                    mediaErrorMessage = "error"
                )
            )
        )
        errorEvents.emit(errors)

        Assertions.assertThat(viewModel.event.value).matches {
            it is MultiLiveEvent.Event.ShowActionSnackbar &&
                it.message == errorMessage
        }
    }

    @Test
    fun `when image uploads gets cleared, then auto-dismiss the snackbar`() = testBlocking {
        val errorEvents = MutableSharedFlow<List<MediaFileUploadHandler.ProductImageUploadData>>()
        doReturn(errorEvents).whenever(mediaFileUploadHandler).observeCurrentUploadErrors(PRODUCT_REMOTE_ID)
        doReturn(productAggregate).whenever(productRepository).fetchAndGetProductAggregate(any())
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())

        viewModel.start()
        errorEvents.emit(emptyList())

        Assertions.assertThat(viewModel.event.value).isEqualTo(ProductDetailViewModel.HideImageUploadErrorSnackbar)
    }

    @Test
    fun `Publish option not shown when product is published except addProduct flow`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        var menuButtonsState: ProductDetailViewModel.MenuButtonsState? = null
        viewModel.menuButtonsState.observeForever { menuButtonsState = it }

        viewModel.start()
        viewModel.updateProductDraft(productStatus = ProductStatus.PUBLISH)
        Assertions.assertThat(menuButtonsState?.publishOption).isFalse
    }

    @Test
    fun `Publish option not shown when product is published privately except addProduct flow`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        var menuButtonsState: ProductDetailViewModel.MenuButtonsState? = null
        viewModel.menuButtonsState.observeForever { menuButtonsState = it }

        viewModel.start()
        viewModel.updateProductDraft(productStatus = ProductStatus.PRIVATE)
        Assertions.assertThat(menuButtonsState?.publishOption).isFalse
    }

    @Test
    fun `Publish option shown when product is Draft`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        viewModel.productDetailViewStateData.observeForever { _, _ -> }

        var menuButtonsState: ProductDetailViewModel.MenuButtonsState? = null
        viewModel.menuButtonsState.observeForever { menuButtonsState = it }

        viewModel.start()
        viewModel.updateProductDraft(productStatus = ProductStatus.DRAFT)
        Assertions.assertThat(menuButtonsState?.publishOption).isTrue
    }

    @Test
    fun `Publish option shown when product is Pending Review`() = testBlocking {
        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
        viewModel.productDetailViewStateData.observeForever { _, _ -> }

        var menuButtonsState: ProductDetailViewModel.MenuButtonsState? = null
        viewModel.menuButtonsState.observeForever { menuButtonsState = it }

        viewModel.start()
        viewModel.updateProductDraft(productStatus = ProductStatus.PENDING)
        Assertions.assertThat(menuButtonsState?.publishOption).isTrue()
    }

    @Test
    fun `Save option shown when product has changes except add product flow irrespective of product statuses`() =
        testBlocking {
            doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())
            viewModel.productDetailViewStateData.observeForever { _, _ -> }

            var menuButtonsState: ProductDetailViewModel.MenuButtonsState? = null
            viewModel.menuButtonsState.observeForever { menuButtonsState = it }

            viewModel.start()
            // Trigger changes
            viewModel.updateProductDraft(title = productAggregate.product.name + "2")

            Assertions.assertThat(menuButtonsState?.saveOption).isTrue()
        }

    @Test
    fun `when restoring saved state, then re-fetch stored product to correctly calculate hasChanges`() = testBlocking {
        // Make sure draft product has different data than draft product
        doReturn(
            productAggregate.copy(
                product = productAggregate.product.copy(name = productAggregate.product.name + "test")
            )
        ).whenever(productRepository).getProductAggregate(any())
        savedState.set(ProductDetailViewModel.ProductDetailViewState::class.java.name, productWithParameters)
        viewModel.productDetailViewStateData.observeForever { _, _ -> }

        var hasChanges: Boolean? = null
        viewModel.hasChanges.observeForever { hasChanges = it }

        viewModel.start()

        Assertions.assertThat(hasChanges).isTrue
    }

    @Test
    fun `given regular price set, when updating inventory, then price remains unchanged`() = testBlocking {
        doReturn(
            productAggregate.copy(product = productAggregate.product.copy(regularPrice = BigDecimal(99)))
        ).whenever(productRepository).getProductAggregate(any())
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        viewModel.start()

        viewModel.updateProductDraft(sku = "E9999999")

        Assertions.assertThat(viewModel.getProduct().productDraft?.regularPrice).isEqualTo(BigDecimal(99))
    }

    @Test
    fun `given sale price set, when updating attributes, then price remains unchanged`() = testBlocking {
        doReturn(
            productAggregate.copy(product = productAggregate.product.copy(salePrice = BigDecimal(99)))
        ).whenever(productRepository).getProductAggregate(any())
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        viewModel.start()

        viewModel.updateProductDraft(sku = "E9999999")

        Assertions.assertThat(viewModel.getProduct().productDraft?.salePrice).isEqualTo(BigDecimal(99))
    }

    @Test
    fun `given regular price greater than 0, when setting price to 0, then price is set to zero`() = testBlocking {
        doReturn(
            productAggregate.copy(product = productAggregate.product.copy(regularPrice = BigDecimal(99)))
        ).whenever(productRepository).getProductAggregate(any())
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        viewModel.start()

        viewModel.updateProductDraft(regularPrice = BigDecimal(0))

        Assertions.assertThat(viewModel.getProduct().productDraft?.regularPrice).isEqualTo(BigDecimal(0))
    }

    @Test
    fun `given sale price greater than 0, when setting price to 0, then price is set to zero`() = testBlocking {
        doReturn(
            productAggregate.copy(product = productAggregate.product.copy(regularPrice = BigDecimal(99)))
        ).whenever(productRepository).getProductAggregate(any())
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        viewModel.start()

        viewModel.updateProductDraft(salePrice = BigDecimal(0))

        Assertions.assertThat(viewModel.getProduct().productDraft?.salePrice).isEqualTo(BigDecimal(0))
    }

    @Test
    fun `given regular price greater than 0, when setting price to null, then price is set to null`() = testBlocking {
        doReturn(
            productAggregate.copy(product = productAggregate.product.copy(regularPrice = BigDecimal(99)))
        ).whenever(productRepository).getProductAggregate(any())
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        viewModel.start()

        viewModel.updateProductDraft(regularPrice = null)

        Assertions.assertThat(viewModel.getProduct().productDraft?.regularPrice).isNull()
    }

    @Test
    fun `given sale price greater than 0, when setting price to null, then price is set to null`() = testBlocking {
        doReturn(
            productAggregate.copy(product = productAggregate.product.copy(regularPrice = BigDecimal(99)))
        ).whenever(productRepository).getProductAggregate(any())
        viewModel.productDetailViewStateData.observeForever { _, _ -> }
        viewModel.start()

        viewModel.updateProductDraft(salePrice = null)

        Assertions.assertThat(viewModel.getProduct().productDraft?.salePrice).isNull()
    }

    @Test
    fun `given image uris when app opened, then a product creation is triggered using the images`() = testBlocking {
        val uris = arrayOf("uri1", "uri2")
        savedState = ProductDetailFragmentArgs(
            ProductDetailFragment.Mode.ShowProduct(PRODUCT_REMOTE_ID),
            images = uris
        ).toSavedStateHandle()

        doReturn(productAggregate).whenever(productRepository).getProductAggregate(any())

        mediaFileUploadHandler = mock {
            on { it.observeCurrentUploadErrors(any()) } doReturn emptyFlow()
            on { it.observeCurrentUploads(any()) } doReturn flowOf(emptyList())
            on { it.observeSuccessfulUploads(any()) } doReturn uris.map {
                MediaModel(0, 0).apply {
                    url = it
                    uploadDate = "2022-09-27 18:00:00.000"
                }
            }.asFlow()
        }

        setup()

        Assertions.assertThat(productsDraft?.images?.map { it.source }).isEqualTo(uris.toList())
    }

    @Test
    fun `give empty mode, when viewmodel init, then error with product not selected message emitted`() = testBlocking {
        // GIVEN
        val mode = ProductDetailFragment.Mode.Empty
        savedState = ProductDetailFragmentArgs(mode).toSavedStateHandle()

        // WHEN
        setup()

        // THEN
        Assertions.assertThat(viewModel.getProduct().auxiliaryState).isEqualTo(
            ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.Error(
                R.string.product_detail_product_not_selected
            )
        )
    }

    @Test
    fun `given tablet, when loaded remote products, then PRODUCT_DETAIL_LOADED tracked with regular horizontal class`() =
        testBlocking {
            // GIVEN
            whenever(isWindowClassLargeThanCompact()).thenReturn(true)

            // WHEN
            setup()

            // THEN
            verify(tracker).track(
                eq(AnalyticsEvent.PRODUCT_DETAIL_LOADED),
                eq(mapOf("horizontal_size_class" to "regular"))
            )
        }

    @Test
    fun `given not tablet, when loaded remote products, then PRODUCT_DETAIL_LOADED tracked with compact horizontal class`() =
        testBlocking {
            // GIVEN
            whenever(isWindowClassLargeThanCompact()).thenReturn(false)

            // WHEN
            setup()

            // THEN
            verify(tracker, times(2)).track(
                eq(AnalyticsEvent.PRODUCT_DETAIL_LOADED),
                eq(mapOf("horizontal_size_class" to "compact"))
            )
        }

    @Test
    fun `given product updated successfuly, when onPublishButtonClicked, then ProductUpdated event emitted`() =
        testBlocking {
            // GIVEN
            whenever(productRepository.getProductAggregate(any())).thenReturn(productAggregate)
            whenever(productRepository.updateProduct(any<ProductAggregate>())).thenReturn(Pair(true, null))
            viewModel.start()

            // WHEN
            viewModel.onPublishButtonClicked()

            // THEN
            Assertions.assertThat(viewModel.event.value).isEqualTo(ProductDetailViewModel.ProductUpdated)
        }

    @Test
    fun `given selected site is private, when product detail is opened, then images are not available`() =
        testBlocking {
            // GIVEN
            whenever(selectedSite.get()).thenReturn(SiteModel().apply { setIsPrivate(true) })
            savedState = ProductDetailFragmentArgs(ProductDetailFragment.Mode.ShowProduct(PRODUCT_REMOTE_ID))
                .toSavedStateHandle()

            setup()
            viewModel.start()

            // WHEN
            var productData: ProductDetailViewModel.ProductDetailViewState? = null
            viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

            // THEN
            Assertions.assertThat(productData?.areImagesAvailable).isFalse()
        }

    @Test
    fun `given selected site is public, when product detail is opened, then images are available`() = testBlocking {
        // GIVEN
        viewModel.start()

        // WHEN
        var productData: ProductDetailViewModel.ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        // THEN
        Assertions.assertThat(productData?.areImagesAvailable).isTrue()
    }

    @Test
    fun `given product password API uses CORE, when product details are fetched, then use password from the model`() =
        testBlocking {
            // GIVEN
            val password = "password"
            whenever(determineProductPasswordApi.invoke()).thenReturn(ProductPasswordApi.CORE)
            whenever(productRepository.getProductAggregate(any()))
                .thenReturn(productAggregate.copy(product = productAggregate.product.copy(password = password)))

            // WHEN
            viewModel.start()
            val viewState = viewModel.productDetailViewStateData.liveData.getOrAwaitValue()

            // THEN
            Assertions.assertThat(viewState.draftPassword).isEqualTo(password)
            verify(productRepository, never()).fetchProductPassword(any())
        }

    @Test
    fun `given product password API uses WPCOM, when product details are fetched, then fetch password from the API`() =
        testBlocking {
            // GIVEN
            val password = "password"
            whenever(determineProductPasswordApi.invoke()).thenReturn(ProductPasswordApi.WPCOM)
            whenever(productRepository.getProductAggregate(any())).thenReturn(productAggregate)
            whenever(productRepository.fetchProductPassword(any())).thenReturn(password)

            // WHEN
            viewModel.start()
            val viewState = viewModel.productDetailViewStateData.liveData.getOrAwaitValue()

            // THEN
            Assertions.assertThat(viewState.draftPassword).isEqualTo(password)
            verify(productRepository).fetchProductPassword(any())
        }

    @Test
    fun `given product password API uses WPCOM, when product is saved, then update password using WPCOM API`() =
        testBlocking {
            // GIVEN
            val password = "password"
            whenever(determineProductPasswordApi.invoke()).thenReturn(ProductPasswordApi.WPCOM)
            whenever(productRepository.getProductAggregate(any())).thenReturn(productAggregate)
            whenever(productRepository.fetchProductPassword(any())).thenReturn(password)
            whenever(productRepository.updateProduct(any<ProductAggregate>())).thenReturn(Pair(true, null))

            // WHEN
            viewModel.start()
            viewModel.updateProductVisibility(ProductVisibility.PASSWORD_PROTECTED, "newPassword")
            viewModel.onSaveButtonClicked()

            // THEN
            verify(productRepository).updateProductPassword(eq(productAggregate.remoteId), eq("newPassword"))
        }

    @Test
    fun `given product password API is not supported, when product details are fetched, then password is empty`() =
        testBlocking {
            // GIVEN
            whenever(determineProductPasswordApi.invoke()).thenReturn(ProductPasswordApi.UNSUPPORTED)
            whenever(productRepository.getProductAggregate(any())).thenReturn(productAggregate)

            // WHEN
            viewModel.start()
            val viewState = viewModel.productDetailViewStateData.liveData.getOrAwaitValue()

            // THEN
            Assertions.assertThat(viewState.draftPassword).isNull()
            verify(productRepository, never()).fetchProductPassword(any())
        }

    private val productsDraft
        get() = viewModel.productDetailViewStateData.liveData.value?.productDraft
}
