package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_UPDATE_BUTTON_TAPPED
import com.woocommerce.android.databinding.FragmentVariationDetailBinding
import com.woocommerce.android.extensions.*
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.ui.aztec.AztecEditorFragment
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.BaseProductEditorFragment
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.adapters.ProductPropertyCardsAdapter
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.variations.VariationDetailViewModel.HideImageUploadErrorSnackbar
import com.woocommerce.android.ui.products.variations.attributes.edit.EditVariationAttributesFragment.Companion.KEY_VARIATION_ATTRIBUTES_RESULT
import com.woocommerce.android.util.Optional
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.*
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCProductImageGalleryView.OnGalleryImageInteractionListener
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

@AndroidEntryPoint
class VariationDetailFragment :
    BaseFragment(R.layout.fragment_variation_detail),
    BackPressListener,
    OnGalleryImageInteractionListener {
    companion object {
        private const val LIST_STATE_KEY = "list_state"
        const val KEY_VARIATION_DETAILS_RESULT = "key_variation_details_result"
    }

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
    private var imageUploadErrorsSnackbar: Snackbar? = null

    private val viewModel: VariationDetailViewModel by viewModels()

    private var _binding: FragmentVariationDetailBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentVariationDetailBinding.bind(view)

        setHasOptionsMenu(true)
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
            R.id.menu_delete -> {
                viewModel.onDeleteVariationClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        this.layoutManager = layoutManager

        savedInstanceState?.getParcelable<Parcelable>(LIST_STATE_KEY)?.let {
            layoutManager.onRestoreInstanceState(it)
        }
        binding.cardsRecyclerView.layoutManager = layoutManager
        binding.cardsRecyclerView.itemAnimator = null
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
                saleStartDate = it.saleStartDate,
                saleEndDate = it.saleEndDate
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
        handleResult<List<Image>>(BaseProductEditorFragment.KEY_IMAGES_DIALOG_RESULT) { updatedImage ->
            viewModel.onVariationChanged(image = Optional(updatedImage.firstOrNull()))
        }
        handleResult<Bundle>(AztecEditorFragment.AZTEC_EDITOR_RESULT) { result ->
            if (result.getBoolean(AztecEditorFragment.ARG_AZTEC_HAS_CHANGES)) {
                viewModel.onVariationChanged(
                    description = result.getString(AztecEditorFragment.ARG_AZTEC_EDITOR_TEXT)
                )
            }
        }
        handleResult<Array<VariantOption>>(KEY_VARIATION_ATTRIBUTES_RESULT) {
            viewModel.onVariationChanged(attributes = it)
        }
    }

    private fun setupObservers(viewModel: VariationDetailViewModel) {
        viewModel.variationViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.variation.takeIfNotEqualTo(old?.variation) { newVariation ->
                newVariation?.let {
                    variationName = it.getName(new.parentProduct)
                    showVariationDetails(it)
                }
            }
            new.parentProduct.takeIfNotEqualTo(old?.parentProduct) { product ->
                new.variation?.let {
                    variationName = it.getName(product)
                }
            }
            new.uploadingImageUri.takeIfNotEqualTo(old?.uploadingImageUri) {
                if (it != null) {
                    binding.imageGallery.clearImages()
                    binding.imageGallery.setPlaceholderImageUris(listOf(it))
                } else {
                    binding.imageGallery.clearPlaceholders()
                }
            }
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                showProgressDialog(it, R.string.product_save_dialog_title)
            }
            new.isDoneButtonVisible?.takeIfNotEqualTo(old?.isDoneButtonVisible) {
                doneOrUpdateMenuItem?.isVisible = it
            }
            new.isDoneButtonEnabled?.takeIfNotEqualTo(old?.isDoneButtonEnabled) {
                doneOrUpdateMenuItem?.isEnabled = it
            }
            new.isDeleteDialogShown?.takeIfNotEqualTo(old?.isDeleteDialogShown) {
                showProgressDialog(it, R.string.product_delete_dialog_title)
            }
        }

        viewModel.variationDetailCards.observe(viewLifecycleOwner) {
            showVariationCards(it)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowActionSnackbar -> displayProductImageUploadErrorSnackBar(event.message, event.action)
                is HideImageUploadErrorSnackbar -> imageUploadErrorsSnackbar?.dismiss()
                is VariationNavigationTarget -> {
                    navigator.navigate(this, event)
                }
                is ExitWithResult<*> -> navigateBackWithResult(KEY_VARIATION_DETAILS_RESULT, event.data)
                is ShowDialog -> event.showDialog()
                is Exit -> requireActivity().onBackPressed()
                else -> event.isHandled = false
            }
        }
    }

    private fun showVariationDetails(variation: ProductVariation) {
        if (variation.image == null && !viewModel.isUploadingImages()) {
            binding.imageGallery.hide()
            binding.addImageContainer.show()
            binding.addImageContainer.setOnClickListener {
                AnalyticsTracker.track(Stat.PRODUCT_DETAIL_ADD_IMAGE_TAPPED)
                viewModel.onAddImageButtonClicked()
            }
        } else {
            binding.addImageContainer.hide()
            binding.imageGallery.show()
            variation.image?.let {
                binding.imageGallery.showProductImage(it, this)
            }
        }
    }

    override fun onGalleryImageClicked(image: Image) {
        viewModel.onImageClicked(image)
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(binding.appBarLayout, R.layout.skeleton_variation_detail, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showProgressDialog(show: Boolean, @StringRes title: Int) {
        if (show) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                getString(title),
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

    private fun displayProductImageUploadErrorSnackBar(
        message: String,
        actionListener: View.OnClickListener
    ) {
        if (imageUploadErrorsSnackbar == null) {
            imageUploadErrorsSnackbar = uiMessageResolver.getIndefiniteActionSnack(
                message = message,
                actionText = getString(R.string.details),
                actionListener = actionListener
            )
        } else {
            imageUploadErrorsSnackbar?.setText(message)
        }
        imageUploadErrorsSnackbar?.show()
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

    override fun getFragmentTitle() = variationName
}
