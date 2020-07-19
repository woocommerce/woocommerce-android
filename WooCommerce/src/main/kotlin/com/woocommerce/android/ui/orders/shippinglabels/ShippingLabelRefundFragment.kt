package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment

class ShippingLabelRefundFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_shipping_label_refund, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.orderdetail_shipping_label_request_refund)
}
