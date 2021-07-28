package com.woocommerce.android.ui.products.addons

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentProductAddonsBinding
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductAddons

class ProductAddonsFragment : BaseProductFragment(R.layout.fragment_product_addons) {
    companion object {
        const val TAG = "ProductAddonsFragment"
    }

    private var layoutManager: LayoutManager? = null

    private var _binding: FragmentProductAddonsBinding? = null
    private val binding get() = _binding!!

    private val storedAddons
        get() = viewModel.getProduct().storedProduct?.addons

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storedAddons?.let { binding.addonsList.adapter = AddonListAdapter(it) }
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitProductAddons)
        return false
    }
}
