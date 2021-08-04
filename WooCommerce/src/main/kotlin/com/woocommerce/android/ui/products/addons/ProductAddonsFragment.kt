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
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductAddonsFragment : BaseProductFragment(R.layout.fragment_product_addons) {
    companion object {
        val TAG: String = ProductAddonsFragment::class.java.simpleName
    }

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private var layoutManager: LayoutManager? = null

    private var _binding: FragmentProductAddonsBinding? = null
    private val binding get() = _binding!!

    private val storedAddons
        get() = viewModel.getProduct().storedProduct?.addons

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductAddonsBinding.bind(view)
        storedAddons?.let { setupRecyclerViewWith(it, viewModel.currencyCode) }
    }

    private fun setupRecyclerViewWith(addonList: List<ProductAddon>, currencyCode: String) {
        layoutManager = LinearLayoutManager(
            activity,
            RecyclerView.VERTICAL,
            false
        )
        binding.addonsList.layoutManager = layoutManager
        binding.addonsList.adapter = AddonListAdapter(
            addonList,
            currencyFormatter.buildBigDecimalFormatter(currencyCode)
        )
    }

    override fun onRequestAllowBackPress(): Boolean {
        // TODO fix back button click not working
        viewModel.onBackButtonClicked(ExitProductAddons)
        return false
    }
}
