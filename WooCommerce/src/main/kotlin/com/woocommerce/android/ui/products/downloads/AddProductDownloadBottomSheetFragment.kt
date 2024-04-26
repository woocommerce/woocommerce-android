package com.woocommerce.android.ui.products.downloads

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogProductAddDownloadableFileBinding
import com.woocommerce.android.mediapicker.MediaPickerHelper
import com.woocommerce.android.mediapicker.MediaPickerHelper.MediaPickerResultHandler
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloadDetails
import com.woocommerce.android.ui.products.ProductNavigator
import com.woocommerce.android.ui.products.details.ProductDetailViewModel
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.AddFile
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.PickDocumentFromDevice
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.PickFileFromMedialLibrary
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.PickMediaFileFromDevice
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.SYSTEM_PICKER
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.WP_MEDIA_LIBRARY
import org.wordpress.android.mediapicker.model.MediaTypes.EVERYTHING
import org.wordpress.android.mediapicker.model.MediaTypes.IMAGES_AND_VIDEOS
import javax.inject.Inject

@AndroidEntryPoint
class AddProductDownloadBottomSheetFragment : WCBottomSheetDialogFragment(), MediaPickerResultHandler {
    @Inject
    lateinit var navigator: ProductNavigator

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var mediaPickerHelper: MediaPickerHelper

    private val viewModel: AddProductDownloadViewModel by viewModels()
    private val parentViewModel: ProductDetailViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_products)

    private var _binding: DialogProductAddDownloadableFileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogProductAddDownloadableFileBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        binding.addDownloadableFromWpmediaLibrary.setOnClickListener { viewModel.onMediaGalleryClicked() }
        binding.addMediaFileFromDevice.setOnClickListener { viewModel.onDeviceMediaFilesClicked() }
        binding.addDocumentFileFromDevice.setOnClickListener { viewModel.onDeviceDocumentsClicked() }
        binding.addDownloadableManually.setOnClickListener { viewModel.onEnterURLClicked() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers(viewModel: AddProductDownloadViewModel) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is PickFileFromMedialLibrary -> mediaPickerHelper.showMediaPicker(
                    source = WP_MEDIA_LIBRARY,
                    mediaTypes = EVERYTHING
                )
                is PickMediaFileFromDevice -> mediaPickerHelper.showMediaPicker(
                    source = DEVICE,
                    mediaTypes = IMAGES_AND_VIDEOS
                )
                is PickDocumentFromDevice -> mediaPickerHelper.showMediaPicker(
                    source = SYSTEM_PICKER,
                    mediaTypes = EVERYTHING
                )
                is AddFile -> {
                    findNavController().navigateUp()
                    parentViewModel.handleSelectedDownloadableFile(event.uri.toString())
                }

                is ViewProductDownloadDetails -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        }
    }

    override fun onDeviceMediaSelected(imageUris: List<Uri>, source: String) {
        if (imageUris.isNotEmpty()) {
            viewModel.launchFileUpload(imageUris.first())
        }
    }

    override fun onWPMediaSelected(images: List<Image>) {
        images.forEach {
            viewModel.launchFileUpload(Uri.parse(it.source))
        }
    }
}
