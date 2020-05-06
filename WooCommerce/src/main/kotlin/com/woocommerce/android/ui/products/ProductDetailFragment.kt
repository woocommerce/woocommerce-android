package com.woocommerce.android.ui.products

import android.os.Bundle
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
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
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
import com.woocommerce.android.ui.products.adapters.ProductDetailCardsAdapter
import com.woocommerce.android.ui.products.models.ProductDetailCard
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCProductImageGalleryView.OnGalleryImageClickListener
import kotlinx.android.synthetic.main.fragment_product_detail.*
import org.wordpress.android.util.ActivityUtils

class ProductDetailFragment : BaseProductFragment(), OnGalleryImageClickListener, NavigationResult {
    private var productName = ""
    private val skeletonView = SkeletonView()

    private var progressDialog: CustomProgressDialog? = null

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
        initializeViews()
        initializeViewModel()
    }

    private fun initializeViews() {
        cardsRecyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
    }

    private fun initializeViewModel() {
        setupObservers(viewModel)
        viewModel.start(navArgs.remoteProductId)
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.productDetailViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isProductUpdated?.takeIfNotEqualTo(old?.isProductUpdated) { showUpdateProductAction(it) }
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) { showProgressDialog(it) }
            new.uploadingImageUris?.takeIfNotEqualTo(old?.uploadingImageUris) {
                imageGallery.setPlaceholderImageUris(it)
            }
        }

        viewModel.productDetailCards.observe(viewLifecycleOwner, Observer {
            showProductCards(viewModel.productDetailViewStateData.liveData.value!!.productDraft!!, it)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_product_detail_fragment, menu)

        menu.findItem(R.id.menu_view_product).isVisible = FeatureFlag.PRODUCT_RELEASE_M2.isEnabled()
        menu.findItem(R.id.menu_product_settings).isVisible = FeatureFlag.PRODUCT_RELEASE_M2.isEnabled()

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

    private fun loadCards(recyclerView: RecyclerView, data: List<ProductDetailCard>) {
        val adapter: ProductDetailCardsAdapter
        if (recyclerView.adapter == null) {
            adapter = ProductDetailCardsAdapter()
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as ProductDetailCardsAdapter
        }

        val recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
        adapter.update(data)
        recyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    private fun showProductCards(product: Product, cards: List<ProductDetailCard>) {
        productName = product.name.fastStripHtml()
        updateActivityTitle()

        if (product.images.isEmpty() && !viewModel.isUploadingImages(product.remoteId)) {
            imageGallery.visibility = View.GONE
            if (FeatureFlag.PRODUCT_RELEASE_M2.isEnabled(requireActivity())) {
                addImageContainer.visibility = View.VISIBLE
                addImageContainer.setOnClickListener {
                    viewModel.onAddImageClicked()
                }
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
        }

        loadCards(cardsRecyclerView, cards)
    }

    override fun onNavigationResult(requestCode: Int, result: Bundle) {
        when (requestCode) {
            RequestCodes.AZTEC_EDITOR_PRODUCT_DESCRIPTION -> {
                if (result.getBoolean(AztecEditorFragment.ARG_AZTEC_HAS_CHANGES)) {
                    viewModel.updateProductDraft(description = result.getString(ARG_AZTEC_EDITOR_TEXT))
                }
            }
            RequestCodes.AZTEC_EDITOR_PRODUCT_SHORT_DESCRIPTION -> {
                if (result.getBoolean(AztecEditorFragment.ARG_AZTEC_HAS_CHANGES)) {
                    viewModel.updateProductDraft(shortDescription = result.getString(ARG_AZTEC_EDITOR_TEXT))
                }
            }
            RequestCodes.WPMEDIA_LIBRARY_PICKER -> {
                result.getParcelableArrayList<Product.Image>(WPMediaPickerFragment.ARG_SELECTED_IMAGES)
                        ?.let {
                            viewModel.addProductImageListToDraft(it)
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
        viewModel.onAddImageClicked()
    }
}
