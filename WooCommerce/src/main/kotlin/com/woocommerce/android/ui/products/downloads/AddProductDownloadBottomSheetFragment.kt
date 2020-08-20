package com.woocommerce.android.ui.products.downloads

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.products.downloads.AddProductDownloadViewModel.PickFileFromMedialLibrary
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.Lazy
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_product_add_downloadable_file.*
import kotlinx.android.synthetic.main.layout_file_picker_sources.*
import javax.inject.Inject

private const val CHOOSE_FILE_REQUEST_CODE = 1

class AddProductDownloadBottomSheetFragment : BottomSheetDialogFragment(), HasAndroidInjector {
    @Inject internal lateinit var childInjector: DispatchingAndroidInjector<Any>

    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>

    val viewModel: AddProductDownloadViewModel by viewModels { viewModelFactory.get() }
    val parentViewModel: ProductDetailViewModel by navGraphViewModels(R.id.nav_graph_products) {
        viewModelFactory.get()
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_product_add_downloadable_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        textWPMediaLibrary.setOnClickListener { viewModel.onMediaGalleryClicked() }
        textChooser.setOnClickListener { chooseFile() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupObservers(viewModel: AddProductDownloadViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner, { old, new ->
            product_add_downloadable_sources.isVisible = !new.isUploading
        })

        viewModel.event.observe(viewLifecycleOwner, { event ->
            when (event) {
                is PickFileFromMedialLibrary -> showWPMediaPicker()
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
            it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        val chooser = Intent.createChooser(intent, null)
        activity?.startActivityFromFragment(this, chooser, CHOOSE_FILE_REQUEST_CODE)
    }

    override fun androidInjector(): AndroidInjector<Any> = childInjector
}
