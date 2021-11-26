package com.woocommerce.android.ui.orders.creation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationBinding
import com.woocommerce.android.ui.base.BaseFragment

class OrderCreationFragment : BaseFragment(R.layout.fragment_order_creation) {
    private val viewModel: OrderCreationViewModel by viewModels()

    private var _binding: FragmentOrderCreationBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrderCreationBinding.bind(view)
    }
}
