package com.woocommerce.android.ui.products.addons

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentProductAddonsBinding
import com.woocommerce.android.model.ProductAddon
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
        _binding = FragmentProductAddonsBinding.bind(view)
        storedAddons?.let { setupRecyclerViewWith(it) }
    }

    private fun setupRecyclerViewWith(addonList: List<ProductAddon>) {
        layoutManager = LinearLayoutManager(
            activity,
            RecyclerView.VERTICAL,
            false
        )
        binding.addonsList.layoutManager = layoutManager
        binding.addonsList.adapter = AddonListAdapter(addonList)
    }

    override fun onRequestAllowBackPress(): Boolean {
        // TODO fix back button click not working
        viewModel.onBackButtonClicked(ExitProductAddons)
        return false
    }
}
