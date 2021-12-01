package com.woocommerce.android.ui.orders.creation

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationBinding
import com.woocommerce.android.ui.base.BaseFragment

class OrderCreationFragment : BaseFragment(R.layout.fragment_order_creation) {
    private val viewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentOrderCreationBinding.bind(view)) {
            setupObserversWith(this)
        }
        viewModel.start()
    }

    private fun setupObserversWith(binding: FragmentOrderCreationBinding) {
        viewModel.orderDraft.observe(viewLifecycleOwner) {
            binding.orderStatusView.updateOrder(it)
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_fragment_title)
}
