package com.woocommerce.android.ui.orders.creation.shipping

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationShippingFeesBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreationShippingFeesFragment : BaseFragment(R.layout.fragment_order_creation_shipping_fees) {
    private val viewModel: OrderCreationShippingFeesViewModel by viewModels()
    private val sharedViewModel: OrderCreationViewModel by navGraphViewModels(R.id.nav_graph_order_creations)

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentOrderCreationShippingFeesBinding.bind(view)
        binding.amountEditText.initView(
            currency = sharedViewModel.currentDraft.currency,
            decimals = viewModel.currencyDecimals,
            currencyFormatter = currencyFormatter
        )
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.order_creation_shipping_title_add)
    }
}
