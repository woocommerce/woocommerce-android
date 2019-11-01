package com.woocommerce.android.ui.products

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_IMAGE_TAPPED
import com.woocommerce.android.media.MediaUploadUtils
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.imageviewer.ImageViewerActivity
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.widgets.WCProductImageGalleryView.OnGalleryImageClickListener
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_product_images.*
import org.wordpress.android.fluxc.model.WCProductImageModel
import javax.inject.Inject

class ProductImagesFragment : BaseFragment(), OnGalleryImageClickListener {
    companion object {
        private const val REQUEST_CODE_CHOOSE_PHOTO = Activity.RESULT_FIRST_USER
        private const val REQUEST_CODE_CAPTURE_PHOTO = REQUEST_CODE_CHOOSE_PHOTO + 1

        private const val KEY_CAPTURED_PHOTO_URI = "captured_photo_uri"
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private lateinit var viewModel: ProductImagesViewModel

    private val navArgs: ProductImagesFragmentArgs by navArgs()
    private var capturedPhotoUri: Uri? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        savedInstanceState?.let { bundle ->
            capturedPhotoUri = bundle.getParcelable(KEY_CAPTURED_PHOTO_URI)
        }
        return inflater.inflate(R.layout.fragment_product_images, container, false)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_CAPTURED_PHOTO_URI, capturedPhotoUri)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModel()
        addImageButton.setOnClickListener {
            showImageSourceMenu()
        }
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ProductImagesViewModel::class.java).also {
            setupObservers(it)
        }
        viewModel.start(navArgs.remoteProductId)
    }

    private fun setupObservers(viewModel: ProductImagesViewModel) {
        viewModel.product.observe(this, Observer {
            imageGallery.showProductImages(it, this)
        })

        viewModel.showSnackbarMessage.observe(this, Observer {
            uiMessageResolver.showSnack(it)
        })

        viewModel.chooseProductImage.observe(this, Observer {
            chooseProductImage()
        })

        viewModel.captureProductImage.observe(this, Observer {
            captureProductImage()
        })

        viewModel.isUploadingProductImage.observe(this, Observer {
            showUploadImageProgress(it)
        })

        viewModel.exit.observe(this, Observer {
            activity?.onBackPressed()
        })
    }

    override fun getFragmentTitle() = getString(R.string.product_images_title)

    override fun onGalleryImageClicked(image: WCProductImageModel, imageView: View) {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        ImageViewerActivity.show(
                requireActivity(),
                image.src,
                title = getFragmentTitle(),
                sharedElement = imageView
        )
    }

    // TODO we need icons to show
    private fun showImageSourceMenu() {
        val popup = PopupMenu(requireActivity(), addImageButton).also { it.inflate(R.menu.menu_image_source) }
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_device_chooser -> {
                    viewModel.onChooseImageClicked()
                    true
                }
                R.id.menu_device_camera -> {
                    viewModel.onCaptureImageClicked()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun chooseProductImage() {
        // only show the chooser if user already allowed storage permission, otherwise simply request the
        // permission and do nothing else - this will be called again if the user then agrees to allow
        // storage permission
        if (requestStoragePermission()) {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            val chooser = Intent.createChooser(intent, getString(R.string.product_change_image))
            activity?.startActivityFromFragment(this, chooser, REQUEST_CODE_CHOOSE_PHOTO)
        }
    }

    private fun captureProductImage() {
        if (requestCameraPermission()) {
            MediaUploadUtils.createCaptureImageIntent(requireActivity())?.let { intent ->
                capturedPhotoUri = intent.getParcelableExtra(android.provider.MediaStore.EXTRA_OUTPUT)
                requireActivity().startActivityFromFragment(this, intent,
                        REQUEST_CODE_CAPTURE_PHOTO
                )
            }
        }
    }

    /**
     * Triggered by the viewModel when an image is being uploaded or has finished uploading
     */
    private fun showUploadImageProgress(isUploading: Boolean) {
        // TODO - for now we simply show a progress spinner in the middle of the gallery, a separate PR
        // will tackle showing the progress on the image being replaced
        imageUploadProgess.visibility = if (isUploading) View.VISIBLE else View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE_PHOTO && data != null) {
                data.data?.let { imageUri ->
                    viewModel.uploadProductMedia(navArgs.remoteProductId, imageUri)
                }
            } else if (requestCode == REQUEST_CODE_CAPTURE_PHOTO) {
                capturedPhotoUri?.let { imageUri ->
                    viewModel.uploadProductMedia(navArgs.remoteProductId, imageUri)
                }
            }
        }
    }

    /**
     * Requests storage permission, returns true only if permission is already available
     */
    private fun requestStoragePermission(): Boolean {
        if (!isAdded) {
            return false
        } else if (WooPermissionUtils.hasStoragePermission(activity!!)) {
            return true
        }

        val permissions = arrayOf(permission.WRITE_EXTERNAL_STORAGE)
        requestPermissions(permissions, WooPermissionUtils.STORAGE_PERMISSION_REQUEST_CODE)
        return false
    }

    /**
     * Requests camera & storage permissions, returns true only if permissions are already
     * available. Note that we need to ask for both permissions because we also need storage
     * permission to store media from the camera.
     */
    private fun requestCameraPermission(): Boolean {
        if (!isAdded) {
            return false
        }

        val hasStorage = WooPermissionUtils.hasStoragePermission(activity!!)
        val hasCamera = WooPermissionUtils.hasCameraPermission(activity!!)
        if (hasStorage && hasCamera) {
            return true
        }

        val permissions = when {
            hasStorage -> arrayOf(permission.CAMERA)
            hasCamera -> arrayOf(permission.WRITE_EXTERNAL_STORAGE)
            else -> arrayOf(permission.CAMERA, permission.WRITE_EXTERNAL_STORAGE)
        }

        requestPermissions(
                permissions,
                WooPermissionUtils.CAMERA_PERMISSION_REQUEST_CODE
        )
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
                activity!!, requestCode, permissions, grantResults, checkForAlwaysDenied = true
        )

        if (allGranted) {
            when (requestCode) {
                WooPermissionUtils.STORAGE_PERMISSION_REQUEST_CODE -> {
                    chooseProductImage()
                }
                WooPermissionUtils.CAMERA_PERMISSION_REQUEST_CODE -> {
                    captureProductImage()
                }
            }
        }
    }
}
