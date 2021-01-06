package com.woocommerce.android.ui.products

import android.Manifest.permission
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentProductImagesBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.media.ProductImagesUtils
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
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.util.setHomeIcon
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.WCProductImageGalleryView.OnGalleryImageInteractionListener

class ProductImagesFragment : BaseProductEditorFragment(R.layout.fragment_product_images),
        OnGalleryImageInteractionListener {
    companion object {
        private const val KEY_CAPTURED_PHOTO_URI = "key_captured_photo_uri"
    }

    private val navArgs: ProductImagesFragmentArgs by navArgs()
    private val viewModel: ProductImagesViewModel by navGraphViewModels(R.id.nav_graph_image_gallery) {
        viewModelFactory.get()
    }

    private var _binding: FragmentProductImagesBinding? = null
    private val binding get() = _binding!!

    override val isDoneButtonVisible: Boolean
        get() = viewModel.viewStateData.liveData.value?.isDoneButtonVisible ?: false

    override val isDoneButtonEnabled: Boolean = true
    override val lastEvent: Event?
        get() = viewModel.event.value

    private var imageSourceDialog: AlertDialog? = null
    private var capturedPhotoUri: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductImagesBinding.bind(view)

        savedInstanceState?.let { bundle ->
            capturedPhotoUri = bundle.getParcelable(KEY_CAPTURED_PHOTO_URI)
        }

        setupObservers(viewModel)
        setupResultHandlers(viewModel)
        setupViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            new.isDoneButtonVisible?.takeIfNotEqualTo(old?.isDoneButtonVisible) { isVisible ->
                doneButton?.isVisible = isVisible
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

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
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
        })
    }

    private fun showConfirmationDialog(image: Image) {
        ConfirmRemoveProductImageDialog(
                requireActivity(),
                onPositiveButton = { viewModel.onDeleteImageConfirmed(image) },
                onNegativeButton = { /* no-op */ }
        ).show()
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
                it.findViewById<View>(R.id.textCamera)?.setOnClickListener {
                    viewModel.onShowCameraButtonClicked()
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
        val intent = Intent(Intent.ACTION_GET_CONTENT).also {
            it.type = "image/*"
            it.addCategory(Intent.CATEGORY_OPENABLE)
            it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, viewModel.isMultiSelectionAllowed)
        }
        val chooser = Intent.createChooser(intent, null)
        activity?.startActivityFromFragment(this, chooser, RequestCodes.CHOOSE_PHOTO)
    }

    private fun captureProductImage() {
        if (requestCameraPermission()) {
            val intent = ProductImagesUtils.createCaptureImageIntent(requireActivity())
            if (intent == null) {
                uiMessageResolver.showSnack(R.string.product_images_camera_error)
                return
            }
            capturedPhotoUri = intent.getParcelableExtra(android.provider.MediaStore.EXTRA_OUTPUT)
            requireActivity().startActivityFromFragment(this, intent, RequestCodes.CAPTURE_PHOTO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                RequestCodes.CHOOSE_PHOTO -> data?.let {
                    val uriList = ArrayList<Uri>()
                    val clipData = it.clipData
                    if (clipData != null) {
                        // handle multiple images
                        for (i in 0 until clipData.itemCount) {
                            val uri = clipData.getItemAt(i).uri
                            uriList.add(uri)
                        }
                    } else {
                        // handle single image
                        it.data?.let { uri ->
                            uriList.add(uri)
                        }
                    }
                    if (uriList.isEmpty()) {
                        WooLog.w(T.MEDIA, "Photo chooser returned empty list")
                        return
                    }
                    AnalyticsTracker.track(
                        Stat.PRODUCT_IMAGE_ADDED,
                        mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to AnalyticsTracker.IMAGE_SOURCE_DEVICE)
                    )
                    viewModel.uploadProductImages(navArgs.remoteId, uriList)
                }
                RequestCodes.CAPTURE_PHOTO -> capturedPhotoUri?.let { imageUri ->
                    AnalyticsTracker.track(
                        Stat.PRODUCT_IMAGE_ADDED,
                        mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to AnalyticsTracker.IMAGE_SOURCE_CAMERA)
                    )
                    val uriList = ArrayList<Uri>().also { it.add(imageUri) }
                    viewModel.uploadProductImages(navArgs.remoteId, uriList)
                }
            }
        }
    }

    /**
     * Requests camera permissions, returns true only if camera permission is already available
     */
    private fun requestCameraPermission(): Boolean {
        if (isAdded) {
            if (WooPermissionUtils.hasCameraPermission(requireActivity())) {
                return true
            }
            requestPermissions(arrayOf(permission.CAMERA), RequestCodes.CAMERA_PERMISSION)
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (!isAdded) {
            return
        }

        val allGranted = WooPermissionUtils.setPermissionListAsked(
            requireActivity(), requestCode, permissions, grantResults, checkForAlwaysDenied = true
        )

        if (allGranted) {
            when (requestCode) {
                RequestCodes.CAMERA_PERMISSION -> {
                    captureProductImage()
                }
            }
        }
    }

    override fun onDoneButtonClicked() {
        viewModel.onDoneButtonClicked()
    }

    override fun onExit() {
        viewModel.onNavigateBackButtonClicked()
    }
}
