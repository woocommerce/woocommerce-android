package com.woocommerce.android.ui.products

import android.Manifest.permission
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_IMAGE_TAPPED
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
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private lateinit var viewModel: ProductImagesViewModel

    private val navArgs: ProductImagesFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_images, container, false)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModel()
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
                    // TODO chooseProductImage()
                }
                WooPermissionUtils.CAMERA_PERMISSION_REQUEST_CODE -> {
                    // TODO captureProduceImage()
                }
            }
        }
    }
}
