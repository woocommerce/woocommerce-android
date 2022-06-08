package com.woocommerce.android.ui.orders.creation.customerlist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomerListFragment :
    BaseFragment(R.layout.fragment_customer_list) {
    private val viewModel by viewModels<CustomerListViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_customer_search_title)
}
