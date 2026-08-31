package com.woocommerce.android.ui.products.details

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.isTwoPanesShouldBeUsed
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.aztec.AztecEditorFragment
import com.woocommerce.android.ui.aztec.AztecEditorFragment.Companion.ARG_AZTEC_EDITOR_TEXT
import com.woocommerce.android.ui.aztec.AztecEditorFragment.Companion.ARG_AZTEC_TITLE_FROM_AI_DESCRIPTION
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.common.webview.AuthenticatedWebViewLauncher
import com.woocommerce.android.ui.compose.designSystemComposeView
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.BottomNavigationPosition
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.products.BaseProductEditorFragment
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDetailBottomSheet
import com.woocommerce.android.ui.products.ProductsCommunicationViewModel
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionBottomSheetFragment.Companion.KEY_AI_GENERATED_DESCRIPTION_RESULT
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.HideImageUploadErrorSnackbar
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.OpenProductDetails
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.Loading
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductUpdated
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ShowAIProductDescriptionBottomSheet
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ShowBlazeCreationScreen
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ShowDuplicateProductError
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ShowDuplicateProductInProgress
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ShowLinkedProductPromoBanner
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.TrashProduct
import com.woocommerce.android.ui.products.grouped.GroupedProductListType
import com.woocommerce.android.ui.products.list.ProductListFragment
import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.ui.products.price.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.reviews.ProductReviewsFragment
import com.woocommerce.android.ui.products.shipping.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.subscriptions.ProductSubscriptionExpirationFragment.Companion.KEY_SUBSCRIPTION_EXPIRATION_RESULT
import com.woocommerce.android.ui.products.subscriptions.ProductSubscriptionFreeTrialFragment.Companion.KEY_SUBSCRIPTION_FREE_TRIAL_RESULT
import com.woocommerce.android.ui.products.subscriptions.ProductSubscriptionFreeTrialViewModel.FreeTrialState
import com.woocommerce.android.ui.products.typesbottomsheet.ProductTypesBottomSheetFragment
import com.woocommerce.android.ui.products.typesbottomsheet.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem
import com.woocommerce.android.ui.products.variations.VariationListFragment
import com.woocommerce.android.ui.products.variations.VariationListViewModel.VariationListData
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.IsWindowClassLargeThanCompact
import com.woocommerce.android.util.UiHelpers.getTextOfUiString
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

@AndroidEntryPoint
class ProductDetailFragment :
    BaseProductFragment() {
    private var productName = ""

    @Inject
    lateinit var blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher

    @Inject
    lateinit var authenticatedWebViewLauncher: AuthenticatedWebViewLauncher

    private var progressDialog: CustomProgressDialog? = null
    private var imageUploadErrorsSnackbar: Snackbar? = null

    private val productDetailUiMapper = ProductDetailUiMapper()
    private val topAppBarPolicy = ProductDetailTopAppBarPolicy()
    private var productDetailPageState by mutableStateOf(INITIAL_PAGE_STATE)
    private var isUploadErrorVisible = false
    private var currentProduct: Product? = null
    private var currentCards = emptyList<ProductDetailCardUiModel>()
    private var currentAuxiliaryState: ProductDetailViewModel.ProductDetailViewState.AuxiliaryState = Loading
    private var areImagesAvailable = true
    private var uploadingImageUris = emptyList<Uri>()
    private var isAddMoreVisible = false
    private var isLinkedProductPromoVisible = false
    private var menuButtonsState: ProductDetailViewModel.MenuButtonsState? = null
    private var isWindowLargerThanCompact = false
    private var isPartOfProductListFlow = false

    private val pageCallbacks by lazy {
        ProductDetailPageCallbacks(
            topAppBar = ProductDetailTopAppBarCallbacks(
                onNavigationClicked = ::onTopAppBarNavigationClicked,
                onActionClicked = ::onTopAppBarActionClicked,
            ),
            image = ProductDetailImageCallbacks(
                onImageClicked = viewModel::onImageClicked,
                onAddImageClicked = viewModel::onAddImageButtonClicked,
                onImagesUnavailableClicked = ::onImagesUnavailableClicked,
            ),
            content = ProductDetailContentCallbacks(
                onLinkedProductPromoClicked = ::onLinkedProductPromoClicked,
                onLinkedProductPromoDismissed = ::onLinkedProductPromoDismissed,
                onAddMoreClicked = ::onAddMoreClicked,
            ),
            onUploadErrorClicked = viewModel::openUploadScreen,
        )
    }

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    @Inject
    lateinit var crashLogging: CrashLogging

    private val productsCommunicationViewModel: ProductsCommunicationViewModel by activityViewModels()

    private val navArgs: ProductDetailFragmentArgs by navArgs()

    private val isInDetailPane: Boolean
        get() = requireContext().isTwoPanesShouldBeUsed && parentFragment?.id == R.id.detail_nav_container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transitionDuration = resources.getInteger(R.integer.default_fragment_transition).toLong()
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.default_window_background)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.snack_root
            duration = transitionDuration
            scrimColor = Color.TRANSPARENT
            startContainerColor = backgroundColor
            endContainerColor = backgroundColor
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = designSystemComposeView {
        ProductDetailScreen(
            state = productDetailPageState,
            callbacks = pageCallbacks,
        )
    }.apply {
        id = R.id.productDetail_root
        isFocusableInTouchMode = true
        ViewCompat.setTransitionName(this, getString(R.string.product_card_detail_transition_name))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productListInBackstack =
            findNavController().previousBackStackEntry?.destination?.id == BottomNavigationPosition.PRODUCTS.id
        val isTrashEnabled = productListInBackstack || isInDetailPane
        viewModel.setTrashActionPossible(isTrashEnabled)

        blazeCampaignCreationDispatcher.attachFragment(this, BlazeFlowSource.PRODUCT_DETAIL_PROMOTE_BUTTON)
        isWindowLargerThanCompact = IsWindowClassLargeThanCompact(requireActivity()).invoke()
        isPartOfProductListFlow = isPartOfProductListFlow()
        updateProductDetailPresentation()
        initializeViewModel()
        handleOnePaneToTwoPaneConversion()
    }

    /**
     * When product details open in one pane mode and the screen size changes to two pane mode,
     * we need to handle the back stack and navigate to the product list fragment
     * to show selected product in the right pane.
     */
    private fun handleOnePaneToTwoPaneConversion() {
        val isScreenLargerThanCompact = requireContext().isTwoPanesShouldBeUsed
        val isProductListFragmentUpInBackStack =
            findNavController().previousBackStackEntry?.destination?.id == R.id.products
        if (isScreenLargerThanCompact && isProductListFragmentUpInBackStack) {
            val mode = navArgs.mode
            when (mode) {
                is Mode.ShowProduct -> {
                    findNavController().popBackStack()
                    productsCommunicationViewModel.pushEvent(
                        ProductsCommunicationViewModel.CommunicationEvent.ProductSelected(mode.remoteProductId)
                    )
                }

                is Mode.Loading, is Mode.Empty -> {
                    findNavController().popBackStack()
                }

                is Mode.AddNewProduct -> {
                }
            }
        }
    }

    override fun onDestroyView() {
        imageUploadErrorsSnackbar?.dismiss()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onPause() {
        super.onPause()
        progressDialog?.dismiss()
    }

    override fun onStop() {
        super.onStop()
        WooDialog.onCleared()
    }

    private fun initializeViewModel() {
        setupObservers(viewModel)
        setupResultHandlers(viewModel)
    }

    @Suppress("LongMethod")
    private fun setupResultHandlers(viewModel: ProductDetailViewModel) {
        handleResult<ProductTypesBottomSheetUiItem>(ProductTypesBottomSheetFragment.KEY_PRODUCT_TYPE_RESULT) {
            viewModel.onProductTypeChanged(productType = it.type, isVirtual = it.isVirtual)
        }
        handleResult<List<Long>>(GroupedProductListType.GROUPED.resultKey) {
            viewModel.updateProductDraft(groupedProductIds = it)
        }
        handleResult<PricingData>(BaseProductEditorFragment.KEY_PRICING_DIALOG_RESULT) {
            viewModel.updateProductDraft(
                regularPrice = it.regularPrice,
                salePrice = it.salePrice,
                saleStartDate = it.saleStartDate,
                saleEndDate = it.saleEndDate,
                isSaleScheduled = it.isSaleScheduled,
                taxClass = it.taxClass,
                taxStatus = it.taxStatus
            )
            if (it.isSubscription) {
                viewModel.updateProductSubscription(
                    price = it.regularPrice,
                    period = it.subscriptionPeriod,
                    periodInterval = it.subscriptionInterval,
                    signUpFee = it.subscriptionSignUpFee,
                )
            }
        }
        handleResult<InventoryData>(BaseProductEditorFragment.KEY_INVENTORY_DIALOG_RESULT) {
            viewModel.updateProductDraft(
                sku = it.sku,
                globalUniqueId = it.globalUniqueId,
                soldIndividually = it.isSoldIndividually,
                stockStatus = it.stockStatus,
                stockQuantity = it.stockQuantity,
                backorderStatus = it.backorderStatus,
                manageStock = it.isStockManaged
            )
        }
        handleResult<ShippingData>(BaseProductEditorFragment.KEY_SHIPPING_DIALOG_RESULT) {
            viewModel.updateProductDraft(
                weight = it.weight,
                length = it.length,
                width = it.width,
                height = it.height,
                shippingClass = it.shippingClassSlug,
                shippingClassId = it.shippingClassId
            )
            if (it.subscriptionShippingData != null) {
                viewModel.updateProductSubscription(
                    oneTimeShipping = it.subscriptionShippingData.oneTimeShipping
                )
            }
        }
        handleResult<QuantityRules>(BaseProductEditorFragment.KEY_QUANTITY_RULES_DIALOG_RESULT) {
            viewModel.updateProductDraft(
                minAllowedQuantity = it.min,
                maxAllowedQuantity = it.max,
                groupOfQuantity = it.groupOf
            )
        }
        handleResult<List<Image>>(BaseProductEditorFragment.KEY_IMAGES_DIALOG_RESULT) {
            viewModel.updateProductDraft(images = it)
        }

        handleResult<Bundle>(AztecEditorFragment.AZTEC_EDITOR_RESULT) { result ->
            if (!result.getBoolean(AztecEditorFragment.ARG_AZTEC_HAS_CHANGES)) return@handleResult
            when (result.getInt(AztecEditorFragment.ARG_AZTEC_REQUEST_CODE)) {
                RequestCodes.AZTEC_EDITOR_PRODUCT_DESCRIPTION -> {
                    viewModel.updateProductDraft(description = result.getString(ARG_AZTEC_EDITOR_TEXT))
                }

                RequestCodes.AZTEC_EDITOR_PRODUCT_SHORT_DESCRIPTION -> {
                    viewModel.updateProductDraft(shortDescription = result.getString(ARG_AZTEC_EDITOR_TEXT))
                }
            }

            if (result.containsKey(ARG_AZTEC_TITLE_FROM_AI_DESCRIPTION)) {
                viewModel.updateProductDraft(
                    title = result.getString(ARG_AZTEC_TITLE_FROM_AI_DESCRIPTION)
                )
            }
        }

        handleResult<VariationListData>(VariationListFragment.KEY_VARIATION_LIST_RESULT) { data ->
            data.currentVariationAmount?.let { viewModel.onVariationAmountReceived(it) }
        }

        handleNotice(ProductReviewsFragment.PRODUCT_REVIEWS_MODIFIED) {
            viewModel.refreshProduct()
        }

        handleResult<Pair<String, String>>(KEY_AI_GENERATED_DESCRIPTION_RESULT) { resultPair ->
            viewModel.updateProductDraft(description = resultPair.first, title = resultPair.second)
        }

        handleResult<Int>(KEY_SUBSCRIPTION_EXPIRATION_RESULT) { newExpiration ->
            viewModel.onSubscriptionExpirationChanged(newExpiration)
        }

        handleResult<FreeTrialState>(KEY_SUBSCRIPTION_FREE_TRIAL_RESULT) { freeTrial ->
            viewModel.updateProductSubscription(trialLength = freeTrial.length, trialPeriod = freeTrial.period)
        }
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.productDetailViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.productDraft?.takeIfNotEqualTo(old?.productDraft) { showProductDetails(it, new.areImagesAvailable) }
            new.auxiliaryState.takeIfNotEqualTo(old?.auxiliaryState) { showAuxiliaryState(it) }
            new.areImagesAvailable.takeIfNotEqualTo(old?.areImagesAvailable) {
                areImagesAvailable = it
                updateProductDetailPresentation()
            }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                if (it) {
                    showProgressDialog(R.string.product_save_dialog_title, R.string.product_update_dialog_message)
                } else {
                    hideProgressDialog()
                }
            }
            new.uploadingImageUris.orEmpty()
                .takeIfNotEqualTo(uploadingImageUris) {
                    uploadingImageUris = it
                    updateProductDetailPresentation()
                }
            new.showBottomSheetButton?.takeIfNotEqualTo(old?.showBottomSheetButton) { isVisible ->
                isAddMoreVisible = isVisible
                updateProductDetailPresentation()
            }
            new.isUploadingDownloadableFile?.takeIfNotEqualTo(old?.isUploadingDownloadableFile) {
                if (it) {
                    showProgressDialog(
                        title = R.string.product_downloadable_files_upload_dialog_title,
                        message = R.string.product_downloadable_files_upload_dialog_message
                    )
                } else {
                    hideProgressDialog()
                }
            }
            new.hasUploadErrors?.takeIfNotEqualTo(old?.hasUploadErrors) { hasErrors ->
                isUploadErrorVisible = hasErrors
                updateProductDetailPresentation()
            }
        }

        viewModel.productDetailCards.observe(viewLifecycleOwner) {
            currentCards = productDetailUiMapper.map(it)
            updateProductDetailPresentation()
        }

        viewModel.menuButtonsState.observe(viewLifecycleOwner) {
            menuButtonsState = it
            updateProductDetailPresentation()
        }

        viewModel.hasChanges.observe(viewLifecycleOwner) { hasChanges ->
            // Only the detail-pane instance should report changes; a full-screen instance pushing this event
            // could overwrite a pending ProductSelected event before the product list resubscribes
            if (isInDetailPane) {
                productsCommunicationViewModel.pushEvent(
                    ProductsCommunicationViewModel.CommunicationEvent.ProductChanges(hasChanges)
                )
            }
        }

        observeEvents(viewModel)
    }

    @Suppress("ComplexMethod")
    private fun observeEvents(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Event.LaunchUrlInChromeTab -> ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                is Event.LaunchUrlInAuthenticatedWebView -> authenticatedWebViewLauncher.showAuthenticatedWebView(event)

                is TrashProduct -> {
                    if (findNavController().previousBackStackEntry != null) {
                        findNavController().popBackStack()
                    }
                    productsCommunicationViewModel.pushEvent(
                        ProductsCommunicationViewModel.CommunicationEvent.ProductTrashed(event.productId)
                    )
                }

                is ShowUiStringSnackbar -> displayProductImageUploadErrorSnackBar(event.message)

                is HideImageUploadErrorSnackbar -> imageUploadErrorsSnackbar?.dismiss()
                is ShowLinkedProductPromoBanner -> showLinkedProductPromoBanner()
                is OpenProductDetails -> openProductDetails(event.productRemoteId)
                is ShowDuplicateProductError -> showDuplicateProductError()
                is ShowBlazeCreationScreen -> openBlazeCreationFlow(event.productId)
                is ShowDuplicateProductInProgress -> showProgressDialog(
                    R.string.product_duplicate_progress_title,
                    R.string.product_duplicate_progress_body
                )

                is ShowAIProductDescriptionBottomSheet -> showAIProductDescriptionBottomSheet(
                    event.productTitle,
                    event.productDescription
                )

                is ProductUpdated -> productsCommunicationViewModel.pushEvent(
                    ProductsCommunicationViewModel.CommunicationEvent.ProductUpdated
                )

                is ProductDetailViewModel.ShowUpdateProductError -> showUpdateProductError(event.message)
                else -> event.isHandled = false
            }
        }
    }

    private fun showAIProductDescriptionBottomSheet(title: String, description: String?) {
        findNavController().navigateSafely(
            ProductDetailFragmentDirections.actionProductDetailFragmentToAIProductDescriptionBottomSheetFragment(
                title,
                description?.fastStripHtml()
            )
        )
    }

    private fun openBlazeCreationFlow(productId: Long) {
        lifecycleScope.launch {
            blazeCampaignCreationDispatcher.startCampaignCreation(
                source = BlazeFlowSource.PRODUCT_DETAIL_PROMOTE_BUTTON,
                productId = productId
            )
        }
    }

    private fun showDuplicateProductError() {
        hideProgressDialog()
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.error_generic)
            .setMessage(R.string.product_duplicate_error)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showUpdateProductError(message: String) {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.product_detail_update_product_error)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun openProductDetails(productRemoteId: Long) {
        hideProgressDialog()
        (activity as? MainNavigationRouter)?.showProductDetail(
            remoteProductId = productRemoteId,
            popUpToProductList = true
        )
    }

    /**
     *  Triggered when the view modal updates or creates an order that doesn't already have linked products
     */
    private fun showProductDetails(product: Product, isImageUploadAvailable: Boolean) {
        currentProduct = product
        areImagesAvailable = isImageUploadAvailable
        isAddMoreVisible = true
        productName = updateProductNameFromDetails(product)
        updateProductDetailPresentation()
    }

    private fun updateProductNameFromDetails(product: Product): String {
        return if (viewModel.isProductUnderCreation && product.name.isEmpty()) {
            getString(R.string.product_add_tool_bar_title)
        } else {
            product.name.fastStripHtml()
        }
    }

    private fun displayProductImageUploadErrorSnackBar(uiString: UiString) {
        if (imageUploadErrorsSnackbar == null) {
            imageUploadErrorsSnackbar = uiMessageResolver.getUiStringSnack(message = uiString)
        } else {
            imageUploadErrorsSnackbar?.setText(getTextOfUiString(requireContext(), uiString))
        }
        imageUploadErrorsSnackbar?.show()
    }

    private val showBackOnLargeScreen: Boolean
        get() = viewModel.startMode == Mode.AddNewProduct ||
            (viewModel.startMode as? Mode.ShowProduct)?.afterGeneratedWithAi == true

    private fun isPartOfProductListFlow(): Boolean = runCatching {
        findNavController().getBackStackEntry(R.id.products)
    }.isSuccess || parentFragment?.parentFragment is ProductListFragment

    private fun onTopAppBarNavigationClicked() {
        if (!viewModel.onBackButtonClickedProductDetail()) return

        if (!findNavController().popBackStack(R.id.products, false)) {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun onTopAppBarActionClicked(action: ProductDetailTopAppBarAction) {
        when (action) {
            ProductDetailTopAppBarAction.SAVE -> {
                ActivityUtils.hideKeyboard(requireActivity())
                viewModel.onSaveButtonClicked()
            }
            ProductDetailTopAppBarAction.PUBLISH -> {
                ActivityUtils.hideKeyboard(requireActivity())
                viewModel.onPublishButtonClicked()
            }
            ProductDetailTopAppBarAction.SAVE_AS_DRAFT -> viewModel.onSaveAsDraftButtonClicked()
            ProductDetailTopAppBarAction.SHARE -> viewModel.onShareButtonClicked()
            ProductDetailTopAppBarAction.VIEW_PRODUCT -> viewModel.onViewProductOnStoreLinkClicked()
            ProductDetailTopAppBarAction.SETTINGS -> viewModel.onSettingsButtonClicked()
            ProductDetailTopAppBarAction.DUPLICATE -> viewModel.onDuplicateProduct()
            ProductDetailTopAppBarAction.TRASH -> viewModel.onTrashButtonClicked()
        }
    }

    private fun onImagesUnavailableClicked() {
        ChromeCustomTabUtils.launchUrl(requireContext(), AppUrls.WORDPRESS_PRIVACY_SETTINGS)
    }

    private fun onLinkedProductPromoClicked() {
        hideLinkedProductPromo()
        viewModel.onLinkedProductPromoClicked()
    }

    private fun onLinkedProductPromoDismissed() {
        hideLinkedProductPromo()
        viewModel.onLinkedProductPromoDismissed()
    }

    private fun onAddMoreClicked() {
        currentProduct?.let { product ->
            viewModel.onEditProductCardClicked(ViewProductDetailBottomSheet(product.productType))
        }
    }

    private fun showAuxiliaryState(auxiliaryState: ProductDetailViewModel.ProductDetailViewState.AuxiliaryState) {
        currentAuxiliaryState = auxiliaryState
        updateProductDetailPresentation()
    }

    private fun updateProductDetailPresentation() {
        val screen = productDetailUiMapper.mapScreenState(
            auxiliaryState = currentAuxiliaryState,
            hasProduct = currentProduct != null,
            cards = currentCards,
            showAddMore = isAddMoreVisible,
            showLinkedProductPromo = isLinkedProductPromoVisible,
        )
        val image = productDetailUiMapper.mapImageState(
            auxiliaryState = currentAuxiliaryState,
            hasProduct = currentProduct != null,
            areImagesAvailable = areImagesAvailable,
            persistedImages = currentProduct?.images.orEmpty(),
            uploadingImageUris = uploadingImageUris.map(Uri::toString),
        )
        val topAppBar = topAppBarPolicy.map(
            menu = menuButtonsState,
            isWindowLargerThanCompact = isWindowLargerThanCompact,
            isPartOfProductListFlow = isPartOfProductListFlow,
            showBackOnLargeScreen = showBackOnLargeScreen,
        )
        productDetailPageState = productDetailUiMapper.mapPageState(
            title = productName,
            topAppBar = topAppBar,
            screen = screen,
            image = image,
            hasUploadErrors = isUploadErrorVisible,
        )
    }

    private fun showProgressDialog(@StringRes title: Int, @StringRes message: Int) {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            getString(title),
            getString(message)
        ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showLinkedProductPromoBanner() {
        isLinkedProductPromoVisible = true
        updateProductDetailPresentation()
    }

    private fun hideLinkedProductPromo() {
        isLinkedProductPromoVisible = false
        updateProductDetailPresentation()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClickedProductDetail()
    }

    override fun getFragmentTitle(): String = productName

    @Parcelize
    sealed class Mode : Parcelable {
        @Parcelize
        data object Loading : Mode()

        @Parcelize
        data object Empty : Mode()

        @Parcelize
        data class ShowProduct(
            val remoteProductId: Long,
            val afterGeneratedWithAi: Boolean = false,
        ) : Mode()

        @Parcelize
        data object AddNewProduct : Mode()
    }

    private companion object {
        val INITIAL_PAGE_STATE = ProductDetailPageUiState(
            title = "",
            topAppBar = ProductDetailTopAppBarUiState(
                navigation = null,
                primaryAction = null,
                shareAction = null,
                overflowActions = emptyList(),
            ),
            screen = ProductDetailScreenState.Loading,
            image = ProductDetailImageUiState.Loading,
            showUploadError = false,
        )
    }
}
