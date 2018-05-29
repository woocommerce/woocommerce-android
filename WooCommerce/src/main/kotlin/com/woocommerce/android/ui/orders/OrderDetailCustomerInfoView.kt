package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.util.AddressUtils
import com.woocommerce.android.util.PhoneUtils
import kotlinx.android.synthetic.main.order_detail_customer_info.view.*
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderDetailCustomerInfoView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_customer_info, this)
    }

    fun initView(order: WCOrderModel, shippingOnly: Boolean, listener: OrderCustomerActionListener? = null) {
        // Populate Shipping information
        customerInfo_shippingName.text = context
                .getString(R.string.customer_full_name, order.shippingFirstName, order.shippingLastName)

        val billingAddr = AddressUtils.getEnvelopeAddress(order.getBillingAddress())
        val billingCountry = AddressUtils.getCountryLabelByCountryCode(order.billingCountry)

        // display shipping address info
        if (order.hasSeparateShippingDetails()) {
            customerInfo_shippingAddr.text = AddressUtils.getEnvelopeAddress(order.getShippingAddress())
            customerInfo_shippingCountry.text = AddressUtils.getCountryLabelByCountryCode(order.shippingCountry)
        } else {
            customerInfo_shippingAddr.text = billingAddr
            customerInfo_shippingCountry.text = billingCountry
        }

        if (shippingOnly) {
            // Only display the shipping information in this card and hide everything else.
            formatViewAsShippingOnly()
        } else {
            // Populate Billing Information
            customerInfo_billingName.text = context
                    .getString(R.string.customer_full_name, order.billingFirstName, order.billingLastName)

            // display billing address info
            customerInfo_billingAddr.text = billingAddr
            customerInfo_billingCountry.text = billingCountry

            // display email address info
            customerInfo_emailAddr.text = order.billingEmail

            // display phone
            if (!order.billingPhone.isEmpty()) {
                customerInfo_phone.text = PhoneUtils.formatPhone(order.billingPhone)
            }

            // configure more/less button
            customerInfo_viewMore.setOnCheckedChangeListener { _, isChecked ->
                customerInfo_morePanel.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            // Set action button listeners
            customerInfo_emailBtn.setOnClickListener {
                listener?.createEmail(order.billingEmail)
            }

            customerInfo_phoneBtn.setOnClickListener {
                listener?.dialPhone(order.billingPhone)
            }

            customerInfo_hangoutsBtn.setOnClickListener {
                listener?.sendSms(order.billingPhone)
            }
        }
    }

    private fun formatViewAsShippingOnly() {
        customerInfo_viewMore.visibility = View.GONE
    }
}
