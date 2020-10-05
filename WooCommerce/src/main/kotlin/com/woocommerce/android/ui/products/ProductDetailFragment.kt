package com.woocommerce.android.ui.products

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_ADD_IMAGE_TAPPED
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.aztec.AztecEditorFragment
import com.woocommerce.android.ui.aztec.AztecEditorFragment.Companion.ARG_AZTEC_EDITOR_TEXT
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
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.Optional
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCProductImageGalleryView.OnGalleryImageClickListener
import kotlinx.android.synthetic.main.fragment_product_detail.*
import org.wordpress.android.util.ActivityUtils

class ProductDetailFragment : BaseProductFragment(), OnGalleryImageClickListener, NavigationResult {
    companion object {
        private const val LIST_STATE_KEY = "list_state"
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
        // hide the skeleton view if fragment is destroyed
        skeletonView.hide()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
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
            viewModel.updateProductDraft(type = it)
            changesMade()
        }
        handleResult<List<Long>>(GroupedProductListFragment.KEY_GROUPED_PRODUCT_IDS_RESULT) {
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
        handleResult<List<Image>>(BaseProductEditorFragment.KEY_IMAGES_DIALOG_RESULT) {
            viewModel.updateProductDraft(
                images = it
            )
            changesMade()
        }
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.productDetailViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.productDraft?.takeIfNotEqualTo(old?.productDraft) { showProductDetails(it) }
            new.isProductUpdated?.takeIfNotEqualTo(old?.isProductUpdated) { showUpdateMenuItem(it) }
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                showProgressDialog(it)
            }
            new.uploadingImageUris?.takeIfNotEqualTo(old?.uploadingImageUris) {
                imageGallery.setPlaceholderImageUris(it)
            }
            new.showBottomSheetButton?.takeIfNotEqualTo(old?.showBottomSheetButton) { isVisible ->
                productDetail_addMoreContainer.isVisible = isVisible
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
                ViewProductDetailBottomSheet(product.type)
            )
        }

        updateOptionsMenuDescription(product.remoteId)
    }

    private fun updateProductNameFromDetails(product: Product): String {
        return if (viewModel.isAddFlow && product.name.isEmpty()) {
            getString(string.product_add_tool_bar_title)
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

        // display View Product on Store menu button only if the Product status is published,
        // otherwise the page is redirected to a 404
        menu.findItem(R.id.menu_view_product).isVisible = viewModel.isProductPublished && !viewModel.isAddFlow
        menu.findItem(R.id.menu_share).isVisible = !viewModel.isAddFlow
        menu.findItem(R.id.menu_product_settings).isVisible = true

        when (viewModel.isAddFlow) {
            true -> setupProductAddOptionsMenu(menu)
            else -> Unit
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setupProductAddOptionsMenu(menu: Menu) {
        doneOrUpdateMenuItem?.let {
            it.title = getString(publishTitleId)
            it.isVisible = true
        }
    }

    private fun updateOptionsMenuDescription(remoteId: Long) {
        val doneButtonTitle =
            when (viewModel.isAddFlow) {
                true -> getString(publishTitleId)
                else -> getString(updateTitleId)
            }
        doneOrUpdateMenuItem?.title = doneButtonTitle
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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

    private fun showProgressDialog(show: Boolean) {
        if (show) {
            hideProgressDialog()
            progressDialog = getSubmitDetailProgressDialog().also {
                it.show(parentFragmentManager, CustomProgressDialog.TAG)
            }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    private fun getSubmitDetailProgressDialog(): CustomProgressDialog {
        val title: Int
        val message: Int
        when (viewModel.isAddFlow) {
            true -> {
                title = R.string.product_publish_dialog_title
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
