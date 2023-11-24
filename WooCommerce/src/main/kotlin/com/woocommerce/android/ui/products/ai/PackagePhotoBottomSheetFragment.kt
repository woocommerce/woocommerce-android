package com.woocommerce.android.ui.products.ai

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.mediapicker.MediaPickerHelper
import com.woocommerce.android.mediapicker.MediaPickerHelper.MediaPickerResultHandler
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ShowMediaLibrary
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ShowMediaLibraryDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PackagePhotoBottomSheetFragment : WCBottomSheetDialogFragment(), MediaPickerResultHandler {
    companion object {
        const val KEY_PACKAGE_PHOTO_SCAN_RESULT = "key_ai_package_photo_scan_result"
    }

    private val viewModel: PackagePhotoViewModel by viewModels()

    @Inject lateinit var mediaPickerHelper: MediaPickerHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooTheme {
                    PackagePhotoBottomSheet(viewModel = viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEvents()
    }

    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ExitWithResult<*> -> navigateBackWithResult(
                    KEY_PACKAGE_PHOTO_SCAN_RESULT,
                    event.data
                )

                is ShowMediaLibraryDialog -> viewModel.onMediaLibraryDialogRequested()

                is ShowMediaLibrary -> mediaPickerHelper.showMediaPicker(event.source)
            }
        }
    }
    override fun onDeviceMediaSelected(imageUris: List<Uri>, source: String) {
        if (imageUris.isNotEmpty()) {
            onImageSelected(imageUris.first().toString())
        }
    }

    override fun onWPMediaSelected(images: List<Image>) {
        if (images.isNotEmpty()) {
            onImageSelected(images.first().source)
        }
    }

    private fun onImageSelected(mediaUri: String) {
        viewModel.onImageChanged(mediaUri)
    }
}
