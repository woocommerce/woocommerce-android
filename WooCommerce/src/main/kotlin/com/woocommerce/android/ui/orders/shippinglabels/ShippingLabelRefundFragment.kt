package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.formatToMMMddYYYYhhmm
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_shipping_label_refund.*
import javax.inject.Inject

class ShippingLabelRefundFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ShippingLabelRefundViewModel
        by navGraphViewModels(R.id.nav_graph_shipping_labels) { viewModelFactory }

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shipping_label_refund, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        viewModel.start()
    }

    private fun setupObservers(viewModel: ShippingLabelRefundViewModel) {
        viewModel.shippingLabelRefundViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.shippingLabel?.takeIfNotEqualTo(old?.shippingLabel) { showShippingLabelDetails(it) }
        }
    }

    private fun showShippingLabelDetails(shippingLabel: ShippingLabel) {
        val formattedAmount = currencyFormatter.buildBigDecimalFormatter(shippingLabel.currency)(shippingLabel.rate)
        shippingLabelRefund_amount.text = formattedAmount
        shippingLabelRefund_btnRefund.text = getString(R.string.shipping_label_refund_button, formattedAmount)

        shippingLabelRefund_purchaseDate.text = shippingLabel.createdDate?.formatToMMMddYYYYhhmm()
    }

    override fun getFragmentTitle() = getString(R.string.orderdetail_shipping_label_request_refund)
}
