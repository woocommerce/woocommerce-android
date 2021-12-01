package com.woocommerce.android.ui.orders.creation

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationBinding
import com.woocommerce.android.ui.base.BaseFragment

class OrderCreationFragment : BaseFragment(R.layout.fragment_order_creation) {
    private val navigationViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentOrderCreationBinding.bind(view)) {
            setupObserversWith(this)
        }
    }

    private fun setupObserversWith(binding: FragmentOrderCreationBinding) {
        navigationViewModel.orderDraftData.observe(viewLifecycleOwner) { _, newOrderData ->
            binding.orderStatusView.updateOrder(newOrderData)
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_fragment_title)
}
