package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductExternalLinkBinding
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitExternalLink
import org.wordpress.android.util.ActivityUtils

class ProductExternalLinkFragment : BaseProductFragment(R.layout.fragment_product_external_link) {
    private var _binding: FragmentProductExternalLinkBinding? = null
    private val binding get() = _binding!!

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getFragmentTitle() = getString(R.string.product_external_link)

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitExternalLink -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
        updateProductView(viewModel.getProduct())
    }

    private fun updateProductView(productData: ProductDetailViewState) {
        if (!isAdded) return

        val product = requireNotNull(productData.productDraft)
        binding.productUrl.setText(product.externalUrl)
        binding.productButtonText.setText(product.buttonText)
    }

    override fun onRequestAllowBackPress(): Boolean {
        ActivityUtils.hideKeyboard(activity)
        viewModel.onBackButtonClicked(ExitExternalLink())
        return false
    }
}
