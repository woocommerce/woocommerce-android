package com.woocommerce.android.ui.products.addons.product

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentProductAddonsBinding
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductAddons
import com.woocommerce.android.ui.products.addons.AddonListAdapter
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
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
        viewModel.event.observe(viewLifecycleOwner, Observer(::onEventReceived))
        storedAddons?.let { setupRecyclerViewWith(it, viewModel.currencyCode) }
    }

    private fun onEventReceived(event: MultiLiveEvent.Event) {
        when (event) {
            is ExitProductAddons -> findNavController().navigateUp()
            else -> event.isHandled = false
        }
    }

    override fun getFragmentTitle() = getString(R.string.product_add_ons_card_button_title)

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
        viewModel.onBackButtonClicked(ExitProductAddons)
        return false
    }
}
