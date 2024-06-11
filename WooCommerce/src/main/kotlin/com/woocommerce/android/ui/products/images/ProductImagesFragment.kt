package com.woocommerce.android.ui.products.images

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductImagesBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.mediapicker.MediaPickerHelper
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.BaseProductEditorFragment
import com.woocommerce.android.ui.products.ConfirmRemoveProductImageDialog
import com.woocommerce.android.ui.products.ProductNavigationTarget
import com.woocommerce.android.ui.products.ProductNavigator
import com.woocommerce.android.ui.products.images.ProductImagesViewModel.ProductImagesState
import com.woocommerce.android.ui.products.images.ProductImagesViewModel.ShowCamera
import com.woocommerce.android.ui.products.images.ProductImagesViewModel.ShowDeleteImageConfirmation
import com.woocommerce.android.ui.products.images.ProductImagesViewModel.ShowImageDetail
import com.woocommerce.android.ui.products.images.ProductImagesViewModel.ShowImageSourceDialog
import com.woocommerce.android.ui.products.images.ProductImagesViewModel.ShowStorageChooser
import com.woocommerce.android.ui.products.images.ProductImagesViewModel.ShowWPMediaPicker
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.setHomeIcon
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowActionSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import com.woocommerce.android.widgets.WCProductImageGalleryView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.mediapicker.MediaPickerUtils
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.CAMERA
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.WP_MEDIA_LIBRARY
import javax.inject.Inject

@AndroidEntryPoint
class ProductImagesFragment :
    BaseProductEditorFragment(R.layout.fragment_product_images),
    WCProductImageGalleryView.OnGalleryImageInteractionListener,
    MediaPickerHelper.MediaPickerResultHandler {
    private val navArgs: ProductImagesFragmentArgs by navArgs()
    private val viewModel: ProductImagesViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_image_gallery)

    @Inject
    lateinit var navigator: ProductNavigator

    @Inject
    lateinit var mediaPickerUtils: MediaPickerUtils

    @Inject
    lateinit var mediaPickerHelper: MediaPickerHelper

    private var _binding: FragmentProductImagesBinding? = null
    private val binding get() = _binding!!

    override val lastEvent: MultiLiveEvent.Event?
        get() = viewModel.event.value

    private var imageSourceDialog: AlertDialog? = null
    private var imageUploadErrorsSnackbar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductImagesBinding.bind(view)

        setupObservers(viewModel)
        setupViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        imageUploadErrorsSnackbar?.dismiss()
    }

    override fun onPause() {
        super.onPause()
        imageSourceDialog?.dismiss()
    }

    private fun onCreateMenu(toolbar: Toolbar) {
        toolbar.setNavigationOnClickListener {
            onExit()
        }
        updateMenuState(toolbar)
    }

    private fun updateMenuState(toolbar: Toolbar) {
        toolbar.menu.clear()
        when (viewModel.viewStateData.liveData.value?.productImagesState) {
            is ProductImagesState.Dragging -> {
                toolbar.inflateMenu(R.menu.menu_dragging)
                setHomeIcon(R.drawable.ic_gridicons_cross_24dp)
            }

            ProductImagesState.Browsing -> {
                setHomeIcon(R.drawable.ic_back_24dp)
            }

            null -> Unit // Do nothing
        }
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (viewModel.viewStateData.liveData.value?.productImagesState) {
            is ProductImagesState.Dragging -> {
                when (item.itemId) {
                    R.id.menu_validate -> {
                        viewModel.onValidateButtonClicked()
                        true
                    }

                    else -> false
                }
            }

            else -> false
        }
    }

    private fun setupViews() {
        binding.addImageButton.setOnClickListener {
            viewModel.onImageSourceButtonClicked()
        }
        with(binding.learnMoreButton) {
            text = HtmlCompat.fromHtml(
                context.getString(R.string.product_images_learn_more_button),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            movementMethod = LinkMovementMethod.getInstance()
            setOnClickListener {
                ChromeCustomTabUtils.launchUrl(it.context, AppUrls.PRODUCT_IMAGE_UPLOADS_TROUBLESHOOTING)
            }
        }

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_images_title),
            onMenuItemSelected = ::onMenuItemSelected,
            onCreateMenu = ::onCreateMenu
        )
    }

    override fun onGalleryImageDeleteIconClicked(image: Product.Image) {
        viewModel.onGalleryImageDeleteIconClicked(image)
    }

    private fun setupObservers(viewModel: ProductImagesViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.uploadingImageUris.takeIfNotEqualTo(old?.uploadingImageUris) { uris ->
                updateImages(new.images ?: emptyList(), uris)
            }
            new.images.takeIfNotEqualTo(old?.images) { images ->
                updateImages(images ?: emptyList(), new.uploadingImageUris)
            }
            new.isWarningVisible?.takeIfNotEqualTo(old?.isWarningVisible) { isVisible ->
                binding.textWarning.isVisible = isVisible
            }
            new.chooserButtonButtonTitleRes?.takeIfNotEqualTo(old?.chooserButtonButtonTitleRes) { titleRes ->
                binding.addImageButton.setText(titleRes)
            }
            new.productImagesState.takeIfNotEqualTo(old?.productImagesState) {
                updateMenuState(binding.toolbar)

                when (new.productImagesState) {
                    ProductImagesState.Browsing -> {
                        binding.addImageButton.isEnabled = true
                        binding.imageGallery.setDraggingState(isDragging = false)
                    }

                    is ProductImagesState.Dragging -> {
                        binding.addImageButton.isEnabled = false
                        binding.imageGallery.setDraggingState(isDragging = true)
                    }
                }
            }
            new.isDragDropDescriptionVisible?.takeIfNotEqualTo(old?.isDragDropDescriptionVisible) { isVisible ->
                binding.dragAndDropDescription.isVisible = isVisible
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().navigateUp()
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowActionSnackbar -> displayProductImageUploadErrorSnackBar(
                    event.message,
                    event.actionText,
                    event.action
                )
                is ProductNavigationTarget -> navigator.navigate(this, event)
                is ExitWithResult<*> -> navigateBackWithResult(KEY_IMAGES_DIALOG_RESULT, event.data)
                is ShowDialog -> event.showDialog()
                ShowImageSourceDialog -> showImageSourceDialog()
                is ShowImageDetail -> showImageDetail(event.image, event.isOpenedDirectly)
                ShowStorageChooser -> mediaPickerHelper.showMediaPicker(DEVICE, allowMultiSelect = true)
                ShowCamera -> mediaPickerHelper.showMediaPicker(CAMERA)
                ShowWPMediaPicker -> mediaPickerHelper.showMediaPicker(WP_MEDIA_LIBRARY, allowMultiSelect = true)
                is ShowDeleteImageConfirmation -> showConfirmationDialog(event.image)
                else -> event.isHandled = false
            }
        }
    }

    private fun showConfirmationDialog(image: Product.Image) {
        ConfirmRemoveProductImageDialog(
            requireActivity(),
            onPositiveButton = { viewModel.onDeleteImageConfirmed(image) },
            onNegativeButton = { /* no-op */ }
        ).show()
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

    private fun updateImages(images: List<Product.Image>, uris: List<Uri>?) {
        binding.imageGallery.showProductImages(images, this)
        binding.imageGallery.setPlaceholderImageUris(uris)
    }

    override fun onGalleryImageClicked(image: Product.Image) {
        viewModel.onGalleryImageClicked(image)
    }

    override fun onGalleryImageDragStarted() {
        viewModel.onGalleryImageDragStarted()
    }

    override fun onGalleryImageMoved(from: Int, to: Int) {
        viewModel.onGalleryImageMoved(from, to)
    }

    private fun showImageDetail(image: Product.Image, skipThrottling: Boolean) {
        val action = ProductImageViewerFragmentDirections.actionGlobalProductImageViewerFragment(
            isDeletingAllowed = viewModel.isImageDeletingAllowed,
            mediaId = image.id,
            remoteId = navArgs.remoteId,
            requestCode = navArgs.requestCode,
            selectedImage = null,
            showChooser = false,
            images = viewModel.images.toTypedArray()
        )
        findNavController().navigateSafely(action, skipThrottling)
    }

    private fun showImageSourceDialog() {
        val inflater = requireActivity().layoutInflater
        val contentView = inflater.inflate(R.layout.dialog_product_image_source, binding.imageGallery, false)
            .also {
                it.findViewById<View>(R.id.textChooser)?.setOnClickListener {
                    viewModel.onShowStorageChooserButtonClicked()
                }
                it.findViewById<View>(R.id.textCamera)?.apply {
                    isVisible = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                    setOnClickListener {
                        viewModel.onShowCameraButtonClicked()
                    }
                }
                it.findViewById<View>(R.id.textWPMediaLibrary)?.setOnClickListener {
                    viewModel.onShowWPMediaPickerButtonClicked()
                }
            }

        imageSourceDialog = MaterialAlertDialogBuilder(requireActivity())
            .setView(contentView)
            .show()
    }

    override fun onExit() {
        viewModel.onNavigateBackButtonClicked()
    }

    override fun onDeviceMediaSelected(imageUris: List<Uri>, source: String) {
        if (imageUris.isNotEmpty()) {
            AnalyticsTracker.track(
                AnalyticsEvent.PRODUCT_IMAGE_ADDED,
                mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to source)
            )
            viewModel.uploadProductImages(navArgs.remoteId, imageUris)
        }
    }

    override fun onWPMediaSelected(images: List<Product.Image>) {
        AnalyticsTracker.track(
            AnalyticsEvent.PRODUCT_IMAGE_ADDED,
            mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to AnalyticsTracker.IMAGE_SOURCE_WPMEDIA)
        )
        viewModel.onMediaLibraryImagesAdded(images)
    }
}
