package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductDownloads
import kotlinx.android.synthetic.main.fragment_product_downloads_list.*

class ProductDownloadsFragment : BaseProductFragment() {
    private val productDownloadsAdapter: ProductDownloadsAdapter by lazy { ProductDownloadsAdapter(viewModel::onProductDownloadClicked) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        with(productDownloadsRecycler) {
            adapter = productDownloadsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.onDoneButtonClicked(ExitProductDownloads(shouldShowDiscardDialog = false))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getFragmentTitle(): String = getString(R.string.product_downloadable_files)

    fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner, { event ->
            when (event) {
                is ExitProductDownloads -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })

        val product = requireNotNull(viewModel.getProduct().productDraft)
        productDownloadsAdapter.filesList = product.downloads
    }

    override fun hasChanges(): Boolean {
        return viewModel.hasChanges()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitProductDownloads())
    }
}
