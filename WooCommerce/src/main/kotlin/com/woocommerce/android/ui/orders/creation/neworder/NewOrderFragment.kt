package com.woocommerce.android.ui.orders.creation.neworder

import android.content.Context
import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.databinding.FragmentNewOrderBinding
import com.woocommerce.android.ui.orders.creation.common.OrderCreationBaseFragment
import com.woocommerce.android.util.setHomeIcon
import dagger.android.support.AndroidSupportInjection

class NewOrderFragment : OrderCreationBaseFragment(layout.fragment_new_order) {
    private var _binding: FragmentNewOrderBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentNewOrderBinding.bind(view)

        setHomeIcon(R.drawable.ic_gridicons_cross_24dp)
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_new_order)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
