package com.woocommerce.android.ui.products

import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductDetailBinding
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.parcelable
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.aztec.AztecEditorFragment
import com.woocommerce.android.ui.aztec.AztecEditorFragment.Companion.ARG_AZTEC_EDITOR_TEXT
import com.woocommerce.android.ui.aztec.AztecEditorFragment.Companion.ARG_AZTEC_TITLE_FROM_AI_DESCRIPTION
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.products.AIProductDescriptionBottomSheetFragment.Companion.KEY_AI_GENERATED_DESCRIPTION_RESULT
import com.woocommerce.android.ui.products.ProductDetailViewModel.HideImageUploadErrorSnackbar
import com.woocommerce.android.ui.products.ProductDetailViewModel.NavigateToBlazeWebView
import com.woocommerce.android.ui.products.ProductDetailViewModel.OpenProductDetails
import com.woocommerce.android.ui.products.ProductDetailViewModel.RefreshMenu
import com.woocommerce.android.ui.products.ProductDetailViewModel.ShowAIProductDescriptionBottomSheet
import com.woocommerce.android.ui.products.ProductDetailViewModel.ShowAiProductCreationSurveyBottomSheet
import com.woocommerce.android.ui.products.ProductDetailViewModel.ShowBlazeCreationScreen
import com.woocommerce.android.ui.products.ProductDetailViewModel.ShowDuplicateProductError
import com.woocommerce.android.ui.products.ProductDetailViewModel.ShowDuplicateProductInProgress
import com.woocommerce.android.ui.products.ProductDetailViewModel.ShowLinkedProductPromoBanner
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDetailBottomSheet
import com.woocommerce.android.ui.products.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem
import com.woocommerce.android.ui.products.adapters.ProductPropertyCardsAdapter
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.price.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.reviews.ProductReviewsFragment
import com.woocommerce.android.ui.products.subscriptions.ProductSubscriptionExpirationFragment.Companion.KEY_SUBSCRIPTION_EXPIRATION_RESULT
import com.woocommerce.android.ui.products.subscriptions.ProductSubscriptionFreeTrialFragment.Companion.KEY_SUBSCRIPTION_FREE_TRIAL_RESULT
import com.woocommerce.android.ui.products.subscriptions.ProductSubscriptionFreeTrialViewModel.FreeTrialState
import com.woocommerce.android.ui.products.variations.VariationListFragment
import com.woocommerce.android.ui.products.variations.VariationListViewModel.VariationListData
import com.woocommerce.android.ui.promobanner.PromoBanner
import com.woocommerce.android.ui.promobanner.PromoBannerType
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.LaunchUrlInChromeTab
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowActionSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCProductImageGalleryView.OnGalleryImageInteractionListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@AndroidEntryPoint
class ProductDetailFragment :
    BaseProductFragment(R.layout.fragment_product_detail),
    OnGalleryImageInteractionListener {
    companion object {
        private const val LIST_STATE_KEY = "list_state"

        const val KEY_PRODUCT_DETAIL_RESULT = "product_detail_result"
        const val KEY_PRODUCT_DETAIL_DID_TRASH = "product_detail_did_trash"
        const val KEY_REMOTE_PRODUCT_ID = "remote_product_id"
    }

    private var productName = ""
        set(value) {
            field = value
            toolbarHelper.updateTitle(value)
        }

    private var productId: Long = ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID

    @Inject
    lateinit var blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher

    @Inject
    lateinit var toolbarHelper: ProductDetailsToolbarHelper

    private val skeletonView = SkeletonView()

    private var progressDialog: CustomProgressDialog? = null
    private var layoutManager: LayoutManager? = null
    private var imageUploadErrorsSnackbar: Snackbar? = null

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    @Inject lateinit var crashLogging: CrashLogging

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        blazeCampaignCreationDispatcher.attachFragment(this, BlazeFlowSource.PRODUCT_DETAIL_PROMOTE_BUTTON)

        _binding = FragmentProductDetailBinding.bind(view)

        toolbarHelper.onViewCreated(this, viewModel, binding)

        ViewCompat.setTransitionName(
            binding.root,
            getString(R.string.product_card_detail_transition_name)
        )
        initializeViews(savedInstanceState)
        initializeViewModel()
    }

    override fun onDestroyView() {
        skeletonView.hide()
        imageUploadErrorsSnackbar?.dismiss()
        super.onDestroyView()
        _binding = null
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

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        this.layoutManager = layoutManager

        savedInstanceState?.parcelable<Parcelable>(LIST_STATE_KEY)?.let {
            layoutManager.onRestoreInstanceState(it)
        }
        binding.cardsRecyclerView.layoutManager = layoutManager
        binding.cardsRecyclerView.itemAnimator = null
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
            new.productDraft?.takeIfNotEqualTo(old?.productDraft) { showProductDetails(it) }
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                if (it) {
                    showProgressDialog(R.string.product_save_dialog_title, R.string.product_update_dialog_message)
                } else {
                    hideProgressDialog()
                }
            }
            new.uploadingImageUris?.takeIfNotEqualTo(old?.uploadingImageUris) {
                binding.imageGallery.setPlaceholderImageUris(it)
            }
            new.showBottomSheetButton?.takeIfNotEqualTo(old?.showBottomSheetButton) { isVisible ->
                binding.productDetailAddMoreContainer.isVisible = isVisible
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
        }

        viewModel.productDetailCards.observe(viewLifecycleOwner) {
            showProductCards(it)
        }

        observeEvents(viewModel)
    }

    @Suppress("ComplexMethod")
    private fun observeEvents(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is LaunchUrlInChromeTab -> ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                is RefreshMenu -> toolbarHelper.setupToolbar()
                is ExitWithResult<*> -> {
                    navigateBackWithResult(
                        KEY_PRODUCT_DETAIL_RESULT,
                        Bundle().also {
                            it.putLong(KEY_REMOTE_PRODUCT_ID, event.data as Long)
                            it.putBoolean(KEY_PRODUCT_DETAIL_DID_TRASH, true)
                        }
                    )
                }

                is ShowActionSnackbar -> displayProductImageUploadErrorSnackBar(
                    event.message,
                    event.actionText,
                    event.action
                )

                is HideImageUploadErrorSnackbar -> imageUploadErrorsSnackbar?.dismiss()
                is ShowLinkedProductPromoBanner -> showLinkedProductPromoBanner()
                is OpenProductDetails -> openProductDetails(event.productRemoteId)
                is ShowDuplicateProductError -> showDuplicateProductError()
                is NavigateToBlazeWebView -> openBlazeWebView(event)
                is ShowBlazeCreationScreen -> openBlazeCreationFlow(event.productId)
                is ShowDuplicateProductInProgress -> showProgressDialog(
                    R.string.product_duplicate_progress_title,
                    R.string.product_duplicate_progress_body
                )

                is ShowAIProductDescriptionBottomSheet -> showAIProductDescriptionBottomSheet(
                    event.productTitle,
                    event.productDescription
                )

                is ShowAiProductCreationSurveyBottomSheet -> openAIProductCreationSurveyBottomSheet()
                else -> event.isHandled = false
            }
        }
    }

    private fun openAIProductCreationSurveyBottomSheet() {
        findNavController().navigateSafely(
            ProductDetailFragmentDirections.actionProductDetailFragmentToAIProductCreationSurveyBottomSheet()
        )
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

    private fun openBlazeWebView(event: NavigateToBlazeWebView) {
        findNavController().navigateSafely(
            NavGraphMainDirections.actionGlobalBlazeCampaignCreationFragment(
                urlToLoad = event.url,
                source = event.source
            )
        )
    }

    private fun showDuplicateProductError() {
        hideProgressDialog()
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.error_generic)
            .setMessage(R.string.product_duplicate_error)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun openProductDetails(productRemoteId: Long) {
        hideProgressDialog()
        (activity as? MainNavigationRouter)?.showProductDetail(productRemoteId, enableTrash = true)
    }

    /**
     *  Triggered when the view modal updates or creates an order that doesn't already have linked products
     */
    private fun showProductDetails(product: Product) {
        productName = updateProductNameFromDetails(product)
        productId = product.remoteId

        if (product.images.isEmpty() && !viewModel.isUploadingImages()) {
            binding.imageGallery.hide()
            startAddImageContainer()
        } else {
            binding.addImageContainer.hide()
            binding.imageGallery.show()
            binding.imageGallery.showProductImages(product.images, this)
        }

        binding.productDetailAddMoreButton.setOnClickListener {
            // TODO: add tracking events here
            viewModel.onEditProductCardClicked(
                ViewProductDetailBottomSheet(product.productType)
            )
        }

        toolbarHelper.setupToolbar()
    }

    private fun updateProductNameFromDetails(product: Product): String {
        return if (viewModel.isProductUnderCreation && product.name.isEmpty()) {
            getString(R.string.product_add_tool_bar_title)
        } else product.name.fastStripHtml()
    }

    private fun displayProductImageUploadErrorSnackBar(
        message: String,
        actionText: String,
        actionListener: View.OnClickListener
    ) {
        if (imageUploadErrorsSnackbar == null) {
            imageUploadErrorsSnackbar = uiMessageResolver.getIndefiniteActionSnack(
                message = message,
                actionText = actionText,
                actionListener = actionListener
            )
        } else {
            imageUploadErrorsSnackbar?.setText(message)
        }
        imageUploadErrorsSnackbar?.show()
    }

    private fun startAddImageContainer() {
        binding.addImageContainer.show()
        binding.addImageContainer.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.PRODUCT_DETAIL_ADD_IMAGE_TAPPED)
            viewModel.onAddImageButtonClicked()
        }
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(binding.appBarLayout, R.layout.skeleton_product_detail, delayed = true)
        } else {
            skeletonView.hide()
        }
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

    private fun showProductCards(cards: List<ProductPropertyCard>) {
        val adapter: ProductPropertyCardsAdapter
        if (binding.cardsRecyclerView.adapter == null) {
            adapter = ProductPropertyCardsAdapter()
            binding.cardsRecyclerView.adapter = adapter
        } else {
            adapter = binding.cardsRecyclerView.adapter as ProductPropertyCardsAdapter
        }

        val recyclerViewState = binding.cardsRecyclerView.layoutManager?.onSaveInstanceState()
        adapter.update(cards)
        binding.cardsRecyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    private fun showLinkedProductPromoBanner() {
        if (binding.promoComposableContainer.isVisible.not()) {
            if (binding.promoComposable.hasComposition.not()) {
                binding.promoComposable.setContent {
                    WooThemeWithBackground {
                        PromoBanner(
                            bannerType = PromoBannerType.LINKED_PRODUCTS,
                            onCtaClick = {
                                WooAnimUtils.scaleOut(binding.promoComposableContainer)
                                viewModel.onLinkedProductPromoClicked()
                            },
                            onDismissClick = {
                                WooAnimUtils.scaleOut(binding.promoComposableContainer)
                                viewModel.onLinkedProductPromoDismissed()
                            }
                        )
                    }
                }
            }
            WooAnimUtils.scaleIn(binding.promoComposableContainer, WooAnimUtils.Duration.MEDIUM)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        layoutManager?.let {
            outState.putParcelable(LIST_STATE_KEY, it.onSaveInstanceState())
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClickedProductDetail()
    }

    override fun onGalleryImageClicked(image: Image) {
        viewModel.onImageClicked()
    }

    override fun onGalleryAddImageClicked() {
        AnalyticsTracker.track(AnalyticsEvent.PRODUCT_DETAIL_ADD_IMAGE_TAPPED)
        viewModel.onAddImageButtonClicked()
    }

    override fun getFragmentTitle(): String = productName

    @Parcelize
    sealed class Mode : Parcelable {
        @Parcelize
        data object Loading : Mode()

        @Parcelize
        data class ShowProduct(val remoteProductId: Long) : Mode()

        @Parcelize
        data object AddNewProduct : Mode()
    }
}
