package com.woocommerce.android.ui.products.ai.productinfo

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.mediapicker.MediaPickerHelper
import com.woocommerce.android.mediapicker.MediaPickerHelper.MediaPickerResultHandler
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.ai.productinfo.AiProductPromptViewModel.ShowMediaDialog
import com.woocommerce.android.ui.products.ai.productinfo.AiProductPromptViewModel.ViewImage
import com.woocommerce.android.ui.products.images.ProductImageViewerFragmentDirections
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AiProductPromptFragment : BaseFragment(), MediaPickerResultHandler {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: AiProductPromptViewModel by viewModels()

    @Inject
    lateinit var mediaPickerHelper: MediaPickerHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    AiProductPromptScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleEvents()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                Exit -> findNavController().navigateUp()

                is ViewImage -> showImageDetail(event.mediaUri)

                is ShowMediaDialog -> mediaPickerHelper.showMediaPicker(event.source)
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
        viewModel.onMediaSelected(mediaUri)
    }

    private fun showImageDetail(imageUri: String) {
        val action = ProductImageViewerFragmentDirections.actionGlobalProductImageViewerFragment(
            isDeletingAllowed = false,
            mediaId = 1,
            remoteId = 0,
            requestCode = 0,
            selectedImage = null,
            showChooser = false,
            images = arrayOf(
                Image(
                    id = 0,
                    name = null,
                    source = imageUri,
                    dateCreated = null,
                )
            )
        )
        findNavController().navigateSafely(action)
    }
}
