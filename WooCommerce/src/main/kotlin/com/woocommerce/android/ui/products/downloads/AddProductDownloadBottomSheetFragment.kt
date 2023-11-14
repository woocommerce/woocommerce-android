package com.woocommerce.android.ui.products.downloads

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogProductAddDownloadableFileBinding
import com.woocommerce.android.mediapicker.MediaPickerUtil.processDeviceMediaResult
import com.woocommerce.android.mediapicker.MediaPickerUtil.processMediaLibraryResult
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloadDetails
import com.woocommerce.android.ui.products.ProductNavigator
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.AddFile
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.PickDocumentFromDevice
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.PickFileFromMedialLibrary
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.PickMediaFileFromDevice
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.model.MediaTypes
import org.wordpress.android.mediapicker.ui.MediaPickerActivity
import javax.inject.Inject

@AndroidEntryPoint
class AddProductDownloadBottomSheetFragment : WCBottomSheetDialogFragment() {
    @Inject
    lateinit var navigator: ProductNavigator
    @Inject
    lateinit var uiMessageResolver: UIMessageResolver
    @Inject
    lateinit var mediaPickerSetupFactory: MediaPickerSetup.Factory

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
                is PickFileFromMedialLibrary -> showMediaLibraryPicker()
                is PickMediaFileFromDevice -> showLocalDeviceMediaPicker()
                is PickDocumentFromDevice -> showLocalDeviceDocumentPicker()
                is AddFile -> {
                    findNavController().navigateUp()
                    parentViewModel.handleSelectedDownloadableFile(event.uri.toString())
                }

                is ViewProductDownloadDetails -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        }
    }

    private val mediaLibraryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        handleMediaLibraryPickerResult(it)
    }

    private val mediaDeviceMediaPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        handleDeviceMediaResult(it)
    }

    private fun handleMediaLibraryPickerResult(result: ActivityResult) {
        result.processMediaLibraryResult()?.let { images ->
            images.forEach {
                viewModel.launchFileUpload(Uri.parse(it.source))
            }
        }
    }

    private fun showMediaLibraryPicker() {
        val intent = MediaPickerActivity.buildIntent(
            requireContext(),
            mediaPickerSetupFactory.build(
                source = MediaPickerSetup.DataSource.WP_MEDIA_LIBRARY,
                mediaTypes = MediaTypes.EVERYTHING
            )
        )

        mediaLibraryLauncher.launch(intent)
    }

    private fun handleDeviceMediaResult(result: ActivityResult) {
        result.processDeviceMediaResult()?.let { mediaUris ->
            if (mediaUris.isNotEmpty()) {
                viewModel.launchFileUpload(mediaUris.first())
            }
        }
    }

    private fun showLocalDeviceMediaPicker() {
        val intent = MediaPickerActivity.buildIntent(
            requireContext(),
            mediaPickerSetupFactory.build(
                source = MediaPickerSetup.DataSource.DEVICE,
                mediaTypes = MediaTypes.MEDIA
            )
        )

        mediaDeviceMediaPickerLauncher.launch(intent)
    }

    private fun showLocalDeviceDocumentPicker() {
        val intent = MediaPickerActivity.buildIntent(
            requireContext(),
            mediaPickerSetupFactory.build(
                source = MediaPickerSetup.DataSource.SYSTEM_PICKER,
                mediaTypes = MediaTypes.DOCUMENTS
            )
        )

        mediaDeviceMediaPickerLauncher.launch(intent)
    }
}
