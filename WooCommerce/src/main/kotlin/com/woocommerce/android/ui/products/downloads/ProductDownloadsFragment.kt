package com.woocommerce.android.ui.products.downloads

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductDownloadsListBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.details.ProductDetailViewModel
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductExitEvent.ExitProductDownloads
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.DraggableItemTouchHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductDownloadsFragment :
    BaseProductFragment(R.layout.fragment_product_downloads_list) {
    private val itemTouchHelper by lazy {
        DraggableItemTouchHelper(
            dragDirs = UP or DOWN,
            onMove = { from, to ->
                viewModel.swapDownloadableFiles(from, to)
                updateFilesFromProductDraft()
            }
        )
    }

    private var _binding: FragmentProductDownloadsListBinding? = null
    private val binding get() = _binding!!

    private val productDownloadsAdapter: ProductDownloadsAdapter by lazy {
        ProductDownloadsAdapter(
            viewModel::onProductDownloadClicked,
            itemTouchHelper
        )
    }

    private var progressDialog: CustomProgressDialog? = null

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductDownloadsListBinding.bind(view)

        setupObservers(viewModel)

        with(binding.productDownloadsRecycler) {
            adapter = productDownloadsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            itemTouchHelper.attachToRecyclerView(this)
        }

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_downloadable_files),
            onMenuItemSelected = ::onMenuItemSelected,
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    viewModel.onBackButtonClicked(ExitProductDownloads)
                }
                onCreateMenu(toolbar)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onCreateMenu(toolbar: Toolbar) {
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_product_downloads_list)
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_product_downloads_settings -> {
                viewModel.onDownloadsSettingsClicked()
                true
            }
            else -> false
        }
    }

    override fun getFragmentTitle(): String = getString(R.string.product_downloadable_files)

    fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.productDownloadsViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isUploadingDownloadableFile?.takeIfNotEqualTo(old?.isUploadingDownloadableFile) {
                if (it) {
                    showUploadingProgressDialog()
                } else {
                    hideUploadingProgressDialog()
                }
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitProductDownloads -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }

        binding.addProductDownloadsView.initView { viewModel.onAddDownloadableFileClicked() }

        updateFilesFromProductDraft()
    }

    private fun showUploadingProgressDialog() {
        hideUploadingProgressDialog()
        progressDialog = CustomProgressDialog.show(
            getString(R.string.product_downloadable_files_upload_dialog_title),
            getString(R.string.product_downloadable_files_upload_dialog_message)
        ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    private fun hideUploadingProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun updateFilesFromProductDraft() {
        val product = requireNotNull(viewModel.getProduct().productDraft)
        productDownloadsAdapter.filesList = product.downloads
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitProductDownloads)
        return false
    }
}
