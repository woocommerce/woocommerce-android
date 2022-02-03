package com.woocommerce.android.ui.orders.creation.fees

import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreationAddFeeFragment :
    BaseFragment(R.layout.fragment_order_creation_add_fee) {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)
    private val addFeeViewModel by viewModels<OrderCreationAddFeeViewModel>()
}
