package com.woocommerce.android.ui.products

import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.viewmodel.ViewModelFactory
import javax.inject.Inject

class GroupedProductListFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: GroupedProductListViewModel by viewModels { viewModelFactory }
}
