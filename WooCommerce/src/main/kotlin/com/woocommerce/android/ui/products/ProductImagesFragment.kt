package com.woocommerce.android.ui.products

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
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
import com.woocommerce.android.mediapicker.MediaPickerUtil.processDeviceMediaResult
import com.woocommerce.android.mediapicker.MediaPickerUtil.processMediaLibraryResult
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.products.ProductImagesViewModel.ProductImagesState.Browsing
import com.woocommerce.android.ui.products.ProductImagesViewModel.ProductImagesState.Dragging
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowCamera
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowDeleteImageConfirmation
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowImageDetail
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowImageSourceDialog
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowStorageChooser
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowWPMediaPicker
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.setHomeIcon
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowActionSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.WCProductImageGalleryView.OnGalleryImageInteractionListener
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.mediapicker.MediaPickerUtils
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.CAMERA
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.WP_MEDIA_LIBRARY
import org.wordpress.android.mediapicker.ui.MediaPickerActivity
import javax.inject.Inject

@AndroidEntryPoint
class ProductImagesFragment :
    BaseProductEditorFragment(R.layout.fragment_product_images), OnGalleryImageInteractionListener {
    private val navArgs: ProductImagesFragmentArgs by navArgs()
    private val viewModel: ProductImagesViewModel by hiltNavGraphViewModels(R.id.nav_graph_image_gallery)

    @Inject lateinit var navigator: ProductNavigator
    @Inject lateinit var mediaPickerUtils: MediaPickerUtils
    @Inject lateinit var mediaPickerSetupFactory: MediaPickerSetup.Factory

    private var _binding: FragmentProductImagesBinding? = null
    private val binding get() = _binding!!

    override val lastEvent: Event?
        get() = viewModel.event.value

    private var imageSourceDialog: AlertDialog? = null
    private var imageUploadErrorsSnackbar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductImagesBinding.bind(view)

        setHasOptionsMenu(true)

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        when (viewModel.viewStateData.liveData.value?.productImagesState) {
            is Dragging -> {
                inflater.inflate(R.menu.menu_dragging, menu)
                setHomeIcon(R.drawable.ic_gridicons_cross_24dp)
            }
            Browsing -> {
                super.onCreateOptionsMenu(menu, inflater)
                setHomeIcon(R.drawable.ic_back_24dp)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (viewModel.viewStateData.liveData.value?.productImagesState) {
            is Dragging -> {
                when (item.itemId) {
                    R.id.menu_validate -> {
                        viewModel.onValidateButtonClicked()
                        true
                    }
                    else -> super.onOptionsItemSelected(item)
                }
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
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
    }

    override fun onGalleryImageDeleteIconClicked(image: Image) {
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
                requireActivity().invalidateOptionsMenu()
                when (new.productImagesState) {
                    Browsing -> {
                        binding.addImageButton.isEnabled = true
                        binding.imageGallery.setDraggingState(isDragging = false)
                    }
                    is Dragging -> {
                        binding.addImageButton.isEnabled = false
                        binding.imageGallery.setDraggingState(isDragging = true)
                    }
                }
            }
            new.isDragDropDescriptionVisible?.takeIfNotEqualTo(old?.isDragDropDescriptionVisible) { isVisible ->
                binding.dragAndDropDescription.isVisible = isVisible
            }
        }

        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is Exit -> findNavController().navigateUp()
                    is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                    is ShowActionSnackbar -> displayProductImageUploadErrorSnackBar(event.message, event.action)
                    is ProductNavigationTarget -> navigator.navigate(this, event)
                    is ExitWithResult<*> -> navigateBackWithResult(KEY_IMAGES_DIALOG_RESULT, event.data)
                    is ShowDialog -> event.showDialog()
                    ShowImageSourceDialog -> showImageSourceDialog()
                    is ShowImageDetail -> showImageDetail(event.image, event.isOpenedDirectly)
                    ShowStorageChooser -> showLocalDeviceMediaPicker()
                    ShowCamera -> captureProductImage()
                    ShowWPMediaPicker -> showMediaLibraryPicker()
                    is ShowDeleteImageConfirmation -> showConfirmationDialog(event.image)
                    else -> event.isHandled = false
                }
            }
        )
    }

    private fun showConfirmationDialog(image: Image) {
        ConfirmRemoveProductImageDialog(
            requireActivity(),
            onPositiveButton = { viewModel.onDeleteImageConfirmed(image) },
            onNegativeButton = { /* no-op */ }
        ).show()
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

    private fun updateImages(images: List<Image>, uris: List<Uri>?) {
        binding.imageGallery.showProductImages(images, this)
        binding.imageGallery.setPlaceholderImageUris(uris)
    }

    override fun getFragmentTitle() = getString(R.string.product_images_title)

    override fun onGalleryImageClicked(image: Image) {
        viewModel.onGalleryImageClicked(image)
    }

    override fun onGalleryImageDragStarted() {
        viewModel.onGalleryImageDragStarted()
    }

    override fun onGalleryImageMoved(from: Int, to: Int) {
        viewModel.onGalleryImageMoved(from, to)
    }

    private fun showImageDetail(image: Image, skipThrottling: Boolean) {
        val action = ProductImageViewerFragmentDirections.actionGlobalProductImageViewerFragment(
            isDeletingAllowed = viewModel.isImageDeletingAllowed,
            mediaId = image.id
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
                    isVisible = mediaPickerUtils.isCameraAvailable
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

    private fun showMediaLibraryPicker() {
        val intent = MediaPickerActivity.buildIntent(
            requireContext(),
            mediaPickerSetupFactory.build(WP_MEDIA_LIBRARY, viewModel.isMultiSelectionAllowed)
        )

        mediaLibraryLauncher.launch(intent)
    }

    private fun showLocalDeviceMediaPicker() {
        val intent = MediaPickerActivity.buildIntent(
            requireContext(),
            mediaPickerSetupFactory.build(DEVICE, viewModel.isMultiSelectionAllowed)
        )

        mediaDeviceMediaPickerLauncher.launch(intent)
    }

    private fun captureProductImage() {
        val intent = MediaPickerActivity.buildIntent(
            requireContext(),
            mediaPickerSetupFactory.build(CAMERA)
        )

        cameraLauncher.launch(intent)
    }

    private val mediaDeviceMediaPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        handleDeviceMediaResult(it, AnalyticsTracker.IMAGE_SOURCE_DEVICE)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        handleDeviceMediaResult(it, AnalyticsTracker.IMAGE_SOURCE_CAMERA)
    }

    private val mediaLibraryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        handleMediaLibraryPickerResult(it)
    }

    private fun handleDeviceMediaResult(result: ActivityResult, source: String) {
        result.processDeviceMediaResult()?.let { mediaUris ->
            if (mediaUris.isNotEmpty()) {
                AnalyticsTracker.track(
                    AnalyticsEvent.PRODUCT_IMAGE_ADDED,
                    mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to source)
                )
                viewModel.uploadProductImages(navArgs.remoteId, mediaUris)
            }
        }
    }

    private fun handleMediaLibraryPickerResult(result: ActivityResult) {
        result.processMediaLibraryResult()?.let {
            AnalyticsTracker.track(
                AnalyticsEvent.PRODUCT_IMAGE_ADDED,
                mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to AnalyticsTracker.IMAGE_SOURCE_WPMEDIA)
            )
            viewModel.onMediaLibraryImagesAdded(it)
        }
    }

    override fun onExit() {
        viewModel.onNavigateBackButtonClicked()
    }
}
