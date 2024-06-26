package com.woocommerce.android.ui.products.details

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.media.MediaFilesRepository
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.ui.products.variations.domain.GenerateVariationCandidates
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.ProductUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class ProductDetailViewModel_AddFlowTest : BaseUnitTest() {
    companion object {
        private const val PRODUCT_REMOTE_ID = 1L
    }

    private val wooCommerceStore: WooCommerceStore = mock()
    private val networkStatus: NetworkStatus = mock()
    private val productRepository: ProductDetailRepository = mock()
    private val productCategoriesRepository: ProductCategoriesRepository = mock()
    private val productTagsRepository: ProductTagsRepository = mock()
    private val mediaFilesRepository: MediaFilesRepository = mock()
    private val variationRepository: VariationRepository = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val resources: ResourceProvider = mock {
        on(it.getString(any())).thenAnswer { i -> i.arguments[0].toString() }
        on(it.getString(any(), any())).thenAnswer { i -> i.arguments[0].toString() }
    }
    private val productImagesServiceWrapper: ProductImagesServiceWrapper = mock()
    private val currencyFormatter: CurrencyFormatter = mock {
        on(it.formatCurrency(any<BigDecimal>(), any(), any())).thenAnswer { i -> "${i.arguments[1]}${i.arguments[0]}" }
    }

    private val mediaFileUploadHandler: MediaFileUploadHandler = mock {
        on { it.observeCurrentUploadErrors(any()) } doReturn emptyFlow()
        on { it.observeCurrentUploads(any()) } doReturn flowOf(emptyList())
        on { it.observeSuccessfulUploads(any()) } doReturn emptyFlow()
    }
    private val isBlazeEnabled: IsBlazeEnabled = mock {
        onBlocking { invoke() } doReturn false
    }
    private var savedState: SavedStateHandle =
        ProductDetailFragmentArgs(
            mode = ProductDetailFragment.Mode.AddNewProduct
        ).toSavedStateHandle()

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

    private val prefs: AppPrefsWrapper = mock {
        on(it.getSelectedProductType()).then { "simple" }
    }
    private val addonRepository: AddonRepository = mock {
        onBlocking { hasAnyProductSpecificAddons(any()) } doReturn false
    }

    private val productUtils = ProductUtils()

    private val product = ProductTestUtils.generateProduct(PRODUCT_REMOTE_ID)
    private lateinit var viewModel: ProductDetailViewModel

    private val defaultPricingGroup: Map<String, String> =
        mapOf("" to resources.getString(R.string.product_price_empty))

    private val addNewProductExpectedCards = listOf(
        ProductPropertyCard(
            type = ProductPropertyCard.Type.PRIMARY,
            properties = listOf(
                ProductProperty.Editable(R.string.product_detail_title_hint, ""),
                ProductProperty.ComplexProperty(
                    R.string.product_description,
                    resources.getString(R.string.product_description_empty),
                    showTitle = false
                )
            )
        ),
        ProductPropertyCard(
            type = ProductPropertyCard.Type.SECONDARY,
            properties = listOf(
                ProductProperty.PropertyGroup(
                    R.string.product_price,
                    defaultPricingGroup,
                    R.drawable.ic_gridicons_money,
                    showTitle = false
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
                appPrefsWrapper = prefs,
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
                isWindowClassLargeThanCompact = mock()
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
    fun `Displays the product detail properties correctly in add new product flow`() = testBlocking {
        viewModel.productDetailViewStateData.observeForever { _, _ -> }

        var cards: List<ProductPropertyCard>? = null
        viewModel.productDetailCards.observeForever {
            cards = it.map { card -> productUtils.stripCallbacks(card) }
        }

        Assertions.assertThat(cards).isEqualTo(addNewProductExpectedCards)
    }

    @Test
    fun `Display success message on add product success`() = testBlocking {
        // given
        doReturn(product).whenever(productRepository).getProductAsync(any())
        doReturn(Pair(true, 1L)).whenever(productRepository).addProduct(any())

        var successSnackbarShown = false
        viewModel.event.observeForever {
            if (it is ShowSnackbar && it.message == R.string.product_detail_publish_product_success) {
                successSnackbarShown = true
            }
        }
        var hasChanges: Boolean? = null
        viewModel.hasChanges.observeForever { hasChanges = it }

        var productData: ProductDetailViewModel.ProductDetailViewState? = null

        // when
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start()

        viewModel.onPublishButtonClicked()

        // then
        verify(productRepository, times(1)).getProductAsync(1L)

        Assertions.assertThat(successSnackbarShown).isTrue()
        Assertions.assertThat(productData?.isProgressDialogShown).isFalse()
        Assertions.assertThat(hasChanges).isFalse()
        Assertions.assertThat(productData?.productDraft).isEqualTo(product)
    }

    @Test
    fun `Display error message on add product failed`() = testBlocking {
        // given
        doReturn(Pair(false, 0L)).whenever(productRepository).addProduct(any())

        var successSnackbarShown = false
        viewModel.event.observeForever {
            if (it is ShowSnackbar && it.message == R.string.product_detail_publish_product_error) {
                successSnackbarShown = true
            }
        }

        var productData: ProductDetailViewModel.ProductDetailViewState? = null

        // when
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start()

        viewModel.onPublishButtonClicked()

        // then
        Assertions.assertThat(successSnackbarShown).isTrue()
        Assertions.assertThat(productData?.isProgressDialogShown).isFalse()
    }

    @Test
    fun `Display error message on add product for NO network`() = testBlocking {
        // given
        doReturn(false).whenever(networkStatus).isConnected()

        var successSnackbarShown = false
        viewModel.event.observeForever {
            if (it is ShowSnackbar && it.message == R.string.offline_error) {
                successSnackbarShown = true
            }
        }

        var productData: ProductDetailViewModel.ProductDetailViewState? = null

        // when
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start()

        viewModel.onPublishButtonClicked()

        // then
        Assertions.assertThat(successSnackbarShown).isTrue()
        Assertions.assertThat(productData?.isProgressDialogShown).isFalse()
    }

    @Test
    fun `Display correct message on updating a freshly added product`() =
        testBlocking {
            // given
            doReturn(product).whenever(productRepository).getProductAsync(any())
            doReturn(Pair(true, 1L)).whenever(productRepository).addProduct(any())

            var successSnackbarShown = false
            viewModel.event.observeForever {
                if (it is ShowSnackbar && it.message == R.string.product_detail_publish_product_success) {
                    successSnackbarShown = true
                }
            }
            var hasChanges: Boolean? = null
            viewModel.hasChanges.observeForever { hasChanges = it }

            var productData: ProductDetailViewModel.ProductDetailViewState? = null

            // when
            viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

            viewModel.start()

            viewModel.onPublishButtonClicked()

            // then
            verify(productRepository, times(1)).getProductAsync(1L)

            Assertions.assertThat(successSnackbarShown).isTrue()
            Assertions.assertThat(productData?.isProgressDialogShown).isFalse()
            Assertions.assertThat(hasChanges).isFalse()
            Assertions.assertThat(productData?.productDraft).isEqualTo(product)

            // when
            doReturn(Pair(true, null)).whenever(productRepository).updateProduct(any())

            viewModel.onPublishButtonClicked()
            verify(productRepository, times(1)).updateProduct(any())

            viewModel.event.observeForever {
                if (it is ShowSnackbar && it.message == R.string.product_detail_save_product_success) {
                    successSnackbarShown = true
                }
            }

            // then
            Assertions.assertThat(successSnackbarShown).isTrue()
            Assertions.assertThat(productData?.isProgressDialogShown).isFalse()
            Assertions.assertThat(hasChanges).isFalse()
            Assertions.assertThat(productData?.productDraft).isEqualTo(product)
        }

    @Test
    fun `Save as draft shown in discard dialog when changes made in add flow`() {
        doReturn(true).whenever(viewModel).isProductUnderCreation
        viewModel.productDetailViewStateData.observeForever { _, _ -> }

        viewModel.start()

        // this will force the viewModel to consider the product as changed, so when we click the back button
        // below it will show the discard dialog
        viewModel.updateProductDraft(productStatus = ProductStatus.DRAFT)

        var saveAsDraftShown = false
        viewModel.event.observeForever {
            if (it is MultiLiveEvent.Event.ShowDialog && it.neutralBtnAction != null) {
                saveAsDraftShown = true
            }
        }

        viewModel.onBackButtonClickedProductDetail()
        Assertions.assertThat(saveAsDraftShown).isTrue()
    }

    @Test
    fun `Save as draft not shown in discard dialog when not in add flow`() {
        doReturn(false).whenever(viewModel).isProductUnderCreation

        viewModel.start()
        viewModel.updateProductDraft(productStatus = ProductStatus.DRAFT)

        var saveAsDraftShown = false
        viewModel.event.observeForever {
            if (it is MultiLiveEvent.Event.ShowDialog && it.neutralBtnAction != null) {
                saveAsDraftShown = true
            }
        }

        viewModel.onBackButtonClickedProductDetail()
        Assertions.assertThat(saveAsDraftShown).isFalse()
    }

    @Test
    fun `when a new product is saved, then assign the new id to ongoing image uploads`() = testBlocking {
        doReturn(Pair(true, PRODUCT_REMOTE_ID)).whenever(productRepository).addProduct(any())
        doReturn(product).whenever(productRepository).getProductAsync(any())
        savedState = ProductDetailFragmentArgs(
            mode = ProductDetailFragment.Mode.AddNewProduct
        ).toSavedStateHandle()

        setup()
        viewModel.start()
        viewModel.onSaveAsDraftButtonClicked()

        verify(mediaFileUploadHandler).assignUploadsToCreatedProduct(PRODUCT_REMOTE_ID)
    }

    @Test
    fun `given a product is under creation, when displaying discard changes dialog, then stop observing uploads`() =
        testBlocking {
            var isObservingEvents: Boolean? = null
            val successEvents = MutableSharedFlow<MediaModel>()
                .onSubscription { isObservingEvents = true }
                .onCompletion { isObservingEvents = false }
            doReturn(successEvents).whenever(mediaFileUploadHandler)
                .observeSuccessfulUploads(ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID)
            viewModel.productDetailViewStateData.observeForever { _, _ -> }

            viewModel.start()
            // Make some changes to trigger discard changes dialog
            viewModel.onProductTitleChanged("Product 2")
            viewModel.onBackButtonClickedProductDetail()

            Assertions.assertThat(isObservingEvents).isFalse()
        }

    @Test
    fun `given a product is under creation, when dismissing discard changes dialog, then start observing uploads`() =
        testBlocking {
            var isObservingEvents: Boolean? = null
            val successEvents = MutableSharedFlow<MediaModel>()
                .onSubscription { isObservingEvents = true }
                .onCompletion { isObservingEvents = false }
            doReturn(successEvents).whenever(mediaFileUploadHandler)
                .observeSuccessfulUploads(ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID)
            viewModel.productDetailViewStateData.observeForever { _, _ -> }

            viewModel.start()
            // Make some changes to trigger discard changes dialog
            viewModel.onProductTitleChanged("Product")
            viewModel.onBackButtonClickedProductDetail()
            (viewModel.event.value as MultiLiveEvent.Event.ShowDialog).negativeBtnAction!!.onClick(null, 0)

            Assertions.assertThat(isObservingEvents).isTrue()
        }

    @Test
    fun `given a product is under creation, when clicking on save product, then assign uploads to the new id`() =
        testBlocking {
            doReturn(Pair(true, PRODUCT_REMOTE_ID)).whenever(productRepository).addProduct(any())
            doReturn(product).whenever(productRepository).getProductAsync(any())
            viewModel.productDetailViewStateData.observeForever { _, _ -> }

            viewModel.start()
            // Make some changes to trigger discard changes dialog
            viewModel.onProductTitleChanged("Product")
            viewModel.onBackButtonClickedProductDetail()
            (viewModel.event.value as MultiLiveEvent.Event.ShowDialog).neutralBtnAction!!.onClick(null, 0)

            verify(mediaFileUploadHandler).assignUploadsToCreatedProduct(PRODUCT_REMOTE_ID)
        }

    @Test
    fun `Publish option shown when product is published and from addProduct flow and is under product creation`() {
        viewModel.productDetailViewStateData.observeForever { _, _ -> }

        var menuButtonsState: ProductDetailViewModel.MenuButtonsState? = null
        viewModel.menuButtonsState.observeForever { menuButtonsState = it }

        viewModel.start()
        viewModel.updateProductDraft(productStatus = ProductStatus.PUBLISH)
        Assertions.assertThat(menuButtonsState?.publishOption).isTrue
    }

    @Test
    fun `Save option not shown when product has changes but in add product flow`() {
        viewModel.productDetailViewStateData.observeForever { _, _ -> }

        var menuButtonsState: ProductDetailViewModel.MenuButtonsState? = null
        viewModel.menuButtonsState.observeForever { menuButtonsState = it }

        viewModel.start()
        viewModel.updateProductDraft(title = "name")

        Assertions.assertThat(menuButtonsState?.saveOption).isFalse()
    }

    @Test
    fun `given product status is draft, when save is clicked, then save product with correct status`() = testBlocking {
        whenever(productRepository.addProduct(any())).thenAnswer { it.arguments.first() as Product }
        var viewState: ProductDetailViewModel.ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> viewState = new }

        viewModel.start()
        viewModel.updateProductDraft(productStatus = ProductStatus.DRAFT)
        viewModel.onSaveButtonClicked()

        Assertions.assertThat(viewState?.productDraft?.status).isEqualTo(ProductStatus.DRAFT)
    }
}
