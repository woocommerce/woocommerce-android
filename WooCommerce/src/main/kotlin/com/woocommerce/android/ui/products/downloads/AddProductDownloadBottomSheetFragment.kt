package com.woocommerce.android.ui.products.downloads

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.media.ProductImagesUtils
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloadDetails
import com.woocommerce.android.ui.products.ProductNavigator
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.PickFileFromDevice
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.PickFileFromMedialLibrary
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.UploadFile
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.Lazy
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_product_add_downloadable_file.*
import javax.inject.Inject

private const val CHOOSE_FILE_REQUEST_CODE = 1
private const val CAPTURE_PHOTO_REQUEST_CODE = 2
private const val KEY_CAPTURED_PHOTO_URI = "captured_photo_uri"

class AddProductDownloadBottomSheetFragment : BottomSheetDialogFragment(), HasAndroidInjector {
    @Inject internal lateinit var childInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var navigator: ProductNavigator
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>

    private val viewModel: AddProductDownloadViewModel by viewModels { viewModelFactory.get() }
    private val parentViewModel: ProductDetailViewModel by navGraphViewModels(R.id.nav_graph_products) {
        viewModelFactory.get()
    }
    private var capturedPhotoUri: Uri? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        savedInstanceState?.let { bundle ->
            capturedPhotoUri = bundle.getParcelable(KEY_CAPTURED_PHOTO_URI)
        }
        return inflater.inflate(R.layout.dialog_product_add_downloadable_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        add_downloadable_from_wpmedia_library.setOnClickListener { viewModel.onMediaGalleryClicked() }
        add_downloadable_from_device.setOnClickListener { viewModel.onDeviceClicked() }
        add_downloadable_manually.setOnClickListener { viewModel.onEnterURLClicked() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_CAPTURED_PHOTO_URI, capturedPhotoUri)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CHOOSE_FILE_REQUEST_CODE -> {
                    if (intent?.data == null) {
                        WooLog.w(T.MEDIA, "File picker return an null data")
                        return
                    }
                    viewModel.launchFileUpload(intent.data!!)
                }
                CAPTURE_PHOTO_REQUEST_CODE -> capturedPhotoUri?.let { imageUri ->
                    viewModel.launchFileUpload(imageUri)
                }
            }
        }
    }

    private fun setupObservers(viewModel: AddProductDownloadViewModel) {
        viewModel.event.observe(viewLifecycleOwner, { event ->
            when (event) {
                is PickFileFromMedialLibrary -> showWPMediaPicker()
                is PickFileFromDevice -> chooseFile()
                is UploadFile -> {
                    parentViewModel.uploadDownloadableFile(event.uri)
                    findNavController().navigateUp()
                }
                is ViewProductDownloadDetails -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        })
    }

    private fun showWPMediaPicker() {
        val action = AddProductDownloadBottomSheetFragmentDirections
            .actionGlobalWpMediaFragment(RequestCodes.WPMEDIA_LIBRARY_PICK_DOWNLOADABLE_FILE, multiSelect = false)
        findNavController().navigateSafely(action)
    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).also {
            it.type = "image/*"
            it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }
        val chooser = Intent.createChooser(intent, null)
        startActivityForResult(chooser, CHOOSE_FILE_REQUEST_CODE)
    }

    private fun captureProductImage() {
        if (requestCameraPermission()) {
            val intent = ProductImagesUtils.createCaptureImageIntent(requireActivity())
            if (intent == null) {
                uiMessageResolver.showSnack(R.string.product_images_camera_error)
                // dismiss the dialog to allow the snackbar to be shown
                findNavController().navigateUp()
                return
            }
            capturedPhotoUri = intent.getParcelableExtra(android.provider.MediaStore.EXTRA_OUTPUT)
            startActivityForResult(intent, CAPTURE_PHOTO_REQUEST_CODE)
        }
    }

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

    override fun androidInjector(): AndroidInjector<Any> = childInjector
}
