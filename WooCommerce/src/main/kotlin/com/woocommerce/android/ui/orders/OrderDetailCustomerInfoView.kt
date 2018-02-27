package com.woocommerce.android.ui.orders

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.google.i18n.addressinput.common.AddressData
import com.google.i18n.addressinput.common.FormOptions
import com.google.i18n.addressinput.common.FormatInterpreter
import com.woocommerce.android.R
import com.woocommerce.android.util.PhoneUtils
import kotlinx.android.synthetic.main.order_detail_customer_info.view.*
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderDetailCustomerInfoView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : RelativeLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_customer_info, this)
    }

    fun initView(order: WCOrderModel) {
        // todo - how to properly display names?
        customerInfo_custName.text = order.billingFirstName + " " + order.billingLastName

        // display shipping address info
        val formatInterpreter = FormatInterpreter(FormOptions().createSnapshot())

        // display billing address info
        val billingAddressData: AddressData = AddressData.builder()
                .setAddressLines(mutableListOf(order.billingAddress1, order.billingAddress2))
                .setLocality(order.billingCity)
                .setAdminArea(order.billingState)
                .setPostalCode(order.billingPostcode)
                .setCountry(order.billingCountry)
                .setOrganization(order.billingCompany)
                .build()
        val billingAddressFrags = formatInterpreter.getEnvelopeAddress(billingAddressData)
        customerInfo_billingAddr.text = TextUtils.join(System.getProperty("line.separator"), billingAddressFrags)

        if (order.hasSeparateShippingDetails()) {
            val shippingAddressData: AddressData = AddressData.builder()
                    .setAddressLines(mutableListOf(order.shippingAddress1, order.shippingAddress2))
                    .setLocality(order.shippingCity)
                    .setAdminArea(order.shippingState)
                    .setPostalCode(order.shippingPostcode)
                    .setCountry(order.shippingCountry)
                    .setOrganization(order.shippingCompany)
                    .build()
            val addressFrags = formatInterpreter.getEnvelopeAddress(shippingAddressData)
            customerInfo_shippingAddr.text = TextUtils.join(System.getProperty("line.separator"), addressFrags)
        } else {
            customerInfo_shippingAddr.text = TextUtils.join(System.getProperty("line.separator"), billingAddressFrags)
        }

        // display email address info
        // todo - validate email?
        customerInfo_emailAddr.text = order.billingEmail

        if (!order.billingPhone.isNullOrEmpty()) {
            customerInfo_phone.text = PhoneUtils.formatPhone(context, order.billingPhone)
        }

        customerInfo_viewMore.setOnCheckedChangeListener { _, isChecked ->
            customerInfo_moreLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }
}
