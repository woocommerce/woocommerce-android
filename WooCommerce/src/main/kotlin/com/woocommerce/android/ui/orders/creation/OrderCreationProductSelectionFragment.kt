package com.woocommerce.android.ui.orders.creation

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationProductSelectionBinding
import com.woocommerce.android.ui.base.BaseFragment

class OrderCreationProductSelectionFragment :
    BaseFragment(R.layout.fragment_order_creation_product_selection) {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentOrderCreationProductSelectionBinding.bind(view)) {

        }
    }
}
