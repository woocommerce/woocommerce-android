package com.woocommerce.android.ui.orders.creation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationProductDetailsBinding
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreationProductDetailsFragment : BaseFragment(R.layout.fragment_order_creation_product_details) {
    private val sharedViewModel: OrderCreationViewModel by navGraphViewModels(R.navigation.nav_graph_order_creations)
    private val viewModel: OrderCreationProductDetailsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentOrderCreationProductDetailsBinding.bind(view)
        TODO()
    }
}
