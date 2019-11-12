package com.woocommerce.android.ui.products

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.ui.aztec.MediaToolbarCameraButton
import com.woocommerce.android.ui.aztec.MediaToolbarGalleryButton
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.GlideImageLoader
import kotlinx.android.synthetic.main.fragment_aztec_editor.*
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.AztecAttributes
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.plugins.IMediaToolbarButton.IMediaToolbarClickListener
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener
import java.util.Random

class AztecEditorFragment : BaseFragment(), IAztecToolbarClickListener,
        OnRequestPermissionsResultCallback {
    companion object {
        const val TAG: String = "AztecEditorFragment"
        private const val MEDIA_CAMERA_PHOTO_PERMISSION_REQUEST_CODE: Int = 1001
        private const val MEDIA_PHOTOS_PERMISSION_REQUEST_CODE: Int = 1003
        private const val REQUEST_MEDIA_CAMERA_PHOTO: Int = 2001
        private const val REQUEST_MEDIA_PHOTO: Int = 2003
    }

    private lateinit var aztec: Aztec

    private val navArgs: AztecEditorFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_aztec_editor, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.product_description)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as? MainActivity)?.hideBottomNav()

        val galleryButton = MediaToolbarGalleryButton(aztecToolbar)
        galleryButton.setMediaToolbarButtonClickListener(object : IMediaToolbarClickListener {
            override fun onClick(view: View) {
                onPhotosMediaOptionSelected()
            }
        })

        val cameraButton = MediaToolbarCameraButton(aztecToolbar)
        cameraButton.setMediaToolbarButtonClickListener(object : IMediaToolbarClickListener {
            override fun onClick(view: View) {
                onCameraPhotoMediaOptionSelected()
            }
        })

        aztec = Aztec.with(visualEditor, sourceEditor, aztecToolbar, this)
                .setImageGetter(GlideImageLoader(requireContext()))
                .addPlugin(galleryButton)
                .addPlugin(cameraButton)

        aztec.visualEditor.fromHtml(navArgs.productDescription)
        aztec.sourceEditor?.displayStyledAndFormattedHtml(navArgs.productDescription)

        aztec.initSourceEditorHistory()
    }

    override fun onToolbarCollapseButtonClicked() {
        // Aztec Toolbar interface methods implemented by default with Aztec. Currently not used
    }

    override fun onToolbarExpandButtonClicked() {
        // Aztec Toolbar interface methods implemented by default with Aztec. Currently not used
    }

    override fun onToolbarFormatButtonClicked(format: ITextFormat, isKeyboardShortcut: Boolean) {
        // Aztec Toolbar interface methods implemented by default with Aztec. Currently not used
    }

    override fun onToolbarHeadingButtonClicked() {
        // Aztec Toolbar interface methods implemented by default with Aztec. Currently not used
    }

    override fun onToolbarHtmlButtonClicked() {
        aztec.toolbar.toggleEditorMode()
    }

    override fun onToolbarListButtonClicked() {
        // Aztec Toolbar interface methods implemented by default with Aztec. Currently not used
    }

    override fun onToolbarMediaButtonClicked(): Boolean {
        return false
    }

    /**
     * TODO: TEMP code added just for testing media support in Aztec
     * This code will be removed once Image upload is integrated
     */
    private lateinit var mediaFile: String
    private lateinit var mediaPath: String

    private fun onCameraPhotoMediaOptionSelected() {
        // TODO: handle camera functionality
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun onPhotosMediaOptionSelected() {
        if (PermissionUtils.checkAndRequestStoragePermission(
                        this,
                        MEDIA_PHOTOS_PERMISSION_REQUEST_CODE
                )) {
            val intent: Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Intent(Intent.ACTION_OPEN_DOCUMENT)
            } else {
                Intent.createChooser(
                        Intent(Intent.ACTION_GET_CONTENT),
                        getString(R.string.title_select_photo)
                )
            }

            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"

            try {
                startActivityForResult(intent, REQUEST_MEDIA_PHOTO)
            } catch (exception: ActivityNotFoundException) {
                AppLog.e(AppLog.T.EDITOR, exception.message)
                ToastUtils.showToast(
                        requireContext(),
                        getString(R.string.error_chooser_photo),
                        ToastUtils.Duration.LONG
                )
                        .show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_MEDIA_CAMERA_PHOTO -> {
                    // By default, BitmapFactory.decodeFile sets the bitmap's density to the device default so, we need
                    //  to correctly set the input density to 160 ourselves.
                    val options = BitmapFactory.Options()
                    options.inDensity = DisplayMetrics.DENSITY_DEFAULT
                    val bitmap = BitmapFactory.decodeFile(mediaPath, options)
                    insertImageAndSimulateUpload(bitmap, mediaPath)
                }
                REQUEST_MEDIA_PHOTO -> {
                    mediaPath = data?.data.toString()
                    val stream = requireActivity().contentResolver.openInputStream(Uri.parse(mediaPath))
                    // By default, BitmapFactory.decodeFile sets the bitmap's density to the device default so, we need
                    //  to correctly set the input density to 160 ourselves.
                    val options = BitmapFactory.Options()
                    options.inDensity = DisplayMetrics.DENSITY_DEFAULT
                    val bitmap = BitmapFactory.decodeStream(stream, null, options)

                    insertImageAndSimulateUpload(bitmap, mediaPath)
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun insertImageAndSimulateUpload(bitmap: Bitmap?, mediaPath: String) {
        val bitmapResized = ImageUtils.getScaledBitmapAtLongestSide(bitmap, aztec.visualEditor.maxImagesWidth)
        val (id, attrs) = generateAttributesForMedia(mediaPath)
        aztec.visualEditor.insertImage(BitmapDrawable(resources, bitmapResized), attrs)

        // TODO: upload the image to media library in backend
//        insertMediaAndSimulateUpload(id, attrs)

        aztec.visualEditor.refreshText()
        aztec.toolbar.toggleMediaToolbar()
    }

    private fun generateAttributesForMedia(mediaPath: String): Pair<String, AztecAttributes> {
        val id = Random().nextInt(Integer.MAX_VALUE).toString()
        val attrs = AztecAttributes()
        attrs.setValue("src", mediaPath) // Temporary source value.  Replace with URL after uploaded.
        attrs.setValue("id", id)
        attrs.setValue("uploading", "true")
        return Pair(id, attrs)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MEDIA_CAMERA_PHOTO_PERMISSION_REQUEST_CODE -> {
                var isPermissionDenied = false

                for (i in grantResults.indices) {
                    when (permissions[i]) {
                        Manifest.permission.CAMERA -> {
                            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                                isPermissionDenied = true
                            }
                        }
                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                                isPermissionDenied = true
                            }
                        }
                    }
                }

                if (isPermissionDenied) {
                    // TODO: handle permission denied
//                    ToastUtils.showToast(this, getString(R.string.permission_required_media_camera))
                } else {
                    when (requestCode) {
                        MEDIA_CAMERA_PHOTO_PERMISSION_REQUEST_CODE -> {
                            onCameraPhotoMediaOptionSelected()
                        }
                    }
                }
            }
            MEDIA_PHOTOS_PERMISSION_REQUEST_CODE -> {
                var isPermissionDenied = false

                for (i in grantResults.indices) {
                    when (permissions[i]) {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                                isPermissionDenied = true
                            }
                        }
                    }
                }

                when (requestCode) {
                    MEDIA_PHOTOS_PERMISSION_REQUEST_CODE -> {
                        if (isPermissionDenied) {
                            // TODO: handle permission denied
//                            ToastUtils.showToast(this, getString(R.string.permission_required_media_photos))
                        } else {
                            onPhotosMediaOptionSelected()
                        }
                    }
                }
            }
            else -> {
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
