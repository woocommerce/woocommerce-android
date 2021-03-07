package com.woocommerce.android.ui.orders.creation.addcustomer

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.databinding.FragmentOrderCreationAddCustomerBinding
import com.woocommerce.android.ui.orders.creation.common.base.BaseOrderCreationFragment
import com.woocommerce.android.ui.orders.creation.common.navigation.OrderCreationNavigator
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddCustomerFragment : BaseOrderCreationFragment(layout.fragment_order_creation_add_customer) {
    @Inject lateinit var navigator: OrderCreationNavigator
    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: AddCustomerViewModel by viewModels { viewModelFactory }

    private var _binding: FragmentOrderCreationAddCustomerBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOrderCreationAddCustomerBinding.bind(view)
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_add_customer_title)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
