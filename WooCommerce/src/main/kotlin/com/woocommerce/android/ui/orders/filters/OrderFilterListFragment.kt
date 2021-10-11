package com.woocommerce.android.ui.orders.filters

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderFilterListBinding
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderFilterListFragment :
    BaseFragment(R.layout.fragment_order_filter_list) {

    private val viewModel: OrderFilterListViewModel by hiltNavGraphViewModels(R.id.nav_graph_order_filters)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentOrderFilterListBinding.bind(view)
        setHasOptionsMenu(true)
    }
}
