package com.woocommerce.android.ui.products.addons.order

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderedAddonBinding
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.addons.AddonListAdapter
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderedAddonFragment : BaseFragment(R.layout.fragment_ordered_addon) {
    companion object {
        val TAG: String = OrderedAddonFragment::class.java.simpleName
    }

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: OrderedAddonViewModel by viewModels()

    private val navArgs: OrderedAddonFragmentArgs by navArgs()

    private var _binding: FragmentOrderedAddonBinding? = null
    private val binding get() = _binding!!
    private var layoutManager: LayoutManager? = null
    private val supportActionBar
        get() = activity
            ?.let { it as? AppCompatActivity }
            ?.supportActionBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrderedAddonBinding.bind(view)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_24dp)

        setupObservers()
        viewModel.start(
            navArgs.orderId,
            navArgs.orderItemId,
            navArgs.addonsProductId
        )
    }

    override fun getFragmentTitle() = getString(R.string.product_add_ons_title)

    private fun setupObservers() {
        viewModel.orderedAddonsData
            .observe(viewLifecycleOwner, Observer(::onOrderedAddonsReceived))
    }

    private fun onOrderedAddonsReceived(orderedAddons: List<ProductAddon>) {
        setupRecyclerViewWith(orderedAddons)
    }

    private fun setupRecyclerViewWith(addonList: List<ProductAddon>) {
        layoutManager = LinearLayoutManager(
            activity,
            RecyclerView.VERTICAL,
            false
        )
        binding.addonsList.layoutManager = layoutManager
        binding.addonsList.adapter = AddonListAdapter(
            addonList,
            currencyFormatter.buildBigDecimalFormatter(viewModel.currencyCode),
            orderMode = true
        )
    }
}
