package com.woocommerce.android.ui.orders.creation.product.discount

import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreateEditProductDiscountFragment : BaseFragment() {
    private val viewModel: OrderCreateEditProductDiscountViewModel by viewModels()
}
