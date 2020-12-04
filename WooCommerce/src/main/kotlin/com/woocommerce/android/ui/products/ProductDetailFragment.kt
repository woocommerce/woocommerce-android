package com.woocommerce.android.ui.products

import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.aztec.AztecEditorFragment
import com.woocommerce.android.ui.aztec.AztecEditorFragment.Companion.ARG_AZTEC_EDITOR_TEXT
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import com.woocommerce.android.ui.products.ProductDetailViewModel.LaunchUrlInChromeTab
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductDetail
import com.woocommerce.android.ui.products.ProductDetailViewModel.RefreshMenu
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDetailBottomSheet
import com.woocommerce.android.ui.products.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.adapters.ProductPropertyCardsAdapter
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.CrashUtils
import com.woocommerce.android.util.Optional
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCProductImageGalleryView.OnGalleryImageInteractionListener
import kotlinx.android.synthetic.main.fragment_product_detail.*
import org.wordpress.android.util.ActivityUtils

class ProductDetailFragment : BaseProductFragment(), OnGalleryImageInteractionListener, NavigationResult {
    companion object {
        private const val LIST_STATE_KEY = "list_state"

        const val KEY_PRODUCT_DETAIL_RESULT = "product_detail_result"
        const val KEY_PRODUCT_DETAIL_DID_TRASH = "product_detail_did_trash"
        const val KEY_REMOTE_PRODUCT_ID = "remote_product_id"
    }

    private var productName = ""
        set(value) {
            field = value
            updateActivityTitle()
        }

    private var productId: Long = ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID

    private val skeletonView = SkeletonView()

    private var progressDialog: CustomProgressDialog? = null
    private var layoutManager: LayoutManager? = null

    private val publishTitleId = R.string.product_add_tool_bar_menu_button_done
    private val updateTitleId = R.string.update

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_detail, container, false)
    }

    override fun onDestroyView() {
        skeletonView.hide()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(savedInstanceState)
        initializeViewModel()
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        this.layoutManager = layoutManager

        savedInstanceState?.getParcelable<Parcelable>(LIST_STATE_KEY)?.let {
            layoutManager.onRestoreInstanceState(it)
        }
        cardsRecyclerView.layoutManager = layoutManager
        cardsRecyclerView.itemAnimator = null
    }

    private fun initializeViewModel() {
        setupObservers(viewModel)
        setupResultHandlers(viewModel)
    }

    private fun setupResultHandlers(viewModel: ProductDetailViewModel) {
        handleResult<ProductType>(ProductTypesBottomSheetFragment.KEY_PRODUCT_TYPE_RESULT) {
            viewModel.updateProductDraft(type = it.value)
            changesMade()
        }
        handleResult<List<Long>>(GroupedProductListType.GROUPED.resultKey) {
            viewModel.updateProductDraft(groupedProductIds = it)
            changesMade()
        }
        handleResult<PricingData>(BaseProductEditorFragment.KEY_PRICING_DIALOG_RESULT) {
            viewModel.updateProductDraft(
                regularPrice = it.regularPrice,
                salePrice = it.salePrice,
                saleStartDate = it.saleStartDate,
                saleEndDate = Optional(it.saleEndDate),
                isSaleScheduled = it.isSaleScheduled,
                taxClass = it.taxClass,
                taxStatus = it.taxStatus
            )
            changesMade()
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
            changesMade()
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
            changesMade()
        }
        handleResult<List<Product.Image>>(BaseProductEditorFragment.KEY_IMAGES_DIALOG_RESULT) {
            viewModel.updateProductDraft(
                images = it
            )
            changesMade()
        }

        handleResult<List<Image>>(WPMediaPickerFragment.KEY_WP_IMAGE_PICKER_RESULT) {
            viewModel.showAddProductDownload(it.first().source)
            changesMade()
        }
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.productDetailViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.productDraft?.takeIfNotEqualTo(old?.productDraft) { showProductDetails(it) }
            new.isProductUpdated?.takeIfNotEqualTo(old?.isProductUpdated) { showUpdateMenuItem(it) }
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                if (it) {
                    showProgressDialog(R.string.product_update_dialog_title, R.string.product_update_dialog_message)
                } else {
                    hideProgressDialog()
                }
            }
            new.uploadingImageUris?.takeIfNotEqualTo(old?.uploadingImageUris) {
                imageGallery.setPlaceholderImageUris(it)
            }
            new.showBottomSheetButton?.takeIfNotEqualTo(old?.showBottomSheetButton) { isVisible ->
                productDetail_addMoreContainer.isVisible = isVisible
            }
            new.isUploadingDownloadableFile?.takeIfNotEqualTo(old?.isUploadingDownloadableFile) {
                if (it) {
                    showProgressDialog(
                        title = R.string.product_downloadable_files_upload_dialog_title,
                        message = R.string.product_downloadable_files_upload_dialog_message)
                } else {
                    hideProgressDialog()
                }
            }
        }

        viewModel.productDetailCards.observe(viewLifecycleOwner, Observer {
            showProductCards(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is LaunchUrlInChromeTab -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }
                is RefreshMenu -> activity?.invalidateOptionsMenu()
                is ExitWithResult<*> -> {
                    navigateBackWithResult(KEY_PRODUCT_DETAIL_RESULT, Bundle().also {
                        it.putLong(KEY_REMOTE_PRODUCT_ID, event.data as Long)
                        it.putBoolean(KEY_PRODUCT_DETAIL_DID_TRASH, true)
                    })
                }
                else -> event.isHandled = false
            }
        })
    }

    private fun showProductDetails(product: Product) {
        productName = updateProductNameFromDetails(product)
        productId = product.remoteId

        if (product.images.isEmpty() && !viewModel.isUploadingImages(product.remoteId)) {
            imageGallery.hide()
            startAddImageContainer()
        } else {
            addImageContainer.hide()
            imageGallery.show()
            imageGallery.showProductImages(product.images, this)
        }

        // show status badge for unpublished products
        product.status?.let { status ->
            if (status != ProductStatus.PUBLISH) {
                frameStatusBadge.show()
                textStatusBadge.text = status.toLocalizedString(requireActivity())
            }
        }

        productDetail_addMoreContainer.setOnClickListener {
            // TODO: add tracking events here
            viewModel.onEditProductCardClicked(
                ViewProductDetailBottomSheet(product.productType)
            )
        }

        requireActivity().invalidateOptionsMenu()
    }

    private fun updateProductNameFromDetails(product: Product): String {
        return if (viewModel.isAddFlow && product.name.isEmpty()) {
            getString(R.string.product_add_tool_bar_title)
        } else product.name.fastStripHtml()
    }

    private fun startAddImageContainer() {
        addImageContainer.show()
        addImageContainer.setOnClickListener {
            AnalyticsTracker.track(Stat.PRODUCT_DETAIL_ADD_IMAGE_TAPPED)
            viewModel.onAddImageButtonClicked()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_product_detail_fragment, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        fun Menu.printItems(): String = buildString {
            this@printItems.forEach {
                append("${resources.getResourceName(it.itemId)}\n")
            }
        }

        if (menu.findItem(R.id.menu_view_product) == null) {
            val message = """menu.findItem(R.id.menu_view_product) is null
                |User is ${if (viewModel.isAddFlow) "creating a product" else "modifying a product"}
                |menu elements:
                |${menu.printItems()}
            """.trimMargin()
            CrashUtils.logException(NullPointerException(message))
        }

        // visibility of these menu items depends on whether we're in the add product flow
        menu.findItem(R.id.menu_view_product)?.isVisible = viewModel.isProductPublished && !viewModel.isAddFlow
        menu.findItem(R.id.menu_share)?.isVisible = !viewModel.isAddFlow
        menu.findItem(R.id.menu_product_settings)?.isVisible = true

        // change the font color of the trash menu item to red, and only show it if it should be enabled
        with(menu.findItem(R.id.menu_trash_product)) {
            if (this == null) return@with
            val title = SpannableString(this.title)
            title.setSpan(ForegroundColorSpan(Color.RED), 0, title.length, 0)
            this.setTitle(title)
            this.isVisible = viewModel.isTrashEnabled
        }

        menu.findItem(R.id.menu_save_as_draft)?.isVisible = viewModel.isAddFlow && viewModel.hasChanges()

        doneOrUpdateMenuItem?.let {
            it.title = if (viewModel.isAddFlow) getString(publishTitleId) else getString(updateTitleId)
            it.isVisible = viewModel.hasChanges()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_save_as_draft -> {
                viewModel.onSaveAsDraftButtonClicked()
                true
            }

            R.id.menu_share -> {
                viewModel.onShareButtonClicked()
                true
            }

            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onUpdateButtonClicked()
                true
            }

            R.id.menu_view_product -> {
                viewModel.onViewProductOnStoreLinkClicked()
                true
            }

            R.id.menu_product_settings -> {
                viewModel.onSettingsButtonClicked()
                true
            }

            R.id.menu_trash_product -> {
                viewModel.onTrashButtonClicked()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(app_bar_layout, R.layout.skeleton_product_detail, delayed = true)
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

    private fun getSubmitDetailProgressDialog(): CustomProgressDialog {
        val title: Int
        val message: Int
        when (viewModel.isAddFlow) {
            true -> {
                title = if (viewModel.isDraftProduct()) {
                    R.string.product_publish_draft_dialog_title
                } else {
                    R.string.product_publish_dialog_title
                }
                message = R.string.product_publish_dialog_message
            }
            else -> {
                title = R.string.product_update_dialog_title
                message = R.string.product_update_dialog_message
            }
        }
        return CustomProgressDialog.show(getString(title), getString(message))
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showProductCards(cards: List<ProductPropertyCard>) {
        val adapter: ProductPropertyCardsAdapter
        if (cardsRecyclerView.adapter == null) {
            adapter = ProductPropertyCardsAdapter()
            cardsRecyclerView.adapter = adapter
        } else {
            adapter = cardsRecyclerView.adapter as ProductPropertyCardsAdapter
        }

        val recyclerViewState = cardsRecyclerView.layoutManager?.onSaveInstanceState()
        adapter.update(cards)
        cardsRecyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        layoutManager?.let {
            outState.putParcelable(LIST_STATE_KEY, it.onSaveInstanceState())
        }
    }

    override fun onNavigationResult(requestCode: Int, result: Bundle) {
        when (requestCode) {
            RequestCodes.AZTEC_EDITOR_PRODUCT_DESCRIPTION -> {
                if (result.getBoolean(AztecEditorFragment.ARG_AZTEC_HAS_CHANGES)) {
                    viewModel.updateProductDraft(description = result.getString(ARG_AZTEC_EDITOR_TEXT))
                    changesMade()
                }
            }
            RequestCodes.AZTEC_EDITOR_PRODUCT_SHORT_DESCRIPTION -> {
                if (result.getBoolean(AztecEditorFragment.ARG_AZTEC_HAS_CHANGES)) {
                    viewModel.updateProductDraft(shortDescription = result.getString(ARG_AZTEC_EDITOR_TEXT))
                    changesMade()
                }
            }
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitProductDetail())
    }

    override fun onGalleryImageClicked(image: Product.Image) {
        viewModel.onImageClicked(image)
    }

    override fun onGalleryAddImageClicked() {
        AnalyticsTracker.track(Stat.PRODUCT_DETAIL_ADD_IMAGE_TAPPED)
        viewModel.onAddImageButtonClicked()
    }

    override fun getFragmentTitle(): String = productName

    /**
     * Override the BaseProductFragment's fun since we want to return True if any changes have been
     * made to the product draft
     */
    override fun hasChanges() = viewModel.hasChanges()
}
