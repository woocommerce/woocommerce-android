package com.woocommerce.android.ui.orders.creation

import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment

class OrderCreationFragment : BaseFragment(R.layout.fragment_order_creation) {
    private val viewModel: OrderCreationViewModel by viewModels()
}
