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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentProductImagesBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.products.ProductImagesViewModel.ProductImagesState.Browsing
import com.woocommerce.android.ui.products.ProductImagesViewModel.ProductImagesState.Dragging
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowCamera
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowDeleteImageConfirmation
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowImageDetail
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowImageSourceDialog
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowStorageChooser
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowWPMediaPicker
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerFragment.Companion.KEY_WP_IMAGE_PICKER_RESULT
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.setHomeIcon
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowActionSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.WCProductImageGalleryView.OnGalleryImageInteractionListener
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.mediapicker.MediaPickerConstants
import org.wordpress.android.mediapicker.MediaPickerUtils
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.model.MediaTypes
import org.wordpress.android.mediapicker.source.device.DeviceMediaPickerSetup
import org.wordpress.android.mediapicker.ui.MediaPickerActivity
import java.security.InvalidParameterException
import javax.inject.Inject

@AndroidEntryPoint
class ProductImagesFragment :
    BaseProductEditorFragment(R.layout.fragment_product_images),
    OnGalleryImageInteractionListener {
    companion object {
        private const val KEY_CAPTURED_PHOTO_URI = "key_captured_photo_uri"
    }

    private val navArgs: ProductImagesFragmentArgs by navArgs()
    private val viewModel: ProductImagesViewModel by hiltNavGraphViewModels(R.id.nav_graph_image_gallery)

    @Inject lateinit var navigator: ProductNavigator
    @Inject lateinit var mediaPickerUtils: MediaPickerUtils

    private var _binding: FragmentProductImagesBinding? = null
    private val binding get() = _binding!!

    override val lastEvent: Event?
        get() = viewModel.event.value

    private var imageSourceDialog: AlertDialog? = null
    private var capturedPhotoUri: Uri? = null
    private var imageUploadErrorsSnackbar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductImagesBinding.bind(view)

        savedInstanceState?.let { bundle ->
            capturedPhotoUri = bundle.getParcelable(KEY_CAPTURED_PHOTO_URI)
        }

        setHasOptionsMenu(true)

        setupObservers(viewModel)
        setupResultHandlers(viewModel)
        setupViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        imageUploadErrorsSnackbar?.dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_CAPTURED_PHOTO_URI, capturedPhotoUri)
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

    private fun setupResultHandlers(viewModel: ProductImagesViewModel) {
        handleResult<List<Image>>(KEY_WP_IMAGE_PICKER_RESULT) {
            viewModel.onImagesAdded(it)
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
                    ShowStorageChooser -> chooseProductImage()
                    ShowCamera -> captureProductImage()
                    ShowWPMediaPicker -> showWPMediaPicker()
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
            viewModel.isImageDeletingAllowed, image.id
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

    private fun showWPMediaPicker() {
        val action = ProductImagesFragmentDirections.actionGlobalWpMediaFragment(viewModel.isMultiSelectionAllowed)
        findNavController().navigateSafely(action)
    }

    private fun chooseProductImage() {
        val intent = MediaPickerActivity.buildIntent(
            requireContext(),
            DeviceMediaPickerSetup.buildMediaPicker(
                mediaTypes = MediaTypes.IMAGES,
                canMultiSelect = viewModel.isMultiSelectionAllowed
            )
        )

        resultLauncher.launch(intent)
    }

    private fun captureProductImage() {
        val intent = MediaPickerActivity.buildIntent(
            requireContext(),
            DeviceMediaPickerSetup.buildCameraPicker()
        )

        resultLauncher.launch(intent)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        handleMediaPickerResult(it)
    }

    @Suppress("NestedBlockDepth")
    private fun handleMediaPickerResult(result: ActivityResult) {
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val mediaUris = (result.data?.extras?.get(MediaPickerConstants.EXTRA_MEDIA_URIS) as? Array<*>)
                ?.mapNotNull { it as? String }
                ?.map { Uri.parse(it) }
                ?: emptyList()

            if (mediaUris.isEmpty()) {
                WooLog.w(T.MEDIA, "Media picker returned empty list")
            } else {
                val sourceExtra = result.data?.getStringExtra(MediaPickerConstants.EXTRA_MEDIA_SOURCE)
                if (sourceExtra != null) {
                    val source = when (val dataSource = MediaPickerSetup.DataSource.valueOf(sourceExtra)) {
                        MediaPickerSetup.DataSource.SYSTEM_PICKER,
                        MediaPickerSetup.DataSource.DEVICE -> AnalyticsTracker.IMAGE_SOURCE_DEVICE
                        MediaPickerSetup.DataSource.CAMERA -> AnalyticsTracker.IMAGE_SOURCE_CAMERA
                        else -> throw InvalidParameterException("${dataSource.name} is not a supported data source")
                    }
                    AnalyticsTracker.track(
                        Stat.PRODUCT_IMAGE_ADDED,
                        mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to source)
                    )
                } else {
                    WooLog.w(T.MEDIA, "Media picker returned empty media source")
                }

                viewModel.uploadProductImages(navArgs.remoteId, mediaUris)
            }
        }
    }

    override fun onExit() {
        viewModel.onNavigateBackButtonClicked()
    }
}
