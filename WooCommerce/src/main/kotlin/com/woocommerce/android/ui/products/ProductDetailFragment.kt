package com.woocommerce.android.ui.products

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_SHARE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_UPDATE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_VIEW_EXTERNAL_TAPPED
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.aztec.AztecEditorFragment
import com.woocommerce.android.ui.aztec.AztecEditorFragment.Companion.ARG_AZTEC_EDITOR_TEXT
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import com.woocommerce.android.ui.products.ProductDetailViewModel.LaunchUrlInChromeTab
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductDetail
import com.woocommerce.android.ui.products.adapters.ProductPropertyCardsAdapter
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCProductImageGalleryView.OnGalleryImageClickListener
import kotlinx.android.synthetic.main.fragment_product_detail.*
import org.wordpress.android.util.ActivityUtils

class ProductDetailFragment : BaseProductFragment(), OnGalleryImageClickListener, NavigationResult {
    private var productName = ""
    private val skeletonView = SkeletonView()
    private val LIST_STATE_KEY = "list_state"

    private var progressDialog: CustomProgressDialog? = null
    private var layoutManager: LayoutManager? = null

    private var viewProductOnStoreMenuItem: MenuItem? = null

    private val navArgs: ProductDetailFragmentArgs by navArgs()

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
        viewModel.start(navArgs.remoteProductId)
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.productDetailViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.productDraft?.takeIfNotEqualTo(old?.productDraft) { showProductDetails(it) }
            new.isProductUpdated?.takeIfNotEqualTo(old?.isProductUpdated) { showUpdateMenuItem(it) }
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) { showProgressDialog(it) }
            new.uploadingImageUris?.takeIfNotEqualTo(old?.uploadingImageUris) {
                imageGallery.setPlaceholderImageUris(it)
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
                else -> event.isHandled = false
            }
        })
    }

    private fun showProductDetails(product: Product) {
        productName = product.name.fastStripHtml()
        updateActivityTitle()

        if (product.images.isEmpty() && !viewModel.isUploadingImages(product.remoteId)) {
            imageGallery.visibility = View.GONE
            addImageContainer.visibility = View.VISIBLE
            addImageContainer.setOnClickListener {
                AnalyticsTracker.track(Stat.PRODUCT_DETAIL_ADD_IMAGE_TAPPED)
                viewModel.onAddImageClicked()
            }
        } else {
            addImageContainer.visibility = View.GONE
            imageGallery.visibility = View.VISIBLE
            imageGallery.showProductImages(product, this)
        }

        // show status badge for unpublished products
        product.status?.let { status ->
            if (status != ProductStatus.PUBLISH) {
                frameStatusBadge.visibility = View.VISIBLE
                textStatusBadge.text = status.toLocalizedString(requireActivity())
            }

            // display View Product on Store menu button only if the Product status is published,
            // otherwise the page is redirected to a 404
            viewProductOnStoreMenuItem?.isVisible = status == ProductStatus.PUBLISH
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_product_detail_fragment, menu)

        viewProductOnStoreMenuItem = menu.findItem(R.id.menu_view_product)
        menu.findItem(R.id.menu_product_settings).isVisible = true

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_share -> {
                AnalyticsTracker.track(PRODUCT_DETAIL_SHARE_BUTTON_TAPPED)
                viewModel.onShareButtonClicked()
                true
            }

            R.id.menu_done -> {
                AnalyticsTracker.track(PRODUCT_DETAIL_UPDATE_BUTTON_TAPPED)
                ActivityUtils.hideKeyboard(activity)
                viewModel.onUpdateButtonClicked()
                true
            }

            R.id.menu_view_product -> {
                viewModel.getProduct().productDraft?.permalink?.let {
                    AnalyticsTracker.track(PRODUCT_DETAIL_VIEW_EXTERNAL_TAPPED)
                    ChromeCustomTabUtils.launchUrl(requireContext(), it)
                }
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
            progressDialog = CustomProgressDialog.show(
                    getString(R.string.product_update_dialog_title),
                    getString(R.string.product_update_dialog_message)
            ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun getFragmentTitle() = productName

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
            RequestCodes.WPMEDIA_LIBRARY_PICKER -> {
                result.getParcelableArrayList<Product.Image>(WPMediaPickerFragment.ARG_SELECTED_IMAGES)
                        ?.let {
                            viewModel.addProductImageListToDraft(it)
                            changesMade()
                        }
            }
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitProductDetail())
    }

    override fun onGalleryImageClicked(image: Product.Image) {
        viewModel.onImageGalleryClicked(image)
    }

    override fun onGalleryAddImageClicked() {
        AnalyticsTracker.track(Stat.PRODUCT_DETAIL_ADD_IMAGE_TAPPED)
        viewModel.onAddImageClicked()
    }

    /**
     * Override the BaseProductFragment's fun since we want to return True if any changes have been
     * made to the product draft
     */
    override fun hasChanges() = viewModel.hasChanges()
}
