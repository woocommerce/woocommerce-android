package com.woocommerce.android.ui.products.downloads

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductDownloadsSettingsBinding
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.details.ProductDetailViewModel
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductExitEvent.ExitProductDownloadsSettings
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductDownloadsSettingsFragment : BaseProductFragment(R.layout.fragment_product_downloads_settings) {
    private var _binding: FragmentProductDownloadsSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductDownloadsSettingsBinding.bind(view)

        setupObservers(viewModel)

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_downloadable_files_download_settings),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    viewModel.onBackButtonClicked(ExitProductDownloadsSettings)
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitProductDownloadsSettings -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }

        initFromProductDraft()
        setupListeners()
    }

    private fun initFromProductDraft() {
        fun Number.formatLimitAndExpiry(): String = if (this.toLong() == -1L) "" else this.toString()
        val product = requireNotNull(viewModel.getProduct().productDraft)
        binding.productDownloadLimit.text = product.downloadLimit.formatLimitAndExpiry()
        binding.productDownloadExpiry.text = product.downloadExpiry.formatLimitAndExpiry()
    }

    private fun setupListeners() {
        binding.productDownloadExpiry.setOnTextChangedListener {
            val value = if (it.isNullOrEmpty()) -1 else it.toString().toInt()
            viewModel.onDownloadExpiryChanged(value)
        }
        binding.productDownloadLimit.setOnTextChangedListener {
            val value = if (it.isNullOrEmpty()) -1 else it.toString().toLong()
            viewModel.onDownloadLimitChanged(value)
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitProductDownloadsSettings)
        return false
    }
}
