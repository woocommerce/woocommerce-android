package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductExternalLinkBinding
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.details.ProductDetailViewModel
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductExitEvent.ExitExternalLink
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils

@AndroidEntryPoint
class ProductExternalLinkFragment : BaseProductFragment(R.layout.fragment_product_external_link) {
    private var _binding: FragmentProductExternalLinkBinding? = null
    private val binding get() = _binding!!

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductExternalLinkBinding.bind(view)
        setupObservers(viewModel)

        binding.productUrl.setOnTextChangedListener {
            viewModel.updateProductDraft(externalUrl = it.toString())
        }
        binding.productButtonText.setOnTextChangedListener {
            viewModel.updateProductDraft(buttonText = it.toString())
        }

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_external_link),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    onRequestAllowBackPress()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitExternalLink -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
        updateProductView(viewModel.getProduct())
    }

    private fun updateProductView(productData: ProductDetailViewState) {
        if (!isAdded) return

        val product = requireNotNull(productData.productDraft)
        binding.productUrl.text = product.externalUrl
        binding.productButtonText.text = product.buttonText
    }

    override fun onRequestAllowBackPress(): Boolean {
        ActivityUtils.hideKeyboard(activity)
        viewModel.onBackButtonClicked(ExitExternalLink)
        return false
    }
}
