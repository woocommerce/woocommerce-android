package com.woocommerce.android.ui.products.addons.order

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderedAddonBinding
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderedAddonFragment : BaseFragment(R.layout.fragment_ordered_addon) {
    companion object {
        val TAG: String = OrderedAddonFragment::class.java.simpleName
    }

    private val viewModel: OrderedAddonViewModel by viewModels()

    private val navArgs: OrderedAddonFragmentArgs by navArgs()

    private var _binding: FragmentOrderedAddonBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrderedAddonBinding.bind(view)
        viewModel.start(
            navArgs.orderId,
            navArgs.orderItemId,
            navArgs.addonsProductId
        )
    }
}
