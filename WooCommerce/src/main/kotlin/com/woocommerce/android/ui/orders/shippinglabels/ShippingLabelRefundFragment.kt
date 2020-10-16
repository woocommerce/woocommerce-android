package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.formatToMMMddYYYYhhmm
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_shipping_label_refund.*
import javax.inject.Inject

class ShippingLabelRefundFragment : BaseFragment(), BackPressListener {
    companion object {
        const val KEY_REFUND_SHIPPING_LABEL_RESULT = "key_refund_shipping_label_result"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ShippingLabelRefundViewModel by viewModels { viewModelFactory }

    @Inject lateinit var uiMessageResolver: UIMessageResolver
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
    }

    private fun setupObservers(viewModel: ShippingLabelRefundViewModel) {
        viewModel.shippingLabelRefundViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.shippingLabel?.takeIfNotEqualTo(old?.shippingLabel) { showShippingLabelDetails(it) }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.getSnack(event.message, *event.args).show()
                is Exit -> navigateBackWithResult(KEY_REFUND_SHIPPING_LABEL_RESULT, true)
                else -> event.isHandled = false
            }
        })
    }

    private fun showShippingLabelDetails(shippingLabel: ShippingLabel) {
        val formattedAmount = currencyFormatter.buildBigDecimalFormatter(shippingLabel.currency)(shippingLabel.rate)
        shippingLabelRefund_amount.text = formattedAmount

        with(shippingLabelRefund_btnRefund) {
            text = getString(R.string.shipping_label_refund_button, formattedAmount)
            setOnClickListener { viewModel.onRefundShippingLabelButtonClicked() }
        }

        shippingLabelRefund_purchaseDate.text = shippingLabel.createdDate?.formatToMMMddYYYYhhmm()
    }

    override fun getFragmentTitle() = getString(R.string.orderdetail_shipping_label_request_refund)

    override fun onRequestAllowBackPress(): Boolean {
        if (viewModel.isRefundInProgress) {
            Toast.makeText(context, R.string.order_refunds_refund_in_progress, Toast.LENGTH_SHORT).show()
        } else {
            findNavController().popBackStack()
        }
        return false
    }
}
