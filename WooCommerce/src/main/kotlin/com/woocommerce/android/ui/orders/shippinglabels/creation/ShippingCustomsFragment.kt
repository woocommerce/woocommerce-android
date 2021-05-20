package com.woocommerce.android.ui.orders.shippinglabels.creation

import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShippingCustomsFragment : BaseFragment(R.layout.fragment_shipping_customs) {
    private val viewModel: ShippingCustomsViewModel by viewModels()
}
