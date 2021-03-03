package com.woocommerce.android.ui.orders.creation.common

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.common.navigation.OrderCreationNavigator
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.Lazy
import javax.inject.Inject

abstract class OrderCreationBaseFragment : BaseFragment {
    @Inject lateinit var navigator: OrderCreationNavigator
    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>

    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    protected val viewModel: OrderCreationViewModel by navGraphViewModels(R.id.nav_graph_order_creation) {
        viewModelFactory.get()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    private fun setupObservers(viewModel: OrderCreationViewModel) {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                else -> event.isHandled = false
            }
        })
    }
}
