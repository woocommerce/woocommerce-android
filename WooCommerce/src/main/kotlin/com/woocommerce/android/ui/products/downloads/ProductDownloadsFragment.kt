package com.woocommerce.android.ui.products.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductDownloads
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerFragment
import com.woocommerce.android.widgets.CustomProgressDialog
import kotlinx.android.synthetic.main.fragment_product_downloads_list.*

class ProductDownloadsFragment : BaseProductFragment() {
    private val itemTouchHelper by lazy {
        DraggableItemTouchHelper(UP or DOWN) { from, to ->
            viewModel.swapDownloadableFiles(from, to)
            updateFilesFromProductDraft()
        }
    }

    private val productDownloadsAdapter: ProductDownloadsAdapter by lazy {
        ProductDownloadsAdapter(
            viewModel::onProductDownloadClicked,
            itemTouchHelper
        )
    }

    private var progressDialog: CustomProgressDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_downloads_list, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        setupResultHandlers(viewModel)
        with(productDownloadsRecycler) {
            adapter = productDownloadsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_product_downloads_list, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.onDoneButtonClicked(ExitProductDownloads(shouldShowDiscardDialog = false))
                true
            }
            R.id.menu_product_downloads_settings -> {
                viewModel.onDownloadsSettingsClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupResultHandlers(viewModel: ProductDetailViewModel) {
        handleResult<List<Image>>(WPMediaPickerFragment.KEY_WP_IMAGE_PICKER_RESULT) {
            viewModel.showAddProductDownload(it.first().source)
            changesMade()
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

        addProductDownloadsView.initView { viewModel.onAddDownloadableFileClicked() }

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
        changesMade()
    }

    override fun hasChanges(): Boolean {
        return viewModel.hasDownloadsChanges() || viewModel.hasDownloadsSettingsChanges()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitProductDownloads())
    }
}
