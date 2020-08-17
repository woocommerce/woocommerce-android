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
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductDownloads
import kotlinx.android.synthetic.main.fragment_product_downloads_settings.*

class ProductDownloadsSettingsFragment : BaseProductFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_downloads_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
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

    fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner, { event ->
            when (event) {
                is ExitProductDownloads -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })

        initFromProductDraft()
        setupListeners()
    }

    private fun initFromProductDraft() {
        fun Int.formatLimitAndExpiry(): String = if (this == -1) "" else this.toString()
        val product = requireNotNull(viewModel.getProduct().productDraft)
        product_download_limit.setText(product.downloadLimit.formatLimitAndExpiry())
        product_download_expiry.setText(product.downloadExpiry.formatLimitAndExpiry())
    }

    private fun setupListeners() {
        product_download_expiry.setOnTextChangedListener {
            val value = if (it.isNullOrEmpty()) -1 else it.toString().toInt()
            viewModel.onDownloadExpiryChanged(value)
            changesMade()
        }
        product_download_limit.setOnTextChangedListener {
            val value = if (it.isNullOrEmpty()) -1 else it.toString().toInt()
            viewModel.onDownloadLimitChanged(value)
            changesMade()
        }
    }

    override fun hasChanges(): Boolean {
        return viewModel.hasDownloadsSettingsChanges()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitProductDownloads())
    }
}
