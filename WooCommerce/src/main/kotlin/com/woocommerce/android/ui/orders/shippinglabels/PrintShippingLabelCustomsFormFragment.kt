package com.woocommerce.android.ui.orders.shippinglabels

import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrintShippingLabelCustomsFormFragment : BaseFragment(R.layout.fragment_print_label_customs_form) {
    private val viewModel: PrintShippingLabelCustomsFormViewModel by viewModels()
}
