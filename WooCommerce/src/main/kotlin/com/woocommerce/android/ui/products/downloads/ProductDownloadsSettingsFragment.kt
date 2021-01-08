package com.woocommerce.android.ui.products.downloads

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentProductDownloadsSettingsBinding
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductDownloads
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductDownloadsSettings

class ProductDownloadsSettingsFragment : BaseProductFragment(R.layout.fragment_product_downloads_settings) {
    private var _binding: FragmentProductDownloadsSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductDownloadsSettingsBinding.bind(view)

        setHasOptionsMenu(true)
        setupObservers(viewModel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                AnalyticsTracker.track(Stat.PRODUCT_DOWNLOADABLE_FILES_SETTINGS_CHANGED)
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
        fun Number.formatLimitAndExpiry(): String = if (this == -1) "" else this.toString()
        val product = requireNotNull(viewModel.getProduct().productDraft)
        binding.productDownloadLimit.setText(product.downloadLimit.formatLimitAndExpiry())
        binding.productDownloadExpiry.setText(product.downloadExpiry.formatLimitAndExpiry())
    }

    private fun setupListeners() {
        binding.productDownloadExpiry.setOnTextChangedListener {
            val value = if (it.isNullOrEmpty()) -1 else it.toString().toInt()
            viewModel.onDownloadExpiryChanged(value)
            changesMade()
        }
        binding.productDownloadLimit.setOnTextChangedListener {
            val value = if (it.isNullOrEmpty()) -1 else it.toString().toLong()
            viewModel.onDownloadLimitChanged(value)
            changesMade()
        }
    }

    override fun hasChanges(): Boolean {
        return viewModel.hasDownloadsSettingsChanges()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitProductDownloadsSettings())
    }
}
