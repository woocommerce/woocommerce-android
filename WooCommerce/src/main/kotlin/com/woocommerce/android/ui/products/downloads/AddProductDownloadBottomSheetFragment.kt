package com.woocommerce.android.ui.products.downloads

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
import com.woocommerce.android.databinding.DialogProductAddDownloadableFileBinding
import com.woocommerce.android.databinding.DialogProductListSortingBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloadDetails
import com.woocommerce.android.ui.products.ProductNavigator
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.PickFileFromDevice
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.PickFileFromMedialLibrary
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.UploadFile
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerFragment
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.Lazy
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

private const val CHOOSE_FILE_REQUEST_CODE = 1
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

    private var _binding: DialogProductAddDownloadableFileBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        savedInstanceState?.let { bundle ->
            capturedPhotoUri = bundle.getParcelable(KEY_CAPTURED_PHOTO_URI)
        }
        _binding = DialogProductAddDownloadableFileBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        binding.addDownloadableFromWpmediaLibrary.setOnClickListener { viewModel.onMediaGalleryClicked() }
        binding.addDownloadableFromDevice.setOnClickListener { viewModel.onDeviceClicked() }
        binding.addDownloadableManually.setOnClickListener { viewModel.onEnterURLClicked() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_CAPTURED_PHOTO_URI, capturedPhotoUri)
    }

    private fun setupResultHandlers(viewModel: AddProductDownloadViewModel) {
        handleResult<List<Image>>(WPMediaPickerFragment.KEY_WP_IMAGE_PICKER_RESULT) { images ->
            images.forEach { viewModel.launchFileUpload(Uri.parse(it.source)) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CHOOSE_FILE_REQUEST_CODE -> {
                    intent?.data
                        ?.let { viewModel.launchFileUpload(it) }
                        ?: WooLog.w(T.MEDIA, "File picker return an null data")
                }
            }
        }
    }

    private fun setupObservers(viewModel: AddProductDownloadViewModel) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
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
        }
    }

    private fun showWPMediaPicker() {
        val action = AddProductDownloadBottomSheetFragmentDirections
            .actionGlobalWpMediaFragment(false)
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

    override fun androidInjector(): AndroidInjector<Any> = childInjector
}
