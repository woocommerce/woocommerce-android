package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment

class PrintShippingLabelFragment : BaseFragment() {
    override fun getFragmentTitle() = getString(R.string.orderdetail_shipping_label_reprint)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_print_shipping_label, container, false)
    }
}
