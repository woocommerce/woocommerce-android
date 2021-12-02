package com.woocommerce.android.ui.orders.creation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationFormBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreationFormFragment : BaseFragment(R.layout.fragment_order_creation_form) {
    private val navigationViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)
    private val formViewModel by viewModels<OrderCreationFormViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentOrderCreationFormBinding.bind(view)) {
            setupObserversWith(this)
        }
    }

    private fun setupObserversWith(binding: FragmentOrderCreationFormBinding) {
        navigationViewModel.orderDraftData.observe(viewLifecycleOwner) { _, newOrderData ->
            binding.orderStatusView.updateOrder(newOrderData)
            formViewModel.onOrderStatusSelected(newOrderData.status)
        }

        formViewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.orderStatus?.takeIfNotEqualTo(old?.orderStatus) {
                binding.orderStatusView.updateStatus(it) {
                }
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_fragment_title)
}
