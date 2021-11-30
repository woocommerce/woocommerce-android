package com.woocommerce.android.ui.orders.creation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationBinding
import com.woocommerce.android.ui.base.BaseFragment

class OrderCreationFragment : BaseFragment(R.layout.fragment_order_creation) {
    private val viewModel: OrderCreationViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentOrderCreationBinding.bind(view)) {
            setupObserversWith(this)
        }
        viewModel.start()
    }

    private fun setupObserversWith(binding: FragmentOrderCreationBinding) {

    }
}
