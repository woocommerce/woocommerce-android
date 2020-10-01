package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_UPDATE_BUTTON_TAPPED
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.aztec.AztecEditorFragment
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import com.woocommerce.android.ui.products.BaseProductEditorFragment
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.adapters.ProductPropertyCardsAdapter
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.util.Optional
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCProductImageGalleryView.OnGalleryImageClickListener
import kotlinx.android.synthetic.main.fragment_variation_detail.addImageContainer
import kotlinx.android.synthetic.main.fragment_variation_detail.app_bar_layout
import kotlinx.android.synthetic.main.fragment_variation_detail.cardsRecyclerView
import kotlinx.android.synthetic.main.fragment_variation_detail.imageGallery
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class VariationDetailFragment : BaseFragment(), BackPressListener, NavigationResult, OnGalleryImageClickListener {
    companion object {
        private const val LIST_STATE_KEY = "list_state"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var navigator: VariationNavigator

    private var doneOrUpdateMenuItem: MenuItem? = null

    private var variationName = ""
        set(value) {
            field = value
            updateActivityTitle()
        }

    private val skeletonView = SkeletonView()
    private var progressDialog: CustomProgressDialog? = null
    private var layoutManager: LayoutManager? = null

    private val viewModel: VariationDetailViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_variation_detail, container, false)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_variation_detail_fragment, menu)
        doneOrUpdateMenuItem = menu.findItem(R.id.menu_done)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        doneOrUpdateMenuItem?.isVisible = viewModel.variationViewStateData.liveData.value?.isDoneButtonVisible ?: false
        doneOrUpdateMenuItem?.isEnabled = viewModel.variationViewStateData.liveData.value?.isDoneButtonEnabled ?: true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                AnalyticsTracker.track(PRODUCT_VARIATION_UPDATE_BUTTON_TAPPED)
                ActivityUtils.hideKeyboard(activity)
                viewModel.onUpdateButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    private fun setupResultHandlers(viewModel: VariationDetailViewModel) {
        handleResult<PricingData>(BaseProductEditorFragment.KEY_PRICING_DIALOG_RESULT) {
            viewModel.onVariationChanged(
                regularPrice = it.regularPrice,
                salePrice = it.salePrice,
                isSaleScheduled = it.isSaleScheduled,
                saleStartDate = Optional(it.saleStartDate),
                saleEndDate = Optional(it.saleEndDate)
            )
        }
        handleResult<InventoryData>(BaseProductEditorFragment.KEY_INVENTORY_DIALOG_RESULT) {
            viewModel.onVariationChanged(
                sku = it.sku,
                stockStatus = it.stockStatus,
                stockQuantity = it.stockQuantity,
                backorderStatus = it.backorderStatus,
                isStockManaged = it.isStockManaged
            )
        }
        handleResult<ShippingData>(BaseProductEditorFragment.KEY_SHIPPING_DIALOG_RESULT) {
            viewModel.onVariationChanged(
                weight = it.weight,
                length = it.length,
                width = it.width,
                height = it.height,
                shippingClass = it.shippingClassSlug,
                shippingClassId = it.shippingClassId
            )
        }
        handleResult<List<Image>>(BaseProductEditorFragment.KEY_IMAGES_DIALOG_RESULT) {
            if (it.isNotEmpty()) {
                viewModel.onVariationChanged(
                    image = it.first()
                )
            }
        }
    }

    private fun setupObservers(viewModel: VariationDetailViewModel) {
        viewModel.variationViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.variation.takeIfNotEqualTo(old?.variation) { showVariationDetails(it) }
            new.parentProduct.takeIfNotEqualTo(old?.parentProduct) { product ->
                if (variationName.isEmpty()) {
                    variationName = product?.attributes?.joinToString(
                        separator = " - ",
                        transform = { "Any ${it.name}" }
                    ) ?: ""
                }
            }
            new.uploadingImageUri?.takeIfNotEqualTo(old?.uploadingImageUri) {
                if (it.value != null) {
                    imageGallery.clearImages()
                    imageGallery.setPlaceholderImageUris(listOf(it.value))
                } else {
                    imageGallery.clearPlaceholders()
                }
            }
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) { showProgressDialog(it) }
            new.isDoneButtonVisible?.takeIfNotEqualTo(old?.isDoneButtonVisible) {
                doneOrUpdateMenuItem?.isVisible = it
            }
            new.isDoneButtonEnabled?.takeIfNotEqualTo(old?.isDoneButtonEnabled) {
                doneOrUpdateMenuItem?.isEnabled = it
            }
        }

        viewModel.variationDetailCards.observe(viewLifecycleOwner, Observer {
            showVariationCards(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is VariationNavigationTarget -> {
                    navigator.navigate(this, event)
                }
                is ShowDialog -> event.showDialog()
                is Exit -> requireActivity().onBackPressed()
                else -> event.isHandled = false
            }
        })
    }

    private fun showVariationDetails(variation: ProductVariation) {
        val optionName = variation.optionName.fastStripHtml()
        if (optionName.isNotBlank()) {
            variationName = variation.optionName.fastStripHtml()
        }

        if (variation.image == null && !viewModel.isUploadingImages(variation.remoteVariationId)) {
            imageGallery.hide()
            addImageContainer.show()
            addImageContainer.setOnClickListener {
                AnalyticsTracker.track(Stat.PRODUCT_DETAIL_ADD_IMAGE_TAPPED)
                viewModel.onAddImageButtonClicked()
            }
        } else {
            addImageContainer.hide()
            imageGallery.show()
            variation.image?.let {
                imageGallery.showProductImage(it, this)
            }
        }
    }

    override fun onGalleryImageClicked(image: Image) {
        viewModel.onImageClicked(image)
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(app_bar_layout, R.layout.skeleton_variation_detail, delayed = true)
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

    private fun showVariationCards(cards: List<ProductPropertyCard>) {
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

    override fun onRequestAllowBackPress(): Boolean {
        return if (viewModel.event.value == Exit) {
            true
        } else {
            viewModel.onExit()
            false
        }
    }

    override fun onNavigationResult(requestCode: Int, result: Bundle) {
        when (requestCode) {
            RequestCodes.AZTEC_EDITOR_VARIATION_DESCRIPTION -> {
                if (result.getBoolean(AztecEditorFragment.ARG_AZTEC_HAS_CHANGES)) {
                    viewModel.onVariationChanged(
                        description = result.getString(AztecEditorFragment.ARG_AZTEC_EDITOR_TEXT)
                    )
                }
            }
        }
    }

    override fun getFragmentTitle() = variationName
}
